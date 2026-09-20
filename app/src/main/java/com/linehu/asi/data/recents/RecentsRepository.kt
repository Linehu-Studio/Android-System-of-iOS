package com.linehu.asi.data.recents

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.linehu.asi.data.apps.AppRepository
import com.linehu.asi.model.AppInfo
import com.linehu.asi.model.RecentsEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * App switcher data. Primary source: our own launch history (always
 * accurate for apps started from ASI). Optional enhancement: system-wide
 * UsageStats, which needs the special PACKAGE_USAGE_STATS grant.
 */
class RecentsRepository(
    private val context: Context,
    private val launchDao: LaunchHistoryDao,
    private val appRepository: AppRepository,
) {

    suspend fun recents(limit: Int = 12, includeUsageStats: Boolean = false): List<RecentsEntry> =
        withContext(Dispatchers.IO) {
            val appsByKey = appRepository.appsByKey.first()
            val own = launchDao.recentKeys(limit).mapNotNull { row ->
                appsByKey[row.appKey]?.let { app ->
                    RecentsEntry(app.key, app.label, row.lastUsed, fromSystemStats = false)
                }
            }
            if (!includeUsageStats || !hasUsageStatsAccess()) {
                return@withContext own
            }
            // Merge in system-wide foreground history (last 24h), dedup by key.
            val system = queryUsageStats(appsByKey)
            (own + system)
                .groupBy { it.appKey }
                .map { (_, entries) -> entries.maxBy { it.lastUsed } }
                .sortedByDescending { it.lastUsed }
                .take(limit)
        }

    suspend fun remove(appKey: String) = withContext(Dispatchers.IO) {
        launchDao.remove(appKey)
    }

    fun hasUsageStatsAccess(): Boolean = try {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    } catch (t: Throwable) {
        false
    }

    private fun queryUsageStats(appsByKey: Map<String, AppInfo>): List<RecentsEntry> {
        if (!hasUsageStatsAccess()) return emptyList()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 24 * 3600_000L, now)
        val lastByPackage = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastByPackage[event.packageName] = event.timeStamp
            }
        }
        val keyByPackage = appsByKey.values.groupBy { it.componentName.packageName }
        return lastByPackage.mapNotNull { (pkg, time) ->
            keyByPackage[pkg]?.firstOrNull()?.let { app ->
                RecentsEntry(app.key, app.label, time, fromSystemStats = true)
            }
        }
    }
}
