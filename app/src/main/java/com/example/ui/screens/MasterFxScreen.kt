package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Project
import com.example.ui.components.AmatistKnob

@Composable
fun MasterFxScreen(
    project: Project,
    onDelayWetChange: (Float) -> Unit,
    onDelayTimeChange: (Float) -> Unit,
    onDelayFeedbackChange: (Float) -> Unit,
    onReverbWetChange: (Float) -> Unit,
    onReverbRoomChange: (Float) -> Unit,
    onDistortionDriveChange: (Float) -> Unit,
    onMasterVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0816))
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "MASTER EFFECTS RACK",
            color = Color(0xFFC4B8DB),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )

        // FX Unit 1: Stereo Delay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF140D24))
                .border(1.dp, Color(0xFF38235C), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "STEREO TAPE DELAY",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "SYNC / 16THS",
                        color = Color(0xFF7E6E99),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AmatistKnob(
                        label = "Wet",
                        value = project.delayWet,
                        onValueChange = onDelayWetChange,
                        accentColor = Color(0xFF00E5FF)
                    )
                    AmatistKnob(
                        label = "Time",
                        value = project.delayTime,
                        onValueChange = onDelayTimeChange,
                        displayValue = "${(project.delayTime * 1000).toInt()}ms",
                        accentColor = Color(0xFF00E5FF)
                    )
                    AmatistKnob(
                        label = "Feedback",
                        value = project.delayFeedback,
                        onValueChange = onDelayFeedbackChange,
                        accentColor = Color(0xFF00E5FF)
                    )
                }
            }
        }

        // FX Unit 2: Studio Reverb
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF140D24))
                .border(1.dp, Color(0xFF38235C), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "STUDIO REVERB DIFFUSER",
                        color = Color(0xFFE040FB),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "LUSH ROOM",
                        color = Color(0xFF7E6E99),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AmatistKnob(
                        label = "Wet",
                        value = project.reverbWet,
                        onValueChange = onReverbWetChange,
                        accentColor = Color(0xFFE040FB)
                    )
                    AmatistKnob(
                        label = "Room Size",
                        value = project.reverbRoom,
                        onValueChange = onReverbRoomChange,
                        accentColor = Color(0xFFE040FB)
                    )
                }
            }
        }

        // FX Unit 3: Master Saturation / Overdrive
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF140D24))
                .border(1.dp, Color(0xFF38235C), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "TUBE SATURATION & LIMITER",
                        color = Color(0xFFFF5252),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ANALOG DRIVE",
                        color = Color(0xFF7E6E99),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AmatistKnob(
                        label = "Drive",
                        value = project.distortionDrive,
                        onValueChange = onDistortionDriveChange,
                        accentColor = Color(0xFFFF5252)
                    )
                    AmatistKnob(
                        label = "Master Out",
                        value = project.masterVolume,
                        onValueChange = onMasterVolumeChange,
                        accentColor = Color(0xFFFFD54F)
                    )
                }
            }
        }
    }
}
