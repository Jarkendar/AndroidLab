package dev.jskrzypczak.androidlab.feature.weather.model

data class AlertsInfo(
    val hasActiveAlerts: Boolean,
    val alerts: List<WeatherAlert>
)
