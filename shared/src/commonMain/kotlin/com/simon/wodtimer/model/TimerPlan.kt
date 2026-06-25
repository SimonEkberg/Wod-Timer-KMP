package com.simon.wodtimer.model

object TimerPlan {

    fun expand(workout: Workout): List<TimerPhase> {
        val phases = mutableListOf<TimerPhase>()
        if (workout.prepSeconds > 0) {
            phases += TimerPhase(PhaseKind.PREP, workout.prepSeconds, 0, workout.rounds, "Get ready")
        }
        for (round in 1..workout.rounds) {
            workout.segmentsPerRound.forEachIndexed { index, segment ->
                val kind = when (segment.kind) {
                    SegmentKind.WORK -> PhaseKind.WORK
                    SegmentKind.REST -> PhaseKind.REST
                    SegmentKind.INTERVAL -> PhaseKind.INTERVAL
                }
                val label = when (kind) {
                    PhaseKind.WORK -> "Work"
                    PhaseKind.REST -> "Rest"
                    else -> "Interval ${index + 1}"
                }
                phases += TimerPhase(kind, segment.seconds, round, workout.rounds, label)
            }
        }
        return phases
    }
}
