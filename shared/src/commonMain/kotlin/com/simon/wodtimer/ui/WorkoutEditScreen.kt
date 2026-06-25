package com.simon.wodtimer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simon.wodtimer.model.Segment
import com.simon.wodtimer.model.SegmentKind
import com.simon.wodtimer.model.Workout
import com.simon.wodtimer.model.WorkoutTemplate

private const val MAX_ROWS = 60

private enum class EditMode { TEMPLATE, CUSTOM }

private data class Row(val minutes: String, val seconds: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkoutEditScreen(
    existing: Workout?,
    onSave: (Workout) -> Unit,
    onCancel: () -> Unit
) {
    val existingIsCustom = existing != null && existing.segmentsPerRound.all { it.kind == SegmentKind.INTERVAL }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var mode by remember { mutableStateOf(if (existingIsCustom) EditMode.CUSTOM else EditMode.TEMPLATE) }
    var template by remember { mutableStateOf(WorkoutTemplate.INTERVAL) }
    var rounds by remember { mutableStateOf((existing?.rounds ?: 10).toString()) }
    val initialWork = existing?.segmentsPerRound?.firstOrNull { it.kind == SegmentKind.WORK }?.seconds ?: 30
    val initialRest = existing?.segmentsPerRound?.firstOrNull { it.kind == SegmentKind.REST }?.seconds ?: 30
    var workMin by remember { mutableStateOf((initialWork / 60).toString()) }
    var workSec by remember { mutableStateOf((initialWork % 60).toString()) }
    var restMin by remember { mutableStateOf((initialRest / 60).toString()) }
    var restSec by remember { mutableStateOf((initialRest % 60).toString()) }

    val rows = remember {
        mutableStateListOf<Row>().apply {
            if (existingIsCustom) {
                existing!!.segmentsPerRound.forEach { add(Row((it.seconds / 60).toString(), (it.seconds % 60).toString())) }
            } else {
                add(Row("0", "30"))
            }
        }
    }

    fun saveWorkout() {
        if (mode == EditMode.TEMPLATE) {
            val workSeconds = ((workMin.toIntOrNull() ?: 0) * 60 + (workSec.toIntOrNull() ?: 0)).coerceAtLeast(1)
            val restSeconds = ((restMin.toIntOrNull() ?: 0) * 60 + (restSec.toIntOrNull() ?: 0)).coerceAtLeast(0)
            val built = template.build(
                name = name.ifBlank { template.displayName },
                rounds = rounds.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                workSeconds = workSeconds,
                restSeconds = restSeconds
            )
            onSave(if (existing != null) built.copy(id = existing.id) else built)
        } else {
            val segments = rows.map { row ->
                val secs = (row.minutes.toIntOrNull() ?: 0) * 60 + (row.seconds.toIntOrNull() ?: 0)
                Segment(secs.coerceAtLeast(1), SegmentKind.INTERVAL)
            }
            val built = Workout(
                name = name.ifBlank { "Custom" },
                rounds = rounds.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                segmentsPerRound = segments
            )
            onSave(if (existing != null) built.copy(id = existing.id) else built)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (existing == null) "New workout" else "Edit workout") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text("Mode")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == EditMode.TEMPLATE, onClick = { mode = EditMode.TEMPLATE }, label = { Text("Template") })
                    FilterChip(selected = mode == EditMode.CUSTOM, onClick = { mode = EditMode.CUSTOM }, label = { Text("Custom intervals") })
                }
            }

            if (mode == EditMode.TEMPLATE) {
                item {
                    Text("Template")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WorkoutTemplate.entries.forEach { option ->
                            FilterChip(
                                selected = template == option,
                                onClick = {
                                    template = option
                                    val (r, w, rst) = WorkoutTemplate.defaultsFor(option)
                                    rounds = r.toString()
                                    workMin = (w / 60).toString(); workSec = (w % 60).toString()
                                    restMin = (rst / 60).toString(); restSec = (rst % 60).toString()
                                },
                                label = { Text(option.displayName) }
                            )
                        }
                    }
                }
                val showRounds = template != WorkoutTemplate.AMRAP && template != WorkoutTemplate.FOR_TIME
                val showRest = template == WorkoutTemplate.TABATA || template == WorkoutTemplate.INTERVAL
                if (showRounds) item { NumberField("Rounds", rounds) { rounds = it } }
                item { MinSecField(if (showRounds) "Work" else "Total", workMin, workSec, { workMin = it }, { workSec = it }) }
                if (showRest) item { MinSecField("Rest", restMin, restSec, { restMin = it }, { restSec = it }) }
            } else {
                item { NumberField("Rounds (repeat whole list)", rounds) { rounds = it } }
                item { Text("Intervals (${rows.size}/$MAX_ROWS)") }
                itemsIndexed(rows) { index, row ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${index + 1}", modifier = Modifier.width(28.dp))
                        OutlinedTextField(
                            value = row.minutes,
                            onValueChange = { rows[index] = row.copy(minutes = it.filter { c -> c.isDigit() }.take(3)) },
                            label = { Text("Min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(90.dp)
                        )
                        OutlinedTextField(
                            value = row.seconds,
                            onValueChange = { rows[index] = row.copy(seconds = it.filter { c -> c.isDigit() }.take(2)) },
                            label = { Text("Sec") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(90.dp)
                        )
                        IconButton(onClick = { if (rows.size > 1) rows.removeAt(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                        }
                    }
                }
                item {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = rows.size < MAX_ROWS,
                        onClick = { if (rows.size < MAX_ROWS) rows.add(Row("0", "30")) }
                    ) { Text("Add interval") }
                }
            }

            item { Button(modifier = Modifier.fillMaxWidth(), onClick = { saveWorkout() }) { Text("Save") } }
            item { Button(modifier = Modifier.fillMaxWidth(), onClick = onCancel) { Text("Cancel") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinSecField(
    label: String,
    minutes: String,
    seconds: String,
    onMinutes: (String) -> Unit,
    onSeconds: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = minutes,
                onValueChange = { onMinutes(it.filter { c -> c.isDigit() }.take(3)) },
                label = { Text("Min") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(110.dp)
            )
            OutlinedTextField(
                value = seconds,
                onValueChange = { onSeconds(it.filter { c -> c.isDigit() }.take(2)) },
                label = { Text("Sec") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(110.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onChange(input.filter { it.isDigit() }) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}
