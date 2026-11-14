package io.github.kaerbr.pizzaorder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.SoftApConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Where the tablet's own hotspot stands, as the admin panel and the QR screen show it. */
sealed interface HotspotStatus {
    data object Off : HotspotStatus
    data object Starting : HotspotStatus
    /** [access] is what Android picked: the app can read name and password, not choose them. */
    data class Running(val access: WifiAccess) : HotspotStatus
    /** [message] is what the admin reads: permission missing, tethering already on, location off. */
    data class Failed(val message: String) : HotspotStatus
}

data class HotspotRuntime(
    /** The admin's choice; the hotspot itself only runs while the guest server does. */
    val enabled: Boolean = false,
    val status: HotspotStatus = HotspotStatus.Off,
)

/**
 * What Android wants before an app may open a hotspot: "Geräte in der Nähe" from Android 13 on, the precise
 * location before that (there the system also insists on location being switched on).
 */
object HotspotPermission {

    /** What to ask for. Android 12 ignores a request for the precise location alone, hence both. */
    val names: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        }

    /** The first of [names] is the one the system checks; "ungefährer Standort" alone is not enough. */
    fun isMissing(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, names.first()) != PackageManager.PERMISSION_GRANTED
}

/**
 * The tablet's own WLAN for the party: a local-only hotspot the app opens itself, so the evening needs no
 * router, no internet and no trip into Android's settings.
 *
 * Android decides the network's name and password and makes up new ones on every start; the app can only
 * read them. That is why they go into a QR code instead of into a setting — and why a printed card is only
 * good for as long as this hotspot runs.
 *
 * The hotspot belongs to the process: it stays up while [GuestService] keeps the app alive and ends with it.
 * Android may also end it on its own (nobody connected for a while, the radio needed elsewhere); then it is
 * requested again once, with a new name and password, which the screen picks up.
 */
class GuestHotspot(private val context: Context, private val settings: GuestSettings) {

    private val _runtime = MutableStateFlow(HotspotRuntime(enabled = settings.hotspot))
    val runtime: StateFlow<HotspotRuntime> = _runtime.asStateFlow()

    private val lock = Any()
    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null

    /** True between [start] and [stop]: a callback arriving after a stop must not bring the hotspot back. */
    private var wanted = false

    /** Counts requests, so the callback of an abandoned one can tell it is no longer the current one. */
    private var generation = 0
    private var requestedAt = 0L

    /**
     * The admin's switch. The choice is kept either way; the radio only comes on while guests can order.
     *
     * @param serverRunning whether the guest server listens right now — without it a hotspot leads nowhere.
     */
    fun setEnabled(enabled: Boolean, serverRunning: Boolean) {
        settings.hotspot = enabled
        _runtime.update { it.copy(enabled = enabled) }
        if (enabled && serverRunning) start() else stop()
    }

    /** Opens the hotspot if the admin chose it; a second call while it runs or starts changes nothing. */
    fun start() {
        synchronized(lock) {
            if (!settings.hotspot) return
            val status = _runtime.value.status
            if (wanted && (status is HotspotStatus.Starting || status is HotspotStatus.Running)) return
            wanted = true
            requestLocked()
        }
    }

    /** Closes the hotspot and leaves the admin's choice alone, so the next server start brings it back. */
    fun stop() {
        synchronized(lock) {
            wanted = false
            generation++
            runCatching { reservation?.close() }
            reservation = null
            _runtime.update { it.copy(status = HotspotStatus.Off) }
        }
    }

    // The permission is checked right above the call; lint can't see through HotspotPermission.
    @SuppressLint("MissingPermission")
    private fun requestLocked() {
        if (HotspotPermission.isMissing(context)) {
            failLocked("Berechtigung für den Hotspot fehlt. Bitte erlauben.")
            return
        }
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        if (wifi == null) {
            failLocked("Dieses Tablet hat kein WLAN.")
            return
        }
        val current = ++generation
        requestedAt = SystemClock.elapsedRealtime()
        _runtime.update { it.copy(status = HotspotStatus.Starting) }
        try {
            wifi.startLocalOnlyHotspot(Callback(current), Handler(Looper.getMainLooper()))
        } catch (e: SecurityException) {
            // Up to Android 12 the system refuses while location is switched off, whatever the app may do.
            failLocked("Android verweigert den Hotspot. Ist der Standort am Tablet eingeschaltet?")
        } catch (e: RuntimeException) {
            failLocked("Hotspot startet nicht: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun failLocked(message: String) {
        wanted = false
        runCatching { reservation?.close() }
        reservation = null
        _runtime.update { it.copy(status = HotspotStatus.Failed(message)) }
    }

    private inner class Callback(private val mine: Int) : WifiManager.LocalOnlyHotspotCallback() {

        override fun onStarted(started: WifiManager.LocalOnlyHotspotReservation) {
            synchronized(lock) {
                if (!wanted || mine != generation) {
                    // Switched off while Android was still starting it: hand the radio back.
                    runCatching { started.close() }
                    return
                }
                reservation = started
                val access = accessOf(started)
                if (access == null) {
                    failLocked("Android nennt Name und Passwort des Hotspots nicht.")
                } else {
                    _runtime.update { it.copy(status = HotspotStatus.Running(access)) }
                }
            }
        }

        override fun onStopped() {
            synchronized(lock) {
                if (!wanted || mine != generation) return
                reservation = null
                // Android ended it, not the admin. Once more, unless it only just started: a hotspot that
                // dies right away would otherwise be requested in a loop.
                if (SystemClock.elapsedRealtime() - requestedAt > RETRY_AFTER_MS) {
                    requestLocked()
                } else {
                    failLocked("Android hat den Hotspot beendet. Aus- und wieder einschalten.")
                }
            }
        }

        override fun onFailed(reason: Int) {
            synchronized(lock) {
                if (mine != generation) return
                failLocked(explain(reason))
            }
        }
    }

    private fun explain(reason: Int): String = when (reason) {
        WifiManager.LocalOnlyHotspotCallback.ERROR_INCOMPATIBLE_MODE ->
            "Am Tablet läuft schon ein Hotspot. Den in den Android-Einstellungen ausschalten oder ihn selbst benutzen."
        WifiManager.LocalOnlyHotspotCallback.ERROR_TETHERING_DISALLOWED ->
            "Hotspots sind auf diesem Tablet gesperrt."
        WifiManager.LocalOnlyHotspotCallback.ERROR_NO_CHANNEL ->
            "Kein freier Funkkanal für den Hotspot."
        else -> "Android konnte den Hotspot nicht starten."
    }

    @Suppress("DEPRECATION") // getSsid() and WifiConfiguration: the only way to plain strings on every version.
    private fun accessOf(reservation: WifiManager.LocalOnlyHotspotReservation): WifiAccess? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val config = reservation.softApConfiguration
            val ssid = config.ssid?.takeIf { it.isNotEmpty() } ?: return null
            return WifiAccess(
                ssid = ssid,
                password = config.passphrase,
                sae = config.securityType == SoftApConfiguration.SECURITY_TYPE_WPA3_SAE,
            )
        }
        val config = reservation.wifiConfiguration ?: return null
        val ssid = config.SSID?.removeSurrounding("\"")?.takeIf { it.isNotEmpty() } ?: return null
        return WifiAccess(ssid, config.preSharedKey?.removeSurrounding("\""))
    }

    private companion object {
        const val RETRY_AFTER_MS = 30_000L
    }
}
