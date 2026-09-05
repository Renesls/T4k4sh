import SwiftUI

/// Primer paso del acceso: credenciales.
///
/// El backend responde con un reto por correo; la sesión solo se crea en el
/// segundo paso, en `LoginVerificationView`.
struct LoginView: View {
    @Bindable var viewModel: AuthViewModel
    @Binding var path: NavigationPath

    @Environment(SessionStore.self) private var session

    @State private var correo = ""
    @State private var password = ""
    @State private var showPassword = false
    @FocusState private var focus: Field?

    private enum Field { case correo, password }

    private var canSubmit: Bool {
        Validation.isValidEmail(correo) && !password.isEmpty && !viewModel.isBusy
    }

    var body: some View {
        ScrollView {
            VStack(spacing: Theme.Spacing.lg) {
                AuthHeader(
                    title: "Bienvenido de vuelta",
                    subtitle: "Accede con tu correo institucional para continuar."
                )

                if let notice = session.sessionExpiredNotice {
                    InlineErrorBanner(message: notice) {
                        session.sessionExpiredNotice = nil
                    }
                }

                if let error = viewModel.errorMessage {
                    InlineErrorBanner(message: error) { viewModel.errorMessage = nil }
                }

                VStack(spacing: Theme.Spacing.md) {
                    LabeledField(label: "Correo institucional") {
                        TextField("nombre@universidad.edu.ni", text: $correo)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .keyboardType(.emailAddress)
                            .textContentType(.username)
                            .focused($focus, equals: .correo)
                            .submitLabel(.next)
                            .onSubmit { focus = .password }
                    }

                    LabeledField(label: "Contraseña") {
                        HStack {
                            Group {
                                if showPassword {
                                    TextField("Tu contraseña", text: $password)
                                } else {
                                    SecureField("Tu contraseña", text: $password)
                                }
                            }
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .textContentType(.password)
                            .focused($focus, equals: .password)
                            .submitLabel(.go)
                            .onSubmit { Task { await submit() } }

                            Button {
                                showPassword.toggle()
                            } label: {
                                Image(systemName: showPassword ? "eye.slash" : "eye")
                                    .foregroundStyle(Theme.Color.textSoft)
                            }
                            .accessibilityLabel(
                                showPassword ? "Ocultar contraseña" : "Mostrar contraseña"
                            )
                        }
                    }

                    Button("¿Olvidaste tu contraseña?") {
                        viewModel.clearMessages()
                        path.append(AuthRoute.forgotPassword)
                    }
                    .font(Theme.Font.footnote)
                    .foregroundStyle(Theme.Color.primary)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                }
                .cardSurface()

                VStack(spacing: Theme.Spacing.sm) {
                    Button {
                        Task { await submit() }
                    } label: {
                        Text("Continuar")
                    }
                    .buttonStyle(PrimaryButtonStyle(isLoading: viewModel.isBusy))
                    .disabled(!canSubmit)

                    Button("Crear una cuenta") {
                        viewModel.clearMessages()
                        path.append(AuthRoute.register)
                    }
                    .buttonStyle(SecondaryButtonStyle())
                    .disabled(viewModel.isBusy)
                }

                Text("Te enviaremos un código de acceso de un solo uso a tu correo.")
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textSoft)
                    .multilineTextAlignment(.center)
            }
            .padding(Theme.Spacing.md)
        }
        .scrollDismissesKeyboard(.interactively)
        .screenBackground()
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.hidden, for: .navigationBar)
    }

    private func submit() async {
        guard canSubmit else { return }
        focus = nil
        if await viewModel.login(correo: correo, password: password) {
            password = ""
            path.append(AuthRoute.loginVerification)
        }
    }
}
