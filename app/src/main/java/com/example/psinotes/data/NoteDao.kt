package com.example.psinotes.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM Note WHERE patientId = :patientId ORDER BY createdAt DESC")
    fun byPatient(patientId: Long): Flow<List<Note>>

    @Insert
    suspend fun insert(n: Note): Long

    @Delete
    suspend fun delete(n: Note)
}
