package io.github.kaerbr.pizzaorder.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.kaerbr.pizzaorder.Diet
import io.github.kaerbr.pizzaorder.DoughStock
import kotlinx.coroutines.launch

/** What the dock shows, worked out once by OrderScreen from the form state. */
internal data class DockOrder(
    val customer: String,
    /** null = no pizza card picked yet. */
    val pizzaName: String?,
    val sauce: String,
    val toppings: List<String>,
    val diet: Diet,
    val dietCulprits: List<String>,
    val dough: DoughStock?,
) {
    // Required steps: name and sauce (no toppings is fine, Marinara-style).
    val hasName get() = customer.isNotBlank()
    val hasSauce get() = sauce.isNotBlank()
    val soldOut get() = dough?.isSoldOut == true
    val complete get() = hasName && hasSauce && !soldOut
}

private fun DockOrder.dietLabel(): String? = when (diet) {
    Diet.VEGAN -> diet.label
    Diet.VEGETARIAN -> "${diet.label} (${dietCulprits.joinToString()})"
    Diet.NONE -> null
}

private fun DoughStock.label() = if (isSoldOut) "Ausverkauft" else "Noch $available ${if (available == 1) "Pizza" else "Pizzen"}"

// One slow ease for every state change in the dock, so filling in the form never snaps.
private fun <T> ease(): FiniteAnimationSpec<T> = tween(durationMillis = 700, easing = FastOutSlowInEasing)

/**
 * The dock as the ticket that's about to print (design/dock.html, variant A): torn-off top edge,
 * monospace lines with dot leaders like the menu, and whatever is still missing in red right on the
 * ticket. The button is a round stamp that goes from dashed outline to solid once the order is complete.
 */
@Composable
internal fun BonDock(order: DockOrder, number: Int, onSubmit: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = remember { TicketEdge(radius = 6.dp, pitch = 18.dp) }
    // After each order a fresh blank ticket feeds up from below, like the next one off the roll.
    val feed = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Row(
        modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = feed.value * size.height }
            .shadow(10.dp, shape)
            .background(colors.surface, shape)
            .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Fades "· Vegan" → "· Vegetarisch (Käse)" as guests add toppings.
                Crossfade(order.dietLabel(), animationSpec = ease(), label = "diet", modifier = Modifier.weight(1f)) { diet ->
                    Eyebrow("Bon #%03d".format(number) + (diet?.let { " · $it" } ?: ""), maxLines = 1)
                }
                order.dough?.let { d ->
                    Eyebrow(d.label(), maxLines = 1, color = if (d.isSoldOut || d.isLow) colors.error else colors.onSurfaceVariant)
                }
            }
            DottedRule(colors.outline, Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    BonLine("Für", order.customer.trim().ifEmpty { "fehlt" }, if (order.hasName) Line.Filled else Line.Missing)
                    BonLine("Sauce", order.sauce.ifEmpty { "fehlt" }, if (order.hasSauce) Line.Filled else Line.Missing)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    BonLine("Pizza", order.pizzaName ?: "noch offen", if (order.pizzaName != null) Line.Filled else Line.Optional)
                    BonLine("Belag", order.toppings.joinToString().ifEmpty { "ohne" }, if (order.toppings.isNotEmpty()) Line.Filled else Line.Optional)
                }
            }
        }
        Stamp(order) {
            onSubmit()
            scope.launch {
                feed.snapTo(1f)
                feed.animateTo(0f, tween(durationMillis = 450, easing = FastOutSlowInEasing))
            }
        }
    }
}

private enum class Line { Filled, Missing, Optional }

private val BonStyle @Composable get() = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)

@Composable
private fun BonLine(key: String, value: String, line: Line) {
    val colors = MaterialTheme.colorScheme
    val valueColor by animateColorAsState(
        when (line) {
            Line.Filled -> colors.onSurface
            Line.Missing -> colors.error
            Line.Optional -> colors.onSurfaceVariant
        },
        ease(), label = "bonLine",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(key, style = BonStyle, color = colors.onSurfaceVariant, maxLines = 1)
        // Dot leader: takes what's left after key and value, so a long value ellipsizes before the dots vanish.
        DottedRule(colors.outline, Modifier.weight(1f).padding(horizontal = 6.dp))
        Text(
            value,
            style = BonStyle,
            color = valueColor,
            fontWeight = if (line == Line.Filled) FontWeight.SemiBold else FontWeight.Normal,
            fontStyle = if (line == Line.Missing) FontStyle.Italic else FontStyle.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DottedRule(color: Color, modifier: Modifier = Modifier) {
    Spacer(
        modifier.height(2.dp).drawBehind {
            drawLine(
                color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                strokeWidth = size.height, cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 4.dp.toPx())),
            )
        },
    )
}

@Composable
private fun Stamp(order: DockOrder, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill by animateColorAsState(if (order.complete) colors.primary else Color.Transparent, ease(), label = "stampFill")
    val ink by animateColorAsState(if (order.complete) colors.onPrimary else colors.onSurfaceVariant, ease(), label = "stampInk")
    val tilt by animateFloatAsState(if (order.complete) -4f else -8f, ease(), label = "stampTilt")
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, spring(dampingRatio = 0.4f), label = "stampPress")
    val ring = colors.outline
    val complete = order.complete

    Box(
        Modifier
            .size(88.dp)
            .graphicsLayer { rotationZ = tilt; scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .clickable(interaction, ripple(), enabled = complete, role = Role.Button, onClick = onClick)
            .drawBehind {
                drawCircle(fill)
                if (complete) {
                    // Inner ring gives the rubber-stamp double edge.
                    drawCircle(ink, radius = size.minDimension / 2 - 5.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                } else {
                    val stroke = 2.dp.toPx()
                    drawCircle(
                        ring, radius = (size.minDimension - stroke) / 2,
                        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.Default.Print, null, Modifier.size(22.dp), tint = ink)
            Text(
                when {
                    order.soldOut -> "Kein Teig"
                    complete -> "Ab in den Ofen"
                    else -> "Noch nicht"
                }.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.06.em,
                lineHeight = 13.sp,
                textAlign = TextAlign.Center,
                color = ink,
            )
        }
    }
}

/** Top edge with half-round notches, like a ticket torn off the printer roll. */
private class TicketEdge(private val radius: Dp, private val pitch: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { radius.toPx() }
        val step = with(density) { pitch.toPx() }
        val path = Path().apply {
            moveTo(0f, 0f)
            var cx = step / 2
            while (cx + r <= size.width) {
                lineTo(cx - r, 0f)
                // Negative sweep from the left point runs through the bottom: the notch bites into the paper.
                arcTo(Rect(cx - r, -r, cx + r, r), 180f, -180f, false)
                cx += step
            }
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}
