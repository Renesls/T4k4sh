import SwiftUI

/// Ajustes de cuenta y cierre de sesión.
struct SettingsView: View {
    @Environment(AppDependencies.self) private var dependencies
    @Environment(SessionStore.self) private var session

    @State private var confirmLogout = false

    var body: some View {
        Form {
            if let user = session.user {
                Section("Cuenta") {
                    LabeledContent("Nombre", value: user.nombreCompleto)
                    LabeledContent("Correo", value: user.correo)
                    if !user.arroba.isEmpty {
                        LabeledContent("Usuario", value: user.arroba)
                    }
                    LabeledContent("Estado", value: user.estadoUsuario.humanizedCode)
                }

                if user.nombreUniversidad != nil || user.nombreCarrera != nil {
                    Section("Institución") {
                        if let universidad = user.nombreUniversidad {
                            LabeledContent("Universidad", value: universidad)
                        }
                        if let carrera = user.nombreCarrera {
                            LabeledContent("Carrera", value: carrera)
                        }
                    }
                }

                if !user.roles.isEmpty {
                    Section("Roles") {
                        ForEach(user.roles, id: \.self) { rol in
                            Label(rol.humanizedCode, systemImage: "person.badge.shield.checkmark")
                        }
                    }
                }
            }

            Section {
                if let expiresAt = session.expiresAt {
                    LabeledContent("La sesión vence", value: DisplayFormatter.dateTime(expiresAt))
                }
            } header: {
                Text("Seguridad")
            } footer: {
                Text("Tu sesión se guarda cifrada en el Llavero del dispositivo. "
                     + "Al cerrar sesión se elimina de aquí y se revoca en el servidor.")
            }

            Section {
                LabeledContent("Servidor", value: AppConfig.apiBaseURL.host() ?? "—")
                LabeledContent(
                    "Versión",
                    value: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
                )
            } header: {
                Text("Aplicación")
            }

            Section {
                Button("Cerrar sesión", role: .destructive) {
                    confirmLogout = true
                }
            }
        }
        .navigationTitle("Ajustes")
        .navigationBarTitleDisplayMode(.inline)
        .alert("¿Cerrar sesión?", isPresented: $confirmLogout) {
            Button("Cancelar", role: .cancel) {}
            Button("Cerrar sesión", role: .destructive) {
                Task {
                    try? await dependencies.auth.logout()
                    session.clear()
                }
            }
        } message: {
            Text("Tendrás que volver a iniciar sesión con tu correo y un código de acceso.")
        }
    }
}

/// Perfil público de otra persona.
struct PublicProfileView: View {
    let username: String

    @Environment(AppDependencies.self) private var dependencies

    @State private var state: LoadState<PublicProfile> = .idle

