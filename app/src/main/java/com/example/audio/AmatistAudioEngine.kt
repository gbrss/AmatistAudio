package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.*
import kotlin.random.Random

class AmatistAudioEngine {

    companion object {
        const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE_SAMPLES = 1024
        private const val TWO_PI = 2.0 * Math.PI

        fun midiToFreq(pitch: Int): Double {
            return 440.0 * 2.0.pow((pitch - 69) / 12.0)
        }
    }

    private var audioTrack: AudioTrack? = null
    private var renderThread: Thread? = null
    private val isRunning = AtomicBoolean(false)

    // Playback state
    val isPlaying = AtomicBoolean(false)
    val playMode = AtomicInteger(PlayMode.PATTERN.ordinal)
    val currentStep = AtomicInteger(0)
    val currentBar = AtomicInteger(0)

    // Volatile project reference
    @Volatile
    private var projectSnapshot: Project? = null

    // Callback for sequencer position & VU meters
    var onSequencerTick: ((step: Int, bar: Int) -> Unit)? = null
    var onVuMeterUpdate: ((masterL: Float, masterR: Float, machinePeaks: Map<String, Float>) -> Unit)? = null

    // Real-time live triggers
    private val liveActiveNotes = ConcurrentHashMap<String, MutableMap<Int, LiveVoice>>()
    private val liveDrumTriggers = ConcurrentHashMap<DrumType, Float>() // velocity

    // Active sequencer voices per machine
    private val seqActiveVoices = ConcurrentHashMap<String, MutableMap<Int, LiveVoice>>()

    // Master FX states
    private val delayBufferL = FloatArray(SAMPLE_RATE * 2)
    private val delayBufferR = FloatArray(SAMPLE_RATE * 2)
    private var delayWritePos = 0

    // Simple reverb comb filters
    private val combBuffer1 = FloatArray(1116)
    private val combBuffer2 = FloatArray(1356)
    private val combBuffer3 = FloatArray(1693)
    private var combPos1 = 0
    private var combPos2 = 0
    private var combPos3 = 0

    // Drum sound states
    private class DrumVoice(
        var active: Boolean = false,
        var samplePos: Int = 0,
        var lengthSamples: Int = 0,
        var velocity: Float = 0.8f,
        var pitch: Float = 0.5f,
        var decay: Float = 0.5f,
        var punch: Float = 0.5f,
        var volume: Float = 0.8f,
        var pan: Float = 0f,
        var type: DrumType = DrumType.KICK
    )

    private val drumVoices = Array(16) { DrumVoice() }

    class LiveVoice(
        val pitch: Int,
        var velocity: Float,
        var startTimeSample: Long = 0L,
        var isReleased: Boolean = false,
        var releaseTimeSample: Long = 0L,
        var phase1: Double = 0.0,
        var phase2: Double = 0.0,
        var lfoPhase: Double = 0.0,
        var filterVal1: Double = 0.0,
        var filterVal2: Double = 0.0
    )

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = max(minBufferSize, BUFFER_SIZE_SAMPLES * 4)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    fun start() {
        if (isRunning.get()) return
        isRunning.set(true)
        audioTrack?.play()

        renderThread = Thread({
            audioLoop()
        }, "AmatistAudio-Engine").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        isRunning.set(false)
        isPlaying.set(false)
        try {
            renderThread?.join(500)
        } catch (_: Exception) {}
        renderThread = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (_: Exception) {}
    }

