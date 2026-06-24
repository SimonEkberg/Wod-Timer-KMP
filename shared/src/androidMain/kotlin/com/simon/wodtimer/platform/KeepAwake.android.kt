package com.simon.wodtimer.platform

import android.view.WindowManager

actual fun setKeepAwake(enabled: Boolean) {
    val activity = AndroidPlatform.currentActivity ?: return
    activity.runOnUiThread {
        if (enabled) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
