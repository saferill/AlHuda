package com.example.alhuda.di

import com.example.alhuda.core.domain.alarm.AlarmScheduler
import com.example.alhuda.main.alarm.AlarmSchedulerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AlarmModule {

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(
        impl: AlarmSchedulerImpl
    ): AlarmScheduler
}
