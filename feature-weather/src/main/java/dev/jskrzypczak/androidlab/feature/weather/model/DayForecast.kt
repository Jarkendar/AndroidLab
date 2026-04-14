package dev.jskrzypczak.androidlab.feature.weather.model

import java.time.LocalDate

data class DayForecast(
    val date: LocalDate,
    val minTempCelsius: Double,
    val maxTempCelsius: Double,
    val precipitationProbability: Int,
    val description: String,
    val iconCode: String
)
