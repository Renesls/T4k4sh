import XCTest
@testable import T4KASH

/// El backend serializa `LocalDateTime` sin zona horaria. Estas pruebas fijan
/// ese contrato para que iOS y Android muestren la misma hora.
final class APIDateFormatterTests: XCTestCase {

    private var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .current
        return calendar
    }

    func testParsesLocalDateTimeWithoutTimezone() throws {
        let date = try XCTUnwrap(APIDateFormatter.date(from: "2026-09-04T14:30:00"))
        let components = calendar.dateComponents(
            [.year, .month, .day, .hour, .minute, .second],
            from: date
        )

        XCTAssertEqual(components.year, 2026)
        XCTAssertEqual(components.month, 9)
        XCTAssertEqual(components.day, 4)
        // Se interpreta en la zona del dispositivo, igual que SimpleDateFormat.
        XCTAssertEqual(components.hour, 14)
        XCTAssertEqual(components.minute, 30)
        XCTAssertEqual(components.second, 0)
    }

    func testDiscardsFractionalSeconds() throws {
        let withFraction = try XCTUnwrap(
            APIDateFormatter.date(from: "2026-09-04T14:30:00.123456")
        )
        let without = try XCTUnwrap(APIDateFormatter.date(from: "2026-09-04T14:30:00"))
        XCTAssertEqual(withFraction, without)
    }

    func testRoundTripsThroughTheCanonicalFormat() throws {
        let raw = "2026-01-15T08:05:09"
        let date = try XCTUnwrap(APIDateFormatter.date(from: raw))
        XCTAssertEqual(APIDateFormatter.string(from: date), raw)
    }

    func testRejectsMalformedValues() {
        XCTAssertNil(APIDateFormatter.date(from: ""))
        XCTAssertNil(APIDateFormatter.date(from: "   "))
        XCTAssertNil(APIDateFormatter.date(from: "no es una fecha"))
        XCTAssertNil(APIDateFormatter.date(from: "2026-13-45T99:99:99"))
    }

    func testDecoderUsesTheCustomStrategy() throws {
        struct Wrapper: Decodable, Equatable {
            let fecha: Date
        }

        let json = Data(#"{"fecha":"2026-09-04T14:30:00"}"#.utf8)
        let decoded = try JSONCoding.decoder.decode(Wrapper.self, from: json)
        XCTAssertEqual(decoded.fecha, APIDateFormatter.date(from: "2026-09-04T14:30:00"))
    }

    func testDecoderReportsBadDatesInsteadOfSilentlyFailing() {
        struct Wrapper: Decodable {
            let fecha: Date
        }

        let json = Data(#"{"fecha":"04/09/2026"}"#.utf8)
        XCTAssertThrowsError(try JSONCoding.decoder.decode(Wrapper.self, from: json))
    }

    func testEncoderProducesISOLocalDateTime() throws {
        struct Wrapper: Encodable {
            let fecha: Date
        }

        let date = try XCTUnwrap(APIDateFormatter.date(from: "2026-09-04T14:30:00"))
        let encoded = try JSONCoding.encoder.encode(Wrapper(fecha: date))
        let text = String(decoding: encoded, as: UTF8.self)
        XCTAssertTrue(text.contains("2026-09-04T14:30:00"))
        XCTAssertFalse(text.contains("Z"))
    }
}

/// Formato de presentación: montos, tamaños, distancias y cuentas atrás.
final class DisplayFormatterTests: XCTestCase {

    func testFormatsNicaraguanCurrency() {
        XCTAssertEqual(DisplayFormatter.money(Decimal(string: "1234.5")), "C$ 1,234.50")
        XCTAssertEqual(DisplayFormatter.money(0), "C$ 0.00")
        XCTAssertEqual(DisplayFormatter.money(nil), "C$ 0.00")
    }

    func testFormatsOtherCurrenciesWithTheirCode() {
        XCTAssertEqual(
            DisplayFormatter.money(Decimal(string: "99.9"), currencyCode: "USD"),
            "USD 99.90"
        )
    }

    func testFormatsFileSizes() {
        XCTAssertEqual(DisplayFormatter.fileSize(512), "512 B")
        XCTAssertEqual(DisplayFormatter.fileSize(2_048), "2.0 KB")
        XCTAssertEqual(DisplayFormatter.fileSize(5 * 1_024 * 1_024), "5.0 MB")
        // Un valor negativo no debe producir texto absurdo.
        XCTAssertEqual(DisplayFormatter.fileSize(-10), "0 B")
    }

    func testFormatsDistances() {
        XCTAssertEqual(DisplayFormatter.distance(0.25), "250 m")
        XCTAssertEqual(DisplayFormatter.distance(2.5), "2.5 km")
    }

    func testFormatsCountdown() {
        XCTAssertEqual(DisplayFormatter.countdown(seconds: 0), "Expirada")
        XCTAssertEqual(DisplayFormatter.countdown(seconds: -5), "Expirada")
        XCTAssertEqual(DisplayFormatter.countdown(seconds: 45), "45 s")
        XCTAssertEqual(DisplayFormatter.countdown(seconds: 600), "10 min")
        XCTAssertEqual(DisplayFormatter.countdown(seconds: 7_800), "2 h 10 min")
    }

    func testHumanizesUnknownBackendCodes() {
        XCTAssertEqual("CAMBIOS_SOLICITADOS".humanizedCode, "Cambios solicitados")
        XCTAssertEqual("ESTADO_NUEVO_DEL_BACKEND".humanizedCode, "Estado nuevo del backend")
    }

    func testDomainLabelsFallBackForUnknownStates() {
        // Si el backend agrega un estado nuevo, la interfaz no debe romperse.
        XCTAssertEqual(Domain.EstadoTarea.label("PUBLICADA"), "Publicada")
        XCTAssertEqual(Domain.EstadoPago.label("ESTADO_DESCONOCIDO"), "Estado desconocido")
    }
}
