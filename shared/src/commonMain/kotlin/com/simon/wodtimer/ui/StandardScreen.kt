package com.simon.wodtimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ClockColors = listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
private val TimerColors = listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))
private val EmomColors = listOf(Color(0xFFE65100), Color(0xFFFFB74D))
private val TabataColors = listOf(Color(0xFFC62828), Color(0xFFEF5350))

@Composable
fun StandardScreen(
    onClock: () -> Unit,
    onTimer: () -> Unit,
    onEmom: () -> Unit,
    onTabata: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Tile("CLOCK", ClockColors, Modifier.weight(1f), onClock)
            Tile("TIMER", TimerColors, Modifier.weight(1f), onTimer)
        }
        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Tile("EMOM", EmomColors, Modifier.weight(1f), onEmom)
            Tile("TABATA", TabataColors, Modifier.weight(1f), onTabata)
        }
    }
}

@Composable
private fun Tile(label: String, colors: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
