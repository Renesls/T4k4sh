import XCTest
@testable import T4KASH

/// Pruebas de los ViewModel contra la capa de red simulada.
@MainActor
final class AuthViewModelTests: XCTestCase {

    override func setUp() {
        super.setUp()
        MockURLProtocol.reset()
    }

    override func tearDown() {
        MockURLProtocol.reset()
        super.tearDown()
    }

    private func makeViewModel(
        tokenProvider: TokenProviding & AnyObject = StubTokenProvider()
    ) -> (AuthViewModel, SessionStore) {
        let client = TestFixtures.makeClient(tokenProvider: tokenProvider)
        let storage = TokenStorage()
        // Cada prueba parte sin sesión previa.
        storage.clear()
        let session = SessionStore(
            tokenStorage: storage,
            defaults: UserDefaults(suiteName: "t4kash.tests.\(UUID().uuidString)")!
        )
        return (AuthViewModel(repository: AuthRepository(client: client), session: session), session)
    }

    func testLoginStoresPendingEmailWithoutCreatingSession() async {
        TestFixtures.respond(json: """
        {
          "correo": "ana@uam.edu.ni",
          "fechaExpiracion": "2026-09-04T15:00:00",
          "mensaje": "Enviamos un codigo a tu correo."
        }
        """)

        let (viewModel, session) = makeViewModel()
        let ok = await viewModel.login(correo: "ana@uam.edu.ni", password: "secreta1")

        XCTAssertTrue(ok)
        XCTAssertEqual(viewModel.pendingEmail, "ana@uam.edu.ni")
        XCTAssertNotNil(viewModel.challengeExpiresAt)
        // El primer paso no autentica: la sesión llega en el segundo.
        XCTAssertFalse(session.isAuthenticated)
        XCTAssertNil(viewModel.errorMessage)
    }

    func testVerifyLoginStartsTheSession() async {
        TestFixtures.respond(json: """
        {
          "token": "token-de-sesion",
          "fechaExpiracion": "2026-10-04T15:00:00",
          "usuario": {
            "idUsuario": 42,
            "nombreUsuario": "ana.lopez",
            "nombre": "Ana",
            "apellido": "López",
            "correo": "ana@uam.edu.ni",
            "idUniversidad": 1,
            "nombreUniversidad": "UAM",
            "idCarrera": 3,
            "nombreCarrera": "Diseño",
            "estadoUsuario": "ACTIVO",
            "roles": ["ADMIN"]
          }
        }
        """)

        let (viewModel, session) = makeViewModel()
        viewModel.pendingEmail = "ana@uam.edu.ni"

        let ok = await viewModel.verifyLogin(codigo: "123456")

        XCTAssertTrue(ok)
        XCTAssertTrue(session.isAuthenticated)
        XCTAssertEqual(session.user?.idUsuario, 42)
        XCTAssertEqual(session.user?.arroba, "@ana.lopez")
        XCTAssertTrue(session.user?.esAdministrador == true)
        XCTAssertEqual(session.user?.iniciales, "AL")

        session.clear()
    }

    func testLoginSurfacesBackendMessageOnFailure() async {
        TestFixtures.respond(status: 401, json: #"{"detail":"Credenciales invalidas."}"#)

        let (viewModel, session) = makeViewModel()
        let ok = await viewModel.login(correo: "ana@uam.edu.ni", password: "mala")

        XCTAssertFalse(ok)
        // El 401 del login significa credenciales incorrectas: debe mostrarse
        // el mensaje del backend, no el genérico de sesión expirada.
        XCTAssertEqual(viewModel.errorMessage, "Credenciales invalidas.")
        XCTAssertFalse(session.isAuthenticated)
    }

    func testTooManyAttemptsIsReportedToTheUser() async {
        TestFixtures.respond(
            status: 429,
            json: #"{"detail":"Demasiados intentos. Intenta en 15 minutos."}"#
        )

        let (viewModel, _) = makeViewModel()
        let ok = await viewModel.login(correo: "ana@uam.edu.ni", password: "secreta1")

        XCTAssertFalse(ok)
        XCTAssertEqual(
            viewModel.errorMessage,
            "Demasiados intentos. Intenta en 15 minutos."
        )
    }

    func testDetectsUniversityFromEmailAndLoadsCareers() async {
        var callCount = 0
        MockURLProtocol.handler = { request in
            callCount += 1
            let path = request.url?.path ?? ""
            let json: String
            if path.hasSuffix("/careers") {
                json = """
                [{"idCarrera": 3, "nombreCarrera": "Diseño", "idUniversidad": 1}]
                """
            } else {
                json = """
                [{
                  "idUniversidad": 1,
                  "nombreUniversidad": "Universidad Americana",
                  "dominiosCorreo": ["uam.edu.ni"]
                }]
                """
            }
            return (HTTPURLResponse.make(url: request.url!, status: 200), Data(json.utf8))
        }

        let (viewModel, _) = makeViewModel()
        await viewModel.loadUniversities()
        await viewModel.updateDetectedUniversity(for: "ana@uam.edu.ni")

        XCTAssertEqual(viewModel.detectedUniversity?.idUniversidad, 1)
        XCTAssertEqual(viewModel.careers.count, 1)
        XCTAssertEqual(viewModel.careers.first?.nombreCarrera, "Diseño")
        XCTAssertEqual(callCount, 2)
    }

    func testUnknownDomainClearsCareers() async {
        TestFixtures.respond(json: """
        [{
          "idUniversidad": 1,
          "nombreUniversidad": "Universidad Americana",
          "dominiosCorreo": ["uam.edu.ni"]
        }]
        """)

        let (viewModel, _) = makeViewModel()
        await viewModel.loadUniversities()
        await viewModel.updateDetectedUniversity(for: "ana@gmail.com")

        XCTAssertNil(viewModel.detectedUniversity)
        XCTAssertTrue(viewModel.careers.isEmpty)
    }
}

@MainActor
final class MarketplaceViewModelTests: XCTestCase {

