import SwiftUI

/// Avatar con iniciales. La API no expone fotos de perfil, así que la identidad
/// visual se resuelve con las iniciales sobre el color de producto.
struct InitialsAvatar: View {
    let initials: String
    var size: CGFloat = 44
    var verified: Bool = false

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            Circle()
                .fill(Theme.Color.primaryContainer)
                .frame(width: size, height: size)
                .overlay(
                    Text(initials)
                        .font(.system(size: size * 0.38, weight: .bold, design: .rounded))
                        .foregroundStyle(Theme.Color.primaryDark)
                )

            if verified {
                Image(systemName: "checkmark.seal.fill")
                    .font(.system(size: size * 0.30))
                    .foregroundStyle(Theme.Color.success)
                    .background(Circle().fill(Theme.Color.surface).frame(
                        width: size * 0.30, height: size * 0.30
                    ))
                    .offset(x: 2, y: 2)
            }
        }
        .accessibilityHidden(true)
    }
}

/// Etiqueta de estado en forma de píldora.
struct StatusPill: View {
    let text: String
    var tint: Color = Theme.Color.primary
    var icon: String?

    var body: some View {
        HStack(spacing: 4) {
            if let icon {
                Image(systemName: icon).font(.system(size: 10, weight: .semibold))
            }
            Text(text)
        }
        .font(Theme.Font.captionEmphasis)
        .foregroundStyle(tint)
        .padding(.horizontal, Theme.Spacing.xs)
        .padding(.vertical, 5)
        .background(
            Capsule().fill(tint.opacity(0.12))
        )
    }
}

/// Color asociado a los estados de dominio, para que el mismo estado se vea
/// igual en todas las pantallas.
enum StatusTint {
    static func task(_ estado: String) -> Color {
        switch estado {
        case Domain.EstadoTarea.publicada: Theme.Color.success
        case Domain.EstadoTarea.asignada: Theme.Color.primary
        case Domain.EstadoTarea.cerrada: Theme.Color.textMuted
        case Domain.EstadoTarea.cancelada: Theme.Color.danger
        default: Theme.Color.textMuted
        }
    }

    static func application(_ estado: String) -> Color {
        switch estado {
        case Domain.EstadoPostulacion.pendiente: Theme.Color.warning
        case Domain.EstadoPostulacion.aceptada: Theme.Color.success
        case Domain.EstadoPostulacion.rechazada, Domain.EstadoPostulacion.canceladaTarea:
            Theme.Color.danger
        default: Theme.Color.textMuted
        }
    }

    static func job(_ estado: String) -> Color {
        switch estado {
        case Domain.EstadoTrabajo.pendientePago: Theme.Color.warning
        case Domain.EstadoTrabajo.enProceso: Theme.Color.primary
        case Domain.EstadoTrabajo.finalizado: Theme.Color.success
        default: Theme.Color.textMuted
        }
    }

    static func delivery(_ estado: String) -> Color {
        switch estado {
        case Domain.EstadoEntrega.enviada: Theme.Color.primary
        case Domain.EstadoEntrega.aprobada: Theme.Color.success
        case Domain.EstadoEntrega.cambiosSolicitados: Theme.Color.warning
        default: Theme.Color.textMuted
        }
    }

    static func payment(_ estado: String) -> Color {
        switch estado {
        case Domain.EstadoPago.fondosRetenidos: Theme.Color.primary
        case Domain.EstadoPago.liberado: Theme.Color.success
        case Domain.EstadoPago.pendientePago: Theme.Color.warning
        case Domain.EstadoPago.enDisputa: Theme.Color.warning
        case Domain.EstadoPago.pagoFallido, Domain.EstadoPago.pagoCancelado,
             Domain.EstadoPago.pagoExpirado, Domain.EstadoPago.pagoRevocado:
            Theme.Color.danger
        case Domain.EstadoPago.reembolsado: Theme.Color.textMuted
        default: Theme.Color.textMuted
        }
    }
}

