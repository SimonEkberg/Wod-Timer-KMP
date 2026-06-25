package com.simon.wodtimer.ui

import com.simon.wodtimer.model.PhaseKind
import com.simon.wodtimer.platform.playBeep

class Beeper(private val enabled: Boolean) {

    fun countBeep() {
        if (enabled) playBeep()
    }

    fun phaseBeep(kind: PhaseKind) {
        if (enabled && kind != PhaseKind.PREP && kind != PhaseKind.COUNT_UP) playBeep()
    }

    fun release() {
        // no-op; platform sound is fire-and-forget
    }
}
