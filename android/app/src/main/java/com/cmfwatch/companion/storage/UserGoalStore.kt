package com.cmfwatch.companion.storage

import android.content.Context

data class UserGoals(
    val stepGoal: Int = 10000,
    val caloriesGoal: Int = 500,
    val distanceGoalKm: Float = 8.0f
)

class UserGoalStore(context: Context) {
    private val preferences = context.getSharedPreferences("cmf_user_goals", Context.MODE_PRIVATE)

    fun getGoals(): UserGoals {
        val steps = preferences.getInt(KEY_STEP_GOAL, 10000)
        val calories = preferences.getInt(KEY_CALORIES_GOAL, 500)
        val dist = preferences.getFloat(KEY_DISTANCE_GOAL, 8.0f)
        return UserGoals(stepGoal = steps, caloriesGoal = calories, distanceGoalKm = dist)
    }

    fun saveGoals(goals: UserGoals) {
        preferences.edit()
            .putInt(KEY_STEP_GOAL, goals.stepGoal)
            .putInt(KEY_CALORIES_GOAL, goals.caloriesGoal)
            .putFloat(KEY_DISTANCE_GOAL, goals.distanceGoalKm)
            .apply()
    }

    private companion object {
        const val KEY_STEP_GOAL = "step_goal"
        const val KEY_CALORIES_GOAL = "calories_goal"
        const val KEY_DISTANCE_GOAL = "distance_goal"
    }
}
