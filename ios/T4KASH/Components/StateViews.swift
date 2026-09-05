import SwiftUI

/// Indicador de carga a pantalla completa.
struct LoadingStateView: View {
    var message: String = "Cargando…"

    var body: some View {
        VStack(spacing: Theme.Spacing.sm) {
            ProgressView().tint(Theme.Color.primary)
            Text(message)
                .font(Theme.Font.footnote)
                .foregroundStyle(Theme.Color.textMuted)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(Theme.Spacing.xl)
    }
}

/// Estado vacío con acción opcional.
struct EmptyStateView: View {
    let icon: String
    let title: String
    var message: String?
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        VStack(spacing: Theme.Spacing.md) {
            Image(systemName: icon)
                .font(.system(size: 42, weight: .light))
                .foregroundStyle(Theme.Color.primary.opacity(0.55))

            Text(title)
                .font(Theme.Font.headline)
                .foregroundStyle(Theme.Color.text)
                .multilineTextAlignment(.center)

            if let message {
                Text(message)
                    .font(Theme.Font.subheadline)
                    .foregroundStyle(Theme.Color.textMuted)
                    .multilineTextAlignment(.center)
            }

            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .buttonStyle(SecondaryButtonStyle())
                    .frame(maxWidth: 260)
                    .padding(.top, Theme.Spacing.xs)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(Theme.Spacing.xl)
    }
}

/// Estado de error con reintento.
struct ErrorStateView: View {
    let message: String
    var retry: (() async -> Void)?

    var body: some View {
        VStack(spacing: Theme.Spacing.md) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 38, weight: .light))
                .foregroundStyle(Theme.Color.warning)

            Text("No pudimos cargar la información")
                .font(Theme.Font.headline)
                .foregroundStyle(Theme.Color.text)

            Text(message)
                .font(Theme.Font.subheadline)
                .foregroundStyle(Theme.Color.textMuted)
                .multilineTextAlignment(.center)

            if let retry {
                AsyncButton("Reintentar") { await retry() }
                    .buttonStyle(SecondaryButtonStyle())
                    .frame(maxWidth: 260)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(Theme.Spacing.xl)
    }
}

/// Banda de error para mostrar sobre contenido ya cargado.
struct InlineErrorBanner: View {
    let message: String
    var onDismiss: (() -> Void)?

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.xs) {
            Image(systemName: "exclamationmark.circle.fill")
                .foregroundStyle(Theme.Color.danger)

            Text(message)
                .font(Theme.Font.footnote)
                .foregroundStyle(Theme.Color.text)
                .frame(maxWidth: .infinity, alignment: .leading)

            if let onDismiss {
                Button {
                    onDismiss()
                } label: {
                    Image(systemName: "xmark")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Theme.Color.textMuted)
                }
                .accessibilityLabel("Descartar aviso")
            }
        }
        .padding(Theme.Spacing.sm)
        .background(
            RoundedRectangle(cornerRadius: Theme.Radius.sm, style: .continuous)
                .fill(Theme.Color.danger.opacity(0.08))
        )
    }
}

/// Aviso de éxito discreto.
struct InlineSuccessBanner: View {
    let message: String

    var body: some View {
        HStack(spacing: Theme.Spacing.xs) {
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(Theme.Color.success)
            Text(message)
                .font(Theme.Font.footnote)
                .foregroundStyle(Theme.Color.text)
            Spacer(minLength: 0)
        }
        .padding(Theme.Spacing.sm)
        .background(
            RoundedRectangle(cornerRadius: Theme.Radius.sm, style: .continuous)
                .fill(Theme.Color.success.opacity(0.10))
        )
    }
}
