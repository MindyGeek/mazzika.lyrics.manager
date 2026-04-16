package com.mazzika.lyrics.ui.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.unit.dp

// ═════════════════════════════════════════════════════════
// Nav Icons — SVG paths reproduced from Studio Moderne mockups.
// All 24x24 viewBox, drawn as outline (stroke 2). The icons
// rely on tint (currentColor) so the same vector is used for
// active/inactive states with a different color applied.
// ═════════════════════════════════════════════════════════

private fun navIcon(name: String, pathBlock: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathData(pathBlock),
        pathFillType = PathFillType.NonZero,
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()

// Filled variant — fills via SolidColor(Color.Black) so tint (currentColor) applies.
private fun navIconFilled(name: String, pathBlock: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathData(pathBlock),
        pathFillType = PathFillType.NonZero,
        fill = SolidColor(Color.Black),
        stroke = null,
    ).build()

// Maison
val NavIconHome: ImageVector = navIcon("home") {
    moveTo(3f, 10.5f); lineTo(12f, 3f); lineTo(21f, 10.5f)
    verticalLineTo(20f)
    arcToRelative(1f, 1f, 0f, false, true, -1f, 1f)
    horizontalLineToRelative(-5f)
    verticalLineToRelative(-7f)
    horizontalLineToRelative(-6f)
    verticalLineToRelative(7f)
    horizontalLineTo(4f)
    arcToRelative(1f, 1f, 0f, false, true, -1f, -1f)
    close()
}
val NavIconHomeFilled: ImageVector = navIconFilled("home_filled") {
    moveTo(3f, 10.5f); lineTo(12f, 3f); lineTo(21f, 10.5f)
    verticalLineTo(20f)
    arcToRelative(1f, 1f, 0f, false, true, -1f, 1f)
    horizontalLineToRelative(-5f)
    verticalLineToRelative(-7f)
    horizontalLineToRelative(-6f)
    verticalLineToRelative(7f)
    horizontalLineTo(4f)
    arcToRelative(1f, 1f, 0f, false, true, -1f, -1f)
    close()
}

// Grille 2x2
private fun PathBuilder.tile(x: Float, y: Float) {
    moveTo(x + 2f, y)
    horizontalLineToRelative(3f)
    arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
    verticalLineToRelative(3f)
    arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
    horizontalLineToRelative(-3f)
    arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
    verticalLineToRelative(-3f)
    arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
    close()
}
val NavIconGrid: ImageVector = navIcon("grid") {
    tile(3f, 4f); tile(14f, 4f); tile(3f, 15f); tile(14f, 15f)
}
val NavIconGridFilled: ImageVector = navIconFilled("grid_filled") {
    tile(3f, 4f); tile(14f, 4f); tile(3f, 15f); tile(14f, 15f)
}

// Dossier
val NavIconFolder: ImageVector = navIcon("folder") {
    moveTo(3f, 7f)
    arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
    horizontalLineToRelative(4f)
    lineToRelative(2f, 2f)
    horizontalLineToRelative(8f)
    arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
    verticalLineToRelative(9f)
    arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
    horizontalLineTo(5f)
    arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
    verticalLineTo(7f)
    close()
}
val NavIconFolderFilled: ImageVector = navIconFilled("folder_filled") {
    moveTo(3f, 7f)
    arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
    horizontalLineToRelative(4f)
    lineToRelative(2f, 2f)
    horizontalLineToRelative(8f)
    arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
    verticalLineToRelative(9f)
    arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
    horizontalLineTo(5f)
    arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
    verticalLineTo(7f)
    close()
}

// Broadcast — 2 arcs + cercle central
val NavIconBroadcast: ImageVector = navIcon("broadcast") {
    moveTo(5f, 12f)
    arcToRelative(7f, 7f, 0f, false, true, 14f, 0f)
    moveTo(8f, 12f)
    arcToRelative(4f, 4f, 0f, false, true, 8f, 0f)
    moveTo(13.8f, 12f)
    arcToRelative(1.8f, 1.8f, 0f, true, true, -3.6f, 0f)
    arcToRelative(1.8f, 1.8f, 0f, true, true, 3.6f, 0f)
    close()
}
val NavIconBroadcastFilled: ImageVector = navIconFilled("broadcast_filled") {
    // Represent filled broadcast as 3 "waves" arcs stacked as stroked bands
    // We approximate by using 3 concentric arcs re-drawn at a slight thickness via subpath.
    // For simplicity we use two arcs + center dot with stroke converted to filled wedges.
    moveTo(5f, 12f)
    arcToRelative(7f, 7f, 0f, false, true, 14f, 0f)
    lineToRelative(-1.6f, 0f)
    arcToRelative(5.4f, 5.4f, 0f, false, false, -10.8f, 0f)
    close()
    moveTo(8f, 12f)
    arcToRelative(4f, 4f, 0f, false, true, 8f, 0f)
    lineToRelative(-1.6f, 0f)
    arcToRelative(2.4f, 2.4f, 0f, false, false, -4.8f, 0f)
    close()
    moveTo(13.8f, 12f)
    arcToRelative(1.8f, 1.8f, 0f, true, true, -3.6f, 0f)
    arcToRelative(1.8f, 1.8f, 0f, true, true, 3.6f, 0f)
    close()
}
