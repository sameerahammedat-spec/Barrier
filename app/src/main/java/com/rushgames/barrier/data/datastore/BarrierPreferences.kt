package com.rushgames.barrier.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.rushgames.barrier.data.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "barrier_prefs")

@Singleton
class BarrierPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_STRICT_MODE_GLOBAL = booleanPreferencesKey("strict_mode_global")
        val KEY_ADULT_PROTECTION = booleanPreferencesKey("adult_protection")
        val KEY_ACTIVE_SESSION_ID = longPreferencesKey("active_session_id")
        val KEY_ACCESSIBILITY_ENABLED = booleanPreferencesKey("accessibility_enabled")
        val KEY_VPN_ENABLED = booleanPreferencesKey("vpn_enabled")
        val KEY_USAGE_STATS_GRANTED = booleanPreferencesKey("usage_stats_granted")
        val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val KEY_SHOW_ONBOARDING = booleanPreferencesKey("show_onboarding")
        // Legacy recovery_pin values are intentionally left untouched for data preservation.

        // Auth & User Session Keys
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_IS_GUEST = booleanPreferencesKey("is_guest")
    }

    val theme: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_THEME] ?: "system" }

    val strictModeGlobal: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_STRICT_MODE_GLOBAL] ?: false }

    val adultProtection: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ADULT_PROTECTION] ?: false }

    val activeSessionId: Flow<Long> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ACTIVE_SESSION_ID] ?: -1L }

    val showOnboarding: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_SHOW_ONBOARDING] ?: true }

    val user: Flow<User?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val id = prefs[KEY_USER_ID] ?: return@map null
            User(
                id = id,
                email = prefs[KEY_USER_EMAIL] ?: "",
                displayName = prefs[KEY_USER_NAME] ?: "Barrier User",
                authToken = prefs[KEY_AUTH_TOKEN] ?: "",
                isGuest = prefs[KEY_IS_GUEST] ?: false
            )
        }

    suspend fun setTheme(theme: String) {
        dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun setStrictModeGlobal(enabled: Boolean) {
        dataStore.edit { it[KEY_STRICT_MODE_GLOBAL] = enabled }
    }

    suspend fun setAdultProtection(enabled: Boolean) {
        dataStore.edit { it[KEY_ADULT_PROTECTION] = enabled }
    }

    suspend fun setActiveSessionId(id: Long) {
        dataStore.edit { it[KEY_ACTIVE_SESSION_ID] = id }
    }

    suspend fun setShowOnboarding(show: Boolean) {
        dataStore.edit { it[KEY_SHOW_ONBOARDING] = show }
    }

    suspend fun saveUser(user: User) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = user.id
            prefs[KEY_USER_EMAIL] = user.email
            prefs[KEY_USER_NAME] = user.displayName
            prefs[KEY_AUTH_TOKEN] = user.authToken
            prefs[KEY_IS_LOGGED_IN] = false
            prefs[KEY_IS_GUEST] = user.isGuest
        }
    }

    suspend fun clearUser() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_EMAIL)
            prefs.remove(KEY_USER_NAME)
            prefs.remove(KEY_AUTH_TOKEN)
            prefs[KEY_IS_LOGGED_IN] = false
            prefs[KEY_IS_GUEST] = false
        }
    }
}
