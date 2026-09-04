import Foundation

/// Acceso al módulo `identity` del backend.
///
/// Replica el flujo real de autenticación: el login es de **dos pasos**
/// (credenciales → código enviado por correo) y el token es opaco, sin refresh.
struct AuthRepository {
    private let client: APIClient

    init(client: APIClient) { self.client = client }

    // MARK: - Registro y activación

    /// `POST /auth/register` — devuelve el reto de verificación por correo.
    func register(_ request: RegisterRequest) async throws -> RegistrationChallenge {
        try await client.send(
            try .json("auth/register", method: .post, body: request, requiresAuth: false)
        )
    }

    /// `POST /auth/verify-email` — activa la cuenta y ya entrega sesión.
    func verifyEmail(correo: String, codigo: String) async throws -> AuthResponse {
        try await client.send(
            try .json(
                "auth/verify-email",
                method: .post,
                body: VerifyEmailRequest(correo: correo, codigo: codigo),
                requiresAuth: false
            )
        )
    }

    /// `POST /auth/resend-verification`
    func resendVerification(correo: String) async throws -> RegistrationChallenge {
        try await client.send(
            try .json(
                "auth/resend-verification",
                method: .post,
                body: ResendVerificationRequest(correo: correo),
                requiresAuth: false
            )
        )
    }

    // MARK: - Acceso en dos pasos

    /// `POST /auth/login` — primer paso: valida credenciales y envía el código.
    func login(correo: String, password: String) async throws -> LoginChallenge {
        try await client.send(
            try .json(
                "auth/login",
                method: .post,
                body: LoginRequest(correo: correo, password: password),
                requiresAuth: false
            )
        )
    }

    /// `POST /auth/login/verify` — segundo paso: confirma el código y da el token.
    func verifyLogin(correo: String, codigo: String) async throws -> AuthResponse {
        try await client.send(
            try .json(
                "auth/login/verify",
                method: .post,
                body: VerifyLoginRequest(correo: correo, codigo: codigo),
                requiresAuth: false
            )
        )
    }

    /// `POST /auth/login/resend`
    func resendLoginCode(correo: String) async throws -> LoginChallenge {
        try await client.send(
            try .json(
                "auth/login/resend",
                method: .post,
                body: ResendVerificationRequest(correo: correo),
                requiresAuth: false
            )
        )
    }

    // MARK: - Contraseña

    /// `POST /auth/password/forgot`
    func forgotPassword(correo: String) async throws -> SimpleMessage {
        try await client.send(
            try .json(
                "auth/password/forgot",
                method: .post,
                body: ForgotPasswordRequest(correo: correo),
                requiresAuth: false
            )
        )
    }

    /// `POST /auth/password/reset`
    func resetPassword(
        correo: String,
        codigo: String,
        nuevaPassword: String
    ) async throws -> SimpleMessage {
        try await client.send(
            try .json(
                "auth/password/reset",
                method: .post,
                body: ResetPasswordRequest(
                    correo: correo,
                    codigo: codigo,
                    nuevaPassword: nuevaPassword
                ),
                requiresAuth: false
            )
        )
    }

    // MARK: - Sesión

    /// `GET /auth/me` — valida el token guardado y refresca el perfil.
    func currentUser() async throws -> AuthenticatedUser {
        try await client.send(.get("auth/me"))
    }

    /// `POST /auth/logout` — revoca el token en el servidor.
    func logout() async throws {
        try await client.send(.empty("auth/logout", method: .post))
    }

    // MARK: - Catálogos institucionales

    /// `GET /identity/universities`
    func universities() async throws -> [University] {
        try await client.send(.get("identity/universities", requiresAuth: false))
    }

    /// `GET /identity/universities/{id}/careers`
    func careers(universityId: Int) async throws -> [Career] {
        try await client.send(
            .get("identity/universities/\(universityId)/careers", requiresAuth: false)
        )
    }

    // MARK: - Perfil público

    /// `GET /profiles/{username}` — el backend acepta el nombre con o sin arroba.
    func publicProfile(username: String) async throws -> PublicProfile {
        let clean = username.hasPrefix("@") ? String(username.dropFirst()) : username
        let encoded = clean.addingPercentEncoding(
            withAllowedCharacters: .urlPathAllowed
        ) ?? clean
        return try await client.send(.get("profiles/\(encoded)"))
    }

    /// `PUT /profiles/me/username`
    func updateUsername(_ nombreUsuario: String) async throws -> PublicProfile {
        try await client.send(
            try .json(
                "profiles/me/username",
                method: .put,
                body: UpdateUsernameRequest(nombreUsuario: nombreUsuario)
            )
        )
    }

    // MARK: - Verificación estudiantil

    /// `GET /student-verifications/me`
    func studentVerification() async throws -> StudentVerification {
        try await client.send(.get("student-verifications/me"))
    }

    /// `POST /student-verifications/me/attachments` (multipart, parte `file`)
    func uploadStudentProof(_ attachment: PreparedAttachment) async throws -> Attachment {
        try await client.upload(
            path: "student-verifications/me/attachments",
            filename: attachment.filename,
            mimeType: attachment.mimeType,
            fileData: attachment.data
        )
    }
}
