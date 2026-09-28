package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DrumType
import com.example.model.MachineType
import com.example.ui.components.AmatistDrumPads
import com.example.ui.components.AmatistKeyboard
import com.example.ui.components.AmatistTransportBar
import com.example.ui.screens.*
import com.example.viewmodel.AmatistViewModel
import com.example.viewmodel.DawTab

@Composable
fun AmatistAudioApp(
    viewModel: AmatistViewModel,
    modifier: Modifier = Modifier
) {
    val project by viewModel.project.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val selectedMachineId by viewModel.selectedMachineId.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playMode by viewModel.playMode.collectAsStateWithLifecycle()
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val currentBar by viewModel.currentBar.collectAsStateWithLifecycle()
    val vuState by viewModel.vuMeterState.collectAsStateWithLifecycle()
    val isDrawerOpen by viewModel.isDrawerOpen.collectAsStateWithLifecycle()
    val keyboardOctave by viewModel.keyboardOctave.collectAsStateWithLifecycle()
    val showProjectDialog by viewModel.showProjectDialog.collectAsStateWithLifecycle()
    val showAddMachineDialog by viewModel.showAddMachineDialog.collectAsStateWithLifecycle()
    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()

    val currentMachine = project.machines.find { it.id == selectedMachineId } ?: project.machines.firstOrNull()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AmatistTransportBar(
                projectName = project.name,
                bpm = project.bpm,
                isPlaying = isPlaying,
                playMode = playMode,
                currentBar = currentBar,
                currentStep = currentStep,
                isDrawerOpen = isDrawerOpen,
                onTogglePlay = { viewModel.togglePlay() },
                onStop = { viewModel.stopPlayback() },
                onTogglePlayMode = { viewModel.togglePlayMode() },
                onBpmChange = { viewModel.setBpm(it) },
                onToggleDrawer = { viewModel.setDrawerOpen(!isDrawerOpen) },
                onOpenProjectDialog = { viewModel.showProjectDialog(true) }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0A19))
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Quick Play Drawer if opened
                AnimatedVisibility(
                    visible = isDrawerOpen,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    if (currentMachine?.type == MachineType.BEATBOX) {
                        AmatistDrumPads(
                            onTriggerDrum = { viewModel.triggerLiveDrum(it) }
                        )
                    } else {
                        AmatistKeyboard(
                            octave = keyboardOctave,
                            onOctaveChange = { viewModel.setOctave(it) },
                            onNoteOn = { pitch ->
                                currentMachine?.let { viewModel.triggerLiveNote(it.id, pitch) }
                            },
                            onNoteOff = { pitch ->
                                currentMachine?.let { viewModel.releaseLiveNote(it.id, pitch) }
                            }
                        )
                    }
                }

                // Modernized Caustic DAW Tab Bar
                Surface(
                    color = Color(0xFF130D21),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFF2C1E47))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        DawTab.entries.forEach { tab ->
                            val isSelected = activeTab == tab
                            val activeColor = Color(0xFFBA68C8)

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFF2B1947) else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) activeColor else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { viewModel.selectTab(tab) }
                                    .testTag("tab_${tab.name}")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = tab.iconLabel,
                                        color = if (isSelected) activeColor else Color(0xFF8877A0),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = tab.title,
                                        color = if (isSelected) Color.White else Color(0xFF6C5A85),
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0C0816),
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                DawTab.RACK -> {
                    RackScreen(
                        machines = project.machines,
                        selectedMachineId = selectedMachineId,
                        keyboardOctave = keyboardOctave,
                        onSelectMachine = { viewModel.selectMachine(it) },
                        onAddMachineClick = { viewModel.showAddMachineDialog(true) },
                        onRemoveMachine = { viewModel.removeMachine(it) },
                        onParamChange = { mId, key, value -> viewModel.updateMachineParam(mId, key, value) },
                        onVolumeChange = { mId, vol -> viewModel.setMachineVolume(mId, vol) },
                        onPanChange = { mId, pan -> viewModel.setMachinePan(mId, pan) },
                        onToggleMute = { viewModel.toggleMachineMute(it) },
                        onToggleSolo = { viewModel.toggleMachineSolo(it) },
                        onOctaveChange = { viewModel.setOctave(it) },
                        onNoteOn = { mId, pitch -> viewModel.triggerLiveNote(mId, pitch) },
                        onNoteOff = { mId, pitch -> viewModel.releaseLiveNote(mId, pitch) },
                        onTriggerDrum = { viewModel.triggerLiveDrum(it) }
                    )
                }
                DawTab.PATTERN -> {
                    PatternScreen(
                        machines = project.machines,
                        selectedMachineId = selectedMachineId,
                        currentStep = currentStep,
                        keyboardOctave = keyboardOctave,
                        onSelectMachine = { viewModel.selectMachine(it) },
                        onSelectPattern = { mId, pId -> viewModel.setMachineActivePattern(mId, pId) },
                        onClearPattern = { mId, pId -> viewModel.clearPattern(mId, pId) },
                        onToggleDrumStep = { mId, pId, drumType, step ->
                            viewModel.toggleDrumStep(mId, pId, drumType, step)
                        },
                        onToggleSynthNote = { mId, pId, step, pitch ->
                            viewModel.addOrRemoveSynthNote(mId, pId, step, pitch)
                        },
                        onAuditionDrum = { viewModel.triggerLiveDrum(it) },
                        onOctaveChange = { viewModel.setOctave(it) }
                    )
                }
                DawTab.SONG -> {
                    SongScreen(
                        machines = project.machines,
                        songMeasures = project.songMeasures,
                        currentBar = currentBar,
                        onSetCell = { bar, mId, pId -> viewModel.setSongArrangerCell(bar, mId, pId) },
                        onAddMeasure = { viewModel.addMeasure() }
                    )
                }
                DawTab.MIXER -> {
                    MixerScreen(
                        machines = project.machines,
                        masterVolume = project.masterVolume,
                        masterVuL = vuState.masterL,
                        masterVuR = vuState.masterR,
                        machineVuPeaks = vuState.machinePeaks,
                        onVolumeChange = { mId, vol -> viewModel.setMachineVolume(mId, vol) },
                        onPanChange = { mId, pan -> viewModel.setMachinePan(mId, pan) },
                        onToggleMute = { viewModel.toggleMachineMute(it) },
                        onToggleSolo = { viewModel.toggleMachineSolo(it) },
                        onMasterVolumeChange = { viewModel.setMasterVolume(it) }
                    )
                }
                DawTab.MASTER_FX -> {
                    MasterFxScreen(
                        project = project,
                        onDelayWetChange = { viewModel.setDelayWet(it) },
                        onDelayTimeChange = { viewModel.setDelayTime(it) },
                        onDelayFeedbackChange = { viewModel.setDelayFeedback(it) },
                        onReverbWetChange = { viewModel.setReverbWet(it) },
                        onReverbRoomChange = { viewModel.setReverbRoom(it) },
                        onDistortionDriveChange = { viewModel.setDistortionDrive(it) },
                        onMasterVolumeChange = { viewModel.setMasterVolume(it) }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showProjectDialog) {
        ProjectsDialog(
            currentProjectName = project.name,
            savedProjects = savedProjects,
            onSave = { name -> viewModel.saveCurrentProject(name) },
            onNewProject = { viewModel.createNewProject() },
            onLoadProject = { id -> viewModel.loadProject(id) },
            onDeleteProject = { id -> viewModel.deleteProject(id) },
            onDismiss = { viewModel.showProjectDialog(false) }
        )
    }

    if (showAddMachineDialog) {
        AddMachineDialog(
            onAddMachine = { type, name -> viewModel.addNewMachine(type, name) },
            onDismiss = { viewModel.showAddMachineDialog(false) }
        )
    }
}
