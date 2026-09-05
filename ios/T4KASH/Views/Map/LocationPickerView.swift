import CoreLocation
import MapKit
import SwiftUI

/// Ubicación elegida en el mapa.
struct PickedLocation: Equatable {
    var coordinate: CLLocationCoordinate2D
    var address: String

    static func == (lhs: PickedLocation, rhs: PickedLocation) -> Bool {
        lhs.coordinate.latitude == rhs.coordinate.latitude
            && lhs.coordinate.longitude == rhs.coordinate.longitude
            && lhs.address == rhs.address
    }
}

/// Selector de ubicación con MapKit.
///
/// Sustituye al `TaskLocationPickerDialog` de Android (MapLibre + OpenFreeMap).
/// El pin queda fijo en el centro y el mapa se mueve debajo, que es el patrón
/// habitual en iOS y evita tocar coordenadas con precisión.
struct LocationPickerView: View {
    let initialLocation: PickedLocation?
    let onConfirm: (PickedLocation) -> Void

    @Environment(AppDependencies.self) private var dependencies
    @Environment(\.dismiss) private var dismiss

    @State private var position: MapCameraPosition
    @State private var centerCoordinate: CLLocationCoordinate2D
    @State private var address = ""
    @State private var isResolvingAddress = false
    @State private var searchText = ""
    @State private var searchResults: [MKMapItem] = []
    @State private var isSearching = false
    @State private var permissionMessage: String?

    /// Managua como centro por defecto cuando no hay ubicación ni permiso.
    private static let fallbackCenter = CLLocationCoordinate2D(
        latitude: 12.114993,
        longitude: -86.236174
    )

    private let geocoder = CLGeocoder()

    init(initialLocation: PickedLocation?, onConfirm: @escaping (PickedLocation) -> Void) {
        self.initialLocation = initialLocation
        self.onConfirm = onConfirm

        let start = initialLocation?.coordinate ?? Self.fallbackCenter
        _centerCoordinate = State(initialValue: start)
        _position = State(initialValue: .region(
            MKCoordinateRegion(
                center: start,
                span: MKCoordinateSpan(latitudeDelta: 0.02, longitudeDelta: 0.02)
            )
        ))
        _address = State(initialValue: initialLocation?.address ?? "")
    }

