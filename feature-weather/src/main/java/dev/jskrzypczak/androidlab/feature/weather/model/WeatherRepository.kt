package dev.jskrzypczak.androidlab.feature.weather.model

import kotlinx.coroutines.flow.Flow

interface WeatherRepository {

    fun observeWeather(cityId: String): Flow<WeatherDashboard>
    suspend fun refresh(cityId: String)
}