    var body: some View {
        Group {
            switch state {
            case .idle, .loading:
                LoadingStateView()

            case let .failed(message):
                ErrorStateView(message: message) { await load() }

            case let .loaded(profile):
                ScrollView {
                    VStack(spacing: Theme.Spacing.md) {
                        VStack(spacing: Theme.Spacing.sm) {
                            InitialsAvatar(
                                initials: profile.identidad.iniciales,
                                size: 84,
                                verified: profile.identidad.estudianteVerificado
                            )

                            Text(profile.identidad.nombreCompleto)
                                .font(Theme.Font.title)
                                .foregroundStyle(Theme.Color.text)

                            if !profile.identidad.arroba.isEmpty {
                                Text(profile.identidad.arroba)
                                    .font(Theme.Font.subheadline)
                                    .foregroundStyle(Theme.Color.primary)
                            }

                            if profile.identidad.estudianteVerificado {
                                StatusPill(
                                    text: "Estudiante verificado",
                                    tint: Theme.Color.success,
                                    icon: "checkmark.seal.fill"
                                )
                            }

                            if let universidad = profile.identidad.nombreUniversidad {
                                Label(universidad, systemImage: "building.columns")
                                    .font(Theme.Font.footnote)
                                    .foregroundStyle(Theme.Color.textMuted)
                            }
                            if let carrera = profile.identidad.nombreCarrera {
                                Text(carrera)
                                    .font(Theme.Font.caption)
                                    .foregroundStyle(Theme.Color.textSoft)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .cardSurface()

                        HStack {
                            VStack(spacing: 3) {
                                Text("\(profile.trabajosCompletados)")
                                    .font(Theme.Font.headline.monospacedDigit())
                                    .foregroundStyle(Theme.Color.text)
                                Text("Trabajos completados")
                                    .font(Theme.Font.caption)
                                    .foregroundStyle(Theme.Color.textMuted)
                                    .multilineTextAlignment(.center)
                            }
                            .frame(maxWidth: .infinity)

                            Divider().frame(height: 40).overlay(Theme.Color.border)

                            VStack(spacing: 3) {
                                Text("\(profile.publicaciones)")
                                    .font(Theme.Font.headline.monospacedDigit())
                                    .foregroundStyle(Theme.Color.text)
                                Text("Publicaciones")
                                    .font(Theme.Font.caption)
                                    .foregroundStyle(Theme.Color.textMuted)
                            }
                            .frame(maxWidth: .infinity)
                        }
                        .cardSurface()

                        if let miembroDesde = profile.miembroDesde {
                            HStack {
                                Label("Miembro desde", systemImage: "calendar")
                                    .font(Theme.Font.footnote)
                                    .foregroundStyle(Theme.Color.textMuted)
                                Spacer()
                                Text(DisplayFormatter.dateTime(miembroDesde))
                                    .font(Theme.Font.footnote)
                                    .foregroundStyle(Theme.Color.text)
                            }
                            .cardSurface()
                        }

                        Text("El perfil público no muestra correo, carnet ni datos privados de la cuenta.")
                            .font(Theme.Font.caption)
                            .foregroundStyle(Theme.Color.textSoft)
                            .multilineTextAlignment(.center)
                    }
                    .padding(Theme.Spacing.md)
                }
                .refreshable { await load() }
            }
        }
        .screenBackground()
        .navigationTitle("Perfil")
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    private func load() async {
        guard !username.isEmpty else {
            state = .failed("Esta persona todavía no tiene un nombre de usuario público.")
            return
        }
        if state.value == nil { state = .loading }
        do {
            state = .loaded(try await dependencies.auth.publicProfile(username: username))
        } catch {
            state = .failed(ErrorPresenter.message(for: error))
        }
    }
}

/// Reportes enviados por el usuario.
struct MyReportsView: View {
    @Environment(AppDependencies.self) private var dependencies

    @State private var state: LoadState<[Report]> = .idle

    var body: some View {
        Group {
            switch state {
            case .idle, .loading:
                LoadingStateView()

            case let .failed(message):
                ErrorStateView(message: message) { await load() }

            case let .loaded(reports):
                if reports.isEmpty {
                    EmptyStateView(
                        icon: "flag",
                        title: "No has enviado reportes",
                        message: "Si encuentras una publicación sospechosa, repórtala desde su detalle."
                    )
                } else {
                    ScrollView {
                        LazyVStack(spacing: Theme.Spacing.xs) {
                            ForEach(reports) { report in
                                VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
                                    HStack {
                                        Text(report.categoriaLegible)
                                            .font(Theme.Font.headline)
                                            .foregroundStyle(Theme.Color.text)
                                        Spacer(minLength: 0)
                                        StatusPill(
                                            text: report.estadoLegible,
                                            tint: report.pendiente
                                                ? Theme.Color.warning
                                                : Theme.Color.success
                                        )
                                    }

                                    if let titulo = report.tituloTarea {
                                        Text(titulo)
                                            .font(Theme.Font.subheadline)
                                            .foregroundStyle(Theme.Color.textMuted)
                                    }

                                    if let descripcion = report.descripcion, !descripcion.isEmpty {
                                        Text(descripcion)
                                            .font(Theme.Font.caption)
                                            .foregroundStyle(Theme.Color.textSoft)
                                    }

                                    Text(DisplayFormatter.dateTime(report.fechaReporte))
                                        .font(Theme.Font.caption)
                                        .foregroundStyle(Theme.Color.textSoft)
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .cardSurface(padding: Theme.Spacing.sm)
                            }
                        }
                        .padding(Theme.Spacing.md)
                    }
                    .refreshable { await load() }
                }
            }
        }
        .screenBackground()
        .navigationTitle("Mis reportes")
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    private func load() async {
        if state.value == nil { state = .loading }
        do {
            let reports = try await dependencies.moderation.myReports()
            state = .loaded(
                reports.sorted { ($0.fechaReporte ?? .distantPast) > ($1.fechaReporte ?? .distantPast) }
            )
        } catch {
            state = .failed(ErrorPresenter.message(for: error))
        }
    }
}
