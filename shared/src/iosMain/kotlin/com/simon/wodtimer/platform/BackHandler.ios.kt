package com.simon.wodtimer.platform

import androidx.compose.runtime.Composable

// iOS has no hardware back button; in-app navigation is driven by on-screen controls.
@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // no-op on iOS
}
