package com.rushgames.barrier.data.model

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String = "",
    val authToken: String = "",
    val isGuest: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
