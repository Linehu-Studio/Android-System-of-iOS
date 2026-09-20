package com.linehu.asi.feature.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linehu.asi.core.designsystem.IosStatusBar
import com.linehu.asi.core.designsystem.IosText
import com.linehu.asi.data.weather.City
import com.linehu.asi.data.weather.DEFAULT_CITIES
import com.linehu.asi.data.weather.WeatherData
import com.linehu.asi.data.weather.WeatherDataSource
import com.linehu.asi.data.weather.weatherText
import kotlinx.coroutines.launch

fun weatherIcon(code: Int, isDay: Boolean = true): ImageVector = when (code) {
    0 -> Icons.Rounded.WbSunny
    1, 2 -> Icons.Rounded.WbCloudy
    3 -> Icons.Rounded.Cloud
    45, 48 -> Icons.Rounded.Umbrella
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Rounded.Grain
    71, 73, 75, 77, 85, 86 -> Icons.Rounded.WaterDrop
    95, 96, 99 -> Icons.Rounded.Bolt
    else -> Icons.Rounded.WbCloudy
}

/** iOS-weather-style screen: gradient background, big temp, hourly strip, 7-day list. */
@Composable
fun WeatherScreen(
    source: WeatherDataSource,
    modifier: Modifier = Modifier,
) {
    var city by remember { mutableStateOf(DEFAULT_CITIES.first()) }
    var pickingCity by remember { mutableStateOf(false) }
    val data by produceState<WeatherData?>(initialValue = null, city) {
        value = runCatching { source.load(city) }.getOrNull()
    }

    Column(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF4A90D9), Color(0xFF22548F), Color(0xFF16325C))),
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        IosStatusBar(contentColor = Color.White)
        LazyColumn(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Spacer(Modifier.height(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { pickingCity = true }
                        .padding(8.dp),
                ) {
                    Icon(Icons.Filled.LocationCity, null, tint = Color(0xCCFFFFFF))
                    IosText("  ${city.name}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                IosText(
                    "${data?.currentTempC ?: "--"}°",
                    fontSize = 92.sp,
                    fontWeight = FontWeight.Thin,
                    color = Color.White,
                )
                IosText(
                    data?.let { weatherText(it.code) } ?: "加载中…",
                    fontSize = 20.sp,
                    color = Color(0xCCFFFFFF),
                )
                val today = data?.daily?.firstOrNull()
                if (today != null) {
                    IosText(
                        "最高${today.maxC}° 最低${today.minC}°",
                        fontSize = 16.sp,
                        color = Color(0xCCFFFFFF),
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
            // Hourly strip
            item {
                LazyRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(vertical = 12.dp),
                ) {
                    items(data?.hourly.orEmpty()) { hour ->
                        Column(
                            Modifier
                                .width(64.dp)
                                .padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            IosText(hour.timeLabel, fontSize = 13.sp, color = Color(0xCCFFFFFF))
                            Spacer(Modifier.height(6.dp))
                            Icon(weatherIcon(hour.code), null, tint = Color.White, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.height(6.dp))
                            IosText("${hour.tempC}°", fontSize = 16.sp, color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            // 7-day forecast
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(14.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        IosText("城市", fontSize = 15.sp, color = Color(0xCCFFFFFF))
                        IosText("7日预报", fontSize = 15.sp, color = Color.White)
                    }
                    data?.daily?.forEach { day ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IosText(day.dateLabel, fontSize = 16.sp, color = Color.White, modifier = Modifier.width(56.dp))
                            Icon(weatherIcon(day.code), null, tint = Color(0xCCFFFFFF), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            IosText("${day.minC}°", fontSize = 16.sp, color = Color(0x99FFFFFF))
                            Box(Modifier.weight(1f).height(4.dp))
                            IosText("${day.maxC}°", fontSize = 16.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (pickingCity) {
        CityPickerDialog(
            source = source,
            onDismiss = { pickingCity = false },
            onPick = {
                city = it
                pickingCity = false
            },
        )
    }
}

@Composable
private fun CityPickerDialog(
    source: WeatherDataSource,
    onDismiss: () -> Unit,
    onPick: (City) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(DEFAULT_CITIES) }
    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C1C1E))
                .clickable(enabled = false) {} // swallow clicks
                .padding(16.dp),
        ) {
            IosText("选择城市", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            androidx.compose.material3.OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    scope.launch { results = runCatching { source.searchCity(it) }.getOrDefault(DEFAULT_CITIES) }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { IosText("输入城市名", color = Color(0x66EBEBF5), fontSize = 14.sp) },
                singleLine = true,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF0A84FF),
                ),
            )
            Spacer(Modifier.height(10.dp))
            results.take(8).forEach { c ->
                IosText(
                    c.name,
                    fontSize = 16.sp,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(c) }
                        .padding(vertical = 10.dp),
                )
            }
        }
    }
}
