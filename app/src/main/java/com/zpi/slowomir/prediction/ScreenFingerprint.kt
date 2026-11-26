package com.zpi.slowomir.prediction

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.zpi.slowomir.overlay.log.ActionHasher
import java.util.Locale

object ScreenFingerprint {

    const val UNKNOWN_SCREEN = "UNKNOWN_SCREEN"

    private const val MAX_NODES = 250
    private const val MAX_RAW_LABEL_LENGTH = 80
    private const val MAX_LABEL_LENGTH = 40
    private const val MAX_SCROLL_PEEK_CHILDREN = 2
    private const val SHALLOW_SCROLL_MAX_DEPTH = 2

    private val SEPARATOR_REGEX = Regex("""[\r\n•·|]+""")
    private val TAB_SUFFIX_REGEX = Regex(""",\s*(?:karta|tab)\s+\d+\s+(?:z|of)\s+\d+""", RegexOption.IGNORE_CASE)
    private val COUNT_SEGMENT_REGEX = Regex(""",\s*\d+\s+[^,]+""", RegexOption.IGNORE_CASE)
    private val TRAILING_BADGE_REGEX = Regex("""\s+\d+\+?$""")
    private val PAREN_COUNT_REGEX = Regex("""\s*\(\s*\d+\s*\)""")
    private val CLOCK_OR_DATE_REGEX = Regex(""".*\d+[:./-]\d+.*""")

    private val SYSTEM_IGNORED_RESOURCE_IDS = setOf(
        "android:id/statusBarBackground",
        "android:id/navigationBarBackground",
        "android:id/content",
    )

    private val SYSTEM_BAR_BACKGROUND_IDS = setOf(
        "android:id/statusBarBackground",
        "android:id/navigationBarBackground",
    )

    private val TOP_CHROME_KEYWORDS = listOf(
        "toolbar",
        "action_bar",
        "actionbar",
        "appbar",
        "collapsing_toolbar",
        "collapsingtoolbar",
        "top_bar",
        "topbar",
    )

    private val BOTTOM_CHROME_KEYWORDS = listOf(
        "bottom_nav",
        "bottomnav",
        "bottom_navigation",
        "bottomnavigation",
        "navigation_bar",
        "navigationbar",
        "bottom_bar",
        "bottombar",
        "tab_layout",
        "tablayout",
        "tab_bar",
        "tabbar",
    )

    private enum class ChromeKind {
        NONE,
        TOP_CHROME,
        BOTTOM_CHROME,
    }

    private data class Entry(
        val node: AccessibilityNodeInfo,
        val inChromeBar: ChromeKind,
        val depth: Int,
    )

