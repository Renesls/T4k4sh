import SwiftUI

/// Segundo paso del acceso: código de un solo uso.
struct LoginVerificationView: View {
    @Bindable var viewModel: AuthViewModel

    @State private var code = ""
    @State private var secondsLeft: Int = 0

    var body: some View {
        CodeChallengeScreen(
            title: "Verifica tu acceso",
            subtitle: "Escribe el código de 6 dígitos que enviamos a \(viewModel.pendingEmail).",
            code: $code,
            secondsLeft: secondsLeft,
            isBusy: viewModel.isBusy,
            errorMessage: viewModel.errorMessage,
            infoMessage: viewModel.infoMessage,
            primaryTitle: "Entrar",
            onDismissError: { viewModel.errorMessage = nil },
            onSubmit: { _ = await viewModel.verifyLogin(codigo: code) },
            onResend: {
                code = ""
                await viewModel.resendLoginCode()
            }
        )
        .task(id: viewModel.challengeExpiresAt) { await runCountdown() }
        .navigationTitle("Acceso")
        .navigationBarTitleDisplayMode(.inline)
    }

    /// Cuenta atrás hasta la expiración informada por el backend.
    private func runCountdown() async {
        guard let expiry = viewModel.challengeExpiresAt else {
            secondsLeft = 0
            return
        }
        while !Task.isCancelled {
            let remaining = Int(expiry.timeIntervalSinceNow.rounded())
            secondsLeft = max(0, remaining)
            if secondsLeft == 0 { return }
            try? await Task.sleep(for: .seconds(1))
        }
    }
}

/// Pantalla compartida por los tres retos de código del backend:
/// activación de cuenta, acceso en dos pasos y recuperación de contraseña.
struct CodeChallengeScreen: View {
    let title: String
    let subtitle: String
    @Binding var code: String
    let secondsLeft: Int
    let isBusy: Bool
    let errorMessage: String?
    let infoMessage: String?
    let primaryTitle: String
    let onDismissError: () -> Void
    let onSubmit: () async -> Void
    let onResend: () async -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: Theme.Spacing.lg) {
                AuthHeader(title: title, subtitle: subtitle)

                if let errorMessage {
                    InlineErrorBanner(message: errorMessage, onDismiss: onDismissError)
                } else if let infoMessage {
                    InlineSuccessBanner(message: infoMessage)
                }

                VStack(spacing: Theme.Spacing.md) {
                    VerificationCodeField(code: $code, isDisabled: isBusy)

                    if secondsLeft > 0 {
                        Label(
                            "El código vence en \(DisplayFormatter.countdown(seconds: Int64(secondsLeft)))",
                            systemImage: "clock"
                        )
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textMuted)
                    } else {
                        Label("El código expiró. Solicita uno nuevo.", systemImage: "clock.badge.exclamationmark")
                            .font(Theme.Font.caption)
                            .foregroundStyle(Theme.Color.warning)
                    }
                }
                .cardSurface()

                VStack(spacing: Theme.Spacing.sm) {
                    AsyncButton(action: onSubmit) { Text(primaryTitle) }
                        .buttonStyle(PrimaryButtonStyle(isLoading: isBusy))
                        .disabled(!Validation.isValidCode(code) || isBusy)

                    AsyncButton("Reenviar código", action: onResend)
                        .buttonStyle(SecondaryButtonStyle())
                        .disabled(isBusy)
                }
            }
            .padding(Theme.Spacing.md)
        }
        .scrollDismissesKeyboard(.interactively)
        .screenBackground()
    }
}
