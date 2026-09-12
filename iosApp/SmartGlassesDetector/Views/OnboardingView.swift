import SwiftUI

struct OnboardingView: View {
    @Environment(AppModel.self) private var model
    @State private var step = 0

    var body: some View {
        VStack {
            Group {
                switch step {
                case 0:
                    welcome
                case 1:
                    howTo
                default:
                    permission
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            HStack(spacing: 10) {
                ForEach(0..<3, id: \.self) { index in
                    Circle()
                        .fill(index == step ? AppTheme.brandOrange : Color.gray.opacity(0.3))
                        .frame(width: index == step ? 10 : 8, height: index == step ? 10 : 8)
                }
            }
            .padding(.bottom, 40)
        }
        .background(AppTheme.background.ignoresSafeArea())
    }

    private var welcome: some View {
        VStack(spacing: 16) {
            Image(systemName: "eyeglasses")
                .font(.system(size: 72))
                .foregroundStyle(AppTheme.brandOrange)
            Text("ようこそ！")
                .font(.title)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.textPrimary)
            Text("このアプリは、近くにある\nスマートグラスを見つけて\nお知らせします")
                .multilineTextAlignment(.center)
                .foregroundStyle(AppTheme.textSecondary)
            Spacer().frame(height: 24)
            Button("次へ") { step = 1 }
                .buttonStyle(PrimaryButtonStyle())
        }
        .padding(.horizontal, 32)
    }

    private var howTo: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("使い方はかんたん！")
                .font(.title)
                .fontWeight(.bold)
                .frame(maxWidth: .infinity)
            Label("「さがす」ボタンをタップするだけ", systemImage: "1.circle.fill")
            Label("近くにスマートグラスがあると通知でお知らせします", systemImage: "2.circle.fill")
            Spacer()
            Button("次へ") { step = 2 }
                .buttonStyle(PrimaryButtonStyle())
        }
        .padding(.horizontal, 32)
    }

    private var permission: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text("アプリの使用許可について")
                    .font(.title2)
                    .fontWeight(.bold)
                permissionCard(
                    icon: "antenna.radiowaves.left.and.right",
                    title: "Bluetooth の使用",
                    text: "周辺のスマートグラスを見つけるために使います。位置情報は収集・保存しません。"
                )
                permissionCard(
                    icon: "bell",
                    title: "通知",
                    text: "スマートグラスを見つけたときにすぐお知らせを表示するために使います。"
                )
                permissionCard(
                    icon: "moon.zzz",
                    title: "バックグラウンド動作",
                    text: "アプリを閉じたあとも、登録済みの Service UUID を出すグラスを探せます。名前やメーカー番号だけのグラスは、アプリを開いているときに検出します。"
                )
                Button("許可して始める") {
                    model.completeOnboarding()
                }
                .buttonStyle(PrimaryButtonStyle())
                Button("あとで設定する") {
                    model.settings.onboardingCompleted = true
                }
                .frame(maxWidth: .infinity)
                .foregroundStyle(AppTheme.textSecondary)
            }
            .padding(32)
        }
    }

    private func permissionCard(icon: String, title: String, text: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .foregroundStyle(AppTheme.brandOrange)
                .frame(width: 28)
            VStack(alignment: .leading, spacing: 4) {
                Text(title).fontWeight(.semibold)
                Text(text)
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.textSecondary)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(AppTheme.brandOrange.opacity(configuration.isPressed ? 0.8 : 1))
            .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
