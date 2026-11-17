package com.zpi.slowomir.ml

class TokenEncoding(val ids: LongArray, val offsets: List<IntRange?>)


interface TextTokenizer {
    fun load() {}

    fun encode(text: String, maxLength: Int): TokenEncoding

    fun close() {}
}
