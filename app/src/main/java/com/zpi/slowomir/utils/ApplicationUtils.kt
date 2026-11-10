package com.zpi.slowomir.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ApplicationUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class AppItem(
        val name: String,
        val packageName: String,
        val icon: Drawable,
    )


    sealed interface ApplicationLookupResult {
        data class Found(val packageName: String) : ApplicationLookupResult
        data class Ambiguous(val options: List<AppItem>) : ApplicationLookupResult
        object NotFound : ApplicationLookupResult
        object EmptyQuery : ApplicationLookupResult
    }

    fun lookup(name: String): ApplicationLookupResult {
        if (name.isBlank()) {
            return ApplicationLookupResult.EmptyQuery
        }

        val apps = getInstalledApps()
        val ordered = orderApps(name, apps)

        if (ordered.isEmpty()) {
            return ApplicationLookupResult.NotFound
        }

        val topMatch = ordered.first()
        val topScore = topMatch.second

        if (topScore >= THRESHOLD) {
            return ApplicationLookupResult.Found(topMatch.first.packageName)
        }

        val candidates = ordered
            .filter { it.second >= MIN_THRESHOLD }
            .map { it.first }
            .take(MAX_AMBIGUOUS_OPTIONS)

        if (candidates.isNotEmpty()) {
            return ApplicationLookupResult.Ambiguous(candidates)
        }

        return ApplicationLookupResult.NotFound
    }

    private fun orderApps(name: String, apps: List<AppItem>): List<Pair<AppItem, Double>> {
        return apps
            .map { app ->
                val score = StringUtils.stringSimilarity(name, app.name)
                val isSubstring = app.name.contains(name, ignoreCase = true)
                val finalScore = if (isSubstring) maxOf(score, THRESHOLD) else score
                app to finalScore
            }
            .sortedByDescending { (_, score) -> score }
    }

    private fun getInstalledApps(): List<AppItem> {
        val packageManager = context.packageManager

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

    companion object {
        const val TAG = "ApplicationUtils"
        const val THRESHOLD = 0.8
        const val MIN_THRESHOLD = 0.35
        const val MAX_AMBIGUOUS_OPTIONS = 5
    }
}