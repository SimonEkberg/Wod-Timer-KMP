package com.simon.wodtimer.model

import kotlinx.serialization.Serializable

@Serializable
data class NoteStroke(val points: List<NotePoint>)
