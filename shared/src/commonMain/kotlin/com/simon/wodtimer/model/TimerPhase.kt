package com.simon.wodtimer.model

enum class PhaseKind { PREP, WORK, REST, INTERVAL, COUNT_UP, DONE }

data class TimerPhase(
    val kind: PhaseKind,
    val totalSeconds: Int,
    val roundIndex: Int,
    val totalRounds: Int,
    val label: String
)
