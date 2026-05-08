package com.example.pizzaorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pizzaorder.Order

@Composable
fun OrdersListScreen(
    orders: List<Order>,
    onToggleDone: (Order) -> Unit,
    onReprint: (Order) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmClear by remember { mutableStateOf(false) }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Alle Bestellungen löschen?") },
            text = { Text("Das kann nicht rückgängig gemacht werden.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClearAll() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Abbrechen") } },
        )
    }

    if (orders.isEmpty()) {
        EmptyState(modifier)
        return
    }

    val (done, pending) = orders.partition { it.isDone() }
    val sortedPending = pending.sortedBy { it.id }
    val sortedDone = done.sortedByDescending { it.completedAt }

    Column(modifier.fillMaxSize().widthIn(max = 720.dp)) {
        TextButton(
            onClick = { confirmClear = true },
            modifier = Modifier.align(Alignment.End).padding(horizontal = 24.dp, vertical = 8.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Alle löschen")
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(sortedPending, key = { it.id }) { order ->
                OrderCard(order, { onToggleDone(order) }, { onReprint(order) }, Modifier.animateItem())
            }

            if (sortedDone.isNotEmpty()) {
                item { SectionRibbon("Fertig (${sortedDone.size})", Modifier.animateItem()) }
                items(sortedDone, key = { it.id }) { order ->
                    OrderCard(order, { onToggleDone(order) }, { onReprint(order) }, Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    onToggleDone: () -> Unit,
    onReprint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onToggleDone,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (order.isDone()) 0.dp else 1.dp),
    ) {
        Row(
            Modifier.padding(16.dp).alpha(if (order.isDone()) 0.55f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DoneIndicator(order.isDone())
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text(order.customer, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(order.formattedTime, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(order.pizza.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(order.pizza.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (order.note.isNotBlank()) {
                    Text("„${order.note}“", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 4.dp))
                }
            }
            IconButton(onClick = onReprint) {
                Icon(Icons.Default.Email, "Erneut drucken", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DoneIndicator(done: Boolean) {
    val modifier = Modifier.size(36.dp).clip(CircleShape).then(
        if (done) Modifier.background(MaterialTheme.colorScheme.primary)
        else Modifier.border(2.dp, MaterialTheme.colorScheme.outline.copy(0.5f), CircleShape)
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        if (done) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SectionRibbon(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f))
        Text("  $text  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        HorizontalDivider(Modifier.weight(1f))
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🍕", style = MaterialTheme.typography.displayLarge)
            Text("Noch keine Bestellungen", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
