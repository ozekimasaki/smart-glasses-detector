import Foundation

final class NearbyTracker {
    private let ttl: TimeInterval
    private var items: [String: (device: DetectedGlasses, lastSeen: Date)] = [:]

    init(ttl: TimeInterval = 120) {
        self.ttl = ttl
    }

    func upsert(_ device: DetectedGlasses) {
        items[device.id] = (device, Date())
    }

    func snapshot() -> [DetectedGlasses] {
        let now = Date()
        items = items.filter { now.timeIntervalSince($0.value.lastSeen) < ttl }
        return items.values
            .map(\.device)
            .sorted { lhs, rhs in
                if lhs.rssi == rhs.rssi {
                    return lhs.detectedAt > rhs.detectedAt
                }
                return lhs.rssi > rhs.rssi
            }
    }

    func clear() {
        items.removeAll()
    }
}
