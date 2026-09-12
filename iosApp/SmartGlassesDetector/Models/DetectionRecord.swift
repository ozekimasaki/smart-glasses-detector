import Foundation
import SwiftData

@Model
final class DetectionRecord {
    var name: String
    var address: String
    var manufacturerName: String
    var rssi: Int
    var distanceLabel: String
    var detectedAt: Date

    init(
        name: String,
        address: String,
        manufacturerName: String,
        rssi: Int,
        distanceLabel: String,
        detectedAt: Date
    ) {
        self.name = name
        self.address = address
        self.manufacturerName = manufacturerName
        self.rssi = rssi
        self.distanceLabel = distanceLabel
        self.detectedAt = detectedAt
    }

    convenience init(device: DetectedGlasses) {
        self.init(
            name: device.displayName,
            address: device.id,
            manufacturerName: device.manufacturerName,
            rssi: device.rssi,
            distanceLabel: device.distanceLabel,
            detectedAt: device.detectedAt
        )
    }
}

enum DetectionStore {
    static let keepCount = 1000

    @MainActor
    static func save(_ device: DetectedGlasses, context: ModelContext) {
        context.insert(DetectionRecord(device: device))
        prune(context: context)
        try? context.save()
    }

    @MainActor
    static func clear(context: ModelContext) {
        let descriptor = FetchDescriptor<DetectionRecord>()
        if let records = try? context.fetch(descriptor) {
            for record in records {
                context.delete(record)
            }
        }
        try? context.save()
    }

    private static func prune(context: ModelContext) {
        var descriptor = FetchDescriptor<DetectionRecord>(
            sortBy: [SortDescriptor(\.detectedAt, order: .reverse)]
        )
        let records = (try? context.fetch(descriptor)) ?? []
        for extra in records.dropFirst(keepCount) {
            context.delete(extra)
        }
    }
}
