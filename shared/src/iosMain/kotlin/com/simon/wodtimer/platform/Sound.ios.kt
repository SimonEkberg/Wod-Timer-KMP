package com.simon.wodtimer.platform

import platform.AudioToolbox.AudioServicesPlaySystemSound

// 1057 = "Tink" system sound. A bundled custom tone can replace this later.
private const val SYSTEM_SOUND_TINK: UInt = 1057u

actual fun playBeep() {
    AudioServicesPlaySystemSound(SYSTEM_SOUND_TINK)
}
