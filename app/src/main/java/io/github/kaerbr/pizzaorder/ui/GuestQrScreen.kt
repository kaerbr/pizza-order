package io.github.kaerbr.pizzaorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.kaerbr.pizzaorder.GuestRuntime
import io.github.kaerbr.pizzaorder.GuestStatus
import io.github.kaerbr.pizzaorder.HotspotRuntime
import io.github.kaerbr.pizzaorder.HotspotStatus
import io.github.kaerbr.pizzaorder.LocalAddress
import io.github.kaerbr.pizzaorder.PrintStatus

/**
 * The guest QR code as its own screen: one tap in the quick menu, no pattern, no admin panel.
 *
 * The admin panel is the wrong home for this. The code is what a guest is supposed to find, and it was
 * three taps and a scroll away behind the lock. Nothing on this screen is secret — the code keeps phones
 * outside the party out, not the person standing at the tablet.
 *
 * @param hotspot the tablet's own WLAN. While it runs the screen shows two codes — WLAN first, order page
 *   last, "mobile Daten aus" between them — because a guest can't know a network Android made up a minute ago.
 * @param printStatus the state of the card print, so the button reports its own outcome like every other
 *   print button in the app.
 */
@Composable
fun GuestQrScreen(
    guest: GuestRuntime,
    hotspot: HotspotRuntime,
    printStatus: PrintStatus?,
    onPrintCard: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wifi = (hotspot.status as? HotspotStatus.Running)?.access
    val ip = rememberLocalIpv4(preferHotspot = wifi != null)
    // The same rules as the panel: a code without a listening server or without an address would point at
    // nobody, and that is worse than a sentence saying why.
    val url = ip?.takeIf { guest.status is GuestStatus.Running }?.let { LocalAddress.url(it, guest.port, guest.code) }

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // No scrolling: this is the screen a guest holds a phone up to, and a code half below the fold is a
        // code nobody scans. Title and buttons take what they need, the codes get the rest and size
        // themselves to it — on an 8.7" tablet as on a big one, upright or on its side.
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Gäste-Bestellung",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    guest.status is GuestStatus.Failed -> Message(
                        "Der Server läuft nicht: ${(guest.status as GuestStatus.Failed).message}",
                        error = true,
                    )
                    guest.status !is GuestStatus.Running -> Message(
                        "Die Gäste-Bestellung ist aus. Im Admin-Panel unter „Gäste-Bestellung“ einschalten, " +
                            "dann erscheint hier der QR-Code.",
                        error = false,
                    )
                    ip == null -> Message(
                        // A hotspot that was chosen and didn't come up is the more useful half of the story.
                        (hotspot.status as? HotspotStatus.Failed)?.let { "Kein eigener Hotspot: ${it.message}" }
                            ?: "Kein WLAN und kein Hotspot. Das Tablet hängt an keinem Netz, also kann kein Handy bestellen.",
                        error = true,
                    )
                    url != null && wifi != null -> GuestWifiSteps(wifi, url, maxQr = 340.dp, modifier = Modifier.fillMaxSize())
                    url != null -> BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        // Black on white whatever the theme: a dark-mode code in the palette's brown doesn't scan.
                        GuestQrCode(
                            url,
                            Modifier
                                .size(minOf(360.dp, maxWidth, maxHeight))
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                        )
                    }
                }
            }

            if (url != null) {
                // Chosen but not up: without this line the screen would show the one-code view and a guest
                // outside the house WLAN would scan a code that leads nowhere.
                when (val status = hotspot.status) {
                    HotspotStatus.Starting -> Note("Der Hotspot startet …")
                    is HotspotStatus.Failed -> Note("Kein eigener Hotspot: ${status.message}", error = true)
                    else -> Unit
                }
                GuestUrl(url)
                when (printStatus) {
                    is PrintStatus.Failed -> Note(printStatus.reason, error = true)
                    PrintStatus.Printed -> Note("Der Bon liegt im Drucker. Auf den Tisch legen, dann findet ihn jedes Handy.")
                    else -> Note(
                        if (wifi != null) HOTSPOT_PHONE_HINT
                        else "Handy draufhalten. Bleibt das Tablet in der Küche: den QR-Bon drucken und auf den Tisch legen.",
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (url != null) {
                    Button(
                        onClick = { onPrintCard(url) },
                        enabled = printStatus != PrintStatus.Printing,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(52.dp),
                    ) {
                        Icon(printStatus.icon, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(printStatus.label("QR-Bon drucken"))
                    }
                }
                OutlinedButton(onClick = onClose, shape = RoundedCornerShape(10.dp), modifier = Modifier.height(52.dp)) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Schließen")
                }
            }
        }
    }
}

/** One short line under the codes; kept small so the codes keep the room. */
@Composable
private fun Note(text: String, error: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Message(text: String, error: Boolean) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )
}
