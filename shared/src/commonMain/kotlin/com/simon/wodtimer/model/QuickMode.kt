package com.simon.wodtimer.model

object QuickMode {

    const val PREP_SECONDS = 10
    private const val TABATA_ROUNDS = 8
    private const val TABATA_WORK_SECONDS = 20
    private const val TABATA_REST_SECONDS = 10

    enum class TimerMode { COUNT_DOWN, COUNT_UP, REPEAT }

    enum class EmomInterval(val label: String, val seconds: Int) {
        ONE_MINUTE("1 min", 60),
        NINETY_SECONDS("1.5 min", 90),
        TWO_MINUTES("2 min", 120)
    }

    fun clock(): RunSpec = RunSpec.CountUp("Clock", PREP_SECONDS, RunSpec.NO_TARGET)

    fun emom(intervalSeconds: Int): RunSpec = RunSpec.InfiniteEmom("EMOM", intervalSeconds, PREP_SECONDS)

    fun tabata(): RunSpec {
        val phases = mutableListOf<TimerPhase>()
        phases += TimerPhase(PhaseKind.PREP, PREP_SECONDS, 0, TABATA_ROUNDS, "Get ready")
        for (round in 1..TABATA_ROUNDS) {
            phases += TimerPhase(PhaseKind.WORK, TABATA_WORK_SECONDS, round, TABATA_ROUNDS, "Work")
            phases += TimerPhase(PhaseKind.REST, TABATA_REST_SECONDS, round, TABATA_ROUNDS, "Rest")
        }
        return RunSpec.Fixed("Tabata", phases)
    }

    fun timer(totalSeconds: Int, mode: TimerMode): RunSpec = when (mode) {
        TimerMode.COUNT_DOWN -> {
            val phases = listOf(
                TimerPhase(PhaseKind.PREP, PREP_SECONDS, 0, 1, "Get ready"),
                TimerPhase(PhaseKind.INTERVAL, totalSeconds, 1, 1, "Timer")
            )
            RunSpec.Fixed("Timer", phases)
        }
        TimerMode.COUNT_UP -> RunSpec.CountUp("Timer", PREP_SECONDS, totalSeconds)
        TimerMode.REPEAT -> RunSpec.Repeat("Timer", PREP_SECONDS, totalSeconds)
    }
}
