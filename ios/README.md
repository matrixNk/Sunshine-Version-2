# Sunshine — iOS (Swift / SwiftUI)

iOS port of the [Udacity Sunshine Android app](../README.md).

## Requirements

| Tool | Version |
|------|---------|
| Xcode | 15+ |
| iOS deployment target | 16.0+ |
| Swift | 5.9+ |

## Setup

### 1. Create the Xcode project

1. Open Xcode → **File → New → Project → App**.
2. Set:
   - **Product Name**: `SunshineApp`
   - **Bundle Identifier**: `com.example.sunshine`
   - **Interface**: SwiftUI
   - **Language**: Swift
   - **Storage**: None *(Core Data is configured programmatically)*
3. Delete the auto-generated `ContentView.swift` and drag all files from `ios/SunshineApp/` into the project navigator.

### 2. Configure the API key

Create a file `Config.xcconfig` (do **not** commit it) with:

```
OPEN_WEATHER_MAP_API_KEY = your_key_here
```

Assign it to your target's build configuration in **Project → Info → Configurations**.

Alternatively, hard-code it temporarily in `WeatherAPI.swift`:

```swift
static var apiKey: String { "your_key_here" }
```

Get a free key at [openweathermap.org](https://openweathermap.org/appid).

### 3. Enable Background App Refresh capability

In Xcode → target → **Signing & Capabilities** → **+ Capability** → **Background Modes**:
- ✅ Background fetch
- ✅ Background processing

### 4. Run

Build and run on a simulator or device.  Enter a postal code or city in **Settings** to see weather for your location.

---

## Architecture

```
ios/SunshineApp/
├── SunshineApp.swift            # @main — background task registration
├── ContentView.swift            # Root scene wrapper
│
├── Models/
│   └── WeatherModels.swift      # Display structs + Codable API models
│
├── Data/
│   ├── PersistenceController.swift  # Core Data stack (programmatic model)
│   ├── WeatherAPI.swift             # OpenWeatherMap URLSession client
│   └── WeatherRepository.swift     # Repository: API + Core Data
│
├── ViewModels/
│   └── ForecastViewModel.swift  # ObservableObject driving the list
│
├── Views/
│   ├── ForecastListView.swift   # 14-day list (NavigationSplitView)
│   ├── ForecastRowView.swift    # Today row + future-day row cells
│   ├── DetailView.swift         # Full-day detail + Share button
│   └── SettingsView.swift       # Location / units / notifications
│
└── Utilities/
    ├── AppSettings.swift        # UserDefaults wrapper (ObservableObject)
    ├── Utility.swift            # Date, temperature, wind, SF Symbol helpers
    └── NotificationManager.swift # UNUserNotificationCenter wrapper
```

## Android → iOS mapping

| Android | iOS |
|---------|-----|
| `SunshineSyncAdapter` | `WeatherRepository.syncWeather()` + `BGAppRefreshTask` |
| `WeatherProvider` (ContentProvider) | `WeatherRepository` + Core Data |
| `WeatherDbHelper` | `PersistenceController` (programmatic `NSManagedObjectModel`) |
| `WeatherContract` | `WeatherModels.swift` + `PersistenceController` entity definitions |
| `ForecastFragment` / `ForecastAdapter` | `ForecastListView` + `ForecastRowView` |
| `DetailFragment` | `DetailView` |
| `SettingsActivity` | `SettingsView` |
| `Utility.kt` | `Utility.swift` |
| `SharedPreferences` | `AppSettings` (`UserDefaults`) |
| `NotificationCompat` | `UNUserNotificationCenter` |
| Two-pane tablet layout | `NavigationSplitView` (automatic on iPad) |
