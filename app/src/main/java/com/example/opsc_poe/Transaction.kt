package com.example.opsc_poe.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val transactionType: String,   // "income" or "expense"
    val category: String,
    val date: String               // format: yyyy-MM-dd
)