    override func setUp() {
        super.setUp()
        MockURLProtocol.reset()
    }

    override func tearDown() {
        MockURLProtocol.reset()
        super.tearDown()
    }

    private func taskJSON(
        id: Int,
        titulo: String,
        categoria: Int = 1,
        tipo: String = "TAREA",
        modalidad: String = "REMOTA"
    ) -> String {
        """
        {
          "idTarea": \(id),
          "titulo": "\(titulo)",
          "descripcion": "Descripción de \(titulo)",
          "presupuesto": 500,
          "fechaPublicacion": "2026-09-01T10:00:00",
          "fechaLimitePostulacion": null,
          "fechaLimite": null,
          "estadoTarea": "PUBLICADA",
          "idCategoria": \(categoria),
          "idCliente": 9,
          "tipoOportunidad": "\(tipo)",
          "modalidad": "\(modalidad)",
          "visibilidad": "PUBLICA",
          "direccionReferencia": null,
          "latitud": null,
          "longitud": null,
          "cliente": null
        }
        """
    }

    private func stubMarketplace() {
        MockURLProtocol.handler = { [self] request in
            let path = request.url?.path ?? ""
            let json: String
            if path.hasSuffix("/categories") {
                json = """
                [
                  {"idCategoria": 1, "nombreCategoria": "Diseño", "descripcion": null, "estado": true},
                  {"idCategoria": 2, "nombreCategoria": "Tutoría", "descripcion": null, "estado": true},
                  {"idCategoria": 3, "nombreCategoria": "Obsoleta", "descripcion": null, "estado": false}
                ]
                """
            } else {
                json = """
                [
                  \(taskJSON(id: 1, titulo: "Diseñar afiche", categoria: 1)),
                  \(taskJSON(id: 2, titulo: "Tutoría de cálculo", categoria: 2)),
                  \(taskJSON(id: 3, titulo: "Mover cajas", categoria: 2,
                             tipo: "RAPIDA", modalidad: "PRESENCIAL"))
                ]
                """
            }
            return (HTTPURLResponse.make(url: request.url!, status: 200), Data(json.utf8))
        }
    }

    private func makeViewModel() -> MarketplaceViewModel {
        MarketplaceViewModel(
            repository: MarketplaceRepository(client: TestFixtures.makeClient())
        )
    }

    func testLoadsTasksAndFiltersInactiveCategories() async {
        stubMarketplace()
        let viewModel = makeViewModel()
        await viewModel.load()

        XCTAssertEqual(viewModel.state.value?.count, 3)
        // La categoría con `estado: false` no debe ofrecerse como filtro.
        XCTAssertEqual(viewModel.categories.count, 2)
        XCTAssertFalse(viewModel.hasActiveFilters)
    }

    func testSearchFiltersByTitleAndDescription() async {
        stubMarketplace()
        let viewModel = makeViewModel()
        await viewModel.load()

        viewModel.searchText = "cálculo"
        XCTAssertEqual(viewModel.visibleTasks.map(\.idTarea), [2])

        viewModel.searchText = "  "
        XCTAssertEqual(viewModel.visibleTasks.count, 3)
    }

