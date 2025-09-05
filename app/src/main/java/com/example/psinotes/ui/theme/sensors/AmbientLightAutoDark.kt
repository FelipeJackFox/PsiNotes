package com.example.psinotes.ui.theme.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/** Umbrales por LUX (no por %). Ajusta a gusto. */
object AmbientDarkDefaults {
    const val ENTER_DARK_LUX = 45f    // Antes: 30f  -> entra a oscuro si EMA(lux) < 45
    const val EXIT_DARK_LUX  = 150f   // Puedes subir/bajar si quieres más/menos histéresis
    const val EMA_ALPHA      = 0.20f
}

/** Lectura en vivo (debug) */
@Stable
data class AmbientLightSample(
    val lux: Float,
    val emaLux: Float?,          // EMA en LUX
    val maxRange: Float,
    val normalizedLin: Float,    // lux/maxRange [0..1]
    val normalizedLog: Float     // log-normalizado [0..1] (más “humano”)
)

@Composable
fun rememberAmbientLightDebug(
    emaAlpha: Float = AmbientDarkDefaults.EMA_ALPHA
): State<AmbientLightSample?> {
    val ctx = LocalContext.current
    val sm = remember { ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val light = remember(sm) { sm.getDefaultSensor(Sensor.TYPE_LIGHT) }
    val state = remember { mutableStateOf<AmbientLightSample?>(null) }
    var ema by remember { mutableStateOf<Float?>(null) }

    fun logNorm(lx: Float, ref: Float = 10_000f): Float {
        // comprime la escala para parecerse a la percepción humana
        val num = ln(lx + 1f)
        val den = ln(ref + 1f)
        return (num / max(den, 1e-6f)).coerceIn(0f, 1f)
    }

    DisposableEffect(light) {
        if (light == null) {
            state.value = null
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(e: android.hardware.SensorEvent) {
                    val lux = e.values.firstOrNull() ?: return
                    ema = when (val last = ema) {
                        null -> lux
                        else -> (emaAlpha * lux) + (1f - emaAlpha) * last
                    }
                    val maxR = max(1f, light.maximumRange)
                    val lin = min(1f, lux / maxR)
                    val log = logNorm(lux)

                    state.value = AmbientLightSample(
                        lux = lux,
                        emaLux = ema,
                        maxRange = maxR,
                        normalizedLin = lin,
                        normalizedLog = log
                    )
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sm.registerListener(listener, light, SensorManager.SENSOR_DELAY_UI)
            onDispose { sm.unregisterListener(listener) }
        }
    }
    return state
}

/** Decide modo oscuro por LUX con histéresis (EMA). */
@Composable
fun rememberAmbientDarkMode(
    enterDarkLux: Float = AmbientDarkDefaults.ENTER_DARK_LUX,
    exitDarkLux: Float = AmbientDarkDefaults.EXIT_DARK_LUX,
    emaAlpha: Float = AmbientDarkDefaults.EMA_ALPHA,
    fallbackDark: Boolean = false
): State<Boolean> {
    val ctx = LocalContext.current
    val sm = remember { ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val light = remember(sm) { sm.getDefaultSensor(Sensor.TYPE_LIGHT) }

    val isDark = remember { mutableStateOf(fallbackDark) }
    var emaLux by remember { mutableStateOf<Float?>(null) }

    DisposableEffect(light) {
        if (light == null) {
            isDark.value = fallbackDark
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(e: android.hardware.SensorEvent) {
                    val lux = e.values.firstOrNull() ?: return
                    emaLux = when (val last = emaLux) {
                        null -> lux
                        else -> (emaAlpha * lux) + (1f - emaAlpha) * last
                    }
                    val v = emaLux ?: lux
                    val cur = isDark.value
                    val next = when {
                        cur && v > exitDarkLux -> false  // salir de oscuro
                        !cur && v < enterDarkLux -> true // entrar a oscuro
                        else -> cur
                    }
                    if (next != cur) isDark.value = next
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sm.registerListener(listener, light, SensorManager.SENSOR_DELAY_UI)
            onDispose { sm.unregisterListener(listener) }
        }
    }
    return isDark
}
