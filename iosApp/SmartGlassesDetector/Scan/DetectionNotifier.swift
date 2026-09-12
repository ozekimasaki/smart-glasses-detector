import Foundation
import UserNotifications
import UIKit
import AudioToolbox

final class DetectionNotifier {
    private let settings: AppSettings

    init(settings: AppSettings) {
        self.settings = settings
    }

    func requestAuthorization() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
    }

    func notify(device: DetectedGlasses) {
        if settings.vibrationEnabled {
            AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
            UINotificationFeedbackGenerator().notificationOccurred(.success)
        }
        guard settings.notificationEnabled else {
            return
        }
        let content = UNMutableNotificationContent()
        content.title = "スマートグラスを見つけました！"
        content.body = "\(device.displayName)（\(device.manufacturerName)）・\(device.distanceLabel)"
        content.threadIdentifier = "smart_glasses_detections"
        if settings.soundEnabled {
            content.sound = .default
        }
        let request = UNNotificationRequest(
            identifier: "detection-\(device.id)",
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)
    }
}
