package com.zpi.slowomir.modules

import android.content.Context
import com.zpi.slowomir.overlay.executors.GridExecutor
import com.zpi.slowomir.overlay.executors.MacroExecutor
import com.zpi.slowomir.overlay.executors.MediaExecutor
import com.zpi.slowomir.overlay.executors.NavigationExecutor
import com.zpi.slowomir.overlay.executors.PhoneCallExecutor
import com.zpi.slowomir.overlay.executors.PredictionExecutor
import com.zpi.slowomir.overlay.executors.TextEditExecutor
import com.zpi.slowomir.overlay.executors.TouchExecutor
import com.zpi.slowomir.overlay.services.CommandExecutorService
import com.zpi.slowomir.overlay.services.PhoneCallService
import com.zpi.slowomir.overlay.ui.voice.VoiceOverlayManager
import com.zpi.slowomir.voice.mediator.CommandMediator
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
    fun provideMacroExecutor(): MacroExecutor? {
        return CommandExecutorService.instance?.macroExecutor
    }

    @Provides
    fun provideMediaExecutor(): MediaExecutor? {
        return CommandExecutorService.instance?.mediaExecutor
    }

    @Provides
    fun provideNavigationExecutor(): NavigationExecutor? {
        return CommandExecutorService.instance?.navigationExecutor
    }

    @Provides
    fun providePhoneCallExecutor(): PhoneCallExecutor? {
        return PhoneCallService.instance?.phoneCallExecutor
    }

    @Provides
    fun providePredictionExecutor(): PredictionExecutor? {
        return CommandExecutorService.instance?.predictionExecutor
    }

    @Provides
    fun provideTextEditExecutor(): TextEditExecutor? {
        return CommandExecutorService.instance?.textEditExecutor
    }


}
