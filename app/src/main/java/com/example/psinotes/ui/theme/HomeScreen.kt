@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.psinotes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.psinotes.data.Patient
import com.example.psinotes.vm.PatientViewModel
import androidx.compose.material.icons.filled.Settings


@Composable
fun HomeScreen(
    onToast: (String) -> Unit,
    onOpenPatient: (Long) -> Unit,
    onOpenSettings: () -> Unit,            // <--- NUEVO
    vm: PatientViewModel = viewModel()
) {
    var showAdd by remember { mutableStateOf(false) }
    val pink = Color(0xFFFF2D6C)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pacientes", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuraciones")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = pink, titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = pink, contentColor = Color.White) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
            }
        }
    ) { inner ->
        val patients by vm.patients.collectAsState(initial = emptyList())
        Box(Modifier.padding(inner).fillMaxSize()) {
            if (patients.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin pacientes. Agrega el primero con +")
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(patients, key = { it.id }) { p ->
                        PatientItem(
                            patient = p,
                            onOpen = { onOpenPatient(p.id) },
                            onDelete = { vm.delete(p) }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddPatientDialog(
            onAdd = { name, age, gender, phone ->
                vm.add(name, age, gender, phone)
                onToast("Paciente agregado")
                showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }
}

@Composable
private fun PatientItem(
    patient: Patient,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.clickable { onOpen() }
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(patient.fullName, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
            }
            val subtitle = buildString {
                patient.age?.let { append("$it años") }
                if (!patient.gender.isNullOrBlank()) {
                    if (isNotEmpty()) append(" · "); append(patient.gender)
                }
                if (!patient.phone.isNullOrBlank()) {
                    if (isNotEmpty()) append(" · "); append(patient.phone)
                }
            }
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddPatientDialog(
    onAdd: (name: String, age: Int?, gender: String?, phone: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val pink = Color(0xFFFF2D6C)
    var name by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<String?>(null) }
    var phone by remember { mutableStateOf("") }
    var genderMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo paciente", fontWeight = FontWeight.Bold, color = pink) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre completo") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(ageText, {
                    ageText = it.filter { c -> c.isDigit() }.take(3)
                }, label = { Text("Edad") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = genderMenu, onExpandedChange = { genderMenu = it }) {
                    OutlinedTextField(
                        value = gender ?: "",
                        onValueChange = {},
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        label = { Text("Género") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderMenu) }
                    )
                    val items = listOf("Femenino", "Masculino", "No binario", "Otro", "Prefiero no decir")
                    ExposedDropdownMenu(expanded = genderMenu, onDismissRequest = { genderMenu = false }) {
                        items.forEach { item ->
                            DropdownMenuItem(text = { Text(item) }, onClick = {
                                gender = item; genderMenu = false
                            })
                        }
                    }
                }
                OutlinedTextField(phone, { phone = it.take(20) },
                    label = { Text("Teléfono (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val age = ageText.toIntOrNull()
                    if (name.isNotBlank()) onAdd(name.trim(), age, gender, phone.ifBlank { null })
                },
                colors = ButtonDefaults.textButtonColors(contentColor = pink)
            ) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
