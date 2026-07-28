package com.t4kash.app.ui.model

data class LoginRequest(
    val correo: String,
    val password: String
)

data class RegisterRequest(
    val nombre: String,
    val apellido: String,
    val correo: String,
    val password: String,
    val idUniversidad: Int,
    val idCarrera: Int
)

data class VerifyEmailRequest(
    val correo: String,
    val codigo: String
)

data class ResendVerificationRequest(
    val correo: String
)

data class RegistrationResponse(
    val correo: String,
    val fechaExpiracion: String,
    val mensaje: String
)

data class UniversityDto(
    val idUniversidad: Int,
    val nombreUniversidad: String,
    val dominioCorreo: String
)

data class CareerDto(
    val idCarrera: Int,
    val nombreCarrera: String,
    val idUniversidad: Int
)

data class AuthenticatedUserDto(
    val idUsuario: Int,
    val nombre: String,
    val apellido: String,
    val correo: String,
    val idUniversidad: Int?,
    val nombreUniversidad: String?,
    val idCarrera: Int?,
    val nombreCarrera: String?,
    val estadoUsuario: String,
    val roles: List<String>
)

data class AuthResponse(
    val token: String,
    val fechaExpiracion: String,
    val usuario: AuthenticatedUserDto
)
