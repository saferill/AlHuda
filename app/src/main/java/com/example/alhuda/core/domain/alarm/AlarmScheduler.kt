package com.example.alhuda.core.domain.alarm

import com.example.alhuda.core.domain.model.PrayerTime

interface AlarmScheduler {
    fun scheduleAlarm(prayerTime: PrayerTime)
    fun cancelAlarm(prayerName: String)
    fun rescheduleAllAlarms(prayerTimes: List<PrayerTime>)
}
