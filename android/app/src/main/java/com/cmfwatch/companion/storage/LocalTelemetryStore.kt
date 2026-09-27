package com.cmfwatch.companion.storage

import android.content.Context
import com.cmfwatch.companion.domain.models.HeartRateSample
import com.cmfwatch.companion.domain.models.SleepEpoch
import com.cmfwatch.companion.domain.models.SleepSession
import com.cmfwatch.companion.domain.models.SleepStage
import com.cmfwatch.companion.domain.models.StepInterval
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SavedWorkout(
    val type: String,
    val startTime: Instant,
    val durationMinutes: Int,
    val distanceKm: Float,
    val caloriesKcal: Int,
    val avgHr: Int = 0,
    val maxHr: Int = 0,
    val avgPaceSec: Int = 0
)

class LocalTelemetryStore(context: Context) {
    private val preferences = context.getSharedPreferences("cmf_telemetry_store", Context.MODE_PRIVATE)

    fun saveHeartRateSample(sample: HeartRateSample) {
        val samples = getHeartRateSamples().toMutableList()
        samples.add(sample)
        // Keep last 1000 samples
        val trimmed = if (samples.size > 1000) samples.takeLast(1000) else samples

        val array = JSONArray()
        trimmed.forEach { item ->
            val obj = JSONObject()
            obj.put("timestamp", item.timestamp.toString())
            obj.put("bpm", item.bpm)
            obj.put("opcode", item.opcode)
            array.put(obj)
        }

        preferences.edit().putString(KEY_HR_SAMPLES, array.toString()).apply()
    }

