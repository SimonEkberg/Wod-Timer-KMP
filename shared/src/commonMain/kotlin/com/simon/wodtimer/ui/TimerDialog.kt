package com.simon.wodtimer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simon.wodtimer.model.QuickMode
import com.simon.wodtimer.model.RunSpec

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimerDialog(onStart: (RunSpec) -> Unit, onDismiss: () -> Unit) {
    var minutes by remember { mutableStateOf("5") }
    var seconds by remember { mutableStateOf("0") }
    var mode by remember { mutableStateOf(QuickMode.TimerMode.COUNT_DOWN) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Timer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { minutes = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(110.dp)
                    )
                    OutlinedTextField(
                        value = seconds,
                        onValueChange = { seconds = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Sec") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(110.dp)
                    )
                }
                Text("Mode")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == QuickMode.TimerMode.COUNT_DOWN, onClick = { mode = QuickMode.TimerMode.COUNT_DOWN }, label = { Text("Count down") })
                    FilterChip(selected = mode == QuickMode.TimerMode.COUNT_UP, onClick = { mode = QuickMode.TimerMode.COUNT_UP }, label = { Text("Count up") })
                    FilterChip(selected = mode == QuickMode.TimerMode.REPEAT, onClick = { mode = QuickMode.TimerMode.REPEAT }, label = { Text("Repeat") })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val total = (minutes.toIntOrNull() ?: 0) * 60 + (seconds.toIntOrNull() ?: 0)
                if (total > 0) onStart(QuickMode.timer(total, mode))
            }) { Text("Start") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
