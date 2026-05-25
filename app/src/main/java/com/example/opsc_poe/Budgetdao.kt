package com.example.opsc_poe.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.opsc_poe.db.entities.Budget

@Dao
interface BudgetDao {

    @Insert
    fun insertBudget(budget: Budget)

    // Always get the most recently inserted budget
    @Query("SELECT * FROM budget ORDER BY id DESC LIMIT 1")
    fun getLatestBudget(): Budget?
}