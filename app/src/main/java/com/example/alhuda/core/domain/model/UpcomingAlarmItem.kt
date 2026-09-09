package com.example.alhuda.core.domain.model

import java.time.LocalDateTime

data class UpcomingAlarmItem(
    val prayerName: String,
    val dateTime: LocalDateTime,
    val isEnabled: Boolean,
    val isSkipped: Boolean
)
