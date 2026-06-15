package com.example.opsc_poe.db

import android.content.Context
import android.util.Log // Imported Android Log utility
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


@Database(
    entities = [Transaction::class, Category::class, Budget::class, User::class],
    version = 4, // 🆙 Incremented to force Room to re-index table structural definitions safely
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Abstraction connection points exposing specific query interfaces
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun userDao(): UserDao

    companion object {
        private const val TAG = "AppDatabase"

        // The Volatile keyword ensures that modifications made by one thread to this instance
        // are immediately visible to all other execution threads.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         Returns a thread-safe Singleton instance of the Room Database setup.
          Leverages Double-Checked Locking block barriers.
         */
        fun getDatabase(context: Context): AppDatabase {
            // If the instance already exists, return it immediately without entering the synchronized block
            INSTANCE?.let {
                Log.d(TAG, "getDatabase: Existing database instance retrieved from memory.")
                return it
            }

            // Synchronize access to prevent multiple threads from instantiating distinct database instances concurrently
            return synchronized(this) {
                // Double-check instance availability after acquiring lock barrier
                val existingInstance = INSTANCE
                if (existingInstance != null) {
                    Log.d(TAG, "getDatabase: Instance created by another thread just before lock acquisition.")
                    return existingInstance
                }

                Log.d(TAG, "getDatabase: Building a brand new Room database instance ('spend_smart_db').")

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spend_smart_db"
                )
                    // Note: Destructive migration wipes local database data cleanly if schema shifts without a explicit migration strategy.
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                Log.d(TAG, "getDatabase: Database instance successfully built and assigned.")
                instance
            }
        }
    }
}