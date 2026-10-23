package com.zpi.slowomir.modules

import com.zpi.slowomir.overlay.services.CommandExecutorService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    fun provideCommandExecutorService(): CommandExecutorService? {
        return CommandExecutorService.instance
    }
}