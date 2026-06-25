package com.simon.wodtimer

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.simon.wodtimer.model.QuickMode
import com.simon.wodtimer.model.RunSpec
import com.simon.wodtimer.model.Workout
import com.simon.wodtimer.model.WorkoutNote
import com.simon.wodtimer.platform.AppBackHandler
import com.simon.wodtimer.ui.EmomDialog
import com.simon.wodtimer.ui.HomeScreen
import com.simon.wodtimer.ui.NoteEditScreen
import com.simon.wodtimer.ui.RunScreen
import com.simon.wodtimer.ui.TAB_CUSTOM
import com.simon.wodtimer.ui.TAB_STANDARD
import com.simon.wodtimer.ui.TAB_WORKOUTS
import com.simon.wodtimer.ui.TimerDialog
import com.simon.wodtimer.ui.WorkoutEditScreen
import com.simon.wodtimer.ui.WorkoutViewModel

private val WodDarkColors = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF00131A),
    secondary = Color(0xFF4DD0E1),
    background = Color(0xFF0A0E14),
    onBackground = Color(0xFFE6F4F1),
    surface = Color(0xFF11161F),
    onSurface = Color(0xFFE6F4F1),
    surfaceVariant = Color(0xFF1B2430),
    onSurfaceVariant = Color(0xFFB9C4CF)
)

private sealed interface Screen {
    data object Home : Screen
    data class Run(val spec: RunSpec, val originTab: Int) : Screen
    data class Edit(val workout: Workout?) : Screen
    data class EditNote(val note: WorkoutNote?) : Screen
}

@Composable
fun App() {
    MaterialTheme(colorScheme = WodDarkColors) {
        Surface {
            AppRoot()
        }
    }
}

@Composable
private fun AppRoot() {
    val viewModel = remember { WorkoutViewModel() }
    val workouts by viewModel.workouts.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showEmomDialog by remember { mutableStateOf(false) }
    var homeTab by remember { mutableIntStateOf(TAB_STANDARD) }

    fun goHome(tab: Int) {
        homeTab = tab
        screen = Screen.Home
    }

    AppBackHandler(enabled = screen != Screen.Home || homeTab != TAB_STANDARD) {
        when (val current = screen) {
            is Screen.Run -> goHome(current.originTab)
            is Screen.Edit -> goHome(TAB_CUSTOM)
            is Screen.EditNote -> goHome(TAB_WORKOUTS)
            Screen.Home -> homeTab = TAB_STANDARD
        }
    }

    when (val current = screen) {
        is Screen.Home -> HomeScreen(
            workouts = workouts,
            notes = notes,
            tab = homeTab,
            onTabChange = { homeTab = it },
            soundEnabled = soundEnabled,
            onToggleSound = { viewModel.toggleSound() },
            onClock = { screen = Screen.Run(QuickMode.clock(), TAB_STANDARD) },
            onTimer = { showTimerDialog = true },
            onEmom = { showEmomDialog = true },
            onTabata = { screen = Screen.Run(QuickMode.tabata(), TAB_STANDARD) },
            onRunWorkout = { screen = Screen.Run(RunSpec.fromWorkout(it), TAB_CUSTOM) },
            onEdit = { screen = Screen.Edit(it) },
            onDelete = { viewModel.delete(it) },
            onCreate = { screen = Screen.Edit(null) },
            onEditNote = { screen = Screen.EditNote(it) },
            onDeleteNote = { viewModel.deleteNote(it) },
            onCreateNote = { screen = Screen.EditNote(null) }
        )
        is Screen.Run -> RunScreen(
            spec = current.spec,
            soundEnabled = soundEnabled,
            notes = notes,
            onSaveNote = { viewModel.upsertNote(it) },
            onExit = { goHome(current.originTab) }
        )
        is Screen.Edit -> WorkoutEditScreen(
            existing = current.workout,
            onSave = {
                viewModel.upsert(it)
                goHome(TAB_CUSTOM)
            },
            onCancel = { goHome(TAB_CUSTOM) }
        )
        is Screen.EditNote -> NoteEditScreen(
            existing = current.note,
            defaultPersist = true,
            showPersistToggle = false,
            onSave = { note, _ ->
                viewModel.upsertNote(note)
                goHome(TAB_WORKOUTS)
            },
            onCancel = { goHome(TAB_WORKOUTS) }
        )
    }

    if (showTimerDialog) {
        TimerDialog(
            onStart = {
                showTimerDialog = false
                screen = Screen.Run(it, TAB_STANDARD)
            },
            onDismiss = { showTimerDialog = false }
        )
    }

    if (showEmomDialog) {
        EmomDialog(
            onStart = {
                showEmomDialog = false
                screen = Screen.Run(it, TAB_STANDARD)
            },
            onDismiss = { showEmomDialog = false }
        )
    }
}
