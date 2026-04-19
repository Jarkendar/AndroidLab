package dev.jskrzypczak.androidlab.feature.weather.model

sealed class WeatherUiState {
    data class Success(val dashboard: WeatherDashboard): WeatherUiState()
    object Loading: WeatherUiState()
    data class Failed(val throwable: Throwable, val retryAction: () -> Unit): WeatherUiState()
    data class Cancelled(val reason: String): WeatherUiState()

}