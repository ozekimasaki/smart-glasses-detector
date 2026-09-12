import CoreBluetooth
import Foundation
import Observation
import SwiftData
import SwiftUI

@Observable
final class AppModel {
    let settings: AppSettings
    let scanner: GlassesScanner
    private let notifier: DetectionNotifier

    var bluetoothState: CBManagerState = .unknown
    var nearbyDevices: [DetectedGlasses] = []

    init(settings: AppSettings = AppSettings()) {
        self.settings = settings
        self.scanner = GlassesScanner(settings: settings)
        self.notifier = DetectionNotifier(settings: settings)
        scanner.onState = { [weak self] state in
            self?.bluetoothState = state
        }
        scanner.onNearby = { [weak self] devices in
            self?.nearbyDevices = devices
        }
        scanner.onDetection = { [weak self] device, shouldNotify in
            self?.handleDetection(device, shouldNotify: shouldNotify)
        }
        scanner.prepare()
        if settings.isScanning {
            scanner.start()
        }
    }

    var isScanning: Bool { settings.isScanning }

    var restoreMessage: String? {
        if !settings.isScanning {
            return nil
        }
        switch bluetoothState {
        case .unauthorized:
            return "探索を続けるには、Bluetooth の使用を許可してください"
        case .poweredOff:
            return "探索を続けるには、Bluetooth をオンにしてください"
        case .unsupported:
            return "この端末では Bluetooth 探索を使えません"
        default:
            return nil
        }
    }

    func completeOnboarding() {
        settings.onboardingCompleted = true
        notifier.requestAuthorization()
        scanner.prepare()
    }

    func toggleScanning() {
        if settings.isScanning {
            stopScanning()
        } else {
            startScanning()
        }
    }

    func startScanning() {
        notifier.requestAuthorization()
        settings.isScanning = true
        scanner.start()
    }

    func stopScanning() {
        settings.isScanning = false
        nearbyDevices = []
        scanner.stop()
    }

    func setForeground(_ foreground: Bool) {
        scanner.setForeground(foreground)
    }

    func openBluetoothSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else {
            return
        }
        UIApplication.shared.open(url)
    }

    @MainActor
    func clearRecords(context: ModelContext) {
        DetectionStore.clear(context: context)
    }

    private func handleDetection(_ device: DetectedGlasses, shouldNotify: Bool) {
        NotificationCenter.default.post(name: .glassesDetected, object: device)
        if shouldNotify {
            notifier.notify(device: device)
        }
    }
}

extension Notification.Name {
    static let glassesDetected = Notification.Name("jp.smartglasses.detector.detected")
}
