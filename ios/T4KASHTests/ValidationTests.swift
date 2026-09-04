import XCTest
@testable import T4KASH

/// Las validaciones del cliente deben coincidir con las anotaciones de los DTO
/// del backend, para no gastar peticiones en datos que serán rechazados.
final class ValidationTests: XCTestCase {

    func testEmailValidationMatchesBackendExpectations() {
        XCTAssertTrue(Validation.isValidEmail("estudiante@uam.edu.ni"))
        XCTAssertTrue(Validation.isValidEmail("nombre.apellido@universidad.edu.ni"))

        XCTAssertFalse(Validation.isValidEmail(""))
        XCTAssertFalse(Validation.isValidEmail("sinArroba.edu.ni"))
        XCTAssertFalse(Validation.isValidEmail("@uam.edu.ni"))
        XCTAssertFalse(Validation.isValidEmail("usuario@sinpunto"))
        XCTAssertFalse(Validation.isValidEmail("usuario@.edu.ni"))
        XCTAssertFalse(Validation.isValidEmail(String(repeating: "a", count: 160) + "@uam.edu.ni"))
    }

    /// `RegisterRequest.password`: `@Size(min = 8, max = 72)`
    func testPasswordLengthBounds() {
        XCTAssertFalse(Validation.isValidPassword("corta12"))
        XCTAssertTrue(Validation.isValidPassword("ochoChar"))
        XCTAssertTrue(Validation.isValidPassword(String(repeating: "a", count: 72)))
        XCTAssertFalse(Validation.isValidPassword(String(repeating: "a", count: 73)))
    }

    /// `VerifyEmailRequest.codigo`: `@Pattern(regexp = "\\d{6}")`
    func testVerificationCodeIsSixDigits() {
        XCTAssertTrue(Validation.isValidCode("123456"))
        XCTAssertFalse(Validation.isValidCode("12345"))
        XCTAssertFalse(Validation.isValidCode("1234567"))
        XCTAssertFalse(Validation.isValidCode("12345a"))
        XCTAssertFalse(Validation.isValidCode(""))
    }

    /// `UpdateUsernameRequest`: `^@?[A-Za-z0-9][A-Za-z0-9._]{2,29}$`
    func testUsernameRules() {
        XCTAssertTrue(Validation.isValidUsername("ana.lopez"))
        XCTAssertTrue(Validation.isValidUsername("@ana_lopez"))
        XCTAssertTrue(Validation.isValidUsername("abc"))

        XCTAssertFalse(Validation.isValidUsername("ab"))
        XCTAssertFalse(Validation.isValidUsername(".empiezaConPunto"))
        XCTAssertFalse(Validation.isValidUsername("con espacio"))
        XCTAssertFalse(Validation.isValidUsername("con-guion"))
        XCTAssertFalse(Validation.isValidUsername(String(repeating: "a", count: 31)))
    }

    func testDecimalParsingAcceptsCommaAndPoint() {
        XCTAssertEqual(Validation.decimal(from: "1500.75"), Decimal(string: "1500.75"))
        XCTAssertEqual(Validation.decimal(from: "1500,75"), Decimal(string: "1500.75"))
        XCTAssertEqual(Validation.decimal(from: " 250 "), 250)
        XCTAssertNil(Validation.decimal(from: ""))
        XCTAssertNil(Validation.decimal(from: "abc"))
    }
}

/// Detección de universidad por dominio: misma lógica que `EmailDomainMatcher.kt`.
final class EmailDomainMatcherTests: XCTestCase {

    private let universities = [
        University(
            idUniversidad: 1,
            nombreUniversidad: "Universidad Americana",
            dominiosCorreo: ["uam.edu.ni", "alumnos.uam.edu.ni"]
        ),
        University(
            idUniversidad: 2,
            nombreUniversidad: "UNI",
            dominiosCorreo: ["uni.edu.ni"]
        ),
    ]

