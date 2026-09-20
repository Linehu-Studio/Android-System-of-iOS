package com.linehu.asi.data.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.UserManager
import androidx.core.content.ContextCompat
import com.linehu.asi.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Enumerates launchable Android apps and observes install/uninstall. The
 * package set is exposed both as a list (for Spotlight / default layout) and
 * as a change signal (to keep the home grid in sync).
 */
class AppRepository(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    private val packageManager = context.packageManager

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: Flow<List<AppInfo>> = _apps
    val appsByKey: Flow<Map<String, AppInfo>> = _apps.map { list -> list.associateBy { it.key } }

    /** Bumps on every package add/remove/replace. */
    val packageChanged: Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        awaitClose { context.unregisterReceiver(receiver) }
    }

    fun start() {
        scope.launch { refresh() }
    }

    suspend fun refresh() = withContext(Dispatchers.Default) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        // queryIntentActivities with MATCH_ALL to catch every launchable entry.
        @Suppress("DEPRECATION")
        val resolveList = packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA)
        val apps = resolveList
            .asSequence()
            .mapNotNull { info -> info.activityInfo?.let { ai -> AppInfo(ai.loadLabel(packageManager).toString(), android.content.ComponentName(ai.packageName, ai.name)) } }
            .distinctBy { it.key }
            .sortedWith(compareByDescending<AppInfo> { it.label.isNotEmpty() && it.label[0] in '一'..'鿿' }.thenBy { it.label.lowercase() })
            .toList()
        _apps.value = apps
    }

    fun appInfoOf(key: String): AppInfo? = _apps.value.firstOrNull { it.key == key }

    suspend fun appInfoOfSuspending(key: String): AppInfo? {
        if (_apps.value.isEmpty()) refresh()
        return appInfoOf(key)
    }
}
