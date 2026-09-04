import SwiftUI

/// Perfil propio y punto de entrada a billetera, verificaciones, ajustes y
/// administración.
struct ProfileView: View {
    @Environment(AppDependencies.self) private var dependencies
    @Environment(SessionStore.self) private var session

    @State private var viewModel: ProfileViewModel?
    @State private var showUsernameEditor = false

    var body: some View {
        Group {
            if let viewModel, let user = session.user {
                content(viewModel: viewModel, user: user)
            } else {
                LoadingStateView()
            }
        }
        .screenBackground()
        .navigationTitle("Perfil")
        .navigationBarTitleDisplayMode(.large)
        .onAppear {
            if viewModel == nil {
                viewModel = ProfileViewModel(
                    auth: dependencies.auth,
                    identityVerification: dependencies.identityVerification,
                    session: session
                )
            }
        }
        .task { await viewModel?.load() }
        .sheet(isPresented: $showUsernameEditor) {
            if let viewModel {
                UsernameEditorSheet(
                    current: session.user?.nombreUsuario ?? "",
                    nextChangeAllowedAt: viewModel.profile?.proximoCambioNombreUsuario
                ) { nuevo in
                    await viewModel.updateUsername(nuevo)
                }
                .presentationDetents([.medium])
            }
        }
    }

    @ViewBuilder
    private func content(viewModel: ProfileViewModel, user: AuthenticatedUser) -> some View {
        ScrollView {
            VStack(spacing: Theme.Spacing.md) {
                if let error = viewModel.actionError {
                    InlineErrorBanner(message: error) { viewModel.actionError = nil }
                }
                if let success = viewModel.successMessage {
                    InlineSuccessBanner(message: success)
                }

                identityCard(viewModel: viewModel, user: user)
                statsCard(viewModel: viewModel)
                shortcutsCard(user: user)
                verificationCard(viewModel: viewModel)
            }
            .padding(Theme.Spacing.md)
        }
        .refreshable { await viewModel.load() }
    }

    private func identityCard(
        viewModel: ProfileViewModel,
        user: AuthenticatedUser
    ) -> some View {
        VStack(spacing: Theme.Spacing.sm) {
            InitialsAvatar(
                initials: user.iniciales,
                size: 84,
                verified: viewModel.profile?.identidad.estudianteVerificado ?? false
            )

            VStack(spacing: 2) {
                Text(user.nombreCompleto)
                    .font(Theme.Font.title)
                    .foregroundStyle(Theme.Color.text)

                Button {
                    showUsernameEditor = true
                } label: {
                    HStack(spacing: 4) {
                        Text(user.arroba.isEmpty ? "Sin nombre de usuario" : user.arroba)
                        Image(systemName: "pencil.circle")
                    }
                    .font(Theme.Font.subheadline.weight(.medium))
                    .foregroundStyle(Theme.Color.primary)
                }
                .buttonStyle(.plain)
            }

            if let universidad = user.nombreUniversidad {
                Label(universidad, systemImage: "building.columns")
                    .font(Theme.Font.footnote)
                    .foregroundStyle(Theme.Color.textMuted)
            }
            if let carrera = user.nombreCarrera {
                Text(carrera)
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textSoft)
            }

            if user.esAdministrador || user.esEvaluador {
                HStack(spacing: Theme.Spacing.xs) {
                    if user.esAdministrador {
                        StatusPill(text: "Administrador", tint: Theme.Color.primary, icon: "shield")
                    }
                    if user.esEvaluador {
                        StatusPill(text: "Evaluador", tint: Theme.Color.amber, icon: "checkmark.seal")
                    }
                }
            }
        }
        .frame(maxWidth: .infinity)
        .cardSurface()
    }

    @ViewBuilder
    private func statsCard(viewModel: ProfileViewModel) -> some View {
        if let profile = viewModel.profile {
            HStack {
                profileMetric(
                    value: "\(profile.trabajosCompletados)",
                    label: "Trabajos completados"
                )
                Divider().frame(height: 40).overlay(Theme.Color.border)
                profileMetric(
                    value: "\(profile.publicaciones)",
                    label: "Publicaciones"
                )
                Divider().frame(height: 40).overlay(Theme.Color.border)
                profileMetric(
                    value: profile.miembroDesde
                        .map { DisplayFormatter.dateTime($0).prefix(10).description } ?? "—",
                    label: "Miembro desde"
                )
            }
            .cardSurface()
        }
    }

    private func profileMetric(value: String, label: String) -> some View {
        VStack(spacing: 3) {
            Text(value)
                .font(Theme.Font.headline.monospacedDigit())
                .foregroundStyle(Theme.Color.text)
            Text(label)
                .font(Theme.Font.caption)
                .foregroundStyle(Theme.Color.textMuted)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }

    private func shortcutsCard(user: AuthenticatedUser) -> some View {
        VStack(spacing: 0) {
            NavigationLink { WalletView() } label: {
                shortcutRow(icon: "wallet.pass", title: "Billetera",
                            subtitle: "Balance, pagos y disputas")
            }
            .buttonStyle(.plain)

            Divider().overlay(Theme.Color.border)

            NavigationLink { MyReportsView() } label: {
                shortcutRow(icon: "flag", title: "Mis reportes",
                            subtitle: "Seguimiento de lo que has reportado")
            }
            .buttonStyle(.plain)

            Divider().overlay(Theme.Color.border)

            NavigationLink { SettingsView() } label: {
                shortcutRow(icon: "gearshape", title: "Ajustes",
                            subtitle: "Cuenta, seguridad y sesión")
            }
            .buttonStyle(.plain)

            if user.esAdministrador {
                Divider().overlay(Theme.Color.border)
                NavigationLink { AdminView() } label: {
                    shortcutRow(icon: "shield.lefthalf.filled", title: "Administración",
                                subtitle: "Reportes, disputas y verificaciones")
                }
                .buttonStyle(.plain)
            }
        }
        .cardSurface(padding: 0)
    }

    private func shortcutRow(icon: String, title: String, subtitle: String) -> some View {
        HStack(spacing: Theme.Spacing.sm) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(Theme.Color.primary)
                .frame(width: 30)

            VStack(alignment: .leading, spacing: 1) {
                Text(title)
                    .font(Theme.Font.headline)
                    .foregroundStyle(Theme.Color.text)
                Text(subtitle)
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textMuted)
            }

            Spacer(minLength: 0)

            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
                .foregroundStyle(Theme.Color.textSoft)
        }
        .padding(Theme.Spacing.md)
        .contentShape(Rectangle())
    }

    private func verificationCard(viewModel: ProfileViewModel) -> some View {
        VStack(spacing: 0) {
            NavigationLink {
                StudentVerificationView(viewModel: viewModel)
            } label: {
                verificationRow(
                    icon: "graduationcap",
                    title: "Verificación estudiantil",
                    status: viewModel.studentVerification?.estadoLegible ?? "No solicitada",
                    tint: statusTint(viewModel.studentVerification?.estado)
                )
            }
            .buttonStyle(.plain)

            Divider().overlay(Theme.Color.border)

            NavigationLink {
                IdentityVerificationView(viewModel: viewModel)
            } label: {
                verificationRow(
                    icon: "person.text.rectangle",
                    title: "Verificación de identidad",
                    status: viewModel.identityStatus?.estadoLegible ?? "No iniciada",
                    tint: viewModel.identityStatus?.verificada == true
                        ? Theme.Color.success
                        : Theme.Color.textMuted
                )
            }
            .buttonStyle(.plain)
        }
        .cardSurface(padding: 0)
    }

    private func verificationRow(
        icon: String,
        title: String,
        status: String,
        tint: Color
    ) -> some View {
        HStack(spacing: Theme.Spacing.sm) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(Theme.Color.primary)
                .frame(width: 30)

            Text(title)
                .font(Theme.Font.headline)
                .foregroundStyle(Theme.Color.text)

            Spacer(minLength: Theme.Spacing.xs)

            StatusPill(text: status, tint: tint)

            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
                .foregroundStyle(Theme.Color.textSoft)
        }
        .padding(Theme.Spacing.md)
        .contentShape(Rectangle())
    }

    private func statusTint(_ estado: String?) -> Color {
        switch estado {
        case Domain.EstadoVerificacionEstudiantil.aprobada: Theme.Color.success
        case Domain.EstadoVerificacionEstudiantil.pendiente: Theme.Color.warning
        case Domain.EstadoVerificacionEstudiantil.rechazada: Theme.Color.danger
        default: Theme.Color.textMuted
        }
    }
}

