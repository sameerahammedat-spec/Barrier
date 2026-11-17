package com.rushgames.barrier.data.repository

import com.rushgames.barrier.data.datastore.BarrierPreferences
import com.rushgames.barrier.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class AuthBackendUnavailableException(operation: String) : IllegalStateException(
    "$operation is unavailable because Firebase Authentication has not been configured for this app."
)

@Singleton
class AuthRepository @Inject constructor(
    private val prefs: BarrierPreferences
) {
    // Legacy locally stored tokens are never exposed as usable credentials.
    // Legacy locally stored tokens are never exposed as usable credentials.
    val currentUser: Flow<User?> = prefs.user.map { user -> user?.copy(authToken = "") }.map { user -> user?.copy(authToken = "") }
    // This repository has no configured identity provider, so local flags must not imply authentication.
    val isLoggedIn: Flow<Boolean> = flowOf(false)

    fun signInWithGoogle(): Result<User> =
        Result.failure(AuthBackendUnavailableException("Google sign-in"))

    fun signUpWithEmail(): Result<User> =
        Result.failure(AuthBackendUnavailableException("Email registration"))

    fun loginWithEmail(): Result<User> =
        Result.failure(AuthBackendUnavailableException("Email sign-in"))

    suspend fun continueAsGuest(): Result<User> {
        return try {
            val guestUser = User(
                id = "guest_${UUID.randomUUID()}",
                email = "guest@barrier.local",
                displayName = "Focus Guest",
                isGuest = true
            )
            prefs.saveUser(guestUser)
            Result.success(guestUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        prefs.clearUser()
    }
}
