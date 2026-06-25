package com.simon.wodtimer.platform

import androidx.compose.runtime.Composable

// Returns a launch function; invoking it opens the platform photo picker.
// On selection the image is copied into app storage and its path is returned via onPicked.
@Composable
expect fun rememberImagePicker(onPicked: (String) -> Unit): () -> Unit
