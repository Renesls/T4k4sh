import SwiftUI

/// Navegación principal tras iniciar sesión.
///
/// Android usa `NavGraph` con rutas de texto; en iOS se resuelve con `TabView`
/// más un `NavigationStack` por pestaña, que es el patrón esperado en iPhone:
/// cada pestaña conserva su propia pila y el gesto de retroceso funciona solo.
struct MainTabView: View {
    @Environment(AppDependencies.self) private var dependencies
    @Environment(SessionStore.self) private var session

    @State private var selection: Tab = .explore
    @State private var unreadNotifications = 0
    @State private var unreadMessages = 0

    enum Tab: Hashable {
        case explore, network, work, chat, profile
    }

    var body: some View {
        TabView(selection: $selection) {
            NavigationStack {
                MarketplaceView(unreadNotifications: $unreadNotifications)
            }
            .tabItem { Label("Explorar", systemImage: "square.grid.2x2") }
            .tag(Tab.explore)

            NavigationStack {
                FeedView()
            }
            .tabItem { Label("Red", systemImage: "person.3") }
            .tag(Tab.network)

            NavigationStack {
                WorkHubView()
            }
            .tabItem { Label("Trabajos", systemImage: "briefcase") }
            .tag(Tab.work)

            NavigationStack {
                ConversationsView()
            }
            .tabItem { Label("Chat", systemImage: "bubble.left.and.bubble.right") }
            .badge(unreadMessages)
            .tag(Tab.chat)

            NavigationStack {
                ProfileView()
            }
            .tabItem { Label("Perfil", systemImage: "person.crop.circle") }
            .tag(Tab.profile)
        }
        .tint(Theme.Color.primary)
        .task { await refreshBadges() }
        // Al volver a la pestaña de chat se recalcula el badge con datos frescos.
        .task(id: selection) { await refreshBadges() }
    }

    /// Consulta los contadores reales de la API para los indicadores de pestaña.
    private func refreshBadges() async {
        async let notifications = try? dependencies.communication.notifications(size: 50)
        async let conversations = try? dependencies.communication.conversations(size: 50)

        unreadNotifications = (await notifications)?.filter { !$0.leida }.count ?? 0
        unreadMessages = (await conversations)?.reduce(0) { $0 + $1.mensajesNoLeidos } ?? 0
    }
}
