package com.simon.wodtimer.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun randomId(): String = Uuid.random().toString()
