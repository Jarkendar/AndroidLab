package dev.jskrzypczak.androidlab.feature.weather

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import dev.jskrzypczak.androidlab.feature.weather.model.WeatherUiState
import dev.jskrzypczak.androidlab.feature.weather.testfixtures.FakeWeatherRepository
import dev.jskrzypczak.androidlab.feature.weather.testfixtures.WeatherTestFixtures
import dev.jskrzypczak.androidlab.feature.weather.viewmodel.WeatherDashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherDataFlowIntegrationTest {

    private lateinit var repository: FakeWeatherRepository
    private lateinit var viewModel: WeatherDashboardViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeWeatherRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fake repository emits data, ViewModel exposes Success with matching data`() = runTest {
        val expectedDashboard = WeatherTestFixtures.sampleDashboard(
            currentConditions = WeatherTestFixtures.sampleCurrentConditions(
                temperatureCelsius = 22.5,
                description = "Sunny"
            )
        )
        
        repository.emit(expectedDashboard)
        viewModel = createViewModel("test-city")
        
        viewModel.uiState.test {
            assertEquals(WeatherUiState.Loading, awaitItem())
            val successState = awaitItem() as WeatherUiState.Success
            assertEquals(expectedDashboard, successState.dashboard)
            assertEquals(22.5, successState.dashboard.currentConditions.temperatureCelsius)
            assertEquals("Sunny", successState.dashboard.currentConditions.description)
        }
    }

    @Test
    fun `fake repository emits updated data, ViewModel reflects the update`() = runTest {
        val initialDashboard = WeatherTestFixtures.sampleDashboard(
            currentConditions = WeatherTestFixtures.sampleCurrentConditions(temperatureCelsius = 20.0)
        )
        val updatedDashboard = WeatherTestFixtures.sampleDashboard(
            currentConditions = WeatherTestFixtures.sampleCurrentConditions(temperatureCelsius = 25.0)
        )
        
        repository.emit(initialDashboard)
        viewModel = createViewModel("test-city")
        
        viewModel.uiState.test {
            assertEquals(WeatherUiState.Loading, awaitItem())
            val initialSuccess = awaitItem() as WeatherUiState.Success
            assertEquals(20.0, initialSuccess.dashboard.currentConditions.temperatureCelsius)
            
            repository.emit(updatedDashboard)
            val updatedSuccess = awaitItem() as WeatherUiState.Success
            assertEquals(25.0, updatedSuccess.dashboard.currentConditions.temperatureCelsius)
        }
    }

    @Test
    fun `repository throws, ViewModel exposes Failed, retry triggers Success`() = runTest {
        val testException = RuntimeException("Connection timeout")
        val recoveryDashboard = WeatherTestFixtures.sampleDashboard()
        
        repository.shouldThrow = true
        repository.throwableToEmit = testException
        viewModel = createViewModel("test-city")
        
        viewModel.uiState.test {
            assertEquals(WeatherUiState.Loading, awaitItem())
            val failedState = awaitItem() as WeatherUiState.Failed
            assertEquals(testException, failedState.throwable)
            
            // Fix repository and retry
            repository.shouldThrow = false
            repository.emit(recoveryDashboard)
            failedState.retryAction()
            
            assertEquals(WeatherUiState.Loading, awaitItem())
            val successState = awaitItem() as WeatherUiState.Success
            assertEquals(recoveryDashboard, successState.dashboard)
        }
    }

    @Test
    fun `cancel mid-loading reaches Cancelled state, no further emissions propagated`() = runTest {
        repository.delayMillis = 1000 // Simulate slow loading
        val dashboardAfterCancel = WeatherTestFixtures.sampleDashboard()
        
        viewModel = createViewModel("test-city")
        
        viewModel.uiState.test {
            assertEquals(WeatherUiState.Loading, awaitItem())
            
            // Cancel while loading
            viewModel.cancel()
            val cancelledState = awaitItem() as WeatherUiState.Cancelled
            assertTrue(cancelledState.reason.isNotEmpty())
            
            // Emit data after cancel - should not propagate
            repository.emit(dashboardAfterCancel)
            advanceTimeBy(2000)
            
            // State should remain Cancelled
            expectNoEvents()
        }
    }

    @Test
    fun `rapid sequential city switches, only latest city data is active`() = runTest {
        val city1Dashboard = WeatherTestFixtures.sampleDashboard(
            currentConditions = WeatherTestFixtures.sampleCurrentConditions(temperatureCelsius = 15.0)
        )
        val city2Dashboard = WeatherTestFixtures.sampleDashboard(
            currentConditions = WeatherTestFixtures.sampleCurrentConditions(temperatureCelsius = 25.0)
        )
        val city3Dashboard = WeatherTestFixtures.sampleDashboard(
            currentConditions = WeatherTestFixtures.sampleCurrentConditions(temperatureCelsius = 35.0)
        )

        repository.cityDataMap["city1"] = city1Dashboard
        repository.cityDataMap["city2"] = city2Dashboard
        repository.cityDataMap["city3"] = city3Dashboard

        // First city
        viewModel = createViewModel("city1")
        val job1 = viewModel.uiState.launchIn(this)
        advanceUntilIdle()
        assertEquals(15.0, (viewModel.uiState.value as WeatherUiState.Success).dashboard.currentConditions.temperatureCelsius)
        job1.cancel()

        // Rapid city switches
        updateCityId("city2")
        updateCityId("city3")
        val job3 = viewModel.uiState.launchIn(this)
        advanceUntilIdle()

        // Only latest city's data
        val finalState = viewModel.uiState.value as WeatherUiState.Success
        assertEquals(35.0, finalState.dashboard.currentConditions.temperatureCelsius)
        job3.cancel()
    }
    @Test
    fun `full pipeline with forecast and alerts data integrity`() = runTest {
        val forecastList = WeatherTestFixtures.sampleForecastList(days = 5)
        val alertsInfo = WeatherTestFixtures.sampleAlertInfo(alertCount = 2)
        val completeDashboard = WeatherTestFixtures.sampleDashboard(
            forecast = forecastList,
            alerts = alertsInfo
        )
        
        repository.emit(completeDashboard)
        viewModel = createViewModel("complete-city")
        
        viewModel.uiState.test {
            assertEquals(WeatherUiState.Loading, awaitItem())
            val successState = awaitItem() as WeatherUiState.Success
            
            // Verify forecast data integrity
            assertEquals(5, successState.dashboard.forecast.size)
            assertEquals(forecastList, successState.dashboard.forecast)
            
            // Verify alerts data integrity
            assertTrue(successState.dashboard.alerts.hasActiveAlerts)
            assertEquals(2, successState.dashboard.alerts.alerts.size)
            assertEquals(alertsInfo, successState.dashboard.alerts)
        }
    }

    private fun createViewModel(cityId: String): WeatherDashboardViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("cityId" to cityId))
        return WeatherDashboardViewModel(repository, savedStateHandle)
    }

    private fun updateCityId(newCityId: String) {
        val newHandle = SavedStateHandle(mapOf("cityId" to newCityId))
        viewModel = WeatherDashboardViewModel(repository, newHandle)
    }
}
