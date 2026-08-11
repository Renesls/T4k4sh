package com.t4kash.app.ui.components

import android.content.Context
import androidx.core.content.edit

enum class ChatBackgroundTheme(
    val storageValue: String,
    val title: String,
    val description: String
) {
    T4KASH(
        storageValue = "t4kash",
        title = "Geometría T4KASH",
        description = "Figuras suaves con los colores de la marca."
    ),
    GRID(
        storageValue = "grid",
        title = "Cuadrícula",
        description = "Una trama ordenada y discreta."
    ),
    WAVES(
        storageValue = "waves",
        title = "Ondas",
        description = "Círculos amplios con un estilo más fluido."
    ),
    CLEAN(
        storageValue = "clean",
        title = "Limpio",
        description = "Fondo claro sin elementos decorativos."
    );

    companion object {
        fun fromStorage(value: String?): ChatBackgroundTheme {
            return entries.firstOrNull { it.storageValue == value } ?: T4KASH
        }
    }
}

object AppPreferences {
    private const val PREFERENCES_NAME = "t4kash_preferences"
    private const val CHAT_BACKGROUND_KEY = "chat_background_theme"

    fun getChatBackgroundTheme(context: Context): ChatBackgroundTheme {
        val preferences = context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
        return ChatBackgroundTheme.fromStorage(
            preferences.getString(CHAT_BACKGROUND_KEY, null)
        )
    }

    fun setChatBackgroundTheme(context: Context, theme: ChatBackgroundTheme) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit { putString(CHAT_BACKGROUND_KEY, theme.storageValue) }
    }
}
