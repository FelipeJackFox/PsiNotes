package com.example.psinotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Patient::class, Note::class],
    version = 4   // <- subimos versión por nueva tabla
)
abstract class AppDb : RoomDatabase() {
    abstract fun patients(): PatientDao
    abstract fun notes(): NoteDao

    companion object {
        @Volatile private var I: AppDb? = null
        fun get(ctx: Context): AppDb =
            I ?: synchronized(this) {
                I ?: Room.databaseBuilder(ctx, AppDb::class.java, "psinotes.db")
                    .fallbackToDestructiveMigration() // OK en desarrollo
                    .build().also { I = it }
            }
    }
}
