// WeatherModels.swift
// Display-layer and API-response data models for the Sunshine weather app.

import Foundation

// MARK: - Display Models

/// A single day's weather forecast used throughout the UI.
struct WeatherForecast: Identifiable, Equatable {
    let id: UUID
    let date: Date
    let weatherId: Int
    let shortDesc: String
    let maxTemp: Double
    let minTemp: Double
    let humidity: Double
    let pressure: Double
    let windSpeed: Double
    let degrees: Double
    let locationSetting: String
}

/// A saved location entry.
struct WeatherLocation: Equatable {
    let locationSetting: String
    let cityName: String
    let coordLat: Double
    let coordLong: Double
}

// MARK: - OpenWeatherMap API Response Models

struct OWMForecastResponse: Codable {
    let city: OWMCity
    let list: [OWMDayForecast]
}

struct OWMCity: Codable {
    let name: String
    let coord: OWMCoord
}

struct OWMCoord: Codable {
    let lat: Double
    let lon: Double
}

struct OWMDayForecast: Codable {
    let temp: OWMTemp
    let pressure: Double
    let humidity: Int
    let speed: Double     // wind speed in m/s (metric)
    let deg: Double       // wind direction in degrees
    let weather: [OWMWeather]
}

struct OWMTemp: Codable {
    let max: Double
    let min: Double
}

struct OWMWeather: Codable {
    let id: Int
    let main: String      // short description e.g. "Clear"
}