/// Fila etiqueta / valor usada en detalles.
struct DetailRow: View {
    let label: String
    let value: String
    var icon: String?
    var valueColor: Color = Theme.Color.text

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: Theme.Spacing.xs) {
            if let icon {
                Image(systemName: icon)
                    .font(.footnote)
                    .foregroundStyle(Theme.Color.textSoft)
                    .frame(width: 18)
            }
            Text(label)
                .font(Theme.Font.footnote)
                .foregroundStyle(Theme.Color.textMuted)
            Spacer(minLength: Theme.Spacing.xs)
            Text(value)
                .font(Theme.Font.subheadline)
                .foregroundStyle(valueColor)
                .multilineTextAlignment(.trailing)
        }
    }
}

/// Encabezado de sección dentro de una pantalla con scroll.
struct SectionHeader: View {
    let title: String
    var subtitle: String?
    var trailing: AnyView?

    init(_ title: String, subtitle: String? = nil, trailing: AnyView? = nil) {
        self.title = title
        self.subtitle = subtitle
        self.trailing = trailing
    }

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(Theme.Font.headline)
                    .foregroundStyle(Theme.Color.text)
                if let subtitle {
                    Text(subtitle)
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textMuted)
                }
            }
            Spacer(minLength: Theme.Spacing.xs)
            trailing
        }
    }
}

/// Fila de chips seleccionables con desplazamiento horizontal.
struct ChipRow<Item: Identifiable & Equatable>: View {
    let items: [Item]
    let title: (Item) -> String
    @Binding var selection: Item?
    var allowsDeselect = true

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Theme.Spacing.xs) {
                ForEach(items) { item in
                    let isSelected = selection == item
                    Button {
                        if isSelected && allowsDeselect {
                            selection = nil
                        } else {
                            selection = item
                        }
                    } label: {
                        Text(title(item))
                            .font(Theme.Font.footnote.weight(.medium))
                            .foregroundStyle(isSelected ? .white : Theme.Color.text)
                            .padding(.horizontal, Theme.Spacing.sm)
                            .padding(.vertical, Theme.Spacing.xs)
                            .background(
                                Capsule().fill(
                                    isSelected ? Theme.Color.primary : Theme.Color.surface
                                )
                            )
                            .overlay(
                                Capsule().stroke(
                                    isSelected ? Color.clear : Theme.Color.border,
                                    lineWidth: 1
                                )
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, Theme.Spacing.md)
        }
        .scrollClipDisabled()
    }
}

/// Campo de texto con etiqueta y mensaje de validación.
struct LabeledField<Content: View>: View {
    let label: String
    var hint: String?
    var error: String?
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xxs) {
            Text(label)
                .font(Theme.Font.captionEmphasis)
                .foregroundStyle(Theme.Color.textMuted)

            content()
                .padding(Theme.Spacing.sm)
                .background(
                    RoundedRectangle(cornerRadius: Theme.Radius.sm, style: .continuous)
                        .fill(Theme.Color.surface)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: Theme.Radius.sm, style: .continuous)
                        .stroke(error == nil ? Theme.Color.border : Theme.Color.danger,
                                lineWidth: 1)
                )

            if let error {
                Text(error)
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.danger)
            } else if let hint {
                Text(hint)
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textSoft)
            }
        }
    }
}

/// Campo para códigos de 6 dígitos (verificación de correo, 2FA, recuperación).
struct VerificationCodeField: View {
    @Binding var code: String
    var isDisabled = false

    var body: some View {
        TextField("000000", text: $code)
            .font(.system(.title, design: .monospaced).weight(.bold))
            .multilineTextAlignment(.center)
            .keyboardType(.numberPad)
            .textContentType(.oneTimeCode)
            .disabled(isDisabled)
            .onChange(of: code) { _, newValue in
                // El backend valida `\d{6}`; se filtra en el cliente para no gastar intentos.
                let digits = newValue.filter(\.isNumber)
                let limited = String(digits.prefix(6))
                if limited != newValue { code = limited }
            }
            .padding(Theme.Spacing.sm)
            .background(
                RoundedRectangle(cornerRadius: Theme.Radius.md, style: .continuous)
                    .fill(Theme.Color.surface)
            )
            .overlay(
                RoundedRectangle(cornerRadius: Theme.Radius.md, style: .continuous)
                    .stroke(Theme.Color.border, lineWidth: 1)
            )
    }
}

/// Hoja del sistema para compartir o guardar un archivo descargado.
struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}
