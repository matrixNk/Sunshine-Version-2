// NotificationManager.swift
// Posts a once-per-day weather notification when the app syncs.
// Mirrors SunshineSyncAdapter.notifyWeather().

import Foundation
import UserNotifications

@MainActor
final class NotificationManager {

    static let shared = NotificationManager()
    private let center = UNUserNotificationCenter.current()

    private let notificationIdentifier = "com.example.sunshine.daily-weather"

    // MARK: - Permissions

    func requestPermission() async {
        do {
            try await center.requestAuthorization(options: [.alert, .sound, .badge])
        } catch {
            print("NotificationManager: permission request failed: \(error)")
        }
    }

    // MARK: - Schedule

    /// Post today's weather notification if:
    ///   1. The user has enabled notifications in settings.
    ///   2. We haven't already shown one today.
    ///   3. There is weather data for today.
    func scheduleWeatherNotification() async {
        let settings = AppSettings.shared
        guard settings.enableNotifications else { return }

        // Only once per day
        if let last = settings.lastNotificationDate,
           Calendar.current.isDateInToday(last) { return }

        let repository = WeatherRepository.shared
        guard let today = repository.forecasts(for: settings.locationQuery).first,
              Calendar.current.isDateInToday(today.date) else { return }

        let isMetric = settings.isMetric
        let high = Utility.formatTemperature(today.maxTemp, isMetric: isMetric)
        let low  = Utility.formatTemperature(today.minTemp, isMetric: isMetric)

        let content = UNMutableNotificationContent()
        content.title = "Sunshine"
        content.body  = "\(today.shortDesc) — High: \(high)  Low: \(low)"
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: notificationIdentifier,
            content: content,
            trigger: nil    // deliver immediately
        )

        do {
            try await center.add(request)
            settings.lastNotificationDate = Date()
        } catch {
            print("NotificationManager: failed to schedule notification: \(error)")
        }
    }
}
