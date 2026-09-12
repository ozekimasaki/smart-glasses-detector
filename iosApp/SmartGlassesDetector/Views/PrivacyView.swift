import SwiftUI

struct PrivacyView: View {
    private let policyURL = URL(string: "https://smart-glasses-detector-policy.maigo999.workers.dev")!
    private let developerURL = URL(string: "https://x.com/mei_999_")!

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("""
                【端末内に保存する情報】

                ・検出記録（機器名、機器識別子、メーカー名、信号強度、推定距離、検出日時）
                ・設定情報

                【情報の取り扱い】

                これらの情報は通常、お使いの端末内だけに保存されます。アプリが自動で外部サーバーへ送信することはありません。

                【位置情報について】

                iOS 13 以降の Bluetooth Low Energy 探索に位置情報の許可は使いません。GPS による位置情報の取得・記録は行いません。

                【バックグラウンド探索】

                アプリを閉じたあとの探索は、Apple の制限によりカタログに登録された Service UUID を出すグラスに限られます。

                【データの削除】

                設定画面または記録画面から、検出記録を削除できます。アプリをアンインストールすると、端末内の記録は削除されます。
                """)
                .foregroundStyle(AppTheme.textPrimary)
                Link("公開ポリシーを開く", destination: policyURL)
                    .frame(maxWidth: .infinity)
                Link("お問い合わせ", destination: developerURL)
                    .frame(maxWidth: .infinity)
            }
            .padding(24)
        }
        .background(AppTheme.background.ignoresSafeArea())
        .navigationTitle("プライバシーポリシー")
        .navigationBarTitleDisplayMode(.inline)
    }
}
