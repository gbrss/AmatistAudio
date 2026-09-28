package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.model.PlayMode

@Composable
fun AmatistTransportBar(
    projectName: String,
    bpm: Int,
    isPlaying: Boolean,
    playMode: PlayMode,
    currentBar: Int,
    currentStep: Int,
    isDrawerOpen: Boolean,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onTogglePlayMode: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onToggleDrawer: () -> Unit,
    onOpenProjectDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playButtonColor by animateColorAsState(
        targetValue = if (isPlaying) Color(0xFF00E676) else Color(0xFFCE93D8),
        label = "playColor"
    )

    Surface(
        color = Color(0xFF130D21),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFF2E1F4D),
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Left: Brand / Project selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onOpenProjectDialog() }
                    .background(Color(0xFF1E1433))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("project_menu_btn")
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFBA68C8))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "AmatistAudio",
                        color = Color(0xFFE1BEE7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = projectName,
                        color = Color(0xFF9E8DB8),
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Project Menu",
                    tint = Color(0xFFBA68C8),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Center: LCD Digital Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF090612), Color(0xFF0E0A1A))
                        )
                    )
                    .border(1.dp, Color(0xFF38235C), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                // Mode Toggle Pill: PAT / SONG
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onTogglePlayMode() }
                        .background(if (playMode == PlayMode.PATTERN) Color(0xFF7B1FA2) else Color(0xFF0097A7))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .testTag("mode_toggle_btn")
                ) {
                    Text(
                        text = if (playMode == PlayMode.PATTERN) "PAT" else "SONG",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Bar : Step LCD
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "BAR:STEP",
                        color = Color(0xFF7E6E99),
                        fontSize = 7.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    val barStr = String.format("%03d", currentBar + 1)
                    val stepStr = String.format("%02d", currentStep + 1)
                    Text(
                        text = "$barStr:$stepStr",
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // BPM nudge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onBpmChange(bpm - 1) },
                        modifier = Modifier.size(24.dp).testTag("bpm_minus_btn")
                    ) {
                        Text("-", color = Color(0xFFBA68C8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "BPM",
                            color = Color(0xFF7E6E99),
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "$bpm",
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = { onBpmChange(bpm + 1) },
                        modifier = Modifier.size(24.dp).testTag("bpm_plus_btn")
                    ) {
                        Text("+", color = Color(0xFFBA68C8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Right: Play / Stop / Jam Drawer Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Stop button
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF26193E))
                        .testTag("transport_stop_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Play / Pause button
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isPlaying) Color(0xFF1B3B2B) else Color(0xFF381F54))
                        .border(
                            1.dp,
                            if (isPlaying) Color(0xFF00E676) else Color(0xFFBA68C8),
                            RoundedCornerShape(6.dp)
                        )
                        .testTag("transport_play_btn")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = playButtonColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Live Keyboard/Drum Jam Drawer toggle
                IconButton(
                    onClick = onToggleDrawer,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDrawerOpen) Color(0xFF7B1FA2) else Color(0xFF26193E))
                        .testTag("transport_jam_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Piano,
                        contentDescription = "Live Keys / Pads",
                        tint = if (isDrawerOpen) Color.White else Color(0xFFCE93D8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
