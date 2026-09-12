import SwiftUI

struct DetectionRow: View {
    let name: String
    let manufacturerName: String
    let distanceLabel: String
    let detectedAt: Date

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "sensor.tag.radiowaves.forward")
                .foregroundStyle(AppTheme.brandOrange)
                .frame(width: 40, height: 40)
                .background(AppTheme.surfaceVariant)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 4) {
                Text(name)
                    .fontWeight(.semibold)
                    .foregroundStyle(AppTheme.textPrimary)
                Text(manufacturerName)
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.textSecondary)
                Text(timeText)
                    .font(.caption)
                    .foregroundStyle(AppTheme.textSecondary)
            }
            Spacer()
            Text(distanceLabel)
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundStyle(.white)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(AppTheme.distanceColor(for: distanceLabel))
                .clipShape(Capsule())
        }
        .padding(14)
        .background(AppTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private var timeText: String {
        detectedAt.formatted(date: .omitted, time: .shortened)
    }
}
