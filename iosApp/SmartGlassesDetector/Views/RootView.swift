import SwiftData
import SwiftUI

struct RootView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.modelContext) private var modelContext
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        Group {
            if model.settings.onboardingCompleted {
                MainTabView()
            } else {
                OnboardingView()
            }
        }
        .tint(AppTheme.brandOrange)
        .onChange(of: scenePhase) { _, phase in
            model.setForeground(phase == .active)
        }
        .onReceive(NotificationCenter.default.publisher(for: .glassesDetected)) { output in
            if let device = output.object as? DetectedGlasses {
                DetectionStore.save(device, context: modelContext)
            }
        }
    }
}

private struct MainTabView: View {
    var body: some View {
        TabView {
            HomeView()
                .tabItem {
                    Label("ホーム", systemImage: "house")
                }
            HistoryView()
                .tabItem {
                    Label("記録", systemImage: "clock")
                }
            SettingsView()
                .tabItem {
                    Label("設定", systemImage: "gearshape")
                }
        }
    }
}
