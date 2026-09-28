package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun VuMeter(
    level: Float, // 0f..1f
    modifier: Modifier = Modifier,
    width: Dp = 8.dp,
    height: Dp = 120.dp,
    segments: Int = 16
) {
    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF0F0B17))
            .padding(1.5.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalHeight = size.height
            val segHeight = totalHeight / segments
            val gap = 1.dp.toPx()
            val actualSegHeight = segHeight - gap

            val activeSegments = (level.coerceIn(0f, 1f) * segments).toInt()

            for (i in 0 until segments) {
                // Bottom is 0, Top is segments - 1
                val segIndexFromBottom = i
                val segY = totalHeight - (segIndexFromBottom + 1) * segHeight

                val isActive = segIndexFromBottom < activeSegments
                val segColor = when {
                    segIndexFromBottom >= segments * 0.85 -> if (isActive) Color(0xFFFF2A6D) else Color(0x33FF2A6D) // Red/Magenta Peak
                    segIndexFromBottom >= segments * 0.65 -> if (isActive) Color(0xFFFFD166) else Color(0x33FFD166) // Yellow Warning
                    else -> if (isActive) Color(0xFF05FFA1) else Color(0x2205FFA1) // Neon Green Safe
                }

                drawRect(
                    color = segColor,
                    topLeft = Offset(0f, segY),
                    size = Size(size.width, actualSegHeight)
                )
            }
        }
    }
}
