package com.rushgames.barrier.data.repository

import com.rushgames.barrier.data.datastore.BarrierPreferences
import com.rushgames.barrier.data.model.User
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val prefs: BarrierPreferences
) {
    val currentUser: Flow<User?> = prefs.user
    val isLoggedIn: Flow<Boolean> = prefs.isLoggedIn

    suspend fun signInWithGoogle(idToken: String, email: String, name: String): Result<User> {
        return try {
            val user = User(
                id = UUID.randomUUID().toString(),
                email = email.ifBlank { "user@google.com" },
                displayName = name.ifBlank { "Google User" },
                authToken = idToken,
                isGuest = false
            )
            prefs.saveUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User> {
        return try {
            if (email.isBlank() || password.length < 6) {
                return Result.failure(IllegalArgumentException("Email cannot be empty and password must be at least 6 characters."))
            }
            val user = User(
                id = UUID.randomUUID().toString(),
                email = email.trim(),
                displayName = displayName.ifBlank { email.substringBefore("@") },
                authToken = "mock_token_${System.currentTimeMillis()}",
                isGuest = false
            )
            prefs.saveUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithEmail(email: String, password: String): Result<User> {
        return try {
            if (email.isBlank() || password.isBlank()) {
                return Result.failure(IllegalArgumentException("Email and password are required."))
            }
            val user = User(
                id = UUID.randomUUID().toString(),
                email = email.trim(),
                displayName = email.substringBefore("@"),
                authToken = "mock_token_${System.currentTimeMillis()}",
                isGuest = false
            )
            prefs.saveUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun continueAsGuest(): Result<User> {
        return try {
            val guestUser = User(
                id = "guest_${UUID.randomUUID()}",
                email = "guest@barrier.local",
                displayName = "Focus Guest",
                authToken = "guest_token",
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
