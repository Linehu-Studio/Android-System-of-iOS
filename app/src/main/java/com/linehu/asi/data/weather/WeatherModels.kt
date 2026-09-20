package com.linehu.asi.data.weather

data class City(val name: String, val lat: Double, val lon: Double)

data class HourForecast(
    val timeLabel: String,
    val tempC: Int,
    val code: Int,
)

data class DayForecast(
    val dateLabel: String,
    val minC: Int,
    val maxC: Int,
    val code: Int,
)

data class WeatherData(
    val city: String,
    val currentTempC: Int,
    val code: Int,
    val isDay: Boolean,
    val humidity: Int?,
    val windKmh: Double?,
    val hourly: List<HourForecast>,
    val daily: List<DayForecast>,
)

/** WMO weather interpretation codes → Chinese description. */
fun weatherText(code: Int): String = when (code) {
    0 -> "晴"
    1 -> "大致晴朗"
    2 -> "多云"
    3 -> "阴"
    45, 48 -> "雾"
    51, 53, 55 -> "毛毛雨"
    56, 57 -> "冻毛毛雨"
    61, 63, 65 -> "雨"
    66, 67 -> "冻雨"
    71, 73, 75 -> "雪"
    77 -> "雪粒"
    80, 81, 82 -> "阵雨"
    85, 86 -> "阵雪"
    95 -> "雷阵雨"
    96, 99 -> "雷阵雨伴冰雹"
    else -> "未知"
}

val DEFAULT_CITIES = listOf(
    City("北京", 39.9042, 116.4074),
    City("上海", 31.2304, 121.4737),
    City("广州", 23.1291, 113.2644),
    City("深圳", 22.5431, 114.0579),
    City("杭州", 30.2741, 120.1551),
    City("成都", 30.5728, 104.0668),
)
