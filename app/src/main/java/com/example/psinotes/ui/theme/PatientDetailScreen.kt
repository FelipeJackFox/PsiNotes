@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.psinotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.psinotes.data.Patient
import com.example.psinotes.ui.sensors.TiltToMeSimple
import com.example.psinotes.vm.PatientViewModel

@Composable
fun PatientDetailScreen(
    patientId: Long,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onToast: (String) -> Unit,
    onOpenNotesRoute: (String) -> Unit,
    vm: PatientViewModel = viewModel()
) {
    val pink = Color(0xFFFF2D6C)

    // Cargar paciente por id. Al inicio será null -> mostramos loading.
    val patient by vm.patient(patientId).collectAsState(initial = null)

    // Detectar si ya cargó alguna vez (para saber si un null posterior es por eliminación)
    var hadValue by remember { mutableStateOf(false) }
    LaunchedEffect(patient) {
        if (patient != null) hadValue = true
        if (hadValue && patient == null) {
            // ya existía y ahora desapareció (p.ej. tras eliminar) -> volver atrás
            onBack()
        }
    }

    var showEdit by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val ctx = LocalContext.current

    // ====== Gesto simple: “inclinar hacia ti” (preset Balanceado) ======
    // Si quieres más/menos sensible, cambia triggerPitchDeg:
    //  -25f (Permisivo) / -35f (Balanceado) / -45f (Estricto)
    TiltToMeSimple(
        context = ctx,
        onTrigger = {
            if (patient != null) {
                val encodedName = java.net.URLEncoder.encode(patient!!.fullName ?: "", "UTF-8")
                onOpenNotesRoute("notes/${patient!!.id}/$encodedName?newNote=1")
                onToast("Gesto detectado: abriendo Notas y creando nueva")
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                title = { Text("Paciente", fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
                    if (patient != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = pink)
            )
        }
    ) { inner ->
        if (patient == null) {
            // Primera carga: spinner
            Box(Modifier.padding(inner).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        // CONTENIDO cuando ya tenemos paciente
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEDEDED)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF8A8A8A),
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    ChipLabel(text = patient!!.fullName, bg = pink)
                    Spacer(Modifier.height(8.dp))
                    ChipLabel(text = patient!!.age?.let { "$it años" } ?: "—", bg = pink)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showEdit = true }) {
                    Icon(Icons.Default.Create, contentDescription = "Editar", tint = pink)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Información del paciente:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            InfoRow("Género", patient!!.gender ?: "—")
            InfoRow("Teléfono", patient!!.phone ?: "—")
            Spacer(Modifier.height(16.dp))
            SectionButton("NOTAS", pink) {
                val encodedName = java.net.URLEncoder.encode(patient!!.fullName ?: "", "UTF-8")
                onOpenNotesRoute("notes/${patient!!.id}/$encodedName?newNote=0")
            }
            Spacer(Modifier.height(10.dp))
            SectionButton("GRABACIONES", pink) {
                val encodedName = java.net.URLEncoder.encode(patient!!.fullName ?: "", "UTF-8")
                onOpenNotesRoute("recordings/${patient!!.id}/$encodedName")
            }
            Spacer(Modifier.height(10.dp))
            SectionButton("IMÁGENES", pink) {
                val encodedName = java.net.URLEncoder.encode(patient!!.fullName ?: "", "UTF-8")
                onOpenNotesRoute("images/${patient!!.id}/$encodedName")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // Diálogo Editar (solo cuando tenemos paciente)
    if (patient != null && showEdit) {
        EditPatientDialog(
            patient = patient!!,
            onDismiss = { showEdit = false },
            onSave = { name, age, gender, phone ->
                vm.update(patient!!.id, name, age, gender, phone)
                showEdit = false
                onToast("Datos actualizados")
            }
        )
    }

    // Confirmación de borrado
    if (patient != null && showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar paciente") },
            text = { Text("¿Seguro que deseas eliminar a ${patient!!.fullName}? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.delete(patient!!)
                        showDeleteConfirm = false
                        onDeleted()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") } }
        )
    }
}

/* ---------- componentes reutilizables ---------- */

@Composable
private fun ChipLabel(text: String, bg: Color) {
    Surface(color = bg, contentColor = Color.White, shape = RoundedCornerShape(10.dp)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(110.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) { Text(text, fontWeight = FontWeight.ExtraBold, color = Color.White) }
}

/** Diálogo para editar datos del paciente */
@Composable
private fun EditPatientDialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onSave: (name: String, age: Int?, gender: String?, phone: String?) -> Unit
) {
    val pink = Color(0xFFFF2D6C)
    var name by remember { mutableStateOf(patient.fullName) }
    var ageText by remember { mutableStateOf(patient.age?.toString() ?: "") }
    var gender by remember { mutableStateOf<String?>(patient.gender) }
    var phone by remember { mutableStateOf(patient.phone ?: "") }
    var genderMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar paciente", fontWeight = FontWeight.Bold, color = pink) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nombre completo") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Edad") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
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
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it.take(20) },
                    label = { Text("Teléfono (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val age = ageText.toIntOrNull()
                    if (name.isNotBlank()) onSave(name.trim(), age, gender, phone.ifBlank { null })
                },
                colors = ButtonDefaults.textButtonColors(contentColor = pink)
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
