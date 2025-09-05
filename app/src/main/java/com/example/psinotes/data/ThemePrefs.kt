package com.example.psinotes.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { AUTO, LIGHT, DARK }

private val Context.dataStore by preferencesDataStore("settings")

object ThemePrefs {
    private val THEME_MODE = intPreferencesKey("theme_mode")

    fun modeFlow(ctx: Context): Flow<ThemeMode> =
        ctx.dataStore.data.map { prefs ->
            when (prefs[THEME_MODE] ?: 0) {
                1 -> ThemeMode.LIGHT
                2 -> ThemeMode.DARK
                else -> ThemeMode.AUTO
            }
        }

    suspend fun setMode(ctx: Context, mode: ThemeMode) {
        ctx.dataStore.edit { it[THEME_MODE] = when (mode) {
            ThemeMode.AUTO -> 0
            ThemeMode.LIGHT -> 1
            ThemeMode.DARK -> 2
        } }
    }
}
