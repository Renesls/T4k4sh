import XCTest
@testable import T4KASH

/// Pruebas de la capa de red: construcción de peticiones, autenticación,
/// decodificación y traducción de errores del backend.
final class APIClientTests: XCTestCase {

    override func setUp() {
        super.setUp()
        MockURLProtocol.reset()
    }

    override func tearDown() {
        MockURLProtocol.reset()
        super.tearDown()
    }

    // MARK: - Construcción de la petición

    func testResolvesPathAgainstBaseURLAndSortsQuery() throws {
        let request = APIRequest.get("tasks", query: ["size": "50", "page": "0"])
        let urlRequest = try request.urlRequest(baseURL: TestFixtures.baseURL, token: nil)

        XCTAssertEqual(
            urlRequest.url?.absoluteString,
            "https://api.test.t4kash/api/tasks?page=0&size=50"
        )
        XCTAssertEqual(urlRequest.httpMethod, "GET")
    }

    func testDropsNilQueryValues() throws {
        let request = APIRequest.get("tasks", query: ["page": "0", "filtro": nil])
        let urlRequest = try request.urlRequest(baseURL: TestFixtures.baseURL, token: nil)

        let query = urlRequest.url?.query ?? ""
        XCTAssertEqual(query, "page=0")
    }

    func testAttachesBearerTokenOnlyWhenRequired() throws {
        let authenticated = try APIRequest.get("auth/me")
            .urlRequest(baseURL: TestFixtures.baseURL, token: "token-123")
        XCTAssertEqual(
            authenticated.value(forHTTPHeaderField: "Authorization"),
            "Bearer token-123"
        )

        let publicRequest = try APIRequest.get("categories", requiresAuth: false)
            .urlRequest(baseURL: TestFixtures.baseURL, token: "token-123")
        XCTAssertNil(publicRequest.value(forHTTPHeaderField: "Authorization"))
    }

    func testJSONRequestSetsContentTypeAndBody() throws {
        let request = try APIRequest.json(
            "auth/login",
            method: .post,
            body: LoginRequest(correo: "a@b.edu.ni", password: "secreta1"),
            requiresAuth: false
        )
        let urlRequest = try request.urlRequest(baseURL: TestFixtures.baseURL, token: nil)

        XCTAssertEqual(urlRequest.value(forHTTPHeaderField: "Content-Type"), "application/json")
        let body = try XCTUnwrap(urlRequest.httpBody)
        let decoded = try JSONSerialization.jsonObject(with: body) as? [String: Any]
        XCTAssertEqual(decoded?["correo"] as? String, "a@b.edu.ni")
        XCTAssertEqual(decoded?["password"] as? String, "secreta1")
    }

    // MARK: - Decodificación

    func testDecodesTaskListWithBackendFieldNames() async throws {
        TestFixtures.respond(json: """
        [{
          "idTarea": 7,
          "titulo": "Diseñar afiche",
          "descripcion": "Propuesta para redes.",
          "presupuesto": 850.50,
          "fechaPublicacion": "2026-09-01T10:15:00",
          "fechaLimitePostulacion": null,
          "fechaLimite": null,
          "estadoTarea": "PUBLICADA",
          "idCategoria": 3,
          "idCliente": 42,
          "tipoOportunidad": "TAREA",
          "modalidad": "REMOTA",
          "visibilidad": "PUBLICA",
          "direccionReferencia": null,
          "latitud": null,
          "longitud": null,
          "cliente": {
            "idUsuario": 42,
            "nombreUsuario": "ana.lopez",
            "nombreCompleto": "Ana López",
            "nombreUniversidad": "UAM",
            "nombreCarrera": "Diseño",
            "estudianteVerificado": true
          }
        }]
        """)

        let client = TestFixtures.makeClient()
        let tasks: [TaskItem] = try await client.send(.get("tasks"))

        XCTAssertEqual(tasks.count, 1)
        let task = try XCTUnwrap(tasks.first)
        XCTAssertEqual(task.idTarea, 7)
        XCTAssertEqual(task.presupuesto, Decimal(string: "850.50"))
        XCTAssertEqual(task.estadoTarea, "PUBLICADA")
        XCTAssertTrue(task.estaPublicada)
        XCTAssertFalse(task.esTareaRapida)
        XCTAssertEqual(task.modalidadResuelta, .remota)
        XCTAssertNil(task.coordenadas)
        XCTAssertEqual(task.cliente?.arroba, "@ana.lopez")
    }

    func testDecodesAttachmentReservedExtensionKey() async throws {
        TestFixtures.respond(json: """
        [{
          "idArchivo": 5,
          "idTarea": 7,
          "idEntrega": null,
          "idVerificacion": null,
          "idUsuarioSube": 42,
          "nombreOriginal": "carnet.pdf",
          "tipoMime": "application/pdf",
          "extension": "pdf",
          "tamanoBytes": 2048,
          "fechaSubida": "2026-09-01T10:15:00",
          "estadoArchivo": "ACTIVO",
          "rutaDescarga": "attachments/5/download"
        }]
        """)

        let client = TestFixtures.makeClient()
        let attachments: [Attachment] = try await client.send(.get("tasks/7/attachments"))

        let attachment = try XCTUnwrap(attachments.first)
        XCTAssertEqual(attachment.extension_, "pdf")
        XCTAssertEqual(attachment.rutaDescarga, "attachments/5/download")
        XCTAssertFalse(attachment.esImagen)
    }

