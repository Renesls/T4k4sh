import SwiftUI

/// Solicita el código de recuperación de contraseña.
struct ForgotPasswordView: View {
    @Bindable var viewModel: AuthViewModel
    @Binding var path: NavigationPath

    @State private var correo = ""

    var body: some View {
        ScrollView {
            VStack(spacing: Theme.Spacing.lg) {
                AuthHeader(
                    title: "Recupera tu acceso",
                    subtitle: "Te enviaremos un código para crear una contraseña nueva."
                )

                if let error = viewModel.errorMessage {
                    InlineErrorBanner(message: error) { viewModel.errorMessage = nil }
                }

                LabeledField(label: "Correo institucional") {
                    TextField("nombre@universidad.edu.ni", text: $correo)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.emailAddress)
                        .textContentType(.username)
                }
                .cardSurface()

                AsyncButton {
                    guard Validation.isValidEmail(correo) else { return }
                    if await viewModel.forgotPassword(
                        correo: correo.trimmingCharacters(in: .whitespaces).lowercased()
                    ) {
                        path.append(AuthRoute.resetPassword)
                    }
                } label: {
                    Text("Enviar código")
                }
                .buttonStyle(PrimaryButtonStyle(isLoading: viewModel.isBusy))
                .disabled(!Validation.isValidEmail(correo) || viewModel.isBusy)

                Text("Por seguridad, la respuesta es la misma exista o no una cuenta con ese correo.")
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textSoft)
                    .multilineTextAlignment(.center)
            }
            .padding(Theme.Spacing.md)
        }
        .scrollDismissesKeyboard(.interactively)
        .screenBackground()
        .navigationTitle("Recuperación")
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// Define la nueva contraseña con el código recibido.
struct ResetPasswordView: View {
    @Bindable var viewModel: AuthViewModel
    @Binding var path: NavigationPath

    @State private var code = ""
    @State private var newPassword = ""
    @State private var confirmPassword = ""
    @State private var didReset = false

    private var canSubmit: Bool {
        Validation.isValidCode(code)
            && Validation.isValidPassword(newPassword)
            && newPassword == confirmPassword
            && !viewModel.isBusy
    }

    var body: some View {
        ScrollView {
            VStack(spacing: Theme.Spacing.lg) {
                AuthHeader(
                    title: "Nueva contraseña",
                    subtitle: "Escribe el código que enviamos a \(viewModel.pendingEmail) y tu nueva contraseña."
                )

                if let error = viewModel.errorMessage {
                    InlineErrorBanner(message: error) { viewModel.errorMessage = nil }
                } else if didReset, let info = viewModel.infoMessage {
                    InlineSuccessBanner(message: info)
                }

                VStack(spacing: Theme.Spacing.md) {
                    VerificationCodeField(code: $code, isDisabled: viewModel.isBusy)

                    LabeledField(
                        label: "Nueva contraseña",
                        hint: "Entre 8 y 72 caracteres.",
                        error: newPassword.isEmpty || Validation.isValidPassword(newPassword)
                            ? nil
                            : "La contraseña debe tener entre 8 y 72 caracteres."
                    ) {
                        SecureField("Nueva contraseña", text: $newPassword)
                            .textContentType(.newPassword)
                    }

                    LabeledField(
                        label: "Confirma la contraseña",
                        error: confirmPassword.isEmpty || newPassword == confirmPassword
                            ? nil
                            : "Las contraseñas no coinciden."
                    ) {
                        SecureField("Repite la contraseña", text: $confirmPassword)
                            .textContentType(.newPassword)
                    }
                }
                .cardSurface()

                if didReset {
                    Button("Volver al inicio de sesión") {
                        path = NavigationPath()
                    }
                    .buttonStyle(PrimaryButtonStyle())
                } else {
                    AsyncButton {
                        guard canSubmit else { return }
                        didReset = await viewModel.resetPassword(
                            codigo: code,
                            nuevaPassword: newPassword
                        )
                    } label: {
                        Text("Actualizar contraseña")
                    }
                    .buttonStyle(PrimaryButtonStyle(isLoading: viewModel.isBusy))
                    .disabled(!canSubmit)
                }
            }
            .padding(Theme.Spacing.md)
        }
        .scrollDismissesKeyboard(.interactively)
        .screenBackground()
        .navigationTitle("Nueva contraseña")
        .navigationBarTitleDisplayMode(.inline)
    }
}
