package com.zpi.slowomir.modules

import com.zpi.slowomir.kws.KeywordSpotter
import com.zpi.slowomir.kws.KeywordSpotterManager
import com.zpi.slowomir.voice.stt.VoskSpeechRecognizer
import com.zpi.slowomir.voice.VoiceSessionManager
import com.zpi.slowomir.voice.stt.SpeechRecognizer
import com.zpi.slowomir.voice.stt.VoiceInputSink
import com.zpi.slowomir.voice.stt.VoiceStateSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceModule {

    @Binds
    abstract fun bindVioceInputSink( impl: VoiceSessionManager ): VoiceInputSink

    @Binds
    abstract fun bindStateSource(impl: VoiceSessionManager): VoiceStateSource

    @Binds
    abstract fun bindKeywordSpotter(impl: KeywordSpotterManager): KeywordSpotter

    @Binds
    abstract fun bindSpeechRecognizer(impl: VoskSpeechRecognizer): SpeechRecognizer

}
