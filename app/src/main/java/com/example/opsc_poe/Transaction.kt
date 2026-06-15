package com.example.opsc_poe.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String?,
    val amount: Double,
    val transactionType: String,
    val category: String,
    val date: String,
    val imagePath: String?,
    val rating: String = "NONE" // "THUMBS_UP", "THUMBS_DOWN", or "NONE"
)