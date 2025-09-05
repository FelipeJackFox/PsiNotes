package com.example.psinotes.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.psinotes.data.AppDb
import com.example.psinotes.data.Patient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PatientViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).patients()

    val patients = dao.all() // Flow<List<Patient>>

    fun patient(id: Long): Flow<Patient?> = dao.byId(id)

    fun add(name: String, age: Int?, gender: String?, phone: String?) =
        viewModelScope.launch { dao.insert(Patient(fullName = name, age = age, gender = gender, phone = phone)) }

    fun update(id: Long, name: String, age: Int?, gender: String?, phone: String?) =
        viewModelScope.launch { dao.update(Patient(id = id, fullName = name, age = age, gender = gender, phone = phone)) }

    fun delete(p: Patient) = viewModelScope.launch { dao.delete(p) }
}
