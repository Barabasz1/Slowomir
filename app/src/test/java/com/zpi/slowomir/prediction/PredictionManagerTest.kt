package com.zpi.slowomir.prediction

import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.zpi.slowomir.overlay.log.ActionDescriptor
import com.zpi.slowomir.overlay.log.ActionHasher
import com.zpi.slowomir.overlay.log.ActionType
import com.zpi.slowomir.overlay.log.HierarchyPath
import com.zpi.slowomir.overlay.log.db.ActionDefEntity
import com.zpi.slowomir.overlay.log.db.PredictionDao
import com.zpi.slowomir.overlay.log.db.PredictionObservationEntity
import com.zpi.slowomir.overlay.ui.prediction.PredictionOverlayManager
import com.zpi.slowomir.voice.stt.VoiceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class PredictionManagerTest {

    private lateinit var dao: FakePredictionDao
    private lateinit var engine: VariableOrderMarkovEngine
    private lateinit var overlayManager: PredictionOverlayManager
    private lateinit var env: FakeEnvironment
    private lateinit var manager: PredictionManager

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        dao = FakePredictionDao()
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
        val (root, targetDef) = setupScreenWithTarget("Wyświetlacz", RectSpec(0, 400, 1080, 560))
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        seedOrder0Dominant(screenFp, targetDef)

        env.root = root
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())

        env.textSessionActive = true
        manager.onScreenSettled()
        assertNull(manager.getActiveSuggestion())
        verify(overlayManager).clearHighlight()

        env.textSessionActive = false
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())

        env.macroPlaying = true
        manager.onScreenSettled()
        assertNull(manager.getActiveSuggestion())

        env.macroPlaying = false
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())
    }

    @Test
    fun `onScreenSettled rejects target occluded by keyboard`() = runBlocking<Unit> {
        val (root, targetDef) = setupScreenWithTarget("Wyświetlacz", RectSpec(0, 1400, 1080, 1800))
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        seedOrder0Dominant(screenFp, targetDef)

        env.root = root
        env.keyboardTop = 1500

        manager.onScreenSettled()
        assertNull(manager.getActiveSuggestion())
        verify(overlayManager, never()).highlight(any())
    }

    @Test
    fun `onHistoryCleared clears in-memory predictions and hides active suggestion`() = runBlocking<Unit> {
        val (root, targetDef) = setupScreenWithTarget("Bateria", RectSpec(0, 400, 1080, 560))
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        seedOrder0Dominant(screenFp, targetDef)

        env.root = root
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())

        dao.clearStats()
        manager.onHistoryCleared()

        assertNull(manager.getActiveSuggestion())
        verify(overlayManager).clearHighlight()

        manager.onScreenSettled()
        assertNull(manager.getActiveSuggestion())
    }

    @Test
    fun `onServiceDisconnected clears active suggestion and hides overlay`() = runBlocking<Unit> {
        val (root, targetDef) = setupScreenWithTarget("Aplikacje", RectSpec(0, 400, 1080, 560))
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        seedOrder0Dominant(screenFp, targetDef)

        env.root = root
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())

        manager.onServiceDisconnected()
        verify(overlayManager, timeout(1000)).clearHighlight()
        assertNull(manager.getActiveSuggestion())
    }

    @Test
    fun `view scrolled event and swipe command clear active suggestion overlay`() = runBlocking<Unit> {
        val (root, targetDef) = setupScreenWithTarget("Wyświetlacz", RectSpec(0, 400, 1080, 560))
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        seedOrder0Dominant(screenFp, targetDef)

        env.root = root
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())

        // Simulate TYPE_VIEW_SCROLLED when launcher scrolls to app drawer
        val scrollEvent = mock<AccessibilityEvent>()
        whenever(scrollEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_SCROLLED)
        whenever(scrollEvent.packageName).thenReturn("com.android.settings")
        manager.onAccessibilityEvent(scrollEvent)

        verify(overlayManager, timeout(1000)).clearHighlight()
        assertNull(manager.getActiveSuggestion())

        // Re-show on a fresh settle after clearing lastShownPair via another screen
        val (otherRoot, _) = setupScreenWithTarget("Bateria", RectSpec(0, 400, 1080, 560), screenTitle = "Bateria")
        env.root = otherRoot
        manager.onScreenSettled()
        env.root = root
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())

        // Voice swipe command clears suggestion immediately as well
        val swipeCmd = ActionDescriptor(
            type = ActionType.COMMAND,
            packageName = "com.android.settings",
            commandType = "com.zpi.slowomir.overlay.commands.navigation.SwipeUpNavCommand",
            commandArgs = "{}",
        )
        manager.onActionRecorded(swipeCmd, "swipe_up", 600L, ScreenSnapshot(screenFp))
        verify(overlayManager, timeout(1000).atLeast(2)).clearHighlight()
        assertNull(manager.getActiveSuggestion())
    }

    @Test
    fun `off screen target bounds are rejected and never highlighted`() = runBlocking<Unit> {
        val (root, targetDef) = setupScreenWithTarget("Ustawienia", RectSpec(1200, 400, 1500, 560))
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        seedOrder0Dominant(screenFp, targetDef)

        env.root = root
        manager.onScreenSettled()
        assertNull(manager.getActiveSuggestion())
        verify(overlayManager, never()).highlight(any())
    }

    @Test
    fun `grid and tool commands do not pollute context buffer`() = runBlocking<Unit> {
        val (screenA, defA) = setupScreenWithTarget(
            label = "Sieć",
            targetBounds = RectSpec(0, 400, 1080, 560),
            pkg = "com.android.settings",
            screenTitle = "Ustawienia",
            idActionDef = 11L,
        )
        val (screenB, defB) = setupScreenWithTarget(
            label = "Wi-Fi",
            targetBounds = RectSpec(0, 600, 1080, 760),
            pkg = "com.android.settings",
            screenTitle = "Sieć",
            idActionDef = 12L,
        )
        dao.putActionDef(defA)
        dao.putActionDef(defB)
        val fpA = ScreenFingerprint.compute(screenA, 1080, 2400)
        val fpB = ScreenFingerprint.compute(screenB, 1080, 2400)

        val toolCmd = ActionDescriptor(
            type = ActionType.COMMAND,
            packageName = "com.android.settings",
            commandType = "com.zpi.slowomir.overlay.commands.grid.ShowGridCommand",
            commandArgs = "{}",
        )

        // Train 3 repetitions of: click A -> noisy tool commands -> click B
        repeat(3) {
            manager.onHistoryCleared()
            env.root = screenA
            manager.onScreenSettled()
            val u1 = dao.upsertCount.get() + 2
            manager.onActionRecorded(descriptorFor(defA), defA.actionHash, defA.idActionDef, ScreenSnapshot(fpA))
            awaitUpserts(u1)

            // Noisy tool command between A and B must NOT overwrite buffer
            manager.onActionRecorded(toolCmd, "tool_show_grid", 500L, ScreenSnapshot(fpA))
            delay(30)

            env.root = screenB
            manager.onScreenSettled()
            val u2 = dao.upsertCount.get() + 2
            manager.onActionRecorded(descriptorFor(defB), defB.actionHash, defB.idActionDef, ScreenSnapshot(fpB))
            awaitUpserts(u2)
        }

        // Now reset idle state, click A, emit another noisy tool command, and settle on B
        manager.onHistoryCleared()
        env.root = screenA
        manager.onScreenSettled()
        val uLast = dao.upsertCount.get() + 2
        manager.onActionRecorded(descriptorFor(defA), defA.actionHash, defA.idActionDef, ScreenSnapshot(fpA))
        awaitUpserts(uLast)

        manager.onActionRecorded(toolCmd, "tool_select_cell", 501L, ScreenSnapshot(fpB))
        delay(30)

        env.root = screenB
        manager.onScreenSettled()

        val suggestion = manager.getActiveSuggestion()
        assertNotNull("Suggestion for B should appear despite tool commands", suggestion)
        assertEquals(defB.idActionDef, suggestion?.targetDef?.idActionDef)
    }

    @Test
    fun `cross app transitions preserve context buffer and predict next click`() = runBlocking<Unit> {
        val (launcherRoot, launcherDef) = setupScreenWithTarget(
            label = "Ustawienia",
            targetBounds = RectSpec(100, 300, 300, 500),
            pkg = "com.google.android.apps.nexuslauncher",
            screenTitle = "Pulpit",
            idActionDef = 21L,
        )
        val (settingsRoot, settingsDef) = setupScreenWithTarget(
            label = "Wyświetlacz",
            targetBounds = RectSpec(0, 400, 1080, 560),
            pkg = "com.android.settings",
            screenTitle = "Ustawienia",
            idActionDef = 22L,
        )
        dao.putActionDef(launcherDef)
        dao.putActionDef(settingsDef)
        val fpLauncher = ScreenFingerprint.compute(launcherRoot, 1080, 2400)
        val fpSettings = ScreenFingerprint.compute(settingsRoot, 1080, 2400)

        // Train 3 cross-app sequences: click Launcher -> open Settings -> click Wyświetlacz
        repeat(3) {
            manager.onHistoryCleared()
            env.root = launcherRoot
            manager.onScreenSettled()
            val u1 = dao.upsertCount.get() + 2
            manager.onActionRecorded(
                descriptorFor(launcherDef),
                launcherDef.actionHash,
                launcherDef.idActionDef,
                ScreenSnapshot(fpLauncher),
            )
            awaitUpserts(u1)

            env.root = settingsRoot
            manager.onScreenSettled()
            val u2 = dao.upsertCount.get() + 2
            manager.onActionRecorded(
                descriptorFor(settingsDef),
                settingsDef.actionHash,
                settingsDef.idActionDef,
                ScreenSnapshot(fpSettings),
            )
            awaitUpserts(u2)
        }

        // Also train a conflicting click on Settings when entering from START (order 1 only),
        // or verify order 1 key uses launcherDef hash across app boundary!
        manager.onHistoryCleared()
        env.root = launcherRoot
        manager.onScreenSettled()
        val u3 = dao.upsertCount.get() + 2
        manager.onActionRecorded(
            descriptorFor(launcherDef),
            launcherDef.actionHash,
            launcherDef.idActionDef,
            ScreenSnapshot(fpLauncher),
        )
        awaitUpserts(u3)

        env.root = settingsRoot
        manager.onScreenSettled()

        val suggestion = manager.getActiveSuggestion()
        assertNotNull("Cross-app transition should preserve buffer and predict settingsDef", suggestion)
        assertEquals(settingsDef.idActionDef, suggestion?.targetDef?.idActionDef)
        assertEquals(1, suggestion?.candidate?.order)

        val confirmedBounds = manager.onPredictionConfirmed()
        assertNotNull(confirmedBounds)
    }

    @Test
    fun `dismiss suppresses suggestion via cooldown and penalty`() = runBlocking<Unit> {
        val (root, targetDef) = setupScreenWithTarget("Wyświetlacz", RectSpec(0, 400, 1080, 560))
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        seedOrder0Dominant(screenFp, targetDef)

        env.root = root
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())

        assertTrue(manager.onPredictionDismissed())
        assertNull(manager.getActiveSuggestion())
        verify(overlayManager, atLeastOnce()).clearHighlight()

        manager.onScreenSettled()
        assertNull(manager.getActiveSuggestion())
        assertFalse(manager.onPredictionDismissed())
    }

    @Test
    fun `repeated hits cap decayedHits at MAX_DECAYED_HITS`() = runBlocking<Unit> {
        val (root, targetDef) = setupScreenWithTarget("Wyświetlacz", RectSpec(0, 400, 1080, 560))
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        dao.putActionDef(targetDef)

        repeat(25) {
            val expectedUpserts = dao.upsertCount.get() + 2
            manager.onActionRecorded(
                action = descriptorFor(targetDef),
                actionHash = targetDef.actionHash,
                actionDefId = targetDef.idActionDef,
                screenSnapshot = ScreenSnapshot(screenFp),
            )
            awaitUpserts(expectedUpserts)
        }

        val stat = dao.getStat("screen:$screenFp", targetDef.idActionDef)
        assertNotNull(stat)
        assertEquals(PredictionConfig.MAX_DECAYED_HITS, stat?.decayedHits ?: 0.0, 0.001)
    }

    @Test
    fun `clicking different target on same screen records implicit dismiss without cooldown`() = runBlocking<Unit> {
        val (root, suggestedDef) = setupScreenWithTarget(
            label = "Wyświetlacz",
            targetBounds = RectSpec(0, 400, 1080, 560),
            idActionDef = 30L,
        )
        val (_, otherDef) = setupScreenWithTarget(
            label = "Bateria",
            targetBounds = RectSpec(0, 600, 1080, 760),
            idActionDef = 31L,
        )
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        seedOrder0Dominant(screenFp, suggestedDef, hits = 5.0)
        dao.putActionDef(otherDef)

        env.root = root
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())

        val expectedUpserts = dao.upsertCount.get() + 3 // implicit dismiss + hit(order 0) + hit(order 1)
        manager.onActionRecorded(
            action = descriptorFor(otherDef),
            actionHash = otherDef.actionHash,
            actionDefId = otherDef.idActionDef,
            screenSnapshot = ScreenSnapshot(screenFp),
        )
        awaitUpserts(expectedUpserts)

        val stat = dao.getStat("screen:$screenFp", suggestedDef.idActionDef)
        assertNotNull(stat)
        assertEquals(PredictionConfig.IMPLICIT_DISMISS_WEIGHT, stat?.decayedDismisses ?: 0.0, 0.01)
        assertEquals(0, stat?.consecutiveDismissCount)
        assertEquals(0L, stat?.cooldownUntilTimestamp)
    }

    @Test
    fun `clicking different target via intermediate grid SelectCellCommand still records implicit dismiss`() = runBlocking<Unit> {
        val (root, suggestedDef) = setupScreenWithTarget(
            label = "Wyświetlacz",
            targetBounds = RectSpec(0, 400, 1080, 560),
            idActionDef = 40L,
        )
        val (_, otherDef) = setupScreenWithTarget(
            label = "Bateria",
            targetBounds = RectSpec(0, 600, 1080, 760),
            idActionDef = 41L,
        )
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        seedOrder0Dominant(screenFp, suggestedDef, hits = 5.0)
        dao.putActionDef(otherDef)

        env.root = root
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())

        // Intermediate SelectCellCommand clears activeSuggestion before TapCommand runs
        val selectCellCmd = ActionDescriptor(
            type = ActionType.COMMAND,
            packageName = "com.android.settings",
            commandType = "com.zpi.slowomir.overlay.commands.grid.SelectCellCommand",
            commandArgs = "{\"cell\":5}",
        )
        manager.onActionRecorded(selectCellCmd, "select_5", 700L, ScreenSnapshot(screenFp))
        delay(30)
        assertNull(manager.getActiveSuggestion())

        // User completes the grid tap on another element -> implicit dismiss must STILL be recorded
        val expectedUpserts = dao.upsertCount.get() + 3
        manager.onActionRecorded(
            action = descriptorFor(otherDef),
            actionHash = otherDef.actionHash,
            actionDefId = otherDef.idActionDef,
            screenSnapshot = ScreenSnapshot(screenFp),
        )
        awaitUpserts(expectedUpserts)

        val stat = dao.getStat("screen:$screenFp", suggestedDef.idActionDef)
        assertNotNull(stat)
        assertEquals(PredictionConfig.IMPLICIT_DISMISS_WEIGHT, stat?.decayedDismisses ?: 0.0, 0.01)
    }

    @Test
    fun `decayedDismisses caps at MAX_DECAYED_DISMISSES and decays on recordHit`() = runBlocking<Unit> {
        val now = System.currentTimeMillis()
        dao.upsert(
            PredictionObservationEntity(
                contextHash = "ctx",
                targetActionDefId = 10L,
                contextOrder = 0,
                packageName = "com.android.settings",
                decayedHits = 5.0,
                decayedDismisses = 0.0,
                lastUpdatedAt = now,
            )
        )

        repeat(10) {
            dao.recordDismiss(
                contextHash = "ctx",
                targetActionDefId = 10L,
                now = now,
                halfLifeMs = PredictionConfig.HALF_LIFE_MS,
                dismissWeight = PredictionConfig.DISMISS_WEIGHT,
                cooldownDurationsMs = PredictionConfig.DISMISS_COOLDOWNS_MS,
                maxDecayedDismisses = PredictionConfig.MAX_DECAYED_DISMISSES,
            )
        }
        val capped = dao.getStat("ctx", 10L)
        assertEquals(PredictionConfig.MAX_DECAYED_DISMISSES, capped?.decayedDismisses ?: 0.0, 0.001)

        dao.recordHit(
            contextHash = "ctx",
            contextOrder = 0,
            packageName = "com.android.settings",
            targetActionDefId = 10L,
            now = now,
            halfLifeMs = PredictionConfig.HALF_LIFE_MS,
            maxDecayedHits = PredictionConfig.MAX_DECAYED_HITS,
            hitDismissDecayFactor = PredictionConfig.HIT_DISMISS_DECAY_FACTOR,
        )
        val afterHit = dao.getStat("ctx", 10L)
        assertEquals(
            PredictionConfig.MAX_DECAYED_DISMISSES * PredictionConfig.HIT_DISMISS_DECAY_FACTOR,
            afterHit?.decayedDismisses ?: 0.0,
            0.001,
        )
    }

    @Test
    fun `dismissing order 1 suggestion weakens order 0 without setting order 0 cooldown`() = runBlocking<Unit> {
        val (root, targetDef) = setupScreenWithTarget("Wyświetlacz", RectSpec(0, 400, 1080, 560), idActionDef = 50L)
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        dao.putActionDef(targetDef)

        val pkg = "com.android.settings"
        val key1 = ActionHasher.sha256Hex("1|$pkg|$screenFp|START:$pkg")
        dao.upsert(
            PredictionObservationEntity(
                contextHash = key1,
                targetActionDefId = targetDef.idActionDef,
                contextOrder = 1,
                packageName = pkg,
                decayedHits = 4.0,
                decayedDismisses = 0.0,
                lastUpdatedAt = System.currentTimeMillis(),
            )
        )
        dao.upsert(
            PredictionObservationEntity(
                contextHash = "screen:$screenFp",
                targetActionDefId = targetDef.idActionDef,
                contextOrder = 0,
                packageName = pkg,
                decayedHits = 4.0,
                decayedDismisses = 0.0,
                lastUpdatedAt = System.currentTimeMillis(),
            )
        )

        env.root = root
        manager.onScreenSettled()
        assertEquals(1, manager.getActiveSuggestion()?.candidate?.order)

        assertTrue(manager.onPredictionDismissed())

        val order1Stat = dao.getStat(key1, targetDef.idActionDef)
        assertEquals(1, order1Stat?.consecutiveDismissCount)
        assertTrue((order1Stat?.cooldownUntilTimestamp ?: 0L) > 0L)

        val order0Stat = dao.getStat("screen:$screenFp", targetDef.idActionDef)
        assertEquals(0, order0Stat?.consecutiveDismissCount)
        assertEquals(0L, order0Stat?.cooldownUntilTimestamp)
        assertEquals(PredictionConfig.DISMISS_WEIGHT * 0.5, order0Stat?.decayedDismisses ?: 0.0, 0.001)
    }

    @Test
    fun `bottom bar target inside full window bounds is accepted when env height excludes system bars`() = runBlocking<Unit> {
        val (root, targetDef) = setupScreenWithTarget(
            label = "Czaty",
            targetBounds = RectSpec(32, 2214, 286, 2361),
            pkg = "com.facebook.orca",
            screenTitle = "Czaty",
            idActionDef = 60L,
        )
        env.height = 2260 // Simulate displayMetrics.heightPixels excluding navigation bar
        val screenFp = ScreenFingerprint.compute(root, env.width, env.height)
        seedOrder0Dominant(screenFp, targetDef, hits = 5.0)

        env.root = root
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())
        verify(overlayManager).highlight(any())
    }

    @Test
    fun `captureScreenSnapshot resolves root for action package via findRootForPackage when activeRoot differs`() = runBlocking<Unit> {
        val (activeRoot, _) = setupScreenWithTarget("Inny", RectSpec(0, 400, 1080, 560), pkg = "com.android.settings")
        val (dialogRoot, dialogDef) = setupScreenWithTarget("Czaty", RectSpec(0, 400, 1080, 560), pkg = "com.facebook.orca", idActionDef = 61L)

        env.root = activeRoot
        env.windowRootsByPackage = mapOf("com.facebook.orca" to dialogRoot)

        val snapshot = manager.captureScreenSnapshot(descriptorFor(dialogDef))
        assertNotNull(snapshot)
        assertEquals(ScreenFingerprint.compute(dialogRoot, env.width, env.height), snapshot?.screenFp)
    }

    @Test
    fun `after TYPE_VIEW_SCROLLED clears active suggestion re-settling same screen re-shows suggestion`() = runBlocking<Unit> {
        val (root, targetDef) = setupScreenWithTarget("Wyświetlacz", RectSpec(0, 400, 1080, 560), idActionDef = 62L)
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        seedOrder0Dominant(screenFp, targetDef, hits = 5.0)

        env.root = root
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())

        val scrollEvent = mock<AccessibilityEvent>()
        whenever(scrollEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_SCROLLED)
        whenever(scrollEvent.packageName).thenReturn("com.android.settings")
        manager.onAccessibilityEvent(scrollEvent)

        verify(overlayManager, timeout(1000)).clearHighlight()
        assertNull(manager.getActiveSuggestion())

        // Without leaving the screen, user scrolls back so the target is visible again -> re-shows
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())
    }

    @Test
    fun `switching to screen with no prediction still calls clearHighlight`() = runBlocking<Unit> {
        val (screenA, targetDef) = setupScreenWithTarget("Wyświetlacz", RectSpec(0, 400, 1080, 560), screenTitle = "Ekran A", idActionDef = 63L)
        val (screenB, _) = setupScreenWithTarget("Bateria", RectSpec(0, 400, 1080, 560), screenTitle = "Ekran B", idActionDef = 64L)
        val fpA = ScreenFingerprint.compute(screenA, 1080, 2400)
        seedOrder0Dominant(fpA, targetDef, hits = 5.0)

        env.root = screenA
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())

        env.root = screenB
        manager.onScreenSettled()
        assertNull(manager.getActiveSuggestion())
        verify(overlayManager).clearHighlight()
    }

    @Test
    fun `transient window state changed clears active suggestion and allows re-showing on same screen`() = runBlocking<Unit> {
        val (root, targetDef) = setupScreenWithTarget("Wyświetlacz", RectSpec(0, 400, 1080, 560), idActionDef = 65L)
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        seedOrder0Dominant(screenFp, targetDef, hits = 5.0)

        env.root = root
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())

        val windowStateEvent = mock<AccessibilityEvent>()
        whenever(windowStateEvent.eventType).thenReturn(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        whenever(windowStateEvent.packageName).thenReturn("com.android.systemui")
        manager.onAccessibilityEvent(windowStateEvent)

        verify(overlayManager, timeout(1000)).clearHighlight()
        assertNull(manager.getActiveSuggestion())

        // Same screen settles again after transient overlay/notification disappears -> re-shows
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())
    }

    @Test
    fun `onScreenSettled suppresses and clears suggestion when voice service is stopped`() = runBlocking<Unit> {
        val (root, targetDef) = setupScreenWithTarget("Wyświetlacz", RectSpec(0, 400, 1080, 560), idActionDef = 66L)
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        seedOrder0Dominant(screenFp, targetDef, hits = 5.0)

        env.root = root
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())

        env.voiceServiceRunning = false
        manager.onScreenSettled()
        assertNull(manager.getActiveSuggestion())
        verify(overlayManager).clearHighlight()

        env.voiceServiceRunning = true
        manager.onScreenSettled()
        assertNotNull(manager.getActiveSuggestion())
    }

    @Test
    fun `onAccessibilityEvent and onActionRecorded are ignored when voice service is stopped`() = runBlocking<Unit> {
        val (root, targetDef) = setupScreenWithTarget("Wyświetlacz", RectSpec(0, 400, 1080, 560), idActionDef = 67L)
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        seedOrder0Dominant(screenFp, targetDef, hits = 5.0)
        val initialUpserts = dao.upsertCount.get()

        env.root = root
        env.voiceServiceRunning = false

        val descriptor = descriptorFor(targetDef)
        assertNull(manager.captureScreenSnapshot(descriptor))

        val windowEvent = mock<AccessibilityEvent>()
        whenever(windowEvent.eventType).thenReturn(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        whenever(windowEvent.packageName).thenReturn("com.android.settings")
        manager.onAccessibilityEvent(windowEvent)

        manager.onActionRecorded(descriptor, targetDef.actionHash, targetDef.idActionDef, ScreenSnapshot(screenFp))
        delay(350)

        assertEquals(initialUpserts, dao.upsertCount.get())
        assertNull(manager.getActiveSuggestion())
        verify(overlayManager, never()).highlight(any())
    }

    @Test
    fun `stopping voice state flow immediately clears active suggestion and disables predictions until restarted`() = runBlocking<Unit> {
        val voiceFlow = MutableStateFlow<VoiceState>(VoiceState.KeywordSpotting)
        val reactiveEnv = object : FakeEnvironment() {
            override fun isVoiceServiceRunning(): Boolean {
                val state = voiceFlow.value
                return state !is VoiceState.Stopped && state !is VoiceState.Failed
            }
        }
        val reactiveManager = PredictionManager(
            ownPackageName = "com.zpi.slowomir",
            dao = dao,
            engine = engine,
            overlayManager = overlayManager,
            env = reactiveEnv,
            voiceStateFlow = voiceFlow,
        )

        val (root, targetDef) = setupScreenWithTarget("Wyświetlacz", RectSpec(0, 400, 1080, 560), idActionDef = 68L)
        val screenFp = ScreenFingerprint.compute(root, 1080, 2400)
        seedOrder0Dominant(screenFp, targetDef, hits = 5.0)

        reactiveEnv.root = root
        reactiveManager.onScreenSettled()
        assertNotNull(reactiveManager.getActiveSuggestion())

        voiceFlow.value = VoiceState.Stopped
        verify(overlayManager, timeout(1000)).clearHighlight()
        assertNull(reactiveManager.getActiveSuggestion())

        reactiveManager.onScreenSettled()
        assertNull(reactiveManager.getActiveSuggestion())

        voiceFlow.value = VoiceState.KeywordSpotting
        reactiveManager.onScreenSettled()
        assertNotNull(reactiveManager.getActiveSuggestion())
    }

    private suspend fun seedOrder0Dominant(screenFp: String, def: ActionDefEntity, hits: Double = 5.0) {
        dao.putActionDef(def)
        dao.upsert(
            PredictionObservationEntity(
                contextHash = "screen:$screenFp",
                targetActionDefId = def.idActionDef,
                contextOrder = 0,
                packageName = def.packageName ?: "com.android.settings",
                decayedHits = hits,
                decayedDismisses = 0.0,
                lastUpdatedAt = System.currentTimeMillis(),
            )
        )
    }

    private suspend fun awaitUpserts(targetCount: Int) {
        repeat(60) {
            if (dao.upsertCount.get() >= targetCount) return
            delay(20)
        }
    }

    private fun descriptorFor(def: ActionDefEntity) = ActionDescriptor(
        type = def.actionType,
        packageName = def.packageName,
        resourceId = def.resourceId,
        hierarchyPath = def.hierarchyPath,
        labelHash = def.labelHash,
    )

    private open class FakeEnvironment : PredictionEnvironment {
        var root: AccessibilityNodeInfo? = null
        var windowRootsByPackage: Map<String, AccessibilityNodeInfo> = emptyMap()
        var width: Int = 1080
        var height: Int = 2400
        var keyboardTop: Int? = null
        var voiceServiceRunning: Boolean = true
        var textSessionActive: Boolean = false
        var macroPlaying: Boolean = false

        override fun activeRoot(): AccessibilityNodeInfo? = root
        override fun findRootForPackage(packageName: String?): AccessibilityNodeInfo? {
            if (packageName != null) {
                windowRootsByPackage[packageName]?.let { return it }
            }
            return root
        }
        override fun screenWidth(): Int = width
        override fun screenHeight(): Int = height
        override fun keyboardTop(): Int? = keyboardTop
        override fun isVoiceServiceRunning(): Boolean = voiceServiceRunning
        override fun isTextSessionActive(): Boolean = textSessionActive
        override fun isMacroPlaying(): Boolean = macroPlaying
    }

    private class FakePredictionDao : PredictionDao() {
        private val stats = ConcurrentHashMap<Pair<String, Long>, PredictionObservationEntity>()
        private val defsById = ConcurrentHashMap<Long, ActionDefEntity>()
        val upsertCount = AtomicInteger(0)

        fun putActionDef(def: ActionDefEntity) {
            defsById[def.idActionDef] = def
        }

        fun clearStats() {
            stats.clear()
        }

        override suspend fun getUiClickStats(hash: String): List<PredictionObservationEntity> =
            stats.values.filter { row ->
                row.contextHash == hash && defsById[row.targetActionDefId]?.actionType == ActionType.UI_CLICK
            }

        override suspend fun getStat(hash: String, target: Long): PredictionObservationEntity? =
            stats[hash to target]

        override suspend fun upsert(stat: PredictionObservationEntity) {
            stats[stat.contextHash to stat.targetActionDefId] = stat
            upsertCount.incrementAndGet()
        }

        override suspend fun pruneStale(cutoffTimestamp: Long) {
            stats.entries.removeIf { it.value.lastUpdatedAt < cutoffTimestamp }
        }

        override suspend fun pruneGlobal(max: Int) {
            // No-op for small test fixtures
        }

        override suspend fun deleteForPackage(pkg: String) {
            stats.entries.removeIf { it.value.packageName == pkg }
        }

        override suspend fun getActionDef(id: Long): ActionDefEntity? = defsById[id]

        override suspend fun getActionDefByHash(hash: String): ActionDefEntity? =
            defsById.values.firstOrNull { it.actionHash == hash }
    }

    private data class RectSpec(val left: Int, val top: Int, val right: Int, val bottom: Int)

    private fun setupScreenWithTarget(
        label: String,
        targetBounds: RectSpec,
        pkg: String = "com.android.settings",
        screenTitle: String = "Ustawienia",
        idActionDef: Long = 10L,
    ): Pair<AccessibilityNodeInfo, ActionDefEntity> {
        val targetRow = mockNode(
            className = "android.widget.LinearLayout",
            packageName = pkg,
            resourceId = "$pkg:id/row",
            isClickable = true,
            bounds = targetBounds,
            children = listOf(
                mockNode(
                    className = "android.widget.TextView",
                    packageName = pkg,
                    text = label,
                    bounds = targetBounds,
                ),
            ),
        )
        val toolbar = mockNode(
            className = "androidx.appcompat.widget.Toolbar",
            packageName = pkg,
            bounds = RectSpec(0, 80, 1080, 220),
            children = listOf(
                mockNode(
                    className = "android.widget.TextView",
                    packageName = pkg,
                    text = screenTitle,
                    bounds = RectSpec(48, 100, 500, 200),
                ),
            ),
        )
        val root = mockNode(
            className = "android.widget.FrameLayout",
            packageName = pkg,
            bounds = RectSpec(0, 0, 1080, 2400),
            children = listOf(toolbar, targetRow),
        )

        val path = HierarchyPath.format(listOf("FrameLayout[0]", "LinearLayout[1]"))
        val labelHash = ActionHasher.sha256Hex(label.lowercase())
        val descriptor = ActionDescriptor(
            type = ActionType.UI_CLICK,
            packageName = pkg,
            resourceId = "$pkg:id/row",
            hierarchyPath = path,
            labelHash = labelHash,
        )
        val def = ActionDefEntity(
            idActionDef = idActionDef,
            actionHash = ActionHasher.hash(descriptor),
            actionType = ActionType.UI_CLICK,
            packageName = pkg,
            resourceId = "$pkg:id/row",
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
