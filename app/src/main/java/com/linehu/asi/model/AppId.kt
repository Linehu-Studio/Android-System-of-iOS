package com.linehu.asi.model

/** Built-in apps that ship with ASI and run as in-process Compose scenes. */
enum class AppId(val displayLabel: String) {
    CALCULATOR("计算器"),
    CLOCK("时钟"),
    NOTES("备忘录"),
    WEATHER("天气"),
    PHOTOS("照片"),
    SETTINGS("设置"),
}
