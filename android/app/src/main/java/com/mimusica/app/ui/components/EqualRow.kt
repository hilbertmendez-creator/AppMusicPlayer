package com.mimusica.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Any number of buttons of equal weight side by side, each the same width and the height of the tallest. When a
 * word no longer fits in its share of the width (large text, narrow screen) they stack, first on top, so a label
 * never breaks mid-word. Generalises [TwoUp].
 */
@Composable
fun EqualRow(
    modifier: Modifier = Modifier,
    gap: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        val gapPx = gap.roundToPx()
        val count = measurables.size
        if (count == 0) return@Layout layout(width, 0) {}
        val slot = (width - gapPx * (count - 1)) / count
        val sideBySide = measurables.all { it.minIntrinsicWidth(Constraints.Infinity) <= slot }
        if (sideBySide) {
            // Intrinsic heights (a child may only be measured once): every button gets the tallest one's height.
            val height = measurables.maxOf { it.minIntrinsicHeight(slot) }
            val placeables = measurables.map {
                it.measure(Constraints(minWidth = slot, maxWidth = slot, minHeight = height, maxHeight = height))
            }
            val h = placeables.maxOf { it.height }
            layout(width, h) {
                placeables.forEachIndexed { i, p -> p.placeRelative(i * (slot + gapPx), 0) }
            }
        } else {
            val placeables = measurables.map { it.measure(Constraints.fixedWidth(width)) }
            val total = placeables.sumOf { it.height } + gapPx * (count - 1)
            layout(width, total) {
                var y = 0
                placeables.forEach { p ->
                    p.placeRelative(0, y)
                    y += p.height + gapPx
                }
            }
        }
    }
}
