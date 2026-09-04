import SafariServices
import SwiftUI

/// Presenta una URL en `SFSafariViewController`.
///
/// Es la pieza que reemplaza a los SDK nativos: tanto el checkout de Pagadito
/// (`checkoutUrl`) como la verificación de identidad de Didit (`urlVerificacion`)
/// son flujos web que entrega el backend. Al cerrarse se avisa para poder
/// refrescar el estado real contra la API.
struct SafariView: UIViewControllerRepresentable {
    let url: URL
    var onFinish: () -> Void = {}

    func makeCoordinator() -> Coordinator { Coordinator(onFinish: onFinish) }

    func makeUIViewController(context: Context) -> SFSafariViewController {
        let configuration = SFSafariViewController.Configuration()
        configuration.entersReaderIfAvailable = false
        configuration.barCollapsingEnabled = false

        let controller = SFSafariViewController(url: url, configuration: configuration)
        controller.delegate = context.coordinator
        controller.dismissButtonStyle = .close
        controller.preferredControlTintColor = UIColor(Theme.Color.primary)
        return controller
    }

    func updateUIViewController(_ controller: SFSafariViewController, context: Context) {}

    final class Coordinator: NSObject, SFSafariViewControllerDelegate {
        private let onFinish: () -> Void

        init(onFinish: @escaping () -> Void) { self.onFinish = onFinish }

        func safariViewControllerDidFinish(_ controller: SFSafariViewController) {
            onFinish()
        }
    }
}

/// Envuelve una URL para poder usarla con `.sheet(item:)`.
struct WebDestination: Identifiable {
    let id = UUID()
    let url: URL

    init?(_ raw: String) {
        guard let url = URL(string: raw), url.scheme?.hasPrefix("http") == true else {
            return nil
        }
        self.url = url
    }
}
