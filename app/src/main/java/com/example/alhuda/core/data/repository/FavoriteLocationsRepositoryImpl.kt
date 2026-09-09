package com.example.alhuda.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.alhuda.core.domain.model.FavoriteLocation
import com.example.alhuda.core.domain.repository.FavoriteLocationsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteLocationsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : FavoriteLocationsRepository {

    companion object {
        private val KEY_FAVORITE_LOCATIONS = stringPreferencesKey("favorite_locations_json")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun getSelectedLocation(): FavoriteLocation? {
        val locations = getAllLocations().first()
        return locations.find { it.isSelected } ?: locations.firstOrNull()
    }

    override suspend fun saveLocation(location: FavoriteLocation) {
        val currentList = getAllLocations().first().toMutableList()
        val index = currentList.indexOfFirst { it.id == location.id }

        if (location.isSelected) {
            // Unselect yang lain
            for (i in currentList.indices) {
                currentList[i] = currentList[i].copy(isSelected = false)
            }
        }

        if (index != -1) {
            currentList[index] = location
        } else {
            currentList.add(location)
        }

        persistList(currentList)
    }

    override fun getAllLocations(): Flow<List<FavoriteLocation>> {
        return dataStore.data.map { preferences ->
            val jsonString = preferences[KEY_FAVORITE_LOCATIONS]
            if (jsonString.isNullOrBlank()) {
                emptyList()
            } else {
                try {
                    json.decodeFromString<List<FavoriteLocation>>(jsonString)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    override suspend fun selectLocation(id: String) {
        val currentList = getAllLocations().first().map { loc ->
            loc.copy(isSelected = (loc.id == id))
        }
        persistList(currentList)
    }

    override suspend fun deleteLocation(id: String) {
        val currentList = getAllLocations().first().filterNot { it.id == id }
        persistList(currentList)
    }

    private suspend fun persistList(list: List<FavoriteLocation>) {
        val jsonString = json.encodeToString(list)
        dataStore.edit { preferences ->
            preferences[KEY_FAVORITE_LOCATIONS] = jsonString
        }
    }
}
