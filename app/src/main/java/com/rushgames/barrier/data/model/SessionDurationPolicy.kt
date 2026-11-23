package com.rushgames.barrier.data.model

/**
 * Supported focus-session duration limits.
 *
 * Durations are persisted as whole minutes so existing session timestamps and
 * Room schema remain compatible.
 */
object SessionDurationPolicy {
    const val MIN_MINUTES = 5
    const val MAX_MINUTES = 7 * 24 * 60

    fun toMinutes(hours: Int, minutes: Int): Int = hours * 60 + minutes

    fun isValid(totalMinutes: Int): Boolean =
        totalMinutes in MIN_MINUTES..MAX_MINUTES
}
