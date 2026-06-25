package com.simon.wodtimer.platform

import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

actual fun setImmersive(enabled: Boolean) {
    val activity = AndroidPlatform.currentActivity ?: return
    activity.runOnUiThread {
        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (enabled) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            controller.show(WindowInsetsCompat.Type.navigationBars())
        }
    }
}
