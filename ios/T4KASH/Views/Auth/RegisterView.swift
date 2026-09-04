import SwiftUI

/// Registro institucional.
///
/// La universidad se detecta por el dominio del correo y las carreras solo
/// aparecen cuando hay coincidencia, tal como funciona el backend y Android.
struct RegisterView: View {
    @Bindable var viewModel: AuthViewModel
    @Binding var path: NavigationPath

    @State private var nombre = ""
    @State private var apellido = ""
    @State private var correo = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var carnet = ""
    @State private var selectedCareer: Career?
    @State private var showPassword = false

    private var passwordsMatch: Bool {
        confirmPassword.isEmpty || password == confirmPassword
    }

    private var canSubmit: Bool {
        !nombre.trimmingCharacters(in: .whitespaces).isEmpty
            && !apellido.trimmingCharacters(in: .whitespaces).isEmpty
            && Validation.isValidEmail(correo)
            && Validation.isValidPassword(password)
            && password == confirmPassword
            && !viewModel.isBusy
    }

    var body: some View {
        ScrollView {
            VStack(spacing: Theme.Spacing.lg) {
                AuthHeader(
                    title: "Crea tu cuenta",
                    subtitle: "Usa tu correo institucional para que podamos reconocer tu universidad."
                )

                if let error = viewModel.errorMessage {
                    InlineErrorBanner(message: error) { viewModel.errorMessage = nil }
                }

                VStack(spacing: Theme.Spacing.md) {
                    LabeledField(label: "Nombre") {
                        TextField("Tu nombre", text: $nombre)
                            .textContentType(.givenName)
                    }

                    LabeledField(label: "Apellido") {
                        TextField("Tu apellido", text: $apellido)
                            .textContentType(.familyName)
                    }

                    LabeledField(
                        label: "Correo institucional",
                        hint: "Detectamos tu universidad a partir del dominio.",
                        error: correo.isEmpty || Validation.isValidEmail(correo)
                            ? nil
                            : "Escribe un correo válido."
                    ) {
                        TextField("nombre@universidad.edu.ni", text: $correo)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .keyboardType(.emailAddress)
                            .textContentType(.username)
                    }

                    universitySection
                }
                .cardSurface()

                VStack(spacing: Theme.Spacing.md) {
                    LabeledField(
                        label: "Contraseña",
                        hint: "Entre 8 y 72 caracteres.",
                        error: password.isEmpty || Validation.isValidPassword(password)
                            ? nil
                            : "La contraseña debe tener entre 8 y 72 caracteres."
                    ) {
                        HStack {
                            Group {
                                if showPassword {
                                    TextField("Crea una contraseña", text: $password)
                                } else {
                                    SecureField("Crea una contraseña", text: $password)
                                }
                            }
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .textContentType(.newPassword)

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

                    LabeledField(
                        label: "Confirma la contraseña",
                        error: passwordsMatch ? nil : "Las contraseñas no coinciden."
                    ) {
                        SecureField("Repite la contraseña", text: $confirmPassword)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .textContentType(.newPassword)
                    }
                }
                .cardSurface()

                Button {
                    Task { await submit() }
                } label: {
                    Text("Crear cuenta")
                }
                .buttonStyle(PrimaryButtonStyle(isLoading: viewModel.isBusy))
                .disabled(!canSubmit)

                Text("Al continuar recibirás un código para activar tu cuenta.")
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textSoft)
                    .multilineTextAlignment(.center)
            }
            .padding(Theme.Spacing.md)
        }
        .scrollDismissesKeyboard(.interactively)
        .screenBackground()
        .navigationTitle("Registro")
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.loadUniversities() }
        .task(id: correo) {
            // Pequeña espera para no consultar en cada pulsación de tecla.
            try? await Task.sleep(for: .milliseconds(350))
            guard !Task.isCancelled else { return }
            await viewModel.updateDetectedUniversity(for: correo)
            selectedCareer = nil
        }
    }

    @ViewBuilder
    private var universitySection: some View {
        if let university = viewModel.detectedUniversity {
            VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
                Label(university.nombreUniversidad, systemImage: "building.columns.fill")
                    .font(Theme.Font.footnote.weight(.semibold))
                    .foregroundStyle(Theme.Color.primaryDark)
                    .padding(.horizontal, Theme.Spacing.sm)
                    .padding(.vertical, Theme.Spacing.xs)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        RoundedRectangle(cornerRadius: Theme.Radius.sm, style: .continuous)
                            .fill(Theme.Color.primaryContainer)
                    )

                if viewModel.isLoadingCareers {
                    HStack(spacing: Theme.Spacing.xs) {
                        ProgressView().controlSize(.small)
                        Text("Cargando carreras…")
                            .font(Theme.Font.caption)
                            .foregroundStyle(Theme.Color.textMuted)
                    }
                } else if !viewModel.careers.isEmpty {
                    LabeledField(label: "Carrera", hint: "Opcional") {
                        Picker("Carrera", selection: $selectedCareer) {
                            Text("Sin especificar").tag(Career?.none)
                            ForEach(viewModel.careers) { career in
                                Text(career.nombreCarrera).tag(Career?.some(career))
                            }
                        }
                        .pickerStyle(.menu)
                        .tint(Theme.Color.primary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }

                    LabeledField(label: "Carnet universitario", hint: "Opcional") {
                        TextField("Ej. 2021-0345U", text: $carnet)
                            .textInputAutocapitalization(.characters)
                            .autocorrectionDisabled()
                    }
                }
            }
        } else if !correo.isEmpty && Validation.isValidEmail(correo) {
            Label(
                "No reconocemos ese dominio institucional. Puedes continuar, pero tu perfil no mostrará universidad.",
                systemImage: "info.circle"
            )
            .font(Theme.Font.caption)
            .foregroundStyle(Theme.Color.textMuted)
        }
    }

    private func submit() async {
        guard canSubmit else { return }
        let created = await viewModel.register(
            nombre: nombre.trimmingCharacters(in: .whitespaces),
            apellido: apellido.trimmingCharacters(in: .whitespaces),
            correo: correo.trimmingCharacters(in: .whitespaces).lowercased(),
            password: password,
            idCarrera: selectedCareer?.idCarrera,
            carnet: carnet.trimmingCharacters(in: .whitespaces)
        )
        if created {
            password = ""
            confirmPassword = ""
            path.append(AuthRoute.verifyEmail)
        }
    }
}
