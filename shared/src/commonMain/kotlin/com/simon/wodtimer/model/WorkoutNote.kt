package com.simon.wodtimer.model

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutNote(
    val id: String = randomId(),
    val name: String,
    val text: String = "",
    val strokes: List<NoteStroke> = emptyList(),
    val imagePath: String? = null
)
