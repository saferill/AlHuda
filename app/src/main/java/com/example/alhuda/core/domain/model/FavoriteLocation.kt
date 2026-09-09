package com.example.alhuda.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteLocation(
    val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val isSelected: Boolean = false
)
