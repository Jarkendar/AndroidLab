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
class WeatherDashboardViewModelTest {

    private lateinit var repository: FakeWeatherRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: WeatherDashboardViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeWeatherRepository()
        savedStateHandle = SavedStateHandle(mapOf("cityId" to "test-city-123"))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        viewModel = createWeatherDashboardViewModel()
        
        assertEquals(WeatherUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `successful repository emission transitions to Success state`() = runTest {
        val sampleDashboard = WeatherTestFixtures.sampleDashboard()
        repository.emit(sampleDashboard)
        
        viewModel = createWeatherDashboardViewModel()
        
        viewModel.uiState.test {
            assertEquals(WeatherUiState.Loading, awaitItem())
            val successState = awaitItem() as WeatherUiState.Success
            assertEquals(sampleDashboard, successState.dashboard)
        }
    }

    @Test
    fun `repository exception transitions to Failed state`() = runTest {
        val testException = RuntimeException("Network error")
        repository.shouldThrow = true
        repository.throwableToEmit = testException
        
        viewModel = createWeatherDashboardViewModel()
        
        viewModel.uiState.test {
            assertEquals(WeatherUiState.Loading, awaitItem())
            val failedState = awaitItem() as WeatherUiState.Failed
            assertEquals(testException, failedState.throwable)
            assertNotNull(failedState.retryAction)
        }
    }

    @Test
    fun `retry action in Failed state re-collects flow through Loading to Success`() = runTest {
        val testException = RuntimeException("Network error")
        val sampleDashboard = WeatherTestFixtures.sampleDashboard()
        
        repository.shouldThrow = true
        repository.throwableToEmit = testException
        
        viewModel = createWeatherDashboardViewModel()
        
        viewModel.uiState.test {
            assertEquals(WeatherUiState.Loading, awaitItem())
            val failedState = awaitItem() as WeatherUiState.Failed
            
            // Fix repository and retry
            repository.shouldThrow = false
            repository.emit(sampleDashboard)
            failedState.retryAction()
            
            assertEquals(WeatherUiState.Loading, awaitItem())
            val successState = awaitItem() as WeatherUiState.Success
            assertEquals(sampleDashboard, successState.dashboard)
        }
    }

    @Test
    fun `refresh triggers repository refresh and re-emits data`() = runTest {
        val initialDashboard = WeatherTestFixtures.sampleDashboard()
        val updatedDashboard = WeatherTestFixtures.sampleDashboard(
            currentConditions = WeatherTestFixtures.sampleCurrentConditions(temperatureCelsius = 30.0)
        )
        
        repository.emit(initialDashboard)
        viewModel = createWeatherDashboardViewModel()
        
        viewModel.uiState.test {
            assertEquals(WeatherUiState.Loading, awaitItem())
            val initialSuccess = awaitItem() as WeatherUiState.Success
            assertEquals(initialDashboard, initialSuccess.dashboard)
            
            repository.emit(updatedDashboard)
            viewModel.refresh()
            
            val updatedSuccess = awaitItem() as WeatherUiState.Success
            assertEquals(updatedDashboard, updatedSuccess.dashboard)
            assertEquals(30.0, updatedSuccess.dashboard.currentConditions.temperatureCelsius)
        }
    }

    @Test
    fun `cancel transitions state to Cancelled with reason`() = runTest {
        viewModel = createWeatherDashboardViewModel()
        
        viewModel.uiState.test {
            assertEquals(WeatherUiState.Loading, awaitItem())
            
            viewModel.cancel()
            
            val cancelledState = awaitItem() as WeatherUiState.Cancelled
            assertTrue(cancelledState.reason.isNotEmpty())
        }
    }

    @Test
    fun `after cancel repository flow collection job is cancelled`() = runTest {
        repository.delayMillis = 1000
        viewModel = createWeatherDashboardViewModel()

        val job = viewModel.uiState.launchIn(this)
        advanceUntilIdle()

        viewModel.cancel()
        advanceUntilIdle()

        repository.emit(WeatherTestFixtures.sampleDashboard())
        advanceTimeBy(2000)

        assertTrue(viewModel.uiState.value is WeatherUiState.Cancelled)
        job.cancel()
    }

    @Test
    fun `multiple rapid emissions only expose latest state`() = runTest {
        val dashboard1 = WeatherTestFixtures.sampleDashboard()
        val dashboard2 = WeatherTestFixtures.sampleDashboard(
            currentConditions = WeatherTestFixtures.sampleCurrentConditions(temperatureCelsius = 25.0)
        )
        val dashboard3 = WeatherTestFixtures.sampleDashboard(
            currentConditions = WeatherTestFixtures.sampleCurrentConditions(temperatureCelsius = 30.0)
        )
        
        viewModel = createWeatherDashboardViewModel()
        
        viewModel.uiState.test {
            assertEquals(WeatherUiState.Loading, awaitItem())
            
            // Rapid emissions
            repository.emit(dashboard1)
            repository.emit(dashboard2)
            repository.emit(dashboard3)
            
            val finalState = awaitItem() as WeatherUiState.Success
            assertEquals(30.0, finalState.dashboard.currentConditions.temperatureCelsius)
        }
    }

    @Test
    fun `cityId is correctly read from SavedStateHandle`() = runTest {
        val customCityId = "custom-city-456"
        savedStateHandle = SavedStateHandle(mapOf("cityId" to customCityId))

        viewModel = createWeatherDashboardViewModel()

        val job = viewModel.uiState.launchIn(this)
        advanceUntilIdle()
        // Verify repository was called with correct cityId
        assertEquals(customCityId, repository.lastObservedCityId)
        job.cancel()
    }

    @Test
    fun `StateFlow uses WhileSubscribed with 5000ms timeout`() = runTest {
        viewModel = createWeatherDashboardViewModel()

        // Subscribe to uiState
        val job = viewModel.uiState.launchIn(this)
        advanceUntilIdle()
        assertEquals(1, repository.activeSubscriptionCount.value)

        // Unsubscribe
        job.cancel()

        // Before timeout — still subscribed upstream
        advanceTimeBy(4_999)
        assertEquals(1, repository.activeSubscriptionCount.value)

        // After timeout — upstream stopped
        advanceTimeBy(2)
        assertEquals(0, repository.activeSubscriptionCount.value)
    }

    @Test
    fun `no coroutine leaks after ViewModel onCleared`() = runTest {
        viewModel = createWeatherDashboardViewModel()
        
        // Start collection
        repository.emit(WeatherTestFixtures.sampleDashboard())
        advanceUntilIdle()

        val expected = viewModel.uiState.value
        // Clear ViewModel
        viewModel.onCleared()
        advanceUntilIdle()

        assertEquals(0, repository.activeSubscriptionCount.value)

        repository.emit(WeatherTestFixtures.sampleDashboard(WeatherTestFixtures.sampleCurrentConditions(temperatureCelsius = 99.0)))
        advanceUntilIdle()

        assertEquals(expected, viewModel.uiState.value)
    }

    private fun createWeatherDashboardViewModel(): WeatherDashboardViewModel {
        return WeatherDashboardViewModel(repository, savedStateHandle)
    }
}
