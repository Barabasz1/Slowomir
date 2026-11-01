package com.zpi.slowomir.kws

class MicroFrontend : AutoCloseable {
    init { System.loadLibrary("microfrontend") }

    fun init(): Boolean = nativeInit()

    fun process(pcm: ShortArray): FloatArray = nativeProcess(pcm)

    override fun close() = nativeRelease()

    private external fun nativeInit(): Boolean
    private external fun nativeProcess(pcm: ShortArray): FloatArray
    private external fun nativeRelease()
}