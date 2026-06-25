package com.simon.wodtimer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.simon.wodtimer.model.QuickMode
import com.simon.wodtimer.model.RunSpec

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EmomDialog(onStart: (RunSpec) -> Unit, onDismiss: () -> Unit) {
    var interval by remember { mutableStateOf(QuickMode.EmomInterval.ONE_MINUTE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("EMOM") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Interval — repeats until you end the session")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickMode.EmomInterval.entries.forEach { option ->
                        FilterChip(
                            selected = interval == option,
                            onClick = { interval = option },
                            label = { Text(option.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onStart(QuickMode.emom(interval.seconds)) }) { Text("Start") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
