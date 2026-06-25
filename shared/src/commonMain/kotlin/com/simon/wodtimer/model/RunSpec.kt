package com.simon.wodtimer.model

sealed interface RunSpec {
    val title: String

    data class Fixed(override val title: String, val phases: List<TimerPhase>) : RunSpec

    data class CountUp(override val title: String, val prepSeconds: Int, val targetSeconds: Int) : RunSpec

    data class Repeat(override val title: String, val prepSeconds: Int, val intervalSeconds: Int) : RunSpec

    data class InfiniteEmom(override val title: String, val intervalSeconds: Int, val prepSeconds: Int) : RunSpec

    companion object {
        const val NO_TARGET = 0
        fun fromWorkout(workout: Workout): Fixed = Fixed(workout.name, TimerPlan.expand(workout))
    }
}
