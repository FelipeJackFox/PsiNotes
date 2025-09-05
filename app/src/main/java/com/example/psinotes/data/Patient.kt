package com.example.psinotes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Patient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val age: Int? = null,
    val gender: String? = null,
    val phone: String? = null
)
