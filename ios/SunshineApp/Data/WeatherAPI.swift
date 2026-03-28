// WeatherAPI.swift
// Fetches 14-day forecast from the OpenWeatherMap daily forecast endpoint.
// Mirrors the network call in SunshineSyncAdapter.onPerformSync().

import Foundation

enum WeatherAPIError: LocalizedError {
    case invalidURL
    case httpError(Int)
    case decodingError(Error)
    case noData

    var errorDescription: String? {
        switch self {
        case .invalidURL:          return "Invalid request URL."
        case .httpError(let code): return "Server returned HTTP \(code)."
        case .decodingError(let e): return "Response parse error: \(e.localizedDescription)"
        case .noData:              return "No data received from server."
        }
    }
}

struct WeatherAPI {

    // Replace with your OpenWeatherMap API key, or inject via an xcconfig / Info.plist entry.
    static var apiKey: String {
        Bundle.main.object(forInfoDictionaryKey: "OPEN_WEATHER_MAP_API_KEY") as? String
            ?? "YOUR_API_KEY_HERE"
    }

    private static let baseURL = "https://api.openweathermap.org/data/2.5/forecast/daily"

    /// Fetch 14-day daily forecast for the given location query (postal code or city name).
    /// Always requests metric units from the API; temperature conversion is done in the UI.
    static func fetchForecast(for locationQuery: String) async throws -> OWMForecastResponse {
        var components = URLComponents(string: baseURL)
        components?.queryItems = [
            URLQueryItem(name: "q",    value: locationQuery),
            URLQueryItem(name: "mode", value: "json"),
            URLQueryItem(name: "units", value: "metric"),
            URLQueryItem(name: "cnt",  value: "14"),
            URLQueryItem(name: "APPID", value: apiKey)
        ]

        guard let url = components?.url else { throw WeatherAPIError.invalidURL }

        let (data, response) = try await URLSession.shared.data(from: url)

        if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
            throw WeatherAPIError.httpError(http.statusCode)
        }

        do {
            return try JSONDecoder().decode(OWMForecastResponse.self, from: data)
        } catch {
            throw WeatherAPIError.decodingError(error)
        }
    }
}
