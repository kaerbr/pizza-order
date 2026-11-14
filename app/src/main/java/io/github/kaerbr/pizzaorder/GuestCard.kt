package io.github.kaerbr.pizzaorder

/**
 * The card that lies on the table: a QR code a guest scans to open the order page, and the address and join
 * code in type, for a phone whose camera refuses.
 *
 * Printed through the same [PrintStyle] pipeline — DantSu markup, 32 characters per line — so it goes out of
 * the same printer as an order slip and [Printout] can draw it on screen unchanged. Nothing here comes from a
 * guest: the address, the port and the code are the admin's, and the hotspot's name and password are
 * Android's.
 */
object GuestCard {

    /**
     * @param url what the QR carries, i.e. [LocalAddress.url]. Printed as QR only; the parts go on paper
     *   separately, because the full URL wraps and nobody types a `?k=` by hand.
     * @param wifi the tablet's own hotspot, when it runs. The card then has two codes, in the order a guest
     *   needs them: into the WLAN first, to the order page last. Between them stands the step that has no
     *   code: mobile data off, or the phone looks for the page in the mobile network. Without it the card is the one it always
     *   was — the guests are in a WLAN they already know.
     */
    fun format(url: String, wifi: WifiAccess? = null): String {
        val withoutScheme = url.removePrefix("http://")
        val address = withoutScheme.substringBefore('/')
        val code = withoutScheme.substringAfter("?k=", "")
        val hotspot = wifi?.takeIf(::printable)
        return if (hotspot == null) plain(url, address, code) else withWifi(url, address, code, hotspot)
    }

    private fun plain(url: String, address: String, code: String): String = """
            |[C]<b>P I Z Z A   P A R T Y</b>
            |[C]Hier bestellen
            |[C]================================
            |[C]
            |[C]<qrcode size='40'>$url</qrcode>
            |[C]
            |[C]<b>Handy draufhalten</b>
            |[C]und im WLAN die Pizza wählen.
            |[C]================================
            |[C]Kein Scanner? Selbst eintippen:
            |[L]Adresse[R]$address
            |[L]Code[R]<b>$code</b>
            |[C]
            |[C]Gilt bis zum neuen Code.
            |[C]
            |[L]
            |[L]
            |[L]
        """.trimMargin()

    private fun withWifi(url: String, address: String, code: String, wifi: WifiAccess): String = """
            |[C]<b>P I Z Z A   P A R T Y</b>
            |[C]In drei Schritten bestellen
            |[C]================================
            |[C]<b>1. Ins WLAN</b>
            |[C]
            |[C]<qrcode size='40'>${GuestWifi.qr(wifi)}</qrcode>
            |[C]
            |[C]Handy draufhalten, verbinden.
            |${pair("WLAN", wifi.ssid)}
            |${wifi.password?.takeIf { it.isNotEmpty() }?.let { pair("Passwort", it) } ?: "[C]Ohne Passwort"}
            |[C]================================
            |[C]<b>2. Mobile Daten aus</b>
            |[C]Das WLAN hat kein Internet.
            |[C]Sonst sucht das Handy die
            |[C]Seite im Mobilfunk.
            |[C]================================
            |[C]<b>3. Pizza bestellen</b>
            |[C]
            |[C]<qrcode size='40'>$url</qrcode>
            |[C]
            |[C]Handy noch einmal draufhalten.
            |[L]Adresse[R]$address
            |[L]Code[R]<b>$code</b>
            |[C]================================
            |[C]Danach mobile Daten wieder an.
            |[C]Gilt, solange der Hotspot läuft.
            |[C]
            |[L]
            |[L]
            |[L]
        """.trimMargin()

    /** Label left, value right — or the value on its own line when both don't fit the paper's 32 characters. */
    private fun pair(label: String, value: String): String =
        if (label.length + value.length < Printout.LINE_CHARS) "[L]$label[R]<b>$value</b>"
        else "[L]$label\n[R]<b>$value</b>"

    /**
     * Name and password come from Android, not from the admin. Anything that would read as markup to the
     * printer keeps the hotspot off the paper rather than garbling the card; the screen still shows the code.
     */
    private fun printable(wifi: WifiAccess): Boolean =
        (wifi.ssid + wifi.password.orEmpty()).none { it in "<>[]\n\r" }
}
