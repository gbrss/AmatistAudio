package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
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
import com.example.model.DrumType
import com.example.model.Machine
import com.example.model.MachineType
import com.example.ui.components.DrumStepSequencer
import com.example.ui.components.PianoRollView

@Composable
fun PatternScreen(
    machines: List<Machine>,
    selectedMachineId: String,
    currentStep: Int,
    keyboardOctave: Int,
    onSelectMachine: (String) -> Unit,
    onSelectPattern: (machineId: String, patternId: String) -> Unit,
    onClearPattern: (machineId: String, patternId: String) -> Unit,
    onToggleDrumStep: (machineId: String, patternId: String, drumType: DrumType, step: Int) -> Unit,
    onToggleSynthNote: (machineId: String, patternId: String, step: Int, pitch: Int) -> Unit,
    onAuditionDrum: (DrumType) -> Unit,
    onOctaveChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedMachine = machines.find { it.id == selectedMachineId } ?: machines.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0816))
            .padding(8.dp)
    ) {
        // Machine selector strip
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            items(machines) { m ->
                val isSelected = m.id == selectedMachineId
                val mColor = Color(m.colorHex)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFF2C1A4A) else Color(0xFF160F26))
                        .border(
                            1.dp,
                            if (isSelected) mColor else Color(0xFF332050),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectMachine(m.id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("pattern_tab_machine_${m.id}")
                ) {
                    Text(
                        text = "${m.name} (${m.activePatternId})",
                        color = if (isSelected) Color.White else Color(0xFFB39DDB),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        if (selectedMachine != null) {
            val activePattern = selectedMachine.patterns.find { it.id == selectedMachineId }
                ?: selectedMachine.patterns.find { it.id == selectedMachine.activePatternId }
                ?: selectedMachine.patterns.firstOrNull()

            val currentPatternId = activePattern?.id ?: "A1"

            // Bank & Pattern Selection Bar (A1 .. D8)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF181029))
                    .border(1.dp, Color(0xFF352254), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // Bank and number buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val currentBank = currentPatternId.take(1)
                    val currentNum = currentPatternId.drop(1).toIntOrNull() ?: 1

                    // Banks A, B, C, D
                    listOf("A", "B", "C", "D").forEach { bank ->
                        val isBank = currentBank == bank
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isBank) Color(0xFF7B1FA2) else Color(0xFF23163B))
                                .clickable {
                                    onSelectPattern(selectedMachine.id, "$bank$currentNum")
                                }
                                .testTag("bank_btn_$bank")
                        ) {
                            Text(
                                text = bank,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Pattern numbers 1..8
                    (1..8).forEach { num ->
                        val isNum = currentNum == num
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isNum) Color(0xFF00E5FF) else Color(0xFF23163B))
                                .clickable {
                                    onSelectPattern(selectedMachine.id, "$currentBank$num")
                                }
                                .testTag("pattern_num_btn_$num")
                        ) {
                            Text(
                                text = "$num",
                                color = if (isNum) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                }

                // Clear button
                IconButton(
                    onClick = { onClearPattern(selectedMachine.id, currentPatternId) },
                    modifier = Modifier.size(28.dp).testTag("clear_pattern_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear Pattern",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Octave controls if synth
            if (selectedMachine.type != MachineType.BEATBOX) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PIANO ROLL (16 STEPS)",
                        color = Color(0xFFC4B8DB),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { onOctaveChange(keyboardOctave - 1) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26193E)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("- OCT", fontSize = 9.sp, color = Color(0xFFCE93D8))
                        }
                        Text(
                            text = " C$keyboardOctave ",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Button(
                            onClick = { onOctaveChange(keyboardOctave + 1) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26193E)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("+ OCT", fontSize = 9.sp, color = Color(0xFFCE93D8))
                        }
                    }
                }
            }

            // Pattern Sequencer Body
            Box(modifier = Modifier.weight(1f)) {
                if (selectedMachine.type == MachineType.BEATBOX) {
                    val lanes = activePattern?.drumLanes ?: emptyList()
                    DrumStepSequencer(
                        drumLanes = lanes,
                        currentStep = currentStep,
                        onToggleStep = { drumType, step ->
                            onToggleDrumStep(selectedMachine.id, currentPatternId, drumType, step)
                        },
                        onTriggerAudition = onAuditionDrum,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val notes = activePattern?.notes ?: emptyList()
                    PianoRollView(
                        notes = notes,
                        currentStep = currentStep,
                        baseOctave = keyboardOctave,
                        onNoteToggle = { step, pitch ->
                            onToggleSynthNote(selectedMachine.id, currentPatternId, step, pitch)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
