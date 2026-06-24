package com.simon.wodtimer.platform

import android.app.Activity
import android.content.Context

object AndroidPlatform {
    lateinit var appContext: Context
    var currentActivity: Activity? = null
}