    fun release() {
        stop()
        try {
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    fun updateProject(project: Project) {
        this.projectSnapshot = project
    }

    fun triggerPlay(startFromBeginning: Boolean = false) {
        if (startFromBeginning) {
            currentStep.set(0)
            currentBar.set(0)
        }
        isPlaying.set(true)
    }

    fun pause() {
        isPlaying.set(false)
    }

    fun stopPlayback() {
        isPlaying.set(false)
        currentStep.set(0)
        currentBar.set(0)
        seqActiveVoices.clear()
    }

    fun setStep(step: Int) {
        currentStep.set(step % 16)
    }

    fun setBar(bar: Int) {
        currentBar.set(max(0, bar))
    }

    fun triggerLiveNote(machineId: String, pitch: Int, velocity: Float) {
        val notes = liveActiveNotes.getOrPut(machineId) { ConcurrentHashMap() }
        notes[pitch] = LiveVoice(
            pitch = pitch,
            velocity = velocity,
            startTimeSample = 0L
        )
    }

    fun releaseLiveNote(machineId: String, pitch: Int) {
        val notes = liveActiveNotes[machineId] ?: return
        val voice = notes[pitch] ?: return
        voice.isReleased = true
        voice.releaseTimeSample = 0L
    }

    fun triggerLiveDrum(drumType: DrumType, velocity: Float) {
        liveDrumTriggers[drumType] = velocity
    }

    private fun audioLoop() {
        val buffer = ShortArray(BUFFER_SIZE_SAMPLES * 2)
        var sampleCounter = 0L
        var samplesPerStep = (SAMPLE_RATE * 60.0 / (128.0 * 4.0)).toLong()
        var stepSampleCounter = 0L

        var lastStepReported = -1
        var lastBarReported = -1
        var vuReportCounter = 0

        val machinePeaks = mutableMapOf<String, Float>()
        var masterPeakL = 0f
        var masterPeakR = 0f

        while (isRunning.get()) {
            val project = projectSnapshot
            val bpm = project?.bpm ?: 128
            samplesPerStep = (SAMPLE_RATE * 60.0 / (bpm * 4.0)).toLong()

            // Process one audio buffer
            var i = 0
            while (i < BUFFER_SIZE_SAMPLES) {
                // Check sequencer step clock
                if (isPlaying.get() && project != null) {
                    if (stepSampleCounter >= samplesPerStep) {
                        stepSampleCounter = 0L
                        val nextStep = (currentStep.get() + 1) % 16
                        currentStep.set(nextStep)

                        if (nextStep == 0) {
                            if (playMode.get() == PlayMode.SONG.ordinal) {
                                val maxBars = max(1, project.songMeasures.size)
                                val nextBar = (currentBar.get() + 1) % maxBars
                                currentBar.set(nextBar)
                            }
                        }

                        // Fire notes for this step
                        triggerSequencerNotesForStep(project, currentStep.get(), currentBar.get())
                    }
                    stepSampleCounter++
                }

                // Check live drum triggers
                if (liveDrumTriggers.isNotEmpty()) {
                    for ((type, vel) in liveDrumTriggers) {
                        triggerDrumInternal(project, type, vel)
                    }
                    liveDrumTriggers.clear()
                }

                // Synthesize stereo sample (left, right)
                var outL = 0f
                var outR = 0f

                project?.machines?.forEach { machine ->
                    if (machine.isMuted) return@forEach

                    var machineSample = 0f

                    when (machine.type) {
                        MachineType.SUBSYNTH -> {
                            machineSample += renderSubSynth(machine, sampleCounter)
                        }
                        MachineType.BASSLINE -> {
                            machineSample += renderBassLine(machine, sampleCounter)
                        }
                        MachineType.BEATBOX -> {
                            machineSample += renderBeatBox(machine)
                        }
                        MachineType.PADSYNTH -> {
                            machineSample += renderPadSynth(machine, sampleCounter)
                        }
                        MachineType.BIT8 -> {
                            machineSample += renderBit8Synth(machine, sampleCounter)
                        }
                    }

                    // Machine pan & volume
                    val vol = machine.volume
                    val pan = machine.pan // -1..1
                    val panL = cos((pan + 1f) * Math.PI / 4.0).toFloat()
                    val panR = sin((pan + 1f) * Math.PI / 4.0).toFloat()

                    val sL = machineSample * vol * panL
                    val sR = machineSample * vol * panR

                    outL += sL
                    outR += sR

                    val currentMachinePeak = max(abs(sL), abs(sR))
                    val prevPeak = machinePeaks[machine.id] ?: 0f
                    machinePeaks[machine.id] = max(prevPeak * 0.999f, currentMachinePeak)
                }

                // Master FX: Delay & Reverb
                val wetDelay = project?.delayWet ?: 0.2f
                val delayTimeSec = (project?.delayTime ?: 0.375f).coerceIn(0.05f, 1.0f)
                val delaySamples = (delayTimeSec * SAMPLE_RATE).toInt()
                val delayFb = (project?.delayFeedback ?: 0.4f).coerceIn(0.0f, 0.85f)

                if (wetDelay > 0.001f) {
                    val readPos = (delayWritePos - delaySamples + delayBufferL.size) % delayBufferL.size
                    val dL = delayBufferL[readPos]
                    val dR = delayBufferR[readPos]

                    delayBufferL[delayWritePos] = outL + dL * delayFb
                    delayBufferR[delayWritePos] = outR + dR * delayFb

                    outL += dL * wetDelay
                    outR += dR * wetDelay
                }
                delayWritePos = (delayWritePos + 1) % delayBufferL.size

                // Master FX: Reverb
                val wetReverb = project?.reverbWet ?: 0.2f
                if (wetReverb > 0.001f) {
                    val inputMono = (outL + outR) * 0.5f
                    val c1 = combBuffer1[combPos1]
                    val c2 = combBuffer2[combPos2]
                    val c3 = combBuffer3[combPos3]

                    combBuffer1[combPos1] = inputMono + c1 * 0.75f
                    combBuffer2[combPos2] = inputMono + c2 * 0.78f
                    combBuffer3[combPos3] = inputMono + c3 * 0.80f

                    combPos1 = (combPos1 + 1) % combBuffer1.size
                    combPos2 = (combPos2 + 1) % combBuffer2.size
                    combPos3 = (combPos3 + 1) % combBuffer3.size

                    val reverbOut = (c1 - c2 + c3) * 0.33f
                    outL += reverbOut * wetReverb
                    outR += reverbOut * wetReverb
                }

                // Master Distortion / Drive
                val drive = project?.distortionDrive ?: 0f
                if (drive > 0.01f) {
                    val gain = 1f + drive * 5f
                    outL = tanh(outL * gain)
                    outR = tanh(outR * gain)
                }

                // Master Volume
                val masterVol = project?.masterVolume ?: 0.85f
                outL *= masterVol
                outR *= masterVol

                // Soft Limiter / Anti-clipping
                outL = outL.coerceIn(-1.0f, 1.0f)
                outR = outR.coerceIn(-1.0f, 1.0f)

                masterPeakL = max(masterPeakL * 0.999f, abs(outL))
                masterPeakR = max(masterPeakR * 0.999f, abs(outR))

                // Convert to 16-bit PCM
                buffer[i * 2] = (outL * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                buffer[i * 2 + 1] = (outR * 32767f).toInt().coerceIn(-32768, 32767).toShort()

                sampleCounter++
                i++
            }

            // Write PCM buffer to audio track
            audioTrack?.write(buffer, 0, buffer.size)

            // UI notifications
            val currentS = currentStep.get()
            val currentB = currentBar.get()
            if (currentS != lastStepReported || currentB != lastBarReported) {
                lastStepReported = currentS
                lastBarReported = currentB
                onSequencerTick?.invoke(currentS, currentB)
            }

            vuReportCounter++
            if (vuReportCounter >= 8) { // ~30 fps
                vuReportCounter = 0
                val pL = masterPeakL
                val pR = masterPeakR
                val pMap = HashMap(machinePeaks)
                onVuMeterUpdate?.invoke(pL, pR, pMap)
            }
        }
    }

    private fun triggerSequencerNotesForStep(project: Project, step: Int, bar: Int) {
        val isSong = playMode.get() == PlayMode.SONG.ordinal
        val measure = if (isSong && bar < project.songMeasures.size) project.songMeasures[bar] else null

        project.machines.forEach { machine ->
            if (machine.isMuted) return@forEach

            val activePatternId = if (isSong && measure != null) {
                measure.patternAssignments[machine.id] ?: machine.activePatternId
            } else {
                machine.activePatternId
            }

            val pattern = machine.patterns.find { it.id == activePatternId } ?: return@forEach

            if (machine.type == MachineType.BEATBOX) {
                pattern.drumLanes.forEach { lane ->
                    if (step < lane.steps.size && lane.steps[step]) {
                        val vel = if (step < lane.velocities.size) lane.velocities[step] else 0.8f
                        triggerDrumInternal(project, lane.drumType, vel)
                    }
                }
            } else {
                val stepNotes = pattern.notes.filter { it.step == step }
                val activeMap = seqActiveVoices.getOrPut(machine.id) { ConcurrentHashMap() }
                // Release old notes
                activeMap.values.forEach { it.isReleased = true }

                stepNotes.forEach { note ->
                    activeMap[note.pitch] = LiveVoice(
                        pitch = note.pitch,
                        velocity = note.velocity,
                        startTimeSample = 0L
                    )
                }
            }
        }
    }

    private fun triggerDrumInternal(project: Project?, drumType: DrumType, velocity: Float) {
        val machine = project?.machines?.find { it.type == MachineType.BEATBOX }
        val activePattern = machine?.patterns?.find { it.id == machine.activePatternId }
        val lane = activePattern?.drumLanes?.find { it.drumType == drumType }

        // Find available drum voice
        val voice = drumVoices.find { !it.active } ?: drumVoices[0]
        voice.active = true
        voice.samplePos = 0
        voice.velocity = velocity
        voice.pitch = lane?.pitch ?: 0.5f
        voice.decay = lane?.decay ?: 0.5f
        voice.punch = lane?.punch ?: 0.5f
        voice.volume = lane?.volume ?: 0.8f
        voice.pan = lane?.pan ?: 0f
        voice.type = drumType

        val decaySec = when (drumType) {
            DrumType.KICK -> 0.15f + voice.decay * 0.45f
            DrumType.SNARE -> 0.10f + voice.decay * 0.35f
            DrumType.CLOSED_HAT -> 0.03f + voice.decay * 0.08f
            DrumType.OPEN_HAT -> 0.15f + voice.decay * 0.6f
            DrumType.CLAP -> 0.12f + voice.decay * 0.3f
            DrumType.LOW_TOM -> 0.2f + voice.decay * 0.5f
            DrumType.HIGH_TOM -> 0.15f + voice.decay * 0.4f
            DrumType.CRASH -> 0.4f + voice.decay * 1.2f
        }
        voice.lengthSamples = (decaySec * SAMPLE_RATE).toInt()
    }

    // Synthesize SubSynth (Analog Subtractive)
    private fun renderSubSynth(machine: Machine, sampleCounter: Long): Float {
        val liveNotes = liveActiveNotes[machine.id]
        val seqNotes = seqActiveVoices[machine.id]

        var totalSample = 0f

        val cutoffNorm = machine.params["cutoff"] ?: 0.7f
        val resNorm = machine.params["resonance"] ?: 0.3f
        val oscMix = machine.params["osc_mix"] ?: 0.5f // 0=osc1, 1=osc2
        val wave1 = (machine.params["osc1_wave"] ?: 0f).toInt() // 0=saw, 1=sqr, 2=tri, 3=sin
        val wave2 = (machine.params["osc2_wave"] ?: 1f).toInt()
        val osc2Detune = (machine.params["osc2_detune"] ?: 0.1f) * 0.04f

        val attackSamples = ((machine.params["env_a"] ?: 0.05f) * SAMPLE_RATE * 2f).toLong().coerceAtLeast(10L)
        val decaySamples = ((machine.params["env_d"] ?: 0.2f) * SAMPLE_RATE * 2f).toLong().coerceAtLeast(10L)
        val sustainLevel = (machine.params["env_s"] ?: 0.7f).coerceIn(0f, 1f)
        val releaseSamples = ((machine.params["env_r"] ?: 0.3f) * SAMPLE_RATE * 2f).toLong().coerceAtLeast(10L)

        fun processVoiceList(voices: MutableMap<Int, LiveVoice>) {
            val iterator = voices.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val voice = entry.value

                // ADSR amplitude
                val age = voice.startTimeSample
                val amp = if (!voice.isReleased) {
                    if (age < attackSamples) {
                        (age.toFloat() / attackSamples)
                    } else if (age < attackSamples + decaySamples) {
                        1f - (1f - sustainLevel) * ((age - attackSamples).toFloat() / decaySamples)
                    } else {
                        sustainLevel
                    }
                } else {
                    val relAge = voice.releaseTimeSample
                    if (relAge >= releaseSamples) {
                        iterator.remove()
                        continue
                    }
                    sustainLevel * (1f - (relAge.toFloat() / releaseSamples))
                }

                if (!voice.isReleased) {
                    voice.startTimeSample++
                } else {
                    voice.releaseTimeSample++
                }

                val baseFreq = midiToFreq(voice.pitch)
                val freq1 = baseFreq
                val freq2 = baseFreq * (1.0 + osc2Detune)

                voice.phase1 = (voice.phase1 + freq1 / SAMPLE_RATE) % 1.0
                voice.phase2 = (voice.phase2 + freq2 / SAMPLE_RATE) % 1.0

                val s1 = generateWaveform(wave1, voice.phase1)
                val s2 = generateWaveform(wave2, voice.phase2)
                val mixedOsc = s1 * (1f - oscMix) + s2 * oscMix

                // Resonant lowpass filter
                val cutoffFreq = (30.0 + (cutoffNorm.toDouble().pow(2.0)) * 12000.0).coerceIn(20.0, 18000.0)
                val q = 0.5 + resNorm * 4.0
                val f = 2.0 * sin(Math.PI * cutoffFreq / SAMPLE_RATE)
                val damp = min(2.0 * (1.0 - q.pow(0.25)), min(2.0, 2.0 / f - f * 0.5))

                voice.filterVal1 += f * (mixedOsc - voice.filterVal1 - damp * voice.filterVal2)
                voice.filterVal2 += f * voice.filterVal1

                val filtered = voice.filterVal2.toFloat()
                totalSample += filtered * amp * voice.velocity
            }
        }

        liveNotes?.let { processVoiceList(it) }
        seqNotes?.let { processVoiceList(it) }

        return totalSample
    }

    // Synthesize Acid BassLine (TB-303 style)
    private fun renderBassLine(machine: Machine, sampleCounter: Long): Float {
        val liveNotes = liveActiveNotes[machine.id]
        val seqNotes = seqActiveVoices[machine.id]

        var totalSample = 0f
        val cutoffNorm = machine.params["cutoff"] ?: 0.5f
        val resNorm = machine.params["resonance"] ?: 0.75f
        val envMod = machine.params["env_mod"] ?: 0.6f
        val decayNorm = machine.params["decay"] ?: 0.4f
        val distortion = machine.params["distortion"] ?: 0.3f
        val waveType = (machine.params["wave"] ?: 0f).toInt() // 0=saw, 1=sqr

        val decaySamples = (0.05f + decayNorm * 0.8f) * SAMPLE_RATE

        fun processVoiceList(voices: MutableMap<Int, LiveVoice>) {
            val iterator = voices.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val voice = entry.value

                val age = voice.startTimeSample
                val envVal = exp(-age.toDouble() / (decaySamples * 0.5)).toFloat()

                if (age > decaySamples * 2.5) {
                    iterator.remove()
                    continue
                }
                voice.startTimeSample++

                val freq = midiToFreq(voice.pitch)
                voice.phase1 = (voice.phase1 + freq / SAMPLE_RATE) % 1.0

                // 303 Saw or Square
                val raw = if (waveType == 0) {
                    (2.0 * voice.phase1 - 1.0).toFloat()
                } else {
                    if (voice.phase1 < 0.5) 1.0f else -1.0f
                }

                // 303 resonant filter sweep
                val sweepCutoff = (cutoffNorm + envMod * envVal * 0.6f).coerceIn(0.05f, 0.98f)
                val cutoffFreq = (50.0 + sweepCutoff.toDouble().pow(3.0) * 10000.0).coerceIn(40.0, 14000.0)
                val resonance = (0.7 + resNorm * 5.0)

                val f = 2.0 * sin(Math.PI * cutoffFreq / SAMPLE_RATE)
                voice.filterVal1 += f * (raw - voice.filterVal1 - (1.0 / resonance) * voice.filterVal2)
                voice.filterVal2 += f * voice.filterVal1

                var out = voice.filterVal2.toFloat()

                // Overdrive / Distortion
                if (distortion > 0.05f) {
                    val drive = 1f + distortion * 8f
                    out = tanh(out * drive)
                }

                totalSample += out * envVal * voice.velocity
            }
        }

        liveNotes?.let { processVoiceList(it) }
        seqNotes?.let { processVoiceList(it) }

        return totalSample
    }

    // Synthesize BeatBox 8-channel drums
    private fun renderBeatBox(machine: Machine): Float {
        var mix = 0f

        for (voice in drumVoices) {
            if (!voice.active) continue

            val pos = voice.samplePos
            val len = voice.lengthSamples
            if (pos >= len) {
                voice.active = false
                continue
            }
            voice.samplePos++

            val t = pos.toFloat() / SAMPLE_RATE
            val progress = pos.toFloat() / len
            val env = exp(-progress * (3.0f + voice.decay * 5f))

            var sample = 0f

            when (voice.type) {
                DrumType.KICK -> {
                    // Pitch drops exponentially from 180Hz down to 45Hz
                    val startFreq = 160f + voice.pitch * 80f
                    val endFreq = 42f + voice.pitch * 20f
                    val curFreq = endFreq + (startFreq - endFreq) * exp(-pos.toFloat() / (SAMPLE_RATE * 0.035f))
                    val phase = curFreq * t * TWO_PI
                    val click = if (pos < 120) (1.0f - pos / 120f) * (voice.punch * 0.8f) else 0f
                    sample = (sin(phase).toFloat() * env * 1.2f + click)
                }
                DrumType.SNARE -> {
                    val toneFreq1 = 180f + voice.pitch * 50f
                    val toneFreq2 = 330f + voice.pitch * 80f
                    val tone = (sin(toneFreq1 * t * TWO_PI) * 0.6 + sin(toneFreq2 * t * TWO_PI) * 0.4).toFloat() * exp(-t * 22f)
                    val noise = (Random.nextFloat() * 2f - 1f) * exp(-t * (10f + (1f - voice.decay) * 20f))
                    val snap = if (pos < 150) (1f - pos / 150f) * voice.punch else 0f
                    sample = tone * 0.5f + noise * 0.6f + snap * 0.4f
                }
                DrumType.CLOSED_HAT -> {
                    val metallic = (sin(t * 3140.0) + sin(t * 4150.0) + sin(t * 5280.0) + sin(t * 6800.0) + (Random.nextFloat() * 2f - 1f) * 1.5).toFloat()
                    sample = metallic * env * 0.4f
                }
                DrumType.OPEN_HAT -> {
                    val metallic = (sin(t * 3140.0) + sin(t * 4150.0) + sin(t * 5280.0) + sin(t * 6800.0) + (Random.nextFloat() * 2f - 1f) * 1.2).toFloat()
                    sample = metallic * env * 0.45f
                }
                DrumType.CLAP -> {
                    // 3 initial mini bursts + body
                    val burstT = pos.toFloat() / SAMPLE_RATE
                    val clapEnv = if (burstT < 0.012f) 0.8f
                    else if (burstT < 0.024f) 0.9f
                    else if (burstT < 0.036f) 1.0f
                    else exp(-(burstT - 0.036f) * 18f)
                    val noise = (Random.nextFloat() * 2f - 1f)
                    sample = noise * clapEnv * 0.6f
                }
                DrumType.LOW_TOM -> {
                    val freq = 90f + (120f - 90f) * exp(-t * 15f)
                    sample = sin(freq * t * TWO_PI).toFloat() * env * 0.9f
                }
                DrumType.HIGH_TOM -> {
                    val freq = 160f + (220f - 160f) * exp(-t * 18f)
                    sample = sin(freq * t * TWO_PI).toFloat() * env * 0.9f
                }
                DrumType.CRASH -> {
                    val noise = (Random.nextFloat() * 2f - 1f)
                    sample = noise * env * 0.5f
                }
            }

            mix += sample * voice.volume * voice.velocity
        }

        return mix
    }

    // Synthesize PadSynth (Warm lush multi-oscillator chords)
    private fun renderPadSynth(machine: Machine, sampleCounter: Long): Float {
        val liveNotes = liveActiveNotes[machine.id]
        val seqNotes = seqActiveVoices[machine.id]

        var totalSample = 0f
        val attackSec = 0.2f + (machine.params["attack"] ?: 0.3f) * 1.5f
        val releaseSec = 0.3f + (machine.params["release"] ?: 0.4f) * 2.0f
        val cutoff = machine.params["cutoff"] ?: 0.6f
        val attackSamples = (attackSec * SAMPLE_RATE).toLong()
        val releaseSamples = (releaseSec * SAMPLE_RATE).toLong()

        fun processVoiceList(voices: MutableMap<Int, LiveVoice>) {
            val iterator = voices.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val voice = entry.value

                val amp = if (!voice.isReleased) {
                    if (voice.startTimeSample < attackSamples) {
                        (voice.startTimeSample.toFloat() / attackSamples)
                    } else 1.0f
                } else {
                    val relAge = voice.releaseTimeSample
                    if (relAge >= releaseSamples) {
                        iterator.remove()
                        continue
                    }
                    1.0f - (relAge.toFloat() / releaseSamples)
                }

                if (!voice.isReleased) voice.startTimeSample++ else voice.releaseTimeSample++

                val freq = midiToFreq(voice.pitch)
                voice.phase1 = (voice.phase1 + freq / SAMPLE_RATE) % 1.0
                voice.phase2 = (voice.phase2 + (freq * 1.004) / SAMPLE_RATE) % 1.0

                val s1 = (2.0 * voice.phase1 - 1.0).toFloat()
                val s2 = (2.0 * voice.phase2 - 1.0).toFloat()
                val combined = (s1 + s2) * 0.5f

                // Gentle lowpass
                voice.filterVal1 += (cutoff * 0.2) * (combined - voice.filterVal1)
                totalSample += voice.filterVal1.toFloat() * amp * voice.velocity * 0.7f
            }
        }

        liveNotes?.let { processVoiceList(it) }
        seqNotes?.let { processVoiceList(it) }

        return totalSample
    }

