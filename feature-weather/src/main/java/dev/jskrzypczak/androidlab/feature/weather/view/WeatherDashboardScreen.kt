package dev.jskrzypczak.androidlab.feature.weather.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jskrzypczak.androidlab.feature.weather.model.AlertSeverity
import dev.jskrzypczak.androidlab.feature.weather.model.AlertsInfo
import dev.jskrzypczak.androidlab.feature.weather.model.CurrentConditions
import dev.jskrzypczak.androidlab.feature.weather.model.DayForecast
import dev.jskrzypczak.androidlab.feature.weather.model.WeatherAlert
import dev.jskrzypczak.androidlab.feature.weather.model.WeatherDashboard
import dev.jskrzypczak.androidlab.feature.weather.model.WeatherUiState
import dev.jskrzypczak.androidlab.feature.weather.viewmodel.WeatherDashboardViewModelContract
import java.time.Instant
import java.time.LocalDate

@Composable
fun WeatherDashboardScreen(viewModel: WeatherDashboardViewModelContract) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    when (state) {
        is WeatherUiState.Loading -> LoadingIndicator()
        is WeatherUiState.Success -> WeatherDashboardContent(state.dashboard)
        is WeatherUiState.Failed -> ErrorScreen(state.throwable, state.retryAction)
        is WeatherUiState.Cancelled -> CancelledScreen(state.reason)
    }
}

@Composable
fun WeatherDashboardContent(dashboard: WeatherDashboard) {
    LazyColumn {
        item {
            CurrentConditions(dashboard.currentConditions)
        }
        if (dashboard.forecast.isNotEmpty()) {
            item {
                Text(
                    modifier = Modifier.testTag("forecast_list"),
                    text = "Forecast"
                )
            }
            items(
                dashboard.forecast.size,
                key = { index -> dashboard.forecast[index].date }) { index ->

                ForecastItem(
                    dashboard.forecast[index], Modifier
                        .testTag("forecast_item_$index")
                        .semantics(mergeDescendants = true) {}
                )
            }
        }
        if (dashboard.alerts.hasActiveAlerts) {
            item {
                Text(
                    modifier = Modifier.testTag("alerts_section"),
                    text = "Alerts"
                )
            }
            items(dashboard.alerts.alerts.size, key = { index -> dashboard.alerts.alerts[index].title }) { index ->
                AlertItem(dashboard.alerts.alerts[index], Modifier
                    .testTag("alert_item_$index")
                    .semantics(mergeDescendants = true) {}
                )
            }
        }
    }
}

@Composable
fun AlertItem(weatherAlert: WeatherAlert, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = weatherAlert.title)
        Text(text = weatherAlert.description)
    }
}

@Composable
fun ForecastItem(forecast: DayForecast, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(color = MaterialTheme.colorScheme.inversePrimary)) {
        Text(text = forecast.date.toString())
        Row {
            Text(text = "Min Temp:")
            Text(text = "${forecast.minTempCelsius}°C")
        }
        Row {
            Text(text = "Max Temp:")
            Text(text = "${forecast.maxTempCelsius}°C")
        }
        Row {
            Text(text = "Precipitation:")
            Text(text = "${forecast.precipitationProbability}%")
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun CurrentConditions(currentConditions: CurrentConditions) {
    Row {
        Column(modifier = Modifier.testTag("current_temperature").semantics(mergeDescendants = true) {}) {
            Text(text = "Temperature:")
            Text(text = "${currentConditions.temperatureCelsius}°C")
        }
        Column(modifier = Modifier.testTag("current_description").semantics(mergeDescendants = true) {}) {
            Text(text = "Description:")
            Text(text = currentConditions.description)
        }
        Column(modifier = Modifier.testTag("humidity_value").semantics(mergeDescendants = true) {}) {
            Text(text = "Humidity:")
            Text(text = "${currentConditions.humidity}%")
        }
    }
}

@Composable
fun CancelledScreen(reason: String) {
    AlertDialog(
        modifier = Modifier.testTag("cancelled_dialog"),
        onDismissRequest = {},
        title = { Text("Cancelled") },
        text = { Text(
            modifier = Modifier.testTag("cancelled_message"),
            text = reason)
               },
        confirmButton = {
            Button(
                onClick = {}
            ) {
                Text("OK")
            }
        }
    )
}

@Composable
fun LoadingIndicator() {
    CircularProgressIndicator(
        modifier = Modifier.testTag("loading_indicator")
    )
}

@Composable
fun ErrorScreen(throwable: Throwable, retryAction: () -> Unit) {
    AlertDialog(
        modifier = Modifier.testTag("error_dialog"),
        onDismissRequest = {},
        title = { Text("Error") },
        text = { Text(
            modifier = Modifier.testTag("error_message"),
            text = throwable.message ?: "Unknown error")
               },
        confirmButton = {
            Button(
                modifier = Modifier.testTag("retry_button"),
                onClick = retryAction
            ) {
                Text("Retry")
            }
        },

    )
}

@Preview(showBackground = true)
@Composable
fun WeatherDashboardContentPreview() {
    MaterialTheme {
        Surface {
            WeatherDashboardContent(
                dashboard = WeatherDashboard(
                    currentConditions = CurrentConditions(
                        temperatureCelsius = 22.0,
                        feelsLikeCelsius = 24.0,
                        humidity = 65,
                        windSpeedKmh = 15.0,
                        description = "Partly Cloudy",
                        iconCode = "04d"
                    ),
                    forecast = listOf(
                        DayForecast(LocalDate.now(), 18.0, 26.0, 10, "Sunny", "01d"),
                        DayForecast(LocalDate.now().plusDays(1), 19.0, 27.0, 20, "Cloudy", "02d"),
                        DayForecast(LocalDate.now().plusDays(2), 17.0, 24.0, 80, "Rain", "09d")
                    ),
                    alerts = AlertsInfo(
                        hasActiveAlerts = true,
                        alerts = listOf(
                            WeatherAlert(
                                AlertSeverity.WARNING,
                                "Thunderstorm Warning",
                                "Severe thunderstorms expected in the afternoon.",
                                Instant.now().plusSeconds(3600)
                            )
                        )
                    )
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingIndicatorPreview() {
    MaterialTheme {
        Surface {
            LoadingIndicator()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorScreenPreview() {
    MaterialTheme {
        ErrorScreen(
            throwable = RuntimeException("Failed to fetch weather data"),
            retryAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CancelledScreenPreview() {
    MaterialTheme {
        CancelledScreen(reason = "Location permission denied")
    }
}
