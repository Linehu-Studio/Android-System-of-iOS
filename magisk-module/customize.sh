#!/system/bin/sh
# ASI System Module installer (Magisk systemless overlay)
#
# Places the ASI APK under /system/priv-app so it can request privileged
# permissions. The APK is expected next to this script (built via
# `..\gradlew.bat assembleDebug` and copied in as ASI.apk).
#
# ⚠️ 注意：
# - 每次更新 ASI 都要重新打包模块（priv-app 中的版本必须 >= 用户数据中的版本）
# - 特权应用要在 manifest 中声明 android:sharedUserId="android.uid.system" 才能拿
#   系统签名权限——当前 ASI 尚未声明（MVP），此模块先提供免授权 appops 环境。

APK="$MODPATH/ASI.apk"
SKIPUNZIP=0

if [ ! -f "$APK" ]; then
    ui_print "! 未找到 ASI.apk —— 请先构建并复制到模块目录"
    ui_print "  ./gradlew.bat assembleDebug"
    ui_print "  cp ../app/build/outputs/apk/debug/app-debug.apk ./ASI.apk"
    abort
fi

ui_print "- 创建 /system/priv-app/ASI"
mkdir -p "$MODPATH/system/priv-app/ASI"
mv -f "$APK" "$MODPATH/system/priv-app/ASI/ASI.apk"
chmod 644 "$MODPATH/system/priv-app/ASI/ASI.apk"
ui_print "- 安装完成，重启后生效"