    // Synthesize 8BitSynth (NES/Chiptune)
    private fun renderBit8Synth(machine: Machine, sampleCounter: Long): Float {
        val liveNotes = liveActiveNotes[machine.id]
        val seqNotes = seqActiveVoices[machine.id]

        var totalSample = 0f
        val pulseWidth = (machine.params["pulse_width"] ?: 0.5f).coerceIn(0.125f, 0.875f)

        fun processVoiceList(voices: MutableMap<Int, LiveVoice>) {
            val iterator = voices.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val voice = entry.value

                if (voice.isReleased && voice.releaseTimeSample > SAMPLE_RATE * 0.2f) {
                    iterator.remove()
                    continue
                }
                if (!voice.isReleased) voice.startTimeSample++ else voice.releaseTimeSample++

                val freq = midiToFreq(voice.pitch)
                voice.phase1 = (voice.phase1 + freq / SAMPLE_RATE) % 1.0

                val raw = if (voice.phase1 < pulseWidth) 0.6f else -0.6f
                // Lo-fi 4-bit quantize
                val bitQuantized = (round(raw * 4.0) / 4.0).toFloat()

                totalSample += bitQuantized * voice.velocity
            }
        }

        liveNotes?.let { processVoiceList(it) }
        seqNotes?.let { processVoiceList(it) }

        return totalSample
    }

    private fun generateWaveform(type: Int, phase: Double): Float {
        return when (type) {
            0 -> (2.0 * phase - 1.0).toFloat() // Saw
            1 -> (if (phase < 0.5) 1.0 else -1.0).toFloat() // Square
            2 -> (if (phase < 0.5) 4.0 * phase - 1.0 else 3.0 - 4.0 * phase).toFloat() // Triangle
            3 -> sin(phase * TWO_PI).toFloat() // Sine
            4 -> (Random.nextFloat() * 2f - 1f) // Noise
            else -> sin(phase * TWO_PI).toFloat()
        }
    }
}
