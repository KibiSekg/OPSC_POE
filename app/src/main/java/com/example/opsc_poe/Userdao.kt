package com.example.opsc_poe.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.opsc_poe.db.entities.User

@Dao
interface UserDao {

    @Insert
    fun insertUser(user: User)

    // Find user by email and password
    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    fun login(email: String, password: String): User?

    // FIX: Check if an email address is already taken
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users")
    fun getAllUsers(): List<User>
}