package com.linehu.asi.data.notifications

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AsiNotification(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val timestamp: Long,
)

/**
 * In-memory notification mirror fed by [com.linehu.asi.service.AsiNotificationListenerService].
 * The listener runs in the same process, so a simple singleton flow is enough.
 */
class NotificationRepository {

    private val _notifications = MutableStateFlow<List<AsiNotification>>(emptyList())
    val notifications: StateFlow<List<AsiNotification>> = _notifications.asStateFlow()

    fun onPosted(context: Context, sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        val label = runCatching {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(sbn.packageName, 0),
            ).toString()
        }.getOrDefault(sbn.packageName)
        val entry = AsiNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            appLabel = label,
            title = title,
            text = text,
            timestamp = sbn.postTime,
        )
        _notifications.update { current ->
            (listOf(entry) + current.filterNot { it.key == entry.key }).take(50)
        }
    }

    fun onRemoved(key: String) = _notifications.update { list ->
        list.filterNot { it.key == key }
    }

    fun clearAll() = _notifications.update { emptyList() }

    fun disconnected() = clearAll()

    companion object {
        fun isListenerEnabled(context: Context): Boolean {
            val enabledPackages = androidx.core.app.NotificationManagerCompat
                .getEnabledListenerPackages(context)
            return enabledPackages.contains(context.packageName)
        }
    }
}
