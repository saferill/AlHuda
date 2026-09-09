package com.example.alhuda.core.domain.model

import java.time.Instant

/**
 * Model representing the next upcoming prayer and the remaining time.
 */
data class NextPrayerInfo(
    val prayerName: String,
    val prayerTime: Instant,
    val remainingSeconds: Long
)
