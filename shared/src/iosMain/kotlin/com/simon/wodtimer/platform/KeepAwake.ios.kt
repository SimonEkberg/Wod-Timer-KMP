package com.simon.wodtimer.platform

import platform.UIKit.UIApplication

actual fun setKeepAwake(enabled: Boolean) {
    UIApplication.sharedApplication.idleTimerDisabled = enabled
}
