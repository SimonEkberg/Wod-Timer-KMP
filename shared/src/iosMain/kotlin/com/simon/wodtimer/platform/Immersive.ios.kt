package com.simon.wodtimer.platform

// iOS has no Android-style navigation bar to hide; the run screen simply uses the full
// safe area. No-op by design.
actual fun setImmersive(enabled: Boolean) {
    // no-op on iOS
}
