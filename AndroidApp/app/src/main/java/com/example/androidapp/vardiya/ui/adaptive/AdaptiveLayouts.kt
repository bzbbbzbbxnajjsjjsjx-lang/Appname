package com.example.androidapp.vardiya.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.androidapp.vardiya.ui.navigation.WindowWidthSizeClass

/**
 * Pure, dependency-free Material 3 Expressive Adaptive Layout Foundation for Vardiya 3.0.
 *
 * Breakpoints (Material 3 Specification):
 * - Compact:  < 600dp (standard portrait phones)
 * - Medium:   600dp ..< 840dp (foldables unfolded, portrait tablets)
 * - Expanded: >= 840dp (landscape tablets, desktop, wide monitors)
 */
object VardiyaBreakpoints {
    val COMPACT_MAX_WIDTH: Dp = 600.dp
    val MEDIUM_MAX_WIDTH: Dp = 840.dp

    fun resolve(width: Dp): WindowWidthSizeClass = WindowWidthSizeClass.fromWidth(width)
}

/**
 * Reusable Two-Pane Layout for List-Detail and split-screen workflows.
 *
 * Behavior:
 * - Compact (< 600dp): Displays only the listPane (single pane).
 * - Medium (600dp..<840dp) / Expanded (>= 840dp): Displays listPane on the left (~42%)
 *   and detailPane on the right (~58%), separated by a subtle vertical divider.
 */
@Composable
fun VardiyaTwoPaneLayout(
    modifier: Modifier = Modifier,
    sizeClass: WindowWidthSizeClass,
    listPaneWeight: Float = 0.42f,
    detailPaneWeight: Float = 0.58f,
    listPane: @Composable () -> Unit,
    detailPane: @Composable () -> Unit
) {
    if (sizeClass.isCompact) {
        Box(modifier = modifier.fillMaxSize()) {
            listPane()
        }
    } else {
        Row(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(listPaneWeight)
                    .fillMaxHeight()
            ) {
                listPane()
            }

            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Box(
                modifier = Modifier
                    .weight(detailPaneWeight)
                    .fillMaxHeight()
            ) {
                detailPane()
            }
        }
    }
}

/**
 * Reusable two-column adaptive container for dashboard, analytics, and settings screens.
 *
 * - Compact: Renders children in a single vertical column.
 * - Medium/Expanded: Renders children side by side in a two-column row with weights.
 */
@Composable
fun AdaptiveDualColumnLayout(
    modifier: Modifier = Modifier,
    sizeClass: WindowWidthSizeClass,
    leftColumnWeight: Float = 0.5f,
    rightColumnWeight: Float = 0.5f,
    spacing: Dp = 16.dp,
    leftColumn: @Composable () -> Unit,
    rightColumn: @Composable () -> Unit
) {
    if (sizeClass.isCompact) {
        Column(modifier = modifier.fillMaxWidth()) {
            leftColumn()
            rightColumn()
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.weight(leftColumnWeight)
            ) {
                leftColumn()
            }

            Box(modifier = Modifier.width(spacing))

            Box(
                modifier = Modifier.weight(rightColumnWeight)
            ) {
                rightColumn()
            }
        }
    }
}
