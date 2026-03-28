// ForecastListView.swift
// Main 14-day forecast list.  Mirrors ForecastFragment + ForecastAdapter.
// On iPad the NavigationSplitView gives the two-pane master/detail layout
// equivalent to the tablet layout in activity_main-sw600dp.xml.

import SwiftUI

struct ForecastListView: View {

    @StateObject private var viewModel = ForecastViewModel()
    @EnvironmentObject  private var settings: AppSettings

    @State private var selectedForecast: WeatherForecast?
    @State private var showingSettings  = false

    var body: some View {
        NavigationSplitView {
            listContent
                .navigationTitle(viewModel.cityName.isEmpty ? "Sunshine" : viewModel.cityName)
                .toolbar { toolbarItems }
                .refreshable { await viewModel.refresh() }
                .task { await viewModel.refresh() }
        } detail: {
            if let forecast = selectedForecast {
                DetailView(forecast: forecast)
            } else {
                Text("Select a day to see details")
                    .foregroundStyle(.secondary)
            }
        }
        .sheet(isPresented: $showingSettings) {
            SettingsView()
        }
    }

    // MARK: - List content

    @ViewBuilder
    private var listContent: some View {
        if viewModel.isLoading && viewModel.forecasts.isEmpty {
            ProgressView("Loading weather…")
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if viewModel.forecasts.isEmpty {
            ContentUnavailableView(
                "No Forecast",
                systemImage: "cloud.slash",
                description: Text("Pull to refresh or check your location in Settings.")
            )
        } else {
            List(selection: $selectedForecast) {
                ForEach(Array(viewModel.forecasts.enumerated()), id: \.element.id) { index, forecast in
                    NavigationLink(value: forecast) {
                        if index == 0 {
                            TodayForecastRow(forecast: forecast, isMetric: settings.isMetric)
                        } else {
                            FutureForecastRow(forecast: forecast, isMetric: settings.isMetric)
                        }
                    }
                    .tag(forecast)
                }
            }
            .listStyle(.plain)
            // Error banner
            .overlay(alignment: .bottom) {
                if let error = viewModel.errorMessage {
                    Text(error)
                        .font(.caption)
                        .padding(8)
                        .background(.red.opacity(0.85), in: RoundedRectangle(cornerRadius: 8))
                        .foregroundStyle(.white)
                        .padding()
                        .transition(.move(edge: .bottom))
                }
            }
        }
    }

    // MARK: - Toolbar  (mirrors forecastfragment menu: map + main menu: settings)

    @ToolbarContentBuilder
    private var toolbarItems: some ToolbarContent {
        ToolbarItem(placement: .navigationBarTrailing) {
            Button {
                showingSettings = true
            } label: {
                Label("Settings", systemImage: "gearshape")
            }
        }
        ToolbarItem(placement: .navigationBarTrailing) {
            Button {
                viewModel.openInMaps()
            } label: {
                Label("Show on Map", systemImage: "map")
            }
            .disabled(viewModel.forecasts.isEmpty)
        }
    }
}

#Preview {
    ForecastListView()
        .environmentObject(AppSettings.shared)
        .environment(\.managedObjectContext,
                     PersistenceController.shared.container.viewContext)
}
