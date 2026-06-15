package com.example.opsc_poe.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.opsc_poe.db.dao.TransactionDao
import com.example.opsc_poe.db.dao.CategoryDao
import com.example.opsc_poe.db.dao.BudgetDao
import com.example.opsc_poe.db.dao.UserDao
import com.example.opsc_poe.db.entities.Transaction
import com.example.opsc_poe.db.entities.Category
import com.example.opsc_poe.db.entities.Budget
import com.example.opsc_poe.db.entities.User

// Increment the version number to 3 to force schema update migration safely
@Database(
    entities = [Transaction::class, Category::class, Budget::class, User::class],
    version = 4, // 🆙 Incremented to force Room to re-index the table maps correctly
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spend_smart_db"
                )
                    .fallbackToDestructiveMigration() // Wipes safely if schema shifts during testing
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}