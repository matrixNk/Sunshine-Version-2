// WeatherRepository.swift
// Combines the API client with Core Data persistence.
// Equivalent to SunshineSyncAdapter.getWeatherDataFromJson() + addLocation() + notifyWeather().

import CoreData
import Foundation

@MainActor
final class WeatherRepository: ObservableObject {

    static let shared = WeatherRepository()

    private let persistence = PersistenceController.shared

    // MARK: - Sync

    /// Fetch fresh weather from the API, persist it, remove stale records, and post a notification.
    func syncWeather() async {
        let location = AppSettings.shared.locationQuery
        do {
            let response = try await WeatherAPI.fetchForecast(for: location)
            await save(response: response, locationSetting: location)
            await deleteStaleForecasts()
            await NotificationManager.shared.scheduleWeatherNotification()
        } catch {
            print("WeatherRepository.syncWeather error: \(error.localizedDescription)")
        }
    }

    // MARK: - Read

    /// Return all stored forecasts for the current location, sorted ascending by date.
    func forecasts(for locationSetting: String) -> [WeatherForecast] {
        let context = persistence.container.viewContext
        let request = NSFetchRequest<WeatherEntity>(entityName: "WeatherEntity")
        request.predicate = NSPredicate(
            format: "location.locationSetting == %@ AND date >= %@",
            locationSetting,
            Calendar.current.startOfDay(for: Date()) as NSDate
        )
        request.sortDescriptors = [NSSortDescriptor(key: "date", ascending: true)]

        return (try? context.fetch(request))?.map { $0.toForecast() } ?? []
    }

    /// Return the forecast for a specific date and location.
    func forecast(for date: Date, locationSetting: String) -> WeatherForecast? {
        let context = persistence.container.viewContext
        let request = NSFetchRequest<WeatherEntity>(entityName: "WeatherEntity")
        let dayStart = Calendar.current.startOfDay(for: date)
        let dayEnd   = Calendar.current.date(byAdding: .day, value: 1, to: dayStart)!
        request.predicate = NSPredicate(
            format: "location.locationSetting == %@ AND date >= %@ AND date < %@",
            locationSetting,
            dayStart as NSDate,
            dayEnd   as NSDate
        )
        request.fetchLimit = 1

        return (try? context.fetch(request))?.first?.toForecast()
    }

    // MARK: - Private helpers

    private func save(response: OWMForecastResponse, locationSetting: String) async {
        let bgContext = persistence.newBackgroundContext()
        await bgContext.perform {
            let location = self.findOrCreateLocation(
                setting: locationSetting,
                cityName: response.city.name,
                lat: response.city.coord.lat,
                lon: response.city.coord.lon,
                in: bgContext
            )

            let calendar = Calendar.current
            let today = calendar.startOfDay(for: Date())

            for (index, day) in response.list.enumerated() {
                guard let dayDate = calendar.date(
                    byAdding: .day, value: index, to: today
                ) else { continue }

                // Upsert: update existing record if one exists for this (location, date).
                let entity = self.findOrCreateWeather(
                    date: dayDate, location: location, in: bgContext
                )
                entity.date      = dayDate
                entity.weatherId = Int32(day.weather.first?.id ?? 0)
                entity.shortDesc = day.weather.first?.main ?? ""
                entity.maxTemp   = day.temp.max
                entity.minTemp   = day.temp.min
                entity.humidity  = Double(day.humidity)
                entity.pressure  = day.pressure
                entity.windSpeed = day.speed
                entity.degrees   = day.deg
                entity.location  = location
            }

            try? bgContext.save()
        }
    }

    private func findOrCreateLocation(
        setting: String,
        cityName: String,
        lat: Double,
        lon: Double,
        in context: NSManagedObjectContext
    ) -> LocationEntity {
        let request = NSFetchRequest<LocationEntity>(entityName: "LocationEntity")
        request.predicate = NSPredicate(format: "locationSetting == %@", setting)
        request.fetchLimit = 1

        if let existing = (try? context.fetch(request))?.first {
            return existing
        }

        let entity = LocationEntity(context: context)
        entity.locationSetting = setting
        entity.cityName        = cityName
        entity.coordLat        = lat
        entity.coordLong       = lon
        return entity
    }

    private func findOrCreateWeather(
        date: Date,
        location: LocationEntity,
        in context: NSManagedObjectContext
    ) -> WeatherEntity {
        let request = NSFetchRequest<WeatherEntity>(entityName: "WeatherEntity")
        let dayEnd = Calendar.current.date(byAdding: .day, value: 1, to: date)!
        request.predicate = NSPredicate(
            format: "location == %@ AND date >= %@ AND date < %@",
            location, date as NSDate, dayEnd as NSDate
        )
        request.fetchLimit = 1

        if let existing = (try? context.fetch(request))?.first {
            return existing
        }
        return WeatherEntity(context: context)
    }

    /// Delete all weather records with a date before today.
    private func deleteStaleForecasts() async {
        let bgContext = persistence.newBackgroundContext()
        await bgContext.perform {
            let request = NSFetchRequest<WeatherEntity>(entityName: "WeatherEntity")
            let today = Calendar.current.startOfDay(for: Date())
            request.predicate = NSPredicate(format: "date < %@", today as NSDate)

            let stale = (try? bgContext.fetch(request)) ?? []
            stale.forEach { bgContext.delete($0) }
            try? bgContext.save()
        }
    }
}
