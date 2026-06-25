package com.simon.wodtimer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simon.wodtimer.platform.NoteImageStore
import com.simon.wodtimer.model.NoteStroke
import com.simon.wodtimer.model.WorkoutNote

val NoteBoardColor = Color(0xFFF7F3E9)
val NoteInkColor = Color(0xFF14181F)
private val NoteImageInk = Color(0xFFF7F3E9)
private const val IMAGE_DECODE_MAX_DIM = 1600

// Whiteboard is a tall canvas (height = BOARD_ASPECT x width); scrollable in the editor,
// scaled to fit on the run screen so the whole note is visible behind the timer.
const val BOARD_ASPECT = 3f

@Composable
fun NoteBoard(
    note: WorkoutNote,
    modifier: Modifier = Modifier,
    boardColor: Color = NoteBoardColor,
    inkColor: Color = NoteInkColor,
    strokeWidthDp: Float = 3f,
    fontSize: Int = 20,
    imageScrimAlpha: Float = 0.35f
) {
    val imageBitmap = remember(note.imagePath) {
        note.imagePath?.let { NoteImageStore.decode(it, IMAGE_DECODE_MAX_DIM) }
    }
    val ink = if (imageBitmap != null) NoteImageInk else inkColor

    Box(modifier = modifier.background(boardColor)) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = imageScrimAlpha)))
        }
        if (note.text.isNotBlank()) {
            androidx.compose.material3.Text(
                text = note.text,
                color = ink,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxSize().padding(20.dp)
            )
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val widthPx = strokeWidthDp * density
            note.strokes.forEach { stroke ->
                drawNoteStroke(stroke, ink, widthPx, size.width, size.height)
            }
        }
    }
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNoteStroke(
    stroke: NoteStroke,
    color: Color,
    widthPx: Float,
    boardWidth: Float,
    boardHeight: Float
) {
    val points = stroke.points
    if (points.isEmpty()) return
    if (points.size == 1) {
        val p = points.first()
        drawCircle(color = color, radius = widthPx / 2f, center = androidx.compose.ui.geometry.Offset(p.x * boardWidth, p.y * boardHeight))
        return
    }
    val path = Path()
    points.forEachIndexed { index, point ->
        val x = point.x * boardWidth
        val y = point.y * boardHeight
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path = path, color = color, style = Stroke(width = widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
