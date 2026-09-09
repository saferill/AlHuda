package com.example.alhuda.core.domain.repository

import com.example.alhuda.core.domain.model.geo.CityGeoInfo
import com.example.alhuda.core.domain.model.geo.CountryGeoInfo

interface GeoInfoRepository {
    suspend fun getCountries(): List<CountryGeoInfo>
    suspend fun getCities(): List<CityGeoInfo>
    suspend fun getCities(countryCode: String): List<CityGeoInfo>
    suspend fun searchCities(query: String, maxResults: Int = 30): List<CityGeoInfo>
}
