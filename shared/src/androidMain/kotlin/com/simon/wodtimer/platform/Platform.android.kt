package com.simon.wodtimer.platform

import android.os.Build

actual fun platformName(): String = "Android ${Build.VERSION.RELEASE}"
