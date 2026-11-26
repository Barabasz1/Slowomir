package com.zpi.slowomir.overlay.ui

import com.zpi.slowomir.overlay.ui.common.ScreenRect
import com.zpi.slowomir.overlay.ui.common.UiNode
import com.zpi.slowomir.overlay.ui.common.UiSnapshot
import com.zpi.slowomir.overlay.ui.labels.LabelTarget
import com.zpi.slowomir.overlay.ui.labels.findByText
import com.zpi.slowomir.overlay.ui.labels.toLabelTargets
import org.junit.Assert.assertEquals
import org.junit.Test

class LabelTargetsTest {

    private fun button(l: Int, t: Int, r: Int, b: Int, enabled: Boolean = true) =
        UiNode(ScreenRect(l, t, r, b), clickable = true, enabled = enabled)

    @Test
    fun `container is cut back around the interactive child inside it`() {
        val snapshot = UiSnapshot(listOf(button(0, 0, 1000, 200), button(100, 50, 300, 150)))
        val result = snapshot.toLabelTargets(minSidePx = 10)
        assertEquals(
            listOf(ScreenRect(100, 50, 300, 150), ScreenRect(300, 0, 1000, 200)),
            result.map { it.bounds }
        )
    }

    @Test
    fun `overlap is resolved vertically when that leaves a usable fragment`() {
        // Cutting the left side would keep more area, but the width must stay.
        val snapshot = UiSnapshot(listOf(button(0, 0, 100, 40), button(60, 30, 200, 70)))
        val result = snapshot.toLabelTargets(minSidePx = 10).map { it.bounds }
        assertEquals(listOf(ScreenRect(0, 0, 100, 40), ScreenRect(60, 40, 200, 70)), result)
    }

    @Test
    fun `list row with a checkbox and a star keeps the part between them`() {
        val snapshot = UiSnapshot(
            listOf(button(0, 0, 360, 60), button(5, 5, 45, 45), button(315, 5, 355, 45))
        )
        val result = snapshot.toLabelTargets(minSidePx = 10).map { it.bounds }
        assertEquals(ScreenRect(45, 0, 315, 60), result.single { it.width == 270 })
        assertEquals(3, result.size)
    }

    @Test
    fun `card behind a player bar made of several pieces is cut above the bar`() {
        val snapshot = UiSnapshot(
            listOf(
                button(0, 0, 300, 200),
                button(0, 120, 60, 180),
                button(63, 120, 230, 180),
                button(233, 120, 300, 180),
            )
        )
        val result = snapshot.toLabelTargets(minSidePx = 10).map { it.bounds }
        assertEquals(4, result.size)
        assertEquals(true, ScreenRect(0, 0, 300, 120) in result)
    }

    @Test
    fun `fragment is dropped when it cannot be cut back to a usable size`() {
        val snapshot = UiSnapshot(listOf(button(0, 0, 100, 100), button(5, 5, 95, 95)))
        assertEquals(listOf(ScreenRect(5, 5, 95, 95)), snapshot.toLabelTargets(minSidePx = 20).map { it.bounds })
    }

    @Test
    fun `labels are numbered in reading order`() {
        val snapshot = UiSnapshot(
            listOf(button(500, 300, 600, 400), button(300, 0, 400, 100), button(0, 5, 100, 105))
        )
        val result = snapshot.toLabelTargets(minSidePx = 10)
        assertEquals(listOf(1, 2, 3), result.map { it.number })
        assertEquals(listOf(0, 300, 500), result.map { it.bounds.left })
    }

    @Test
    fun `partially overlapping fragments are cut so they do not overlap`() {
        val snapshot = UiSnapshot(listOf(button(0, 0, 200, 200), button(150, 0, 300, 100)))
        val result = snapshot.toLabelTargets(minSidePx = 10).map { it.bounds }
        assertEquals(2, result.size)
        assertEquals(0L, result[0].intersectionArea(result[1]))
    }

    @Test
    fun `ignores non interactive, disabled and tiny nodes`() {
        val snapshot = UiSnapshot(
            listOf(
                UiNode(ScreenRect(0, 0, 100, 100)),
                button(0, 200, 100, 300, enabled = false),
                button(0, 400, 5, 405),
            )
        )
        assertEquals(emptyList<Any>(), snapshot.toLabelTargets(minSidePx = 10))
    }

    private fun text(l: Int, t: Int, r: Int, b: Int, text: String) =
        UiNode(ScreenRect(l, t, r, b), text = text)

    @Test
    fun `label collects the text of the elements inside it`() {
        val snapshot = UiSnapshot(
            listOf(
                button(0, 0, 400, 100),
                text(10, 10, 200, 50, "Ustawienia"),
                button(0, 200, 400, 300).copy(description = "Wstecz"),
            )
        )
        val result = snapshot.toLabelTargets(minSidePx = 10)
        assertEquals(listOf("Ustawienia"), result[0].texts)
        assertEquals(listOf("Wstecz"), result[1].texts)
    }

    @Test
    fun `text of an inner element goes to the smaller label only`() {
        val snapshot = UiSnapshot(
            listOf(button(0, 0, 400, 200), button(0, 0, 100, 100), text(10, 10, 90, 50, "OK"))
        )
        val result = snapshot.toLabelTargets(minSidePx = 10)
        assertEquals(listOf("OK"), result.single { it.bounds.right == 100 }.texts)
        assertEquals(emptyList<String>(), result.single { it.bounds.right != 100 }.texts)
    }

    @Test
    fun `findByText prefers whole text over a part of it and ignores case and punctuation`() {
        val targets = listOf(
            LabelTarget(1, ScreenRect(0, 0, 10, 10), listOf("Ustawienia zaawansowane")),
            LabelTarget(2, ScreenRect(0, 20, 10, 30), listOf("Ustawienia!")),
        )
        assertEquals(2, targets.findByText("ustawienia")?.number)
        assertEquals(1, targets.findByText("zaawansowane")?.number)
        assertEquals(null, targets.findByText("kamera"))
    }
}
