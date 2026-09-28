package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AmatistAudioEngine
import com.example.database.AmatistDatabase
import com.example.database.ProjectEntity
import com.example.database.ProjectRepository
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

enum class DawTab(val title: String, val iconLabel: String) {
    RACK("Rack", "RACK"),
    PATTERN("Pattern", "PAT"),
    SONG("Arranger", "SONG"),
    MIXER("Mixer", "MIX"),
    MASTER_FX("Master & FX", "FX")
}

data class VuMeterState(
    val masterL: Float = 0f,
    val masterR: Float = 0f,
    val machinePeaks: Map<String, Float> = emptyMap()
)

class AmatistViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AmatistDatabase.getDatabase(application)
    private val repository = ProjectRepository(db.projectDao())
    val audioEngine = AmatistAudioEngine()

    private val vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private val _project = MutableStateFlow<Project>(ProjectRepository.createDemoProjectAmethyst())
    val project: StateFlow<Project> = _project.asStateFlow()

    private val _activeTab = MutableStateFlow(DawTab.RACK)
    val activeTab: StateFlow<DawTab> = _activeTab.asStateFlow()

    private val _selectedMachineId = MutableStateFlow("sub1")
    val selectedMachineId: StateFlow<String> = _selectedMachineId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playMode = MutableStateFlow(PlayMode.PATTERN)
    val playMode: StateFlow<PlayMode> = _playMode.asStateFlow()

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _currentBar = MutableStateFlow(0)
    val currentBar: StateFlow<Int> = _currentBar.asStateFlow()

    private val _vuMeterState = MutableStateFlow(VuMeterState())
    val vuMeterState: StateFlow<VuMeterState> = _vuMeterState.asStateFlow()

    // Keyboard / Drum Pad drawer
    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    private val _keyboardOctave = MutableStateFlow(4)
    val keyboardOctave: StateFlow<Int> = _keyboardOctave.asStateFlow()

    // Dialogs
    private val _showProjectDialog = MutableStateFlow(false)
    val showProjectDialog: StateFlow<Boolean> = _showProjectDialog.asStateFlow()

    private val _showAddMachineDialog = MutableStateFlow(false)
    val showAddMachineDialog: StateFlow<Boolean> = _showAddMachineDialog.asStateFlow()

    val savedProjects: StateFlow<List<ProjectEntity>> = MutableStateFlow<List<ProjectEntity>>(emptyList()).also { flow ->
        viewModelScope.launch {
            repository.allProjects.collectLatest {
                (flow as MutableStateFlow).value = it
            }
        }
    }

    init {
        // Start engine
        audioEngine.start()
        audioEngine.updateProject(_project.value)

        audioEngine.onSequencerTick = { step, bar ->
            _currentStep.value = step
            _currentBar.value = bar
        }

        audioEngine.onVuMeterUpdate = { mL, mR, peaks ->
            _vuMeterState.value = VuMeterState(mL, mR, peaks)
        }

        viewModelScope.launch {
            repository.seedInitialProjectsIfNeeded()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }

    fun vibrateShort() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(12)
            }
        } catch (_: Exception) {}
    }

    fun selectTab(tab: DawTab) {
        _activeTab.value = tab
    }

    fun selectMachine(id: String) {
        _selectedMachineId.value = id
    }

    fun togglePlay() {
        vibrateShort()
        if (_isPlaying.value) {
            audioEngine.pause()
            _isPlaying.value = false
        } else {
            audioEngine.triggerPlay(startFromBeginning = false)
            _isPlaying.value = true
        }
    }

    fun stopPlayback() {
        vibrateShort()
        audioEngine.stopPlayback()
        _isPlaying.value = false
        _currentStep.value = 0
        _currentBar.value = 0
    }

    fun togglePlayMode() {
        vibrateShort()
        val nextMode = if (_playMode.value == PlayMode.PATTERN) PlayMode.SONG else PlayMode.PATTERN
        _playMode.value = nextMode
        audioEngine.playMode.set(nextMode.ordinal)
    }

    fun setBpm(newBpm: Int) {
        val clamped = newBpm.coerceIn(40, 260)
        _project.value = _project.value.copy(bpm = clamped)
        audioEngine.updateProject(_project.value)
    }

    fun setMasterVolume(vol: Float) {
        _project.value = _project.value.copy(masterVolume = vol.coerceIn(0f, 1f))
        audioEngine.updateProject(_project.value)
    }

    fun setDelayWet(wet: Float) {
        _project.value = _project.value.copy(delayWet = wet.coerceIn(0f, 1f))
        audioEngine.updateProject(_project.value)
    }

    fun setDelayTime(time: Float) {
        _project.value = _project.value.copy(delayTime = time.coerceIn(0.05f, 1f))
        audioEngine.updateProject(_project.value)
    }

    fun setDelayFeedback(fb: Float) {
        _project.value = _project.value.copy(delayFeedback = fb.coerceIn(0f, 0.9f))
        audioEngine.updateProject(_project.value)
    }

    fun setReverbWet(wet: Float) {
        _project.value = _project.value.copy(reverbWet = wet.coerceIn(0f, 1f))
        audioEngine.updateProject(_project.value)
    }

    fun setReverbRoom(room: Float) {
        _project.value = _project.value.copy(reverbRoom = room.coerceIn(0f, 1f))
        audioEngine.updateProject(_project.value)
    }

    fun setDistortionDrive(drive: Float) {
        _project.value = _project.value.copy(distortionDrive = drive.coerceIn(0f, 1f))
        audioEngine.updateProject(_project.value)
    }

    // Machine updates
    fun updateMachineParam(machineId: String, paramKey: String, value: Float) {
        val updatedMachines = _project.value.machines.map { m ->
            if (m.id == machineId) {
                val newParams = m.params.toMutableMap()
                newParams[paramKey] = value
                m.copy(params = newParams)
            } else m
        }
        _project.value = _project.value.copy(machines = updatedMachines)
        audioEngine.updateProject(_project.value)
    }

    fun setMachineVolume(machineId: String, vol: Float) {
        val updatedMachines = _project.value.machines.map { m ->
            if (m.id == machineId) m.copy(volume = vol.coerceIn(0f, 1f)) else m
        }
        _project.value = _project.value.copy(machines = updatedMachines)
        audioEngine.updateProject(_project.value)
    }

    fun setMachinePan(machineId: String, pan: Float) {
        val updatedMachines = _project.value.machines.map { m ->
            if (m.id == machineId) m.copy(pan = pan.coerceIn(-1f, 1f)) else m
        }
        _project.value = _project.value.copy(machines = updatedMachines)
        audioEngine.updateProject(_project.value)
    }

    fun toggleMachineMute(machineId: String) {
        vibrateShort()
        val updatedMachines = _project.value.machines.map { m ->
            if (m.id == machineId) m.copy(isMuted = !m.isMuted) else m
        }
        _project.value = _project.value.copy(machines = updatedMachines)
        audioEngine.updateProject(_project.value)
    }

    fun toggleMachineSolo(machineId: String) {
        vibrateShort()
        val target = _project.value.machines.find { it.id == machineId } ?: return
        val newSolo = !target.isSolo
        val updatedMachines = _project.value.machines.map { m ->
            if (m.id == machineId) m.copy(isSolo = newSolo)
            else if (newSolo) m.copy(isMuted = true, isSolo = false)
            else m.copy(isMuted = false)
        }
        _project.value = _project.value.copy(machines = updatedMachines)
        audioEngine.updateProject(_project.value)
    }

    fun setMachineActivePattern(machineId: String, patternId: String) {
        val updatedMachines = _project.value.machines.map { m ->
            if (m.id == machineId) m.copy(activePatternId = patternId) else m
        }
        _project.value = _project.value.copy(machines = updatedMachines)
        audioEngine.updateProject(_project.value)
    }

    // Pattern updates
    fun toggleDrumStep(machineId: String, patternId: String, drumType: DrumType, stepIndex: Int) {
        vibrateShort()
        val updatedMachines = _project.value.machines.map { m ->
            if (m.id == machineId) {
                val updatedPatterns = m.patterns.map { p ->
                    if (p.id == patternId) {
                        val updatedLanes = p.drumLanes.map { lane ->
                            if (lane.drumType == drumType) {
                                val newSteps = lane.steps.toMutableList()
                                if (stepIndex < newSteps.size) {
                                    newSteps[stepIndex] = !newSteps[stepIndex]
                                }
                                lane.copy(steps = newSteps)
                            } else lane
                        }
                        p.copy(drumLanes = updatedLanes)
                    } else p
                }
                m.copy(patterns = updatedPatterns)
            } else m
        }
        _project.value = _project.value.copy(machines = updatedMachines)
        audioEngine.updateProject(_project.value)
    }

    fun addOrRemoveSynthNote(machineId: String, patternId: String, step: Int, pitch: Int) {
        vibrateShort()
        val updatedMachines = _project.value.machines.map { m ->
            if (m.id == machineId) {
                val updatedPatterns = m.patterns.map { p ->
                    if (p.id == patternId) {
                        val existingIndex = p.notes.indexOfFirst { it.step == step && it.pitch == pitch }
                        val newNotes = p.notes.toMutableList()
                        if (existingIndex >= 0) {
                            newNotes.removeAt(existingIndex)
                        } else {
                            newNotes.add(Note(step = step, pitch = pitch, length = 1, velocity = 0.85f))
                            // Preview note immediately
                            audioEngine.triggerLiveNote(machineId, pitch, 0.85f)
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(180)
                                audioEngine.releaseLiveNote(machineId, pitch)
                            }
                        }
                        p.copy(notes = newNotes)
                    } else p
                }
                m.copy(patterns = updatedPatterns)
            } else m
        }
        _project.value = _project.value.copy(machines = updatedMachines)
        audioEngine.updateProject(_project.value)
    }

    fun clearPattern(machineId: String, patternId: String) {
        vibrateShort()
        val updatedMachines = _project.value.machines.map { m ->
            if (m.id == machineId) {
                val updatedPatterns = m.patterns.map { p ->
                    if (p.id == patternId) {
                        if (m.type == MachineType.BEATBOX) {
                            val clearedLanes = p.drumLanes.map { it.copy(steps = List(16) { false }) }
                            p.copy(drumLanes = clearedLanes)
                        } else {
                            p.copy(notes = emptyList())
                        }
                    } else p
                }
                m.copy(patterns = updatedPatterns)
            } else m
        }
        _project.value = _project.value.copy(machines = updatedMachines)
        audioEngine.updateProject(_project.value)
    }

    // Song Arranger updates
    fun setSongArrangerCell(barIndex: Int, machineId: String, patternId: String?) {
        vibrateShort()
        val measures = _project.value.songMeasures.toMutableList()
        while (measures.size <= barIndex) {
            measures.add(SongMeasure(measures.size, emptyMap()))
        }
        val targetMeasure = measures[barIndex]
        val newAssignments = targetMeasure.patternAssignments.toMutableMap()
        if (patternId == null) {
            newAssignments.remove(machineId)
        } else {
            newAssignments[machineId] = patternId
        }
        measures[barIndex] = targetMeasure.copy(patternAssignments = newAssignments)
        _project.value = _project.value.copy(songMeasures = measures)
        audioEngine.updateProject(_project.value)
    }

    fun addMeasure() {
        val measures = _project.value.songMeasures.toMutableList()
        measures.add(SongMeasure(measures.size, emptyMap()))
        _project.value = _project.value.copy(songMeasures = measures)
        audioEngine.updateProject(_project.value)
    }

    // Add Machine
    fun addNewMachine(type: MachineType, name: String) {
        vibrateShort()
        val newId = "m_" + UUID.randomUUID().toString().take(6)
        val defaultParams = when (type) {
            MachineType.SUBSYNTH -> mapOf("cutoff" to 0.7f, "resonance" to 0.3f, "osc_mix" to 0.5f, "env_a" to 0.05f, "env_d" to 0.2f, "env_s" to 0.7f, "env_r" to 0.3f)
            MachineType.BASSLINE -> mapOf("cutoff" to 0.6f, "resonance" to 0.8f, "env_mod" to 0.65f, "decay" to 0.4f, "distortion" to 0.3f)
            MachineType.BEATBOX -> emptyMap()
            MachineType.PADSYNTH -> mapOf("cutoff" to 0.65f, "attack" to 0.35f, "release" to 0.5f)
            MachineType.BIT8 -> mapOf("pulse_width" to 0.25f)
        }
        val newMachine = Machine(
            id = newId,
            name = name,
            type = type,
            colorHex = type.defaultColorHex,
            params = defaultParams,
            patterns = ProjectRepository.createEmptyPatterns(type == MachineType.BEATBOX)
        )
        _project.value = _project.value.copy(machines = _project.value.machines + newMachine)
        _selectedMachineId.value = newId
        audioEngine.updateProject(_project.value)
        _showAddMachineDialog.value = false
    }

    fun removeMachine(machineId: String) {
        if (_project.value.machines.size <= 1) return // Keep at least one
        vibrateShort()
        val filtered = _project.value.machines.filter { it.id != machineId }
        _project.value = _project.value.copy(machines = filtered)
        if (_selectedMachineId.value == machineId) {
            _selectedMachineId.value = filtered.first().id
        }
        audioEngine.updateProject(_project.value)
    }

    // Live Note Trigger from UI keyboard / pads
    fun triggerLiveNote(machineId: String, pitch: Int, velocity: Float = 0.85f) {
        audioEngine.triggerLiveNote(machineId, pitch, velocity)
    }

    fun releaseLiveNote(machineId: String, pitch: Int) {
        audioEngine.releaseLiveNote(machineId, pitch)
    }

    fun triggerLiveDrum(drumType: DrumType, velocity: Float = 0.85f) {
        vibrateShort()
        audioEngine.triggerLiveDrum(drumType, velocity)
    }

    fun setDrawerOpen(open: Boolean) {
        _isDrawerOpen.value = open
    }

    fun setOctave(octave: Int) {
        _keyboardOctave.value = octave.coerceIn(1, 6)
    }

    fun showProjectDialog(show: Boolean) {
        _showProjectDialog.value = show
    }

    fun showAddMachineDialog(show: Boolean) {
        _showAddMachineDialog.value = show
    }

    fun saveCurrentProject(newName: String? = null) {
        val proj = if (newName != null) _project.value.copy(name = newName) else _project.value
        _project.value = proj
        viewModelScope.launch {
            repository.saveProject(proj)
        }
    }

    fun loadProject(id: String) {
        viewModelScope.launch {
            val proj = repository.getProject(id)
            if (proj != null) {
                stopPlayback()
                _project.value = proj
                _selectedMachineId.value = proj.machines.firstOrNull()?.id ?: "m1"
                audioEngine.updateProject(proj)
                _showProjectDialog.value = false
            }
        }
    }

    fun createNewProject(name: String = "New Beat") {
        val newProj = Project(
            id = UUID.randomUUID().toString(),
            name = name,
            bpm = 120,
            machines = listOf(
                Machine(
                    id = "sub_main",
                    name = "Sub Synth",
                    type = MachineType.SUBSYNTH,
                    colorHex = MachineType.SUBSYNTH.defaultColorHex,
                    params = mapOf("cutoff" to 0.7f, "resonance" to 0.3f, "osc_mix" to 0.5f, "env_a" to 0.05f, "env_d" to 0.2f, "env_s" to 0.7f, "env_r" to 0.3f),
                    patterns = ProjectRepository.createEmptyPatterns(false)
                ),
                Machine(
                    id = "drum_main",
                    name = "BeatBox 909",
                    type = MachineType.BEATBOX,
                    colorHex = MachineType.BEATBOX.defaultColorHex,
                    patterns = ProjectRepository.createEmptyPatterns(true)
                )
            ),
            songMeasures = (0 until 8).map { SongMeasure(it, emptyMap()) }
        )
        stopPlayback()
        _project.value = newProj
        _selectedMachineId.value = "sub_main"
        audioEngine.updateProject(newProj)
        _showProjectDialog.value = false
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }
}
