import Foundation

final class DetectionCooldown {
    private let sameDevice: TimeInterval
    private let sameManufacturer: TimeInterval
    private var lastDevice: [String: Date] = [:]
    private var lastManufacturer: [String: Date] = [:]

    init(
        sameDevice: TimeInterval = 30,
        sameManufacturer: TimeInterval = 15
    ) {
        self.sameDevice = sameDevice
        self.sameManufacturer = sameManufacturer
    }

    func shouldEmit(deviceKey: String, manufacturerKey: String) -> Bool {
        let now = Date()
        if let last = lastDevice[deviceKey], now.timeIntervalSince(last) < sameDevice {
            return false
        }
        let distinguishableDevice = deviceKey.hasPrefix("address:")
        if !distinguishableDevice {
            if let last = lastManufacturer[manufacturerKey], now.timeIntervalSince(last) < sameManufacturer {
                return false
            }
            lastManufacturer[manufacturerKey] = now
        }
        lastDevice[deviceKey] = now
        return true
    }

    func clear() {
        lastDevice.removeAll()
        lastManufacturer.removeAll()
    }
}
