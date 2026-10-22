package com.iridium.core.data.di

import com.iridium.core.common.AppClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/** Binds the production clock (system wall + uptime). */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class ClockModule {

    @Binds
    @Singleton
    abstract fun bindAppClock(impl: SystemAppClock): AppClock
}

/** Production [AppClock]: real system wall and uptime clocks. */
@Singleton
internal class SystemAppClock @Inject constructor() : AppClock {
    override fun wallNow(): Long = System.currentTimeMillis()
    override fun uptimeNow(): Long = android.os.SystemClock.uptimeMillis()
}
