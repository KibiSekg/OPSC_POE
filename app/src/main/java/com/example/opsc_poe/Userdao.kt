package com.example.opsc_poe.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.opsc_poe.db.entities.User

@Dao
interface UserDao {

    @Insert
    fun insertUser(user: User)

    // Used for login: find user by email and password
    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    fun login(email: String, password: String): User?

    @Query("SELECT * FROM users")
    fun getAllUsers(): List<User>
}