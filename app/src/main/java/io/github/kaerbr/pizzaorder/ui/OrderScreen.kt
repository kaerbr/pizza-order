package io.github.kaerbr.pizzaorder.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.kaerbr.pizzaorder.Diet
import io.github.kaerbr.pizzaorder.MenuConfig
import io.github.kaerbr.pizzaorder.Pizza
import io.github.kaerbr.pizzaorder.Sauce
import io.github.kaerbr.pizzaorder.Topping

private const val CUSTOM_NAME = "Eigenkreation"
private const val NOTE_MAX = 160

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrderScreen(
    menu: MenuConfig,
    onSubmit: (customer: String, pizza: Pizza, note: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var customer by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    // CRITICAL: We store simple names to satisfy rememberSaveable (must be Parcelable/Primitive)
    // Starts empty: guests pick a pizza themselves instead of finding the first preset ticked.
    var sauceName by rememberSaveable { mutableStateOf("") }
    var toppingNames by rememberSaveable { mutableStateOf(emptyList<String>()) }
    // Tapping Eigenkreation yields the same empty pizza as "nothing chosen yet"; this tells them apart.
    var customPicked by rememberSaveable { mutableStateOf(false) }

    // Reconstruct objects for logic
    val sauce = remember(sauceName) { Sauce(sauceName) }
    val toppings = remember(toppingNames) { Pizza.toppingsOf(toppingNames) }

    val nothingChosen = sauceName.isBlank() && toppingNames.isEmpty() && !customPicked
    // null = no card selected yet.
    val pizzaName = remember(sauce, toppings, nothingChosen) {
        menu.presets.find { it.sauce == sauce && it.toppings.toSet() == toppings.toSet() }?.name
            ?: CUSTOM_NAME.takeUnless { nothingChosen }
    }
    val current = Pizza(pizzaName ?: CUSTOM_NAME, sauce, toppings)
    // No diet for an empty pizza: a bare crust would read as "Vegan" before anything is picked.
    val diet = if (nothingChosen) Diet.NONE else menu.dietOf(current)

    Column(modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Spacing tuned so the whole form fits one portrait screen of the 8.7" party tablet (~600×1000dp).
            Column(Modifier.widthIn(max = 920.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Section("01", "Dein Name") {
                    OutlinedTextField(
                        value = customer,
                        onValueChange = { customer = it },
                        placeholder = { Text("z.B. Anna") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        textStyle = MaterialTheme.typography.titleLarge,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Section("02", "Pizza wählen") {
                    // Eigenkreation only shows a diet while it's the one being built.
                    val tiles = menu.presets.map { Triple(it.name, it.description, menu.dietOf(it)) } +
                        Triple(CUSTOM_NAME, "Sauce und Belag frei wählen", if (pizzaName == CUSTOM_NAME) diet else Diet.NONE)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tiles.chunked(3).forEach { row ->
                            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (name, desc, tileDiet) ->
                                    PizzaCard(name, desc, tileDiet, pizzaName == name, Modifier.weight(1f).fillMaxHeight()) {
                                        menu.presets.find { it.name == name }?.let { p ->
                                            sauceName = p.sauce.name; toppingNames = p.toppings.map { it.name }
                                        } ?: run { sauceName = ""; toppingNames = emptyList(); customPicked = true } // Eigenkreation: start empty
                                    }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }

                Section("03", "Sauce") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        menu.sauces.forEach { s ->
                            Pill(s.name, s.name == sauceName, diet = menu.dietOf(s.name)) { sauceName = s.name }
                        }
                    }
                }

                Section("04", "Belag", trailing = { DietLegend() }) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        menu.toppings.forEach { t ->
                            val selected = t.name in toppingNames
                            Pill(t.name, selected, if (selected) Icons.Default.Check else Icons.Default.Add, menu.dietOf(t.name)) {
                                toppingNames = if (selected) toppingNames - t.name else toppingNames + t.name
                            }
                        }
                    }
                }

                // Counter sits in the header row: as supportingText it cost a line under the field.
                Section("05", "Notiz für die Küche", trailing = {
                    Text("${note.length} / $NOTE_MAX", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(NOTE_MAX) },
                        placeholder = { Text("z.B. extra knusprig, ohne Oregano…") },
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // Dock: order summary + submit, pinned above the tab bar.
        HorizontalDivider()
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    // Fades "· Vegan" → "· Vegetarisch (Käse)" as guests add toppings, same ease as the button.
                    val dietSuffix = when (diet) {
                        Diet.VEGAN -> " · ${diet.label}"
                        Diet.VEGETARIAN -> " · ${diet.label} (${menu.dietCulprits(current).joinToString()})"
                        Diet.NONE -> ""
                    }
                    // Dough stock rides in the eyebrow line instead of a third line under the summary.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Crossfade(
                            dietSuffix, animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing), label = "diet",
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            Eyebrow("Deine Bestellung$it", maxLines = 1)
                        }
                        menu.dough?.let { d ->
                            Eyebrow(
                                if (d.isSoldOut) " · Ausverkauft" else " · Noch ${d.available} ${if (d.available == 1) "Pizza" else "Pizzen"}",
                                maxLines = 1,
                                color = if (d.isSoldOut || d.isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        when {
                            pizzaName == null -> "Noch keine Pizza gewählt"
                            customer.isBlank() -> pizzaName
                            else -> "$pizzaName für ${customer.trim()}"
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                // Required steps: name and sauce (no toppings is fine, Marinara-style).
                // The button fills from the left as they get done and only enables when all are.
                val steps = listOf(customer.isNotBlank(), sauceName.isNotBlank())
                val soldOut = menu.dough?.isSoldOut == true
                val complete = steps.all { it } && !soldOut
                // One slow ease for fill, colour and text so enabling doesn't snap: the button is always
                // transparent and everything visible is this animated fill.
                val ease = tween<Float>(durationMillis = 700, easing = FastOutSlowInEasing)
                val progress by animateFloatAsState(steps.count { it } / steps.size.toFloat(), ease, label = "progress")
                val fillAlpha by animateFloatAsState(if (complete) 1f else 0.35f, ease, label = "fillAlpha")
                val contentColor by animateColorAsState(
                    if (complete) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    tween(durationMillis = 700, easing = FastOutSlowInEasing), label = "content",
                )
                val shape = RoundedCornerShape(14.dp)
                val fill = MaterialTheme.colorScheme.primary
                Button(
                    onClick = {
                        onSubmit(customer, current, note)
                        customer = ""; note = ""
                        sauceName = ""; toppingNames = emptyList(); customPicked = false
                    },
                    enabled = complete,
                    shape = shape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent, contentColor = contentColor,
                        disabledContainerColor = Color.Transparent, disabledContentColor = contentColor,
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp).widthIn(min = 260.dp)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .drawBehind { drawRect(fill.copy(alpha = fillAlpha), size = size.copy(width = size.width * progress)) },
                ) {
                    Icon(Icons.Default.Print, null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        when {
                            soldOut -> "Kein Teig mehr"
                            customer.isBlank() -> "Erst Namen eingeben"
                            sauceName.isBlank() -> "Erst Sauce wählen"
                            else -> "Bestellen & drucken"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
internal fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) = Text(
    text.uppercase(),
    style = MaterialTheme.typography.labelMedium,
    color = color,
    fontWeight = FontWeight.SemiBold,
    maxLines = maxLines,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier,
)

// Filled leaf = vegan, outlined = vegetarian, nothing for the rest: the missing leaf is the signal.
internal val Diet.icon: ImageVector?
    get() = when (this) {
        Diet.VEGAN -> Icons.Filled.Eco
        Diet.VEGETARIAN -> Icons.Outlined.Eco
        Diet.NONE -> null
    }

/** The `.diet` pill from design/index.html; renders nothing for [Diet.NONE]. */
@Composable
internal fun DietBadge(diet: Diet, modifier: Modifier = Modifier) {
    val icon = diet.icon ?: return
    Row(
        modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
        Text(diet.label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, letterSpacing = 0.09.em)
    }
}

@Composable
private fun DietLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(Diet.VEGAN, Diet.VEGETARIAN).forEach { d ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(d.icon!!, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(d.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    errorContainerColor = MaterialTheme.colorScheme.surface,
)

@Composable
private fun Section(step: String, title: String, trailing: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(step, style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize), modifier = Modifier.weight(1f))
            trailing()
        }
        content()
    }
}

@Composable
private fun PizzaCard(name: String, description: String, diet: Diet, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outline),
    ) {
        // Name with diet leaf on top, ingredients below (wrapping as needed). The separate badge row made
        // every card row ~50dp taller on the 8.7" tablet. Selection shows as border and fill, like the chips.
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Shrinks instead of wrapping or "…": at three columns "Eigenkreation" used to break mid-word.
                val nameStyle = MaterialTheme.typography.headlineSmall.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize * 1.15f)
                BasicText(
                    name,
                    style = nameStyle.copy(color = colors.onSurface),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = nameStyle.fontSize),
                    modifier = Modifier.weight(1f, fill = false),
                )
                diet.icon?.let { Icon(it, diet.label, Modifier.size(16.dp), tint = colors.onTertiaryContainer) }
            }
            Text(description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, icon: ImageVector? = null, diet: Diet = Diet.NONE, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = icon?.let { { Icon(it, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) } },
        trailingIcon = diet.icon?.let { { Icon(it, diet.label, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer) } },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true, selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier.heightIn(min = 40.dp),
    )
}
