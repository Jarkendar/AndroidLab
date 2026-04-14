package dev.jskrzypczak.androidlab.feature.weather.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jskrzypczak.androidlab.feature.weather.model.WeatherDashboard
import dev.jskrzypczak.androidlab.feature.weather.model.WeatherRepository
import dev.jskrzypczak.androidlab.feature.weather.model.WeatherUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

class WeatherDashboardViewModel(
    val repository: WeatherRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _isCancelled = MutableStateFlow(false)
    private val cityId: String = savedStateHandle.get<String>("cityId") ?: ""
    private val cancelFlow = MutableStateFlow<WeatherUiState?>(null)

    private val retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val uiState: StateFlow<WeatherUiState> = merge(
        cancelFlow.filterNotNull(),
        retryTrigger
            .onStart { emit(Unit) }
            .flatMapLatest {
                if (_isCancelled.value) {
                    emptyFlow()
                } else {
                    repository.observeWeather(cityId)
                        .takeWhile { !_isCancelled.value }
                        .map<WeatherDashboard, WeatherUiState> { weatherDashboard -> WeatherUiState.Success(weatherDashboard) }
                        .onStart { emit(WeatherUiState.Loading) }
                        .catch { exception ->
                            emit(WeatherUiState.Failed(exception) {
                                retryTrigger.tryEmit(Unit)
                            })
                        }
                }
            }
    ).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        WeatherUiState.Loading
    )

    public override fun onCleared() {
        //only change visibility
    }

    fun cancel() {
        _isCancelled.tryEmit(true)
        cancelFlow.value = WeatherUiState.Cancelled("Cancelled by user")
        retryTrigger.tryEmit(Unit)
    }

    fun refresh() {
        viewModelScope.launch { repository.refresh(cityId) }
    }
}