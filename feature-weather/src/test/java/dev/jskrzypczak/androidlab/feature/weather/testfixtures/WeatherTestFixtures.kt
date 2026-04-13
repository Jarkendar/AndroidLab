package dev.jskrzypczak.androidlab.feature.weather.testfixtures

import java.time.LocalDate
import java.time.Instant

object WeatherTestFixtures {

    fun sampleCurrentConditions(
        temperatureCelsius: Double = 22.0,
        feelsLikeCelsius: Double = 24.0,
        humidity: Int = 65,
        windSpeedKmh: Double = 15.5,
        description: String = "Partly Cloudy",
        iconCode: String = "04d"
    ): CurrentConditions {
        return TODO("Create CurrentConditions with provided parameters")
    }

    fun sampleDayForecast(
        dayOffset: Int = 0,
        minTempCelsius: Double = 18.0,
        maxTempCelsius: Double = 26.0,
        precipitationProbability: Int = 30,
        description: String = "Mostly Sunny",
        iconCode: String = "02d"
    ): DayForecast {
        return TODO("Create DayForecast with date = LocalDate.now().plusDays(dayOffset) and other parameters")
    }

    fun sampleForecastList(days: Int = 7): List<DayForecast> {
        return (0 until days).map { dayOffset ->
            sampleDayForecast(
                dayOffset = dayOffset,
                minTempCelsius = 15.0 + dayOffset,
                maxTempCelsius = 25.0 + dayOffset,
                precipitationProbability = (dayOffset * 10) % 100,
                description = "Day $dayOffset forecast",
                iconCode = if (dayOffset % 2 == 0) "01d" else "02d"
            )
        }
    }

    fun sampleWeatherAlert(
        severity: AlertSeverity = AlertSeverity.ADVISORY,
        title: String = "Weather Advisory",
        description: String = "Moderate weather conditions expected",
        expiresAt: Instant = Instant.now().plusSeconds(3600) // 1 hour from now
    ): WeatherAlert {
        return TODO("Create WeatherAlert with provided parameters")
    }

    fun sampleAlertInfo(
        alertCount: Int = 0,
        hasActiveAlerts: Boolean = alertCount > 0
    ): AlertsInfo {
        val alerts = if (alertCount > 0) {
            (1..alertCount).map { index ->
                sampleWeatherAlert(
                    severity = when (index % 3) {
                        0 -> AlertSeverity.ADVISORY
                        1 -> AlertSeverity.WATCH
                        else -> AlertSeverity.WARNING
                    },
                    title = "Alert $index",
                    description = "Description for alert $index"
                )
            }
        } else {
            emptyList()
        }
        
        return TODO("Create AlertsInfo with hasActiveAlerts and alerts list")
    }

    fun sampleDashboard(
        currentConditions: CurrentConditions = sampleCurrentConditions(),
        forecast: List<DayForecast> = sampleForecastList(),
        alerts: AlertsInfo = sampleAlertInfo()
    ): WeatherDashboard {
        return TODO("Create WeatherDashboard with provided parameters")
    }

    // Additional helper methods for specific test scenarios
    fun sampleDashboardWithAlerts(): WeatherDashboard {
        return sampleDashboard(
            alerts = sampleAlertInfo(alertCount = 3)
        )
    }

    fun sampleDashboardWithoutAlerts(): WeatherDashboard {
        return sampleDashboard(
            alerts = sampleAlertInfo(alertCount = 0)
        )
    }

    fun sampleDashboardWithExtremeForecast(): WeatherDashboard {
        val extremeForecast = listOf(
            sampleDayForecast(0, -10.0, -5.0, 90, "Heavy Snow", "13d"),
            sampleDayForecast(1, 35.0, 42.0, 0, "Extreme Heat", "01d"),
            sampleDayForecast(2, 20.0, 25.0, 100, "Thunderstorms", "11d")
        )
        
        return sampleDashboard(forecast = extremeForecast)
    }
}