/// Cambio del nombre de usuario público, con el bloqueo de 30 días del backend.
struct UsernameEditorSheet: View {
    let current: String
    let nextChangeAllowedAt: Date?
    let onSubmit: (String) async -> Bool

    @Environment(\.dismiss) private var dismiss
    @State private var nuevo = ""

    private var isBlocked: Bool {
        guard let nextChangeAllowedAt else { return false }
        return nextChangeAllowedAt > Date()
    }

    private var isValid: Bool {
        Validation.isValidUsername(nuevo) && nuevo != current && !isBlocked
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: Theme.Spacing.md) {
                if isBlocked, let nextChangeAllowedAt {
                    InlineErrorBanner(
                        message: "Podrás cambiarlo de nuevo a partir del "
                            + DisplayFormatter.dateTime(nextChangeAllowedAt) + "."
                    )
                }

                LabeledField(
                    label: "Nombre de usuario",
                    hint: "Entre 3 y 30 caracteres: letras, números, punto o guion bajo.",
                    error: nuevo.isEmpty || Validation.isValidUsername(nuevo)
                        ? nil
                        : "Ese formato no es válido."
                ) {
                    HStack {
                        Text("@").foregroundStyle(Theme.Color.textMuted)
                        TextField("tuusuario", text: $nuevo)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                    }
                }

                Text("Tu nombre de usuario es público: aparece en tus publicaciones, "
                     + "postulaciones y conversaciones. Solo puedes cambiarlo cada 30 días.")
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textSoft)

                Spacer()

                AsyncButton {
                    if await onSubmit(nuevo) { dismiss() }
                } label: {
                    Text("Guardar")
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(!isValid)
            }
            .padding(Theme.Spacing.md)
            .screenBackground()
            .navigationTitle("Nombre de usuario")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancelar") { dismiss() }
                }
            }
            .onAppear {
                if nuevo.isEmpty {
                    nuevo = current.hasPrefix("@") ? String(current.dropFirst()) : current
                }
            }
        }
    }
}
