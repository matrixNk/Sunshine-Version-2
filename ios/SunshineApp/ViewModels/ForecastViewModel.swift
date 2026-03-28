// ForecastViewModel.swift
// Drives ForecastListView.  Mirrors ForecastFragment's role:
// loading forecasts, triggering sync, and detecting location changes.

import Foundation
import Combine
import CoreData
import CoreLocation

@MainActor
final class ForecastViewModel: ObservableObject {

    @Published var forecasts: [WeatherForecast] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var cityName: String = ""

    private let repository = WeatherRepository.shared
    private let settings   = AppSettings.shared
    private var cancellables = Set<AnyCancellable>()

    init() {
        // Re-load whenever the location setting changes (mirrors MainActivity.onResume)
        settings.$locationQuery
            .removeDuplicates()
            .sink { [weak self] _ in
                Task { await self?.refresh() }
            }
            .store(in: &cancellables)
    }

    // MARK: - Actions

    /// Load cached data immediately, then fetch fresh data from the network.
    func refresh() async {
        loadCachedForecasts()
        await syncFromNetwork()
    }

    func openInMaps() {
        guard let first = forecasts.first else { return }
        // Open via geo: URL — uses the location stored by the first forecast's location entity.
        // We look up coordinates from Core Data.
        let context = PersistenceController.shared.container.viewContext
        let req = NSFetchRequest<LocationEntity>(entityName: "LocationEntity")
        req.predicate = NSPredicate(
            format: "locationSetting == %@", first.locationSetting
        )
        req.fetchLimit = 1
        guard let loc = (try? context.fetch(req))?.first else { return }

        let urlString = "http://maps.apple.com/?ll=\(loc.coordLat),\(loc.coordLong)"
        if let url = URL(string: urlString) {
            UISharedApplication.shared.open(url)
        }
    }

    // MARK: - Private

    private func loadCachedForecasts() {
        forecasts = repository.forecasts(for: settings.locationQuery)
        loadCityName()
    }

    private func syncFromNetwork() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        await repository.syncWeather()
        loadCachedForecasts()
    }

    private func loadCityName() {
        let context = PersistenceController.shared.container.viewContext
        let req = NSFetchRequest<LocationEntity>(entityName: "LocationEntity")
        req.predicate = NSPredicate(format: "locationSetting == %@", settings.locationQuery)
        req.fetchLimit = 1
        cityName = (try? context.fetch(req))?.first?.cityName ?? settings.locationQuery
    }
}

// UISharedApplication thin wrapper to avoid importing UIKit in a SwiftUI ViewModel.
// UIApplication is accessible in iOS SwiftUI apps.
import UIKit
private typealias UISharedApplication = UIApplication
