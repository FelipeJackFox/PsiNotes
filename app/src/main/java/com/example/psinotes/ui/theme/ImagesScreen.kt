@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.example.psinotes.ui.theme

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import android.Manifest
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun ImagesScreen(
    patientId: Long,
    patientName: String?,
    onBack: () -> Unit,
    onToast: (String) -> Unit
) {
    val pink = Color(0xFFFF2D6C)
    val ctx = LocalContext.current

    // Carpeta privada por paciente: .../files/Pictures/patient_<id>/
    val picsDir = remember {
        File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "patient_$patientId")
            .apply { mkdirs() }
    }

    // Permiso de cámara (solo CAMERA)
    var hasCamPerm by remember { mutableStateOf(false) }
    val camPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamPerm = granted }
    LaunchedEffect(Unit) { camPerm.launch(Manifest.permission.CAMERA) }

    // Lista de imágenes (como Uri de file provider)
    var images by remember { mutableStateOf(loadImagesAsUris(ctx, picsDir)) }
    fun refresh() { images = loadImagesAsUris(ctx, picsDir) }

    // Estado del visor full-screen
    var previewUri by remember { mutableStateOf<Uri?>(null) }

    // Captura con cámara → TAKE_PICTURE exige Uri de destino
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val takePic = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            onToast("Imagen guardada")
            refresh()
        } else {
            // si no tomó, elimina archivo vacío
            pendingPhotoUri?.let { uri ->
                runCatching { ctx.contentResolver.delete(uri, null, null) }
            }
        }
        pendingPhotoUri = null
    }

    fun startCamera() {
        if (!hasCamPerm) { onToast("Se requiere permiso de cámara"); camPerm.launch(Manifest.permission.CAMERA); return }
        val file = createImageFile(picsDir)
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        pendingPhotoUri = uri
        takePic.launch(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                title = { Text("Imágenes" + if (!patientName.isNullOrBlank()) " · $patientName" else "", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = pink)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = ::startCamera,
                containerColor = pink,
                contentColor = Color.White
            ) { Icon(Icons.Filled.CameraAlt, contentDescription = "Tomar foto") }
        }
    ) { inner ->
        if (images.isEmpty()) {
            Box(Modifier.padding(inner).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sin imágenes. Usa el botón de cámara para agregar.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(images, key = { it.toString() }) { uri ->
                    Card(
                        onClick = { previewUri = uri },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }

    // Visor fullscreen con zoom
    if (previewUri != null) {
        FullscreenPreview(
            imageUri = previewUri!!,
            onDismiss = { previewUri = null },
            accent = pink
        )
    }
}

/* ---------------- Helpers ---------------- */

private fun loadImagesAsUris(ctx: android.content.Context, dir: File): List<Uri> {
    val files = dir.listFiles { f -> f.isFile && f.extension.lowercase() in listOf("jpg","jpeg","png") }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()
    return files.map { f -> FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f) }
}

private fun createImageFile(dir: File): File {
    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return File(dir, "img_$ts.jpg")
}

@Composable
private fun FullscreenPreview(
    imageUri: Uri,
    onDismiss: () -> Unit,
    accent: Color
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,   // <-- sin márgenes: ocupa toda la pantalla
            decorFitsSystemWindows = false
        )
    ) {
        // Raíz negro FULL
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black,
            tonalElevation = 0.dp
        ) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = imageUri,
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
                        .padding(12.dp)
                ) {
                    TextButton(
                        onClick = { scale = 1f; offsetX = 0f; offsetY = 0f },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.85f))
                    ) { Text("Reset") }
                    Spacer(Modifier.width(4.dp))
                    FilledTonalButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = accent)
                    ) {
                        Text("Cerrar", color = Color.White)
                    }
                }
            }
        }
    }
}
