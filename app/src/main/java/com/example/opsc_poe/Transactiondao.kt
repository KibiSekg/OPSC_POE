package com.example.opsc_poe.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.opsc_poe.db.entities.Transaction

@Dao
interface TransactionDao {
    @Insert
    fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE title = :title ORDER BY date DESC LIMIT 1")
    fun getLastTransactionByTitle(title: String): Transaction?
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: String, endDate: String): List<Transaction>
}