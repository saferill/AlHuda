package com.example.alhuda.core.domain.repository

import com.example.alhuda.core.domain.model.FavoriteLocation
import kotlinx.coroutines.flow.Flow

interface FavoriteLocationsRepository {
    suspend fun getSelectedLocation(): FavoriteLocation?
    suspend fun saveLocation(location: FavoriteLocation)
    fun getAllLocations(): Flow<List<FavoriteLocation>>
    suspend fun selectLocation(id: String)
    suspend fun deleteLocation(id: String)
}
