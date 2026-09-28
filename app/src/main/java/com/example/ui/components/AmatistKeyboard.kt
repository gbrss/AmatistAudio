package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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

@Composable
fun AmatistKeyboard(
    octave: Int,
    onOctaveChange: (Int) -> Unit,
    onNoteOn: (pitch: Int) -> Unit,
    onNoteOff: (pitch: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 2 octaves = 24 semitones
    val rootPitch = octave * 12

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0A19))
            .border(1.dp, Color(0xFF2E1C4D))
            .padding(4.dp)
    ) {
        // Octave selector bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "VIRTUAL KEYBOARD",
                color = Color(0xFFC4B8DB),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { onOctaveChange(octave - 1) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B1D42)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("octave_down_btn")
                ) {
                    Text("OCT -", fontSize = 10.sp, color = Color(0xFFCE93D8))
                }

                Text(
                    text = " C$octave ",
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )

                Button(
                    onClick = { onOctaveChange(octave + 1) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B1D42)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("octave_up_btn")
                ) {
                    Text("OCT +", fontSize = 10.sp, color = Color(0xFFCE93D8))
                }
            }
        }

        // 2 Octaves of Keys: White keys row with overlaid black keys
        // White notes in 2 octaves: 14 white keys
        // Octave 0: C, D, E, F, G, A, B (7)
        // Octave 1: C, D, E, F, G, A, B (7)
        val whiteNotes = remember(rootPitch) {
            listOf(
                rootPitch + 0, rootPitch + 2, rootPitch + 4, rootPitch + 5,
                rootPitch + 7, rootPitch + 9, rootPitch + 11,
                rootPitch + 12, rootPitch + 14, rootPitch + 16, rootPitch + 17,
                rootPitch + 19, rootPitch + 21, rootPitch + 23
            )
        }

        // Relative white key index where black keys appear:
        // C#=0, D#=1, F#=3, G#=4, A#=5, C#=7, D#=8, F#=10, G#=11, A#=12
        val blackNotes = remember(rootPitch) {
            listOf(
                0 to rootPitch + 1,
                1 to rootPitch + 3,
                3 to rootPitch + 6,
                4 to rootPitch + 8,
                5 to rootPitch + 10,
                7 to rootPitch + 13,
                8 to rootPitch + 15,
                10 to rootPitch + 18,
                11 to rootPitch + 20,
                12 to rootPitch + 22
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val totalWidth = maxWidth
            val whiteKeyWidth = totalWidth / 14

            // White keys
            Row(modifier = Modifier.fillMaxSize()) {
                whiteNotes.forEach { pitch ->
                    var isPressed by remember { mutableStateOf(false) }

                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .width(whiteKeyWidth)
                            .fillMaxHeight()
                            .padding(horizontal = 0.5.dp)
                            .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                            .background(if (isPressed) Color(0xFFD1C4E9) else Color(0xFFF3E5F5))
                            .border(
                                1.dp,
                                Color(0xFF6A4C93),
                                RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
                            )
                            .pointerInput(pitch) {
                                detectTapGestures(
                                    onPress = {
                                        isPressed = true
                                        onNoteOn(pitch)
                                        tryAwaitRelease()
                                        isPressed = false
                                        onNoteOff(pitch)
                                    }
                                )
                            }
                            .testTag("key_white_$pitch")
                    ) {
                        if (pitch % 12 == 0) {
                            Text(
                                text = "C${pitch / 12 - 1}",
                                color = Color(0xFF4A148C),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }

            // Black keys overlay
            val blackKeyWidth = whiteKeyWidth * 0.65f
            val blackKeyHeight = 65.dp

            blackNotes.forEach { (whiteIndex, pitch) ->
                var isPressed by remember { mutableStateOf(false) }
                val leftOffset = whiteKeyWidth * (whiteIndex + 1) - (blackKeyWidth / 2f)

                Box(
                    modifier = Modifier
                        .offset(x = leftOffset)
                        .width(blackKeyWidth)
                        .height(blackKeyHeight)
                        .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(if (isPressed) Color(0xFF7B1FA2) else Color(0xFF1E1033))
                        .border(
                            1.dp,
                            if (isPressed) Color(0xFFE1BEE7) else Color(0xFF5E35B1),
                            RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)
                        )
                        .pointerInput(pitch) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    onNoteOn(pitch)
                                    tryAwaitRelease()
                                    isPressed = false
                                    onNoteOff(pitch)
                                }
                            )
                        }
                        .testTag("key_black_$pitch")
                )
            }
        }
    }
}
