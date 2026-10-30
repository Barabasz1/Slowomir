package com.zpi.slowomir.modules

import com.zpi.slowomir.voice.VoiceSessionManager
import com.zpi.slowomir.voice.stt.VoiceInputSink
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceModule {

    @Binds
    @Singleton
    abstract fun bindVioceInputSink(
        impl: VoiceSessionManager
    ): VoiceInputSink
}