    func testFiltersByCategoryAndModality() async {
        stubMarketplace()
        let viewModel = makeViewModel()
        await viewModel.load()

        viewModel.selectedCategory = viewModel.categories.first { $0.idCategoria == 2 }
        XCTAssertEqual(viewModel.visibleTasks.map(\.idTarea), [2, 3])

        viewModel.selectedModalidad = .presencial
        XCTAssertEqual(viewModel.visibleTasks.map(\.idTarea), [3])
    }

    func testQuickTaskFilter() async {
        stubMarketplace()
        let viewModel = makeViewModel()
        await viewModel.load()

        viewModel.onlyQuickTasks = true
        XCTAssertEqual(viewModel.visibleTasks.map(\.idTarea), [3])
        XCTAssertTrue(viewModel.hasActiveFilters)

        viewModel.clearFilters()
        XCTAssertFalse(viewModel.hasActiveFilters)
        XCTAssertEqual(viewModel.visibleTasks.count, 3)
    }

    func testCategoryNameFallsBackWhenUnknown() async {
        stubMarketplace()
        let viewModel = makeViewModel()
        await viewModel.load()

        XCTAssertEqual(viewModel.categoryName(for: 1), "Diseño")
        XCTAssertEqual(viewModel.categoryName(for: 999), "Categoría")
    }

    func testFailureIsReportedAsErrorState() async {
        TestFixtures.respond(status: 500, json: #"{"detail":"Error interno"}"#)
        let viewModel = makeViewModel()
        await viewModel.load()

        XCTAssertNil(viewModel.state.value)
        XCTAssertEqual(viewModel.state.errorMessage, "Error interno")
    }

    func testApplyUpdatedInsertsNewTaskAtTop() async {
        stubMarketplace()
        let viewModel = makeViewModel()
        await viewModel.load()

        let created = TaskItem(
            idTarea: 99,
            titulo: "Nueva oportunidad",
            descripcion: "Recién publicada",
            presupuesto: 1_000,
            fechaPublicacion: Date(),
            fechaLimitePostulacion: nil,
            fechaLimite: nil,
            estadoTarea: "PUBLICADA",
            idCategoria: 1,
            idCliente: 9,
            tipoOportunidad: "TAREA",
            modalidad: "REMOTA",
            visibilidad: "PUBLICA",
            direccionReferencia: nil,
            latitud: nil,
            longitud: nil,
            cliente: nil
        )
        viewModel.apply(updated: created)

        XCTAssertEqual(viewModel.state.value?.first?.idTarea, 99)
        XCTAssertEqual(viewModel.state.value?.count, 4)
    }
}

/// El estado de carga debe distinguir vacío de error, para que la interfaz
/// nunca muestre una lista en blanco sin explicación.
final class LoadStateTests: XCTestCase {

    func testEmptyIsOnlyTrueWhenLoadedWithNoElements() {
        XCTAssertTrue(LoadState<[Int]>.loaded([]).isEmpty)
        XCTAssertFalse(LoadState<[Int]>.loaded([1]).isEmpty)
        XCTAssertFalse(LoadState<[Int]>.loading.isEmpty)
        XCTAssertFalse(LoadState<[Int]>.failed("boom").isEmpty)
    }

    func testErrorMessageIsOnlyExposedInFailure() {
        XCTAssertEqual(LoadState<[Int]>.failed("boom").errorMessage, "boom")
        XCTAssertNil(LoadState<[Int]>.loaded([]).errorMessage)
    }

    func testPresenterUsesAPIErrorDescription() {
        XCTAssertEqual(
            ErrorPresenter.message(for: APIError.offline),
            "Sin conexión a internet. Revisa tu red e inténtalo de nuevo."
        )
        XCTAssertEqual(
            ErrorPresenter.message(for: APIError.conflict("Ya existe.")),
            "Ya existe."
        )
    }

    func testRetryableErrorsAreClassifiedCorrectly() {
        XCTAssertTrue(APIError.offline.isRetryable)
        XCTAssertTrue(APIError.timeout.isRetryable)
        XCTAssertTrue(APIError.server(status: 503, message: "").isRetryable)
        XCTAssertFalse(APIError.unauthorized("").isRetryable)
        XCTAssertFalse(APIError.validation("dato inválido").isRetryable)
    }

    /// Un 401 sin detalle cae en el mensaje genérico de sesión expirada.
    func testUnauthorizedFallsBackToSessionExpiredMessage() {
        XCTAssertEqual(
            ErrorPresenter.message(for: APIError.unauthorized("")),
            "Tu sesión expiró. Inicia sesión de nuevo."
        )
        XCTAssertEqual(
            ErrorPresenter.message(for: APIError.unauthorized("Credenciales invalidas.")),
            "Credenciales invalidas."
        )
    }
}
