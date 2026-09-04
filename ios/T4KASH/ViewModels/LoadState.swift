import Foundation

/// Estado de carga de una pantalla.
///
/// Obliga a que cada vista contemple carga, vacío, error y contenido, en vez de
/// mostrar una lista en blanco cuando algo falla.
enum LoadState<Value: Equatable>: Equatable {
    case idle
    case loading
    case loaded(Value)
    case failed(String)

    var value: Value? {
        if case let .loaded(value) = self { return value }
        return nil
    }

    var isLoading: Bool {
        if case .loading = self { return true }
        return false
    }

    var errorMessage: String? {
        if case let .failed(message) = self { return message }
        return nil
    }
}

extension LoadState where Value: Collection {
    /// `true` cuando la carga terminó bien pero no hay nada que mostrar.
    var isEmpty: Bool {
        if case let .loaded(value) = self { return value.isEmpty }
        return false
    }
}

/// Traduce cualquier error a un mensaje presentable.
enum ErrorPresenter {
    static func message(for error: Error) -> String {
        if let apiError = error as? APIError {
            return apiError.errorDescription ?? "Ocurrió un error inesperado."
        }
        if let localized = error as? LocalizedError, let description = localized.errorDescription {
            return description
        }
        return error.localizedDescription
    }
}
