package com.simon.wodtimer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simon.wodtimer.model.RoundBreakdowns
import com.simon.wodtimer.model.RunSpec
import com.simon.wodtimer.model.WorkoutNote
import com.simon.wodtimer.platform.setImmersive
import com.simon.wodtimer.platform.setKeepAwake
import kotlinx.coroutines.delay

private const val FLASH_RISE_MS = 120
private const val FLASH_HOLD_MS = 140
private const val FLASH_FALL_MS = 320
private const val NOTE_TINT_ALPHA = 0.5f
private const val NOTE_IMAGE_TINT_ALPHA = 0.3f
private const val NOTE_CONTENT_ALPHA = 0.8f
private val FlashColor = Color(0xFFD50000)
private val AccentRing = Color(0xFFFF7A00)
private val DiffFaster = Color(0xFF4CAF50)
private val DiffSlower = Color(0xFFFF9800)

@Composable
fun RunScreen(
    spec: RunSpec,
    soundEnabled: Boolean,
    notes: List<WorkoutNote>,
    onSaveNote: (WorkoutNote) -> Unit,
    onExit: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val beeper = remember { Beeper(soundEnabled) }
    val engine = remember(spec) {
        TimerEngine(
            scope = scope,
            spec = spec,
            onCountBeep = { beeper.countBeep() },
            onPhaseChangeBeep = { kind -> beeper.phaseBeep(kind) }
        )
    }
    val state by engine.state.collectAsState()

    var selectedNote by remember { mutableStateOf<WorkoutNote?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        setKeepAwake(true)
        setImmersive(true)
        onDispose {
            setKeepAwake(false)
            setImmersive(false)
            beeper.release()
        }
    }

    val baseBackground = when {
        state.isFinished -> Color(0xFF1B5E20)
        state.isWork -> Color(0xFF0D47A1)
        state.isRest -> Color(0xFF6A0000)
        state.isNeutral -> Color(0xFF00695C)
        else -> Color(0xFF424242)
    }

    var flashing by remember { mutableStateOf(false) }
    LaunchedEffect(state.flashSeq) {
        if (state.isRunning && !state.isFinished && state.flashSeconds in 1..3) {
            flashing = true
            delay((FLASH_RISE_MS + FLASH_HOLD_MS).toLong())
            flashing = false
        } else {
            flashing = false
        }
    }
    val background by animateColorAsState(
        targetValue = if (flashing) FlashColor else baseBackground,
        animationSpec = tween(durationMillis = if (flashing) FLASH_RISE_MS else FLASH_FALL_MS),
        label = "flashBackground"
    )

    val showRounds = state.phase.totalRounds > 0 && !state.isFinished
    val note = selectedNote
    val noteAttached = note != null

    Box(modifier = Modifier.fillMaxSize()) {
        if (noteAttached) {
            NoteBoard(note = note, modifier = Modifier.fillMaxSize().systemBarsPadding())
            val tintAlpha = if (note.imagePath != null) NOTE_IMAGE_TINT_ALPHA else NOTE_TINT_ALPHA
            Box(modifier = Modifier.fillMaxSize().background(background.copy(alpha = tintAlpha)))
        } else {
            Box(modifier = Modifier.fillMaxSize().background(background))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp)
                .then(if (noteAttached) Modifier.alpha(NOTE_CONTENT_ALPHA) else Modifier)
        ) {
            if (state.isFinished) {
                FinishedContent(spec = spec, state = state, onBack = { engine.stop(); onExit() })
            } else {
                RunningContent(
                    spec = spec,
                    state = state,
                    showRounds = showRounds,
                    showWorkoutButton = !state.isRunning && state.elapsedSeconds == 0,
                    noteName = selectedNote?.name,
                    onWorkout = { showPicker = true },
                    onStartPause = { if (state.isRunning) engine.pause() else engine.resume() },
                    onReset = { engine.stop() },
                    onEnd = {
                        if (state.elapsedSeconds > 0 || state.roundCount > 0 || state.splits.isNotEmpty()) {
                            engine.end()
                        } else {
                            engine.stop()
                            onExit()
                        }
                    },
                    onIncrementRound = { engine.incrementRound() }
                )
            }
        }

        if (showEditor) {
            NoteEditScreen(
                existing = null,
                defaultPersist = false,
                showPersistToggle = true,
                onSave = { newNote, persist ->
                    if (persist) onSaveNote(newNote)
                    selectedNote = newNote
                    showEditor = false
                },
                onCancel = { showEditor = false }
            )
        }
    }

    if (showPicker) {
        NotePickerDialog(
            notes = notes,
            selected = selectedNote,
            onPick = { picked ->
                selectedNote = picked
                showPicker = false
            },
            onNew = {
                showPicker = false
                showEditor = true
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun RunningContent(
    spec: RunSpec,
    state: RunState,
    showRounds: Boolean,
    showWorkoutButton: Boolean,
    noteName: String?,
    onWorkout: () -> Unit,
    onStartPause: () -> Unit,
    onReset: () -> Unit,
    onEnd: () -> Unit,
    onIncrementRound: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(spec.title, color = Color.White, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            if (showRounds) {
                Text(
                    "Round ${state.phase.roundIndex} / ${state.phase.totalRounds}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            } else if (state.phase.roundIndex > 0) {
                Text("Round ${state.phase.roundIndex}", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            if (showWorkoutButton) {
                OutlinedButton(onClick = onWorkout, modifier = Modifier.padding(top = 8.dp)) {
                    Text(if (noteName.isNullOrBlank()) "Workout" else "Workout: $noteName", color = Color.White)
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.phase.label.uppercase(),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatMmSs(state.displaySeconds),
                color = Color.White,
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold
            )
            if (state.showCounterButton) {
                RoundCounter(
                    count = if (state.repCounter) state.currentReps else state.roundCount,
                    label = "round counter",
                    onIncrement = onIncrementRound
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 96.dp)
        ) {
            Button(onClick = onStartPause) {
                Text(if (state.isRunning) "Pause" else "Start")
            }
            Button(onClick = onReset) { Text("Reset") }
            Button(onClick = onEnd) { Text("End") }
        }
    }
}

@Composable
private fun RoundCounter(count: Int, label: String, onIncrement: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 24.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            FilledIconButton(
                onClick = onIncrement,
                modifier = Modifier.size(76.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = label, modifier = Modifier.size(38.dp))
            }
            if (count > 0) {
                Text(
                    "$count",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp)
                )
            }
        }
        Text(
            label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun FinishedContent(spec: RunSpec, state: RunState, onBack: () -> Unit) {
    val groups = remember(state.splits) { RoundBreakdowns.grouped(state.splits) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            Text(spec.title, color = Color.White, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text("DONE", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

            if (groups.isEmpty()) {
                Text("No rounds recorded", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
            } else {
                val showHeaders = groups.size > 1 || groups.first().interval > 0
                groups.forEach { group ->
                    if (showHeaders) {
                        Text(
                            "Interval ${group.interval}",
                            color = AccentRing,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                    RoundTimeline(breakdowns = group.rounds, averageMillis = group.averageMillis)
                }
            }

            Text(
                "Total: ${formatMmSs(state.elapsedSeconds)}",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 24.dp)
            )
        }

        Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("Back") }
    }
}

@Composable
private fun RoundTimeline(breakdowns: List<com.simon.wodtimer.model.RoundBreakdown>, averageMillis: Long) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(end = 16.dp, top = 4.dp)
        ) {
            Box(modifier = Modifier.size(20.dp)) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(AccentRing.copy(alpha = 0.25f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(AccentRing)
                )
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height((breakdowns.size * 32 + 24).dp)
                    .background(Color.White.copy(alpha = 0.3f))
            )
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            breakdowns.forEach { b ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Round ${b.round}", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text(formatMmSsCc(b.durationMillis), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.width(64.dp)) {
                        if (b.hasDiff) {
                            Text(
                                formatDiffSeconds(b.diffMillis),
                                color = when {
                                    b.diffMillis < 0 -> DiffFaster
                                    b.diffMillis > 0 -> DiffSlower
                                    else -> Color.White
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Ø ${formatMmSsCc(averageMillis)}",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 15.sp
                )
            }
        }
    }
}
