import SwiftUI

/// Destinos del flujo de autenticación.
enum AuthRoute: Hashable {
    case loginVerification
    case register
    case verifyEmail
    case forgotPassword
    case resetPassword
}

/// Contenedor del flujo previo a la sesión.
struct AuthFlowView: View {
    @Environment(AppDependencies.self) private var dependencies
    @Environment(SessionStore.self) private var session

    @State private var viewModel: AuthViewModel?
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            Group {
                if let viewModel {
                    LoginView(viewModel: viewModel, path: $path)
                } else {
                    LoadingStateView()
                }
            }
            .navigationDestination(for: AuthRoute.self) { route in
                if let viewModel {
                    destination(for: route, viewModel: viewModel)
                }
            }
        }
        .onAppear {
            if viewModel == nil {
                viewModel = AuthViewModel(
                    repository: dependencies.auth,
                    session: session
                )
            }
        }
    }

    @ViewBuilder
    private func destination(for route: AuthRoute, viewModel: AuthViewModel) -> some View {
        switch route {
        case .loginVerification:
            LoginVerificationView(viewModel: viewModel)
        case .register:
            RegisterView(viewModel: viewModel, path: $path)
        case .verifyEmail:
            VerifyEmailView(viewModel: viewModel)
        case .forgotPassword:
            ForgotPasswordView(viewModel: viewModel, path: $path)
        case .resetPassword:
            ResetPasswordView(viewModel: viewModel, path: $path)
        }
    }
}

/// Cabecera de marca compartida por las pantallas de acceso.
struct AuthHeader: View {
    let title: String
    let subtitle: String

    var body: some View {
        VStack(spacing: Theme.Spacing.sm) {
            Image("BrandLogo")
                .resizable()
                .scaledToFit()
                .frame(width: 84)
                .accessibilityHidden(true)

            Text(title)
                .font(Theme.Font.title)
                .foregroundStyle(Theme.Color.text)

            Text(subtitle)
                .font(Theme.Font.subheadline)
                .foregroundStyle(Theme.Color.textMuted)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, Theme.Spacing.lg)
    }
}
