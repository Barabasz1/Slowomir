package com.zpi.slowomir.modules

import com.zpi.slowomir.overlay.executors.TouchExecutor
import com.zpi.slowomir.overlay.services.CommandExecutorService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class ExecutorModule {
    @Provides
    fun provideTouchExecutor(): TouchExecutor? {
        return CommandExecutorService.instance?.touchExecutor
    }
}