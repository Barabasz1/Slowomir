package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TextEditVerificationTest {
    private val service = mock<AccessibilityService>()
    private val node = mock<AccessibilityNodeInfo>()
    private val executor = TextEditExecutor(service, mock())
    private var text = "fwafbhw  jhfawhj"
    private var cursor = 1
    private var end = 1

    @Before
    fun setUp() {
        whenever(service.rootInActiveWindow).thenReturn(node)
        whenever(node.isFocused).thenReturn(true)
        whenever(node.isEnabled).thenReturn(true)
        whenever(node.isEditable).thenReturn(true)
        whenever(node.refresh()).thenReturn(true)
        whenever(node.text).thenAnswer { text }
        whenever(node.textSelectionStart).thenAnswer { cursor }
        whenever(node.textSelectionEnd).thenAnswer { end }
        val setText = mock<AccessibilityNodeInfo.AccessibilityAction>()
        val setSelection = mock<AccessibilityNodeInfo.AccessibilityAction>()
        whenever(setText.id).thenReturn(AccessibilityNodeInfo.ACTION_SET_TEXT)
        whenever(setSelection.id).thenReturn(AccessibilityNodeInfo.ACTION_SET_SELECTION)
        whenever(node.actionList).thenReturn(listOf(setText, setSelection))
    }

    @Test
    fun `cursor command fails when selection action is rejected`() {
        assertFalse(executor.cursorForward())
        verify(node).performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())
    }

    @Test
    fun `accepted selection without cursor change is not success`() {
        whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())).thenReturn(true)
        assertFalse(executor.cursorForward())
    }

    @Test
    fun `accepted text action without text change does not set selection or report success`() {
        whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())).thenReturn(true)
        assertFalse(executor.deleteWord())
        verify(node, never()).performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())
    }

    @Test
    fun `text change with failed selection is failure and deletion is never retried`() {
        acceptTextChange()
        assertFalse(executor.deleteWord())
        org.junit.Assert.assertEquals("  jhfawhj", text)
        verify(node, times(1)).performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())
    }

    @Test
    fun `unknown and out of range selections do not trigger actions`() {
        for ((start, finish) in listOf(-1 to -1, -1 to 1, 1 to 99)) {
            cursor = start
            end = finish
            assertFalse(executor.deleteWord())
        }
        verify(node, never()).performAction(any(), any<Bundle>())
    }

    @Test
    fun `stale editor after accepted text does not fall back to another field`() {
        acceptTextChange()
        whenever(node.refresh()).thenReturn(true, false)
        assertFalse(executor.deleteWord())
        verify(node, never()).performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())
        verify(node, times(1)).performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())
    }

    @Test
    fun `unfocused fallback editor is never modified`() {
        whenever(node.isFocused).thenReturn(false)
        assertFalse(executor.deleteWord())
        verify(node, never()).performAction(any(), any<Bundle>())
    }

    @Test
    fun `no op is not reported as an executed deletion`() {
        text = ""
        cursor = 0
        end = 0
        assertFalse(executor.deleteWord())
        verify(node, never()).performAction(any(), any<Bundle>())
    }

    @Test
    fun `immediate cursor then deletion verifies text before restoring cursor`() {
        cursor = 0
        end = 0
        whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())).thenAnswer {
            cursor = if (text == "  jhfawhj") 0 else 1
            end = cursor
            true
        }
        assertTrue(executor.cursorForward())
        acceptTextChange()
        assertTrue(executor.deleteWord())
        org.junit.Assert.assertEquals("  jhfawhj", text)
        org.junit.Assert.assertEquals(0, cursor)
        verify(node, times(1)).performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())
        val order = org.mockito.kotlin.inOrder(node)
        order.verify(node).performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())
        order.verify(node).performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())
        order.verify(node).performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())
    }

    @Test
    fun `already restored cursor does not require another selection action`() {
        whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())).thenAnswer {
            text = "  jhfawhj"
            cursor = 0
            end = 0
            true
        }
        assertTrue(executor.deleteWord())
        verify(node, never()).performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())
    }

    @Test
    fun `delayed text and selection updates are awaited without repeating actions`() {
        val mainLooper = mock<Looper>()
        org.mockito.Mockito.mockStatic(Looper::class.java).use { loopers ->
            loopers.`when`<Looper> { Looper.getMainLooper() }.thenReturn(mainLooper)
            var phase = 0
            var reads = 0
            whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())).thenAnswer {
                phase = 1
                reads = 0
                true
            }
            whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())).thenAnswer {
                phase = 2
                reads = 0
                true
            }
            whenever(node.refresh()).thenAnswer {
                if (phase != 0 && ++reads >= 3) {
                    if (phase == 1) {
                        text = "  jhfawhj"
                        cursor = text.length
                    } else {
                        cursor = 0
                    }
                    end = cursor
                }
                true
            }
            assertTrue(executor.deleteWord())
            org.junit.Assert.assertEquals(0, cursor)
            verify(node, times(1)).performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())
            verify(node, times(1)).performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())
        }
    }

    @Test
    fun `accepted action that never changes text times out without retry`() {
        val mainLooper = mock<Looper>()
        org.mockito.Mockito.mockStatic(Looper::class.java).use { loopers ->
            loopers.`when`<Looper> { Looper.getMainLooper() }.thenReturn(mainLooper)
            whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())).thenReturn(true)
            assertFalse(executor.deleteWord())
            verify(node, times(1)).performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())
            verify(node, never()).performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())
        }
    }

    @Test
    fun `missing disabled or initially stale editor never receives an action`() {
        whenever(service.rootInActiveWindow).thenReturn(null)
        assertFalse(executor.deleteWord())
        whenever(service.rootInActiveWindow).thenReturn(node)
        whenever(node.isEnabled).thenReturn(false)
        assertFalse(executor.deleteWord())
        whenever(node.isEnabled).thenReturn(true)
        whenever(node.refresh()).thenReturn(false)
        assertFalse(executor.deleteWord())
        verify(node, never()).performAction(any(), any<Bundle>())
    }

    @Test
    fun `unsupported text action and rejected text action do not move cursor`() {
        val actions = node.actionList
        whenever(node.actionList).thenReturn(emptyList())
        assertFalse(executor.deleteWord())
        verify(node, never()).performAction(any(), any<Bundle>())
        whenever(node.actionList).thenReturn(actions)
        assertFalse(executor.deleteWord())
        verify(node, times(1)).performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())
        verify(node, never()).performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())
    }

    @Test
    fun `selection support is checked again after text update`() {
        whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())).thenAnswer {
            text = "  jhfawhj"
            whenever(node.actionList).thenReturn(emptyList())
            true
        }
        assertFalse(executor.deleteWord())
        verify(node, never()).performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())
        verify(node, times(1)).performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())
    }

    @Test
    fun `focus loss after text change stops selection without retry`() {
        whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())).thenAnswer {
            text = "  jhfawhj"
            whenever(node.isFocused).thenReturn(false)
            true
        }
        assertFalse(executor.deleteWord())
        verify(node, never()).performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())
    }

    @Test
    fun `stale editor after selection is not a verified cursor movement`() {
        whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())).thenAnswer {
            cursor = 2
            end = 2
            whenever(node.refresh()).thenReturn(false)
            true
        }
        assertFalse(executor.cursorForward())
    }

    @Test
    fun `selection verification checks both endpoints and unchanged text`() {
        whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())).thenAnswer {
            cursor = 2
            end = 3
            true
        }
        assertFalse(executor.cursorForward())

        cursor = 1
        end = 1
        whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())).thenAnswer {
            cursor = 2
            end = 2
            text = "another message"
            true
        }
        assertFalse(executor.cursorForward())
    }

    @Test
    fun `same length text replacement is not mistaken for verified deletion`() {
        whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())).thenAnswer {
            text = "XXXXXXXXX"
            true
        }
        assertFalse(executor.deleteWord())
        verify(node, never()).performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())
    }

    @Test
    fun `main thread verification never polls a stale state`() {
        val mainLooper = mock<Looper>()
        org.mockito.Mockito.mockStatic(Looper::class.java).use { loopers ->
            loopers.`when`<Looper> { Looper.getMainLooper() }.thenReturn(mainLooper)
            loopers.`when`<Looper> { Looper.myLooper() }.thenReturn(mainLooper)
            whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())).thenReturn(true)
            assertFalse(executor.cursorForward())
            verify(node, times(2)).refresh()
        }
    }

    @Test
    fun `interrupted background verification preserves interruption and never retries`() {
        val mainLooper = mock<Looper>()
        org.mockito.Mockito.mockStatic(Looper::class.java).use { loopers ->
            loopers.`when`<Looper> { Looper.getMainLooper() }.thenReturn(mainLooper)
            whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())).thenReturn(true)
            Thread.currentThread().interrupt()
            try {
                assertFalse(executor.deleteWord())
                assertTrue(Thread.currentThread().isInterrupted)
                verify(node, times(1)).performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())
            } finally {
                Thread.interrupted()
            }
        }
    }

    @Test
    fun `simultaneous cursor commands serialize their reads and writes`() {
        cursor = 0
        end = 0
        whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())).thenAnswer {
            cursor++
            end = cursor
            true
        }
        val workers = java.util.concurrent.Executors.newFixedThreadPool(8)
        val start = java.util.concurrent.CountDownLatch(1)
        try {
            val results = (1..8).map {
                workers.submit<Boolean> {
                    check(start.await(5, java.util.concurrent.TimeUnit.SECONDS))
                    executor.cursorForward()
                }
            }
            start.countDown()
            results.forEach { assertTrue(it.get(5, java.util.concurrent.TimeUnit.SECONDS)) }
            org.junit.Assert.assertEquals(8, cursor)
            verify(node, times(8)).performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())
        } finally {
            start.countDown()
            workers.shutdownNow()
        }
    }

    @Test
    fun `all editing operations send expected text and cursor in action arguments`() {
        withWorkingBundles {
            applyActionArguments()
            val cases = listOf(
                executor::cursorForward to com.zpi.slowomir.utils.EditorState("Ala ma", 2, 2),
                executor::cursorBackward to com.zpi.slowomir.utils.EditorState("Ala ma", 0, 0),
                executor::cursorWordNext to com.zpi.slowomir.utils.EditorState("Ala ma", 4, 4),
                executor::cursorWordPrevious to com.zpi.slowomir.utils.EditorState("Ala ma", 0, 0),
                executor::deleteChar to com.zpi.slowomir.utils.EditorState("la ma", 0, 0),
                executor::deleteWord to com.zpi.slowomir.utils.EditorState(" ma", 0, 0),
            )
            for ((operation, expected) in cases) {
                text = "Ala ma"
                cursor = 1
                end = 1
                assertTrue(operation())
                org.junit.Assert.assertEquals(expected, com.zpi.slowomir.utils.EditorState(text, cursor, end))
            }
        }
    }

    @Test
    fun `exact reported regression uses action arguments for one and two cursor steps`() {
        withWorkingBundles {
            applyActionArguments()
            for (steps in 1..2) {
                text = "fwafbhw bhfawh jhfawhj"
                cursor = 10
                end = 10
                assertTrue(executor.deleteWord())
                org.junit.Assert.assertEquals("fwafbhw  jhfawhj", text)
                cursor = 0
                end = 0
                repeat(steps) { assertTrue(executor.cursorForward()) }
                org.junit.Assert.assertEquals(steps, cursor)
                assertTrue(executor.deleteWord())
                org.junit.Assert.assertEquals("  jhfawhj", text)
                org.junit.Assert.assertEquals(0, cursor)
                org.junit.Assert.assertEquals(0, end)
                assertFalse(executor.deleteWord())
                org.junit.Assert.assertEquals("  jhfawhj", text)
                repeat(2) { assertTrue(executor.cursorForward()) }
                assertTrue(executor.deleteWord())
                org.junit.Assert.assertEquals("  ", text)
            }
        }
    }

    @Test
    fun `selection deletion handles reversed and whole field ranges in action payloads`() {
        withWorkingBundles {
            applyActionArguments()
            text = "Ala ma kota"
            cursor = 6
            end = 4
            assertTrue(executor.deleteWord())
            org.junit.Assert.assertEquals("Ala  kota", text)
            org.junit.Assert.assertEquals(4, cursor)
            org.junit.Assert.assertEquals(4, end)

            cursor = text.length
            end = 0
            assertTrue(executor.deleteWord())
            org.junit.Assert.assertEquals("", text)
            org.junit.Assert.assertEquals(0, cursor)
            org.junit.Assert.assertEquals(0, end)
        }
    }

    @Test
    fun `cursor action support is required before any movement`() {
        whenever(node.actionList).thenReturn(emptyList())
        assertFalse(executor.cursorForward())
        verify(node, never()).performAction(any(), any<Bundle>())
    }

    @Test
    fun `cursor only operation awaits delayed selection without modifying text`() {
        val mainLooper = mock<Looper>()
        org.mockito.Mockito.mockStatic(Looper::class.java).use { loopers ->
            loopers.`when`<Looper> { Looper.getMainLooper() }.thenReturn(mainLooper)
            var accepted = false
            var reads = 0
            whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())).thenAnswer {
                accepted = true
                true
            }
            whenever(node.refresh()).thenAnswer {
                if (accepted && ++reads == 3) {
                    cursor = 2
                    end = 2
                }
                true
            }
            assertTrue(executor.cursorForward())
            verify(node, times(1)).performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())
            verify(node, never()).performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())
        }
    }

    @Test
    fun `cursor confirmation times out when the accepted action never takes effect`() {
        val mainLooper = mock<Looper>()
        org.mockito.Mockito.mockStatic(Looper::class.java).use { loopers ->
            loopers.`when`<Looper> { Looper.getMainLooper() }.thenReturn(mainLooper)
            whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())).thenReturn(true)
            assertFalse(executor.cursorForward())
            verify(node, org.mockito.kotlin.atLeast(3)).refresh()
            verify(node, times(1)).performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())
        }
    }

    private fun applyActionArguments() {
        whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())).thenAnswer {
            text = it.getArgument<Bundle>(1).getCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE).toString()
            cursor = text.length
            end = cursor
            true
        }
        whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_SELECTION), any<Bundle>())).thenAnswer {
            val args = it.getArgument<Bundle>(1)
            cursor = args.getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT)
            end = args.getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT)
            true
        }
    }

    // Local Android stubs do not store Bundle values; emulate only the payload API used by actions.
    private fun withWorkingBundles(block: () -> Unit) {
        org.mockito.Mockito.mockConstruction(Bundle::class.java) { bundle, _ ->
            val values = mutableMapOf<String, Any?>()
            org.mockito.kotlin.doAnswer {
                values[it.getArgument(0)] = it.getArgument<Int>(1)
                null
            }.`when`(bundle).putInt(any(), any())
            org.mockito.kotlin.doAnswer {
                values[it.getArgument(0)] = it.getArgument<CharSequence>(1)
                null
            }.`when`(bundle).putCharSequence(any(), any())
            whenever(bundle.getInt(any())).thenAnswer { values[it.getArgument(0)] as Int }
            whenever(bundle.getCharSequence(any())).thenAnswer { values[it.getArgument(0)] as CharSequence }
        }.use { block() }
    }

    private fun acceptTextChange() {
        whenever(node.performAction(eq(AccessibilityNodeInfo.ACTION_SET_TEXT), any<Bundle>())).thenAnswer {
            text = "  jhfawhj"
            cursor = text.length
            end = cursor
            true
        }
    }
}
