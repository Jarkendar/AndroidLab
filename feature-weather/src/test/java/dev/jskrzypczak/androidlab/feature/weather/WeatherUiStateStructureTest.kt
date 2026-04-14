package dev.jskrzypczak.androidlab.feature.weather

import dev.jskrzypczak.androidlab.feature.weather.model.AlertSeverity
import dev.jskrzypczak.androidlab.feature.weather.model.AlertsInfo
import dev.jskrzypczak.androidlab.feature.weather.model.CurrentConditions
import dev.jskrzypczak.androidlab.feature.weather.model.DayForecast
import dev.jskrzypczak.androidlab.feature.weather.model.WeatherAlert
import dev.jskrzypczak.androidlab.feature.weather.model.WeatherDashboard
import dev.jskrzypczak.androidlab.feature.weather.model.WeatherUiState
import kotlin.test.Test
import kotlin.test.*
import java.time.LocalDate
import java.time.Instant

class WeatherUiStateStructureTest {

    @Test
    fun `WeatherUiState is sealed interface with exactly 4 subtypes`() {
        val sealedSubclasses = WeatherUiState::class.sealedSubclasses
        assertEquals(4, sealedSubclasses.size)
        
        val expectedSubtypes = setOf("Loading", "Success", "Failed", "Cancelled")
        val actualSubtypes = sealedSubclasses.map { it.simpleName }.toSet()
        assertEquals(expectedSubtypes, actualSubtypes)
    }

    @Test
    fun `Loading is an object singleton`() {
        val loadingClass = WeatherUiState::class.sealedSubclasses
            .first { it.simpleName == "Loading" }
        assertTrue(loadingClass.objectInstance != null)
    }

    @Test
    fun `Success is data class holding WeatherDashboard`() {
        val successClass = WeatherUiState::class.sealedSubclasses
            .first { it.simpleName == "Success" }
        assertTrue(successClass.isData)
        
        val constructor = successClass.constructors.first()
        assertEquals(1, constructor.parameters.size)
        assertEquals("dashboard", constructor.parameters.first().name)
    }

    @Test
    fun `Failed is data class with throwable and retryAction`() {
        val failedClass = WeatherUiState::class.sealedSubclasses
            .first { it.simpleName == "Failed" }
        assertTrue(failedClass.isData)
        
        val constructor = failedClass.constructors.first()
        assertEquals(2, constructor.parameters.size)
        
        val paramNames = constructor.parameters.map { it.name }.toSet()
        assertTrue(paramNames.contains("throwable"))
        assertTrue(paramNames.contains("retryAction"))
    }

    @Test
    fun `Cancelled is data class with reason string`() {
        val cancelledClass = WeatherUiState::class.sealedSubclasses
            .first { it.simpleName == "Cancelled" }
        assertTrue(cancelledClass.isData)
        
        val constructor = cancelledClass.constructors.first()
        assertEquals(1, constructor.parameters.size)
        assertEquals("reason", constructor.parameters.first().name)
    }

    @Test
    fun `WeatherDashboard contains required non-null properties`() {
        val dashboard = WeatherDashboard(
            CurrentConditions(.0, .0, 34, 1.0, "description", "iconCode"),
            listOf(),
            AlertsInfo(true, listOf())
        )
        
        assertNotNull(dashboard.currentConditions)
        assertNotNull(dashboard.forecast)
        assertNotNull(dashboard.alerts)
    }

    @Test
    fun `CurrentConditions is data class with correct properties`() {
        val currentConditions = CurrentConditions(
            .0,
            -0.5,
            50,
            20.0,
            "description",
            "iconCode"
        )
        
        assertTrue(currentConditions.temperatureCelsius is Double)
        assertTrue(currentConditions.feelsLikeCelsius is Double)
        assertTrue(currentConditions.humidity is Int)
        assertTrue(currentConditions.windSpeedKmh is Double)
        assertTrue(currentConditions.description is String)
        assertTrue(currentConditions.iconCode is String)
    }

    @Test
    fun `DayForecast is data class with correct properties`() {
        val dayForecast = DayForecast(
            LocalDate.now(),
            .0,
            1.0,
            1,
            "test",
            "iconCodeTest")
        
        assertTrue(dayForecast.date is LocalDate)
        assertTrue(dayForecast.minTempCelsius is Double)
        assertTrue(dayForecast.maxTempCelsius is Double)
        assertTrue(dayForecast.precipitationProbability is Int)
        assertTrue(dayForecast.description is String)
        assertTrue(dayForecast.iconCode is String)
    }

    @Test
    fun `AlertsInfo is data class with correct properties`() {
        val alertsInfo = AlertsInfo(
            true,
            listOf()
        )
        
        assertTrue(alertsInfo.hasActiveAlerts is Boolean)
        assertTrue(alertsInfo.alerts is List<*>)
    }

    @Test
    fun `WeatherAlert is data class with correct properties`() {
        val weatherAlert = WeatherAlert(
            AlertSeverity.WATCH,
            "Title",
            "description",
            Instant.now())
        
        assertTrue(weatherAlert.severity is AlertSeverity)
        assertTrue(weatherAlert.title is String)
        assertTrue(weatherAlert.description is String)
        assertTrue(weatherAlert.expiresAt is Instant)
    }

    @Test
    fun `AlertSeverity enum has correct values`() {
        val values = AlertSeverity.entries.toTypedArray()
        assertEquals(3, values.size)
        
        val expectedValues = setOf(AlertSeverity.ADVISORY, AlertSeverity.WATCH, AlertSeverity.WARNING)
        assertEquals(expectedValues, values.toSet())
    }

    @Test
    fun `data class copy works correctly for immutability`() {
        val original = CurrentConditions(.0, 50.0, 50, 20.0,"test", "iconCode")
        val copied = original.copy(temperatureCelsius = 25.0)
        
        assertNotEquals(original.temperatureCelsius, copied.temperatureCelsius)
        assertEquals(original.humidity, copied.humidity)
        assertEquals(original.description, copied.description)
    }

    @Test
    fun `humidity values are within valid range 0 to 100`() {
        val currentConditions = CurrentConditions(.0, .0, 85, .0,"description", "iconCode")
        assertTrue(currentConditions.humidity in 0..100)
    }

    @Test
    fun `precipitation probability is within valid range 0 to 100`() {
        val dayForecast = DayForecast(LocalDate.now(), .0, .0, 60, "description", "iconCode")
        assertTrue(dayForecast.precipitationProbability in 0..100)
    }
}
