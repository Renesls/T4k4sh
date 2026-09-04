import SwiftUI

/// Botón que ejecuta trabajo asíncrono y se bloquea mientras dura.
///
/// Evita el doble envío en acciones sensibles (postular, aceptar, pagar) sin
/// que cada pantalla tenga que llevar su propio indicador.
struct AsyncButton<Label: View>: View {
    private let role: ButtonRole?
    private let action: () async -> Void
    private let label: () -> Label

    @State private var isRunning = false

    init(
        role: ButtonRole? = nil,
        action: @escaping () async -> Void,
        @ViewBuilder label: @escaping () -> Label
    ) {
        self.role = role
        self.action = action
        self.label = label
    }

    var body: some View {
        Button(role: role) {
            guard !isRunning else { return }
            isRunning = true
            Task {
                await action()
                isRunning = false
            }
        } label: {
            ZStack {
                label().opacity(isRunning ? 0 : 1)
                if isRunning {
                    ProgressView().tint(.white)
                }
            }
        }
        .disabled(isRunning)
    }
}

extension AsyncButton where Label == Text {
    init(_ title: String, role: ButtonRole? = nil, action: @escaping () async -> Void) {
        self.init(role: role, action: action) { Text(title) }
    }
}
