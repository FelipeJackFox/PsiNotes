@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.psinotes.ui

import androidx.compose.material.icons.filled.Delete
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.psinotes.data.Note
import com.example.psinotes.ui.sensors.TiltToMeSimple
import com.example.psinotes.vm.NotesViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotesScreen(
    patientId: Long,
    patientName: String?,
    startWithNewNote: Boolean = false,
    onBack: () -> Unit,
    onToast: (String) -> Unit,
    vm: NotesViewModel = viewModel()
) {
    val pink = Color(0xFFFF2D6C)
    val ctx = LocalContext.current
    val notes by vm.notesOf(patientId).collectAsState(initial = emptyList())

    var showNew by remember { mutableStateOf(startWithNewNote) }

    // ====== Gesto simple: “inclinar hacia ti” ======
    // Elige sensibilidad cambiando "triggerPitchDeg":
    //  -25f (Permisivo) / -35f (Balanceado) / -45f (Estricto)
    TiltToMeSimple(
        context = ctx,
        onTrigger = {
            showNew = true
            onToast("Gesto detectado: creando nueva nota")
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
                title = {
                    Text(
                        "Notas" + if (!patientName.isNullOrBlank()) " · $patientName" else "",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = pink)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNew = true }, containerColor = pink, contentColor = Color.White) {
                Icon(Icons.Filled.Add, contentDescription = "Nueva nota")
            }
        }
    ) { inner ->
        if (notes.isEmpty()) {
            Box(Modifier.padding(inner).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sin notas. Crea la primera con + o con el gesto hacia ti.")
            }
        } else {
            LazyColumn(
                Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(note = note, onDelete = { vm.delete(note) })
                }
            }
        }
    }

    if (showNew) {
        NewNoteDialog(
            onSave = { title, body ->
                vm.add(patientId, title, body)
                showNew = false
                onToast("Nota creada")
            },
            onDismiss = { showNew = false }
        )
    }
}

/* ----------------------- UI helpers ----------------------- */

@Composable
private fun NoteCard(note: Note, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    ElevatedCard {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(note.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
            }
            Spacer(Modifier.height(4.dp))
            Text(note.body, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                fmt.format(Date(note.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NewNoteDialog(onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    val pink = Color(0xFFFF2D6C)
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva nota", color = pink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it.take(80) }, label = { Text("Título") }, singleLine = true)
                OutlinedTextField(body, { body = it.take(1000) }, label = { Text("Nota") }, minLines = 4)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank() && body.isNotBlank()) onSave(title.trim(), body.trim()) },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF2D6C))
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
