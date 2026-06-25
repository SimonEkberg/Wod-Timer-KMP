package com.simon.wodtimer.platform

import androidx.compose.ui.graphics.ImageBitmap

expect object NoteImageStore {
    fun decode(path: String, maxDim: Int): ImageBitmap?
    fun delete(path: String?)
}
