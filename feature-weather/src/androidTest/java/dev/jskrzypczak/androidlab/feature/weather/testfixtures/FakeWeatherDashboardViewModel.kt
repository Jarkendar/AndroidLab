package dev.jskrzypczak.androidlab.feature.weather.testfixtures

import dev.jskrzypczak.androidlab.feature.weather.model.WeatherUiState
import dev.jskrzypczak.androidlab.feature.weather.viewmodel.WeatherDashboardViewModelContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeWeatherDashboardViewModel(initState: WeatherUiState): WeatherDashboardViewModelContract {

    private val _uiState = MutableStateFlow(initState)
    override val uiState: StateFlow<WeatherUiState> = _uiState

    override fun cancel() = Unit

    override fun refresh() = Unit
}