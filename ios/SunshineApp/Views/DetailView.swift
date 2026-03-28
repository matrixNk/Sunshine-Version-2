// DetailView.swift
// Full-day weather detail screen.  Mirrors DetailFragment / fragment_detail.xml.
// Shows weather art, date, description, high/low, humidity, wind, and pressure.
// Includes a Share button matching the ShareActionProvider in the Android detail menu.

import SwiftUI

struct DetailView: View {

    let forecast: WeatherForecast
    @EnvironmentObject private var settings: AppSettings

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                headerSection
                temperatureSection
                Divider()
                detailSection
            }
            .padding()
        }
        .navigationTitle(Utility.getDayName(for: forecast.date))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                ShareLink(item: shareText) {
                    Label("Share", systemImage: "square.and.arrow.up")
                }
            }
        }
    }

    // MARK: - Sections

    private var headerSection: some View {
        VStack(spacing: 8) {
            // Large weather artwork (art_* drawables equivalent)
            Image(systemName: Utility.artSymbolName(for: forecast.weatherId))
                .resizable()
                .scaledToFit()
                .frame(width: 120, height: 120)
                .foregroundStyle(Utility.iconColor(for: forecast.weatherId))
                .accessibilityLabel(forecast.shortDesc)

            Text(Utility.getFriendlyDayString(for: forecast.date))
                .font(.headline)
                .foregroundStyle(.secondary)

            Text(Utility.getFormattedMonthDay(forecast.date))
                .font(.subheadline)
                .foregroundStyle(.secondary)

            Text(forecast.shortDesc)
                .font(.title2.weight(.medium))
        }
        .frame(maxWidth: .infinity)
    }

    private var temperatureSection: some View {
        HStack(spacing: 32) {
            VStack {
                Label(
                    Utility.formatTemperature(forecast.maxTemp, isMetric: settings.isMetric),
                    systemImage: "thermometer.high"
                )
                .font(.title.weight(.bold))
                Text("High")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            VStack {
                Label(
                    Utility.formatTemperature(forecast.minTemp, isMetric: settings.isMetric),
                    systemImage: "thermometer.low"
                )
                .font(.title2)
                .foregroundStyle(.secondary)
                Text("Low")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private var detailSection: some View {
        VStack(spacing: 16) {
            DetailRow(
                icon: "humidity.fill",
                label: "Humidity",
                value: String(format: "%.0f%%", forecast.humidity)
            )
            DetailRow(
                icon: "wind",
                label: "Wind",
                value: Utility.getFormattedWind(
                    speedMs: forecast.windSpeed,
                    degrees: forecast.degrees,
                    isMetric: settings.isMetric
                )
            )
            DetailRow(
                icon: "gauge.with.needle",
                label: "Pressure",
                value: String(format: "%.1f hPa", forecast.pressure)
            )
        }
    }

    // MARK: - Share text  (mirrors createShareForecastIntent + #SunshineApp hashtag)

    private var shareText: String {
        let date  = Utility.getFormattedMonthDay(forecast.date)
        let high  = Utility.formatTemperature(forecast.maxTemp, isMetric: settings.isMetric)
        let low   = Utility.formatTemperature(forecast.minTemp, isMetric: settings.isMetric)
        return "\(date) - \(forecast.shortDesc) - \(high)/\(low) #SunshineApp"
    }
}

// MARK: - Reusable detail row

private struct DetailRow: View {
    let icon: String
    let label: String
    let value: String

    var body: some View {
        HStack {
            Label(label, systemImage: icon)
                .foregroundStyle(.secondary)
                .frame(width: 120, alignment: .leading)
            Spacer()
            Text(value)
                .font(.body.monospacedDigit())
        }
    }
}

// MARK: - Preview

#Preview {
    NavigationStack {
        DetailView(forecast: WeatherForecast(
            id: UUID(),
            date: Date(),
            weatherId: 500,
            shortDesc: "Rain",
            maxTemp: 22,
            minTemp: 14,
            humidity: 80,
            pressure: 1008,
            windSpeed: 5.2,
            degrees: 270,
            locationSetting: "94043"
        ))
        .environmentObject(AppSettings.shared)
    }
}
