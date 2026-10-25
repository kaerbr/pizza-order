package com.example.pizzaorder.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Print
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pizzaorder.MenuConfig
import com.example.pizzaorder.Pizza
import com.example.pizzaorder.Sauce
import com.example.pizzaorder.Topping

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

    val firstPreset = menu.presets.firstOrNull()
    // CRITICAL: We store simple names to satisfy rememberSaveable (must be Parcelable/Primitive)
    var sauceName by rememberSaveable { mutableStateOf(firstPreset?.sauce?.name ?: menu.sauces.firstOrNull()?.name ?: "Tomate") }
    var toppingNames by rememberSaveable { mutableStateOf(firstPreset?.toppings?.map { it.name } ?: emptyList<String>()) }

    // Reconstruct objects for logic
    val sauce = remember(sauceName) { Sauce(sauceName) }
    val toppings = remember(toppingNames) { Pizza.toppingsOf(toppingNames) }

    val pizzaName = remember(sauce, toppings) {
        menu.presets.find { it.sauce == sauce && it.toppings.toSet() == toppings.toSet() }?.name ?: CUSTOM_NAME
    }

    Column(modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 920.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
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
                    val tiles = menu.presets.map { it.name to it.description } + (CUSTOM_NAME to "Sauce und Belag frei wählen")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        tiles.chunked(3).forEach { row ->
                            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { (name, desc) ->
                                    PizzaCard(name, desc, pizzaName == name, Modifier.weight(1f).fillMaxHeight()) {
                                        menu.presets.find { it.name == name }?.let { p ->
                                            sauceName = p.sauce.name; toppingNames = p.toppings.map { it.name }
                                        } ?: run { sauceName = ""; toppingNames = emptyList() } // Eigenkreation: start empty
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
                            Pill(s.name, s.name == sauceName) { sauceName = s.name }
                        }
                    }
                }

                Section("04", "Belag") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        menu.toppings.forEach { t ->
                            val selected = t.name in toppingNames
                            Pill(t.name, selected, if (selected) Icons.Default.Check else Icons.Default.Add) {
                                toppingNames = if (selected) toppingNames - t.name else toppingNames + t.name
                            }
                        }
                    }
                }

                Section("05", "Notiz für die Küche") {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(NOTE_MAX) },
                        placeholder = { Text("z.B. extra knusprig, ohne Oregano…") },
                        minLines = 3,
                        supportingText = { Text("${note.length} / $NOTE_MAX", Modifier.fillMaxWidth(), textAlign = TextAlign.End) },
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
                Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Eyebrow("Deine Bestellung")
                    Text(
                        if (customer.isBlank()) pizzaName else "$pizzaName für ${customer.trim()}",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                // Required steps: name and sauce (no toppings is fine, Marinara-style).
                // The button fills from the left as they get done and only enables when all are.
                val steps = listOf(customer.isNotBlank(), sauceName.isNotBlank())
                val complete = steps.all { it }
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
                        onSubmit(customer, Pizza(pizzaName, sauce, toppings), note)
                        customer = ""; note = ""
                        menu.presets.firstOrNull()?.let { p ->
                            sauceName = p.sauce.name; toppingNames = p.toppings.map { it.name }
                        }
                    },
                    enabled = complete,
                    shape = shape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent, contentColor = contentColor,
                        disabledContainerColor = Color.Transparent, disabledContentColor = contentColor,
                    ),
                    modifier = Modifier
                        .heightIn(min = 56.dp).widthIn(min = 280.dp)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .drawBehind { drawRect(fill.copy(alpha = fillAlpha), size = size.copy(width = size.width * progress)) },
                ) {
                    Icon(Icons.Default.Print, null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        when {
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
internal fun Eyebrow(text: String, modifier: Modifier = Modifier) = Text(
    text.uppercase(),
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontWeight = FontWeight.SemiBold,
    modifier = modifier,
)

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    errorContainerColor = MaterialTheme.colorScheme.surface,
)

@Composable
private fun Section(step: String, title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(step, style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.headlineSmall)
        }
        content()
    }
}

@Composable
private fun PizzaCard(name: String, description: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outline),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(name, style = MaterialTheme.typography.headlineSmall.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize))
            Text(description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Surface(
                shape = CircleShape,
                color = if (selected) colors.primary else colors.surface,
                border = if (selected) null else BorderStroke(1.5.dp, colors.outline),
                modifier = Modifier.align(Alignment.End).size(22.dp),
            ) {
                if (selected) Icon(Icons.Default.Check, null, tint = colors.onPrimary, modifier = Modifier.padding(4.dp))
            }
        }
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = icon?.let { { Icon(it, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) } },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true, selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier.heightIn(min = 44.dp),
    )
}
