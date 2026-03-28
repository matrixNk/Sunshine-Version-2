// Utility.swift
// Date formatting, temperature conversion, wind formatting, and weather icon/art mapping.
// Direct port of Utility.kt.

import Foundation
import SwiftUI

struct Utility {

    // MARK: - Temperature

    /// Format a temperature value for display, converting to Fahrenheit if the user prefers imperial.
    /// The API always returns Celsius (metric).
    static func formatTemperature(_ celsius: Double, isMetric: Bool) -> String {
        let value = isMetric ? celsius : celsius * 1.8 + 32
        let unit  = isMetric ? "°C" : "°F"
        return String(format: "%.0f%@", value, unit)
    }

    // MARK: - Date

    static func getFriendlyDayString(for date: Date) -> String {
        let calendar = Calendar.current
        if calendar.isDateInToday(date) {
            let monthDay = getFormattedMonthDay(date)
            return "Today, \(monthDay)"
        } else if calendar.isDateInTomorrow(date) {
            return "Tomorrow"
        } else if let daysAhead = calendar.dateComponents([.day], from: Date(), to: date).day,
                  daysAhead < 7 {
            return dayName(for: date)
        } else {
            let fmt = DateFormatter()
            fmt.dateFormat = "EEE MMM d"
            return fmt.string(from: date)
        }
    }

    static func getDayName(for date: Date) -> String {
        let calendar = Calendar.current
        if calendar.isDateInToday(date)     { return "Today" }
        if calendar.isDateInTomorrow(date)  { return "Tomorrow" }
        return dayName(for: date)
    }

    static func getFormattedMonthDay(_ date: Date) -> String {
        let fmt = DateFormatter()
        fmt.dateFormat = "MMMM d"
        return fmt.string(from: date)
    }

    private static func dayName(for date: Date) -> String {
        let fmt = DateFormatter()
        fmt.dateFormat = "EEEE"
        return fmt.string(from: date)
    }

    // MARK: - Wind

    /// Format wind speed and direction into a human-readable string.
    /// API returns speed in m/s; convert to km/h (metric) or mph (imperial).
    static func getFormattedWind(speedMs: Double, degrees: Double, isMetric: Bool) -> String {
        let speed: Double
        let unit: String
        if isMetric {
            speed = speedMs * 3.6   // m/s → km/h
            unit  = "km/h"
        } else {
            speed = speedMs * 2.237 // m/s → mph
            unit  = "mph"
        }
        let direction = compassDirection(degrees: degrees)
        return String(format: "%.1f %@ %@", speed, unit, direction)
    }

    private static func compassDirection(degrees: Double) -> String {
        switch degrees {
        case 337.5..<360, 0..<22.5:  return "N"
        case 22.5..<67.5:            return "NE"
        case 67.5..<112.5:           return "E"
        case 112.5..<157.5:          return "SE"
        case 157.5..<202.5:          return "S"
        case 202.5..<247.5:          return "SW"
        case 247.5..<292.5:          return "W"
        case 292.5..<337.5:          return "NW"
        default:                     return "N"
        }
    }

    // MARK: - Weather Icons (SF Symbols, mapped from OWM weather IDs)

    /// Returns an SF Symbol name for the compact weather icon (list rows).
    static func iconName(for weatherId: Int) -> String {
        switch weatherId {
        case 200...232: return "cloud.bolt.rain.fill"
        case 300...321: return "cloud.drizzle.fill"
        case 500...504: return "cloud.rain.fill"
        case 511:       return "cloud.sleet.fill"
        case 520...531: return "cloud.heavyrain.fill"
        case 600...622: return "cloud.snow.fill"
        case 701...761: return "cloud.fog.fill"
        case 761, 781:  return "tornado"
        case 800:       return "sun.max.fill"
        case 801:       return "cloud.sun.fill"
        case 802...804: return "cloud.fill"
        default:        return "questionmark.circle"
        }
    }

    /// Returns an SF Symbol name for the large weather artwork (today row + detail screen).
    static func artSymbolName(for weatherId: Int) -> String {
        switch weatherId {
        case 200...232: return "cloud.bolt.rain"
        case 300...321: return "cloud.drizzle"
        case 500...504: return "cloud.rain"
        case 511:       return "cloud.sleet"
        case 520...531: return "cloud.heavyrain"
        case 600...622: return "cloud.snow"
        case 701...761: return "cloud.fog"
        case 761, 781:  return "tornado"
        case 800:       return "sun.max"
        case 801:       return "cloud.sun"
        case 802...804: return "cloud"
        default:        return "questionmark.circle"
        }
    }

    /// Accent color for the weather icon (approximates the original drawable tints).
    static func iconColor(for weatherId: Int) -> Color {
        switch weatherId {
        case 200...232: return .purple
        case 300...321: return .blue
        case 500...531: return .blue
        case 600...622: return .cyan
        case 700...781: return .gray
        case 800:       return .yellow
        case 801...804: return .gray
        default:        return .secondary
        }
    }
}
