package com.example.lostandfoundapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "app_ratings")
data class Rating(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userEmail: String,
    val userName: String,
    val rating: Int,
    val feedback: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)
