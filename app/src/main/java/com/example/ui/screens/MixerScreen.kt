package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import com.example.model.Machine
import com.example.ui.components.AmatistFader
import com.example.ui.components.AmatistKnob
import com.example.ui.components.VuMeter

@Composable
fun MixerScreen(
    machines: List<Machine>,
    masterVolume: Float,
    masterVuL: Float,
    masterVuR: Float,
    machineVuPeaks: Map<String, Float>,
    onVolumeChange: (machineId: String, volume: Float) -> Unit,
    onPanChange: (machineId: String, pan: Float) -> Unit,
    onToggleMute: (machineId: String) -> Unit,
    onToggleSolo: (machineId: String) -> Unit,
    onMasterVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0816))
            .padding(8.dp)
    ) {
        Text(
            text = "STUDIO MIXING CONSOLE",
            color = Color(0xFFC4B8DB),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Machine Channel Strips
            machines.forEachIndexed { index, m ->
                val mColor = Color(m.colorHex)
                val peak = machineVuPeaks[m.id] ?: 0f

                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF140D24))
                        .border(1.dp, Color(0xFF2E1C4D), RoundedCornerShape(8.dp))
                        .padding(6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Top Color Accent Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(mColor)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "CH ${index + 1}",
                            color = Color(0xFF9E8DB8),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = m.name,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Pan knob
                        AmatistKnob(
                            label = "Pan",
                            value = (m.pan + 1f) / 2f,
                            onValueChange = { onPanChange(m.id, it * 2f - 1f) },
                            size = 38.dp,
                            displayValue = if (m.pan == 0f) "C" else if (m.pan < 0) "L${(-m.pan * 50).toInt()}" else "R${(m.pan * 50).toInt()}",
                            accentColor = mColor
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Mute & Solo Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onToggleMute(m.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (m.isMuted) Color(0xFFFF5252) else Color(0xFF231738)
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(24.dp).testTag("mix_mute_${m.id}")
                            ) {
                                Text(
                                    text = "M",
                                    color = if (m.isMuted) Color.White else Color(0xFFC4B8DB),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { onToggleSolo(m.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (m.isSolo) Color(0xFFFFD54F) else Color(0xFF231738)
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(24.dp).testTag("mix_solo_${m.id}")
                            ) {
                                Text(
                                    text = "S",
                                    color = if (m.isSolo) Color.Black else Color(0xFFC4B8DB),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Vertical Fader with VU
                        AmatistFader(
                            value = m.volume,
                            onValueChange = { onVolumeChange(m.id, it) },
                            vuLevel = if (m.isMuted) 0f else peak,
                            height = 140.dp,
                            label = m.id
                        )
                    }
                }
            }

            // Master Channel Strip
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1B1130))
                    .border(1.5.dp, Color(0xFFBA68C8), RoundedCornerShape(8.dp))
                    .padding(6.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF00E5FF))
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "MASTER",
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "OUT L/R",
                        color = Color(0xFFE1BEE7),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Master dual fader with L and R meters
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AmatistFader(
                            value = masterVolume,
                            onValueChange = onMasterVolumeChange,
                            vuLevel = masterVuL,
                            height = 160.dp,
                            label = "master"
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        VuMeter(
                            level = masterVuR,
                            width = 8.dp,
                            height = 160.dp,
                            segments = 16
                        )
                    }
                }
            }
        }
    }
}
