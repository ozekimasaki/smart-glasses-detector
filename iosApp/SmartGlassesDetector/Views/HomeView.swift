import SwiftData
import SwiftUI

struct HomeView: View {
    @Environment(AppModel.self) private var model
    @Query(sort: \DetectionRecord.detectedAt, order: .reverse) private var records: [DetectionRecord]

    private var todayCount: Int {
        records.filter { Calendar.current.isDateInToday($0.detectedAt) }.count
    }

    private var recent: [DetectionRecord] {
        Array(records.prefix(3))
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                header
                Text("近くのスマートグラスを\n見つけましょう")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundStyle(AppTheme.textPrimary)
                Text("Bluetooth を使って周辺の機器を検出します。\n「さがす」ボタンを押すと探索を開始します。")
                    .foregroundStyle(AppTheme.textSecondary)
                todayCard
                if model.isScanning {
                    scanningBadge
                }
                if let message = model.restoreMessage {
                    restoreBanner(message)
                }
                scanButton
                if model.isScanning {
                    nearbySection
                } else if recent.isEmpty {
                    howItWorks
                } else {
                    recentSection
                }
            }
            .padding(20)
        }
        .background(AppTheme.background.ignoresSafeArea())
    }

    private var header: some View {
        HStack(spacing: 12) {
            Image(systemName: "sensor.tag.radiowaves.forward")
                .foregroundStyle(.white)
                .frame(width: 44, height: 44)
                .background(AppTheme.brandOrange)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            VStack(alignment: .leading) {
                Text("スマートグラス検出")
                    .fontWeight(.bold)
                Text("近くのスマートグラスを見つけるアプリ")
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.textSecondary)
            }
        }
    }

    private var todayCard: some View {
        HStack {
            Text("\(todayCount)")
                .font(.system(size: 52, weight: .bold))
                .foregroundStyle(AppTheme.brandOrange)
            VStack(alignment: .leading) {
                Text("個").fontWeight(.semibold)
                Text("今日見つけたスマートグラス")
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.textSecondary)
            }
            Spacer()
        }
        .padding(20)
        .background(AppTheme.surfaceVariant)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var scanningBadge: some View {
        HStack(spacing: 8) {
            Circle()
                .fill(AppTheme.brandOrange)
                .frame(width: 8, height: 8)
            Text(
                model.settings.backgroundEnabled
                    ? "探索中 - 設定に応じて閉じたあとも続けられます"
                    : "探索中 - アプリを開いている間だけ動作します"
            )
            .font(.subheadline)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(AppTheme.brandOrangeLight)
        .clipShape(Capsule())
    }

    private func restoreBanner(_ message: String) -> some View {
        Button(action: model.openBluetoothSettings) {
            VStack(alignment: .leading, spacing: 8) {
                Text(message)
                    .foregroundStyle(AppTheme.textPrimary)
                Text("設定を開く")
                    .fontWeight(.bold)
                    .foregroundStyle(AppTheme.brandOrange)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(AppTheme.brandOrangeLight)
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
    }

    private var scanButton: some View {
        Button(action: model.toggleScanning) {
            HStack(spacing: 16) {
                Image(systemName: model.isScanning ? "stop.circle.fill" : "antenna.radiowaves.left.and.right")
                    .font(.title2)
                    .frame(width: 40, height: 40)
                    .foregroundStyle(model.isScanning ? AppTheme.textPrimary : .white)
                    .background(model.isScanning ? Color.clear : .white.opacity(0.2))
                    .clipShape(Circle())
                VStack(alignment: .leading) {
                    Text(model.isScanning ? "とめる" : "周辺をさがす")
                        .font(.headline)
                    Text(
                        model.isScanning
                            ? "タップして探索を停止する"
                            : "タップして周辺のスマートグラスを探す"
                    )
                    .font(.subheadline)
                    .opacity(0.85)
                }
                Spacer()
            }
            .foregroundStyle(model.isScanning ? AppTheme.textPrimary : .white)
            .padding(18)
            .background(model.isScanning ? AppTheme.surface : AppTheme.brandOrange)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(model.isScanning ? Color.gray.opacity(0.3) : Color.clear, lineWidth: 2)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(model.isScanning ? "とめる。タップして探索を停止する" : "周辺をさがす。タップして周辺のスマートグラスを探す")
    }

    private var howItWorks: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("このアプリについて")
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundStyle(AppTheme.textSecondary)
            labeledHint("antenna.radiowaves.left.and.right", "Bluetooth で周辺の機器を検出します")
            labeledHint("bell", "スマートグラスを見つけたら通知でお知らせします")
            labeledHint("clock", "検出した記録は「記録」タブで確認できます")
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppTheme.surfaceVariant)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func labeledHint(_ icon: String, _ text: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: icon)
                .foregroundStyle(AppTheme.brandOrange)
            Text(text)
                .font(.subheadline)
                .foregroundStyle(AppTheme.textSecondary)
        }
    }

    private var nearbySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("いま近くにいるスマートグラス")
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundStyle(AppTheme.textSecondary)
            if model.nearbyDevices.isEmpty {
                Text("探索中です。見つかるとここに表示されます")
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.textSecondary)
            } else {
                ForEach(model.nearbyDevices) { device in
                    DetectionRow(
                        name: device.displayName,
                        manufacturerName: device.manufacturerName,
                        distanceLabel: device.distanceLabel,
                        detectedAt: device.detectedAt
                    )
                }
            }
        }
    }

    private var recentSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("最近見つけたスマートグラス")
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundStyle(AppTheme.textSecondary)
            ForEach(recent) { record in
                DetectionRow(
                    name: record.name,
                    manufacturerName: record.manufacturerName,
                    distanceLabel: record.distanceLabel,
                    detectedAt: record.detectedAt
                )
            }
        }
    }
}