    var body: some View {
        NavigationStack {
            ZStack(alignment: .top) {
                mapLayer

                VStack(spacing: Theme.Spacing.xs) {
                    searchBar

                    if !searchResults.isEmpty {
                        searchResultsList
                    }

                    if let permissionMessage {
                        InlineErrorBanner(message: permissionMessage) {
                            self.permissionMessage = nil
                        }
                    }
                }
                .padding(Theme.Spacing.md)

                VStack {
                    Spacer()
                    bottomPanel
                }
            }
            .navigationTitle("Elegir ubicación")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancelar") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        Task { await centerOnUser() }
                    } label: {
                        Image(systemName: "location")
                    }
                    .accessibilityLabel("Centrar en mi ubicación")
                }
            }
            .task {
                if initialLocation == nil { await centerOnUser() }
                await resolveAddress()
            }
        }
    }

    private var mapLayer: some View {
        ZStack {
            Map(position: $position)
                .mapControls { MapCompass() }
                .onMapCameraChange(frequency: .onEnd) { context in
                    centerCoordinate = context.region.center
                    Task { await resolveAddress() }
                }
                .ignoresSafeArea(edges: .bottom)

            // Pin fijo sobre el centro del mapa.
            Image(systemName: "mappin.circle.fill")
                .font(.system(size: 40))
                .foregroundStyle(Theme.Color.primary, Theme.Color.surface)
                .shadow(radius: 3)
                .offset(y: -20)
                .allowsHitTesting(false)
        }
    }

    private var searchBar: some View {
        HStack(spacing: Theme.Spacing.xs) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(Theme.Color.textSoft)

            TextField("Buscar dirección o lugar", text: $searchText)
                .submitLabel(.search)
                .onSubmit { Task { await search() } }

            if isSearching {
                ProgressView().controlSize(.small)
            } else if !searchText.isEmpty {
                Button {
                    searchText = ""
                    searchResults = []
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(Theme.Color.textSoft)
                }
                .accessibilityLabel("Limpiar búsqueda")
            }
        }
        .padding(Theme.Spacing.sm)
        .background(
            RoundedRectangle(cornerRadius: Theme.Radius.md, style: .continuous)
                .fill(Theme.Color.surface)
                .shadow(color: .black.opacity(0.08), radius: 6, y: 2)
        )
    }

    private var searchResultsList: some View {
        VStack(spacing: 0) {
            ForEach(searchResults.prefix(5), id: \.self) { item in
                Button {
                    select(item)
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.name ?? "Lugar")
                                .font(Theme.Font.footnote.weight(.medium))
                                .foregroundStyle(Theme.Color.text)
                            Text(Self.describe(item.placemark))
                                .font(Theme.Font.caption)
                                .foregroundStyle(Theme.Color.textMuted)
                                .lineLimit(1)
                        }
                        Spacer(minLength: 0)
                    }
                    .padding(Theme.Spacing.sm)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)

                if item != searchResults.prefix(5).last {
                    Divider().overlay(Theme.Color.border)
                }
            }
        }
        .background(
            RoundedRectangle(cornerRadius: Theme.Radius.md, style: .continuous)
                .fill(Theme.Color.surface)
                .shadow(color: .black.opacity(0.08), radius: 6, y: 2)
        )
    }

    private var bottomPanel: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            HStack(spacing: Theme.Spacing.xs) {
                Image(systemName: "mappin.and.ellipse")
                    .foregroundStyle(Theme.Color.primary)

                if isResolvingAddress {
                    Text("Buscando dirección…")
                        .font(Theme.Font.footnote)
                        .foregroundStyle(Theme.Color.textMuted)
                } else {
                    Text(address.isEmpty ? "Ubicación seleccionada" : address)
                        .font(Theme.Font.footnote.weight(.medium))
                        .foregroundStyle(Theme.Color.text)
                        .lineLimit(2)
                }
                Spacer(minLength: 0)
            }

            Text(String(
                format: "Lat %.5f · Lon %.5f",
                centerCoordinate.latitude,
                centerCoordinate.longitude
            ))
            .font(Theme.Font.caption.monospacedDigit())
            .foregroundStyle(Theme.Color.textSoft)

            Button("Usar esta ubicación") {
                onConfirm(PickedLocation(coordinate: centerCoordinate, address: address))
                dismiss()
            }
            .buttonStyle(PrimaryButtonStyle())
        }
        .padding(Theme.Spacing.md)
        .background(
            UnevenRoundedRectangle(
                topLeadingRadius: Theme.Radius.lg,
                topTrailingRadius: Theme.Radius.lg,
                style: .continuous
            )
            .fill(Theme.Color.surface)
            .shadow(color: .black.opacity(0.12), radius: 10, y: -2)
        )
    }

    // MARK: - Acciones

    private func centerOnUser() async {
        let outcome = await dependencies.location.requestCurrentLocation()
        switch outcome {
        case let .coordinate(coordinate):
            centerCoordinate = coordinate
            position = .region(MKCoordinateRegion(
                center: coordinate,
                span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
            ))
            await resolveAddress()
        case .denied:
            permissionMessage = "Sin permiso de ubicación. Puedes mover el mapa a mano o "
                + "activarlo en Ajustes › Privacidad › Localización."
        case .restricted:
            permissionMessage = "El acceso a la ubicación está restringido en este dispositivo."
        case let .failed(message):
            permissionMessage = message
        }
    }

    private func search() async {
        let query = searchText.trimmingCharacters(in: .whitespaces)
        guard !query.isEmpty else {
            searchResults = []
            return
        }

        isSearching = true
        defer { isSearching = false }

        let request = MKLocalSearch.Request()
        request.naturalLanguageQuery = query
        request.region = MKCoordinateRegion(
            center: centerCoordinate,
            span: MKCoordinateSpan(latitudeDelta: 0.5, longitudeDelta: 0.5)
        )

        guard let response = try? await MKLocalSearch(request: request).start() else {
            searchResults = []
            return
        }
        searchResults = response.mapItems
    }

    private func select(_ item: MKMapItem) {
        let coordinate = item.placemark.coordinate
        centerCoordinate = coordinate
        address = item.name.map { "\($0), \(Self.describe(item.placemark))" }
            ?? Self.describe(item.placemark)
        position = .region(MKCoordinateRegion(
            center: coordinate,
            span: MKCoordinateSpan(latitudeDelta: 0.005, longitudeDelta: 0.005)
        ))
        searchResults = []
        searchText = ""
    }

    /// Geocodificación inversa para rellenar `direccionReferencia`.
    private func resolveAddress() async {
        isResolvingAddress = true
        defer { isResolvingAddress = false }

        let location = CLLocation(
            latitude: centerCoordinate.latitude,
            longitude: centerCoordinate.longitude
        )
        guard let placemark = try? await geocoder.reverseGeocodeLocation(location).first else {
            return
        }
        address = Self.describe(placemark)
    }

    private static func describe(_ placemark: CLPlacemark) -> String {
        [
            placemark.thoroughfare,
            placemark.subLocality,
            placemark.locality,
            placemark.administrativeArea,
        ]
        .compactMap { $0 }
        .filter { !$0.isEmpty }
        .joined(separator: ", ")
    }
}
