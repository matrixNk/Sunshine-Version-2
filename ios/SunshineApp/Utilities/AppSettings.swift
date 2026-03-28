// AppSettings.swift
// Wraps UserDefaults preferences — equivalent to Android SharedPreferences in SettingsActivity.
// Published properties automatically update any SwiftUI view that observes this object.

import Foundation
import Combine

final class AppSettings: ObservableObject {

    static let shared = AppSettings()

    // MARK: - Keys (match Android pref keys for clarity)

    private enum Keys {
        static let location           = "location"
        static let units              = "units"
        static let enableNotifications = "enable_notifications"
        static let lastNotification   = "last_notification"
    }

    // MARK: - Published properties

    /// Location query sent to OpenWeatherMap (postal code or city name).
    @Published var locationQuery: String {
        didSet { UserDefaults.standard.set(locationQuery, forKey: Keys.location) }
    }

    /// "metric" → Celsius  |  "imperial" → Fahrenheit
    @Published var units: String {
        didSet { UserDefaults.standard.set(units, forKey: Keys.units) }
    }

    @Published var enableNotifications: Bool {
        didSet { UserDefaults.standard.set(enableNotifications, forKey: Keys.enableNotifications) }
    }

    // MARK: - Non-published preferences

    var lastNotificationDate: Date? {
        get {
            let ts = UserDefaults.standard.double(forKey: Keys.lastNotification)
            return ts == 0 ? nil : Date(timeIntervalSince1970: ts)
        }
        set {
            UserDefaults.standard.set(
                newValue?.timeIntervalSince1970 ?? 0,
                forKey: Keys.lastNotification
            )
        }
    }

    var isMetric: Bool { units == "metric" }

    // MARK: - Init

    private init() {
        let defaults = UserDefaults.standard
        // Register factory defaults (mirrors pref_general.xml defaults)
        defaults.register(defaults: [
            Keys.location:            "94043",
            Keys.units:               "metric",
            Keys.enableNotifications: true
        ])

        locationQuery       = defaults.string(forKey: Keys.location) ?? "94043"
        units               = defaults.string(forKey: Keys.units)    ?? "metric"
        enableNotifications = defaults.bool(forKey: Keys.enableNotifications)
    }
}