    func testExtractsDomain() {
        XCTAssertEqual(EmailDomainMatcher.domain(from: "Ana@UAM.edu.ni"), "uam.edu.ni")
        XCTAssertEqual(EmailDomainMatcher.domain(from: "  ana@uni.edu.ni "), "uni.edu.ni")
    }

    func testRejectsMalformedDomains() {
        XCTAssertNil(EmailDomainMatcher.domain(from: "ana@"))
        XCTAssertNil(EmailDomainMatcher.domain(from: "@uam.edu.ni"))
        XCTAssertNil(EmailDomainMatcher.domain(from: "ana@sinpunto"))
        XCTAssertNil(EmailDomainMatcher.domain(from: "ana@.uam.edu.ni"))
        XCTAssertNil(EmailDomainMatcher.domain(from: "ana@uam.edu.ni."))
        XCTAssertNil(EmailDomainMatcher.domain(from: "ana@uam edu ni"))
    }

    func testDetectsUniversityIgnoringCase() {
        let match = EmailDomainMatcher.detectUniversity(
            email: "Ana.Lopez@UAM.edu.ni",
            universities: universities
        )
        XCTAssertEqual(match?.idUniversidad, 1)
    }

    func testMatchesSecondaryDomain() {
        let match = EmailDomainMatcher.detectUniversity(
            email: "ana@alumnos.uam.edu.ni",
            universities: universities
        )
        XCTAssertEqual(match?.idUniversidad, 1)
    }

    func testReturnsNilForUnknownDomain() {
        XCTAssertNil(
            EmailDomainMatcher.detectUniversity(
                email: "ana@gmail.com",
                universities: universities
            )
        )
    }

    /// La coincidencia es exacta: un subdominio no registrado no debe colar.
    func testDoesNotMatchPartialDomains() {
        XCTAssertNil(
            EmailDomainMatcher.detectUniversity(
                email: "ana@falso-uam.edu.ni",
                universities: universities
            )
        )
    }
}

/// Cuerpo multipart: el backend espera exactamente la parte `file`.
final class MultipartFormDataTests: XCTestCase {

    func testBuildsFilePartWithBackendFieldName() throws {
        var form = MultipartFormData(boundary: "LIMITE")
        form.addFile(
            name: "file",
            filename: "carnet.pdf",
            mimeType: "application/pdf",
            data: Data("contenido".utf8)
        )
        let body = String(decoding: form.finalized(), as: UTF8.self)

        XCTAssertTrue(body.hasPrefix("--LIMITE\r\n"))
        XCTAssertTrue(body.contains(
            #"Content-Disposition: form-data; name="file"; filename="carnet.pdf""#
        ))
        XCTAssertTrue(body.contains("Content-Type: application/pdf"))
        XCTAssertTrue(body.contains("contenido"))
        XCTAssertTrue(body.hasSuffix("--LIMITE--\r\n"))
    }

    func testContentTypeCarriesTheBoundary() {
        let form = MultipartFormData(boundary: "LIMITE")
        XCTAssertEqual(form.contentType, "multipart/form-data; boundary=LIMITE")
    }

    func testUploadRequestUsesMultipartContentType() throws {
        let request = APIRequest.upload(
            "tasks/1/attachments",
            filename: "foto.jpg",
            mimeType: "image/jpeg",
            fileData: Data([0xFF, 0xD8])
        )
        let urlRequest = try request.urlRequest(
            baseURL: TestFixtures.baseURL,
            token: "token"
        )

        XCTAssertEqual(urlRequest.httpMethod, "POST")
        XCTAssertTrue(
            urlRequest.value(forHTTPHeaderField: "Content-Type")?
                .hasPrefix("multipart/form-data; boundary=") == true
        )
        XCTAssertEqual(
            urlRequest.value(forHTTPHeaderField: "Authorization"),
            "Bearer token"
        )
    }
}
