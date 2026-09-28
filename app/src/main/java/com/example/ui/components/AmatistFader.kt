package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.log10

@Composable
fun AmatistFader(
    value: Float, // 0f..1f
    onValueChange: (Float) -> Unit,
    vuLevel: Float, // 0f..1f
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    faderWidth: Dp = 44.dp,
    label: String = "LEVEL"
) {
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    val dbText = remember(value) {
        if (value <= 0.001f) "-∞ dB"
        else {
            val db = 20 * log10(value)
            "${if (db > 0) "+" else ""}${String.format("%.1f", db)} dB"
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.testTag("fader_$label")
    ) {
        Text(
            text = dbText,
            color = Color(0xFFE1BEE7),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(height)
        ) {
            // dB scale markers
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .height(height)
                    .padding(end = 4.dp)
            ) {
                Text("+6", color = Color(0xFF7E6E99), fontSize = 7.sp)
                Text("0", color = Color(0xFFC4B8DB), fontSize = 7.sp)
                Text("-6", color = Color(0xFF7E6E99), fontSize = 7.sp)
                Text("-12", color = Color(0xFF7E6E99), fontSize = 7.sp)
                Text("-24", color = Color(0xFF7E6E99), fontSize = 7.sp)
                Text("-∞", color = Color(0xFF7E6E99), fontSize = 7.sp)
            }

            // Fader track and draggable cap
            Box(
                modifier = Modifier
                    .width(faderWidth)
                    .height(height)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF140F21))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                accumulatedDrag = value
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val delta = -dragAmount.y / height.toPx()
                                accumulatedDrag = (accumulatedDrag + delta).coerceIn(0f, 1f)
                                onValueChange(accumulatedDrag)
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val trackWidth = 4.dp.toPx()
                    val trackX = (size.width - trackWidth) / 2f
                    // Center slit
                    drawRoundRect(
                        color = Color(0xFF090610),
                        topLeft = Offset(trackX, 8.dp.toPx()),
                        size = Size(trackWidth, size.height - 16.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )

                    // Draw 0dB reference tick
                    val zeroDbY = size.height * 0.25f
                    drawLine(
                        color = Color(0xFFBA68C8),
                        start = Offset(trackX - 6.dp.toPx(), zeroDbY),
                        end = Offset(trackX + trackWidth + 6.dp.toPx(), zeroDbY),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // Fader Cap
                    val thumbHeight = 24.dp.toPx()
                    val thumbWidth = size.width - 8.dp.toPx()
                    val thumbX = 4.dp.toPx()
                    // value 1 is top, value 0 is bottom
                    val thumbY = (1f - value) * (size.height - thumbHeight)

                    // Cap body
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF4A3868), Color(0xFF231838), Color(0xFF181028)),
                            startY = thumbY,
                            endY = thumbY + thumbHeight
                        ),
                        topLeft = Offset(thumbX, thumbY),
                        size = Size(thumbWidth, thumbHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Cap border
                    drawRoundRect(
                        color = Color(0xFF8E73B5),
                        topLeft = Offset(thumbX, thumbY),
                        size = Size(thumbWidth, thumbHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())
                    )

                    // Cap center white line
                    val lineY = thumbY + thumbHeight / 2f
                    drawLine(
                        color = Color.White,
                        start = Offset(thumbX + 3.dp.toPx(), lineY),
                        end = Offset(thumbX + thumbWidth - 3.dp.toPx(), lineY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // VU meter next to fader
            VuMeter(
                level = vuLevel,
                width = 8.dp,
                height = height,
                segments = 16
            )
        }
    }
}
