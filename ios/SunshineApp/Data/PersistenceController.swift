// PersistenceController.swift
// Core Data stack created programmatically (no .xcdatamodeld file required).
// Mirrors the two-table SQLite schema from WeatherContract / WeatherDbHelper.

import CoreData
import Foundation

final class PersistenceController {

    static let shared = PersistenceController()

    let container: NSPersistentContainer

    // MARK: - Init

    init(inMemory: Bool = false) {
        container = NSPersistentContainer(
            name: "Sunshine",
            managedObjectModel: Self.makeModel()
        )

        if inMemory {
            container.persistentStoreDescriptions.first?.url = URL(fileURLWithPath: "/dev/null")
        }

        container.loadPersistentStores { _, error in
            if let error {
                fatalError("Core Data store failed to load: \(error)")
            }
        }
        container.viewContext.automaticallyMergesChangesFromParent = true
    }

    // MARK: - Programmatic NSManagedObjectModel

    private static func makeModel() -> NSManagedObjectModel {
        let model = NSManagedObjectModel()

        // ── Location entity ────────────────────────────────────────────────
        let locationEntity = NSEntityDescription()
        locationEntity.name = "LocationEntity"
        locationEntity.managedObjectClassName = NSStringFromClass(LocationEntity.self)

        let locSetting = makeAttribute("locationSetting", type: .stringAttributeType)
        let cityName   = makeAttribute("cityName",        type: .stringAttributeType)
        let coordLat   = makeAttribute("coordLat",        type: .doubleAttributeType)
        let coordLong  = makeAttribute("coordLong",       type: .doubleAttributeType)
        locationEntity.properties = [locSetting, cityName, coordLat, coordLong]

        // ── Weather entity ─────────────────────────────────────────────────
        let weatherEntity = NSEntityDescription()
        weatherEntity.name = "WeatherEntity"
        weatherEntity.managedObjectClassName = NSStringFromClass(WeatherEntity.self)

        let date      = makeAttribute("date",      type: .dateAttributeType)
        let weatherId = makeAttribute("weatherId", type: .integer32AttributeType)
        let shortDesc = makeAttribute("shortDesc", type: .stringAttributeType)
        let maxTemp   = makeAttribute("maxTemp",   type: .doubleAttributeType)
        let minTemp   = makeAttribute("minTemp",   type: .doubleAttributeType)
        let humidity  = makeAttribute("humidity",  type: .doubleAttributeType)
        let pressure  = makeAttribute("pressure",  type: .doubleAttributeType)
        let windSpeed = makeAttribute("windSpeed", type: .doubleAttributeType)
        let degrees   = makeAttribute("degrees",   type: .doubleAttributeType)
        weatherEntity.properties = [
            date, weatherId, shortDesc, maxTemp, minTemp,
            humidity, pressure, windSpeed, degrees
        ]

        // ── Relationship: WeatherEntity ←→ LocationEntity ─────────────────
        let weatherToLocation = NSRelationshipDescription()
        weatherToLocation.name = "location"
        weatherToLocation.destinationEntity = locationEntity
        weatherToLocation.minCount = 1
        weatherToLocation.maxCount = 1
        weatherToLocation.deleteRule = .nullifyDeleteRule

        let locationToWeather = NSRelationshipDescription()
        locationToWeather.name = "weatherEntries"
        locationToWeather.destinationEntity = weatherEntity
        locationToWeather.minCount = 0
        locationToWeather.maxCount = 0          // to-many
        locationToWeather.deleteRule = .cascadeDeleteRule

        weatherToLocation.inverseRelationship = locationToWeather
        locationToWeather.inverseRelationship = weatherToLocation

        weatherEntity.properties.append(weatherToLocation)
        locationEntity.properties.append(locationToWeather)

        model.entities = [locationEntity, weatherEntity]
        return model
    }

    private static func makeAttribute(
        _ name: String,
        type: NSAttributeType,
        optional: Bool = false
    ) -> NSAttributeDescription {
        let attr = NSAttributeDescription()
        attr.name = name
        attr.attributeType = type
        attr.isOptional = optional
        return attr
    }

    // MARK: - Convenience context for background work

    func newBackgroundContext() -> NSManagedObjectContext {
        container.newBackgroundContext()
    }
}

// MARK: - NSManagedObject Subclasses

@objc(LocationEntity)
final class LocationEntity: NSManagedObject {
    @NSManaged var locationSetting: String
    @NSManaged var cityName: String
    @NSManaged var coordLat: Double
    @NSManaged var coordLong: Double
    @NSManaged var weatherEntries: NSSet
}

@objc(WeatherEntity)
final class WeatherEntity: NSManagedObject {
    @NSManaged var date: Date
    @NSManaged var weatherId: Int32
    @NSManaged var shortDesc: String
    @NSManaged var maxTemp: Double
    @NSManaged var minTemp: Double
    @NSManaged var humidity: Double
    @NSManaged var pressure: Double
    @NSManaged var windSpeed: Double
    @NSManaged var degrees: Double
    @NSManaged var location: LocationEntity
}

// MARK: - WeatherEntity → WeatherForecast conversion

extension WeatherEntity {
    func toForecast() -> WeatherForecast {
        WeatherForecast(
            id: objectID.uriRepresentation().absoluteString.asUUID(),
            date: date,
            weatherId: Int(weatherId),
            shortDesc: shortDesc,
            maxTemp: maxTemp,
            minTemp: minTemp,
            humidity: humidity,
            pressure: pressure,
            windSpeed: windSpeed,
            degrees: degrees,
            locationSetting: location.locationSetting
        )
    }
}

private extension String {
    /// Deterministic UUID from any string via hashing.
    func asUUID() -> UUID {
        UUID(uuidString: String(prefix(36).padding(toLength: 36, withPad: "0", startingAt: 0)))
            ?? UUID()
    }
}
