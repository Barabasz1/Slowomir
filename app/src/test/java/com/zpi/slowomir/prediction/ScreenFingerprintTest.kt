package com.zpi.slowomir.prediction

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ScreenFingerprintTest {

    @Test
    fun `expanded CollapsingToolbarLayout and collapsed Toolbar produce identical fingerprint`() {
        val expandedRoot = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.android.settings",
            bounds = RectSpec(0, 0, 1080, 2400),
            children = listOf(
                mockNode(
                    className = "com.google.android.material.appbar.AppBarLayout",
                    bounds = RectSpec(0, 0, 1080, 750),
                    children = listOf(
                        mockNode(
                            className = "com.google.android.material.appbar.CollapsingToolbarLayout",
                            bounds = RectSpec(0, 0, 1080, 750),
                            children = listOf(
                                mockNode(
                                    className = "androidx.appcompat.widget.Toolbar",
                                    bounds = RectSpec(0, 80, 1080, 220),
                                    children = listOf(
                                        mockNode(
                                            className = "android.widget.ImageButton",
                                            contentDescription = "Wstecz",
                                            isClickable = true,
                                            bounds = RectSpec(0, 80, 120, 220),
                                        ),
                                    ),
                                ),
                                mockNode(
                                    className = "android.widget.TextView",
                                    text = "Wyświetlacz",
                                    bounds = RectSpec(48, 550, 500, 680), // ~23%-28% of screen height
                                ),
                            ),
                        ),
                    ),
                ),
                mockNode(
                    className = "androidx.recyclerview.widget.RecyclerView",
                    isScrollable = true,
                    bounds = RectSpec(0, 750, 1080, 2400),
                ),
            ),
        )

        val collapsedRoot = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.android.settings",
            bounds = RectSpec(0, 0, 1080, 2400),
            children = listOf(
                mockNode(
                    className = "com.google.android.material.appbar.AppBarLayout",
                    bounds = RectSpec(0, 0, 1080, 220),
                    children = listOf(
                        mockNode(
                            className = "androidx.appcompat.widget.Toolbar",
                            bounds = RectSpec(0, 80, 1080, 220),
                            children = listOf(
                                mockNode(
                                    className = "android.widget.ImageButton",
                                    contentDescription = "Wstecz",
                                    isClickable = true,
                                    bounds = RectSpec(0, 80, 120, 220),
                                ),
                                mockNode(
                                    className = "android.widget.TextView",
                                    text = "Wyświetlacz",
                                    bounds = RectSpec(140, 100, 500, 200), // ~4%-8% of screen height
                                ),
                            ),
                        ),
                    ),
                ),
                mockNode(
                    className = "androidx.recyclerview.widget.RecyclerView",
                    isScrollable = true,
                    bounds = RectSpec(0, 220, 1080, 2400),
                ),
            ),
        )

        val fpExpanded = ScreenFingerprint.compute(expandedRoot, 1080, 2400)
        val fpCollapsed = ScreenFingerprint.compute(collapsedRoot, 1080, 2400)

        assertNotEquals(ScreenFingerprint.UNKNOWN_SCREEN, fpExpanded)
        assertEquals(fpExpanded, fpCollapsed)
    }

    @Test
    fun `settings sub-screens with different titles produce distinct fingerprints`() {
        fun settingsSubScreen(title: String): AccessibilityNodeInfo = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.android.settings",
            bounds = RectSpec(0, 0, 1080, 2400),
            children = listOf(
                mockNode(
                    className = "com.google.android.material.appbar.CollapsingToolbarLayout",
                    bounds = RectSpec(0, 0, 1080, 720),
                    children = listOf(
                        mockNode(
                            className = "androidx.appcompat.widget.Toolbar",
                            bounds = RectSpec(0, 80, 1080, 220),
                            children = listOf(
                                mockNode(
                                    className = "android.widget.ImageButton",
                                    contentDescription = "Wstecz",
                                    isClickable = true,
                                    bounds = RectSpec(0, 80, 120, 220),
                                ),
                            ),
                        ),
                        mockNode(
                            className = "android.widget.TextView",
                            text = title,
                            bounds = RectSpec(48, 520, 600, 660),
                        ),
                    ),
                ),
            ),
        )

        val fpDisplay = ScreenFingerprint.compute(settingsSubScreen("Wyświetlacz"), 1080, 2400)
        val fpBattery = ScreenFingerprint.compute(settingsSubScreen("Bateria"), 1080, 2400)

        assertNotEquals(ScreenFingerprint.UNKNOWN_SCREEN, fpDisplay)
        assertNotEquals(ScreenFingerprint.UNKNOWN_SCREEN, fpBattery)
        assertNotEquals(fpDisplay, fpBattery)
    }

    @Test
    fun `same screen title in different packages produces distinct fingerprints`() {
        fun screenForPackage(pkg: String): AccessibilityNodeInfo = mockNode(
            className = "android.widget.FrameLayout",
            packageName = pkg,
            bounds = RectSpec(0, 0, 1080, 2400),
            children = listOf(
                mockNode(
                    className = "androidx.appcompat.widget.Toolbar",
                    bounds = RectSpec(0, 80, 1080, 220),
                    children = listOf(
                        mockNode(
                            className = "android.widget.TextView",
                            text = "Ustawienia",
                            bounds = RectSpec(48, 100, 400, 200),
                        ),
                    ),
                ),
            ),
        )

        val fpSettings = ScreenFingerprint.compute(screenForPackage("com.android.settings"), 1080, 2400)
        val fpChrome = ScreenFingerprint.compute(screenForPackage("com.android.chrome"), 1080, 2400)

        assertNotEquals(ScreenFingerprint.UNKNOWN_SCREEN, fpSettings)
        assertNotEquals(fpSettings, fpChrome)
    }

    @Test
    fun `long RecyclerView does not starve BottomNavigationView and distinguishes selected tab`() {
        fun screenWithListAndBottomNav(itemCount: Int, homeSelected: Boolean): AccessibilityNodeInfo {
            val listItems = (0 until itemCount).map { idx ->
                mockNode(
                    className = "android.widget.LinearLayout",
                    bounds = RectSpec(0, 200 + idx * 10, 1080, 210 + idx * 10),
                    children = listOf(
                        mockNode(
                            className = "android.widget.TextView",
                            text = "Wiersz listy",
                            bounds = RectSpec(20, 200 + idx * 10, 500, 210 + idx * 10),
                        ),
                    ),
                )
            }

            return mockNode(
                className = "android.widget.LinearLayout",
                packageName = "com.example.app",
                bounds = RectSpec(0, 0, 1080, 2400),
                children = listOf(
                    mockNode(
                        className = "androidx.recyclerview.widget.RecyclerView",
                        isScrollable = true,
                        bounds = RectSpec(0, 200, 1080, 2200),
                        children = listItems,
                    ),
                    mockNode(
                        className = "com.google.android.material.bottomnavigation.BottomNavigationView",
                        bounds = RectSpec(0, 2200, 1080, 2400),
                        children = listOf(
                            mockNode(
                                className = "android.widget.FrameLayout",
                                text = "Główna",
                                isClickable = true,
                                isSelected = homeSelected,
                                bounds = RectSpec(0, 2200, 540, 2400),
                            ),
                            mockNode(
                                className = "android.widget.FrameLayout",
                                text = "Profil",
                                isClickable = true,
                                isSelected = !homeSelected,
                                bounds = RectSpec(540, 2200, 1080, 2400),
                            ),
                        ),
                    ),
                ),
            )
        }

        val fpTab1Short = ScreenFingerprint.compute(screenWithListAndBottomNav(10, homeSelected = true), 1080, 2400)
        val fpTab1Long = ScreenFingerprint.compute(screenWithListAndBottomNav(120, homeSelected = true), 1080, 2400)
        val fpTab2Long = ScreenFingerprint.compute(screenWithListAndBottomNav(120, homeSelected = false), 1080, 2400)

        assertNotEquals(ScreenFingerprint.UNKNOWN_SCREEN, fpTab1Short)
        assertEquals(fpTab1Short, fpTab1Long)
        assertNotEquals(fpTab1Long, fpTab2Long)
    }

    @Test
    fun `empty window with only package name returns UNKNOWN_SCREEN`() {
        val emptyRoot = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.example.empty",
            bounds = RectSpec(0, 0, 1080, 2400),
        )

        val fp = ScreenFingerprint.compute(emptyRoot, 1080, 2400)
        assertEquals(ScreenFingerprint.UNKNOWN_SCREEN, fp)
    }

    @Test
    fun `fallback heading in top 40 percent works when toolbar and window title are absent`() {
        fun headingScreen(headingText: String): AccessibilityNodeInfo = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.example.composeapp",
            bounds = RectSpec(0, 0, 1080, 2400),
            children = listOf(
                mockNode(
                    className = "android.widget.TextView",
                    text = headingText,
                    isHeading = true,
                    bounds = RectSpec(48, 500, 800, 620), // below top 16%, within top 42%
                ),
            ),
        )

        val fpOne = ScreenFingerprint.compute(headingScreen("Powiadomienia"), 1080, 2400)
        val fpTwo = ScreenFingerprint.compute(headingScreen("Prywatność"), 1080, 2400)

        assertNotEquals(ScreenFingerprint.UNKNOWN_SCREEN, fpOne)
        assertNotEquals(ScreenFingerprint.UNKNOWN_SCREEN, fpTwo)
        assertNotEquals(fpOne, fpTwo)
    }

    @Test
    fun `messenger style tabs with digits and notification counts produce distinct fingerprints`() {
        fun messengerScreen(selectedIndex: Int): AccessibilityNodeInfo = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.facebook.orca",
            bounds = RectSpec(0, 0, 1080, 2400),
            children = listOf(
                mockNode(
                    className = "android.view.ViewGroup",
                    resourceId = "com.facebook.orca:id/(name removed)",
                    bounds = RectSpec(0, 2214, 1080, 2361),
                    children = listOf(
                        mockNode(
                            className = "android.widget.Button",
                            contentDescription = "Czaty, Karta 1 z 4",
                            isClickable = true,
                            isSelected = selectedIndex == 0,
                            bounds = RectSpec(32, 2214, 286, 2361),
                        ),
                        mockNode(
                            className = "android.widget.Button",
                            contentDescription = "Karta Osoby, Karta 2 z 4",
                            isClickable = true,
                            isSelected = selectedIndex == 1,
                            bounds = RectSpec(286, 2214, 540, 2361),
                        ),
                        mockNode(
                            className = "android.widget.Button",
                            contentDescription = "Karta Menu, 1 nowa aktualizacja, Karta 4 z 4",
                            isClickable = true,
                            isSelected = selectedIndex == 2,
                            bounds = RectSpec(794, 2214, 1048, 2361),
                        ),
                    ),
                ),
            ),
        )

        val fpChats = ScreenFingerprint.compute(messengerScreen(0), 1080, 2400)
        val fpPeople = ScreenFingerprint.compute(messengerScreen(1), 1080, 2400)
        val fpMenu = ScreenFingerprint.compute(messengerScreen(2), 1080, 2400)

        assertNotEquals(ScreenFingerprint.UNKNOWN_SCREEN, fpChats)
        assertNotEquals(fpChats, fpPeople)
        assertNotEquals(fpPeople, fpMenu)
    }

    @Test
    fun `obfuscated resourceId falls back to contentDescription for top actions`() {
        fun screenWithTopAction(label: String): AccessibilityNodeInfo = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.facebook.orca",
            bounds = RectSpec(0, 0, 1080, 2400),
            children = listOf(
                mockNode(
                    className = "androidx.appcompat.widget.Toolbar",
                    resourceId = "com.facebook.orca:id/(name removed)",
                    bounds = RectSpec(0, 80, 1080, 220),
                    children = listOf(
                        mockNode(
                            className = "android.widget.ImageButton",
                            resourceId = "com.facebook.orca:id/(name removed)",
                            contentDescription = label,
                            isClickable = true,
                            bounds = RectSpec(900, 80, 1050, 220),
                        ),
                    ),
                ),
            ),
        )

        val fpSearch = ScreenFingerprint.compute(screenWithTopAction("Szukaj"), 1080, 2400)
        val fpCompose = ScreenFingerprint.compute(screenWithTopAction("Nowa wiadomość"), 1080, 2400)

        assertNotEquals(ScreenFingerprint.UNKNOWN_SCREEN, fpSearch)
        assertNotEquals(fpSearch, fpCompose)
    }

    @Test
    fun `shallow root ScrollView still descends into Toolbar`() {
        fun settingsWithRootScroll(title: String): AccessibilityNodeInfo = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.android.settings",
            bounds = RectSpec(0, 0, 1080, 2400),
            children = listOf(
                mockNode(
                    className = "android.widget.ScrollView",
                    isScrollable = true,
                    bounds = RectSpec(0, 0, 1080, 2400),
                    children = listOf(
                        mockNode(
                            className = "androidx.appcompat.widget.Toolbar",
                            bounds = RectSpec(0, 80, 1080, 220),
                            children = listOf(
                                mockNode(
                                    className = "android.widget.TextView",
                                    text = title,
                                    bounds = RectSpec(48, 100, 500, 200),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val fpNetwork = ScreenFingerprint.compute(settingsWithRootScroll("Sieć i internet"), 1080, 2400)
        val fpApps = ScreenFingerprint.compute(settingsWithRootScroll("Aplikacje"), 1080, 2400)

        assertNotEquals(ScreenFingerprint.UNKNOWN_SCREEN, fpNetwork)
        assertNotEquals(fpNetwork, fpApps)
    }

    @Test
    fun `system navigation and status bar backgrounds alone return UNKNOWN_SCREEN`() {
        val root = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.example.loading",
            bounds = RectSpec(0, 0, 1080, 2400),
            children = listOf(
                mockNode(
                    className = "android.view.View",
                    resourceId = "android:id/statusBarBackground",
                    bounds = RectSpec(0, 0, 1080, 100),
                ),
                mockNode(
                    className = "android.view.View",
                    resourceId = "android:id/navigationBarBackground",
                    bounds = RectSpec(0, 2300, 1080, 2400),
                ),
            ),
        )

        val fp = ScreenFingerprint.compute(root, 1080, 2400)
        assertEquals(ScreenFingerprint.UNKNOWN_SCREEN, fp)
    }

    private data class RectSpec(val left: Int, val top: Int, val right: Int, val bottom: Int)

    private fun mockNode(
        className: String = "android.view.View",
        packageName: String? = null,
        resourceId: String? = null,
        text: String? = null,
        contentDescription: String? = null,
        paneTitle: String? = null,
        isClickable: Boolean = false,
        isScrollable: Boolean = false,
        isSelected: Boolean = false,
        isHeading: Boolean = false,
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
        whenever(node.paneTitle).thenReturn(paneTitle)
        whenever(node.isClickable).thenReturn(isClickable)
        whenever(node.isScrollable).thenReturn(isScrollable)
        whenever(node.isSelected).thenReturn(isSelected)
        whenever(node.isHeading).thenReturn(isHeading)
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
