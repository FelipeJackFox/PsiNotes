package com.example.psinotes.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM Patient ORDER BY fullName")
    fun all(): Flow<List<Patient>>

    @Query("SELECT * FROM Patient WHERE id = :id LIMIT 1")
    fun byId(id: Long): Flow<Patient?>

    @Insert
    suspend fun insert(p: Patient): Long

    @Update
    suspend fun update(p: Patient)

    @Delete
    suspend fun delete(p: Patient)
}
