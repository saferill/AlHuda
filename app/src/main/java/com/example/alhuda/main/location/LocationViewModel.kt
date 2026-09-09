package com.example.alhuda.main.location

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alhuda.core.domain.model.FavoriteLocation
import com.example.alhuda.core.domain.model.geo.CityGeoInfo
import com.example.alhuda.core.domain.model.geo.CountryGeoInfo
import com.example.alhuda.core.domain.repository.FavoriteLocationsRepository
import com.example.alhuda.core.domain.repository.GeoInfoRepository
import com.example.alhuda.core.util.android.LocationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class LocationUiState(
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<CityGeoInfo> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isNewLocationDialogOpen: Boolean = false,
)

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val geoInfoRepository: GeoInfoRepository,
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

    suspend fun getCountries(): List<CountryGeoInfo> = geoInfoRepository.getCountries()

    suspend fun getCities(countryCode: String): List<CityGeoInfo> = geoInfoRepository.getCities(countryCode)

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val results = geoInfoRepository.searchCities(query.trim(), maxResults = 25)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun openNewLocationDialog() {
        _uiState.update { it.copy(isNewLocationDialogOpen = true) }
    }

    fun closeNewLocationDialog() {
        _uiState.update { it.copy(isNewLocationDialogOpen = false) }
    }

    fun useCurrentGpsLocation(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = LocationUtils.requestCurrentLocation(context, forceFresh = true)
            result.onSuccess { loc ->
                var detectedName = "Lokasi Saat Ini (GPS)"
                try {
                    val geocoder = Geocoder(context, Locale("id", "ID"))
                    val addresses = withContext(Dispatchers.IO) {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                    }
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                        if (!locality.isNullOrBlank()) {
                            detectedName = locality
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to default
                }

                val gpsLoc = FavoriteLocation(
                    id = "gps_current",
                    label = detectedName,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    isSelected = true
                )
                favoriteLocationsRepository.saveLocation(gpsLoc)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Berhasil mendapatkan lokasi GPS: $detectedName"
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

    fun selectCityGeo(city: CityGeoInfo, customLabel: String? = null, onSuccess: () -> Unit = {}) {
        val label = if (!customLabel.isNullOrBlank()) customLabel.trim() else city.name
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isNewLocationDialogOpen = false, searchQuery = "", searchResults = emptyList()) }
            val newLocation = FavoriteLocation(
                id = UUID.randomUUID().toString(),
                label = label,
                latitude = city.lat,
                longitude = city.long,
                isSelected = true
            )
            favoriteLocationsRepository.saveLocation(newLocation)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    successMessage = "Lokasi $label berhasil disimpan & dijadikan aktif!"
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
