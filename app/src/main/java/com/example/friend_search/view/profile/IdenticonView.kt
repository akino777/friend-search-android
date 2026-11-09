package com.example.friend_search.view.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.friend_search.viewmodel.shared.IdenticonPattern

/** Identicon の描画（パターンは ViewModel 層の IdenticonPattern）。相対座標で描くのでサイズに依存しない。 */
@Composable
fun IdenticonView(publicUserId: String, avatarVersion: Int, size: Dp) {
    val pattern = remember(publicUserId, avatarVersion) { IdenticonPattern.of(publicUserId, avatarVersion) }
    val background = Color(0xFF000000 or pattern.palette.background.toLong())
    val foreground = Color(0xFF000000 or pattern.palette.foreground.toLong())
    Canvas(Modifier.size(size).clip(RoundedCornerShape(size * 0.2f))) {
        val side = minOf(this.size.width, this.size.height)
        val padding = side * 0.1f
        val cell = (side - padding * 2) / IdenticonPattern.SIZE
        drawRect(background)
        pattern.cells.forEachIndexed { row, rowCells ->
            rowCells.forEachIndexed { column, filled ->
                if (filled) drawRect(foreground, topLeft = Offset(padding + column * cell, padding + row * cell), size = Size(cell, cell))
            }
        }
    }
}

@Preview
@Composable
private fun IdenticonPreview() {
    IdenticonView("b24683ec-304d-40f6-a1d1-8c8e52a3d09d", 1, size = 80.dp)
}
