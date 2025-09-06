@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.psinotes.ui.theme

import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.layout.ContentScale
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/* ============================================================
   Galería de Imágenes — por paciente, con borrado y visor zoom
   ============================================================ */

@Composable
fun ImagesScreen(
    patientId: Long,
    patientName: String?,
    onBack: () -> Unit,
    onToast: (String) -> Unit
) {
    val pink = Color(0xFFFF2D6C)
    val ctx = androidx.compose.ui.platform.LocalContext.current

    // Carpeta por paciente
    val picsDir = remember(patientId) {
        File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "patient_$patientId").apply { mkdirs() }
    }

    // Lista de imágenes (archivo + uri)
    var images by remember { mutableStateOf(loadImages(ctx, picsDir)) }
    fun refresh() { images = loadImages(ctx, picsDir) }

    // Estado del visor
    var preview by remember { mutableStateOf<Img?>(null) }

    // Tomar foto
    var pendingFile by remember { mutableStateOf<File?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = pendingFile
        if (ok && f != null && f.exists()) {
            refresh()
            onToast("Foto guardada")
        } else {
            // limpiar si se canceló
            if (f != null && f.exists()) runCatching { f.delete() }
        }
        pendingFile = null
    }

    fun startCamera() {
        val f = createImageFile(picsDir)
        pendingFile = f
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
        takePicture.launch(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                title = {
                    Text(
                        "Imágenes" + if (!patientName.isNullOrBlank()) " · $patientName" else "",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = pink)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { startCamera() },
                containerColor = pink,
                contentColor = Color.White
            ) { Icon(Icons.Filled.AddAPhoto, contentDescription = "Tomar foto") }
        }
    ) { inner ->
        if (images.isEmpty()) {
            Box(Modifier.padding(inner).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aún no hay fotos. Usa el botón para tomar una.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(images, key = { it.file.absolutePath }) { img ->
                    ElevatedCard(
                        onClick = { preview = img },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    ) {
                        AsyncImage(
                            model = img.uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }

    // Visor fullscreen con Reset / Eliminar / Cerrar
    if (preview != null) {
        FullscreenPreview(
            image = preview!!,
            onDismiss = { preview = null },
            onDelete = { img ->
                // intenta borrar por ContentResolver, si no por File
                val byResolver = runCatching { ctx.contentResolver.delete(img.uri, null, null) }.getOrNull() ?: 0
                if (byResolver == 0) runCatching { img.file.delete() }
                refresh()
                onToast("Imagen eliminada")
                preview = null
            },
            accent = pink
        )
    }
}

/* --------------------- Helpers / modelos --------------------- */

private data class Img(val file: File, val uri: Uri)

private fun loadImages(ctx: android.content.Context, dir: File): List<Img> {
    val files = dir.listFiles { f -> f.isFile && f.extension.lowercase() in listOf("jpg","jpeg","png") }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()
    return files.map { f ->
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
        Img(file = f, uri = uri)
    }
}

private fun createImageFile(dir: File): File {
    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return File(dir, "img_$ts.jpg")
}

/* --------------------- Visor fullscreen --------------------- */

@Composable
private fun FullscreenPreview(
    image: Img,
    onDismiss: () -> Unit,
    onDelete: (Img) -> Unit,
    accent: Color
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var askDelete by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black, tonalElevation = 0.dp) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = image.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = offsetX
                            translationY = offsetY
                            scaleX = scale
                            scaleY = scale
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                val factor = if (scale > 1f) 1f else 0f
                                offsetX += pan.x * factor
                                offsetY += pan.y * factor
                            }
                        },
                    contentScale = ContentScale.Fit
                )

                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { scale = 1f; offsetX = 0f; offsetY = 0f },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.85f))
                    ) { Text("Reset") }

                    Spacer(Modifier.width(6.dp))

                    FilledTonalButton(
                        onClick = { askDelete = true },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Eliminar", color = Color.White)
                    }

                    Spacer(Modifier.width(6.dp))

                    FilledTonalButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = accent)
                    ) { Text("Cerrar", color = Color.White) }
                }
            }
        }
    }

    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            title = { Text("Eliminar imagen") },
            text = { Text("¿Seguro que deseas eliminar esta imagen? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = { askDelete = false; onDelete(image) },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { askDelete = false }) { Text("Cancelar") } }
        )
    }
}
