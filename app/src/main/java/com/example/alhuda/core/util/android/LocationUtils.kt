package com.example.alhuda.core.util.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withTimeoutOrNull

object LocationUtils {

    /**
     * Meminta lokasi saat ini menggunakan LocationManager bawaan Android tanpa Google Play Services.
     */
    suspend fun requestCurrentLocation(
        context: Context,
        timeoutMillis: Long = 12000,
        forceFresh: Boolean = false
    ): Result<Location> {
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCoarse && !hasFine) {
            return Result.failure(SecurityException("Permission ACCESS_COARSE_LOCATION belum diberikan"))
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return Result.failure(IllegalStateException("LocationManager tidak tersedia pada perangkat"))

        // 1. Cek Last Known Location dari GPS lalu NETWORK
        if (!forceFresh) {
            val lastGps = try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                } else null
            } catch (e: SecurityException) { null }

            val lastNetwork = try {
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                } else null
            } catch (e: SecurityException) { null }

            val candidate = lastGps ?: lastNetwork
            if (candidate != null) {
                val ageMillis = System.currentTimeMillis() - candidate.time
                val isFresh = ageMillis < 15 * 60 * 1000 // Di bawah 15 menit
                val isAccurate = candidate.accuracy in 0f..500f // Akurasi di bawah 500 meter
                if (isFresh && isAccurate) {
                    return Result.success(candidate)
                }
            }
        }

        // 2. Ambil lokasi fresh dari GPS dan NETWORK (Network di-delay 7 detik)
        return try {
            val freshLocation = withTimeoutOrNull(timeoutMillis) {
                val gpsFlow = requestLocationFlow(locationManager, LocationManager.GPS_PROVIDER)
                val networkFlow = callbackFlow<Location> {
                    delay(7000) // Delay 7 detik untuk provider network sebagai fallback
                    val subFlow = requestLocationFlow(locationManager, LocationManager.NETWORK_PROVIDER)
                    subFlow.collect { send(it) }
                    awaitClose { }
                }

                merge(gpsFlow, networkFlow).firstOrNull()
            }

            if (freshLocation != null) {
                Result.success(freshLocation)
            } else {
                // Fallback ke last known lokasi apapun yang ada jika timeout
                val fallback = try {
                    locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                } catch (e: SecurityException) { null }

                if (fallback != null) {
                    Result.success(fallback)
                } else {
                    Result.failure(Exception("Gagal mendapatkan lokasi: Waktu permintaan lokasi habis ($timeoutMillis ms)"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun requestLocationFlow(
        locationManager: LocationManager,
        provider: String
    ) = callbackFlow<Location> {
        if (!locationManager.isProviderEnabled(provider)) {
            close()
            return@callbackFlow
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(p: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }

        try {
            locationManager.requestLocationUpdates(
                provider,
                1000L,
                0f,
                listener,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose {
            try {
                locationManager.removeUpdates(listener)
            } catch (e: Exception) { }
        }
    }
}
