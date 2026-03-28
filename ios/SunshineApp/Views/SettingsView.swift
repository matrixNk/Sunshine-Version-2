// SettingsView.swift
// User-facing settings screen.  Mirrors SettingsActivity / pref_general.xml.
// Three settings: location query, temperature units, and notification toggle.

import SwiftUI

struct SettingsView: View {

    @EnvironmentObject private var settings: AppSettings
    @Environment(\.dismiss) private var dismiss

    // Local editing state
    @State private var locationDraft = ""
    @State private var showingLocationAlert = false

    var body: some View {
        NavigationStack {
            Form {
                // ── Location ────────────────────────────────────────────
                Section {
                    HStack {
                        Text("Location")
                        Spacer()
                        Text(settings.locationQuery)
                            .foregroundStyle(.secondary)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        locationDraft = settings.locationQuery
                        showingLocationAlert = true
                    }
                } header: {
                    Text("Location")
                } footer: {
                    Text("Enter a postal code (e.g. 94043) or city name (e.g. London, UK).")
                }

                // ── Temperature Units ────────────────────────────────────
                Section("Temperature Units") {
                    Picker("Units", selection: $settings.units) {
                        Text("Celsius (°C)").tag("metric")
                        Text("Fahrenheit (°F)").tag("imperial")
                    }
                    .pickerStyle(.segmented)
                }

                // ── Notifications ────────────────────────────────────────
                Section {
                    Toggle("Weather Notifications", isOn: $settings.enableNotifications)
                        .onChange(of: settings.enableNotifications) { _, enabled in
                            if enabled {
                                Task {
                                    await NotificationManager.shared.requestPermission()
                                }
                            }
                        }
                } footer: {
                    Text("Receive a daily notification with today's weather forecast.")
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
            .alert("Location", isPresented: $showingLocationAlert) {
                TextField("Postal code or city", text: $locationDraft)
                    .autocorrectionDisabled()
                Button("Save") {
                    let trimmed = locationDraft.trimmingCharacters(in: .whitespaces)
                    if !trimmed.isEmpty {
                        settings.locationQuery = trimmed
                    }
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Enter a postal code or city name.")
            }
        }
    }
}

#Preview {
    SettingsView()
        .environmentObject(AppSettings.shared)
}
