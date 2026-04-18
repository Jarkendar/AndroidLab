package dev.jskrzypczak.androidlab.feature.weather.viewmodel

import dev.jskrzypczak.androidlab.feature.weather.model.WeatherUiState
import kotlinx.coroutines.flow.StateFlow

interface WeatherDashboardViewModelContract {
    val uiState: StateFlow<WeatherUiState>
    fun cancel()
    fun refresh()
}