package com.simon.wodtimer.platform

expect object Settings {
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
}
