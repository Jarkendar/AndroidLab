# Feature: Weather Dashboard 🌦️

A learning module exploring **reactive state management** in Android using StateFlow, Kotlin Coroutines, and Jetpack Compose.

## What This Module Covers

- **Sealed class UI state** — modeling screen states (`Loading`, `Success`, `Failed`, `Cancelled`) as a sealed hierarchy
- **Declarative StateFlow pipeline** — building ViewModel state from composed Flow operators instead of imperative `MutableStateFlow.value = ...`
- **`stateIn` with `WhileSubscribed`** — lifecycle-aware sharing that stops upstream collection when no one is observing
- **`flatMapLatest` for restart/retry** — using a trigger-based pattern to restart data collection without managing Jobs manually
- **`merge` for multiple state sources** — combining data flow and cancellation flow into a single StateFlow
- **Compose UI driven by sealed state** — rendering different screens per state with `collectAsStateWithLifecycle()`
- **Test-first development** — all tests were written before implementation, defining the expected API surface

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Compose UI                        │
│          collectAsStateWithLifecycle()               │
└──────────────────────┬──────────────────────────────┘
                       │ StateFlow<WeatherUiState>
┌──────────────────────┴──────────────────────────────┐
│              WeatherDashboardViewModel               │
│                                                      │
│  merge(                                              │
│    cancelFlow,                                       │
│    retryTrigger                                      │
│      .onStart { emit(Unit) }                         │
│      .flatMapLatest {                                │
│          repository.observeWeather(cityId)           │
│            .map { Success(it) }                      │
│            .onStart { emit(Loading) }                │
│            .catch { emit(Failed(it) { retry }) }     │
│      }                                               │
│  ).stateIn(WhileSubscribed(5_000))                   │
└──────────────────────┬──────────────────────────────┘
                       │ Flow<WeatherDashboard>
┌──────────────────────┴──────────────────────────────┐
│              WeatherRepository (interface)            │
│  observeWeather(cityId): Flow<WeatherDashboard>      │
│  suspend refresh(cityId)                             │
└─────────────────────────────────────────────────────┘
```

## Data Model

```
WeatherUiState (sealed class)
├── Loading                  — initial / retry state
├── Success(dashboard)       — data ready to display
├── Failed(throwable, retry) — error with retry lambda
└── Cancelled(reason)        — user-initiated cancellation

WeatherDashboard (data class)
├── currentConditions: CurrentConditions
│   ├── temperatureCelsius, feelsLikeCelsius
│   ├── humidity, windSpeedKmh
│   └── description, iconCode
├── forecast: List<DayForecast>
│   ├── date, minTempCelsius, maxTempCelsius
│   ├── precipitationProbability
│   └── description, iconCode
└── alerts: AlertsInfo
    ├── hasActiveAlerts
    └── alerts: List<WeatherAlert>
        ├── severity (ADVISORY, WATCH, WARNING)
        ├── title, description
        └── expiresAt
```

## Key Patterns Learned

### Trigger-based retry with `flatMapLatest`

Instead of managing coroutine Jobs manually, a `MutableSharedFlow<Unit>` acts as a restart signal. Each emission cancels the previous collection and starts a fresh one:

```kotlin
private val retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

retryTrigger
    .onStart { emit(Unit) }        // auto-start on first subscribe
    .flatMapLatest {               // cancel previous, start new
        repository.observeWeather(cityId)
            .catch { e -> emit(Failed(e) { retryTrigger.tryEmit(Unit) }) }
    }
```

### Cancellation via `merge`

A separate `MutableStateFlow` provides the `Cancelled` state through `merge`, keeping the main pipeline clean:

```kotlin
merge(
    cancelFlow.filterNotNull(),    // cancellation source
    retryTrigger.flatMapLatest {}  // data source
).stateIn(...)
```

### `WhileSubscribed(5_000)` lifecycle awareness

The upstream Flow stays active for 5 seconds after the last subscriber disconnects (survives configuration changes like rotation), then stops to save resources.

## Test Coverage

| Test File | Type | Tests | What It Verifies |
|---|---|---|---|
| `WeatherUiStateStructureTest` | Unit | 14 | Sealed class structure, data class properties, enum values |
| `WeatherDashboardViewModelTest` | Unit | 11 | State transitions, retry, cancel, refresh, WhileSubscribed behavior |
| `WeatherDataFlowIntegrationTest` | Unit | 7 | Full pipeline: fake repo → real ViewModel → state assertions |
| `WeatherDashboardScreenTest` | Instrumented | 11 | Compose UI rendering per state (in progress) |

**Total: 31 unit tests passing, 11 instrumented tests in progress**

## Module Structure

```
feature-weather/
├── src/main/java/.../weather/
│   ├── model/
│   │   ├── WeatherModels.kt          — data classes, sealed class, enum
│   │   └── WeatherRepository.kt      — repository interface
│   └── viewmodel/
│       └── WeatherDashboardViewModel.kt
├── src/test/java/.../weather/
│   ├── WeatherUiStateStructureTest.kt
│   ├── WeatherDashboardViewModelTest.kt
│   ├── WeatherDataFlowIntegrationTest.kt
│   └── testfixtures/
│       ├── WeatherTestFixtures.kt
│       └── FakeWeatherRepository.kt
└── src/androidTest/java/.../weather/
    ├── WeatherDashboardScreenTest.kt
    └── testfixtures/
        ├── WeatherTestFixtures.kt
        └── FakeWeatherRepository.kt
```

## Status

- [x] Data model (sealed class, data classes, enum)
- [x] Repository interface
- [x] ViewModel with declarative StateFlow pipeline
- [x] Unit tests (31/31 passing)
- [ ] Compose UI (`WeatherDashboardScreen`)
- [ ] Instrumented Compose tests
- [ ] Koin DI module
- [ ] Navigation integration