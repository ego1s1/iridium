package com.iridium.core.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Drive wiring for the `foss` flavor: both bindings are no-ops, so nothing
 * outside the flavor has to know that Drive is missing.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DriveStoreModule {

    @Binds
    @Singleton
    abstract fun bindDriveGatewayFactory(impl: UnavailableDriveGatewayFactory): DriveGatewayFactory

    @Binds
    @Singleton
    abstract fun bindDriveSignIn(impl: UnavailableDriveSignIn): DriveSignIn
}
