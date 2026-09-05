import SwiftUI

/// Activación de cuenta con el código enviado al correo institucional.
/// El backend devuelve sesión directamente al verificar.
struct VerifyEmailView: View {
    @Bindable var viewModel: AuthViewModel

    @State private var code = ""
    @State private var secondsLeft = 0

    var body: some View {
        CodeChallengeScreen(
            title: "Activa tu cuenta",
            subtitle: "Enviamos un código de 6 dígitos a \(viewModel.pendingEmail).",
            code: $code,
            secondsLeft: secondsLeft,
            isBusy: viewModel.isBusy,
            errorMessage: viewModel.errorMessage,
            infoMessage: viewModel.infoMessage,
            primaryTitle: "Activar cuenta",
            onDismissError: { viewModel.errorMessage = nil },
            onSubmit: { _ = await viewModel.verifyEmail(codigo: code) },
            onResend: {
                code = ""
                await viewModel.resendVerification()
            }
        )
        .task(id: viewModel.challengeExpiresAt) { await runCountdown() }
        .navigationTitle("Activación")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func runCountdown() async {
        guard let expiry = viewModel.challengeExpiresAt else {
            secondsLeft = 0
            return
        }
        while !Task.isCancelled {
            secondsLeft = max(0, Int(expiry.timeIntervalSinceNow.rounded()))
            if secondsLeft == 0 { return }
            try? await Task.sleep(for: .seconds(1))
        }
    }
}
