package com.iridium.core.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Drive wiring for the `play` flavor: the real transport and the Play
 * services account picker. Flavors get one module each so the publishing
 * channel picks the implementations through variant-aware source sets.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DriveStoreModule {

    @Binds
    @Singleton
    abstract fun bindDriveGatewayFactory(impl: GoogleDriveGatewayFactory): DriveGatewayFactory

    @Binds
    @Singleton
    abstract fun bindDriveSignIn(impl: GoogleDriveSignIn): DriveSignIn
}
