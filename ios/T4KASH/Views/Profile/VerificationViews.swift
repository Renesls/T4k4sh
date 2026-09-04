import SwiftUI

/// Verificación estudiantil: subida de comprobante y estado de la revisión.
struct StudentVerificationView: View {
    @Bindable var viewModel: ProfileViewModel

    @Environment(AppDependencies.self) private var dependencies

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Theme.Spacing.md) {
                if let error = viewModel.actionError {
                    InlineErrorBanner(message: error) { viewModel.actionError = nil }
                }
                if let success = viewModel.successMessage {
                    InlineSuccessBanner(message: success)
                }

                VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
                    SectionHeader(
                        "Estado",
                        subtitle: "La revisa el equipo de T4KASH manualmente."
                    )

                    if let verification = viewModel.studentVerification {
                        StatusPill(
                            text: verification.estadoLegible,
                            tint: tint(for: verification.estado)
                        )

                        if let observacion = verification.observacion, !observacion.isEmpty {
                            Text(observacion)
                                .font(Theme.Font.subheadline)
                                .foregroundStyle(Theme.Color.textMuted)
                        }

                        DetailRow(
                            label: "Solicitada",
                            value: DisplayFormatter.dateTime(verification.fechaSolicitud),
                            icon: "calendar"
                        )
                    } else {
                        Text("Todavía no has enviado un comprobante.")
                            .font(Theme.Font.subheadline)
                            .foregroundStyle(Theme.Color.textMuted)
                    }
                }
                .cardSurface()

                VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
                    SectionHeader(
                        "Comprobante",
                        subtitle: "Carnet universitario, constancia de matrícula o documento equivalente."
                    )

                    if let archivos = viewModel.studentVerification?.archivos, !archivos.isEmpty {
                        AttachmentList(
                            attachments: archivos,
                            repository: dependencies.attachments
                        )
                    }

                    AttachmentPickerMenu(title: "Subir comprobante") { prepared in
                        await viewModel.uploadStudentProof(prepared)
                    }

                    Text("Formatos aceptados: PDF, PNG, JPG o WEBP, hasta "
                         + DisplayFormatter.fileSize(Int64(AppConfig.maxAttachmentBytes)) + ".")
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textSoft)
                }
                .cardSurface()

                VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
                    Label("¿Para qué sirve?", systemImage: "info.circle")
                        .font(Theme.Font.headline)
                        .foregroundStyle(Theme.Color.text)
                    Text("La verificación estudiantil muestra un distintivo en tu perfil "
                         + "público y da confianza a quienes publican oportunidades.")
                        .font(Theme.Font.subheadline)
                        .foregroundStyle(Theme.Color.textMuted)
                }
                .cardSurface()
            }
            .padding(Theme.Spacing.md)
        }
        .screenBackground()
        .navigationTitle("Verificación estudiantil")
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.load() }
    }

    private func tint(for estado: String) -> Color {
        switch estado {
        case Domain.EstadoVerificacionEstudiantil.aprobada: Theme.Color.success
        case Domain.EstadoVerificacionEstudiantil.rechazada: Theme.Color.danger
        default: Theme.Color.warning
        }
    }
}

/// Verificación de identidad (KYC) con Didit.
///
/// El backend crea la sesión y devuelve una URL alojada por el proveedor. Didit
/// no publica SDK para iOS en este flujo, así que se abre en Safari y al volver
/// se consulta el estado real contra la API.
struct IdentityVerificationView: View {
    @Bindable var viewModel: ProfileViewModel

    @State private var destination: WebDestination?
    @State private var isStarting = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Theme.Spacing.md) {
                if let error = viewModel.actionError {
                    InlineErrorBanner(message: error) { viewModel.actionError = nil }
                }

                VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
                    HStack {
                        SectionHeader("Estado")
                        Spacer()
                        StatusPill(
                            text: viewModel.identityStatus?.estadoLegible ?? "No iniciada",
                            tint: viewModel.identityStatus?.verificada == true
                                ? Theme.Color.success
                                : Theme.Color.warning
                        )
                    }

                    if let mensaje = viewModel.identityStatus?.mensaje, !mensaje.isEmpty {
                        Text(mensaje)
                            .font(Theme.Font.subheadline)
                            .foregroundStyle(Theme.Color.textMuted)
                    }

                    if let status = viewModel.identityStatus {
                        if let inicio = status.fechaInicio {
                            DetailRow(
                                label: "Iniciada",
                                value: DisplayFormatter.dateTime(inicio),
                                icon: "play.circle"
                            )
                        }
                        if let decision = status.fechaDecision {
                            DetailRow(
                                label: "Resuelta",
                                value: DisplayFormatter.dateTime(decision),
                                icon: "checkmark.circle"
                            )
                        }
                        if let expira = status.fechaExpiracion {
                            DetailRow(
                                label: "Vence",
                                value: DisplayFormatter.dateTime(expira),
                                icon: "clock.badge.exclamationmark"
                            )
                        }
                    }
                }
                .cardSurface()

                VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
                    Label("Qué te pedirán", systemImage: "list.bullet.clipboard")
                        .font(Theme.Font.headline)
                        .foregroundStyle(Theme.Color.text)

                    Text("El proceso lo realiza Didit, nuestro proveedor de verificación: "
                         + "una foto de tu documento, una prueba de vida y la coincidencia "
                         + "facial. T4KASH solo conserva el resultado, nunca tu documento.")
                        .font(Theme.Font.subheadline)
                        .foregroundStyle(Theme.Color.textMuted)
                }
                .cardSurface()

                VStack(spacing: Theme.Spacing.sm) {
                    if viewModel.identityStatus?.verificada == true {
                        Label("Tu identidad está verificada", systemImage: "checkmark.seal.fill")
                            .font(Theme.Font.headline)
                            .foregroundStyle(Theme.Color.success)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, Theme.Spacing.sm)
                    } else {
                        AsyncButton {
                            isStarting = true
                            destination = await viewModel.startIdentityVerification()
                            isStarting = false
                        } label: {
                            Text(viewModel.identityStatus?.idVerificacion == nil
                                 ? "Comenzar verificación"
                                 : "Reintentar verificación")
                        }
                        .buttonStyle(PrimaryButtonStyle(isLoading: isStarting))
                    }

                    AsyncButton("Actualizar estado") {
                        await viewModel.refreshIdentityStatus()
                    }
                    .buttonStyle(SecondaryButtonStyle())
                }
            }
            .padding(Theme.Spacing.md)
        }
        .screenBackground()
        .navigationTitle("Verificación de identidad")
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.load() }
        .sheet(item: $destination) { destination in
            SafariView(url: destination.url) {
                // Al cerrar el navegador, el estado real lo tiene el backend.
                Task { await viewModel.refreshIdentityStatus() }
            }
            .ignoresSafeArea()
        }
    }
}
