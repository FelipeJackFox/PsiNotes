@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.psinotes.ui.theme

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.example.psinotes.data.ThemeMode
import com.example.psinotes.data.ThemePrefs

import com.example.psinotes.ui.theme.sensors.AmbientDarkDefaults
import com.example.psinotes.ui.theme.sensors.rememberAmbientLightDebug

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val pink = Color(0xFFFF2D6C)
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estado actual desde DataStore
    val current by ThemePrefs.modeFlow(ctx).collectAsState(initial = ThemeMode.AUTO)
    var selected by remember(current) { mutableStateOf(current) }

    // Lectura en vivo del sensor de luz (para calibrar el umbral)
    val lightSample by rememberAmbientLightDebug()   // devuelve State<AmbientLightSample?>

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                title = { Text("Configuraciones", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = pink)
            )
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Apariencia", style = MaterialTheme.typography.titleMedium)

            // Radios: Automático / Claro / Oscuro
            ThemeOptionRow(
                title = "Automático (sensor de luz)",
                selected = selected == ThemeMode.AUTO
            ) { selected = ThemeMode.AUTO }

            ThemeOptionRow(
                title = "Claro",
                selected = selected == ThemeMode.LIGHT
            ) { selected = ThemeMode.LIGHT }

            ThemeOptionRow(
                title = "Oscuro",
                selected = selected == ThemeMode.DARK
            ) { selected = ThemeMode.DARK }

            // ----- Debug del sensor de luz -----
            val sample = lightSample
            val enterLux = AmbientDarkDefaults.ENTER_DARK_LUX
            val exitLux  = AmbientDarkDefaults.EXIT_DARK_LUX

            Spacer(Modifier.height(8.dp))
            Text("Sensor de luz (lectura en vivo)", style = MaterialTheme.typography.titleMedium)

            Text(
                text = buildString {
                    if (sample == null) {
                        append("No hay sensor de luz disponible en este dispositivo.")
                    } else {
                        val s = sample!!
                        appendLine("LUX: ${"%.1f".format(s.lux)}   (EMA: ${if (s.emaLux == null) "—" else "%.1f".format(s.emaLux)} lx)")
                        appendLine("Umbrales AUTO (LUX): entrar < ${"%.0f".format(enterLux)} · salir > ${"%.0f".format(exitLux)}")
                        appendLine("Normalizado (lineal): ${"%.1f".format(s.normalizedLin * 100)} %   · máx≈ ${"%.0f".format(s.maxRange)} lx")
                        appendLine("Normalizado (log):    ${"%.1f".format(s.normalizedLog * 100)} %")
                        append("Sugerencia: si EMA(lux) en tu ambiente iluminado es > ${"%.0f".format(exitLux)}, nunca activarás oscuro ahí. En penumbra real baja de ~${"%.0f".format(enterLux)} lx.")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // -----------------------------------

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { scope.launch { ThemePrefs.setMode(ctx, selected) } },
                colors = ButtonDefaults.buttonColors(containerColor = pink),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar", color = Color.White, fontWeight = FontWeight.ExtraBold) }
        }
    }
}

@Composable
private fun ThemeOptionRow(title: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, modifier = Modifier.weight(1f))
        RadioButton(selected = selected, onClick = onSelect)
    }
}
