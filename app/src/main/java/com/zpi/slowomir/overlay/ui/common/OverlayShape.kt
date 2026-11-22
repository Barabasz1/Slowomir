package com.zpi.slowomir.overlay.ui.common

sealed interface OverlayShape {
    data class Bar(val height: Int) : OverlayShape
    data class Card(val width: Int, val height: Int) : OverlayShape
}

val OverlayShape.height: Int
    get() = when (this) {
        is OverlayShape.Bar -> height
        is OverlayShape.Card -> height
    }

data class ScreenGeometry(
    val width: Int,
    val height: Int,
    val topInset: Int,
    val bottomInset: Int,
    val keyboardTop: Int? = null,
) {
    val usableBottom: Int get() = height - bottomInset
    val bottomEdge: Int get() = keyboardTop?.coerceIn(topInset, usableBottom) ?: usableBottom
}
