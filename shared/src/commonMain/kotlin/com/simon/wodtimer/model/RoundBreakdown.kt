package com.simon.wodtimer.model

data class RoundBreakdown(
    val round: Int,
    val durationMillis: Long,
    val diffMillis: Long,
    val hasDiff: Boolean,
    val reps: Int = 0
)

data class IntervalGroup(
    val interval: Int,
    val rounds: List<RoundBreakdown>,
    val averageMillis: Long
)

object RoundBreakdowns {

    fun from(splits: List<RoundSplit>): List<RoundBreakdown> {
        val result = mutableListOf<RoundBreakdown>()
        var previousAt = 0L
        var previousDuration = 0L
        splits.forEachIndexed { index, split ->
            val duration = maxOf(0L, split.atMillis - previousAt)
            val hasDiff = index > 0
            val diff = if (hasDiff) duration - previousDuration else 0L
            result += RoundBreakdown(split.round, duration, diff, hasDiff, split.reps)
            previousAt = split.atMillis
            previousDuration = duration
        }
        return result
    }

    fun grouped(splits: List<RoundSplit>): List<IntervalGroup> {
        if (splits.isEmpty()) return emptyList()
        if (splits.all { it.interval == 0 }) {
            val rounds = from(splits)
            return listOf(IntervalGroup(0, rounds, averageMillis(rounds)))
        }
        return splits.groupBy { it.interval }
            .toSortedMap()
            .map { (interval, group) ->
                val rounds = mutableListOf<RoundBreakdown>()
                val baseline = group.firstOrNull()?.intervalStartMillis ?: 0L
                var previousAt = baseline
                var previousDuration = 0L
                group.forEachIndexed { index, split ->
                    val duration = maxOf(0L, split.atMillis - previousAt)
                    val hasDiff = index > 0
                    val diff = if (hasDiff) duration - previousDuration else 0L
                    rounds += RoundBreakdown(split.round, duration, diff, hasDiff, split.reps)
                    previousAt = split.atMillis
                    previousDuration = duration
                }
                IntervalGroup(interval, rounds, averageMillis(rounds))
            }
    }

    fun averageMillis(breakdowns: List<RoundBreakdown>): Long {
        if (breakdowns.isEmpty()) return 0L
        return breakdowns.sumOf { it.durationMillis } / breakdowns.size
    }

    fun anyReps(breakdowns: List<RoundBreakdown>): Boolean = breakdowns.any { it.reps > 0 }
}
