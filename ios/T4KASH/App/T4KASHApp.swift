import SwiftUI

@main
struct T4KASHApp: App {
    @State private var dependencies = AppDependencies()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(dependencies)
                .environment(dependencies.session)
                .tint(Theme.Color.primary)
        }
    }
}
