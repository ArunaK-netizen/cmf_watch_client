package com.cmfwatch.companion.storage

import android.content.Context

data class UserGoals(
    val stepGoal: Int = 10000,
    val caloriesGoal: Int = 650,
    val distanceGoalKm: Float = 8.0f,
    val exerciseMinutesGoal: Int = 45,
    val standHoursGoal: Int = 12,
    val weeklyDistanceGoalKm: Float = 30.0f
)

class UserGoalStore(context: Context) {
    private val preferences = context.getSharedPreferences("cmf_user_goals", Context.MODE_PRIVATE)

    fun getGoals(): UserGoals {
        return UserGoals(
            stepGoal = preferences.getInt(KEY_STEP_GOAL, 10000),
            caloriesGoal = preferences.getInt(KEY_CALORIES_GOAL, 650),
            distanceGoalKm = preferences.getFloat(KEY_DISTANCE_GOAL, 8.0f),
            exerciseMinutesGoal = preferences.getInt(KEY_EXERCISE_GOAL, 45),
            standHoursGoal = preferences.getInt(KEY_STAND_GOAL, 12),
            weeklyDistanceGoalKm = preferences.getFloat(KEY_WEEKLY_DISTANCE, 30.0f)
        )
    }

    fun saveGoals(goals: UserGoals) {
        preferences.edit()
            .putInt(KEY_STEP_GOAL, goals.stepGoal)
            .putInt(KEY_CALORIES_GOAL, goals.caloriesGoal)
            .putFloat(KEY_DISTANCE_GOAL, goals.distanceGoalKm)
            .putInt(KEY_EXERCISE_GOAL, goals.exerciseMinutesGoal)
            .putInt(KEY_STAND_GOAL, goals.standHoursGoal)
            .putFloat(KEY_WEEKLY_DISTANCE, goals.weeklyDistanceGoalKm)
            .apply()
    }

    private companion object {
        const val KEY_STEP_GOAL = "step_goal"
        const val KEY_CALORIES_GOAL = "calories_goal"
        const val KEY_DISTANCE_GOAL = "distance_goal"
        const val KEY_EXERCISE_GOAL = "exercise_minutes_goal"
        const val KEY_STAND_GOAL = "stand_hours_goal"
        const val KEY_WEEKLY_DISTANCE = "weekly_distance_goal"
    }
}
