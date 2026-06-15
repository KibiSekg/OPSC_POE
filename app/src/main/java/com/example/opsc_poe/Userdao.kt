package com.example.opsc_poe.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.opsc_poe.db.entities.User

@Dao
interface UserDao {

    @Insert
    fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun getUserByEmail(email: String): User?

    // Used by Login.kt — matches email AND password
    @Query("SELECT * FROM users WHERE email = :email AND passwordHash = :password LIMIT 1")
    fun login(email: String, password: String): User?
}