package io.github.kaerbr.pizzaorder.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.kaerbr.pizzaorder.Order
import io.github.kaerbr.pizzaorder.PrintStatus
import kotlinx.coroutines.delay

// Thermal paper stays paper in every palette and in dark mode: it's the printout, not app chrome.
private val Paper = Color(0xFFFFFDF7)
private val Ink = Color(0xFF1C1B1A)
private val InkMuted = Color(0xFF6B6862)
private val InkError = Color(0xFFB3261E)

/** How long the slip stays after a successful print before it rolls back up. */
private const val SHOW_AFTER_PRINT_MS = 2_500L

/** Button text for a print in flight, so every print button reports its own outcome in place. */
internal fun PrintStatus?.label(idle: String) = when (this) {
    PrintStatus.Printing -> "Druckt…"
    PrintStatus.Printed -> "Gedruckt"
    is PrintStatus.Failed -> "Nochmal drucken"
    null -> idle
}

internal val PrintStatus?.icon: ImageVector
    get() = when (this) {
        PrintStatus.Printed -> Icons.Default.Check
        is PrintStatus.Failed -> Icons.Default.Warning
        else -> Icons.Default.Print
    }

/**
 * The one confirmation in the app: after "Bestellen & drucken" a paper slip feeds down from the top
 * edge, like the printer's, with the order number big enough to call out across the room. It rolls
 * back up on its own once the printer is done; a failed print keeps it up until tapped.
 */
@Composable
fun OrderConfirmation(order: Order?, printStatus: PrintStatus?, onDismiss: () -> Unit) {
    // Keep the last order around so the slip still has content while it slides away.
    var shown by remember { mutableStateOf(order) }
    if (order != null) shown = order

    val haptics = LocalHapticFeedback.current
    LaunchedEffect(order?.id) {
        if (order != null) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    LaunchedEffect(order?.id, printStatus) {
        if (order != null && printStatus == PrintStatus.Printed) {
            delay(SHOW_AFTER_PRINT_MS)
            onDismiss()
        }
    }

    AnimatedVisibility(order != null, enter = fadeIn(tween(250)), exit = fadeOut(tween(350))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            order != null,
            // Slow linear-ish feed in, quick roll back up: it should read as paper, not as a dialog.
            enter = slideInVertically(tween(900, easing = LinearOutSlowInEasing)) { -it },
            exit = slideOutVertically(tween(350)) { -it } + fadeOut(tween(350)),
        ) {
            shown?.let { Slip(it, printStatus, onDismiss) }
        }
    }
}

@Composable
private fun Slip(order: Order, printStatus: PrintStatus?, onDismiss: () -> Unit) {
    Column(
        Modifier
            .width(380.dp)
            .shadow(16.dp, TornEdge)
            .background(Paper, TornEdge)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .padding(start = 28.dp, end = 28.dp, top = 36.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "BESTELLUNG AUFGENOMMEN",
            style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace,
            letterSpacing = 0.18.em, color = InkMuted,
        )
        Text(
            order.formattedNumber,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 72.sp, lineHeight = 76.sp,
            color = Ink,
        )
        Text(order.customer, style = MaterialTheme.typography.headlineMedium, color = Ink, textAlign = TextAlign.Center)

        DashedRule()
        Text(order.pizza.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(order.pizza.description, style = MaterialTheme.typography.bodyMedium, color = InkMuted, textAlign = TextAlign.Center)
        if (order.note.isNotBlank()) {
            Text("„${order.note}“", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = InkMuted, textAlign = TextAlign.Center)
        }
        DashedRule()

        PrintLine(printStatus)
    }
}

@Composable
private fun PrintLine(status: PrintStatus?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (status) {
            PrintStatus.Printed -> Icon(Icons.Default.Check, null, Modifier.size(20.dp), tint = Ink)
            is PrintStatus.Failed -> Icon(Icons.Default.Warning, null, Modifier.size(20.dp), tint = InkError)
            else -> CircularProgressIndicator(Modifier.size(16.dp), color = Ink, strokeWidth = 2.dp)
        }
        Text(
            when (status) {
                PrintStatus.Printed -> "Bon ist raus – guten Appetit!"
                is PrintStatus.Failed -> "Druck fehlgeschlagen"
                else -> "Bon wird gedruckt…"
            },
            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            color = if (status is PrintStatus.Failed) InkError else Ink,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
    if (status is PrintStatus.Failed) {
        Text(
            "${status.reason}\nUnter „Bestellungen“ nachdrucken. Tippen zum Schließen.",
            style = MaterialTheme.typography.bodySmall, color = InkMuted, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DashedRule() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(1.dp)
            .drawBehind {
                drawLine(
                    InkMuted, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                )
            },
    )
}

/** Straight top (it comes out of the screen edge), sawtooth bottom like a slip torn off the printer. */
private val TornEdge = GenericShape { size, _ ->
    val tooth = size.width / 27
    val depth = tooth / 2
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - depth)
    var x = size.width
    var down = true
    while (x > 0f) {
        x = (x - tooth / 2).coerceAtLeast(0f)
        lineTo(x, if (down) size.height else size.height - depth)
        down = !down
    }
    close()
}
