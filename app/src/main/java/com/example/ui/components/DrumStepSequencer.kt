package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DrumLane
import com.example.model.DrumType

@Composable
fun DrumStepSequencer(
    drumLanes: List<DrumLane>,
    currentStep: Int,
    onToggleStep: (drumType: DrumType, stepIndex: Int) -> Unit,
    onTriggerAudition: (drumType: DrumType) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF110C1C))
            .border(1.dp, Color(0xFF2E1F47), RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        // Step number header row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(bottom = 6.dp)
        ) {
            Spacer(modifier = Modifier.width(72.dp)) // Offset for lane label
            for (step in 0 until 16) {
                val isBeatStart = step % 4 == 0
                val isCurrent = step == currentStep
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(36.dp)
                        .padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = "${step + 1}",
                        color = if (isCurrent) Color(0xFF00E5FF) else if (isBeatStart) Color(0xFFFFD54F) else Color(0xFF7E6E99),
                        fontSize = 10.sp,
                        fontWeight = if (isBeatStart || isCurrent) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Drum lanes
        drumLanes.forEach { lane ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .horizontalScroll(scrollState)
            ) {
                // Audition button / label
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(68.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF291B3D))
                        .border(1.dp, Color(0xFF563B7D), RoundedCornerShape(4.dp))
                        .clickable { onTriggerAudition(lane.drumType) }
                        .padding(horizontal = 4.dp)
                        .testTag("audition_${lane.drumType.shortName}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lane.drumType.shortName,
                            color = Color(0xFFFF5252),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFFF7043))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 16 step pads
                for (step in 0 until 16) {
                    val isActive = step < lane.steps.size && lane.steps[step]
                    val isCurrent = step == currentStep
                    val isBeatGroup1 = (step / 4) % 2 == 0

                    val padBg = when {
                        isActive -> Color(0xFFFF5252)
                        isCurrent -> Color(0x5500E5FF)
                        isBeatGroup1 -> Color(0xFF1E1530)
                        else -> Color(0xFF291D42)
                    }

                    val borderColor = when {
                        isCurrent -> Color(0xFF00E5FF)
                        isActive -> Color(0xFFFF8A80)
                        else -> Color(0xFF3B2A59)
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(36.dp)
                            .height(32.dp)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(padBg)
                            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                            .clickable {
                                onToggleStep(lane.drumType, step)
                            }
                            .testTag("drum_step_${lane.drumType.shortName}_$step")
                    ) {
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }
    }
}
