package com.simon.wodtimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simon.wodtimer.platform.Settings
import com.simon.wodtimer.platform.playBeep
import com.simon.wodtimer.platform.platformName
import com.simon.wodtimer.platform.setKeepAwake
import kotlinx.coroutines.delay
import kotlin.time.TimeSource

private val WodDarkColors = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF00131A),
    background = Color(0xFF0A0E14),
    onBackground = Color(0xFFE6F4F1),
    surface = Color(0xFF11161F),
    onSurface = Color(0xFFE6F4F1)
)

private const val TAP_COUNT_KEY = "tap_count"

@Composable
fun App() {
    MaterialTheme(colorScheme = WodDarkColors) {
        var running by remember { mutableStateOf(false) }
        var elapsedMillis by remember { mutableStateOf(0L) }
        var taps by remember { mutableIntStateOf(Settings.getInt(TAP_COUNT_KEY, 0)) }
        var keepAwake by remember { mutableStateOf(false) }

        LaunchedEffect(running) {
            if (running) {
                val start = TimeSource.Monotonic.markNow()
                val base = elapsedMillis
                while (running) {
                    elapsedMillis = base + start.elapsedNow().inWholeMilliseconds
                    delay(33)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text("WOD Timer", color = Color.White, fontSize = 32.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text("Kotlin Multiplatform · ${platformName()}", color = Color(0xFF4DD0E1), fontSize = 14.sp, textAlign = TextAlign.Center)

            Text(formatMmSsCc(elapsedMillis), color = Color.White, fontSize = 56.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)

            Button(onClick = { running = !running }) {
                Text(if (running) "Pause" else "Start")
            }
            Button(onClick = { running = false; elapsedMillis = 0L }) {
                Text("Reset")
            }

            Button(onClick = {
                taps += 1
                Settings.putInt(TAP_COUNT_KEY, taps)
                playBeep()
            }) {
                Text("Beep + count: $taps")
            }
            Text("(count persists across launches)", color = Color(0xFFB9C4CF), fontSize = 12.sp)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(checked = keepAwake, onCheckedChange = { keepAwake = it; setKeepAwake(it) })
                Text("Keep screen awake", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

private fun formatMmSsCc(totalMillis: Long): String {
    val v = if (totalMillis < 0) 0L else totalMillis
    val totalCentis = v / 10
    val centis = totalCentis % 100
    val totalSeconds = totalCentis / 100
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    val mm = m.toString().padStart(2, '0')
    val ss = s.toString().padStart(2, '0')
    val cc = centis.toString().padStart(2, '0')
    return "$mm:$ss.$cc"
}
