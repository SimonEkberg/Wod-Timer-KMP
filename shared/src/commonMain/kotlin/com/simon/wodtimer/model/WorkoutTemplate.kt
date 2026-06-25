package com.simon.wodtimer.model

enum class WorkoutTemplate(val displayName: String) {
    EMOM("EMOM"),
    AMRAP("AMRAP"),
    TABATA("Tabata"),
    FOR_TIME("For Time"),
    INTERVAL("Interval (work/rest)");

    fun build(name: String, rounds: Int, workSeconds: Int, restSeconds: Int): Workout = when (this) {
        EMOM -> Workout(
            name = name, rounds = rounds,
            segmentsPerRound = listOf(Segment(workSeconds, SegmentKind.WORK))
        )
        AMRAP -> Workout(
            name = name, rounds = 1,
            segmentsPerRound = listOf(Segment(workSeconds, SegmentKind.WORK))
        )
        FOR_TIME -> Workout(
            name = name, rounds = 1,
            segmentsPerRound = listOf(Segment(workSeconds, SegmentKind.WORK))
        )
        TABATA -> Workout(
            name = name, rounds = rounds,
            segmentsPerRound = listOf(Segment(workSeconds, SegmentKind.WORK), Segment(restSeconds, SegmentKind.REST))
        )
        INTERVAL -> Workout(
            name = name, rounds = rounds,
            segmentsPerRound = listOf(Segment(workSeconds, SegmentKind.WORK), Segment(restSeconds, SegmentKind.REST))
        )
    }

    companion object {
        fun defaultsFor(template: WorkoutTemplate): Triple<Int, Int, Int> = when (template) {
            EMOM -> Triple(10, 60, 0)
            AMRAP -> Triple(1, 8 * 60, 0)
            FOR_TIME -> Triple(1, 12 * 60, 0)
            TABATA -> Triple(8, 20, 10)
            INTERVAL -> Triple(10, 30, 30)
        }
    }
}
