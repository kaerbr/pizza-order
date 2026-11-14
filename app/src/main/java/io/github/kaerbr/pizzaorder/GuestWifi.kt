package io.github.kaerbr.pizzaorder

/**
 * What a phone needs to join the tablet's own hotspot: the network's name and its password.
 *
 * Plain data, no Android: [GuestHotspot] fills it from what the system hands out, and the QR code, the card
 * and the tests read it.
 *
 * @param password null for an open network.
 * @param sae true for a WPA3-only network; WPA2 and the WPA2/WPA3 mix both go out as "WPA".
 */
data class WifiAccess(val ssid: String, val password: String?, val sae: Boolean = false)

/** The hotspot as a QR code: the camera app of a phone offers "mit WLAN verbinden" for it. */
object GuestWifi {

    /**
     * [access] in the `WIFI:` format phone cameras understand: `WIFI:T:WPA;S:name;P:password;;`.
     *
     * Name and password are escaped: a `;` in a password would otherwise end the field and the phone would
     * try half of it.
     */
    fun qr(access: WifiAccess): String {
        val password = access.password?.takeIf { it.isNotEmpty() }
        val type = when {
            password == null -> "nopass"
            access.sae -> "SAE"
            else -> "WPA"
        }
        val secret = password?.let { "P:${escape(it)};" }.orEmpty()
        return "WIFI:T:$type;S:${escape(access.ssid)};$secret;"
    }

    private fun escape(text: String): String = buildString {
        text.forEach { c ->
            if (c in SPECIAL) append('\\')
            append(c)
        }
    }

    private const val SPECIAL = "\\;,:\""
}
