import SwiftData
import SwiftUI

@main
struct SmartGlassesDetectorApp: App {
    @State private var model = AppModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(model)
        }
        .modelContainer(for: DetectionRecord.self)
    }
}
