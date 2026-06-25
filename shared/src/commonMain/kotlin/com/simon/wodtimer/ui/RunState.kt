package com.simon.wodtimer.ui

import com.simon.wodtimer.model.PhaseKind
import com.simon.wodtimer.model.RoundSplit
import com.simon.wodtimer.model.TimerPhase

data class RunState(
    val phase: TimerPhase,
    val displaySeconds: Int,
    val phaseIndex: Int,
    val totalPhases: Int,
    val isRunning: Boolean,
    val isFinished: Boolean,
    val isCountUp: Boolean = false,
    val elapsedSeconds: Int = 0,
    val manualCounter: Boolean = false,
    val repCounter: Boolean = false,
    val roundCount: Int = 0,
    val currentReps: Int = 0,
    val canCount: Boolean = true,
    val splits: List<RoundSplit> = emptyList(),
    val flashSeconds: Int = 0,
    val flashSeq: Int = 0
) {
    val showCounterButton: Boolean
        get() = manualCounter || (repCounter && canCount)
    val isWork: Boolean get() = phase.kind == PhaseKind.WORK
    val isRest: Boolean get() = phase.kind == PhaseKind.REST
    val isPrep: Boolean get() = phase.kind == PhaseKind.PREP
    val isNeutral: Boolean get() = phase.kind == PhaseKind.INTERVAL || phase.kind == PhaseKind.COUNT_UP
}
