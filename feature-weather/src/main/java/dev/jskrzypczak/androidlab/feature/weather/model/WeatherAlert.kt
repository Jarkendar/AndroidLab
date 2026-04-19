package dev.jskrzypczak.androidlab.feature.weather.model

import java.time.Instant


data class WeatherAlert(
    val severity: AlertSeverity,
    val title: String,
    val description: String,
    val expiresAt: Instant
)
