package com.example.pizzaorder.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.pizzaorder.MenuConfig
import com.example.pizzaorder.Pizza

private const val CUSTOM_NAME = "Eigenkreation"

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
    var sauce by rememberSaveable { mutableStateOf(firstPreset?.sauce ?: menu.sauces.firstOrNull().orEmpty()) }
    var toppings by rememberSaveable { mutableStateOf(firstPreset?.toppings ?: emptyList()) }

    val pizzaName = remember(sauce, toppings) {
        menu.presets.find { it.sauce == sauce && it.toppings.toSet() == toppings.toSet() }?.name ?: CUSTOM_NAME
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .widthIn(max = 720.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = customer,
            onValueChange = { customer = it },
            label = { Text("Dein Name") },
            placeholder = { Text("z.B. Anna") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            leadingIcon = { Icon(Icons.Default.Person, null) },
            textStyle = MaterialTheme.typography.titleLarge,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        SectionHeader("Pizza")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            menu.presets.forEach { p ->
                PizzaTile(p.name, pizzaName == p.name) {
                    sauce = p.sauce; toppings = p.toppings
                }
            }
            PizzaTile(CUSTOM_NAME, pizzaName == CUSTOM_NAME, Icons.Default.Add) { /* updates via derived name */ }
        }

        SectionHeader("Sauce")
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                menu.sauces.forEach { s ->
                    FilterChip(
                        selected = s == sauce,
                        onClick = { sauce = s },
                        label = { Text(s) },
                    )
                }
            }
        }

        SectionHeader("Belag")
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                menu.toppings.forEach { t ->
                    FilterChip(
                        selected = t in toppings,
                        onClick = {
                            toppings = if (t in toppings) toppings - t else toppings + t
                        },
                        label = { Text(t) },
                    )
                }
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Notiz (optional)") },
            placeholder = { Text("z.B. ohne Oregano") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                onSubmit(customer, Pizza(pizzaName, sauce, toppings), note)
                customer = ""; note = ""
                menu.presets.firstOrNull()?.let { p ->
                    sauce = p.sauce; toppings = p.toppings
                }
            },
            enabled = customer.isNotBlank() && sauce.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) {
            Icon(Icons.Default.Check, null)
            Spacer(Modifier.width(12.dp))
            Text("Bestellen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeader(text: String) = Text(
    text.uppercase(),
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontWeight = FontWeight.SemiBold,
    modifier = Modifier.fillMaxWidth()
)

@Composable
private fun PizzaTile(
    name: String,
    selected: Boolean,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    val color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val onColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = color,
        contentColor = onColor,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.3f)),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) Icon(leadingIcon, null, Modifier.padding(end = 6.dp))
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}
