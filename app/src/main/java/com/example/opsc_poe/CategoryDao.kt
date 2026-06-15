package com.example.opsc_poe.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.opsc_poe.db.entities.Category

@Dao
interface CategoryDao {
    @Query("SELECT * FROM custom_categories")
    fun getAllCategories(): List<Category>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertCategory(category: Category)
}