package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AmatistKnob(
    label: String,
    value: Float, // 0f..1f
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    displayValue: String? = null,
    accentColor: Color = Color(0xFFBA68C8),
    minAngle: Float = 135f,
    maxAngle: Float = 405f
) {
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.testTag("knob_$label")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .pointerInput(label) {
                    detectDragGestures(
                        onDragStart = {
                            accumulatedDrag = value
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // Drag up to increase, down to decrease
                            val delta = -dragAmount.y * 0.006f
                            accumulatedDrag = (accumulatedDrag + delta).coerceIn(0f, 1f)
                            onValueChange(accumulatedDrag)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 4.dp.toPx()
                val radius = (size.toPx() - strokeWidth * 2) / 2f
                val center = Offset(size.toPx() / 2f, size.toPx() / 2f)

                // Background track arc
                val totalSweep = maxAngle - minAngle
                drawArc(
                    color = Color(0xFF221A33),
                    startAngle = minAngle,
                    sweepAngle = totalSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active value arc
                val currentSweep = totalSweep * value.coerceIn(0f, 1f)
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color(0xFF7B1FA2), accentColor, Color(0xFFE1BEE7))
                    ),
                    startAngle = minAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth + 1.dp.toPx(), cap = StrokeCap.Round)
                )

                // Inner Knob Cap (brushed dark metal with amethyst reflection)
                val innerRadius = radius * 0.72f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF3B2D54), Color(0xFF1B1429), Color(0xFF0F0B18)),
                        center = center,
                        radius = innerRadius
                    ),
                    center = center,
                    radius = innerRadius
                )

                // Outer border ring of knob cap
                drawCircle(
                    color = Color(0xFF553D75),
                    center = center,
                    radius = innerRadius,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Pointer indicator line
                val currentAngleDeg = minAngle + currentSweep
                val currentAngleRad = Math.toRadians(currentAngleDeg.toDouble())
                val pointerStart = Offset(
                    (center.x + innerRadius * 0.35f * cos(currentAngleRad)).toFloat(),
                    (center.y + innerRadius * 0.35f * sin(currentAngleRad)).toFloat()
                )
                val pointerEnd = Offset(
                    (center.x + (innerRadius - 2.dp.toPx()) * cos(currentAngleRad)).toFloat(),
                    (center.y + (innerRadius - 2.dp.toPx()) * sin(currentAngleRad)).toFloat()
                )
                drawLine(
                    color = Color.White,
                    start = pointerStart,
                    end = pointerEnd,
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Value text
        val formattedVal = displayValue ?: "${(value * 100).toInt()}%"
        Text(
            text = formattedVal,
            color = accentColor,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        // Parameter Label
        Text(
            text = label.uppercase(),
            color = Color(0xFFC4B8DB),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
