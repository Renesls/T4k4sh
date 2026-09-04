import CoreLocation
import Foundation
import Observation

/// Acceso a la ubicación del dispositivo.
///
/// Android declara `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`; en iOS el
/// equivalente es `NSLocationWhenInUseUsageDescription` más la autorización en
/// tiempo de ejecución que se pide aquí.
@MainActor
@Observable
final class LocationService: NSObject {
    /// Resultado de pedir la ubicación, incluyendo los casos de permiso denegado.
    enum LocationOutcome: Equatable {
        case coordinate(CLLocationCoordinate2D)
        case denied
        case restricted
        case failed(String)

        static func == (lhs: LocationOutcome, rhs: LocationOutcome) -> Bool {
            switch (lhs, rhs) {
            case let (.coordinate(a), .coordinate(b)):
                a.latitude == b.latitude && a.longitude == b.longitude
            case (.denied, .denied), (.restricted, .restricted):
                true
            case let (.failed(a), .failed(b)):
                a == b
            default:
                false
            }
        }
    }

    private let manager = CLLocationManager()
    private var continuation: CheckedContinuation<LocationOutcome, Never>?

    private(set) var authorizationStatus: CLAuthorizationStatus
    /// Última ubicación conocida, útil para centrar el mapa sin volver a pedirla.
    private(set) var lastKnownCoordinate: CLLocationCoordinate2D?

    override init() {
        authorizationStatus = manager.authorizationStatus
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
    }

    var isDenied: Bool {
        authorizationStatus == .denied || authorizationStatus == .restricted
    }

    /// Pide la ubicación actual, solicitando permiso si aún no se ha decidido.
    /// Nunca lanza: los rechazos se devuelven como caso del resultado para que
    /// la interfaz pueda explicarlos.
    func requestCurrentLocation() async -> LocationOutcome {
        switch manager.authorizationStatus {
        case .denied: return .denied
        case .restricted: return .restricted
        default: break
        }

        // Una petición a la vez: si ya hay una en curso, se reutiliza su resultado.
        if continuation != nil { return .failed("Ya hay una solicitud de ubicación en curso.") }

        return await withCheckedContinuation { continuation in
            self.continuation = continuation
            if manager.authorizationStatus == .notDetermined {
                manager.requestWhenInUseAuthorization()
            } else {
                manager.requestLocation()
            }
        }
    }

    private func finish(_ outcome: LocationOutcome) {
        continuation?.resume(returning: outcome)
        continuation = nil
    }
}

extension LocationService: CLLocationManagerDelegate {
    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        Task { @MainActor in
            self.authorizationStatus = status
            switch status {
            case .authorizedWhenInUse, .authorizedAlways:
                // Solo continúa si había una petición esperando el permiso.
                if self.continuation != nil { self.manager.requestLocation() }
            case .denied:
                self.finish(.denied)
            case .restricted:
                self.finish(.restricted)
            case .notDetermined:
                break
            @unknown default:
                self.finish(.failed("Estado de autorización desconocido."))
            }
        }
    }

    nonisolated func locationManager(
        _ manager: CLLocationManager,
        didUpdateLocations locations: [CLLocation]
    ) {
        guard let coordinate = locations.last?.coordinate else { return }
        Task { @MainActor in
            self.lastKnownCoordinate = coordinate
            self.finish(.coordinate(coordinate))
        }
    }

    nonisolated func locationManager(
        _ manager: CLLocationManager,
        didFailWithError error: Error
    ) {
        Task { @MainActor in
            self.finish(.failed("No pudimos obtener tu ubicación. \(error.localizedDescription)"))
        }
    }
}
