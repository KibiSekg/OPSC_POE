package com.example.opsc_poe.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val monthlyBudget: Double
)