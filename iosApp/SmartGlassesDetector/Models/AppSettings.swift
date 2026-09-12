import Foundation
import Observation

enum ScanSensitivitySetting: Int, CaseIterable, Identifiable {
    case lowPower = 0
    case balanced = 1
    case highAccuracy = 2

    var id: Int { rawValue }

    var kotlinName: String {
        switch self {
        case .lowPower:
            return "LOW_POWER"
        case .balanced:
            return "BALANCED"
        case .highAccuracy:
            return "HIGH_ACCURACY"
        }
    }

    var title: String {
        switch self {
        case .lowPower:
            return "バッテリー節約"
        case .balanced:
            return "おすすめ"
        case .highAccuracy:
            return "精度重視"
        }
    }

    var subtitle: String {
        switch self {
        case .lowPower:
            return "近くにある機器だけを検出します。バッテリーにやさしい設定です"
        case .balanced:
            return "バッテリーと精度のバランスが良い、おすすめの設定です"
        case .highAccuracy:
            return "より広い範囲を検出します。バッテリーを多く使います"
        }
    }
}

@Observable
final class AppSettings {
    private let defaults: UserDefaults

    var backgroundEnabled: Bool {
        didSet { defaults.set(backgroundEnabled, forKey: Keys.backgroundEnabled) }
    }
    var notificationEnabled: Bool {
        didSet { defaults.set(notificationEnabled, forKey: Keys.notificationEnabled) }
    }
    var vibrationEnabled: Bool {
        didSet { defaults.set(vibrationEnabled, forKey: Keys.vibrationEnabled) }
    }
    var soundEnabled: Bool {
        didSet { defaults.set(soundEnabled, forKey: Keys.soundEnabled) }
    }
    var sensitivity: ScanSensitivitySetting {
        didSet { defaults.set(sensitivity.rawValue, forKey: Keys.sensitivity) }
    }
    var onboardingCompleted: Bool {
        didSet { defaults.set(onboardingCompleted, forKey: Keys.onboardingCompleted) }
    }
    var isScanning: Bool {
        didSet { defaults.set(isScanning, forKey: Keys.isScanning) }
    }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        backgroundEnabled = defaults.object(forKey: Keys.backgroundEnabled) as? Bool ?? true
        notificationEnabled = defaults.object(forKey: Keys.notificationEnabled) as? Bool ?? true
        vibrationEnabled = defaults.object(forKey: Keys.vibrationEnabled) as? Bool ?? true
        soundEnabled = defaults.object(forKey: Keys.soundEnabled) as? Bool ?? true
        let storedSensitivity = defaults.object(forKey: Keys.sensitivity) as? Int ?? 1
        sensitivity = ScanSensitivitySetting(rawValue: storedSensitivity) ?? .balanced
        onboardingCompleted = defaults.bool(forKey: Keys.onboardingCompleted)
        isScanning = defaults.bool(forKey: Keys.isScanning)
    }

    private enum Keys {
        static let backgroundEnabled = "background_enabled"
        static let notificationEnabled = "notification_enabled"
        static let vibrationEnabled = "vibration_enabled"
        static let soundEnabled = "sound_enabled"
        static let sensitivity = "sensitivity"
        static let onboardingCompleted = "onboarding_completed"
        static let isScanning = "is_scanning"
    }
}
