package com.linehu.asi.data.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.math.abs
import kotlin.math.sin

/** Where the weather app gets its data from. */
interface WeatherDataSource {
    suspend fun load(city: City): WeatherData
    suspend fun searchCity(query: String): List<City>
}

/** Deterministic offline data — the default until the user enables online. */
class MockWeatherSource : WeatherDataSource {

    override suspend fun load(city: City): WeatherData {
        val seed = abs(city.name.hashCode())
        val base = 8 + seed % 22 // 8..29°C
        val hourly = (0 until 24).map { h ->
            HourForecast(
                timeLabel = "${(h + 9) % 24}时",
                tempC = base + (sin(h / 3.0) * 4).toInt(),
                code = when {
                    h % 9 == 0 -> 3
                    h % 5 == 0 -> 61
                    h % 7 == 0 -> 2
                    else -> 0
                },
            )
        }
        val daily = (0 until 7).map { d ->
            DayForecast(
                dateLabel = if (d == 0) "今天" else "周${"一二三四五六日"[d % 7]}",
                minC = base - 4 - (d % 3),
                maxC = base + 3 + (d % 2),
                code = when (d % 4) {
                    0 -> 0
                    1 -> 2
                    2 -> 3
                    else -> 61
                },
            )
        }
        return WeatherData(
            city = city.name,
            currentTempC = base,
            code = 0,
            isDay = true,
            humidity = 40 + seed % 40,
            windKmh = 5.0 + seed % 20,
            hourly = hourly,
            daily = daily,
        )
    }

    override suspend fun searchCity(query: String): List<City> =
        DEFAULT_CITIES.filter { it.name.contains(query) }
}

/** Open-Meteo (free, keyless). Only network client in the whole app. */
class OpenMeteoSource(private val client: OkHttpClient = OkHttpClient()) : WeatherDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun load(city: City): WeatherData = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${city.lat}&longitude=${city.lon}" +
            "&current=temperature_2m,relative_humidity_2m,weather_code,is_day,wind_speed_10m" +
            "&hourly=temperature_2m,weather_code&forecast_days=2&timezone=auto" +
            "&daily=temperature_2m_max,temperature_2m_min,weather_code&forecast_days=7"
        val body = httpGet(url)
        val root = json.parseToJsonElement(body).jsonObject
        val current = root.getValue("current").jsonObject
        val hourly = root.getValue("hourly").jsonObject
        val daily = root.getValue("daily").jsonObject

        val hourTemps = hourly["temperature_2m"]?.jsonArray ?: emptyList()
        val hourCodes = hourly["weather_code"]?.jsonArray ?: emptyList()
        val startHour = java.time.LocalTime.now().hour
        val hourlyForecast = (startHour until startHour + 24).map { i ->
            HourForecast(
                timeLabel = if (i == startHour) "现在" else "${i % 24}时",
                tempC = hourTemps.getOrNull(i)?.jsonPrimitive?.content?.toDoubleOrNull()?.toInt() ?: 0,
                code = hourCodes.getOrNull(i)?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            )
        }

        val dayTimes = daily["time"]?.jsonArray ?: emptyList()
        val dayMax = daily["temperature_2m_max"]?.jsonArray ?: emptyList()
        val dayMin = daily["temperature_2m_min"]?.jsonArray ?: emptyList()
        val dayCodes = daily["weather_code"]?.jsonArray ?: emptyList()
        val weekNames = listOf("今天", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val dailyForecast = dayTimes.indices.map { d ->
            DayForecast(
                dateLabel = weekNames.getOrElse(d) { "" },
                minC = dayMin.getOrNull(d)?.jsonPrimitive?.content?.toDoubleOrNull()?.toInt() ?: 0,
                maxC = dayMax.getOrNull(d)?.jsonPrimitive?.content?.toDoubleOrNull()?.toInt() ?: 0,
                code = dayCodes.getOrNull(d)?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            )
        }

        WeatherData(
            city = city.name,
            currentTempC = current["temperature_2m"]?.jsonPrimitive?.content?.toDoubleOrNull()?.toInt() ?: 0,
            code = current["weather_code"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            isDay = current["is_day"]?.jsonPrimitive?.content == "1",
            humidity = current["relative_humidity_2m"]?.jsonPrimitive?.content?.toIntOrNull(),
            windKmh = current["wind_speed_10m"]?.jsonPrimitive?.content?.toDoubleOrNull(),
            hourly = hourlyForecast,
            daily = dailyForecast,
        )
    }

    override suspend fun searchCity(query: String): List<City> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext DEFAULT_CITIES
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$query&count=8&language=zh"
        runCatching {
            val root = json.parseToJsonElement(httpGet(url)).jsonObject
            root["results"]?.jsonArray?.map { r ->
                val o = r.jsonObject
                City(
                    name = o["name"]?.jsonPrimitive?.content.orEmpty(),
                    lat = o["latitude"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                    lon = o["longitude"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                )
            }.orEmpty()
        }.getOrDefault(DEFAULT_CITIES.filter { it.name.contains(query) })
    }

    private fun httpGet(url: String): String =
        client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            resp.body?.string().orEmpty()
        }
}
