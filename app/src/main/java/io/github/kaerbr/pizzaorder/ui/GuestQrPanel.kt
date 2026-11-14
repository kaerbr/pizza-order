package io.github.kaerbr.pizzaorder.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.kaerbr.pizzaorder.GuestQr
import io.github.kaerbr.pizzaorder.GuestWifi
import io.github.kaerbr.pizzaorder.WifiAccess
import kotlinx.coroutines.delay
import io.github.kaerbr.pizzaorder.LocalAddress

/**
 * The tablet's address, kept current while the panel or the QR screen is open.
 *
 * A DHCP lease, a dropped WLAN or a hotspot switched on changes the address behind a running server's back:
 * the socket keeps listening, the QR code on screen starts pointing at nobody. The callback is what turns a
 * new default network into a recomposition, instead of a value read once when the view opened.
 *
 * A hotspot is the exception the callback cannot cover: it is no `Network` at all (see [LocalAddress]), so
 * nothing is announced when it appears and the fallback list changes silently. Hence the slow poll — only
 * while this is composed, and cheap: a `NetworkInterface` walk, no socket and no radio.
 *
 * @param preferHotspot true while the app's own hotspot runs: the guests are in that network, so its address
 *   is the one to hand out even when the tablet is in the house WLAN as well.
 */
@Composable
internal fun rememberLocalIpv4(preferHotspot: Boolean = false): String? {
    val context = LocalContext.current
    var address by remember(preferHotspot) { mutableStateOf(LocalAddress.ipv4(context, preferHotspot)) }

    LaunchedEffect(context, preferHotspot) {
        while (true) {
            delay(2_000)
            address = LocalAddress.ipv4(context, preferHotspot)
        }
    }

    DisposableEffect(context, preferHotspot) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = refresh()
            override fun onLost(network: Network) = refresh()
            // A new lease usually arrives here, not as a new network.
            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) = refresh()
            private fun refresh() { address = LocalAddress.ipv4(context, preferHotspot) }
        }
        runCatching { manager?.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { manager?.unregisterNetworkCallback(callback) } }
    }

    return address
}

/**
 * The guest URL as text with a copy button.
 *
 * On a tablet the URL is the fallback for a phone whose camera won't scan; typing `192.168.178.23:8080/?k=x7k2m9`
 * by hand is the part that fails. Copying it into a messenger is how the link actually reaches a guest, so the
 * button has to hand over the exact string the QR code encodes.
 *
 * The system clipboard, not Compose's: `LocalClipboardManager` is deprecated and `LocalClipboard` wants a
 * `ClipEntry`. Three lines through [android.content.ClipboardManager] do the same on every supported version.
 */
@Composable
internal fun GuestUrl(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var copied by remember(url) { mutableStateOf(false) }
    // Android 13+ shows its own confirmation overlay; a second one on screen would just flicker.
    val showLabel = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

    LaunchedEffect(copied) {
        if (!copied) return@LaunchedEffect
        delay(2_000)
        copied = false
    }

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            url,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard?.setPrimaryClip(ClipData.newPlainText("Gäste-Link", url))
            copied = true
        }) {
            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (copied && showLabel) "Kopiert" else "Kopieren")
        }
    }
}

/**
 * The QR code a guest scans, drawn module by module in one path.
 *
 * Always black on white and never themed: a scanner works on contrast, and a dark-mode variant in the
 * palette's brown on near-black is exactly the "it doesn't scan" that costs an evening.
 *
 * @param description what a screen reader says. The default names the URL; the WLAN code passes its own, so
 *   the password is not read out loud across the room.
 */
@Composable
internal fun GuestQrCode(url: String, modifier: Modifier = Modifier, description: String = "QR-Code für $url") {
    val matrix = remember(url) { GuestQr.matrix(url) }
    Canvas(modifier.semantics { contentDescription = description }) {
        drawRect(Color.White)
        val module = minOf(size.width, size.height) / matrix.size
        val path = Path()
        matrix.forEachIndexed { y, row ->
            row.forEachIndexed { x, dark ->
                if (dark) path.addRect(Rect(x * module, y * module, (x + 1) * module, (y + 1) * module))
            }
        }
        drawPath(path, Color.Black)
    }
}

/**
 * What a guest does while the tablet's own hotspot runs, in order: into the WLAN, mobile data off, order
 * page. Side by side or stacked, whichever leaves the codes bigger in the space there is.
 *
 * The middle step has no code and is the one that decides whether the last one works: the hotspot has no
 * internet, so a phone with mobile data on sends the request for the order page into the mobile network.
 * Android offers no dependable "stay connected" prompt for this (a Pixel 9 Pro shows none), hence a step of
 * its own instead of a footnote.
 *
 * Name and password stand under the first code in type: Android made them up, so the screen is the only
 * place a guest with a stubborn camera can read them.
 *
 * @param maxQr the largest a code gets; it shrinks to fit the width and, where the height is limited, that.
 * @param onZoom called with the code that was tapped; null leaves the codes untappable (the admin panel).
 */
