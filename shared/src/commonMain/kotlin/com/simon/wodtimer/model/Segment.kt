package com.simon.wodtimer.model

import kotlinx.serialization.Serializable

@Serializable
enum class SegmentKind { WORK, REST, INTERVAL }

@Serializable
data class Segment(val seconds: Int, val kind: SegmentKind)
