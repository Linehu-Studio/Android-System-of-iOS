package com.linehu.asi.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.linehu.asi.data.notifications.NotificationRepository
import com.linehu.asi.util.asiContainer

/**
 * Feeds system notifications into the ASI notification center. Requires the
 * user to grant notification access in system settings; the repository
 * clears itself when the listener disconnects (revoked access, ROM killer).
 */
class AsiNotificationListenerService : NotificationListenerService() {

    private val repository get() = asiContainer().notificationRepository

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        repository.onPosted(this, sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        repository.onRemoved(sbn.key)
    }

    override fun onListenerConnected() {
        // Mirror what is already in the shade.
        activeNotifications?.forEach { repository.onPosted(this, it) }
    }

    override fun onListenerDisconnected() {
        repository.disconnected()
    }
}
