package com.zpi.slowomir.modules

import android.content.Context
import com.zpi.slowomir.kws.KeywordSpotter
import com.zpi.slowomir.kws.KeywordSpotterManager
import com.zpi.slowomir.overlay.ui.voice.VoiceOverlayManager
import com.zpi.slowomir.voice.stt.VoskSpeechRecognizer
import com.zpi.slowomir.voice.VoiceSessionManager
import com.zpi.slowomir.voice.mediator.CommandMediator
import com.zpi.slowomir.voice.stt.SpeechRecognizer
import com.zpi.slowomir.voice.stt.VoiceInputSink
import com.zpi.slowomir.voice.stt.VoiceStateSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceModule {

    @Binds
    abstract fun bindVioceInputSink(
        impl: VoiceSessionManager
    ): VoiceInputSink

    @Binds
    abstract fun bindStateSource(impl: VoiceSessionManager): VoiceStateSource

    @Binds
    abstract fun bindKeywordSpotter(impl: KeywordSpotterManager): KeywordSpotter

    @Binds
    abstract fun bindSpeechRecognizer(impl: VoskSpeechRecognizer): SpeechRecognizer


    companion object {
        @Provides
        @Singleton
        fun provideVoiceOverlayManager(
            commandMediator: CommandMediator,
            @ApplicationContext context: Context,
        ): VoiceOverlayManager {
            return VoiceOverlayManager(commandMediator, context)
        }
    }
}