    fun getHeartRateSamples(): List<HeartRateSample> {
        val jsonStr = preferences.getString(KEY_HR_SAMPLES, null) ?: return emptyList()
        val list = mutableListOf<HeartRateSample>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val ts = Instant.parse(obj.getString("timestamp"))
                val bpm = obj.getInt("bpm")
                val opcode = obj.optString("opcode", "0x0053")
                list.add(HeartRateSample(ts, bpm, opcode))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getHeartRateSamplesToday(): List<HeartRateSample> {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        return getHeartRateSamples().filter { it.timestamp.atZone(zone).toLocalDate() == today }
    }

    fun saveStepInterval(interval: StepInterval) {
        val list = getStepIntervals().toMutableList()
        list.add(interval)
        val trimmed = if (list.size > 1000) list.takeLast(1000) else list

        val array = JSONArray()
        trimmed.forEach { item ->
            val obj = JSONObject()
            obj.put("timestamp", item.timestamp.toString())
            obj.put("steps", item.steps)
            obj.put("distanceMeters", item.distanceMeters.toDouble())
            obj.put("caloriesKcal", item.caloriesKcal.toDouble())
            array.put(obj)
        }

        preferences.edit().putString(KEY_STEP_INTERVALS, array.toString()).apply()
    }

    fun getStepIntervals(): List<StepInterval> {
        val jsonStr = preferences.getString(KEY_STEP_INTERVALS, null) ?: return emptyList()
        val list = mutableListOf<StepInterval>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val ts = Instant.parse(obj.getString("timestamp"))
                val steps = obj.getInt("steps")
                val dist = obj.getDouble("distanceMeters").toFloat()
                val kcal = obj.getDouble("caloriesKcal").toFloat()
                list.add(StepInterval(ts, steps, dist, kcal))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getStepIntervalsToday(): List<StepInterval> {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        return getStepIntervals().filter { it.timestamp.atZone(zone).toLocalDate() == today }
    }

    fun computeStandHoursToday(): Int {
        val todayIntervals = getStepIntervalsToday()
        if (todayIntervals.isEmpty()) return 0
        val zone = ZoneId.systemDefault()
        val hourlySteps = IntArray(24)
        todayIntervals.forEach { interval ->
            val hour = interval.timestamp.atZone(zone).hour
            hourlySteps[hour] += interval.steps
        }
        return hourlySteps.count { it >= 250 }
    }

    fun saveWorkout(workout: SavedWorkout) {
        val list = getSavedWorkouts().toMutableList()
        list.add(workout)
        val trimmed = if (list.size > 100) list.takeLast(100) else list

        val array = JSONArray()
        trimmed.forEach { item ->
            val obj = JSONObject()
            obj.put("type", item.type)
            obj.put("startTime", item.startTime.toString())
            obj.put("durationMinutes", item.durationMinutes)
            obj.put("distanceKm", item.distanceKm.toDouble())
            obj.put("caloriesKcal", item.caloriesKcal)
            obj.put("avgHr", item.avgHr)
            obj.put("maxHr", item.maxHr)
            obj.put("avgPaceSec", item.avgPaceSec)
            array.put(obj)
        }

        preferences.edit().putString(KEY_WORKOUTS, array.toString()).apply()
    }

    fun getSavedWorkouts(): List<SavedWorkout> {
        val jsonStr = preferences.getString(KEY_WORKOUTS, null) ?: return emptyList()
        val list = mutableListOf<SavedWorkout>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SavedWorkout(
                        type = obj.getString("type"),
                        startTime = Instant.parse(obj.getString("startTime")),
                        durationMinutes = obj.getInt("durationMinutes"),
                        distanceKm = obj.getDouble("distanceKm").toFloat(),
                        caloriesKcal = obj.getInt("caloriesKcal"),
                        avgHr = obj.optInt("avgHr", 0),
                        maxHr = obj.optInt("maxHr", 0),
                        avgPaceSec = obj.optInt("avgPaceSec", 0)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getSavedWorkoutsToday(): List<SavedWorkout> {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        return getSavedWorkouts().filter { it.startTime.atZone(zone).toLocalDate() == today }
    }

    fun saveSleepSession(session: SleepSession) {
        val sessions = getSleepSessions().toMutableList()
        sessions.add(session)
        val trimmed = if (sessions.size > 50) sessions.takeLast(50) else sessions

        val array = JSONArray()
        trimmed.forEach { item ->
            val obj = JSONObject()
            obj.put("startTime", item.startTime.toString())
            obj.put("endTime", item.endTime.toString())
            obj.put("totalSleepMinutes", item.totalSleepMinutes)
            val epochArray = JSONArray()
            item.epochs.forEach { ep ->
                val epObj = JSONObject()
                epObj.put("stage", ep.stage.name)
                epObj.put("startTime", ep.startTime.toString())
                epObj.put("durationMinutes", ep.durationMinutes)
                epochArray.put(epObj)
            }
            obj.put("epochs", epochArray)
            array.put(obj)
        }

        preferences.edit().putString(KEY_SLEEP_SESSIONS, array.toString()).apply()
    }

    fun getSleepSessions(): List<SleepSession> {
        val jsonStr = preferences.getString(KEY_SLEEP_SESSIONS, null) ?: return emptyList()
        val list = mutableListOf<SleepSession>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val startTime = Instant.parse(obj.getString("startTime"))
                val endTime = Instant.parse(obj.getString("endTime"))
                val totalMins = obj.getInt("totalSleepMinutes")
                val epochs = mutableListOf<SleepEpoch>()
                val epochArray = obj.optJSONArray("epochs")
                if (epochArray != null) {
                    for (j in 0 until epochArray.length()) {
                        val epObj = epochArray.getJSONObject(j)
                        epochs.add(
                            SleepEpoch(
                                stage = SleepStage.valueOf(epObj.getString("stage")),
                                startTime = Instant.parse(epObj.getString("startTime")),
                                durationMinutes = epObj.getInt("durationMinutes")
                            )
                        )
                    }
                }
                list.add(SleepSession(startTime, endTime, totalMins, epochs))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getLatestSleepSession(): SleepSession? {
        return getSleepSessions().lastOrNull()
    }

    private companion object {
        const val KEY_HR_SAMPLES = "hr_samples_list"
        const val KEY_STEP_INTERVALS = "step_intervals_list"
        const val KEY_WORKOUTS = "saved_workouts_list"
        const val KEY_SLEEP_SESSIONS = "sleep_sessions_list"
    }
}
