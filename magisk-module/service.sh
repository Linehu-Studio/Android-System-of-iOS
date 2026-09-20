#!/system/bin/sh
# Runs after boot (late_start service).
# Grants ASI the special-access permissions that normally require manual
# user navigation, so a factory-reset or ROM-update device is ready to go.

PKG=com.linehu.asi
until [ "$(dumpsys package $PKG 2>/dev/null | grep -c 'Package \[')" -ge 1 ]; do
    sleep 2
done

# Runtime permissions
pm grant $PKG android.permission.POST_NOTIFICATIONS 2>/dev/null
pm grant $PKG android.permission.READ_MEDIA_IMAGES 2>/dev/null
pm grant $PKG android.permission.READ_EXTERNAL_STORAGE 2>/dev/null

# Special app-ops
appops set $PKG WRITE_SETTINGS allow 2>/dev/null
appops set $PKG PACKAGE_USAGE_STATS allow 2>/dev/null

# Notification listener
cmd notification allow_listener "$PKG/com.linehu.asi.service.AsiNotificationListenerService" 2>/dev/null

# Global freeform windowing
settings put global enable_freeform_support 1
