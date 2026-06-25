package com.simon.wodtimer.platform

import androidx.compose.runtime.Composable

@Composable
expect fun AppBackHandler(enabled: Boolean, onBack: () -> Unit)
