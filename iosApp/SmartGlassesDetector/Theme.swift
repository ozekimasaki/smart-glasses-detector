import SwiftUI

enum AppTheme {
    static let brandOrange = Color(red: 230 / 255, green: 81 / 255, blue: 0)
    static let brandOrangeLight = Color(red: 1, green: 243 / 255, blue: 224 / 255)
    static let background = Color(red: 250 / 255, green: 250 / 255, blue: 250 / 255)
    static let surface = Color.white
    static let surfaceVariant = Color(red: 245 / 255, green: 245 / 255, blue: 245 / 255)
    static let textPrimary = Color(red: 26 / 255, green: 26 / 255, blue: 26 / 255)
    static let textSecondary = Color(red: 97 / 255, green: 97 / 255, blue: 97 / 255)
    static let veryClose = Color(red: 211 / 255, green: 47 / 255, blue: 47 / 255)
    static let close = brandOrange
    static let moderate = Color(red: 121 / 255, green: 85 / 255, blue: 72 / 255)
    static let far = Color(red: 117 / 255, green: 117 / 255, blue: 117 / 255)

    static func distanceColor(for label: String) -> Color {
        switch label {
        case "とても近い":
            return veryClose
        case "近い":
            return close
        case "少し離れている":
            return moderate
        default:
            return far
        }
    }
}
