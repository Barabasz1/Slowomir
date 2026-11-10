package com.zpi.slowomir.modules

import com.zpi.slowomir.overlay.log.CommandLogger
import com.zpi.slowomir.overlay.log.ICommandLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LogModule {

    @Binds
    abstract fun bindLogger(impl: CommandLogger): ICommandLogger

}
