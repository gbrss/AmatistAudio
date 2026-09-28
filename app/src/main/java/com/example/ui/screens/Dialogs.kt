package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
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
import com.example.database.ProjectEntity
import com.example.model.MachineType

@Composable
fun ProjectsDialog(
    currentProjectName: String,
    savedProjects: List<ProjectEntity>,
    onSave: (newName: String) -> Unit,
    onNewProject: () -> Unit,
    onLoadProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var projectNameText by remember { mutableStateOf(currentProjectName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "PROJECT MANAGER",
                color = Color(0xFFE1BEE7),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Save current project row
                OutlinedTextField(
                    value = projectNameText,
                    onValueChange = { projectNameText = it },
                    label = { Text("Project Name", color = Color(0xFFBA68C8)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFE1BEE7),
                        focusedBorderColor = Color(0xFFBA68C8),
                        unfocusedBorderColor = Color(0xFF4A3275)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("project_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            if (projectNameText.isNotBlank()) {
                                onSave(projectNameText)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                        modifier = Modifier.weight(1f).testTag("save_project_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Project", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { onNewProject() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26193E)),
                        modifier = Modifier.weight(1f).testTag("new_project_btn")
                    ) {
                        Text("New Project", color = Color(0xFF00E5FF), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "SAVED PROJECTS & DEMOS",
                    color = Color(0xFFBA68C8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(savedProjects) { proj ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1B112D))
                                .border(1.dp, Color(0xFF38235C), RoundedCornerShape(6.dp))
                                .clickable { onLoadProject(proj.id) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("project_item_${proj.id}")
                        ) {
                            Column {
                                Text(
                                    text = proj.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${proj.bpm} BPM",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { onLoadProject(proj.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF332050)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("Load", fontSize = 10.sp, color = Color(0xFFE1BEE7))
                                }

                                if (savedProjects.size > 1) {
                                    IconButton(
                                        onClick = { onDeleteProject(proj.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFFCE93D8))
            }
        },
        containerColor = Color(0xFF140D24)
    )
}

@Composable
fun AddMachineDialog(
    onAddMachine: (type: MachineType, name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(MachineType.SUBSYNTH) }
    var machineName by remember { mutableStateOf("SubSynth") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ADD RACK MACHINE",
                color = Color(0xFFE1BEE7),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select Machine Architecture:",
                    color = Color(0xFFC4B8DB),
                    fontSize = 11.sp
                )

                MachineType.entries.forEach { type ->
                    val isSelected = selectedType == type
                    val color = Color(type.defaultColorHex)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color(0xFF2C1947) else Color(0xFF160F26))
                            .border(
                                1.dp,
                                if (isSelected) color else Color(0xFF332050),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                selectedType = type
                                machineName = type.title
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("add_machine_type_${type.name}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = type.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (type) {
                                    MachineType.SUBSYNTH -> "Subtractive 2-Oscillator Analog Synthesizer"
                                    MachineType.BASSLINE -> "Acid 303 Style Bass Synthesizer with Overdrive"
                                    MachineType.BEATBOX -> "8-Channel Drum Computer / Drum Machine"
                                    MachineType.PADSYNTH -> "Lush Harmonic Atmospheric Pad Synthesizer"
                                    MachineType.BIT8 -> "NES & Arcade Lo-Fi Chiptune Synthesizer"
                                },
                                color = Color(0xFF9E8DB8),
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = machineName,
                    onValueChange = { machineName = it },
                    label = { Text("Machine Name", color = Color(0xFFBA68C8)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFE1BEE7),
                        focusedBorderColor = Color(0xFFBA68C8),
                        unfocusedBorderColor = Color(0xFF4A3275)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddMachine(selectedType, machineName) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                modifier = Modifier.testTag("confirm_add_machine_btn")
            ) {
                Text("Add to Rack")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFCE93D8))
            }
        },
        containerColor = Color(0xFF140D24)
    )
}
