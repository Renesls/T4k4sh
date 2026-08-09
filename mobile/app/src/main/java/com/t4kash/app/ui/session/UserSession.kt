package com.t4kash.app.ui.session

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SessionUser(
    val id: Int,
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val universityName: String?,
    val careerName: String?,
    val accountStatus: String,
    val roles: Set<String>
) {
    val fullName: String
        get() = "$firstName $lastName".trim()

    val initials: String
        get() = listOf(firstName, lastName)
            .mapNotNull { it.trim().firstOrNull()?.uppercase() }
            .joinToString("")
            .take(2)
            .ifBlank { "TK" }
}

data class AuthSession(
    val token: String,
    val expiresAt: String,
    val user: SessionUser
)

object UserSession {
    private const val PREFERENCES_NAME = "t4kash_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_FIRST_NAME = "first_name"
    private const val KEY_LAST_NAME = "last_name"
    private const val KEY_EMAIL = "email"
    private const val KEY_UNIVERSITY_NAME = "university_name"
    private const val KEY_CAREER_NAME = "career_name"
    private const val KEY_ACCOUNT_STATUS = "account_status"
    private const val KEY_ROLES = "roles"

    private var preferences: SharedPreferences? = null
    private var secureTokenStore: SecureTokenStore? = null
    private val mutableSession = MutableStateFlow<AuthSession?>(null)

    val session: StateFlow<AuthSession?> = mutableSession.asStateFlow()
    val current: AuthSession?
        get() = mutableSession.value
    val accessToken: String?
        get() = current?.token

    fun initialize(context: Context) {
        if (preferences != null) return
        preferences = context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
        secureTokenStore = SecureTokenStore(requirePreferences())
        migrateLegacyToken()
        mutableSession.value = readSession()
    }

    fun save(session: AuthSession) {
        requireTokenStore().save(session.token)
        requirePreferences().edit()
            .remove(KEY_TOKEN)
            .putString(KEY_EXPIRES_AT, session.expiresAt)
            .putInt(KEY_USER_ID, session.user.id)
            .putString(KEY_USERNAME, session.user.username)
            .putString(KEY_FIRST_NAME, session.user.firstName)
            .putString(KEY_LAST_NAME, session.user.lastName)
            .putString(KEY_EMAIL, session.user.email)
            .putString(KEY_UNIVERSITY_NAME, session.user.universityName)
            .putString(KEY_CAREER_NAME, session.user.careerName)
            .putString(KEY_ACCOUNT_STATUS, session.user.accountStatus)
            .putStringSet(KEY_ROLES, session.user.roles)
            .apply()
        mutableSession.value = session
    }

    fun updateUser(user: SessionUser) {
        current?.let { save(it.copy(user = user)) }
    }

    fun clear() {
        requirePreferences().edit().clear().apply()
        mutableSession.value = null
    }

    fun requireUserId(): Int {
        return current?.user?.id
            ?: error("No existe una sesion de usuario activa.")
    }

    private fun readSession(): AuthSession? {
        val prefs = requirePreferences()
        val token = requireTokenStore().read()?.takeIf { it.isNotBlank() }
            ?: return null
        val userId = prefs.getInt(KEY_USER_ID, -1).takeIf { it > 0 }
            ?: return null

        return AuthSession(
            token = token,
            expiresAt = prefs.getString(KEY_EXPIRES_AT, "").orEmpty(),
            user = SessionUser(
                id = userId,
                username = prefs.getString(KEY_USERNAME, "").orEmpty(),
                firstName = prefs.getString(KEY_FIRST_NAME, "").orEmpty(),
                lastName = prefs.getString(KEY_LAST_NAME, "").orEmpty(),
                email = prefs.getString(KEY_EMAIL, "").orEmpty(),
                universityName = prefs.getString(KEY_UNIVERSITY_NAME, null),
                careerName = prefs.getString(KEY_CAREER_NAME, null),
                accountStatus = prefs.getString(KEY_ACCOUNT_STATUS, "ACTIVO").orEmpty(),
                roles = prefs.getStringSet(KEY_ROLES, emptySet()).orEmpty().toSet()
            )
        )
    }

    private fun requirePreferences(): SharedPreferences {
        return checkNotNull(preferences) {
            "UserSession debe inicializarse antes de utilizarse."
        }
    }

    private fun requireTokenStore(): SecureTokenStore {
        return checkNotNull(secureTokenStore) {
            "UserSession debe inicializarse antes de utilizarse."
        }
    }

    private fun migrateLegacyToken() {
        val prefs = requirePreferences()
        val store = requireTokenStore()
        if (store.hasEncryptedToken()) {
            prefs.edit().remove(KEY_TOKEN).apply()
            return
        }
        val legacyToken = prefs.getString(KEY_TOKEN, null)
            ?.takeIf { it.isNotBlank() }
            ?: return
        store.save(legacyToken)
        prefs.edit().remove(KEY_TOKEN).apply()
    }
}
