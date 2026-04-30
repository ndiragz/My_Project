package com.example.lostandfoundapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    USER, ADMIN
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val email: String,
    val fullName: String,
    val password: String,
    val role: UserRole = UserRole.USER,
    val profilePicUri: String? = null
)
