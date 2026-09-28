package com.example.model

enum class Waveform(val label: String) {
    SAW("Saw"),
    SQUARE("Square"),
    TRIANGLE("Triangle"),
    SINE("Sine"),
    NOISE("Noise")
}

enum class MachineType(val title: String, val shortName: String, val defaultColorHex: Long) {
    SUBSYNTH("SubSynth", "SUB", 0xFF9C27B0),
    BASSLINE("BassLine 303", "303", 0xFF00E5FF),
    BEATBOX("BeatBox", "DRUM", 0xFFFF5252),
    PADSYNTH("PadSynth", "PAD", 0xFF7C4DFF),
    BIT8("8BitSynth", "8BIT", 0xFFFFD700)
}

enum class DrumType(val title: String, val shortName: String) {
    KICK("Bass Drum", "BD"),
    SNARE("Snare Drum", "SD"),
    CLOSED_HAT("Closed Hat", "CH"),
    OPEN_HAT("Open Hat", "OH"),
    CLAP("Hand Clap", "CP"),
    LOW_TOM("Low Tom", "LT"),
    HIGH_TOM("High Tom", "HT"),
    CRASH("Crash Cymbal", "CY")
}

data class Note(
    val step: Int,          // 0..15
    val pitch: Int,         // MIDI note 36..84 (e.g., 60 = Middle C)
    val length: Int = 1,    // in 16th steps
    val velocity: Float = 0.8f // 0f..1f
)

data class DrumLane(
    val drumType: DrumType,
    val steps: List<Boolean> = List(16) { false },
    val velocities: List<Float> = List(16) { 0.8f },
    val pitch: Float = 0.5f,
    val decay: Float = 0.5f,
    val punch: Float = 0.5f,
    val volume: Float = 0.8f,
    val pan: Float = 0f
)

data class Pattern(
    val id: String,         // e.g. "A1", "A2", ... "D8"
    val steps: Int = 16,
    val notes: List<Note> = emptyList(),
    val drumLanes: List<DrumLane> = emptyList()
)

data class Machine(
    val id: String,
    val name: String,
    val type: MachineType,
    val colorHex: Long,
    val volume: Float = 0.8f,
    val pan: Float = 0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val eqLow: Float = 0f,   // -1f..+1f
    val eqMid: Float = 0f,
    val eqHigh: Float = 0f,
    // Machine specific parameters:
    // SubSynth: osc1_wave, osc1_octave, osc1_semi, osc2_wave, osc2_detune, osc2_octave, osc_mix,
    //           cutoff, resonance, filter_env, env_a, env_d, env_s, env_r, lfo_rate, lfo_depth
    // BassLine: cutoff, resonance, env_mod, decay, accent, distortion, wave (0=saw, 1=sqr)
    // PadSynth: cutoff, resonance, attack, release, detune, chorus
    // 8BitSynth: pulse_width, arp_mode, bit_depth, rate_reduce
    val params: Map<String, Float> = emptyMap(),
    val activePatternId: String = "A1",
    val patterns: List<Pattern> = emptyList()
)

data class SongMeasure(
    val barIndex: Int,
    val patternAssignments: Map<String, String> = emptyMap() // machineId -> patternId (e.g. "m1" -> "A1")
)

enum class PlayMode {
    PATTERN,
    SONG
}

data class Project(
    val id: String,
    val name: String,
    val bpm: Int = 128,
    val masterVolume: Float = 0.85f,
    val delayWet: Float = 0.25f,
    val delayTime: Float = 0.375f,
    val delayFeedback: Float = 0.45f,
    val reverbWet: Float = 0.20f,
    val reverbRoom: Float = 0.5f,
    val distortionDrive: Float = 0.0f,
    val machines: List<Machine> = emptyList(),
    val songMeasures: List<SongMeasure> = emptyList()
)
