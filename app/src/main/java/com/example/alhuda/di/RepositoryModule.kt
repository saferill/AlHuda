package com.example.alhuda.di

import com.example.alhuda.core.data.repository.FavoriteLocationsRepositoryImpl
import com.example.alhuda.core.data.repository.PrayerTimeRepositoryImpl
import com.example.alhuda.core.domain.repository.FavoriteLocationsRepository
import com.example.alhuda.core.domain.repository.PrayerTimeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    companion object {
        @dagger.Provides
        @Singleton
        fun providePrayerTimeRepository(
            dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
            appSettingsRepository: com.example.alhuda.core.domain.repository.AppSettingsRepository
        ): PrayerTimeRepository {
            return PrayerTimeRepositoryImpl(dataStore, appSettingsRepository)
        }
    }

    @Binds
    @Singleton
    abstract fun bindFavoriteLocationsRepository(
        impl: FavoriteLocationsRepositoryImpl
    ): FavoriteLocationsRepository

    @Binds
    @Singleton
    abstract fun bindGeoInfoRepository(
        impl: com.example.alhuda.core.data.repository.GeoInfoRepositoryImpl
    ): com.example.alhuda.core.domain.repository.GeoInfoRepository

    @Binds
    @Singleton
    abstract fun bindAppSettingsRepository(
        impl: com.example.alhuda.core.data.repository.AppSettingsRepositoryImpl
    ): com.example.alhuda.core.domain.repository.AppSettingsRepository
}
