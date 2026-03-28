// ContentView.swift
// Root view — injects shared environment objects into the view hierarchy.

import SwiftUI

struct ContentView: View {

    @StateObject private var settings = AppSettings.shared

    var body: some View {
        ForecastListView()
            .environmentObject(settings)
    }
}

#Preview {
    ContentView()
        .environment(\.managedObjectContext,
                     PersistenceController.shared.container.viewContext)
}
