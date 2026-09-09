package com.example.alhuda.main.location

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alhuda.core.domain.model.FavoriteLocation
import com.example.alhuda.core.domain.repository.FavoriteLocationsRepository
import com.example.alhuda.core.util.android.LocationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class LocationUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val locations: StateFlow<List<FavoriteLocation>> = favoriteLocationsRepository
        .getAllLocations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    fun useCurrentGpsLocation(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = LocationUtils.requestCurrentLocation(context, forceFresh = true)
            result.onSuccess { loc ->
                val gpsLoc = FavoriteLocation(
                    id = "gps_current",
                    label = "Lokasi GPS Saat Ini",
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    isSelected = true
                )
                favoriteLocationsRepository.saveLocation(gpsLoc)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Berhasil mendapatkan lokasi GPS!"
                    )
                }
                onSuccess()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Gagal mengambil lokasi GPS"
                    )
                }
            }
        }
    }

    fun addManualLocation(
        label: String,
        latitudeStr: String,
        longitudeStr: String,
        onSuccess: () -> Unit = {}
    ) {
        val trimmedLabel = label.trim()
        if (trimmedLabel.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Nama lokasi/kota tidak boleh kosong") }
            return
        }

        val lat = latitudeStr.trim().toDoubleOrNull()
        val lng = longitudeStr.trim().toDoubleOrNull()

        if (lat == null || lat < -90.0 || lat > 90.0) {
            _uiState.update { it.copy(errorMessage = "Latitude harus berupa angka antara -90 dan 90") }
            return
        }

        if (lng == null || lng < -180.0 || lng > 180.0) {
            _uiState.update { it.copy(errorMessage = "Longitude harus berupa angka antara -180 dan 180") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val newLocation = FavoriteLocation(
                id = UUID.randomUUID().toString(),
                label = trimmedLabel,
                latitude = lat,
                longitude = lng,
                isSelected = true
            )
            favoriteLocationsRepository.saveLocation(newLocation)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    successMessage = "Lokasi $trimmedLabel berhasil disimpan!"
                )
            }
            onSuccess()
        }
    }

    fun selectLocation(id: String, onSelected: () -> Unit = {}) {
        viewModelScope.launch {
            favoriteLocationsRepository.selectLocation(id)
            onSelected()
        }
    }

    fun deleteLocation(id: String) {
        viewModelScope.launch {
            favoriteLocationsRepository.deleteLocation(id)
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
