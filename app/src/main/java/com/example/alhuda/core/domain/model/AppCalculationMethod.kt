package com.example.alhuda.core.domain.model

enum class AppCalculationMethod(
    val displayName: String,
    val description: String
) {
    KEMENAG_INDONESIA(
        displayName = "Kemenag Indonesia (RI)",
        description = "Standar Kementerian Agama Republik Indonesia (Fajr 20°, Isha 18°)"
    ),
    MUSLIM_WORLD_LEAGUE(
        displayName = "Muslim World League (Umum/Internasional)",
        description = "Liga Muslim Dunia, standar internasional populer (Fajr 18°, Isha 17°)"
    ),
    UMM_AL_QURA(
        displayName = "Umm Al-Qura (Arab Saudi)",
        description = "Standar Universitas Umm Al-Qura, Makkah (Fajr 18.5°, Isha +90 min)"
    ),
    EGYPTIAN(
        displayName = "Mesir",
        description = "Egyptian General Authority of Survey (Fajr 19.5°, Isha 17.5°)"
    ),
    KARACHI(
        displayName = "Karachi (Asia Selatan)",
        description = "University of Islamic Sciences, Karachi (Fajr 18°, Isha 18°)"
    ),
    SINGAPORE(
        displayName = "Singapura",
        description = "Majlis Ugama Islam Singapura (MUIS) (Fajr 20°, Isha 18°)"
    )
}
