package com.zpi.slowomir.prediction

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.zpi.slowomir.overlay.log.ActionDescriptor
import com.zpi.slowomir.overlay.log.ActionHasher
import com.zpi.slowomir.overlay.log.ActionType
import com.zpi.slowomir.overlay.log.HierarchyPath
import com.zpi.slowomir.overlay.log.db.ActionDefEntity
import com.zpi.slowomir.overlay.log.db.PredictionDao
import com.zpi.slowomir.overlay.ui.prediction.PredictionOverlayManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PredictionManagerTest {

    private lateinit var dao: PredictionDao
    private lateinit var engine: VariableOrderMarkovEngine
    private lateinit var overlayManager: PredictionOverlayManager
    private lateinit var env: FakeEnvironment
    private lateinit var manager: PredictionManager

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        dao = mock()
        engine = VariableOrderMarkovEngine(dao)
        overlayManager = mock()
        env = FakeEnvironment()
        manager = PredictionManager(
            ownPackageName = "com.zpi.slowomir",
            dao = dao,
            engine = engine,
            overlayManager = overlayManager,
            env = env,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onScreenSettled suppresses and clears suggestion when text session or macro is active`() = runBlocking<Unit> {
        val (root, targetDef) = setupSettingsScreenWithTarget("Wyświetlacz", RectSpec(0, 400, 1080, 560))
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        whenever(dao.lastUiClickTarget("screen:$screenFp")).thenReturn(targetDef.idActionDef)
        whenever(dao.getActionDef(targetDef.idActionDef)).thenReturn(targetDef)

        env.root = root
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())

        // Text session active suppresses suggestion
        env.textSessionActive = true
        manager.onScreenSettled()
        assertNull(manager.getActiveSuggestion())
        verify(overlayManager).clearHighlight()

        // Text session closed: suggestion reappears without needing to leave screen
        env.textSessionActive = false
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())

        // Macro playing suppresses suggestion
        env.macroPlaying = true
        manager.onScreenSettled()
        assertNull(manager.getActiveSuggestion())

        // Macro finished: suggestion reappears
        env.macroPlaying = false
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())
    }

    @Test
    fun `onScreenSettled rejects target occluded by keyboard`() = runBlocking<Unit> {
        val (root, targetDef) = setupSettingsScreenWithTarget("Wyświetlacz", RectSpec(0, 1400, 1080, 1800))
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        whenever(dao.lastUiClickTarget("screen:$screenFp")).thenReturn(targetDef.idActionDef)
        whenever(dao.getActionDef(targetDef.idActionDef)).thenReturn(targetDef)

        env.root = root
        env.keyboardTop = 1500

        manager.onScreenSettled()
        assertNull(manager.getActiveSuggestion())
        verify(overlayManager, never()).highlight(any())
    }

    @Test
    fun `onHistoryCleared clears in-memory predictions and hides active suggestion`() = runBlocking<Unit> {
        val (root, targetDef) = setupSettingsScreenWithTarget("Bateria", RectSpec(0, 400, 1080, 560))
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        whenever(dao.lastUiClickTarget("screen:$screenFp")).thenReturn(targetDef.idActionDef)
        whenever(dao.getActionDef(targetDef.idActionDef)).thenReturn(targetDef)

        env.root = root
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())

        // Also stub DB null after clearing history so re-settling won't reload from DB
        whenever(dao.lastUiClickTarget("screen:$screenFp")).thenReturn(null)
        manager.onHistoryCleared()

        assertNull(manager.getActiveSuggestion())
        verify(overlayManager).clearHighlight()

        manager.onScreenSettled()
        assertNull(manager.getActiveSuggestion())
    }

    @Test
    fun `onServiceDisconnected clears active suggestion and hides overlay`() = runBlocking<Unit> {
        val (root, targetDef) = setupSettingsScreenWithTarget("Aplikacje", RectSpec(0, 400, 1080, 560))
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        whenever(dao.lastUiClickTarget("screen:$screenFp")).thenReturn(targetDef.idActionDef)
        whenever(dao.getActionDef(targetDef.idActionDef)).thenReturn(targetDef)

        env.root = root
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())

        manager.onServiceDisconnected()
        verify(overlayManager, timeout(1000)).clearHighlight()
        assertNull(manager.getActiveSuggestion())
    }

    @Test
    fun `guarded action does not overwrite previously recorded safe action on same screen`() = runBlocking<Unit> {
        val (root, safeDef) = setupSettingsScreenWithTarget("Wyświetlacz", RectSpec(0, 400, 1080, 560))
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        whenever(dao.getActionDef(safeDef.idActionDef)).thenReturn(safeDef)

        val safeDescriptor = ActionDescriptor(
            type = ActionType.UI_CLICK,
            packageName = "com.android.settings",
            resourceId = safeDef.resourceId,
            hierarchyPath = safeDef.hierarchyPath,
            labelHash = safeDef.labelHash,
        )
        manager.onActionRecorded(
            action = safeDescriptor,
            actionHash = safeDef.actionHash,
            actionDefId = safeDef.idActionDef,
            screenSnapshot = ScreenSnapshot(screenFp = screenFp, isGuardedTarget = false),
        )
        verify(dao, timeout(1000)).upsertSuppression(any())

        // Now simulate recording a guarded action (e.g. password/delete button) on the same screen
        val guardedDescriptor = ActionDescriptor(
            type = ActionType.UI_CLICK,
            packageName = "com.android.settings",
            resourceId = "com.android.settings:id/delete",
            hierarchyPath = "FrameLayout[0]/Button[0]",
            labelHash = ActionHasher.sha256Hex("usuń wszystko"),
        )
        manager.onActionRecorded(
            action = guardedDescriptor,
            actionHash = "guarded_hash",
            actionDefId = 999L,
            screenSnapshot = ScreenSnapshot(screenFp = screenFp, isGuardedTarget = true),
        )
        delay(50)

        // Leave to another screen and return to original screen; safe action (id=10L) must still be suggested!
        val (otherRoot, _) = setupSettingsScreenWithTarget("Bateria", RectSpec(0, 400, 1080, 560), screenTitle = "Bateria")
        env.root = otherRoot
        manager.onScreenSettled()
        env.root = root
        manager.onScreenSettled()

        verify(dao, never()).getActionDef(999L)
        assertNotNull(manager.getActiveSuggestion())
        Unit
    }

    @Test
    fun `last UI_CLICK on screen is suggested after returning from ownPackageName and can be confirmed`() = runBlocking<Unit> {
        val (root, targetDef) = setupSettingsScreenWithTarget("Wyświetlacz", RectSpec(0, 400, 1080, 560))
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        whenever(dao.getActionDef(targetDef.idActionDef)).thenReturn(targetDef)

        env.root = root
        val descriptor = ActionDescriptor(
            type = ActionType.UI_CLICK,
            packageName = "com.android.settings",
            resourceId = targetDef.resourceId,
            hierarchyPath = targetDef.hierarchyPath,
            labelHash = targetDef.labelHash,
        )
        manager.onActionRecorded(
            action = descriptor,
            actionHash = targetDef.actionHash,
            actionDefId = targetDef.idActionDef,
            screenSnapshot = ScreenSnapshot(screenFp = screenFp, isGuardedTarget = false),
        )
        verify(dao, timeout(1000)).upsertSuppression(any())

        // User briefly opens own app (ownPackageName) or navigates away
        val ownRoot = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.zpi.slowomir",
            bounds = RectSpec(0, 0, 1080, 2400),
        )
        env.root = ownRoot
        manager.onScreenSettled()
        assertNull(manager.getActiveSuggestion())

        // User returns to Settings screen -> suggestion must appear and confirm
        env.root = root
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())
        verify(overlayManager).highlight(any())

        val confirmedBounds = manager.onPredictionConfirmed()
        assertNotNull(confirmedBounds)
        assertNull(manager.getActiveSuggestion())
    }

    private class FakeEnvironment : PredictionEnvironment {
        var root: AccessibilityNodeInfo? = null
        var width: Int = 1080
        var height: Int = 2400
        var keyboardTop: Int? = null
        var textSessionActive: Boolean = false
        var macroPlaying: Boolean = false

        override fun activeRoot(): AccessibilityNodeInfo? = root
        override fun screenWidth(): Int = width
        override fun screenHeight(): Int = height
        override fun keyboardTop(): Int? = keyboardTop
        override fun isTextSessionActive(): Boolean = textSessionActive
        override fun isMacroPlaying(): Boolean = macroPlaying
    }

    private data class RectSpec(val left: Int, val top: Int, val right: Int, val bottom: Int)

    private fun setupSettingsScreenWithTarget(
        label: String,
        targetBounds: RectSpec,
        screenTitle: String = "Ustawienia",
    ): Pair<AccessibilityNodeInfo, ActionDefEntity> {
        val targetRow = mockNode(
            className = "android.widget.LinearLayout",
            packageName = "com.android.settings",
            resourceId = "com.android.settings:id/row",
            isClickable = true,
            bounds = targetBounds,
            children = listOf(
                mockNode(
                    className = "android.widget.TextView",
                    packageName = "com.android.settings",
                    text = label,
                    bounds = targetBounds,
                ),
            ),
        )
        val toolbar = mockNode(
            className = "androidx.appcompat.widget.Toolbar",
            packageName = "com.android.settings",
            bounds = RectSpec(0, 80, 1080, 220),
            children = listOf(
                mockNode(
                    className = "android.widget.TextView",
                    packageName = "com.android.settings",
                    text = screenTitle,
                    bounds = RectSpec(48, 100, 500, 200),
                ),
            ),
        )
        val root = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.android.settings",
            bounds = RectSpec(0, 0, 1080, 2400),
            children = listOf(toolbar, targetRow),
        )

        val path = HierarchyPath.format(listOf("FrameLayout[0]", "LinearLayout[1]"))
        val labelHash = ActionHasher.sha256Hex(label.lowercase())
        val descriptor = ActionDescriptor(
            type = ActionType.UI_CLICK,
            packageName = "com.android.settings",
            resourceId = "com.android.settings:id/row",
            hierarchyPath = path,
            labelHash = labelHash,
        )
        val def = ActionDefEntity(
            idActionDef = 10L,
            actionHash = ActionHasher.hash(descriptor),
            actionType = ActionType.UI_CLICK,
            packageName = "com.android.settings",
            resourceId = "com.android.settings:id/row",
            hierarchyPath = path,
            labelHash = labelHash,
        )
        return root to def
    }

    private fun mockNode(
        className: String,
        packageName: String? = null,
        resourceId: String? = null,
        text: String? = null,
        contentDescription: String? = null,
        isClickable: Boolean = false,
        isVisibleToUser: Boolean = true,
        bounds: RectSpec = RectSpec(0, 0, 100, 100),
        children: List<AccessibilityNodeInfo> = emptyList(),
    ): AccessibilityNodeInfo {
        val node = mock<AccessibilityNodeInfo>()
        whenever(node.className).thenReturn(className)
        whenever(node.packageName).thenReturn(packageName)
        whenever(node.viewIdResourceName).thenReturn(resourceId)
        whenever(node.text).thenReturn(text)
        whenever(node.contentDescription).thenReturn(contentDescription)
        whenever(node.isClickable).thenReturn(isClickable)
        whenever(node.isVisibleToUser).thenReturn(isVisibleToUser)
        whenever(node.actionList).thenReturn(emptyList())
        whenever(node.childCount).thenReturn(children.size)
        for (i in children.indices) {
            whenever(node.getChild(i)).thenReturn(children[i])
        }
        doAnswer { invocation ->
            val rect = invocation.getArgument<Rect>(0)
            rect.left = bounds.left
            rect.top = bounds.top
            rect.right = bounds.right
            rect.bottom = bounds.bottom
            null
        }.whenever(node).getBoundsInScreen(any())
        return node
    }
}
