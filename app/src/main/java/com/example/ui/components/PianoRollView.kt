package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Note

@Composable
fun PianoRollView(
    notes: List<Note>,
    currentStep: Int,
    baseOctave: Int,
    onNoteToggle: (step: Int, pitch: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 2 octaves range (24 semitones) based on baseOctave
    // baseOctave 3 => MIDI note 48 (C3) up to 71 (B4)
    val rootPitch = baseOctave * 12
    val pitches = remember(rootPitch) {
        (rootPitch + 23 downTo rootPitch).toList() // High notes at top, low at bottom
    }

    val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val blackKeyIndices = setOf(1, 3, 6, 8, 10)

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F0B18))
            .border(1.dp, Color(0xFF2C1B47), RoundedCornerShape(8.dp))
    ) {
        // Left Column: Piano Keys
        Column(
            modifier = Modifier
                .width(52.dp)
                .verticalScroll(verticalScrollState)
                .background(Color(0xFF161024))
        ) {
            pitches.forEach { pitch ->
                val semitone = pitch % 12
                val oct = pitch / 12 - 1
                val isBlack = blackKeyIndices.contains(semitone)
                val name = "${noteNames[semitone]}$oct"

                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .border(0.5.dp, Color(0xFF2C1E42))
                        .background(if (isBlack) Color(0xFF1C132E) else Color(0xFF32244F))
                        .padding(start = 6.dp)
                ) {
                    Text(
                        text = name,
                        color = if (isBlack) Color(0xFFBA68C8) else Color(0xFFFFFFFF),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (semitone == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Right Area: 16 Step Matrix
        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(horizontalScrollState)
                .verticalScroll(verticalScrollState)
        ) {
            Column {
                pitches.forEach { pitch ->
                    val semitone = pitch % 12
                    val isBlack = blackKeyIndices.contains(semitone)

                    Row(modifier = Modifier.height(24.dp)) {
                        for (step in 0 until 16) {
                            val isStepHeader = step % 4 == 0
                            val isCurrentStep = step == currentStep
                            val hasNote = notes.any { it.step == step && it.pitch == pitch }

                            val cellBg = when {
                                hasNote -> Color(0xFFE040FB)
                                isCurrentStep -> Color(0x3300E5FF)
                                isStepHeader -> if (isBlack) Color(0xFF191226) else Color(0xFF221735)
                                else -> if (isBlack) Color(0xFF130D1E) else Color(0xFF1A112B)
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(24.dp)
                                    .border(
                                        width = if (isCurrentStep) 1.dp else 0.5.dp,
                                        color = if (isCurrentStep) Color(0xFF00E5FF) else Color(0xFF2E2045)
                                    )
                                    .background(cellBg)
                                    .clickable {
                                        onNoteToggle(step, pitch)
                                    }
                                    .testTag("pianoroll_cell_${step}_$pitch")
                            ) {
                                if (hasNote) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFFE040FB))
                                            .border(1.dp, Color.White, RoundedCornerShape(3.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