    func testEmptyResponseDoesNotRequireBody() async throws {
        MockURLProtocol.handler = { request in
            (HTTPURLResponse.make(url: request.url!, status: 204), Data())
        }

        let client = TestFixtures.makeClient()
        // No debe lanzar aunque el cuerpo esté vacío.
        try await client.sendIgnoringResponse(
            .empty("notifications/read-all", method: .post)
        )
    }

    // MARK: - Errores

    func testMapsProblemDetailToValidationError() async {
        TestFixtures.respond(status: 400, json: """
        {
          "type": "about:blank",
          "title": "Bad Request",
          "status": 400,
          "detail": "presupuesto: debe ser mayor o igual que 0"
        }
        """)

        let client = TestFixtures.makeClient()
        do {
            let _: TaskItem = try await client.send(.get("tasks/1"))
            XCTFail("Se esperaba un error de validación")
        } catch let error as APIError {
            guard case let .validation(message) = error else {
                return XCTFail("Caso inesperado: \(error)")
            }
            XCTAssertEqual(message, "presupuesto: debe ser mayor o igual que 0")
        } catch {
            XCTFail("Tipo de error inesperado: \(error)")
        }
    }

    func testMapsConflictToBusinessError() async {
        TestFixtures.respond(status: 409, json: #"{"detail":"Ya te postulaste a esta tarea."}"#)
        let client = TestFixtures.makeClient()

        do {
            let _: Application = try await client.send(.get("tasks/1/applications"))
            XCTFail("Se esperaba un conflicto")
        } catch let error as APIError {
            guard case let .conflict(message) = error else {
                return XCTFail("Caso inesperado: \(error)")
            }
            XCTAssertEqual(message, "Ya te postulaste a esta tarea.")
        } catch {
            XCTFail("Tipo de error inesperado: \(error)")
        }
    }

    func testMapsNotFoundWithBackendDetail() async {
        TestFixtures.respond(status: 404, json: #"{"detail":"La tarea no existe."}"#)
        let client = TestFixtures.makeClient()

        do {
            let _: TaskItem = try await client.send(.get("tasks/999"))
            XCTFail("Se esperaba un 404")
        } catch let error as APIError {
            XCTAssertEqual(error, .notFound("La tarea no existe."))
        } catch {
            XCTFail("Tipo de error inesperado: \(error)")
        }
    }

    func testUnauthorizedInvalidatesSessionWhenTokenPresent() async {
        TestFixtures.respond(status: 401, json: #"{"detail":"Sesion expirada"}"#)

        let provider = StubTokenProvider(token: "token-vencido")
        let client = TestFixtures.makeClient(tokenProvider: provider)

        do {
            let _: AuthenticatedUser = try await client.send(.get("auth/me"))
            XCTFail("Se esperaba un 401")
        } catch let error as APIError {
            XCTAssertEqual(error, .unauthorized("Sesion expirada"))
        } catch {
            XCTFail("Tipo de error inesperado: \(error)")
        }

        XCTAssertEqual(provider.invalidationCount, 1)
        XCTAssertNil(provider.currentToken)
    }

    func testUnauthorizedWithoutTokenDoesNotInvalidate() async {
        TestFixtures.respond(status: 401, json: #"{"detail":"Credenciales invalidas"}"#)

        let provider = StubTokenProvider(token: nil)
        let client = TestFixtures.makeClient(tokenProvider: provider)

        _ = try? await client.send(
            try APIRequest.json(
                "auth/login",
                method: .post,
                body: LoginRequest(correo: "a@b.edu.ni", password: "mala"),
                requiresAuth: false
            ),
            as: LoginChallenge.self
        )

        XCTAssertEqual(provider.invalidationCount, 0)
    }

    func testSurfacesDecodingErrorInsteadOfCrashing() async {
        // Falta `titulo`, que el modelo declara como obligatorio.
        TestFixtures.respond(json: #"{"idTarea": 1}"#)
        let client = TestFixtures.makeClient()

        do {
            let _: TaskItem = try await client.send(.get("tasks/1"))
            XCTFail("Se esperaba un error de decodificación")
        } catch let error as APIError {
            guard case .decoding = error else {
                return XCTFail("Caso inesperado: \(error)")
            }
        } catch {
            XCTFail("Tipo de error inesperado: \(error)")
        }
    }

    func testAttachmentLargerThanLimitIsRejectedBeforeUpload() async {
        let client = TestFixtures.makeClient()
        let oversized = Data(count: AppConfig.maxAttachmentBytes + 1)

        do {
            let _: Attachment = try await client.upload(
                path: "tasks/1/attachments",
                filename: "grande.pdf",
                mimeType: "application/pdf",
                fileData: oversized
            )
            XCTFail("Se esperaba el rechazo por tamaño")
        } catch let error as APIError {
            guard case .attachmentTooLarge = error else {
                return XCTFail("Caso inesperado: \(error)")
            }
            // No debe haberse enviado nada a la red.
            XCTAssertTrue(MockURLProtocol.recorded.isEmpty)
        } catch {
            XCTFail("Tipo de error inesperado: \(error)")
        }
    }
}
