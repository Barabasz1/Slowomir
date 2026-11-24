package com.zpi.slowomir.prediction

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.zpi.slowomir.overlay.log.ActionHasher
import com.zpi.slowomir.overlay.log.ActionType
import com.zpi.slowomir.overlay.log.UiNodeDescriber
import com.zpi.slowomir.overlay.log.db.ActionDefEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UiNodeDescriberHitTestTest {

    private val describer = UiNodeDescriber()

    @Test
    fun `tapping launcher icon behind full-screen non-clickable backdrop resolves icon and PredictionNodeMatcher matches only icon`() {
        val messengerIcon = mockNode(
            className = "android.widget.TextView",
            packageName = "com.google.android.apps.nexuslauncher",
            text = "Messenger",
            isClickable = true,
            bounds = RectSpec(50, 400, 250, 650),
        )
        val workspace = mockNode(
            className = "com.android.launcher3.Workspace",
            packageName = "com.google.android.apps.nexuslauncher",
            bounds = RectSpec(0, 152, 1080, 2361),
            children = listOf(messengerIcon),
        )
        val fullScreenBackdrop = mockNode(
            className = "android.view.View",
            packageName = "com.google.android.apps.nexuslauncher",
            contentDescription = "Ekran główny",
            isClickable = false,
            bounds = RectSpec(0, 152, 1080, 2361),
        )
        val root = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.google.android.apps.nexuslauncher",
            bounds = RectSpec(0, 0, 1080, 2424),
            children = listOf(workspace, fullScreenBackdrop),
        )

        val descriptor = describer.resolveInRoot(
            root = root,
            ownPackage = "com.zpi.slowomir",
            x = 150,
            y = 500,
        )

        assertNotNull(descriptor)
        assertEquals(ActionType.UI_CLICK, descriptor!!.type)
        assertEquals("FrameLayout[0]/Workspace[0]/TextView[0]", descriptor.hierarchyPath)
        assertEquals(ActionHasher.sha256Hex("messenger"), descriptor.labelHash)

        val entity = ActionDefEntity(
            idActionDef = 100L,
            actionHash = ActionHasher.hash(descriptor),
            actionType = descriptor.type,
            packageName = descriptor.packageName,
            resourceId = descriptor.resourceId,
            hierarchyPath = descriptor.hierarchyPath,
            labelHash = descriptor.labelHash,
        )
        val matches = PredictionNodeMatcher.findMatchingNodes(root, entity)
        assertEquals(1, matches.size)
        assertSame(messengerIcon, matches[0])
    }

    @Test
    fun `tapping icon covered by clickable full-screen backdrop prefers smaller clickable icon`() {
        val messengerIcon = mockNode(
            className = "android.widget.TextView",
            packageName = "com.google.android.apps.nexuslauncher",
            text = "Messenger",
            isClickable = true,
            bounds = RectSpec(50, 400, 250, 650),
        )
        val workspace = mockNode(
            className = "com.android.launcher3.Workspace",
            packageName = "com.google.android.apps.nexuslauncher",
            bounds = RectSpec(0, 152, 1080, 2361),
            children = listOf(messengerIcon),
        )
        val clickableFullScreenBackdrop = mockNode(
            className = "android.view.View",
            packageName = "com.google.android.apps.nexuslauncher",
            contentDescription = "Ekran główny",
            isClickable = true,
            bounds = RectSpec(0, 152, 1080, 2361),
        )
        val root = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.google.android.apps.nexuslauncher",
            bounds = RectSpec(0, 0, 1080, 2424),
            children = listOf(workspace, clickableFullScreenBackdrop),
        )

        val descriptor = describer.resolveInRoot(
            root = root,
            ownPackage = "com.zpi.slowomir",
            x = 150,
            y = 500,
        )

        assertNotNull(descriptor)
        assertEquals("FrameLayout[0]/Workspace[0]/TextView[0]", descriptor!!.hierarchyPath)
        assertEquals(ActionHasher.sha256Hex("messenger"), descriptor.labelHash)
    }

    @Test
    fun `nested clickable row selects inner switch when tapped on switch and row when tapped elsewhere`() {
        val switchNode = mockNode(
            className = "android.widget.Switch",
            packageName = "com.android.settings",
            resourceId = "android:id/switch_widget",
            contentDescription = "Wi-Fi",
            isClickable = true,
            bounds = RectSpec(900, 340, 1040, 460),
        )
        val labelNode = mockNode(
            className = "android.widget.TextView",
            packageName = "com.android.settings",
            text = "Wi-Fi",
            isClickable = false,
            bounds = RectSpec(48, 360, 400, 440),
        )
        val rowNode = mockNode(
            className = "android.widget.LinearLayout",
            packageName = "com.android.settings",
            resourceId = "com.android.settings:id/row_wifi",
            isClickable = true,
            bounds = RectSpec(0, 300, 1080, 500),
            children = listOf(labelNode, switchNode),
        )
        val root = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.android.settings",
            bounds = RectSpec(0, 0, 1080, 2400),
            children = listOf(rowNode),
        )

        val switchDescriptor = describer.resolveInRoot(
            root = root,
            ownPackage = "com.zpi.slowomir",
            x = 960,
            y = 400,
        )
        assertNotNull(switchDescriptor)
        assertEquals("FrameLayout[0]/LinearLayout[0]/Switch[1]", switchDescriptor!!.hierarchyPath)
        assertEquals("android:id/switch_widget", switchDescriptor.resourceId)

        val rowDescriptor = describer.resolveInRoot(
            root = root,
            ownPackage = "com.zpi.slowomir",
            x = 200,
            y = 400,
        )
        assertNotNull(rowDescriptor)
        assertEquals("FrameLayout[0]/LinearLayout[0]", rowDescriptor!!.hierarchyPath)
        assertEquals("com.android.settings:id/row_wifi", rowDescriptor.resourceId)
    }

    @Test
    fun `enabled smaller icon beats disabled clickable overlay`() {
        val enabledIcon = mockNode(
            className = "android.widget.TextView",
            packageName = "com.example.app",
            text = "Dalej",
            isClickable = true,
            isEnabled = true,
            bounds = RectSpec(100, 100, 300, 200),
        )
        val disabledOverlay = mockNode(
            className = "android.view.View",
            packageName = "com.example.app",
            contentDescription = "Overlay",
            isClickable = true,
            isEnabled = false,
            bounds = RectSpec(120, 120, 200, 180), // smaller area, but disabled
        )
        val root = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.example.app",
            bounds = RectSpec(0, 0, 1080, 2400),
            children = listOf(enabledIcon, disabledOverlay),
        )

        val descriptor = describer.resolveInRoot(root, "com.zpi.slowomir", 150, 150)
        assertNotNull(descriptor)
        assertEquals("FrameLayout[0]/TextView[0]", descriptor!!.hierarchyPath)
        assertEquals(ActionHasher.sha256Hex("dalej"), descriptor.labelHash)
    }

    private data class RectSpec(val left: Int, val top: Int, val right: Int, val bottom: Int)

    private fun mockNode(
        className: String,
        packageName: String? = null,
        resourceId: String? = null,
        text: String? = null,
        contentDescription: String? = null,
        isClickable: Boolean = false,
        isEnabled: Boolean = true,
        isPassword: Boolean = false,
        isEditable: Boolean = false,
        isVisibleToUser: Boolean = true,
        bounds: RectSpec,
        children: List<AccessibilityNodeInfo> = emptyList(),
    ): AccessibilityNodeInfo {
        val node = mock<AccessibilityNodeInfo>()
        whenever(node.className).thenReturn(className)
        whenever(node.packageName).thenReturn(packageName)
        whenever(node.viewIdResourceName).thenReturn(resourceId)
        whenever(node.text).thenReturn(text)
        whenever(node.contentDescription).thenReturn(contentDescription)
        whenever(node.isClickable).thenReturn(isClickable)
        whenever(node.isEnabled).thenReturn(isEnabled)
        whenever(node.isPassword).thenReturn(isPassword)
        whenever(node.isEditable).thenReturn(isEditable)
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
