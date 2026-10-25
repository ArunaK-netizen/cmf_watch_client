package com.cmfwatch.companion.domain.models

/**
 * CMF Watch supported sport & workout activities with corresponding hardware protocol IDs.
 */
enum class CmfSport(val id: Int, val displayName: String, val isGpsEnabled: Boolean = false) {
    OUTDOOR_RUN(1, "Outdoor Run", isGpsEnabled = true),
    OUTDOOR_WALK(2, "Outdoor Walk", isGpsEnabled = true),
    OUTDOOR_CYCLE(3, "Cycling", isGpsEnabled = true),
    INDOOR_RUN(4, "Indoor Run"),
    INDOOR_WALK(5, "Indoor Walk"),
    INDOOR_CYCLE(6, "Indoor Cycle"),
    POOL_SWIM(7, "Pool Swim"),
    OPEN_WATER_SWIM(8, "Open Water Swim", isGpsEnabled = true),
    ELLIPTICAL(9, "Elliptical"),
    ROWER(10, "Rowing Machine"),
    FREE_TRAINING(11, "Free Training"),
    HIIT(12, "HIIT"),
    YOGA(13, "Yoga"),
    PILATES(14, "Pilates"),
    JUMP_ROPE(15, "Jump Rope"),
    HIKING(16, "Hiking", isGpsEnabled = true),
    CRICKET(17, "Cricket"),
    FOOTBALL(18, "Football"),
    BASKETBALL(19, "Basketball"),
    BADMINTON(20, "Badminton"),
    TABLE_TENNIS(21, "Table Tennis"),
    TENNIS(22, "Tennis"),
    VOLLEYBALL(23, "Volleyball"),
    BASEBALL(24, "Baseball"),
    BOXING(25, "Boxing"),
    STRENGTH_TRAINING(26, "Strength Training"),
    FUNCTIONAL_TRAINING(27, "Functional Training"),
    CORE_TRAINING(28, "Core Training"),
    CROSS_TRAINING(29, "Cross Training"),
    DANCE(30, "Dance");

    companion object {
        fun fromId(id: Int): CmfSport = entries.firstOrNull { it.id == id } ?: FREE_TRAINING

        fun fromName(name: String): CmfSport = entries.firstOrNull {
            it.displayName.equals(name.trim(), ignoreCase = true) ||
            it.name.equals(name.trim().replace(' ', '_'), ignoreCase = true)
        } ?: OUTDOOR_RUN

        val popularSports = listOf(
            CRICKET,
            OUTDOOR_CYCLE,
            OUTDOOR_RUN,
            OUTDOOR_WALK,
            FREE_TRAINING,
            HIIT,
            BADMINTON,
            YOGA,
            FOOTBALL,
            POOL_SWIM
        )
    }
}
