import SwiftData
import SwiftUI

struct SettingsView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.modelContext) private var modelContext
    @State private var confirmClear = false

    var body: some View {
        @Bindable var settings = model.settings
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    group("動作") {
                        toggle(
                            "バックグラウンドで探す",
                            "アプリを閉じたあとも、登録済みの Service UUID を出すグラスを探せます。名前やメーカー番号だけのグラスは、アプリを開いているときに検出します。",
                            $settings.backgroundEnabled
                        )
                        Divider()
                        toggle(
                            "通知",
                            "スマートグラスを見つけたときにお知らせします",
                            $settings.notificationEnabled
                        )
                        Divider()
                        toggle(
                            "バイブレーション",
                            "見つけたときに振動でお知らせします",
                            $settings.vibrationEnabled
                        )
                        Divider()
                        toggle(
                            "通知音",
                            "見つけたときに音でお知らせします",
                            $settings.soundEnabled
                        )
                    }
                    group("検出の感度") {
                        ForEach(ScanSensitivitySetting.allCases) { option in
                            Button {
                                settings.sensitivity = option
                            } label: {
                                HStack(alignment: .top) {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(option.title)
                                            .foregroundStyle(AppTheme.textPrimary)
                                            .fontWeight(settings.sensitivity == option ? .bold : .regular)
                                        Text(option.subtitle)
                                            .font(.subheadline)
                                            .foregroundStyle(AppTheme.textSecondary)
                                            .multilineTextAlignment(.leading)
                                    }
                                    Spacer()
                                    if settings.sensitivity == option {
                                        Image(systemName: "checkmark.circle.fill")
                                            .foregroundStyle(AppTheme.brandOrange)
                                    }
                                }
                            }
                            .buttonStyle(.plain)
                            if option != ScanSensitivitySetting.allCases.last {
                                Divider()
                            }
                        }
                    }
                    group("その他") {
                        Button {
                            confirmClear = true
                        } label: {
                            navRow("検出記録を削除", "trash")
                        }
                        .buttonStyle(.plain)
                        Divider()
                        NavigationLink {
                            AboutView()
                        } label: {
                            navRow("このアプリについて", "info.circle")
                        }
                        Divider()
                        NavigationLink {
                            PrivacyView()
                        } label: {
                            navRow("プライバシーポリシー", "hand.raised")
                        }
                    }
                }
                .padding(16)
            }
            .background(AppTheme.background.ignoresSafeArea())
            .navigationTitle("設定")
            .alert("記録を削除しますか？", isPresented: $confirmClear) {
                Button("削除する", role: .destructive) {
                    model.clearRecords(context: modelContext)
                }
                Button("やめる", role: .cancel) {}
            } message: {
                Text("見つけた記録をこの端末から削除します。この操作は取り消せません。")
            }
        }
    }

    private func group(_ title: String, @ViewBuilder content: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundStyle(AppTheme.textSecondary)
            VStack(alignment: .leading, spacing: 12) {
                content()
            }
            .padding(16)
            .background(AppTheme.surface)
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }

    private func toggle(_ title: String, _ subtitle: String, _ isOn: Binding<Bool>) -> some View {
        Toggle(isOn: isOn) {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.textSecondary)
            }
        }
        .tint(AppTheme.brandOrange)
    }

    private func navRow(_ title: String, _ icon: String) -> some View {
        HStack {
            Image(systemName: icon)
                .foregroundStyle(AppTheme.brandOrange)
            Text(title)
                .foregroundStyle(AppTheme.textPrimary)
            Spacer()
            Image(systemName: "chevron.right")
                .foregroundStyle(AppTheme.textSecondary)
        }
    }
}
