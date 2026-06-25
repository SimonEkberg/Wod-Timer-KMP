package com.simon.wodtimer.model

import kotlinx.serialization.Serializable

@Serializable
data class Workout(
    val id: String = randomId(),
    val name: String,
    val rounds: Int,
    val segmentsPerRound: List<Segment>,
    val prepSeconds: Int = 10
) {
    val totalSeconds: Int
        get() = prepSeconds + rounds * segmentsPerRound.sumOf { it.seconds }
}
