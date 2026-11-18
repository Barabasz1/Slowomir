package com.zpi.slowomir.overlay.ui.common

import com.zpi.slowomir.overlay.services.CommandExecutorService
import javax.inject.Inject
import javax.inject.Singleton

data class FieldPosition(val top: Int, val bottom: Int)

data class FocusLayout(
    val field: FieldPosition? = null,
    val keyboardTop: Int? = null,
)

@Singleton
class FocusLayoutSource @Inject constructor() {
    fun read(): FocusLayout = CommandExecutorService.instance?.focusLayout() ?: FocusLayout()
}

enum class OverlaySide { TOP, BOTTOM }


data class OverlayPlacement(val side: OverlaySide, val edgeOffset: Int = 0)

/**
 * Decides on which side of the screen the overlay should sit so that it does not cover the focused text field.
 */
object OverlayPlacementPolicy {

    /**
     * @param layout Where the field and the keyboard are.
     * @param screenHeight Full height of the display.
     * @param topInset Space taken at the top by the status bar and display cutout.
     * @param bottomInset Space taken at the bottom by the navigation bar.
     * @param overlayHeight Height the overlay needs when it shows its card.
     * @param gap Extra space to keep between the overlay and the field.
     * @param current The side the overlay is docked to now. It is kept as long as the field is clear of it, so
     * the overlay does not jump around while the field moves slightly (scrolling, the keyboard opening).
     */
    fun choose(
        layout: FocusLayout,
        screenHeight: Int,
        topInset: Int,
        bottomInset: Int,
        overlayHeight: Int,
        gap: Int,
        current: OverlaySide,
    ): OverlayPlacement {
        val field = layout.field ?: return OverlayPlacement(OverlaySide.TOP)

        val need = overlayHeight + gap
        val usableBottom = screenHeight - bottomInset
        val bottomEdge = layout.keyboardTop?.coerceAtMost(usableBottom) ?: usableBottom

        val top = OverlayPlacement(OverlaySide.TOP)
        val bottom = OverlayPlacement(OverlaySide.BOTTOM, (usableBottom - bottomEdge).coerceAtLeast(0))

        fun clearSide(obstacle: FieldPosition): OverlayPlacement? {
            val fits = mapOf(
                OverlaySide.TOP to (obstacle.top - topInset >= need),
                OverlaySide.BOTTOM to (bottomEdge - obstacle.bottom >= need),
            )
            return when {
                fits.getValue(current) -> if (current == OverlaySide.TOP) top else bottom
                fits.getValue(OverlaySide.TOP) -> top
                fits.getValue(OverlaySide.BOTTOM) -> bottom
                else -> null
            }
        }

        clearSide(field)?.let { return it }

        val topFree = field.top - topInset
        val bottomFree = bottomEdge - field.bottom
        return if (bottomFree > topFree) bottom else top
    }
}
