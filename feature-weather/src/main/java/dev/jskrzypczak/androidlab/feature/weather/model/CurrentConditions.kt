package dev.jskrzypczak.androidlab.feature.weather.model

data class CurrentConditions(
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double,
    val humidity: Int,
    val windSpeedKmh: Double,
    val description: String,
    val iconCode: String
)