    fun compute(
        root: AccessibilityNodeInfo?,
        screenWidth: Int = 1080,
        screenHeight: Int = 2400,
        windowTitle: CharSequence? = null,
    ): String {
        if (root == null) return UNKNOWN_SCREEN

        val pkg = root.packageName?.toString()?.trim()?.takeIf { it.isNotEmpty() }

        val windowBounds = Rect()
        root.getBoundsInScreen(windowBounds)
        val rawW = windowBounds.right - windowBounds.left
        val rawH = windowBounds.bottom - windowBounds.top

        val winTop: Int
        val winBottom: Int
        val winHeight: Int
        if (rawW > 0 && rawH > 0) {
            winTop = windowBounds.top
            winBottom = windowBounds.bottom
            winHeight = rawH
        } else {
            winTop = 0
            winBottom = screenHeight.coerceAtLeast(1)
            winHeight = winBottom
        }

        val topBarThreshold = winTop + (winHeight * 0.12f).toInt()
        val bottomBarThreshold = winTop + (winHeight * 0.86f).toInt()
        val headingThreshold = winTop + (winHeight * 0.42f).toInt()

        val signals = mutableSetOf<String>()
        val titles = mutableSetOf<String>()
        val topTexts = mutableSetOf<String>()
        val fallbackHeadings = mutableListOf<String>()

        val resolvedWindowTitle = windowTitle ?: runCatching { root.window?.title }.getOrNull()
        normalizeLabel(resolvedWindowTitle)?.let { title ->
            titles.add(title)
            signals.add("TITLE:$title")
        }

        normalizeLabel(root.paneTitle)?.let { pane ->
            signals.add("PANE:$pane")
        }

        val bounds = Rect()
        val queue = ArrayDeque<Entry>()
        queue.add(Entry(root, ChromeKind.NONE, depth = 0))
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val entry = queue.removeFirst()
            val node = entry.node
            visited++

            val rawResId = node.viewIdResourceName
            if (isSystemBarBackground(rawResId)) {
                continue
            }

            normalizeLabel(node.paneTitle)?.let { pane ->
                signals.add("PANE:$pane")
            }

            val effectiveChrome = if (entry.inChromeBar != ChromeKind.NONE) {
                entry.inChromeBar
            } else {
                classifyChrome(node)
            }

            val cleanResId = cleanResourceId(rawResId)
            node.getBoundsInScreen(bounds)
            val hasBounds = bounds.right > bounds.left && bounds.bottom > bounds.top

            when (effectiveChrome) {
                ChromeKind.TOP_CHROME -> {
                    if (isClickable(node)) {
                        if (cleanResId != null) {
                            signals.add("TOP_ACTION_RES:$cleanResId")
                        } else {
                            normalizeLabel(node.contentDescription ?: node.text)?.let { label ->
                                signals.add("TOP_ACTION:$label")
                            }
                        }
                    } else {
                        normalizeLabel(node.text ?: node.contentDescription)?.let { title ->
                            titles.add(title)
                            signals.add("TITLE:$title")
                        }
                    }
                }

                ChromeKind.BOTTOM_CHROME -> {
                    if (node.isSelected) {
                        normalizeLabel(node.contentDescription ?: node.text)?.let { selected ->
                            signals.add("TAB_SELECTED:$selected")
                        }
                    }
                    cleanResId?.let { resId ->
                        signals.add("BOTTOM_RES:$resId")
                    }
                    normalizeLabel(node.contentDescription ?: node.text)?.let { label ->
                        signals.add("BOTTOM_LABEL:$label")
                    }
                }

                ChromeKind.NONE -> {
                    val inTopBar = hasBounds && bounds.top >= winTop && bounds.bottom <= topBarThreshold
                    val inBottomBar = hasBounds && bounds.top >= bottomBarThreshold && bounds.bottom <= winBottom

                    if (inTopBar) {
                        if (node.isHeading) {
                            normalizeLabel(node.text ?: node.contentDescription)?.let { title ->
                                titles.add(title)
                                signals.add("TITLE:$title")
                            }
                        } else if (isClickable(node)) {
                            if (cleanResId != null) {
                                signals.add("TOP_ACTION_RES:$cleanResId")
                            } else {
                                normalizeLabel(node.contentDescription ?: node.text)?.let { label ->
                                    signals.add("TOP_ACTION:$label")
                                }
                            }
                        } else {
                            normalizeLabel(node.text ?: node.contentDescription)?.let { text ->
                                topTexts.add(text)
                            }
                        }
                    } else if (inBottomBar) {
                        if (node.isSelected) {
                            normalizeLabel(node.contentDescription ?: node.text)?.let { selected ->
                                signals.add("TAB_SELECTED:$selected")
                            }
                        }
                        cleanResId?.let { resId ->
                            signals.add("BOTTOM_RES:$resId")
                        }
                        normalizeLabel(node.contentDescription ?: node.text)?.let { label ->
                            signals.add("BOTTOM_LABEL:$label")
                        }
                    }

                    if (node.isHeading && !isClickable(node) && hasBounds &&
                        bounds.top >= winTop && bounds.bottom <= headingThreshold
                    ) {
                        normalizeLabel(node.text ?: node.contentDescription)?.let { heading ->
                            fallbackHeadings.add(heading)
                        }
                    }
                }
            }

            if (effectiveChrome == ChromeKind.NONE && isVerticalScrollContainer(node, entry.depth)) {
                var peeked = 0
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    if (!child.isVisibleToUser) continue
                    peeked++

                    normalizeLabel(child.paneTitle)?.let { pane ->
                        signals.add("PANE:$pane")
                    }
                    if (child.isHeading && !isClickable(child)) {
                        child.getBoundsInScreen(bounds)
                        val childHasBounds = bounds.right > bounds.left && bounds.bottom > bounds.top
                        if (childHasBounds && bounds.top >= winTop && bounds.bottom <= headingThreshold) {
                            normalizeLabel(child.text ?: child.contentDescription)?.let { heading ->
                                fallbackHeadings.add(heading)
                            }
                        }
                    }
                    if (peeked >= MAX_SCROLL_PEEK_CHILDREN) break
                }
            } else {
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    if (child.isVisibleToUser) {
                        queue.add(Entry(child, effectiveChrome, entry.depth + 1))
                    }
                }
            }
        }

        for (text in topTexts) {
            if (text !in titles) {
                signals.add("TOP_TEXT:$text")
            }
        }

        if (titles.isEmpty() && signals.none { it.startsWith("PANE:") } && fallbackHeadings.isNotEmpty()) {
            signals.add("TITLE:${fallbackHeadings.first()}")
        }

        if (signals.size < PredictionConfig.MIN_SCREEN_IDS) {
            return UNKNOWN_SCREEN
        }

        val allIdentifiers = if (pkg != null) signals + "PKG:$pkg" else signals
        val sorted = allIdentifiers.sorted().joinToString(";")
        return ActionHasher.sha256Hex(sorted)
    }

    private fun cleanResourceId(raw: String?): String? {
        val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (trimmed in SYSTEM_IGNORED_RESOURCE_IDS) return null
        val lower = trimmed.lowercase(Locale.ROOT)
        if ("(name removed)" in lower || "obfuscated" in lower) return null
        return trimmed
    }

    private fun isSystemBarBackground(raw: String?): Boolean =
        raw?.trim() in SYSTEM_BAR_BACKGROUND_IDS

    private fun normalizeLabel(raw: CharSequence?): String? {
        val initial = raw?.toString()?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_RAW_LABEL_LENGTH }
            ?: return null

        val stripped = initial
            .replace(SEPARATOR_REGEX, ", ")
            .replace(TAB_SUFFIX_REGEX, "")
            .replace(COUNT_SEGMENT_REGEX, "")
            .replace(PAREN_COUNT_REGEX, "")
            .replace(TRAILING_BADGE_REGEX, "")
            .trim()

        if (stripped.isEmpty() || stripped.length > MAX_LABEL_LENGTH) return null
        if (CLOCK_OR_DATE_REGEX.matches(stripped)) return null
        if (!stripped.any { it.isLetter() }) return null
        if (stripped.count { it.isDigit() } > 2) return null

        return stripped.lowercase(Locale.ROOT)
    }

    private fun classifyChrome(node: AccessibilityNodeInfo): ChromeKind {
        val rawResId = node.viewIdResourceName
        if (isSystemBarBackground(rawResId)) return ChromeKind.NONE

        val cls = node.className?.toString()?.lowercase(Locale.ROOT) ?: ""
        val cleanResName = cleanResourceId(rawResId)?.substringAfterLast('/')?.lowercase(Locale.ROOT) ?: ""

        if (TOP_CHROME_KEYWORDS.any { it in cls || it in cleanResName }) {
            return ChromeKind.TOP_CHROME
        }
        if (BOTTOM_CHROME_KEYWORDS.any { it in cls || it in cleanResName }) {
            return ChromeKind.BOTTOM_CHROME
        }
        return ChromeKind.NONE
    }

    private fun isVerticalScrollContainer(node: AccessibilityNodeInfo, depth: Int): Boolean {
        val cls = node.className?.toString()?.lowercase(Locale.ROOT) ?: ""
        if (cls.contains("recyclerview") ||
            cls.contains("listview") ||
            cls.contains("gridview") ||
            cls.contains("webview")
        ) {
            return true
        }
        if (depth <= SHALLOW_SCROLL_MAX_DEPTH) {
            return false
        }
        if (cls.contains("scrollview") && !cls.contains("horizontalscrollview")) {
            return true
        }
        return node.isScrollable &&
            !cls.contains("horizontal") &&
            !cls.contains("tab") &&
            !cls.contains("pager")
    }

    private fun isClickable(node: AccessibilityNodeInfo): Boolean =
        node.isClickable || node.actionList.orEmpty().any { it.id == AccessibilityNodeInfo.ACTION_CLICK }
}
