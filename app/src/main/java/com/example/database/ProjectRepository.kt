package com.example.database

import com.example.model.*
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ProjectRepository(private val projectDao: ProjectDao) {

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun saveProject(project: Project) {
        val json = serializeProject(project)
        val entity = ProjectEntity(
            id = project.id,
            name = project.name,
            bpm = project.bpm,
            updatedAt = System.currentTimeMillis(),
            jsonPayload = json
        )
        projectDao.insertProject(entity)
    }

    suspend fun getProject(id: String): Project? {
        val entity = projectDao.getProjectById(id) ?: return null
        return deserializeProject(entity.jsonPayload)
    }

    suspend fun deleteProject(id: String) {
        projectDao.deleteProjectById(id)
    }

    suspend fun seedInitialProjectsIfNeeded() {
        val defaultDemo = createDemoProjectAmethyst()
        val exists = projectDao.getProjectById(defaultDemo.id)
        if (exists == null) {
            saveProject(defaultDemo)
            saveProject(createDemoProjectAcid())
            saveProject(createDemoProjectChiptune())
        }
    }

    fun serializeProject(project: Project): String {
        val root = JSONObject()
        root.put("id", project.id)
        root.put("name", project.name)
        root.put("bpm", project.bpm)
        root.put("masterVolume", project.masterVolume.toDouble())
        root.put("delayWet", project.delayWet.toDouble())
        root.put("delayTime", project.delayTime.toDouble())
        root.put("delayFeedback", project.delayFeedback.toDouble())
        root.put("reverbWet", project.reverbWet.toDouble())
        root.put("reverbRoom", project.reverbRoom.toDouble())
        root.put("distortionDrive", project.distortionDrive.toDouble())

        // Machines
        val machinesArr = JSONArray()
        project.machines.forEach { m ->
            val mObj = JSONObject()
            mObj.put("id", m.id)
            mObj.put("name", m.name)
            mObj.put("type", m.type.name)
            mObj.put("colorHex", m.colorHex)
            mObj.put("volume", m.volume.toDouble())
            mObj.put("pan", m.pan.toDouble())
            mObj.put("isMuted", m.isMuted)
            mObj.put("isSolo", m.isSolo)
            mObj.put("eqLow", m.eqLow.toDouble())
            mObj.put("eqMid", m.eqMid.toDouble())
            mObj.put("eqHigh", m.eqHigh.toDouble())
            mObj.put("activePatternId", m.activePatternId)

            val pObj = JSONObject()
            m.params.forEach { (k, v) -> pObj.put(k, v.toDouble()) }
            mObj.put("params", pObj)

            val patArr = JSONArray()
            m.patterns.forEach { pat ->
                val patObj = JSONObject()
                patObj.put("id", pat.id)
                patObj.put("steps", pat.steps)

                val notesArr = JSONArray()
                pat.notes.forEach { n ->
                    val nObj = JSONObject()
                    nObj.put("step", n.step)
                    nObj.put("pitch", n.pitch)
                    nObj.put("length", n.length)
                    nObj.put("velocity", n.velocity.toDouble())
                    notesArr.put(nObj)
                }
                patObj.put("notes", notesArr)

                val drumLanesArr = JSONArray()
                pat.drumLanes.forEach { lane ->
                    val dObj = JSONObject()
                    dObj.put("drumType", lane.drumType.name)
                    dObj.put("pitch", lane.pitch.toDouble())
                    dObj.put("decay", lane.decay.toDouble())
                    dObj.put("punch", lane.punch.toDouble())
                    dObj.put("volume", lane.volume.toDouble())
                    dObj.put("pan", lane.pan.toDouble())

                    val stepsArr = JSONArray()
                    lane.steps.forEach { stepsArr.put(it) }
                    dObj.put("steps", stepsArr)

                    val velsArr = JSONArray()
                    lane.velocities.forEach { velsArr.put(it.toDouble()) }
                    dObj.put("velocities", velsArr)

                    drumLanesArr.put(dObj)
                }
                patObj.put("drumLanes", drumLanesArr)

                patArr.put(patObj)
            }
            mObj.put("patterns", patArr)
            machinesArr.put(mObj)
        }
        root.put("machines", machinesArr)

        // Song measures
        val measuresArr = JSONArray()
        project.songMeasures.forEach { sm ->
            val smObj = JSONObject()
            smObj.put("barIndex", sm.barIndex)
            val assignObj = JSONObject()
            sm.patternAssignments.forEach { (k, v) -> assignObj.put(k, v) }
            smObj.put("assignments", assignObj)
            measuresArr.put(smObj)
        }
        root.put("songMeasures", measuresArr)

        return root.toString()
    }

    fun deserializeProject(jsonStr: String): Project {
        val root = JSONObject(jsonStr)
        val id = root.optString("id", UUID.randomUUID().toString())
        val name = root.optString("name", "Untitled")
        val bpm = root.optInt("bpm", 128)
        val masterVolume = root.optDouble("masterVolume", 0.85).toFloat()
        val delayWet = root.optDouble("delayWet", 0.25).toFloat()
        val delayTime = root.optDouble("delayTime", 0.375).toFloat()
        val delayFeedback = root.optDouble("delayFeedback", 0.45).toFloat()
        val reverbWet = root.optDouble("reverbWet", 0.20).toFloat()
        val reverbRoom = root.optDouble("reverbRoom", 0.5).toFloat()
        val distortionDrive = root.optDouble("distortionDrive", 0.0).toFloat()

        val machinesList = mutableListOf<Machine>()
        val machinesArr = root.optJSONArray("machines")
        if (machinesArr != null) {
            for (i in 0 until machinesArr.length()) {
                val mObj = machinesArr.getJSONObject(i)
                val mId = mObj.getString("id")
                val mName = mObj.getString("name")
                val mType = MachineType.valueOf(mObj.optString("type", MachineType.SUBSYNTH.name))
                val colorHex = mObj.optLong("colorHex", mType.defaultColorHex)
                val vol = mObj.optDouble("volume", 0.8).toFloat()
                val pan = mObj.optDouble("pan", 0.0).toFloat()
                val isMuted = mObj.optBoolean("isMuted", false)
                val isSolo = mObj.optBoolean("isSolo", false)
                val eqLow = mObj.optDouble("eqLow", 0.0).toFloat()
                val eqMid = mObj.optDouble("eqMid", 0.0).toFloat()
                val eqHigh = mObj.optDouble("eqHigh", 0.0).toFloat()
                val activePatternId = mObj.optString("activePatternId", "A1")

                val paramsMap = mutableMapOf<String, Float>()
                val pObj = mObj.optJSONObject("params")
                pObj?.keys()?.forEach { k ->
                    paramsMap[k] = pObj.optDouble(k, 0.0).toFloat()
                }

                val patternsList = mutableListOf<Pattern>()
                val patArr = mObj.optJSONArray("patterns")
                if (patArr != null) {
                    for (p in 0 until patArr.length()) {
                        val patObj = patArr.getJSONObject(p)
                        val pId = patObj.getString("id")
                        val steps = patObj.optInt("steps", 16)

                        val notesList = mutableListOf<Note>()
                        val notesArr = patObj.optJSONArray("notes")
                        if (notesArr != null) {
                            for (n in 0 until notesArr.length()) {
                                val nObj = notesArr.getJSONObject(n)
                                notesList.add(
                                    Note(
                                        step = nObj.getInt("step"),
                                        pitch = nObj.getInt("pitch"),
                                        length = nObj.optInt("length", 1),
                                        velocity = nObj.optDouble("velocity", 0.8).toFloat()
                                    )
                                )
                            }
                        }

                        val drumLanesList = mutableListOf<DrumLane>()
                        val drumArr = patObj.optJSONArray("drumLanes")
                        if (drumArr != null) {
                            for (d in 0 until drumArr.length()) {
                                val dObj = drumArr.getJSONObject(d)
                                val dType = DrumType.valueOf(dObj.getString("drumType"))
                                val stepsList = mutableListOf<Boolean>()
                                val sArr = dObj.optJSONArray("steps")
                                if (sArr != null) {
                                    for (s in 0 until sArr.length()) {
                                        stepsList.add(sArr.getBoolean(s))
                                    }
                                }
                                val velsList = mutableListOf<Float>()
                                val vArr = dObj.optJSONArray("velocities")
                                if (vArr != null) {
                                    for (v in 0 until vArr.length()) {
                                        velsList.add(vArr.getDouble(v).toFloat())
                                    }
                                }
                                drumLanesList.add(
                                    DrumLane(
                                        drumType = dType,
                                        steps = stepsList,
                                        velocities = velsList,
                                        pitch = dObj.optDouble("pitch", 0.5).toFloat(),
                                        decay = dObj.optDouble("decay", 0.5).toFloat(),
                                        punch = dObj.optDouble("punch", 0.5).toFloat(),
                                        volume = dObj.optDouble("volume", 0.8).toFloat(),
                                        pan = dObj.optDouble("pan", 0.0).toFloat()
                                    )
                                )
                            }
                        }

                        patternsList.add(Pattern(pId, steps, notesList, drumLanesList))
                    }
                }

                machinesList.add(
                    Machine(
                        id = mId,
                        name = mName,
                        type = mType,
                        colorHex = colorHex,
                        volume = vol,
                        pan = pan,
                        isMuted = isMuted,
                        isSolo = isSolo,
                        eqLow = eqLow,
                        eqMid = eqMid,
                        eqHigh = eqHigh,
                        params = paramsMap,
                        activePatternId = activePatternId,
                        patterns = patternsList
                    )
                )
            }
        }

        val measuresList = mutableListOf<SongMeasure>()
        val measuresArr = root.optJSONArray("songMeasures")
        if (measuresArr != null) {
            for (m in 0 until measuresArr.length()) {
                val smObj = measuresArr.getJSONObject(m)
                val barIndex = smObj.getInt("barIndex")
                val assignMap = mutableMapOf<String, String>()
                val aObj = smObj.optJSONObject("assignments")
                aObj?.keys()?.forEach { k ->
                    assignMap[k] = aObj.getString(k)
                }
                measuresList.add(SongMeasure(barIndex, assignMap))
            }
        }

        return Project(
            id = id,
            name = name,
            bpm = bpm,
            masterVolume = masterVolume,
            delayWet = delayWet,
            delayTime = delayTime,
            delayFeedback = delayFeedback,
            reverbWet = reverbWet,
            reverbRoom = reverbRoom,
            distortionDrive = distortionDrive,
            machines = machinesList,
            songMeasures = measuresList
        )
    }

    companion object {
        fun createEmptyPatterns(isDrums: Boolean): List<Pattern> {
            val list = mutableListOf<Pattern>()
            val banks = listOf("A", "B", "C", "D")
            for (b in banks) {
                for (num in 1..8) {
                    val pid = "$b$num"
                    val lanes = if (isDrums) {
                        DrumType.entries.map { type ->
                            DrumLane(drumType = type, steps = List(16) { false }, velocities = List(16) { 0.8f })
                        }
                    } else emptyList()
                    list.add(Pattern(id = pid, steps = 16, notes = emptyList(), drumLanes = lanes))
                }
            }
            return list
        }

        fun createDemoProjectAmethyst(): Project {
            val mSubId = "sub1"
            val mBassId = "bass1"
            val mDrumsId = "drums1"
            val mPadId = "pad1"

            // SubSynth Lead
            val subPatterns = createEmptyPatterns(false).toMutableList()
            // Pattern A1: melodic hook in D minor
            // Notes: D4 (62), F4 (65), A4 (69), G4 (67), F4 (65), E4 (64), D4 (62), C4 (60)
            val subNotesA1 = listOf(
                Note(0, 62, 2, 0.9f),
                Note(2, 65, 2, 0.85f),
                Note(4, 69, 3, 0.95f),
                Note(8, 67, 2, 0.85f),
                Note(10, 65, 2, 0.8f),
                Note(12, 64, 2, 0.85f),
                Note(14, 62, 2, 0.9f)
            )
            subPatterns[0] = Pattern("A1", 16, subNotesA1, emptyList())

            val subMachine = Machine(
                id = mSubId,
                name = "Amethyst Lead",
                type = MachineType.SUBSYNTH,
                colorHex = 0xFFAB47BC,
                volume = 0.8f,
                pan = -0.1f,
                params = mapOf(
                    "cutoff" to 0.72f,
                    "resonance" to 0.35f,
                    "osc_mix" to 0.45f,
                    "osc1_wave" to 0f, // Saw
                    "osc2_wave" to 1f, // Square
                    "osc2_detune" to 0.15f,
                    "env_a" to 0.02f,
                    "env_d" to 0.25f,
                    "env_s" to 0.65f,
                    "env_r" to 0.35f
                ),
                activePatternId = "A1",
                patterns = subPatterns
            )

            // BassLine 303: Acid bass
            val bassPatterns = createEmptyPatterns(false).toMutableList()
            // 16th rolling acid line: D2(38), D2, D3(50), D2, F2(41), D2, G2(43), A2(45)
            val bassNotesA1 = listOf(
                Note(0, 38, 1, 0.9f),
                Note(1, 38, 1, 0.6f),
                Note(2, 50, 1, 1.0f),
                Note(3, 38, 1, 0.6f),
                Note(4, 41, 1, 0.85f),
                Note(6, 38, 1, 0.7f),
                Note(7, 43, 1, 0.9f),
                Note(8, 38, 1, 0.85f),
                Note(10, 45, 1, 0.95f),
                Note(12, 38, 1, 0.85f),
                Note(14, 43, 1, 0.85f),
                Note(15, 41, 1, 0.75f)
            )
            bassPatterns[0] = Pattern("A1", 16, bassNotesA1, emptyList())

            val bassMachine = Machine(
                id = mBassId,
                name = "Acid Line 303",
                type = MachineType.BASSLINE,
                colorHex = 0xFF00E5FF,
                volume = 0.85f,
                pan = 0f,
                params = mapOf(
                    "cutoff" to 0.52f,
                    "resonance" to 0.82f,
                    "env_mod" to 0.68f,
                    "decay" to 0.42f,
                    "distortion" to 0.35f,
                    "wave" to 0f // Saw
                ),
                activePatternId = "A1",
                patterns = bassPatterns
            )

            // BeatBox: 8 Drum Channels
            val drumPatterns = createEmptyPatterns(true).toMutableList()
            val drumLanesA1 = listOf(
                DrumLane(
                    drumType = DrumType.KICK,
                    steps = listOf(true, false, false, false, true, false, false, false, true, false, false, false, true, false, false, false),
                    velocities = List(16) { 0.95f },
                    punch = 0.75f, decay = 0.55f, volume = 0.95f
                ),
                DrumLane(
                    drumType = DrumType.SNARE,
                    steps = listOf(false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false),
                    velocities = List(16) { 0.8f },
                    punch = 0.6f, decay = 0.45f, volume = 0.8f
                ),
                DrumLane(
                    drumType = DrumType.CLOSED_HAT,
                    steps = List(16) { true },
                    velocities = List(16) { i -> if (i % 2 == 0) 0.85f else 0.55f },
                    punch = 0.5f, decay = 0.3f, volume = 0.7f
                ),
                DrumLane(
                    drumType = DrumType.OPEN_HAT,
                    steps = listOf(false, false, true, false, false, false, true, false, false, false, true, false, false, false, true, false),
                    velocities = List(16) { 0.75f },
                    punch = 0.5f, decay = 0.65f, volume = 0.75f
                ),
                DrumLane(
                    drumType = DrumType.CLAP,
                    steps = listOf(false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false),
                    velocities = List(16) { 0.9f },
                    punch = 0.7f, decay = 0.5f, volume = 0.85f
                ),
                DrumLane(
                    drumType = DrumType.LOW_TOM,
                    steps = listOf(false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false),
                    velocities = List(16) { 0.8f }
                ),
                DrumLane(
                    drumType = DrumType.HIGH_TOM,
                    steps = listOf(false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true),
                    velocities = List(16) { 0.85f }
                ),
                DrumLane(
                    drumType = DrumType.CRASH,
                    steps = listOf(true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false),
                    velocities = List(16) { 0.75f },
                    decay = 0.8f, volume = 0.7f
                )
            )
            drumPatterns[0] = Pattern("A1", 16, emptyList(), drumLanesA1)

            val beatMachine = Machine(
                id = mDrumsId,
                name = "Cyber Drums 909",
                type = MachineType.BEATBOX,
                colorHex = 0xFFFF5252,
                volume = 0.9f,
                pan = 0f,
                activePatternId = "A1",
                patterns = drumPatterns
            )

            // PadSynth: Warm lush ambient layer
            val padPatterns = createEmptyPatterns(false).toMutableList()
            // Dm chord: D3(50), F3(53), A3(57)
            val padNotesA1 = listOf(
                Note(0, 50, 16, 0.7f),
                Note(0, 53, 16, 0.7f),
                Note(0, 57, 16, 0.7f)
            )
            padPatterns[0] = Pattern("A1", 16, padNotesA1, emptyList())

            val padMachine = Machine(
                id = mPadId,
                name = "Amethyst Atmosphere",
                type = MachineType.PADSYNTH,
                colorHex = 0xFF7C4DFF,
                volume = 0.75f,
                pan = 0.2f,
                params = mapOf(
                    "cutoff" to 0.65f,
                    "attack" to 0.35f,
                    "release" to 0.55f
                ),
                activePatternId = "A1",
                patterns = padPatterns
            )

            // Song Measures (8 measures arrangement)
            val songMeasures = (0 until 16).map { bar ->
                val assignments = mutableMapOf<String, String>()
                assignments[mSubId] = "A1"
                assignments[mBassId] = "A1"
                assignments[mDrumsId] = "A1"
                assignments[mPadId] = "A1"
                SongMeasure(bar, assignments)
            }

            return Project(
                id = "demo_amethyst_horizon",
                name = "Amethyst Horizon",
                bpm = 126,
                masterVolume = 0.85f,
                delayWet = 0.28f,
                delayTime = 0.375f,
                delayFeedback = 0.45f,
                reverbWet = 0.25f,
                reverbRoom = 0.55f,
                distortionDrive = 0.05f,
                machines = listOf(subMachine, bassMachine, beatMachine, padMachine),
                songMeasures = songMeasures
            )
        }

        fun createDemoProjectAcid(): Project {
            val patterns = createEmptyPatterns(false).toMutableList()
            patterns[0] = Pattern(
                id = "A1",
                steps = 16,
                notes = listOf(
                    Note(0, 36, 1, 1f), Note(2, 48, 1, 0.9f), Note(4, 39, 1, 0.85f),
                    Note(6, 42, 1, 1f), Note(8, 36, 1, 0.7f), Note(10, 48, 1, 1f),
                    Note(12, 44, 1, 0.85f), Note(14, 46, 1, 0.9f)
                ),
                drumLanes = emptyList()
            )
            val bass = Machine(
                id = "acid_303",
                name = "Devil Fish 303",
                type = MachineType.BASSLINE,
                colorHex = 0xFF00E5FF,
                volume = 0.9f,
                params = mapOf("cutoff" to 0.68f, "resonance" to 0.9f, "env_mod" to 0.75f, "decay" to 0.38f, "distortion" to 0.55f, "wave" to 1f),
                activePatternId = "A1",
                patterns = patterns
            )
            return Project(
                id = "demo_acid_neon",
                name = "Acid Neon 303",
                bpm = 138,
                distortionDrive = 0.2f,
                delayWet = 0.35f,
                machines = listOf(bass)
            )
        }

        fun createDemoProjectChiptune(): Project {
            val patterns = createEmptyPatterns(false).toMutableList()
            patterns[0] = Pattern(
                id = "A1",
                steps = 16,
                notes = listOf(
                    Note(0, 60, 1, 1f), Note(2, 64, 1, 1f), Note(4, 67, 1, 1f), Note(6, 72, 1, 1f),
                    Note(8, 71, 1, 1f), Note(10, 67, 1, 1f), Note(12, 64, 1, 1f), Note(14, 62, 1, 1f)
                ),
                drumLanes = emptyList()
            )
            val chiptune = Machine(
                id = "chip_1",
                name = "Game Boy 8-Bit",
                type = MachineType.BIT8,
                colorHex = 0xFFFFD700,
                volume = 0.85f,
                params = mapOf("pulse_width" to 0.25f),
                activePatternId = "A1",
                patterns = patterns
            )
            return Project(
                id = "demo_chiptune_rush",
                name = "Chiptune Rush",
                bpm = 142,
                machines = listOf(chiptune)
            )
        }
    }
}
