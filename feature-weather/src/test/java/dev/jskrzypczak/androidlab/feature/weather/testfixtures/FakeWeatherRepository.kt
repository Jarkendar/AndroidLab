package dev.jskrzypczak.androidlab.feature.weather.testfixtures

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow

class FakeWeatherRepository : WeatherRepository {
    
    var shouldThrow: Boolean = false
    var throwableToEmit: Throwable = RuntimeException("Test exception")
    var delayMillis: Long = 0
    var lastObservedCityId: String? = null
    var lastRefreshedCityId: String? = null
    
    // For testing multiple cities
    val cityDataMap = mutableMapOf<String, WeatherDashboard>()
    
    private val _weatherFlow = MutableSharedFlow<WeatherDashboard>(replay = 1)
    
    fun emit(dashboard: WeatherDashboard) {
        _weatherFlow.tryEmit(dashboard)
    }
    
    override fun observeWeather(cityId: String): Flow<WeatherDashboard> {
        lastObservedCityId = cityId
        
        return flow {
            if (delayMillis > 0) {
                delay(delayMillis)
            }
            
            if (shouldThrow) {
                throw throwableToEmit
            }
            
            // Check if we have specific data for this city
            val citySpecificData = cityDataMap[cityId]
            if (citySpecificData != null) {
                emit(citySpecificData)
            } else {
                // Collect from shared flow for general testing
                _weatherFlow.collect { dashboard ->
                    emit(dashboard)
                }
            }
        }
    }
    
    override suspend fun refresh(cityId: String) {
        lastRefreshedCityId = cityId
        
        if (delayMillis > 0) {
            delay(delayMillis)
        }
        
        if (shouldThrow) {
            throw throwableToEmit
        }
        
        // Simulate refresh by re-emitting current data
        val currentData = cityDataMap[cityId] ?: _weatherFlow.replayCache.lastOrNull()
        currentData?.let { emit(it) }
    }
    
    // Helper methods for testing
    fun reset() {
        shouldThrow = false
        delayMillis = 0
        lastObservedCityId = null
        lastRefreshedCityId = null
        cityDataMap.clear()
        _weatherFlow.resetReplayCache()
    }
    
    fun simulateNetworkError() {
        shouldThrow = true
        throwableToEmit = RuntimeException("Network connection failed")
    }
    
    fun simulateSlowNetwork(delayMs: Long = 2000) {
        delayMillis = delayMs
    }
    
    fun addCityData(cityId: String, dashboard: WeatherDashboard) {
        cityDataMap[cityId] = dashboard
    }
}
