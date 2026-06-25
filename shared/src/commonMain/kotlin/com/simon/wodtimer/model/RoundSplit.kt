package com.simon.wodtimer.model

data class RoundSplit(
    val round: Int,
    val elapsedSeconds: Int,
    val atMillis: Long,
    val reps: Int = 0,
    val interval: Int = 0,
    val intervalStartMillis: Long = 0L
)
