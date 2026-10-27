package com.zpi.slowomir.modules

import com.zpi.slowomir.voice.mediator.CommandMediator
import com.zpi.slowomir.voice.mediator.DefaultCommandMediator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediatorModule {

    @Binds
    @Singleton
    abstract fun bindCommandMediator(
        defaultCommandMediator: DefaultCommandMediator
    ): CommandMediator
}
