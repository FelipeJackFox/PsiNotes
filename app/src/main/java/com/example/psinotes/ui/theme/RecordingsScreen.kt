@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.psinotes.ui.theme

import android.Manifest
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.psinotes.ui.theme.sensors.FaceUpDownListener
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordingsScreen(
    patientId: Long,
    patientName: String?,
    onBack: () -> Unit,
    onToast: (String) -> Unit
) {
    val pink = Color(0xFFFF2D6C)
    val ctx = LocalContext.current

    // ===== Permiso MIC =====
    var hasPermission by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )
    LaunchedEffect(Unit) { permLauncher.launch(Manifest.permission.RECORD_AUDIO) }

    // ===== Carpeta por paciente =====
    val recDir = remember {
        File(ctx.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "patient_$patientId").apply { mkdirs() }
    }

    // ===== Estado de grabación =====
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var currentFile by remember { mutableStateOf<File?>(null) }
    var recordStartMs by remember { mutableStateOf(0L) }
    var elapsedMs by remember { mutableStateOf(0L) }
    var lastSavedFile by remember { mutableStateOf<File?>(null) }
    val promptRenameAfterSave = true

    // ===== Estados para eliminar / renombrar =====
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var renameTarget by remember { mutableStateOf<File?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }


    // Timer de grabación
    LaunchedEffect(isRecording, isPaused) {
        while (isRecording) {
            if (!isPaused) elapsedMs = System.currentTimeMillis() - recordStartMs
            delay(200)
        }
    }

    // ===== Listado de archivos =====
    var recordings by remember { mutableStateOf(emptyList<File>()) }
    fun refreshList() { recordings = recDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList() }
    LaunchedEffect(recDir) { refreshList() }

    // ===== Reproductor simple =====
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingPath by remember { mutableStateOf<String?>(null) }
    var playPosMs by remember { mutableStateOf(0) }
    var playDurMs by remember { mutableStateOf(1) }

    LaunchedEffect(playingPath) {
        while (playingPath != null) {
            player?.let {
                playPosMs = it.currentPosition
                playDurMs = it.duration.coerceAtLeast(1)
            }
            delay(200)
        }
    }

    fun stopAndReleasePlayer() {
        try { player?.stop(); player?.release() } catch (_: Exception) {}
        player = null
        playingPath = null
    }

    fun formatHMS(ms: Long): String {
        val total = (ms / 1000).toInt()
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    fun fileDurationMs(file: File): Long {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(file.absolutePath)
            val d = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            mmr.release(); d
        } catch (_: Exception) { 0L }
    }

    fun sanitizeBaseName(raw: String): String {
        val trimmed = raw.trim().ifBlank { "grabacion" }
        // sólo letras/números/espacio/-_
        val s = trimmed.map { c ->
            if (c.isLetterOrDigit() || c == ' ' || c == '-' || c == '_') c else '_'
        }.joinToString("")
        return s.replace(Regex("\\s+"), " ").trim()
    }

    fun uniqueFileIn(dir: File, baseNoExt: String): File {
        var name = "$baseNoExt.m4a"
        var f = File(dir, name)
        var i = 1
        while (f.exists() && i < 1000) {
            name = "$baseNoExt ($i).m4a"
            f = File(dir, name)
            i++
        }
        return f
    }

    fun startRecording() {
        if (!hasPermission) { onToast("Permiso de micrófono requerido"); return }
        if (isRecording) return

        val defaultBase = "rec_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
        val file = File(recDir, "$defaultBase.m4a")
        currentFile = file

        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(ctx) else MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(128_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(file.absolutePath)

        try {
            r.prepare()
            r.start()
            recorder = r
            isRecording = true
            isPaused = false
            recordStartMs = System.currentTimeMillis()
            elapsedMs = 0
            onToast("Grabando… boca abajo = pausa, boca arriba = continúa")
        } catch (e: Exception) {
            onToast("Error al iniciar grabación")
            try { r.release() } catch (_: Exception) {}
            recorder = null
            isRecording = false
            currentFile = null
        }
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try { recorder?.pause(); isPaused = true } catch (_: Exception) {}
        }
    }
    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try { recorder?.resume(); isPaused = false } catch (_: Exception) {}
        }
    }
    fun stopRecording() {
        try { recorder?.stop() } catch (_: Exception) {}
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null
        isRecording = false
        isPaused = false
        lastSavedFile = currentFile
        currentFile = null
        refreshList()
        onToast("Grabación guardada")
        // Renombrar inmediatamente si así se desea
        if (promptRenameAfterSave && lastSavedFile != null) {
            renameTarget = lastSavedFile
            renameText = lastSavedFile!!.name.removeSuffix(".m4a")
            showRenameDialog = true
        }
    }

    // ===== Sensor: boca arriba/abajo → pausa/continúa =====
    FaceUpDownListener(
        context = ctx,
        onFaceDown = { if (isRecording && !isPaused) pauseRecording() },
        onFaceUp = { if (isRecording && isPaused) resumeRecording() }
    )

    fun performDelete(file: File) {
        if (playingPath == file.absolutePath) stopAndReleasePlayer()
        try { file.delete() } catch (_: Exception) {}
        refreshList()
        onToast("Grabación eliminada")
    }

    fun performRename(file: File, newBase: String) {
        val base = sanitizeBaseName(newBase).ifBlank { "grabacion" }
        val newFile = uniqueFileIn(recDir, base)
        if (playingPath == file.absolutePath) stopAndReleasePlayer()
        val ok = try { file.renameTo(newFile) } catch (_: Exception) { false }
        if (ok) {
            refreshList()
            onToast("Renombrada a ${newFile.name}")
        } else {
            onToast("No se pudo renombrar")
        }
    }

    // ===== UI =====
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
                        "Grabaciones" + if (!patientName.isNullOrBlank()) " · $patientName" else "",
                        color = Color.White, fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = pink)
            )
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ===== Hero Recorder =====
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        if (isRecording) (if (isPaused) "Grabación en pausa" else "Grabando…")
                        else "Listo para grabar",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Botón circular grande
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (!isRecording) startRecording()
                                else if (!isPaused) pauseRecording()
                                else resumeRecording()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // anillo
                        Canvas(Modifier.fillMaxSize()) { drawCircle(color = pink.copy(alpha = 0.2f), style = Stroke(width = 10f)) }
                        Surface(
                            color = if (isRecording && !isPaused) pink else Color(0xFFECECEC),
                            contentColor = if (isRecording && !isPaused) Color.White else pink,
                            shape = CircleShape,
                            modifier = Modifier.size(120.dp),
                            tonalElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when {
                                    !isRecording -> Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(48.dp))
                                    isPaused -> Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(56.dp))
                                    else -> Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(56.dp))
                                }
                            }
                        }
                    }

                    // Onda animada
                    WaveformBar(running = isRecording && !isPaused, width = 220.dp, height = 36.dp, color = pink)

                    // Timer y botón detener
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(
                            if (isRecording) formatHMS(elapsedMs) else "00:00",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isRecording) pink else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (isRecording) {
                            Button(onClick = { stopRecording() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))) {
                                Icon(Icons.Filled.Stop, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Detener")
                            }
                        }
                    }
                }
            }

            // ===== Lista de grabaciones =====
            Text("Grabaciones anteriores", style = MaterialTheme.typography.titleMedium)
            if (recordings.isEmpty()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No hay grabaciones") }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(recordings, key = { it.absolutePath }) { file ->
                        val duration = remember(file.absolutePath) { fileDurationMs(file) }
                        val isThisPlaying = playingPath == file.absolutePath
                        ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Play / Pause
                                    FilledIconButton(
                                        onClick = {
                                            if (!isThisPlaying) {
                                                stopAndReleasePlayer()
                                                val p = MediaPlayer()
                                                try {
                                                    p.setDataSource(file.absolutePath)
                                                    p.prepare(); p.start()
                                                    player = p
                                                    playingPath = file.absolutePath
                                                    playDurMs = p.duration
                                                    playPosMs = 0
                                                    p.setOnCompletionListener {
                                                        stopAndReleasePlayer()
                                                    }
                                                } catch (_: Exception) {
                                                    onToast("No se pudo reproducir")
                                                    try { p.release() } catch (_: Exception) {}
                                                }
                                            } else {
                                                if (player?.isPlaying == true) player?.pause() else player?.start()
                                            }
                                        },
                                        shape = CircleShape,
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = if (isThisPlaying && player?.isPlaying == true) pink else Color(0xFFEDEDED),
                                            contentColor = if (isThisPlaying && player?.isPlaying == true) Color.White else pink
                                        )
                                    ) {
                                        if (isThisPlaying && player?.isPlaying == true)
                                            Icon(Icons.Filled.Pause, contentDescription = null)
                                        else
                                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(Modifier.weight(1f)) {
                                        Text(file.name.removeSuffix(".m4a"), fontWeight = FontWeight.SemiBold)
                                        val dateText = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                            .format(Date(file.lastModified()))
                                        Text(
                                            "$dateText · ${formatHMS(duration)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Renombrar
                                    IconButton(onClick = {
                                        renameTarget = file
                                        renameText = file.name.removeSuffix(".m4a")
                                        showRenameDialog = true
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Renombrar", tint = pink)
                                    }
                                    // Eliminar
                                    IconButton(onClick = { deleteTarget = file; showDeleteConfirm = true }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color(0xFFD32F2F))
                                    }
                                }

                                // Progreso cuando es el que suena
                                if (isThisPlaying) {
                                    val pos = playPosMs.toFloat().coerceIn(0f, playDurMs.toFloat())
                                    Slider(
                                        value = if (playDurMs == 0) 0f else pos / playDurMs,
                                        onValueChange = { ratio ->
                                            val seekTo = (ratio * playDurMs).toInt()
                                            player?.seekTo(seekTo)
                                            playPosMs = seekTo
                                        }
                                    )
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(formatHMS(playPosMs.toLong()), style = MaterialTheme.typography.bodySmall)
                                        Text(formatHMS(playDurMs.toLong()), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogo Eliminar
    if (showDeleteConfirm && deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; deleteTarget = null },
            title = { Text("Eliminar grabación") },
            text = { Text("¿Seguro que deseas eliminar \"${deleteTarget!!.name}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        performDelete(deleteTarget!!)
                        showDeleteConfirm = false
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false; deleteTarget = null }) { Text("Cancelar") }
            }
        )
    }

    // Dialogo Renombrar
    if (showRenameDialog && renameTarget != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false; renameTarget = null },
            title = { Text("Renombrar grabación") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it.take(80) },
                    label = { Text("Nuevo nombre") },
                    singleLine = true,
                    supportingText = { Text(".m4a se agregará automáticamente") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        performRename(renameTarget!!, renameText)
                        showRenameDialog = false
                        renameTarget = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = pink)
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false; renameTarget = null }) { Text("Cancelar") } }
        )
    }

    // Liberar player al salir
    DisposableEffect(Unit) {
        onDispose { stopAndReleasePlayer() }
    }
}

/* -------------------------- UI helpers -------------------------- */

@Composable
private fun WaveformBar(
    running: Boolean,
    width: Dp,
    height: Dp,
    color: Color
) {
    val bars = 24
    val baseH = height.value
    val infinite = rememberInfiniteTransition(label = "wave")
    val anim = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "waveAnim"
    )
    Canvas(modifier = Modifier.size(width, height)) {
        val barWidth = size.width / (bars * 1.5f)
        val gap = barWidth / 2f
        for (i in 0 until bars) {
            val phase = (i.toFloat() / bars + anim.value) % 1f
            val amp = if (running) (0.3f + 0.7f * kotlin.math.abs(kotlin.math.sin(phase * Math.PI).toFloat())) else 0.2f
            val h = baseH * amp
            val x = i * (barWidth + gap)
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(x, (size.height - h) / 2f),
                end = androidx.compose.ui.geometry.Offset(x, (size.height + h) / 2f),
                strokeWidth = barWidth
            )
        }
    }
}
