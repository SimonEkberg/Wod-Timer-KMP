package com.simon.wodtimer.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual object NoteImageStore {

    actual fun decode(path: String, maxDim: Int): ImageBitmap? {
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        val length = data.length.toInt()
        if (length <= 0) return null
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length.convert())
        }
        return try {
            Image.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (e: Throwable) {
            null
        }
    }

    actual fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        NSFileManager.defaultManager.removeItemAtPath(path, null)
    }
}
