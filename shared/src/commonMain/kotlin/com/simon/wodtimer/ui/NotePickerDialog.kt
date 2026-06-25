package com.simon.wodtimer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.simon.wodtimer.model.WorkoutNote

@Composable
fun NotePickerDialog(
    notes: List<WorkoutNote>,
    selected: WorkoutNote?,
    onPick: (WorkoutNote?) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val selectedId = selected?.id
    val transient = if (selected != null && notes.none { it.id == selectedId }) selected else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Workout note") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                NoteRow(label = "None", selected = selected == null) { onPick(null) }
                if (transient != null) {
                    NoteRow(label = "${transient.name} (unsaved)", selected = true) { onPick(transient) }
                }
                if (notes.isNotEmpty()) HorizontalDivider()
                notes.forEach { note ->
                    NoteRow(label = note.name, selected = note.id == selectedId) { onPick(note) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onNew) { Text("New note") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun NoteRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = if (selected) "• $label" else label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp)
    )
}
