package com.example.alhuda.core.data.repository

import android.content.Context
import com.example.alhuda.R
import com.example.alhuda.core.domain.model.geo.CityGeoInfo
import com.example.alhuda.core.domain.model.geo.CountryGeoInfo
import com.example.alhuda.core.domain.repository.GeoInfoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeoInfoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : GeoInfoRepository {

    private val countriesMutex = Mutex()
    private val citiesMutex = Mutex()

    @Volatile
    private var cachedCountries: List<CountryGeoInfo>? = null

    @Volatile
    private var cachedAllCities: List<CityGeoInfo>? = null

    private suspend fun loadCountriesOnce(): List<CountryGeoInfo> {
        cachedCountries?.let { return it }
        return countriesMutex.withLock {
            cachedCountries?.let { return@withLock it }
            val loaded = withContext(Dispatchers.IO) {
                try {
                    context.resources
                        .openRawResource(R.raw.countries_haystack)
                        .bufferedReader()
                        .useLines { lines ->
                            lines.mapNotNull { line ->
                                val pipeIndex = line.indexOf('|')
                                if (pipeIndex <= 0 || pipeIndex == line.lastIndex) return@mapNotNull null

                                val code = line.substring(0, pipeIndex)
                                val names = line.substring(pipeIndex + 1)

                                CountryGeoInfo(
                                    code = code,
                                    names = names,
                                    name = names.substringBefore(','),
                                )
                            }.toList()
                        }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            cachedCountries = loaded
            loaded
        }
    }

    private suspend fun loadAllCitiesOnce(): List<CityGeoInfo> {
        cachedAllCities?.let { return it }
        return citiesMutex.withLock {
            cachedAllCities?.let { return@withLock it }
            val loaded = withContext(Dispatchers.IO) {
                try {
                    context.resources
                        .openRawResource(R.raw.cities_haystack)
                        .bufferedReader()
                        .useLines { lines ->
                            lines.mapNotNull { line ->
                                val p1 = line.indexOf('|')
                                if (p1 <= 0) return@mapNotNull null
                                val p2 = line.indexOf('|', startIndex = p1 + 1)
                                if (p2 <= p1 + 1) return@mapNotNull null
                                val p3 = line.indexOf('|', startIndex = p2 + 1)
                                if (p3 <= p2 + 1 || p3 == line.lastIndex) return@mapNotNull null

                                val names = line.substring(0, p1)
                                val lat = line.substring(p1 + 1, p2).toDoubleOrNull() ?: return@mapNotNull null
                                val lng = line.substring(p2 + 1, p3).toDoubleOrNull() ?: return@mapNotNull null
                                val country = line.substring(p3 + 1)

                                CityGeoInfo(
                                    name = names.substringBefore(','),
                                    names = names,
                                    lat = lat,
                                    long = lng,
                                    country = country,
                                )
                            }.toList()
                        }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            cachedAllCities = loaded
            loaded
        }
    }

    override suspend fun getCountries(): List<CountryGeoInfo> = loadCountriesOnce()

    override suspend fun getCities(): List<CityGeoInfo> = loadAllCitiesOnce()

    override suspend fun getCities(countryCode: String): List<CityGeoInfo> {
        val code = countryCode.trim().uppercase(Locale.ROOT)
        if (code.isEmpty()) return emptyList()
        return loadAllCitiesOnce().filter { it.country.equals(code, ignoreCase = true) }
    }

    override suspend fun searchCities(query: String, maxResults: Int): List<CityGeoInfo> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isBlank()) return emptyList()
        val allCities = loadAllCitiesOnce()
        return withContext(Dispatchers.Default) {
            allCities
                .filter { it.names.lowercase(Locale.ROOT).contains(q) || it.name.lowercase(Locale.ROOT).contains(q) }
                .take(maxResults)
        }
    }
}
