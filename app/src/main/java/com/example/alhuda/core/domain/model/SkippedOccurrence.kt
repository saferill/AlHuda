package com.example.alhuda.core.domain.model

import java.time.LocalDate

data class SkippedOccurrence(
    val prayerName: String,
    val date: LocalDate
) {
    fun toSerializedString(): String = "$prayerName|${date}"

    companion object {
        fun fromSerializedString(str: String): SkippedOccurrence? {
            val parts = str.split("|")
            if (parts.size != 2) return null
            val date = try {
                LocalDate.parse(parts[1])
            } catch (e: Exception) {
                return null
            }
            return SkippedOccurrence(prayerName = parts[0], date = date)
        }
    }
}
