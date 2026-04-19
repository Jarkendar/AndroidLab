package dev.jskrzypczak.androidlab.feature.weather.model

data class WeatherDashboard(
    val currentConditions: CurrentConditions,
    val forecast: List<DayForecast>,
    val alerts: AlertsInfo
)