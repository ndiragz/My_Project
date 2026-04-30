package com.example.lostandfoundapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class ItemType {
    LOST, FOUND
}

enum class ItemStatus {
    PENDING, APPROVED, REJECTED
}

@Entity(tableName = "lost_items")
data class LostItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: String,
    val location: String,
    val date: String,
    val type: ItemType,
    val contactInfo: String,
    val imageUri: String? = null,
    val status: ItemStatus = ItemStatus.PENDING,
    val reporterEmail: String? = null,
    val reporterName: String? = null
)
