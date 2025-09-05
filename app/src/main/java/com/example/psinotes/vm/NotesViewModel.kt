package com.example.psinotes.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.psinotes.data.AppDb
import com.example.psinotes.data.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).notes()

    fun notesOf(patientId: Long): Flow<List<Note>> = dao.byPatient(patientId)

    fun add(patientId: Long, title: String, body: String) = viewModelScope.launch {
        dao.insert(Note(patientId = patientId, title = title, body = body))
    }

    fun delete(note: Note) = viewModelScope.launch {
        dao.delete(note)
    }
}
