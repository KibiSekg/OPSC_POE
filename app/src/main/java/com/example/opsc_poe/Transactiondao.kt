package com.example.opsc_poe.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.opsc_poe.db.entities.Transaction

@Dao
interface TransactionDao {

    @Insert
    fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions")
    fun getAllTransactions(): List<Transaction>

    // Filter by date range — used for This Week / This Month / Last Month
    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate")
    fun getTransactionsByDateRange(startDate: String, endDate: String): List<Transaction>
}