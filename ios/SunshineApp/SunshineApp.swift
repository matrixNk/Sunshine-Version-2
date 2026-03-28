// SunshineApp.swift
// App entry point.  Handles:
//   • Core Data container injection
//   • Background fetch registration (mirrors SunshineSyncAdapter periodic sync every 3 hours)
//   • Initial notification permission request

import SwiftUI
import BackgroundTasks

@main
struct SunshineApp: App {

    private let persistence = PersistenceController.shared

    // Background task identifier — must also be declared in Info.plist under
    // "Permitted background task scheduler identifiers".
    private static let bgRefreshID = "com.example.sunshine.weather-refresh"

    init() {
        registerBackgroundTasks()
    }

    // MARK: - Scene

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.managedObjectContext, persistence.container.viewContext)
                .task {
                    // Request notification permission on first launch
                    await NotificationManager.shared.requestPermission()
                }
        }
    }

    // MARK: - Background Fetch (replaces SunshineSyncAdapter + configurePeriodicSync)

    private func registerBackgroundTasks() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: Self.bgRefreshID,
            using: nil
        ) { task in
            guard let refreshTask = task as? BGAppRefreshTask else { return }
            self.handleBackgroundRefresh(task: refreshTask)
        }
    }

    private func handleBackgroundRefresh(task: BGAppRefreshTask) {
        // Schedule the next refresh before doing work (mirrors configurePeriodicSync)
        scheduleNextBackgroundRefresh()

        let syncTask = Task {
            await WeatherRepository.shared.syncWeather()
            task.setTaskCompleted(success: true)
        }

        task.expirationHandler = {
            syncTask.cancel()
            task.setTaskCompleted(success: false)
        }
    }

    /// Schedule the next background refresh ~3 hours from now.
    static func scheduleNextBackgroundRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: bgRefreshID)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 3 * 60 * 60)
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            print("SunshineApp: failed to schedule background refresh: \(error)")
        }
    }
}
