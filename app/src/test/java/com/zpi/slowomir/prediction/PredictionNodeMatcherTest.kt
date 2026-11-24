package com.zpi.slowomir.prediction

import android.view.accessibility.AccessibilityNodeInfo
import com.zpi.slowomir.overlay.log.ActionDescriptor
import com.zpi.slowomir.overlay.log.ActionHasher
import com.zpi.slowomir.overlay.log.ActionType
import com.zpi.slowomir.overlay.log.HierarchyPath
import com.zpi.slowomir.overlay.log.db.ActionDefEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class PredictionNodeMatcherTest {

    @Test
    fun `exact match by actionHash returns single target node`() {
        val expectedPath = HierarchyPath.format(
            listOf(
                "FrameLayout[0]",
                "RecyclerView[0]",
                "LinearLayout[2]",
            )
        )
        val labelHash = ActionHasher.sha256Hex("wyświetlacz")
        val targetDef = makeTargetDef(
            packageName = "com.android.settings",
            resourceId = "com.android.settings:id/row",
            hierarchyPath = expectedPath,
            labelHash = labelHash,
        )

        val displayRow = makeRowNode(
            packageName = "com.android.settings",
            resourceId = "com.android.settings:id/row",
            label = "Wyświetlacz",
        )
        val root = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.android.settings",
            children = listOf(
                mockNode(
                    className = "androidx.recyclerview.widget.RecyclerView",
                    children = listOf(
                        makeRowNode("com.android.settings", "com.android.settings:id/row", "Sieć i internet"),
                        makeRowNode("com.android.settings", "com.android.settings:id/row", "Połączone urządzenia"),
                        displayRow,
                    ),
                ),
            ),
        )

        val matches = PredictionNodeMatcher.findMatchingNodes(root, targetDef)
        assertEquals(1, matches.size)
        assertSame(displayRow, matches[0])
    }

    @Test
    fun `scroll-shifted item falls back cleanly to package resourceId labelHash when unique`() {
        // Recorded at index 3 in RecyclerView
        val oldPath = HierarchyPath.format(
            listOf(
                "FrameLayout[0]",
                "RecyclerView[0]",
                "LinearLayout[3]",
            )
        )
        val labelHash = ActionHasher.sha256Hex("bateria")
        val targetDef = makeTargetDef(
            packageName = "com.android.settings",
            resourceId = "com.android.settings:id/row",
            hierarchyPath = oldPath,
            labelHash = labelHash,
        )

        // After scrolling, "Bateria" moved to index 1
        val batteryRow = makeRowNode(
            packageName = "com.android.settings",
            resourceId = "com.android.settings:id/row",
            label = "Bateria",
        )
        val root = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.android.settings",
            children = listOf(
                mockNode(
                    className = "androidx.recyclerview.widget.RecyclerView",
                    children = listOf(
                        makeRowNode("com.android.settings", "com.android.settings:id/row", "Aplikacje"),
                        batteryRow,
                    ),
                ),
            ),
        )

        val matches = PredictionNodeMatcher.findMatchingNodes(root, targetDef)
        assertEquals(1, matches.size)
        assertSame(batteryRow, matches[0])
    }

    @Test
    fun `ambiguous fallback returns multiple matches so caller rejects suggestion`() {
        val oldPath = HierarchyPath.format(
            listOf(
                "FrameLayout[0]",
                "RecyclerView[0]",
                "Button[5]",
            )
        )
        val labelHash = ActionHasher.sha256Hex("otwórz")
        val targetDef = makeTargetDef(
            packageName = "com.example.app",
            resourceId = "com.example.app:id/btn",
            hierarchyPath = oldPath,
            labelHash = labelHash,
        )

        val root = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.example.app",
            children = listOf(
                mockNode(
                    className = "androidx.recyclerview.widget.RecyclerView",
                    children = listOf(
                        mockNode(
                            className = "android.widget.Button",
                            resourceId = "com.example.app:id/btn",
                            text = "Otwórz",
                            isClickable = true,
                        ),
                        mockNode(
                            className = "android.widget.Button",
                            resourceId = "com.example.app:id/btn",
                            text = "Otwórz",
                            isClickable = true,
                        ),
                    ),
                ),
            ),
        )

        val matches = PredictionNodeMatcher.findMatchingNodes(root, targetDef)
        assertEquals(2, matches.size)
    }

    @Test
    fun `guarded node with protected descendant label or password flag is never returned`() {
        // Guarded child label inside clickable LinearLayout
        val path = HierarchyPath.format(listOf("FrameLayout[0]", "LinearLayout[0]"))
        val protectedLabelHash = ActionHasher.sha256Hex("usuń konto")
        val protectedDef = makeTargetDef(
            packageName = "com.example.app",
            resourceId = "com.example.app:id/danger_row",
            hierarchyPath = path,
            labelHash = protectedLabelHash,
        )

        val rootWithProtectedDescendant = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.example.app",
            children = listOf(
                makeRowNode("com.example.app", "com.example.app:id/danger_row", "Usuń konto"),
            ),
        )
        assertTrue(PredictionNodeMatcher.findMatchingNodes(rootWithProtectedDescendant, protectedDef).isEmpty())

        // Password field
        val passwordPath = HierarchyPath.format(listOf("FrameLayout[0]", "EditText[0]"))
        val passwordDef = makeTargetDef(
            packageName = "com.example.app",
            resourceId = "com.example.app:id/password",
            hierarchyPath = passwordPath,
            labelHash = null,
        )
        val rootWithPassword = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.example.app",
            children = listOf(
                mockNode(
                    className = "android.widget.EditText",
                    packageName = "com.example.app",
                    resourceId = "com.example.app:id/password",
                    isClickable = true,
                    isPassword = true,
                ),
            ),
        )
        assertTrue(PredictionNodeMatcher.findMatchingNodes(rootWithPassword, passwordDef).isEmpty())
    }

    @Test
    fun `scroll fallback distinguishes clickable parent LinearLayout from clickable child TextView with same label`() {
        val recordedPath = HierarchyPath.format(
            listOf(
                "FrameLayout[0]",
                "RecyclerView[0]",
                "LinearLayout[4]",
            )
        )
        val labelHash = ActionHasher.sha256Hex("dźwięk")
        val targetDef = makeTargetDef(
            packageName = "com.android.settings",
            resourceId = null,
            hierarchyPath = recordedPath,
            labelHash = labelHash,
        )

        // After scroll, the row sits at index 1, and both the LinearLayout row and inner TextView are clickable with null resId
        val targetRow = mockNode(
            className = "android.widget.LinearLayout",
            packageName = "com.android.settings",
            resourceId = null,
            isClickable = true,
            children = listOf(
                mockNode(
                    className = "android.widget.TextView",
                    packageName = "com.android.settings",
                    resourceId = null,
                    text = "Dźwięk",
                    isClickable = true,
                ),
            ),
        )
        val root = mockNode(
            className = "android.widget.FrameLayout",
            packageName = "com.android.settings",
            children = listOf(
                mockNode(
                    className = "androidx.recyclerview.widget.RecyclerView",
                    children = listOf(
                        makeRowNode("com.android.settings", "com.android.settings:id/other", "Sieć"),
                        targetRow,
                    ),
                ),
            ),
        )

        val matches = PredictionNodeMatcher.findMatchingNodes(root, targetDef)
        assertEquals(1, matches.size)
        assertSame(targetRow, matches[0])
    }

    private fun makeTargetDef(
        packageName: String,
        resourceId: String?,
        hierarchyPath: String,
        labelHash: String?,
    ): ActionDefEntity {
        val descriptor = ActionDescriptor(
            type = ActionType.UI_CLICK,
            packageName = packageName,
            resourceId = resourceId,
            hierarchyPath = hierarchyPath,
            labelHash = labelHash,
        )
        return ActionDefEntity(
            idActionDef = 42L,
            actionHash = ActionHasher.hash(descriptor),
            actionType = ActionType.UI_CLICK,
            packageName = packageName,
            resourceId = resourceId,
            hierarchyPath = hierarchyPath,
            labelHash = labelHash,
        )
    }

    private fun makeRowNode(packageName: String, resourceId: String, label: String): AccessibilityNodeInfo =
        mockNode(
            className = "android.widget.LinearLayout",
            packageName = packageName,
            resourceId = resourceId,
            isClickable = true,
            children = listOf(
                mockNode(
                    className = "android.widget.TextView",
                    packageName = packageName,
                    text = label,
                ),
            ),
        )

    private fun mockNode(
        className: String,
        packageName: String? = null,
        resourceId: String? = null,
        text: String? = null,
        contentDescription: String? = null,
        isClickable: Boolean = false,
        isPassword: Boolean = false,
        isEditable: Boolean = false,
        isVisibleToUser: Boolean = true,
        children: List<AccessibilityNodeInfo> = emptyList(),
    ): AccessibilityNodeInfo {
        val node = mock<AccessibilityNodeInfo>()
        whenever(node.className).thenReturn(className)
        whenever(node.packageName).thenReturn(packageName)
        whenever(node.viewIdResourceName).thenReturn(resourceId)
        whenever(node.text).thenReturn(text)
        whenever(node.contentDescription).thenReturn(contentDescription)
        whenever(node.isClickable).thenReturn(isClickable)
        whenever(node.isPassword).thenReturn(isPassword)
        whenever(node.isEditable).thenReturn(isEditable)
        whenever(node.isVisibleToUser).thenReturn(isVisibleToUser)
        whenever(node.actionList).thenReturn(emptyList())
        whenever(node.childCount).thenReturn(children.size)
        for (i in children.indices) {
            whenever(node.getChild(i)).thenReturn(children[i])
        }
        return node
    }
}
