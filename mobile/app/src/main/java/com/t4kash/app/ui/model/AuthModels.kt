package com.t4kash.app.ui.model

data class LoginRequest(
    val correo: String,
    val password: String
)

data class RegisterRequest(
    val nombre: String,
    val apellido: String,
    val correo: String,
    val password: String
)

data class AuthenticatedUserDto(
    val idUsuario: Int,
    val nombre: String,
    val apellido: String,
    val correo: String,
    val estadoUsuario: String,
    val roles: List<String>
)

data class AuthResponse(
    val token: String,
    val fechaExpiracion: String,
    val usuario: AuthenticatedUserDto
)
