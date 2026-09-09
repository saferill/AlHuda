package com.example.alhuda.core.domain.alarm

import com.example.alhuda.core.domain.model.PrayerTime

interface AlarmScheduler {
    fun canScheduleExactAlarms(): Boolean
    fun scheduleAlarm(prayerTime: PrayerTime): Boolean
    fun cancelAlarm(prayerName: String)
    fun rescheduleAllAlarms(prayerTimes: List<PrayerTime>): Boolean
}
