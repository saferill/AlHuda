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

    @Binds
    @Singleton
    abstract fun bindPrayerTimeRepository(
        impl: PrayerTimeRepositoryImpl
    ): PrayerTimeRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteLocationsRepository(
        impl: FavoriteLocationsRepositoryImpl
    ): FavoriteLocationsRepository
}
