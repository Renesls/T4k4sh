import SwiftUI

/// Sistema de diseño de T4KASH en iOS.
///
/// Conserva la identidad visual del producto (los colores salen de
/// `mobile/.../ui/theme/Color.kt`) pero expresada con las convenciones de iOS:
/// tipografía dinámica del sistema, esquinas continuas y espaciado en múltiplos
/// de 4. La app es de tema claro, igual que Android, que solo define
/// `lightColorScheme`.
enum Theme {

    // MARK: - Color

    enum Color {
        /// Índigo de producto: acciones, énfasis y estados activos.
        static let primary = SwiftUI.Color("BrandPrimary")
        static let primarySoft = SwiftUI.Color("BrandPrimarySoft")
        static let primaryDark = SwiftUI.Color("BrandPrimaryDark")
        static let primaryContainer = SwiftUI.Color("BrandPrimaryContainer")

        /// Azul del logotipo: reservado para momentos de marca.
        static let brand = SwiftUI.Color("BrandBlue")

        static let mint = SwiftUI.Color("BrandMint")
        static let mintDark = SwiftUI.Color("BrandMintDark")
        static let amber = SwiftUI.Color("BrandAmber")
        static let amberContainer = SwiftUI.Color("BrandAmberContainer")

        static let background = SwiftUI.Color("BrandBackground")
        static let surface = SwiftUI.Color("BrandSurface")
        static let surfaceVariant = SwiftUI.Color("BrandSurfaceVariant")
        static let border = SwiftUI.Color("BrandBorder")

        static let text = SwiftUI.Color("BrandDark")
        static let textMuted = SwiftUI.Color("BrandTextMuted")
        static let textSoft = SwiftUI.Color("BrandTextSoft")

        static let success = SwiftUI.Color("BrandSuccess")
        static let warning = SwiftUI.Color("BrandOrange")
        static let danger = SwiftUI.Color("BrandDanger")
    }

    // MARK: - Espaciado

    enum Spacing {
        static let xxs: CGFloat = 4
        static let xs: CGFloat = 8
        static let sm: CGFloat = 12
        static let md: CGFloat = 16
        static let lg: CGFloat = 20
        static let xl: CGFloat = 28
        static let xxl: CGFloat = 40
    }

    // MARK: - Radio

    enum Radius {
        static let sm: CGFloat = 10
        static let md: CGFloat = 14
        static let lg: CGFloat = 20
        static let pill: CGFloat = 999
    }

    // MARK: - Tipografía

    /// Usa siempre estilos relativos para respetar Dynamic Type.
    enum Font {
        static let largeTitle = SwiftUI.Font.system(.largeTitle, design: .rounded).weight(.bold)
        static let title = SwiftUI.Font.system(.title2, design: .rounded).weight(.bold)
        static let headline = SwiftUI.Font.system(.headline, design: .rounded)
        static let body = SwiftUI.Font.system(.body)
        static let callout = SwiftUI.Font.system(.callout)
        static let subheadline = SwiftUI.Font.system(.subheadline)
        static let footnote = SwiftUI.Font.system(.footnote)
        static let caption = SwiftUI.Font.system(.caption)
        static let captionEmphasis = SwiftUI.Font.system(.caption).weight(.semibold)
        static let numeric = SwiftUI.Font.system(.title3, design: .rounded)
            .weight(.bold)
            .monospacedDigit()
    }
}

// MARK: - Estilos reutilizables

/// Botón principal de acción.
struct PrimaryButtonStyle: ButtonStyle {
    var isLoading = false
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: Theme.Spacing.xs) {
            if isLoading {
                ProgressView()
                    .progressViewStyle(.circular)
                    .tint(.white)
            }
            configuration.label
        }
        .font(Theme.Font.headline)
        .foregroundStyle(.white)
        .frame(maxWidth: .infinity)
        .frame(minHeight: 50)
        .background(
            RoundedRectangle(cornerRadius: Theme.Radius.md, style: .continuous)
                .fill(isEnabled ? Theme.Color.primary : Theme.Color.primary.opacity(0.4))
        )
        .opacity(configuration.isPressed ? 0.85 : 1)
        .scaleEffect(configuration.isPressed ? 0.99 : 1)
        .animation(.easeOut(duration: 0.15), value: configuration.isPressed)
    }
}

/// Botón secundario con borde.
struct SecondaryButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(Theme.Font.headline)
            .foregroundStyle(isEnabled ? Theme.Color.primary : Theme.Color.textSoft)
            .frame(maxWidth: .infinity)
            .frame(minHeight: 50)
            .background(
                RoundedRectangle(cornerRadius: Theme.Radius.md, style: .continuous)
                    .fill(Theme.Color.surface)
            )
            .overlay(
                RoundedRectangle(cornerRadius: Theme.Radius.md, style: .continuous)
                    .stroke(isEnabled ? Theme.Color.primary.opacity(0.35) : Theme.Color.border,
                            lineWidth: 1.5)
            )
            .opacity(configuration.isPressed ? 0.8 : 1)
    }
}

/// Botón destructivo con borde.
struct DestructiveButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(Theme.Font.headline)
            .foregroundStyle(Theme.Color.danger)
            .frame(maxWidth: .infinity)
            .frame(minHeight: 50)
            .background(
                RoundedRectangle(cornerRadius: Theme.Radius.md, style: .continuous)
                    .fill(Theme.Color.danger.opacity(0.08))
            )
            .opacity(configuration.isPressed ? 0.8 : 1)
    }
}

extension View {
    /// Tarjeta estándar: fondo, borde sutil y esquinas continuas.
    func cardSurface(padding: CGFloat = Theme.Spacing.md) -> some View {
        self
            .padding(padding)
            .background(
                RoundedRectangle(cornerRadius: Theme.Radius.lg, style: .continuous)
                    .fill(Theme.Color.surface)
            )
            .overlay(
                RoundedRectangle(cornerRadius: Theme.Radius.lg, style: .continuous)
                    .stroke(Theme.Color.border, lineWidth: 1)
            )
    }

    /// Fondo de pantalla de la aplicación.
    func screenBackground() -> some View {
        background(Theme.Color.background.ignoresSafeArea())
    }
}
