package com.example.pizzaorder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pizzaorder.MenuConfig
import com.example.pizzaorder.Pizza

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    menu: MenuConfig,
    onAddSauce: (String) -> Unit,
    onRemoveSauce: (String) -> Unit,
    onAddTopping: (String) -> Unit,
    onRemoveTopping: (String) -> Unit,
    onAddPreset: (Pizza) -> Unit,
    onRemovePreset: (String) -> Unit,
    onResetMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddPreset by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    if (showAddPreset) {
        AddPresetDialog(
            menu = menu,
            onDismiss = { showAddPreset = false },
            onAdd = { p ->
                onAddPreset(p)
                showAddPreset = false
            },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Standardmenü wiederherstellen?") },
            text = { Text("Alle aktuellen Saucen, Beläge und Pizzen werden ersetzt. Bestellungen bleiben erhalten.") },
            confirmButton = {
                TextButton(
                    onClick = { confirmReset = false; onResetMenu() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Wiederherstellen") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Abbrechen") }
            },
        )
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {

            Section(title = "Saucen") {
                ChipList(items = menu.sauces, onRemove = onRemoveSauce)
                AddRow(placeholder = "Neue Sauce", onAdd = onAddSauce)
            }

            Section(title = "Beläge") {
                ChipList(items = menu.toppings, onRemove = onRemoveTopping)
                AddRow(placeholder = "Neuer Belag", onAdd = onAddTopping)
            }

            Section(title = "Vordefinierte Pizzen") {
                if (menu.presets.isEmpty()) {
                    Text(
                        "(noch keine)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    menu.presets.forEach { preset ->
                        PresetRow(
                            preset = preset,
                            onRemove = { onRemovePreset(preset.name) },
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showAddPreset = true },
                    shape = RoundedCornerShape(10.dp),
                    enabled = menu.sauces.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pizza hinzufügen")
                }
            }

            HorizontalDivider()

            TextButton(
                onClick = { confirmReset = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Standardmenü wiederherstellen")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Section building blocks ───────────────────────────────────────────────

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipList(items: List<String>, onRemove: (String) -> Unit) {
    if (items.isEmpty()) {
        Text(
            "(noch keine)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            AssistChip(
                onClick = { onRemove(item) },
                label = { Text(item) },
                trailingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Löschen",
                        modifier = Modifier.size(16.dp),
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun AddRow(placeholder: String, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(placeholder) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = {
                if (text.isNotBlank()) {
                    onAdd(text)
                    text = ""
                }
            },
            enabled = text.isNotBlank(),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(56.dp),  // match OutlinedTextField height
        ) {
            Icon(Icons.Default.Add, contentDescription = "Hinzufügen")
        }
    }
}

@Composable
private fun PresetRow(preset: Pizza, onRemove: () -> Unit) {
    val toppingDesc = if (preset.toppings.isEmpty()) "—"
                      else preset.toppings.joinToString(", ")

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    preset.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${preset.sauce} · $toppingDesc",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Löschen",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// ─── Add-preset dialog ─────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddPresetDialog(
    menu: MenuConfig,
    onDismiss: () -> Unit,
    onAdd: (Pizza) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var sauce by remember { mutableStateOf(menu.sauces.firstOrNull().orEmpty()) }
    var toppings by remember { mutableStateOf<List<String>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pizza hinzufügen") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)   // keep dialog scrollable on small screens
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("z.B. Mafiosa") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    "SAUCE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    menu.sauces.forEach { s ->
                        FilterChip(
                            selected = s == sauce,
                            onClick = { sauce = s },
                            label = { Text(s) },
                        )
                    }
                }

                Text(
                    "BELAG",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                if (menu.toppings.isEmpty()) {
                    Text(
                        "Erst Beläge im Hauptmenü anlegen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(Pizza(name.trim(), sauce, toppings)) },
                enabled = name.isNotBlank() && sauce.isNotBlank(),
            ) { Text("Hinzufügen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}
