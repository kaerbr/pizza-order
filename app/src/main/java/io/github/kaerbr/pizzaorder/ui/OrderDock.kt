package io.github.kaerbr.pizzaorder.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import io.github.kaerbr.pizzaorder.Diet
import io.github.kaerbr.pizzaorder.DoughStock
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

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

// One slow ease for every state change in the dock, so filling in the form never snaps.
private fun <T> ease(): FiniteAnimationSpec<T> = tween(durationMillis = 700, easing = FastOutSlowInEasing)

private val TrackHeight = 56.dp
private val KnobSize = 48.dp
private val TrackInset = 4.dp

/**
 * The dock as a pizza peel (design/dock.html, variant B): guests order by sliding their pizza into the
 * oven instead of tapping, so a stray touch at the party can't send a second ticket. Checks above the
 * track show what's still missing; pressing the pizza too early wiggles it and flashes that check.
 * Double tap and TalkBack's click also order, for anyone who can't drag.
 */
@Composable
internal fun PeelDock(order: DockOrder, number: Int, onSubmit: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    // The pointer handlers outlive recompositions: without this they'd submit the name as it was when they started.
    val submit by rememberUpdatedState(onSubmit)
    val complete by rememberUpdatedState(order.complete)

    val wiggle = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    fun nudge() {
        scope.launch { wiggle.animateTo(0f, keyframes { durationMillis = 450; 8f at 90; -4f at 180; 8f at 270; -4f at 360 }) }
        scope.launch { flash.animateTo(0f, keyframes { durationMillis = 1200; 1f at 200; 0f at 500; 1f at 700 }) }
    }
    // Only the first missing step flashes, so the eye goes straight to it.
    val missing = when {
        !order.hasName -> 0
        !order.hasSauce -> 1
        else -> 2
    }

    Column(modifier) {
        HorizontalDivider(color = colors.primary.copy(alpha = 0.4f))
        Surface(color = colors.primaryContainer) {
            Column(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        // Fades "· Vegan" → "· Vegetarisch (Käse)" as guests add toppings.
                        Crossfade(order.dietLabel(), animationSpec = ease(), label = "diet") { diet ->
                            Eyebrow("Bon #%03d".format(number) + (diet?.let { " · $it" } ?: ""), maxLines = 1)
                        }
                        Text(
                            when {
                                order.pizzaName == null -> "Noch keine Pizza gewählt"
                                !order.hasName -> order.pizzaName
                                else -> "${order.pizzaName} für ${order.customer.trim()}"
                            },
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    StepCheck("Name", order.hasName, if (missing == 0) flash.value else 0f)
                    StepCheck("Sauce", order.hasSauce, if (missing == 1) flash.value else 0f)
                    order.dough?.let { d ->
                        StepCheck(if (d.isSoldOut) "Ausverkauft" else "Teig ${d.available}", !d.isSoldOut && !d.isLow, if (missing == 2) flash.value else 0f)
                    }
                }

                PeelTrack(
                    order = order,
                    wiggle = wiggle.value,
                    onLocked = ::nudge,
                    onSubmit = { if (complete) submit() },
                )
            }
        }
    }
}

