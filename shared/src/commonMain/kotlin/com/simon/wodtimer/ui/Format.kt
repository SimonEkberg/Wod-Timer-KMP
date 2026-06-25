package com.simon.wodtimer.ui

import com.simon.wodtimer.model.SegmentKind
import com.simon.wodtimer.model.Workout

private fun Int.pad2(): String = if (this < 10) "0$this" else this.toString()

private fun Long.pad2(): String = if (this < 10) "0$this" else this.toString()

fun formatMmSs(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "$m:${s.pad2()}"
}

fun formatMmSsCc(totalMillis: Long): String {
    val safeMillis = totalMillis.coerceAtLeast(0L)
    val totalCentis = safeMillis / 10
    val centis = totalCentis % 100
    val totalSeconds = totalCentis / 100
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "${m.pad2()}:${s.pad2()}.${centis.pad2()}"
}

fun formatDiffSeconds(diffMillis: Long): String {
    if (diffMillis == 0L) return "0 s"
    val sign = if (diffMillis > 0) "+" else "-"
    val tenths = (kotlin.math.abs(diffMillis) + 50) / 100
    val whole = tenths / 10
    val frac = tenths % 10
    return "$sign$whole.$frac s"
}

fun workoutSummary(workout: Workout): String {
    val isIntervalList = workout.segmentsPerRound.all { it.kind == SegmentKind.INTERVAL }
    if (isIntervalList) {
        val count = workout.segmentsPerRound.size
        return "${workout.rounds} × $count intervals"
    }
    val parts = workout.segmentsPerRound.joinToString(" / ") { segment ->
        val tag = when (segment.kind) {
            SegmentKind.WORK -> "work"
            SegmentKind.REST -> "rest"
            SegmentKind.INTERVAL -> "interval"
        }
        "${formatMmSs(segment.seconds)} $tag"
    }
    return "${workout.rounds} × [$parts]"
}
