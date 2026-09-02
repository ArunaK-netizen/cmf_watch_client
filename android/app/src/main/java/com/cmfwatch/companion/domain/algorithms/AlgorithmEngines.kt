package com.cmfwatch.companion.domain.algorithms

import com.cmfwatch.companion.domain.models.SleepEpoch
import com.cmfwatch.companion.domain.models.StepInterval

/**
 * Replaceable Algorithm Engine Interfaces.
 * Ensures our software can evaluate and replace proprietary Nothing X calculations.
 */

interface StrideLengthEngine {
    /**
     * Calculate dynamic stride length based on step cadence (steps/min) and user height.
     */
    fun calculateStrideLength(cadenceSpm: Float, userHeightCm: Float): Float

    /**
     * Compute cumulative distance over a series of step intervals.
     */
    fun calculateAdaptiveDistance(intervals: List<StepInterval>, userHeightCm: Float): Float
}

interface StressNormalizationEngine {
    /**
     * Normalize rMSSD HRV values against a rolling 14-day individual baseline.
     */
    fun normalizeStressScore(rawRmssdMs: Float, meanBaselineMs: Float, stdBaselineMs: Float): Int
}

interface SleepStageFilterEngine {
    /**
     * Apply Hidden Markov Model (HMM) Viterbi smoothing to sleep stage sequences.
     */
    fun smoothSleepStageSequence(epochs: List<SleepEpoch>): List<SleepEpoch>
}

class DefaultStrideLengthEngine : StrideLengthEngine {
    override fun calculateStrideLength(cadenceSpm: Float, userHeightCm: Float): Float {
        val heightM = userHeightCm / 100.0f
        // Dynamic cadence scaling: Stride(f) = Height * (a * f + b)
        val scalingFactor = 0.0025f * cadenceSpm + 0.165f
        return heightM * scalingFactor.coerceIn(0.35f, 0.95f)
    }

    override fun calculateAdaptiveDistance(intervals: List<StepInterval>, userHeightCm: Float): Float {
        var totalDistanceM = 0.0f
        for (interval in intervals) {
            val cadence = interval.steps.toFloat() // assuming 1-min interval
            val stride = calculateStrideLength(cadence, userHeightCm)
            totalDistanceM += interval.steps * stride
        }
        return totalDistanceM
    }
}