@Composable
private fun PeelTrack(order: DockOrder, wiggle: Float, onLocked: () -> Unit, onSubmit: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val complete by rememberUpdatedState(order.complete)
    val border by animateColorAsState(if (order.complete) colors.primary else colors.outline, ease(), label = "trackBorder")
    // Embers smoulder while locked, burn once the order is complete and flare as the pizza comes close.
    val glowBase by animateFloatAsState(if (order.complete) 0.7f else 0.3f, ease(), label = "ovenGlow")
    val fire = rememberInfiniteTransition(label = "fire")
    val flicker by fire.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "flicker")
    val shimmer by fire.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "shimmer")

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(TrackHeight)
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.5.dp, border, CircleShape),
    ) {
        // Knob and oven are the same size, each concentric with one round end of the track.
        // Knob rests left; at maxX it touches the oven, and past 85% of that letting go orders.
        // Knob position where it covers the oven exactly.
        val ovenX = with(density) { (maxWidth - KnobSize - TrackInset * 2).toPx() }
        val maxX = with(density) { (maxWidth - KnobSize * 2 - TrackInset * 3).toPx() }.coerceAtLeast(1f)
        val x = remember { Animatable(0f) }
        val knobScale = remember { Animatable(1f) }
        val knobAlpha = remember { Animatable(1f) }
        var armed by remember { mutableStateOf(false) }
        val progress = { (x.value / maxX).coerceIn(0f, 1f) }
        val fill = colors.primary

        fun slideIn() {
            scope.launch {
                // Into the oven's mouth: on to the oven centre, shrinking as it goes.
                launch { knobScale.animateTo(0.45f, tween(300, easing = FastOutSlowInEasing)) }
                x.animateTo(ovenX, tween(300, easing = FastOutSlowInEasing))
                knobAlpha.snapTo(0f)
                onSubmit()
                armed = false
                x.snapTo(0f)
                knobScale.snapTo(1f)
                knobAlpha.animateTo(1f, tween(400))
            }
        }

        // Peel fill trails the knob as a pill, so the track reads as "how far into the oven". It ends at the
        // knob's centre and fades in with the first few dp of drag: at rest it showed as a halo round the pizza.
        Box(
            Modifier.fillMaxSize().drawBehind {
                val w = x.value + TrackInset.toPx() + KnobSize.toPx() / 2
                drawRoundRect(
                    Brush.horizontalGradient(listOf(fill.copy(alpha = 0.15f), fill.copy(alpha = 0.45f)), endX = w),
                    size = Size(w, size.height),
                    cornerRadius = CornerRadius(size.height / 2),
                    alpha = (progress() * 6f).coerceAtMost(1f),
                )
            },
        )

        val label = when {
            order.soldOut -> "Kein Teig mehr"
            !order.hasName -> "Erst Namen eingeben"
            !order.hasSauce -> "Erst Sauce wählen"
            else -> "Pizza in den Ofen schieben  ›››"
        }
        val labelStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        val shimmerPx = with(density) { maxWidth.toPx() }
        Text(
            label,
            style = if (order.complete) labelStyle.copy(
                brush = Brush.linearGradient(
                    listOf(colors.onSurface, colors.primary, colors.onSurface),
                    start = Offset(shimmer * shimmerPx * 1.5f - shimmerPx * 0.5f - 80f, 0f),
                    end = Offset(shimmer * shimmerPx * 1.5f - shimmerPx * 0.5f + 80f, 0f),
                ),
            ) else labelStyle.copy(color = colors.onSurfaceVariant),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = KnobSize + TrackInset * 3)
                // Gets out of the way while the pizza slides over it.
                .graphicsLayer { alpha = 1f - (progress() * 1.6f).coerceAtMost(1f) },
        )

        Oven(
            glow = { (glowBase + progress() * 0.3f).coerceAtMost(1f) },
            flicker = flicker,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = TrackInset).size(KnobSize),
        )

        // The pizza on the peel.
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((x.value + TrackInset.toPx() + wiggle.dp.toPx()).roundToInt(), 0) }
                .graphicsLayer { alpha = knobAlpha.value; scaleX = knobScale.value; scaleY = knobScale.value }
                .size(KnobSize)
                .shadow(4.dp, CircleShape)
                .background(colors.surface, CircleShape)
                .draggable(
                    rememberDraggableState { delta ->
                        scope.launch {
                            x.snapTo((x.value + delta).coerceIn(0f, maxX))
                            // One tick when letting go would order, so it's felt, not just seen.
                            val past = x.value > maxX * 0.85f
                            if (past && !armed) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                            armed = past
                        }
                    },
                    Orientation.Horizontal,
                    enabled = order.complete,
                    onDragStopped = {
                        if (x.value > maxX * 0.85f) slideIn()
                        else { armed = false; x.animateTo(0f, spring(dampingRatio = 0.5f)) }
                    },
                )
                .pointerInput(ovenX) {
                    detectTapGestures(
                        onPress = { if (!complete) onLocked() },
                        onDoubleTap = { if (complete) slideIn() },
                    )
                }
                .semantics {
                    role = Role.Button
                    contentDescription = if (order.complete) "Bestellen: Pizza in den Ofen schieben" else label
                    onClick { if (complete) slideIn() else onLocked(); true }
                },
            contentAlignment = Alignment.Center,
        ) {
            MiniPizza(order, Modifier.size(KnobSize - 6.dp))
        }
    }
}