@Composable
internal fun GuestWifiSteps(
    wifi: WifiAccess,
    url: String,
    maxQr: Dp,
    modifier: Modifier = Modifier,
    onZoom: ((GuestCode) -> Unit)? = null,
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // The codes take what the place leaves them: on the guest screen that is the height between the title
        // and the buttons, so nothing has to scroll; in the admin panel, which scrolls anyway, only the width.
        val height = if (constraints.hasBoundedHeight) maxHeight else Dp.Infinity
        val beside = minOf(maxQr, (maxWidth - DataStepWidth - StepGap * 2) / 2, height - StepChrome)
        val stacked = minOf(maxQr, maxWidth, (height - StepChrome * 2 - DataStepHeight - StepGap * 2) / 2)
        // Whichever arrangement gives the bigger code: a scanner cares about size, not about layout.
        if (beside >= stacked) {
            val qr = beside.coerceAtLeast(MinQr)
            Row(horizontalArrangement = Arrangement.spacedBy(StepGap), verticalAlignment = Alignment.Top) {
                WifiStep(wifi, qr, onZoom)
                DataStep(Modifier.width(DataStepWidth))
                OrderStep(url, qr, onZoom)
            }
        } else {
            val qr = stacked.coerceAtLeast(MinQr)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StepGap),
            ) {
                WifiStep(wifi, qr, onZoom)
                DataStep(Modifier.fillMaxWidth())
                OrderStep(url, qr, onZoom)
            }
        }
    }
}

private val StepGap = 16.dp
private val DataStepWidth = 140.dp

/** Roughly what the middle step needs when it spans the width: a title and two lines. */
private val DataStepHeight = 84.dp

/** Roughly what a step needs besides its code: the title above, two lines of type below. */
private val StepChrome = 100.dp

/** Below this a phone stops finding the code; rather let the place overflow than shrink further. */
private val MinQr = 132.dp

@Composable
private fun DataStep(modifier: Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "2. Mobile Daten aus",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            "Sonst sucht das Handy die Bestellseite im Mobilfunk statt in diesem WLAN.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WifiStep(wifi: WifiAccess, qrSize: Dp, onZoom: ((GuestCode) -> Unit)?) {
    GuestStep(
        title = WIFI_STEP_TITLE,
        content = GuestWifi.qr(wifi),
        description = wifiCodeDescription(wifi),
        qrSize = qrSize,
        onZoom = onZoom?.let { zoom -> { zoom(GuestCode.Wifi) } },
    ) {
        Text(wifi.ssid, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
        wifi.password?.takeIf { it.isNotEmpty() }?.let { password ->
            Text(
                "Passwort: $password",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun OrderStep(url: String, qrSize: Dp, onZoom: ((GuestCode) -> Unit)?) {
    GuestStep(
        title = ORDER_STEP_TITLE,
        content = url,
        description = "QR-Code für $url",
        qrSize = qrSize,
        onZoom = onZoom?.let { zoom -> { zoom(GuestCode.Order) } },
    ) {
        Text(
            "Erst nach Schritt 1 und 2 scannen.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GuestStep(
    title: String,
    content: String,
    description: String,
    qrSize: Dp,
    onZoom: (() -> Unit)?,
    below: @Composable () -> Unit,
) {
    Column(
        Modifier.width(qrSize),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        GuestQrCode(
            content,
            Modifier.size(qrSize).zoomOnTap(onZoom).background(Color.White, RoundedCornerShape(12.dp)).padding(8.dp),
            description,
        )
        below()
    }
}

/** The end of the three steps: what the guest may undo, and why the phone complains about this WLAN. */
internal const val HOTSPOT_PHONE_HINT =
    "Nach dem Bestellen können die mobilen Daten wieder an. Meldet das Handy „kein Internet“, stimmt das: " +
        "der Hotspot hat absichtlich keins."

// ─── Großansicht ────────────────────────────────────────────────────────────

/** Which of the guest screen's codes is meant; the content is looked up when drawn, so it is never stale. */
internal enum class GuestCode { Wifi, Order }

internal const val WIFI_STEP_TITLE = "1. Ins WLAN"
internal const val ORDER_STEP_TITLE = "3. Pizza bestellen"

internal fun wifiCodeDescription(wifi: WifiAccess) = "QR-Code für das WLAN ${wifi.ssid}"

/** Makes a code tappable where there is something to open; without [onZoom] the code stays plain. */
internal fun Modifier.zoomOnTap(onZoom: (() -> Unit)?): Modifier =
    if (onZoom == null) this else clickable(onClickLabel = "QR-Code groß anzeigen", onClick = onZoom)

/**
 * One code as large as the screen allows, over the dimmed guest screen.
 *
 * For the phone that won't focus on a small code and the guest who stands a step away. A tap anywhere —
 * beside the code or on it — or the back button closes it again; there is nothing else to do here, so there
 * is no button to look for.
 *
 * @param content what the code carries; [description] is what a screen reader says instead of it.
 */
@Composable
internal fun GuestQrZoom(title: String, content: String, description: String, onClose: () -> Unit) {
    // The platform's default width would cap the dialog at a card's size, which is the opposite of the point.
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    // No ripple across the whole screen for what is simply "put it away".
                    indication = null,
                    onClickLabel = "Schließen",
                    onClick = onClose,
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val side = minOf(maxWidth, maxHeight - ZoomTitleHeight)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // White on the dialog's scrim, like the code's own background: readable in both themes.
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
                Box(Modifier.size(side).background(Color.White, RoundedCornerShape(16.dp)).padding(12.dp)) {
                    GuestQrCode(content, Modifier.fillMaxSize(), description)
                }
            }
        }
    }
}

/** Room for the title above a zoomed code, spacing included. */
private val ZoomTitleHeight = 44.dp
