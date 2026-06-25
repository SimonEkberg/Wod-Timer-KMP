package com.simon.wodtimer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.simon.wodtimer.model.Workout
import com.simon.wodtimer.model.WorkoutNote

const val TAB_STANDARD = 0
const val TAB_CUSTOM = 1
const val TAB_WORKOUTS = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    workouts: List<Workout>,
    notes: List<WorkoutNote>,
    tab: Int,
    onTabChange: (Int) -> Unit,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onClock: () -> Unit,
    onTimer: () -> Unit,
    onEmom: () -> Unit,
    onTabata: () -> Unit,
    onRunWorkout: (Workout) -> Unit,
    onEdit: (Workout) -> Unit,
    onDelete: (Workout) -> Unit,
    onCreate: () -> Unit,
    onEditNote: (WorkoutNote) -> Unit,
    onDeleteNote: (WorkoutNote) -> Unit,
    onCreateNote: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WOD Timer") },
                actions = {
                    IconButton(onClick = onToggleSound) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (soundEnabled) "Sound on" else "Sound off"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            when (tab) {
                TAB_CUSTOM -> FloatingActionButton(onClick = onCreate) {
                    Icon(Icons.Default.Add, contentDescription = "New workout")
                }
                TAB_WORKOUTS -> FloatingActionButton(onClick = onCreateNote) {
                    Icon(Icons.Default.Add, contentDescription = "New workout note")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == TAB_STANDARD, onClick = { onTabChange(TAB_STANDARD) }, text = { Text("STANDARD") })
                    Tab(selected = tab == TAB_CUSTOM, onClick = { onTabChange(TAB_CUSTOM) }, text = { Text("CUSTOM") })
                    Tab(selected = tab == TAB_WORKOUTS, onClick = { onTabChange(TAB_WORKOUTS) }, text = { Text("WORKOUTS") })
                }
                when (tab) {
                    TAB_STANDARD -> StandardScreen(onClock = onClock, onTimer = onTimer, onEmom = onEmom, onTabata = onTabata)
                    TAB_CUSTOM -> CustomWorkoutsList(workouts = workouts, onRun = onRunWorkout, onEdit = onEdit, onDelete = onDelete)
                    else -> WorkoutNotesList(notes = notes, onEdit = onEditNote, onDelete = onDeleteNote)
                }
            }
        }
    }
}