/** A round wood-fired oven seen from the front: dark dome, arched mouth with embers inside. */
@Composable
private fun Oven(glow: () -> Float, flicker: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2
        drawCircle(Brush.radialGradient(listOf(Color(0xFF4A3328), Color(0xFF261A15)), center, r), r)
        // Arched mouth, sitting low like a real oven door: straight sides, round top.
        val w = r * 1.2f
        val h = r * 0.95f
        val left = center.x - w / 2
        val floor = center.y + r * 0.5f
        val mouth = Path().apply {
            moveTo(left, floor)
            lineTo(left, floor - h + w / 2)
            arcTo(Rect(left, floor - h, left + w, floor - h + w), 180f, 180f, false)
            lineTo(left + w, floor)
            close()
        }
        drawPath(mouth, Color(0xFF140C09))
        clipPath(mouth) {
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFFFFD36A), Color(0xFFF0702A), Color(0x008B2A12)),
                    center = Offset(center.x, floor), radius = h * 1.15f * flicker,
                ),
                radius = h * 1.2f, center = Offset(center.x, floor), alpha = glow(),
            )
        }
    }
}

@Composable
private fun StepCheck(label: String, ok: Boolean, flash: Float) {
    val colors = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (ok) colors.tertiaryContainer else Color.Transparent, ease(), label = "checkBg")
    val ink by animateColorAsState(if (ok) colors.onTertiaryContainer else colors.onSurfaceVariant, ease(), label = "checkInk")
    val line by animateColorAsState(if (ok) Color.Transparent else colors.onSurfaceVariant, ease(), label = "checkLine")
    // flash 0..1 blends towards the error colour while a locked pizza gets pressed.
    val shape = CircleShape
    Row(
        Modifier
            .clip(shape)
            .background(lerp(bg, colors.error, flash), shape)
            .border(1.dp, lerp(line, colors.error, flash), shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (ok) Icon(Icons.Default.Check, null, Modifier.size(14.dp), tint = ink)
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.06.em,
            color = lerp(ink, colors.onError, flash),
            maxLines = 1,
        )
    }
}

// Known toppings get their own colour; anything added in Settings falls back to a neutral brown.
private val ToppingColors = mapOf(
    "Salami" to Color(0xFFA9352A),
    "Käse" to Color(0xFFF7E3A3),
    "Rucola" to Color(0xFF4F8A35),
    "Oliven" to Color(0xFF2B2622),
    "Kirschtomaten" to Color(0xFFD8452F),
    "Zwiebeln" to Color(0xFFC9A3D6),
    "Knoblauch" to Color(0xFFF6EFDF),
    "Pinienkerne" to Color(0xFFEAD6A3),
)

/** The order as a tiny pizza: crust, sauce (green for pesto, red otherwise), a few dots per topping. */
@Composable
private fun MiniPizza(order: DockOrder, modifier: Modifier = Modifier) {
    val empty = order.sauce.isBlank() && order.toppings.isEmpty()
    val outline = MaterialTheme.colorScheme.outline
    Canvas(modifier) {
        val r = size.minDimension / 2
        if (empty) {
            drawCircle(outline, radius = r * 0.92f, style = Stroke(1.5.dp.toPx()))
            return@Canvas
        }
        drawCircle(Color(0xFFE3B877), radius = r)
        drawCircle(Color(0xFFF1DCB2), radius = r * 0.84f)
        if (order.sauce.isNotBlank()) {
            drawCircle(if ("pesto" in order.sauce.lowercase()) Color(0xFF5F8D3A) else Color(0xFFC8432A), radius = r * 0.8f)
        }
        order.toppings.forEach { t ->
            // Seeded by name, so the dots stay put while other toppings come and go.
            val rnd = Random(t.hashCode())
            repeat(5) {
                val a = rnd.nextFloat() * 2 * Math.PI.toFloat()
                val d = sqrt(rnd.nextFloat()) * r * 0.62f
                drawCircle(
                    ToppingColors[t] ?: Color(0xFF8A7A6A),
                    radius = r * 0.11f,
                    center = center + Offset(cos(a) * d, sin(a) * d),
                )
            }
        }
    }
}
