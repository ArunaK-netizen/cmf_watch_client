package com.cmfwatch.companion.domain.models

/**
 * High-level sports categories for grouping the 120+ CMF Watch sports modes.
 */
enum class SportCategory(val displayName: String) {
    RUNNING_WALKING("Running & Walking"),
    CYCLING("Cycling & Wheeling"),
    OUTDOOR_WINTER("Outdoor & Winter"),
    FITNESS_GYM("Fitness & Gym"),
    BALL_SPORTS("Ball Sports"),
    COMBAT("Combat & Martial Arts"),
    DANCE("Dance"),
    WATER_ICE("Water & Ice"),
    LEISURE("Leisure & Games")
}

/**
 * CMF Watch supported sport & workout activities with corresponding hardware protocol IDs and categories.
 * Supports all 120 sports modes available on CMF Watch Pro / Pro 2.
 */
enum class CmfSport(
    val id: Int,
    val displayName: String,
    val category: SportCategory,
    val isGpsEnabled: Boolean = false
) {
    // -------------------------------------------------------------------------
    // 1. RUNNING & WALKING
    // -------------------------------------------------------------------------
    OUTDOOR_RUN(1, "Outdoor Run", SportCategory.RUNNING_WALKING, isGpsEnabled = true),
    OUTDOOR_WALK(2, "Outdoor Walk", SportCategory.RUNNING_WALKING, isGpsEnabled = true),
    INDOOR_RUN(4, "Indoor Run", SportCategory.RUNNING_WALKING),
    INDOOR_WALK(5, "Indoor Walk", SportCategory.RUNNING_WALKING),
    CROSS_COUNTRY_RUN(31, "Cross Country Run", SportCategory.RUNNING_WALKING, isGpsEnabled = true),
    TRACK_AND_FIELD(32, "Track & Field", SportCategory.RUNNING_WALKING),
    RACE_WALKING(33, "Race Walking", SportCategory.RUNNING_WALKING, isGpsEnabled = true),

    // -------------------------------------------------------------------------
    // 2. CYCLING & WHEELING
    // -------------------------------------------------------------------------
    OUTDOOR_CYCLE(3, "Cycling", SportCategory.CYCLING, isGpsEnabled = true),
    INDOOR_CYCLE(6, "Indoor Cycle", SportCategory.CYCLING),
    DYNAMIC_CYCLE(34, "Dynamic Cycle", SportCategory.CYCLING),
    ROLLER_SKATING(35, "Roller Skating", SportCategory.CYCLING, isGpsEnabled = true),
    SKATEBOARDING(36, "Skateboarding", SportCategory.CYCLING, isGpsEnabled = true),
    HAND_CYCLING(37, "Hand Cycling", SportCategory.CYCLING),
    KARTING(38, "Karting", SportCategory.CYCLING, isGpsEnabled = true),
    RACING(39, "Racing", SportCategory.CYCLING, isGpsEnabled = true),

    // -------------------------------------------------------------------------
    // 3. OUTDOOR & WINTER SPORTS
    // -------------------------------------------------------------------------
    HIKING(16, "Hiking", SportCategory.OUTDOOR_WINTER, isGpsEnabled = true),
    MOUNTAIN_HIKE(40, "Mountain Hike", SportCategory.OUTDOOR_WINTER, isGpsEnabled = true),
    ROCK_CLIMBING(41, "Rock Climbing", SportCategory.OUTDOOR_WINTER),
    SKIING(42, "Skiing", SportCategory.OUTDOOR_WINTER, isGpsEnabled = true),
    SNOWBOARDING(43, "Snowboarding", SportCategory.OUTDOOR_WINTER, isGpsEnabled = true),
    CROSS_COUNTRY_SKIING(44, "Cross Country Skiing", SportCategory.OUTDOOR_WINTER, isGpsEnabled = true),
    SLEDDING(45, "Sledding", SportCategory.OUTDOOR_WINTER),
    SKYDIVING(46, "Skydiving", SportCategory.OUTDOOR_WINTER, isGpsEnabled = true),
    PARKOUR(47, "Parkour", SportCategory.OUTDOOR_WINTER),
    SNOW_SPORTS(48, "Snow Sports", SportCategory.OUTDOOR_WINTER),

    // -------------------------------------------------------------------------
    // 4. FITNESS, GYM & TRAINING
    // -------------------------------------------------------------------------
    FREE_TRAINING(11, "Free Training", SportCategory.FITNESS_GYM),
    STRENGTH_TRAINING(26, "Strength Training", SportCategory.FITNESS_GYM),
    ELLIPTICAL(9, "Elliptical", SportCategory.FITNESS_GYM),
    ROWER(10, "Rowing Machine", SportCategory.FITNESS_GYM),
    STAIR_STEPPER(49, "Stair Stepper", SportCategory.FITNESS_GYM),
    TREADMILL(50, "Treadmill", SportCategory.FITNESS_GYM),
    HIIT(12, "HIIT", SportCategory.FITNESS_GYM),
    YOGA(13, "Yoga", SportCategory.FITNESS_GYM),
    PILATES(14, "Pilates", SportCategory.FITNESS_GYM),
    CROSSFIT(51, "CrossFit", SportCategory.FITNESS_GYM),
    FUNCTIONAL_TRAINING(27, "Functional Training", SportCategory.FITNESS_GYM),
    CORE_TRAINING(28, "Core Training", SportCategory.FITNESS_GYM),
    CROSS_TRAINING(29, "Cross Training", SportCategory.FITNESS_GYM),
    PUSH_UP(52, "Push-up", SportCategory.FITNESS_GYM),
    SIT_UP(53, "Sit-up", SportCategory.FITNESS_GYM),
    PULL_UP(54, "Pull-up", SportCategory.FITNESS_GYM),
    BATTLE_ROPE(55, "Battle Rope", SportCategory.FITNESS_GYM),
    PLANK(56, "Plank", SportCategory.FITNESS_GYM),
    DUMBBELL(57, "Dumbbell", SportCategory.FITNESS_GYM),
    SMITH_MACHINE(58, "Smith Machine", SportCategory.FITNESS_GYM),
    HORIZONTAL_BAR(59, "Horizontal Bar", SportCategory.FITNESS_GYM),
    PARALLEL_BARS(60, "Parallel Bars", SportCategory.FITNESS_GYM),
    GYMNASTICS(61, "Gymnastics", SportCategory.FITNESS_GYM),
    TRAMPOLINE(62, "Trampoline", SportCategory.FITNESS_GYM),
    FLEXIBILITY(63, "Flexibility", SportCategory.FITNESS_GYM),
    AEROBICS(64, "Aerobics", SportCategory.FITNESS_GYM),
    COOLDOWN(65, "Cooldown", SportCategory.FITNESS_GYM),

    // -------------------------------------------------------------------------
    // 5. BALL & TEAM SPORTS
    // -------------------------------------------------------------------------
    CRICKET(17, "Cricket", SportCategory.BALL_SPORTS),
    FOOTBALL(18, "Football", SportCategory.BALL_SPORTS),
    BASKETBALL(19, "Basketball", SportCategory.BALL_SPORTS),
    BADMINTON(20, "Badminton", SportCategory.BALL_SPORTS),
    TABLE_TENNIS(21, "Table Tennis", SportCategory.BALL_SPORTS),
    TENNIS(22, "Tennis", SportCategory.BALL_SPORTS),
    VOLLEYBALL(23, "Volleyball", SportCategory.BALL_SPORTS),
    BASEBALL(24, "Baseball", SportCategory.BALL_SPORTS),
    SOFTBALL(66, "Softball", SportCategory.BALL_SPORTS),
    RUGBY(67, "Rugby", SportCategory.BALL_SPORTS),
    HOCKEY(68, "Hockey", SportCategory.BALL_SPORTS),
    SQUASH(69, "Squash", SportCategory.BALL_SPORTS),
    DODGEBALL(70, "Dodgeball", SportCategory.BALL_SPORTS),
    GOLF(71, "Golf", SportCategory.BALL_SPORTS),
    PICKLEBALL(72, "Pickleball", SportCategory.BALL_SPORTS),
    HANDBALL(73, "Handball", SportCategory.BALL_SPORTS),
    LACROSSE(74, "Lacrosse", SportCategory.BALL_SPORTS),
    BEACH_SOCCER(75, "Beach Soccer", SportCategory.BALL_SPORTS),
    BEACH_VOLLEYBALL(76, "Beach Volleyball", SportCategory.BALL_SPORTS),
    GATEBALL(77, "Gateball", SportCategory.BALL_SPORTS),
    SEPAK_TAKRAW(78, "Sepak Takraw", SportCategory.BALL_SPORTS),

    // -------------------------------------------------------------------------
    // 6. COMBAT & MARTIAL ARTS
    // -------------------------------------------------------------------------
    BOXING(25, "Boxing", SportCategory.COMBAT),
    TAEKWONDO(79, "Taekwondo", SportCategory.COMBAT),
    KARATE(80, "Karate", SportCategory.COMBAT),
    FENCING(81, "Fencing", SportCategory.COMBAT),
    KENDO(82, "Kendo", SportCategory.COMBAT),
    MARTIAL_ARTS(83, "Martial Arts", SportCategory.COMBAT),
    TAI_CHI(84, "Tai Chi", SportCategory.COMBAT),
    WRESTLING(85, "Wrestling", SportCategory.COMBAT),
    FREE_SPARRING(86, "Free Sparring", SportCategory.COMBAT),
    KABADDI(87, "Kabaddi", SportCategory.COMBAT),
    KICKBOXING_AEROBICS(88, "Kickboxing Aerobics", SportCategory.COMBAT),

    // -------------------------------------------------------------------------
    // 7. DANCE & ARTISTIC ACTIVITIES
    // -------------------------------------------------------------------------
    DANCE(30, "Dance", SportCategory.DANCE),
    ZUMBA(89, "Zumba", SportCategory.DANCE),
    BELLY_DANCE(90, "Belly Dance", SportCategory.DANCE),
    JAZZ_DANCE(91, "Jazz Dance", SportCategory.DANCE),
    LATIN_DANCE(92, "Latin Dance", SportCategory.DANCE),
    BALLET(93, "Ballet", SportCategory.DANCE),
    STREET_DANCE(94, "Street Dance", SportCategory.DANCE),

    // -------------------------------------------------------------------------
    // 8. WATER & ICE SPORTS
    // -------------------------------------------------------------------------
    POOL_SWIM(7, "Pool Swim", SportCategory.WATER_ICE),
    OPEN_WATER_SWIM(8, "Open Water Swim", SportCategory.WATER_ICE, isGpsEnabled = true),
    SAILING(95, "Sailing", SportCategory.WATER_ICE, isGpsEnabled = true),
    SURFING(96, "Surfing", SportCategory.WATER_ICE, isGpsEnabled = true),
    JET_SKI(97, "Jet Skiing", SportCategory.WATER_ICE, isGpsEnabled = true),
    ICE_SKATING(98, "Ice Skating", SportCategory.WATER_ICE),
    ICE_HOCKEY(99, "Ice Hockey", SportCategory.WATER_ICE),
    CURLING(100, "Curling", SportCategory.WATER_ICE),

    // -------------------------------------------------------------------------
    // 9. LEISURE, GAMES & OTHER ACTIVITIES
    // -------------------------------------------------------------------------
    JUMP_ROPE(15, "Jump Rope", SportCategory.LEISURE),
    HULA_HOOP(101, "Hula Hoop", SportCategory.LEISURE),
    DARTS(102, "Darts", SportCategory.LEISURE),
    ARCHERY(103, "Archery", SportCategory.LEISURE),
    HORSE_RIDING(104, "Horse Riding", SportCategory.LEISURE, isGpsEnabled = true),
    KITE_FLYING(105, "Kite Flying", SportCategory.LEISURE),
    FISHING(106, "Fishing", SportCategory.LEISURE),
    BILLIARDS(107, "Billiards", SportCategory.LEISURE),
    BOWLING(108, "Bowling", SportCategory.LEISURE),
    FITNESS_GAMING(109, "Fitness Gaming", SportCategory.LEISURE),
    SHUTTLECOCK(110, "Shuttlecock", SportCategory.LEISURE);

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
            POOL_SWIM,
            BASKETBALL,
            TENNIS,
            STRENGTH_TRAINING,
            HIKING,
            BOXING
        )
    }
}
