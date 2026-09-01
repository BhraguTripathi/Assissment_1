package com.example.assissment_1

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assissment_1.ui.theme.Assissment_1Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class WeatherData(
    val city: String,
    val temperature: Double,
    val condition: String,
    val humidity: Int,
    val description: String
)

private object WeatherRepository {
    private const val OPEN_WEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5/weather"

    suspend fun fetchWeather(city: String): WeatherData = withContext(Dispatchers.IO) {
        val normalizedCity = city.trim().ifBlank { "New York" }
        val apiKey = BuildConfig.OPENWEATHER_API_KEY

        if (apiKey.isBlank()) {
            throw IllegalStateException("OpenWeather API key is missing. Add OPENWEATHER_API_KEY in local.properties")
        }

        val encodedCity = Uri.encode(normalizedCity)
        val requestUrl = "$OPEN_WEATHER_BASE_URL?q=$encodedCity&appid=$apiKey&units=metric"
        val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
        }

        try {
            val responseCode = connection.responseCode
            val responseBody = (if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            })?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (responseCode !in 200..299) {
                val apiMessage = runCatching {
                    JSONObject(responseBody).optString("message")
                }.getOrNull().orEmpty()
                val message = apiMessage.ifBlank { "HTTP $responseCode" }
                throw IOException("Failed to fetch weather: $message")
            }

            val json = JSONObject(responseBody)
            val weatherObject = json.getJSONArray("weather").getJSONObject(0)
            val mainObject = json.getJSONObject("main")

            WeatherData(
                city = json.optString("name").ifBlank { normalizedCity },
                temperature = mainObject.getDouble("temp"),
                condition = weatherObject.optString("main").ifBlank { "Unknown" },
                humidity = mainObject.getInt("humidity"),
                description = weatherObject
                    .optString("description")
                    .replaceFirstChar { ch ->
                        if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                    }
            )
        } finally {
            connection.disconnect()
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Assissment_1Theme {
                WeatherApp()
            }
        }
    }
}

@Composable
fun WeatherApp() {
    var cityInput by rememberSaveable { mutableStateOf("Delhi") }
    var searchCity by rememberSaveable { mutableStateOf("Delhi") }
    var reloadCount by rememberSaveable { mutableIntStateOf(0) }
    var weather by remember { mutableStateOf<WeatherData?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(searchCity, reloadCount) {
        isLoading = true
        errorMessage = null

        try {
            weather = WeatherRepository.fetchWeather(searchCity)
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unable to fetch weather data"
        } finally {
            isLoading = false
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E3A8A),
                            Color(0xFF0F172A),
                            Color(0xFF0B1120)
                        )
                    )
                )
                .padding(innerPadding)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "Weather App",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = cityInput,
                    onValueChange = { cityInput = it },
                    label = { Text("City name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            searchCity = cityInput
                            reloadCount += 1
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF60A5FA))
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Search")
                    }

                    OutlinedButton(
                        onClick = { reloadCount += 1 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Reload")
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFF93C5FD))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Loading weather...",
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    errorMessage != null -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D))
                        ) {
                            Text(
                                text = errorMessage ?: "Something went wrong",
                                color = Color.White,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }

                    weather != null -> {
                        WeatherCard(weather = weather!!)
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherCard(weather: WeatherData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = weather.city,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${weather.temperature}°C",
                color = Color(0xFFFDE68A),
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = weather.condition,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = weather.description,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.18f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Humidity",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${weather.humidity}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}
