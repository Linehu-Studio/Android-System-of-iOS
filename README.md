# ASI — Android System of iOS

在 Android 上获得 iOS 18 桌面体验的整套方案：一个可设为默认桌面的 iOS 风格 Launcher、一套 PC 端设备初始化工具，以及（规划中的）root 设备系统模块。

> **重要：这不是 iOS 虚拟机。** iOS 无法在非苹果硬件上合法运行（硬件绑定 + 闭源）。ASI 是在 Android 原生层复刻 iOS 18 的桌面环境——锁屏、SpringBoard、灵动岛、控制中心、通知中心、多任务切换器，以及六个内置 App。

## 功能总览

| 模块 | 说明 |
|---|---|
| 锁屏 | 大时钟 + 日期，上滑解锁（橡皮筋阻尼 + 视差退场） |
| SpringBoard | 4×6 分页网格、毛玻璃 Dock、文件夹（拖拽合并/自动展开）、长按抖动重排、Spotlight 下拉搜索 |
| 真实 App 映射 | 桌面显示设备上全部可启动 App，squircle 化图标，点击直接启动 |
| 灵动岛 | 贴合挖孔屏；充电态、计时器倒读秒、"正在打开 xxx" 瞬时态 |
| 控制中心 | 顶部右侧下拉；手电/音量/亮度真实可控，WiFi/蓝牙等跳系统面板 |
| 通知中心 | 顶部左侧下拉；读取真实系统通知（需授权），支持一键清除 |
| 多任务 | 上滑到底触发；卡片式切换器（自记录 + 可选系统使用统计） |
| 内置 App | 计算器、时钟（秒表/计时器→灵动岛联动/到点通知）、备忘录（Room 持久化）、天气（离线 mock / Open-Meteo）、相册（MediaStore + 双指缩放查看）、设置 |
| 个性化 | 深/浅色、6 款渐变壁纸（可写回系统壁纸）、自由窗口（freeform）开关 |
| AssistiveTouch | 可拖动悬浮球，全功能入口兜底（顶部下拉被系统抢占时的替代入口） |

## 构建与安装

环境要求：JDK 17、Android SDK（platform 35）。

```powershell
$env:JAVA_HOME = "C:\Program Files\Zulu\zulu-17"   # 或写入 gradle.properties（已内置）
.\gradlew.bat assembleDebug
.\gradlew.bat :app:testDebugUnitTest               # 纯 JVM 单元测试
```

安装到设备（USB 调试已开启）：

```powershell
.\gradlew.bat installDebug
# 或一键初始化（授权 + freeform + 能力检测）：
tools\setup-device.bat
```

安装后在系统"桌面"选择框中把 ASI 设为默认桌面即可。

## ASI 套件（不止一个 App）

- **`tools/setup-device.ps1`** — ADB 一键初始化：授予通知/照片运行时权限，`appops` 授予 WRITE_SETTINGS 与 PACKAGE_USAGE_STATS，直接启用通知监听服务，强制开启全局 freeform 多窗口，并检测设备 root 状态。
- **`magisk-module/`（规划中）** — root 设备可选：将 ASI 提升为系统特权应用，解锁真实最近任务、系统级窗口管理等普通应用做不到的能力。
- **ASI Linux（规划中）** — root + chroot Debian，提供真正的"类 WSL"终端体验。

## 平台限制（如实告知）

1. **无法把第三方 App 画面嵌入自绘窗口** —— freeform 窗口由系统 WM 摆放（圆角归系统），无法套 iOS 边框；需要系统签名/特权才可能做到。
2. **拿不到系统真实最近任务** —— `REAL_GET_TASKS` 是系统权限；切换器使用自记录启动历史 + 可选 UsageStats（两种来源在卡片上标注）。
3. **锁屏是模拟场景** —— 无法拦截电源键/指纹。
4. **WiFi/蓝牙/飞行模式/勿扰** —— Android 10+ 第三方应用只能拉起系统面板，无法直接切换（控制中心磁贴已如实标注）。
5. **灵动岛仅 ASI 进程内可见** —— 全局悬浮需要 `SYSTEM_ALERT_WINDOW`，当前未做。
6. **顶部下拉与系统通知帘抢手势** —— 已提供悬浮球/长按壁纸替代入口兜底。
7. **API < 31 无实时毛玻璃** —— 自动降级为半透明 scrim + 预模糊壁纸。
8. **Spotlight 暂不支持拼音首字母搜索**。

## 技术栈

Kotlin 2.1 · Jetpack Compose (BOM 2025.01) · Room · DataStore · 单 Activity + 自绘场景状态机 · 手动 DI · minSdk 26 / targetSdk 35

## 项目结构

```
app/src/main/java/com/linehu/asi/
├── core/          # designsystem(主题/squircle/毛玻璃/状态栏) gesture animation util
├── data/          # Room(布局/备忘录/启动历史) DataStore(设置) apps/ weather/ wallpaper/ notifications/ recents/
├── shell/         # 场景状态机 + 壳层（LauncherRoot）
├── system/        # AppLauncher FreeformManager 手电/亮度/音量
├── service/       # NotificationListenerService
└── feature/       # lockscreen springboard dynamicisland controlcenter notificationcenter recents assistivetouch calculator clock notes weather photos settings
```
