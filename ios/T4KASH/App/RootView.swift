import SwiftUI

/// Decide qué mostrar según el estado de la sesión.
///
/// Al abrir la app hay un token guardado o no lo hay. Si lo hay, se valida
/// contra `/auth/me` antes de entrar: el backend puede haberlo revocado.
struct RootView: View {
    @Environment(AppDependencies.self) private var dependencies
    @Environment(SessionStore.self) private var session

    @State private var phase: Phase = .launching

    private enum Phase {
        case launching
        case ready
    }

    var body: some View {
        Group {
            switch phase {
            case .launching:
                LaunchView()
            case .ready:
                if session.isAuthenticated {
                    MainTabView()
                        .transition(.opacity)
                } else {
                    AuthFlowView()
                        .transition(.opacity)
                }
            }
        }
        .animation(.easeInOut(duration: 0.25), value: session.isAuthenticated)
        .animation(.easeInOut(duration: 0.25), value: phase)
        .task { await bootstrap() }
    }

    /// Restaura la sesión guardada. Un 401 la limpia por sí solo desde
    /// `APIClient`; cualquier otro fallo (por ejemplo, servidor dormido) deja
    /// entrar con el perfil en caché para no bloquear la app sin motivo.
    private func bootstrap() async {
        guard phase == .launching else { return }

        if session.hasStoredToken {
            do {
                let user = try await dependencies.auth.currentUser()
                session.update(user: user)
            } catch APIError.unauthorized {
                // `SessionStore` ya recibió el aviso de invalidación.
            } catch {
                // Sin red: se conserva la sesión local y las pantallas
                // mostrarán su propio estado de error al cargar datos.
            }
        }
        phase = .ready
    }
}

/// Pantalla de arranque con la marca, equivalente al `SplashScreen` de Android.
struct LaunchView: View {
    var body: some View {
        VStack(spacing: Theme.Spacing.lg) {
            Image("BrandLogo")
                .resizable()
                .scaledToFit()
                .frame(width: 120)
                .accessibilityLabel("T4KASH")

            ProgressView()
                .tint(Theme.Color.primary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .screenBackground()
    }
}
