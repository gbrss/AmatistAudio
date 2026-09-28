package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.model.Machine
import com.example.model.SongMeasure

@Composable
fun SongScreen(
    machines: List<Machine>,
    songMeasures: List<SongMeasure>,
    currentBar: Int,
    onSetCell: (barIndex: Int, machineId: String, patternId: String?) -> Unit,
    onAddMeasure: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()

    var pickerCell by remember { mutableStateOf<Triple<Int, String, String?>?>(null) } // bar, machineId, currentPattern

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0816))
            .padding(8.dp)
    ) {
        // Song Arranger Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            Text(
                text = "SONG ARRANGER / TIMELINE",
                color = Color(0xFFC4B8DB),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Button(
                onClick = onAddMeasure,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26193E)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(26.dp).testTag("add_measure_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ 1 BAR", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Timeline Matrix
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF100B1A))
                .border(1.dp, Color(0xFF2E1C4D), RoundedCornerShape(8.dp))
        ) {
            // Left Column: Machine Track Labels
            Column(
                modifier = Modifier
                    .width(100.dp)
                    .verticalScroll(vScroll)
                    .background(Color(0xFF160F24))
            ) {
                // Header spacer for bar numbers
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .background(Color(0xFF1D1430))
                        .padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = "TRACKS",
                        color = Color(0xFF9E8DB8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                machines.forEach { m ->
                    val mColor = Color(m.colorHex)
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .border(0.5.dp, Color(0xFF2B1D45))
                            .padding(horizontal = 6.dp)
                    ) {
                        Column {
                            Text(
                                text = m.name,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = m.type.shortName,
                                color = mColor,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Right: Scrollable Measures Timeline
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(hScroll)
                    .verticalScroll(vScroll)
            ) {
                Column {
                    // Bar Numbers Header Row
                    Row(
                        modifier = Modifier
                            .height(30.dp)
                            .background(Color(0xFF1B132B))
                    ) {
                        songMeasures.forEach { measure ->
                            val isCurrentBar = measure.barIndex == currentBar
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .width(48.dp)
                                    .fillMaxHeight()
                                    .border(
                                        width = if (isCurrentBar) 1.5.dp else 0.5.dp,
                                        color = if (isCurrentBar) Color(0xFF00E5FF) else Color(0xFF2E1F47)
                                    )
                                    .background(if (isCurrentBar) Color(0x3300E5FF) else Color.Transparent)
                            ) {
                                Text(
                                    text = "${measure.barIndex + 1}",
                                    color = if (isCurrentBar) Color(0xFF00E5FF) else Color(0xFFBA68C8),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Machine Rows
                    machines.forEach { m ->
                        val mColor = Color(m.colorHex)
                        Row(modifier = Modifier.height(44.dp)) {
                            songMeasures.forEach { measure ->
                                val assignedPat = measure.patternAssignments[m.id]
                                val isCurrentBar = measure.barIndex == currentBar

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .width(48.dp)
                                        .fillMaxHeight()
                                        .border(
                                            width = if (isCurrentBar) 1.dp else 0.5.dp,
                                            color = if (isCurrentBar) Color(0x6600E5FF) else Color(0xFF251A3B)
                                        )
                                        .background(
                                            if (assignedPat != null) mColor.copy(alpha = 0.28f)
                                            else if (isCurrentBar) Color(0x1100E5FF)
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            pickerCell = Triple(measure.barIndex, m.id, assignedPat)
                                        }
                                        .testTag("song_cell_${measure.barIndex}_${m.id}")
                                ) {
                                    if (assignedPat != null) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(mColor.copy(alpha = 0.85f))
                                                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                        ) {
                                            Text(
                                                text = assignedPat,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "-",
                                            color = Color(0xFF43305E),
                                            fontSize = 12.sp
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

    // Pattern Picker Dialog for Arranger Cell
    pickerCell?.let { (barIndex, machineId, currentPattern) ->
        val machine = machines.find { it.id == machineId }
        AlertDialog(
            onDismissRequest = { pickerCell = null },
            title = {
                Text(
                    text = "Assign Pattern (Bar ${barIndex + 1})",
                    color = Color.White,
                    fontSize = 14.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = machine?.name ?: "Track",
                        color = Color(0xFFCE93D8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Quick grid of patterns A1..A8, B1..B4
                    val patterns = listOf("A1", "A2", "A3", "A4", "B1", "B2", "B3", "B4")
                    for (row in 0 until 2) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            for (col in 0 until 4) {
                                val p = patterns[row * 4 + col]
                                val isSelected = currentPattern == p
                                Button(
                                    onClick = {
                                        onSetCell(barIndex, machineId, p)
                                        pickerCell = null
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFF9C27B0) else Color(0xFF26193E)
                                    ),
                                    modifier = Modifier.weight(1f).height(36.dp)
                                ) {
                                    Text(p, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Remove pattern button
                    Button(
                        onClick = {
                            onSetCell(barIndex, machineId, null)
                            pickerCell = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E1A24)),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Text("Clear Block (Empty)", color = Color(0xFFFF5252), fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pickerCell = null }) {
                    Text("Cancel", color = Color(0xFFCE93D8))
                }
            },
            containerColor = Color(0xFF1E1430)
        )
    }
}
