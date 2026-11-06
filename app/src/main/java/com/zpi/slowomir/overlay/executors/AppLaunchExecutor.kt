package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import com.zpi.slowomir.voice.mediator.CommandMediator
import kotlin.math.max

data class AppItem(
    val name: String,
    val packageName: String,
    val icon: Drawable,
)


class AppLaunchExecutor(
    private val service: AccessibilityService,
    private val
) {

    fun findApps(name: String): Result<AppItem> {
        val apps = getInstalledApps()
        val ordered = orderApps(name, apps)

        if (ordered.isEmpty()) {
            return Result.failure(Exception("No app with name: $name found."))
        }

        if (ordered.first().second > THRESHOLD) {
            return Result.success(ordered.first().first)
        }


        return
    }

    private fun orderApps(name: String, apps: List<AppItem>): List<Pair<AppItem, Double>> {
        return apps
            .map { app ->
                val score = stringSimilarity(name, app.name)
                val isSubstring = app.name.contains(name, ignoreCase = true)
                val finalScore = if (isSubstring) maxOf(score, THRESHOLD) else score
                app to finalScore
            }
        .sortedByDescending { (_, score) -> score }
    }

    private fun getInstalledApps(): List<AppItem> {
        val packageManager = service.packageManager

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfoList: List<ResolveInfo> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    mainIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(mainIntent, 0)
            }

        return resolveInfoList.map { resolveInfo ->
            AppItem(
                name = resolveInfo.loadLabel(packageManager).toString(),
                packageName = resolveInfo.activityInfo.packageName,
                icon = resolveInfo.loadIcon(packageManager)
            )
        }

    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[m][n]
    }

    private fun stringSimilarity(s1: String, s2: String): Double {
        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 1.0
        val distance = levenshteinDistance(s1.lowercase(), s2.lowercase())
        return 1.0 - (distance.toDouble() / maxLen)
    }

    companion object {
        val TAG = "AppLaunchExecutor"
        val THRESHOLD = 0.8
    }
}
