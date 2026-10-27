package io.github.kaerbr.pizzaorder.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** Centre of dot [i] (0..8, row by row) in a square of [size]. */
private fun dotCenter(i: Int, size: Size) =
    Offset(size.width * (i % 3 * 2 + 1) / 6f, size.height * (i / 3 * 2 + 1) / 6f)

/**
 * Android-style 3×3 pattern pad. Calls [onPattern] with the dot indices when
 * the finger lifts. The drawn stroke stays visible (red when [error]) until
 * the next stroke starts.
 */
@Composable
fun PatternLock(onPattern: (List<Int>) -> Unit, error: Boolean, modifier: Modifier = Modifier) {
    val path = remember { mutableStateListOf<Int>() }
    var finger by remember { mutableStateOf<Offset?>(null) }
    val currentOnPattern by rememberUpdatedState(onPattern)

    val active = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier
            .size(260.dp)
            .pointerInput(Unit) {
                val area = Size(size.width.toFloat(), size.height.toFloat())
                val hitRadius = area.width / 8f
                fun hit(p: Offset) {
                    (0..8).firstOrNull { it !in path && (dotCenter(it, area) - p).getDistance() < hitRadius }
                        ?.let { path += it }
                }
                detectDragGestures(
                    onDragStart = { path.clear(); finger = it; hit(it) },
                    onDrag = { change, _ -> finger = change.position; hit(change.position) },
                    onDragEnd = { finger = null; currentOnPattern(path.toList()) },
                    onDragCancel = { finger = null; path.clear() },
                )
            },
    ) {
        val line = 6.dp.toPx()
        path.zipWithNext { a, b -> drawLine(active, dotCenter(a, size), dotCenter(b, size), line, StrokeCap.Round) }
        finger?.let { f -> path.lastOrNull()?.let { drawLine(active.copy(alpha = 0.5f), dotCenter(it, size), f, line, StrokeCap.Round) } }
        for (i in 0..8) {
            val c = dotCenter(i, size)
            if (i in path) {
                drawCircle(active, 10.dp.toPx(), c)
                drawCircle(active, 22.dp.toPx(), c, style = Stroke(2.dp.toPx()))
            } else {
                drawCircle(idle, 7.dp.toPx(), c)
            }
        }
    }
}
