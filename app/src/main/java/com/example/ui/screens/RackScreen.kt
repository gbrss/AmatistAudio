package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DrumType
import com.example.model.Machine
import com.example.model.MachineType
import com.example.ui.components.AmatistDrumPads
import com.example.ui.components.AmatistKeyboard
import com.example.ui.components.AmatistKnob

@Composable
fun RackScreen(
    machines: List<Machine>,
    selectedMachineId: String,
    keyboardOctave: Int,
    onSelectMachine: (String) -> Unit,
    onAddMachineClick: () -> Unit,
    onRemoveMachine: (String) -> Unit,
    onParamChange: (machineId: String, paramKey: String, value: Float) -> Unit,
    onVolumeChange: (machineId: String, volume: Float) -> Unit,
    onPanChange: (machineId: String, pan: Float) -> Unit,
    onToggleMute: (machineId: String) -> Unit,
    onToggleSolo: (machineId: String) -> Unit,
    onOctaveChange: (Int) -> Unit,
    onNoteOn: (machineId: String, pitch: Int) -> Unit,
    onNoteOff: (machineId: String, pitch: Int) -> Unit,
    onTriggerDrum: (DrumType) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedMachine = machines.find { it.id == selectedMachineId } ?: machines.firstOrNull()

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF0C0816))) {
        // Machine Rack Slot Selector Header
        Surface(
            color = Color(0xFF140D24),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFF2F1F4F))
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(machines) { m ->
                    val isSelected = m.id == selectedMachineId
                    val machineColor = Color(m.colorHex)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color(0xFF2B1947) else Color(0xFF18102B))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) machineColor else Color(0xFF382559),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onSelectMachine(m.id) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("machine_slot_${m.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(machineColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = m.name,
                            color = if (isSelected) Color.White else Color(0xFFC4B8DB),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = m.type.shortName,
                            color = machineColor,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Add machine button
                item {
                    IconButton(
                        onClick = onAddMachineClick,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF22163A))
                            .border(1.dp, Color(0xFF4A3275), RoundedCornerShape(6.dp))
                            .testTag("add_machine_slot_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Machine",
                            tint = Color(0xFFCE93D8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Active Machine Panel
        if (selectedMachine != null) {
            val machineColor = Color(selectedMachine.colorHex)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                // Rack Mount Unit Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1B122E), Color(0xFF130C21), Color(0xFF0F091A))
                            )
                        )
                        .border(1.dp, Color(0xFF3B255E), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        // Machine Faceplate Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Rack Ears & Machine Brand Badge
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Screw aesthetic
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF463363))
                                        .border(1.dp, Color(0xFF7E6E99), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = selectedMachine.name.uppercase(),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "AMATIST AUDIO • ${selectedMachine.type.title.uppercase()}",
                                        color = machineColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Quick controls: Mute, Solo, Remove
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onToggleMute(selectedMachine.id) },
                                    modifier = Modifier.size(28.dp).testTag("rack_mute_btn")
                                ) {
                                    Icon(
                                        imageVector = if (selectedMachine.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                        contentDescription = "Mute",
                                        tint = if (selectedMachine.isMuted) Color(0xFFFF5252) else Color(0xFFBA68C8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Button(
                                    onClick = { onToggleSolo(selectedMachine.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedMachine.isSolo) Color(0xFFFFD54F) else Color(0xFF26193E)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(24.dp).testTag("rack_solo_btn")
                                ) {
                                    Text(
                                        text = "S",
                                        color = if (selectedMachine.isSolo) Color.Black else Color(0xFFC4B8DB),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (machines.size > 1) {
                                    IconButton(
                                        onClick = { onRemoveMachine(selectedMachine.id) },
                                        modifier = Modifier.size(28.dp).testTag("rack_remove_machine_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Machine",
                                            tint = Color(0xFF7E6E99),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Divider(
                            color = Color(0xFF332050),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        // Machine-specific knobs and controls
                        when (selectedMachine.type) {
                            MachineType.SUBSYNTH -> {
                                SubSynthControls(
                                    params = selectedMachine.params,
                                    onParamChange = { k, v -> onParamChange(selectedMachine.id, k, v) }
                                )
                            }
                            MachineType.BASSLINE -> {
                                BassLineControls(
                                    params = selectedMachine.params,
                                    onParamChange = { k, v -> onParamChange(selectedMachine.id, k, v) }
                                )
                            }
                            MachineType.BEATBOX -> {
                                BeatBoxRackControls(
                                    onAudition = onTriggerDrum
                                )
                            }
                            MachineType.PADSYNTH -> {
                                PadSynthControls(
                                    params = selectedMachine.params,
                                    onParamChange = { k, v -> onParamChange(selectedMachine.id, k, v) }
                                )
                            }
                            MachineType.BIT8 -> {
                                Bit8Controls(
                                    params = selectedMachine.params,
                                    onParamChange = { k, v -> onParamChange(selectedMachine.id, k, v) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Output Volume & Pan row
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF181029))
                                .padding(8.dp)
                        ) {
                            AmatistKnob(
                                label = "Volume",
                                value = selectedMachine.volume,
                                onValueChange = { onVolumeChange(selectedMachine.id, it) },
                                accentColor = machineColor
                            )
                            AmatistKnob(
                                label = "Pan",
                                value = (selectedMachine.pan + 1f) / 2f,
                                onValueChange = { onPanChange(selectedMachine.id, it * 2f - 1f) },
                                displayValue = if (selectedMachine.pan == 0f) "C" else if (selectedMachine.pan < 0) "L${(-selectedMachine.pan * 50).toInt()}" else "R${(selectedMachine.pan * 50).toInt()}",
                                accentColor = machineColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom play section on rack: Keyboard for Synths, Pads for BeatBox
                if (selectedMachine.type == MachineType.BEATBOX) {
                    AmatistDrumPads(
                        onTriggerDrum = onTriggerDrum
                    )
                } else {
                    AmatistKeyboard(
                        octave = keyboardOctave,
                        onOctaveChange = onOctaveChange,
                        onNoteOn = { pitch -> onNoteOn(selectedMachine.id, pitch) },
                        onNoteOff = { pitch -> onNoteOff(selectedMachine.id, pitch) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubSynthControls(
    params: Map<String, Float>,
    onParamChange: (String, Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Section: Filter & Resonance
        Text(
            text = "RESONANT 2-POLE FILTER",
            color = Color(0xFFBA68C8),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            AmatistKnob(
                label = "Cutoff",
                value = params["cutoff"] ?: 0.7f,
                onValueChange = { onParamChange("cutoff", it) }
            )
            AmatistKnob(
                label = "Resonance",
                value = params["resonance"] ?: 0.3f,
                onValueChange = { onParamChange("resonance", it) }
            )
            AmatistKnob(
                label = "Osc Mix",
                value = params["osc_mix"] ?: 0.5f,
                onValueChange = { onParamChange("osc_mix", it) }
            )
            AmatistKnob(
                label = "Detune",
                value = params["osc2_detune"] ?: 0.15f,
                onValueChange = { onParamChange("osc2_detune", it) }
            )
        }

        // Section: ADSR Envelopes
        Text(
            text = "AMPLITUDE ENVELOPE (ADSR)",
            color = Color(0xFFBA68C8),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            AmatistKnob(
                label = "Attack",
                value = params["env_a"] ?: 0.05f,
                onValueChange = { onParamChange("env_a", it) }
            )
            AmatistKnob(
                label = "Decay",
                value = params["env_d"] ?: 0.2f,
                onValueChange = { onParamChange("env_d", it) }
            )
            AmatistKnob(
                label = "Sustain",
                value = params["env_s"] ?: 0.7f,
                onValueChange = { onParamChange("env_s", it) }
            )
            AmatistKnob(
                label = "Release",
                value = params["env_r"] ?: 0.3f,
                onValueChange = { onParamChange("env_r", it) }
            )
        }
    }
}

@Composable
private fun BassLineControls(
    params: Map<String, Float>,
    onParamChange: (String, Float) -> Unit
) {
    val wave = (params["wave"] ?: 0f).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "TB-303 ACID ENGINE",
                color = Color(0xFF00E5FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            // Saw / Square switch
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF221538))
                    .padding(2.dp)
            ) {
                Button(
                    onClick = { onParamChange("wave", 0f) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (wave == 0) Color(0xFF00E5FF) else Color.Transparent
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("SAW", color = if (wave == 0) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onParamChange("wave", 1f) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (wave == 1) Color(0xFF00E5FF) else Color.Transparent
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("SQR", color = if (wave == 1) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            AmatistKnob(
                label = "Cutoff",
                value = params["cutoff"] ?: 0.5f,
                onValueChange = { onParamChange("cutoff", it) },
                accentColor = Color(0xFF00E5FF)
            )
            AmatistKnob(
                label = "Resonance",
                value = params["resonance"] ?: 0.8f,
                onValueChange = { onParamChange("resonance", it) },
                accentColor = Color(0xFF00E5FF)
            )
            AmatistKnob(
                label = "Env Mod",
                value = params["env_mod"] ?: 0.65f,
                onValueChange = { onParamChange("env_mod", it) },
                accentColor = Color(0xFF00E5FF)
            )
            AmatistKnob(
                label = "Decay",
                value = params["decay"] ?: 0.4f,
                onValueChange = { onParamChange("decay", it) },
                accentColor = Color(0xFF00E5FF)
            )
        }

        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            AmatistKnob(
                label = "Distortion",
                value = params["distortion"] ?: 0.3f,
                onValueChange = { onParamChange("distortion", it) },
                accentColor = Color(0xFFFF5252)
            )
        }
    }
}

@Composable
private fun BeatBoxRackControls(
    onAudition: (DrumType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "8-CHANNEL DRUM SYNTHESIZER",
            color = Color(0xFFFF5252),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Tap any pad to audition or navigate to PATTERN to edit step triggers.",
            color = Color(0xFFB39DDB),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun PadSynthControls(
    params: Map<String, Float>,
    onParamChange: (String, Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "HARMONIC PAD SYNTH",
            color = Color(0xFF7C4DFF),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            AmatistKnob(
                label = "Cutoff",
                value = params["cutoff"] ?: 0.65f,
                onValueChange = { onParamChange("cutoff", it) },
                accentColor = Color(0xFF7C4DFF)
            )
            AmatistKnob(
                label = "Attack",
                value = params["attack"] ?: 0.35f,
                onValueChange = { onParamChange("attack", it) },
                accentColor = Color(0xFF7C4DFF)
            )
            AmatistKnob(
                label = "Release",
                value = params["release"] ?: 0.5f,
                onValueChange = { onParamChange("release", it) },
                accentColor = Color(0xFF7C4DFF)
            )
        }
    }
}

@Composable
private fun Bit8Controls(
    params: Map<String, Float>,
    onParamChange: (String, Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "CHIPTUNE 8-BIT SYNTH",
            color = Color(0xFFFFD700),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            AmatistKnob(
                label = "Pulse W",
                value = params["pulse_width"] ?: 0.25f,
                onValueChange = { onParamChange("pulse_width", it) },
                accentColor = Color(0xFFFFD700)
            )
        }
    }
}
