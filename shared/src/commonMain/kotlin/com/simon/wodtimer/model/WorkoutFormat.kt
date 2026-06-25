package com.simon.wodtimer.model

object WorkoutFormat {

    private const val SEPARATOR = "|"
    private const val FLAG_NO_PREP = 0
    private const val FLAG_LONG_PREP = 2
    private const val DEFAULT_PREP_SECONDS = 10
    private const val LONG_PREP_SECONDS = 10

    fun parse(name: String, encoded: String): Workout {
        val parts = encoded.split(SEPARATOR).map { it.trim().toInt() }
        require(parts.size >= 3) { "Encoded workout too short: $encoded" }

        val rounds = parts[0]
        val segCount = parts[1]
        val durations = parts.subList(2, 2 + segCount)
        val flag = parts.getOrElse(2 + segCount) { FLAG_NO_PREP }

        val segments = durations.mapIndexed { index, seconds ->
            val kind = if (segCount == 1 || index % 2 == 0) SegmentKind.WORK else SegmentKind.REST
            Segment(seconds, kind)
        }
        val prep = if (flag == FLAG_LONG_PREP) LONG_PREP_SECONDS else DEFAULT_PREP_SECONDS
        return Workout(name = name, rounds = rounds, segmentsPerRound = segments, prepSeconds = prep)
    }

    fun encode(workout: Workout): String {
        val flag = if (workout.prepSeconds >= LONG_PREP_SECONDS) FLAG_LONG_PREP else FLAG_NO_PREP
        val durations = workout.segmentsPerRound.joinToString(SEPARATOR) { it.seconds.toString() }
        return listOf(workout.rounds.toString(), workout.segmentsPerRound.size.toString(), durations, flag.toString())
            .joinToString(SEPARATOR)
    }
}
