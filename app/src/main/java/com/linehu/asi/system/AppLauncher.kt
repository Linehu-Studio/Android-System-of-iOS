package com.linehu.asi.system

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import com.linehu.asi.data.recents.LaunchEventEntity
import com.linehu.asi.data.recents.LaunchHistoryDao
import com.linehu.asi.data.settings.FreeformMode
import com.linehu.asi.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Single funnel for launching external Android apps from ASI: records the
 * launch into history (for the app switcher) and picks freeform vs fullscreen
 * per the user's setting and device capability.
 */
class AppLauncher(
    private val context: Context,
    private val launchHistory: LaunchHistoryDao,
    private val freeform: FreeformManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    fun launch(
        app: AppInfo,
        freeformMode: FreeformMode = FreeformMode.AUTO,
        onIslandFlash: (String) -> Unit = {},
    ) {
        scope.launch {
            launchHistory.insert(LaunchEventEntity(appKey = app.key, timestamp = System.currentTimeMillis()))
        }
        onIslandFlash(app.label)

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(app.componentName.packageName, app.componentName.className)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }

        if (freeform.shouldLaunchFreeform(freeformMode)) {
            val options = ActivityOptions.makeBasic().apply {
                try {
                    // setLaunchWindowingMode is a TestApi in the public SDK —
                    // call it reflectively; 5 == WINDOWING_MODE_FREEFORM (stable value).
                    val method = ActivityOptions::class.java
                        .getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                    method.invoke(this, 5)
                    setLaunchBounds(freeform.defaultWindowBounds())
                } catch (t: Throwable) {
                    // Not exposed on this build — fall through to normal launch.
                }
            }
            val ok = runCatching { context.startActivity(intent, options.toBundle()) }.isSuccess
            if (!ok) runCatching { context.startActivity(intent) }
        } else {
            runCatching { context.startActivity(intent) }
        }
    }
}
