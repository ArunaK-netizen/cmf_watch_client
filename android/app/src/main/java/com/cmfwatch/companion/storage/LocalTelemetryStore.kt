package com.cmfwatch.companion.storage

import android.content.Context
import com.cmfwatch.companion.domain.models.HeartRateSample
import com.cmfwatch.companion.domain.models.StepInterval
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

data class SavedWorkout(
    val type: String,
    val startTime: Instant,
    val durationMinutes: Int,
    val distanceKm: Float,
    val caloriesKcal: Int
)

class LocalTelemetryStore(context: Context) {
    private val preferences = context.getSharedPreferences("cmf_telemetry_store", Context.MODE_PRIVATE)

    fun saveHeartRateSample(sample: HeartRateSample) {
        val samples = getHeartRateSamples().toMutableList()
        samples.add(sample)
        // Keep last 500 samples
        val trimmed = if (samples.size > 500) samples.takeLast(500) else samples

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

    fun saveStepInterval(interval: StepInterval) {
        val list = getStepIntervals().toMutableList()
        list.add(interval)
        val trimmed = if (list.size > 500) list.takeLast(500) else list

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
                        caloriesKcal = obj.getInt("caloriesKcal")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private companion object {
        const val KEY_HR_SAMPLES = "hr_samples_list"
        const val KEY_STEP_INTERVALS = "step_intervals_list"
        const val KEY_WORKOUTS = "saved_workouts_list"
    }
}
