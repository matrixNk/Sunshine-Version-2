// ForecastRowView.swift
// Two list-row styles that mirror list_item_forecast_today.xml and list_item_forecast.xml.

import SwiftUI

// MARK: - Today Row (prominent, large icon)

struct TodayForecastRow: View {

    let forecast: WeatherForecast
    let isMetric: Bool

    var body: some View {
        HStack(spacing: 16) {
            // Large weather art (mirrors art_* drawables)
            Image(systemName: Utility.artSymbolName(for: forecast.weatherId))
                .resizable()
                .scaledToFit()
                .frame(width: 72, height: 72)
                .foregroundStyle(Utility.iconColor(for: forecast.weatherId))
                .accessibilityLabel(forecast.shortDesc)

            VStack(alignment: .leading, spacing: 4) {
                Text(Utility.getFriendlyDayString(for: forecast.date))
                    .font(.title3.weight(.semibold))
                Text(forecast.shortDesc)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                HStack(spacing: 12) {
                    Label(Utility.formatTemperature(forecast.maxTemp, isMetric: isMetric),
                          systemImage: "thermometer.high")
                        .font(.headline)
                    Label(Utility.formatTemperature(forecast.minTemp, isMetric: isMetric),
                          systemImage: "thermometer.low")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer()
        }
        .padding(.vertical, 8)
    }
}

// MARK: - Future Day Row (compact)

struct FutureForecastRow: View {

    let forecast: WeatherForecast
    let isMetric: Bool

    var body: some View {
        HStack(spacing: 12) {
            // Small icon
            Image(systemName: Utility.iconName(for: forecast.weatherId))
                .resizable()
                .scaledToFit()
                .frame(width: 32, height: 32)
                .foregroundStyle(Utility.iconColor(for: forecast.weatherId))
                .accessibilityLabel(forecast.shortDesc)

            VStack(alignment: .leading, spacing: 2) {
                Text(Utility.getFriendlyDayString(for: forecast.date))
                    .font(.subheadline.weight(.medium))
                Text(forecast.shortDesc)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 2) {
                Text(Utility.formatTemperature(forecast.maxTemp, isMetric: isMetric))
                    .font(.subheadline.weight(.medium))
                Text(Utility.formatTemperature(forecast.minTemp, isMetric: isMetric))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Preview

#Preview {
    let sample = WeatherForecast(
        id: UUID(),
        date: Date(),
        weatherId: 800,
        shortDesc: "Clear",
        maxTemp: 28,
        minTemp: 16,
        humidity: 55,
        pressure: 1013,
        windSpeed: 3.5,
        degrees: 180,
        locationSetting: "94043"
    )
    return List {
        TodayForecastRow(forecast: sample, isMetric: true)
        FutureForecastRow(forecast: sample, isMetric: true)
    }
}
