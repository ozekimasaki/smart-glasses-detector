import Foundation

struct DetectedGlasses: Identifiable, Equatable {
    let id: String
    let name: String
    let manufacturerName: String
    let rssi: Int
    let distanceLabel: String
    let detectedAt: Date

    var displayName: String {
        name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? manufacturerName : name
    }
}
