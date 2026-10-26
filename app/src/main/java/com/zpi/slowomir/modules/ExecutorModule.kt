package com.zpi.slowomir.modules

import android.content.Context
import com.zpi.slowomir.overlay.executors.GridExecutor
import com.zpi.slowomir.overlay.executors.TouchExecutor
import com.zpi.slowomir.overlay.services.CommandExecutorService
import com.zpi.slowomir.overlay.ui.VoiceOverlayManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ExecutorModule {

    @Provides
    fun provideTouchExecutor(): TouchExecutor? {
        return CommandExecutorService.instance?.touchExecutor
    }
    @Provides
    fun provideGridExecutor(): GridExecutor? {
        return CommandExecutorService.instance?.gridExecutor
    }

    @Provides
    @Singleton
    fun provideVoiceOverlayManager(
        @ApplicationContext context: Context
    ): VoiceOverlayManager {
        return VoiceOverlayManager(context)
    }
}