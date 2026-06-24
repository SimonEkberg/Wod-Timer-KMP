package com.simon.wodtimer.platform

import android.media.AudioManager
import android.media.ToneGenerator

private val toneGenerator by lazy { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }

actual fun playBeep() {
    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
}
