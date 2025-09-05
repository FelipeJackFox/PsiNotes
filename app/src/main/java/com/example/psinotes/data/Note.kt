package com.example.psinotes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val title: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis()
)
