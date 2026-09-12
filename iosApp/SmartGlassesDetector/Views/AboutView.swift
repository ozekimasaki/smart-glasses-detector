import SwiftUI

struct AboutView: View {
    private let developerURL = URL(string: "https://x.com/mei_999_")!

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("スマートグラス検出")
                    .font(.title)
                    .fontWeight(.bold)
                Text("このアプリは、Bluetooth を使って近くのスマートグラスを検出し、記録するアプリです。\n\n検出記録と設定は端末内に保存されます。検出記録には機器名、機器識別子、メーカー名、信号強度、推定距離、検出日時が含まれます。")
                    .foregroundStyle(AppTheme.textSecondary)
                Divider()
                Text("開発者")
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.textSecondary)
                Link("https://x.com/mei_999_", destination: developerURL)
                Text("バージョン")
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.textSecondary)
                Text(versionName)
            }
            .padding(24)
        }
        .background(AppTheme.background.ignoresSafeArea())
        .navigationTitle("このアプリについて")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var versionName: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
    }
}
