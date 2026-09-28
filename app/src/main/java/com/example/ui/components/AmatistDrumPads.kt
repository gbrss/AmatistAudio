package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DrumType

@Composable
fun AmatistDrumPads(
    onTriggerDrum: (DrumType) -> Unit,
    modifier: Modifier = Modifier
) {
    val drumPads = listOf(
        DrumType.KICK, DrumType.SNARE, DrumType.CLOSED_HAT, DrumType.OPEN_HAT,
        DrumType.CLAP, DrumType.LOW_TOM, DrumType.HIGH_TOM, DrumType.CRASH
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0A19))
            .border(1.dp, Color(0xFF2E1C4D))
            .padding(6.dp)
    ) {
        Text(
            text = "LIVE BEATBOX PADS",
            color = Color(0xFFC4B8DB),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // 2 rows of 4 pads
        for (row in 0 until 2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                for (col in 0 until 4) {
                    val index = row * 4 + col
                    val drum = drumPads[index]
                    var isPressed by remember { mutableStateOf(false) }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isPressed) Color(0xFFFF5252) else Color(0xFF261A3B)
                            )
                            .border(
                                1.dp,
                                if (isPressed) Color.White else Color(0xFF5B3C84),
                                RoundedCornerShape(6.dp)
                            )
                            .pointerInput(drum) {
                                detectTapGestures(
                                    onPress = {
                                        isPressed = true
                                        onTriggerDrum(drum)
                                        tryAwaitRelease()
                                        isPressed = false
                                    }
                                )
                            }
                            .testTag("drumpad_${drum.shortName}")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = drum.shortName,
                                color = if (isPressed) Color.White else Color(0xFFFF8A80),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = drum.title,
                                color = if (isPressed) Color.White else Color(0xFF9E8DB8),
                                fontSize = 8.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
