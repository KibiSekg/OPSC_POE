package com.example.opsc_poe.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.opsc_poe.db.dao.BudgetDao
import com.example.opsc_poe.db.dao.TransactionDao
import com.example.opsc_poe.db.dao.UserDao
import com.example.opsc_poe.db.entities.Budget
import com.example.opsc_poe.db.entities.Transaction
import com.example.opsc_poe.db.entities.User

@Database(
    entities = [User::class, Transaction::class, Budget::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "opsc_poe_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}