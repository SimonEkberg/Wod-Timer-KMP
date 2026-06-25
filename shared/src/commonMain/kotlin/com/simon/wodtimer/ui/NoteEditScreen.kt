package com.simon.wodtimer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simon.wodtimer.model.NotePoint
import com.simon.wodtimer.model.NoteStroke
import com.simon.wodtimer.model.WorkoutNote
import com.simon.wodtimer.model.randomId
import com.simon.wodtimer.platform.NoteImageStore
import com.simon.wodtimer.platform.rememberImagePicker
import kotlinx.coroutines.launch

private enum class NoteMode { TYPE, DRAW }

private val NoteImageInk = Color(0xFFF7F3E9)
private const val EDITOR_IMAGE_SCRIM = 0.4f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditScreen(
    existing: WorkoutNote?,
    defaultPersist: Boolean,
    showPersistToggle: Boolean,
    onSave: (WorkoutNote, Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var text by remember { mutableStateOf(existing?.text ?: "") }
    var mode by remember { mutableStateOf(NoteMode.TYPE) }
    var persist by remember { mutableStateOf(defaultPersist) }
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    var imagePath by remember { mutableStateOf(existing?.imagePath) }

    val strokes = remember { mutableStateListOf<NoteStroke>().apply { existing?.strokes?.let { addAll(it) } } }
    val currentStroke = remember { mutableStateListOf<NotePoint>() }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val imageBitmap = remember(imagePath) {
        imagePath?.let { NoteImageStore.decode(it, 1600) }
    }
    val ink = if (imageBitmap != null) NoteImageInk else NoteInkColor

    val launchImagePicker = rememberImagePicker { path -> imagePath = path }

    fun normalize(offset: Offset): NotePoint {
        val w = boardSize.width.coerceAtLeast(1)
        val h = boardSize.height.coerceAtLeast(1)
        return NotePoint((offset.x / w).coerceIn(0f, 1f), (offset.y / h).coerceIn(0f, 1f))
    }

    fun scrollByViewport(direction: Int) {
        scope.launch {
            val delta = (scrollState.viewportSize * 0.8f).coerceAtLeast(400f)
            scrollState.animateScrollBy(direction * delta)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (existing == null) "New workout" else "Edit workout") }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(selected = mode == NoteMode.TYPE, onClick = { mode = NoteMode.TYPE }, label = { Text("Type") })
                FilterChip(selected = mode == NoteMode.DRAW, onClick = { mode = NoteMode.DRAW }, label = { Text("Draw") })
                if (mode == NoteMode.DRAW) {
                    OutlinedButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }) { Text("Undo") }
                    OutlinedButton(onClick = { strokes.clear() }) { Text("Clear") }
                }
                OutlinedIconButton(onClick = { scrollByViewport(-1) }) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll up")
                }
                OutlinedIconButton(onClick = { scrollByViewport(1) }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll down")
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    launchImagePicker()
                }) { Text(if (imagePath == null) "Pick image" else "Change image") }
                if (imagePath != null) {
                    OutlinedButton(onClick = { imagePath = null }) { Text("Remove image") }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NoteBoardColor)
            ) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState, enabled = mode != NoteMode.DRAW)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f / BOARD_ASPECT)
                            .onSizeChanged { boardSize = it }
                    ) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                            )
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = EDITOR_IMAGE_SCRIM)))
                        }

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val widthPx = 3f * density
                            strokes.forEach { drawNoteStroke(it, ink, widthPx, size.width, size.height) }
                            if (currentStroke.isNotEmpty()) {
                                drawNoteStroke(NoteStroke(currentStroke.toList()), ink, widthPx, size.width, size.height)
                            }
                        }

                        if (mode == NoteMode.TYPE) {
                            BasicTextField(
                                value = text,
                                onValueChange = { text = it },
                                textStyle = TextStyle(color = ink, fontSize = 20.sp, fontWeight = FontWeight.Medium),
                                modifier = Modifier.fillMaxSize().padding(20.dp),
                                decorationBox = { inner ->
                                    if (text.isEmpty()) {
                                        Text("Type the workout…", color = ink.copy(alpha = 0.4f), fontSize = 20.sp)
                                    }
                                    inner()
                                }
                            )
                        } else {
                            if (text.isNotBlank()) {
                                Text(text, color = ink, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxSize().padding(20.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(boardSize) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                currentStroke.clear()
                                                currentStroke.add(normalize(offset))
                                            },
                                            onDrag = { change, _ ->
                                                currentStroke.add(normalize(change.position))
                                                change.consume()
                                            },
                                            onDragEnd = {
                                                if (currentStroke.isNotEmpty()) {
                                                    strokes.add(NoteStroke(currentStroke.toList()))
                                                    currentStroke.clear()
                                                }
                                            },
                                            onDragCancel = { currentStroke.clear() }
                                        )
                                    }
                            )
                        }
                    }
                }
            }

            if (showPersistToggle) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = persist, onCheckedChange = { persist = it })
                    Text("Persist (save to this phone)")
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val note = WorkoutNote(
                        id = existing?.id ?: randomId(),
                        name = name.ifBlank { "Workout" },
                        text = text,
                        strokes = strokes.toList(),
                        imagePath = imagePath
                    )
                    onSave(note, persist)
                }
            ) { Text("Save") }
            Button(modifier = Modifier.fillMaxWidth(), onClick = onCancel) { Text("Cancel") }
        }
    }
}
