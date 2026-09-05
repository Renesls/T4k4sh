import Foundation

// Modelos del módulo `identity` del backend.
// Los nombres de propiedad coinciden exactamente con los `record` de Java, así
// que no hacen falta `CodingKeys` y el contrato queda a la vista.

/// `identity/dto/AuthenticatedUserResponse`
struct AuthenticatedUser: Codable, Identifiable, Equatable {
    let idUsuario: Int
    let nombreUsuario: String?
    let nombre: String
    let apellido: String
    let correo: String
    let idUniversidad: Int?
    let nombreUniversidad: String?
    let idCarrera: Int?
    let nombreCarrera: String?
    let estadoUsuario: String
    let roles: [String]

    var id: Int { idUsuario }

    var nombreCompleto: String {
        "\(nombre) \(apellido)".trimmingCharacters(in: .whitespaces)
    }

    var arroba: String {
        guard let nombreUsuario, !nombreUsuario.isEmpty else { return "" }
        return nombreUsuario.hasPrefix("@") ? nombreUsuario : "@\(nombreUsuario)"
    }

    var iniciales: String {
        let letters = [nombre, apellido]
            .compactMap { $0.trimmingCharacters(in: .whitespaces).first }
            .map(String.init)
            .joined()
            .uppercased()
        return letters.isEmpty ? "TK" : String(letters.prefix(2))
    }

    var esAdministrador: Bool { roles.contains(Domain.Rol.admin) }
    var esEvaluador: Bool { roles.contains(Domain.Rol.evaluador) }
}

/// `identity/dto/AuthResponse`
struct AuthResponse: Codable {
    let token: String
    let fechaExpiracion: Date?
    let usuario: AuthenticatedUser
}

/// `identity/dto/LoginChallengeResponse` — respuesta del primer paso del login.
struct LoginChallenge: Codable {
    let correo: String
    let fechaExpiracion: Date?
    let mensaje: String
}

/// `identity/dto/RegistrationResponse`
struct RegistrationChallenge: Codable {
    let correo: String
    let fechaExpiracion: Date?
    let mensaje: String
}

/// `identity/dto/MessageResponse`
struct SimpleMessage: Codable {
    let mensaje: String
}

/// `identity/dto/UniversityResponse`
struct University: Codable, Identifiable, Equatable, Hashable {
    let idUniversidad: Int
    let nombreUniversidad: String
    let dominiosCorreo: [String]

    var id: Int { idUniversidad }
}

/// `identity/dto/CareerResponse`
struct Career: Codable, Identifiable, Equatable, Hashable {
    let idCarrera: Int
    let nombreCarrera: String
    let idUniversidad: Int

    var id: Int { idCarrera }
}

/// `identity/dto/PublicIdentityResponse` — identidad pública embebida en tareas,
/// postulaciones, trabajos, publicaciones y comentarios.
struct PublicIdentity: Codable, Identifiable, Equatable, Hashable {
    let idUsuario: Int
    let nombreUsuario: String?
    let nombreCompleto: String
    let nombreUniversidad: String?
    let nombreCarrera: String?
    let estudianteVerificado: Bool

    var id: Int { idUsuario }

    var arroba: String {
        guard let nombreUsuario, !nombreUsuario.isEmpty else { return "" }
        return nombreUsuario.hasPrefix("@") ? nombreUsuario : "@\(nombreUsuario)"
    }

    var iniciales: String {
        let letters = nombreCompleto
            .split(separator: " ")
            .prefix(2)
            .compactMap(\.first)
            .map(String.init)
            .joined()
            .uppercased()
        return letters.isEmpty ? "TK" : letters
    }
}

/// `identity/dto/PublicProfileResponse`
struct PublicProfile: Codable, Equatable {
    let identidad: PublicIdentity
    let miembroDesde: Date?
    let publicaciones: Int
    let trabajosCompletados: Int
    let proximoCambioNombreUsuario: Date?

    /// El backend impone 30 días entre cambios de nombre de usuario.
    var puedeCambiarNombreUsuario: Bool {
        guard let proximoCambioNombreUsuario else { return true }
        return proximoCambioNombreUsuario <= Date()
    }
}

/// `identity/dto/StudentVerificationResponse`
struct StudentVerification: Codable, Identifiable, Equatable {
    let idVerificacion: Int
    let idUsuario: Int
    let correo: String?
    let estado: String
    let observacion: String?
    let fechaSolicitud: Date?
    let archivos: [Attachment]

    var id: Int { idVerificacion }
    var estadoLegible: String { Domain.EstadoVerificacionEstudiantil.label(estado) }
}

/// `identity/dto/IdentityVerificationStatusResponse` — KYC con Didit.
struct IdentityVerificationStatus: Codable, Equatable {
    let idVerificacion: Int?
    let estado: String
    let estadoProveedor: String?
    let verificada: Bool
    let operacionesProtegidasHabilitadas: Bool
    let mensaje: String?
    let fechaInicio: Date?
    let fechaActualizacion: Date?
    let fechaDecision: Date?
    let fechaExpiracion: Date?

    var estadoLegible: String { Domain.EstadoVerificacionIdentidad.label(estado) }

    /// Estados en los que tiene sentido ofrecer un nuevo intento.
    var puedeIniciarNuevoIntento: Bool {
        !verificada
    }
}

/// `identity/dto/IdentityVerificationSessionResponse`
struct IdentityVerificationSession: Codable {
    let idSesionProveedor: String?
    let urlVerificacion: String
    let estado: String?
}

// MARK: - Peticiones

struct RegisterRequest: Encodable {
    let nombre: String
    let apellido: String
    let correo: String
    let password: String
    let idUniversidad: Int?
    let idCarrera: Int?
    let carnetUniversitario: String?
}

struct LoginRequest: Encodable {
    let correo: String
    let password: String
}

struct VerifyLoginRequest: Encodable {
    let correo: String
    let codigo: String
}

struct VerifyEmailRequest: Encodable {
    let correo: String
    let codigo: String
}

struct ResendVerificationRequest: Encodable {
    let correo: String
}

struct ForgotPasswordRequest: Encodable {
    let correo: String
}

struct ResetPasswordRequest: Encodable {
    let correo: String
    let codigo: String
    let nuevaPassword: String
}

struct UpdateUsernameRequest: Encodable {
    let nombreUsuario: String
}

struct ReviewStudentVerificationRequest: Encodable {
    let observacion: String?
}
