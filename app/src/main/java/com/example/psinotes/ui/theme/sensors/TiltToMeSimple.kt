package com.example.psinotes.ui.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import java.lang.Math.toDegrees
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Detector mínimo de gesto "inclinar hacia ti" usando ROTATION_VECTOR.
 *
 * Preset por defecto: MÁS ESTRICTO (menos sensible).
 * Para hacerlo más sensible, sube triggerPitchDeg (p.ej., -35f o -25f),
 * o baja minRiseRateDegPerSec y minDeltaDeg.
 */
@Composable
fun TiltToMeSimple(
    context: Context,
    onTrigger: () -> Unit,
    // === PRESET BALANCEADO (los que tenías cuando funcionaba) ===
    triggerPitchDeg: Float = -40f,      // umbral hacia ti
    minDeltaDeg: Float = 20f,           // cambio mínimo desde el “armado”
    armWindowMs: Long = 800L,           // ventana armar→disparar
    minRiseRateDegPerSec: Float = 120f, // rapidez mínima
    holdMs: Long = 60L,                 // sostener un instante
    debounceMs: Long = 1500L,           // espera entre disparos
    neutralMaxDeg: Float = -10f,        // considerar “neutro”
    invert: Boolean = false
) {
    val sm = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val rot = remember { sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }

    var lastPitch: Float? = null
    var lastTs: Long? = null
    var armedAt: Long? = null
    var pitchAtArm: Float? = null
    var holdStart: Long? = null
    var lastFireTs = 0L

    var lpPitch = 0f
    val alpha = 0.2f

    val R = FloatArray(9)
    val orientation = FloatArray(3)

    DisposableEffect(rot) {
        if (rot == null) return@DisposableEffect onDispose {}

        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(R, e.values)
                SensorManager.getOrientation(R, orientation)
                var pitch = toDegrees(orientation[1].toDouble()).toFloat()
                if (invert) pitch = -pitch

                lpPitch = alpha * pitch + (1 - alpha) * lpPitch

                val now = System.currentTimeMillis()
                val prevPitch = lastPitch
                val prevTs = lastTs
                lastPitch = lpPitch
                lastTs = now
                if (prevPitch == null || prevTs == null) return

                val dt = max(1L, now - prevTs) / 1000f
                val dPitchDt = (lpPitch - prevPitch) / dt

                if (now - lastFireTs < debounceMs) return

                // 1) Armar: solo si estábamos casi neutros (no ya hacia ti)
                if (lpPitch > neutralMaxDeg) {
                    armedAt = now
                    pitchAtArm = lpPitch
                    holdStart = null
                    return
                }

                // 2) Disparar dentro de ventana y cumpliendo umbrales
                val t0 = armedAt
                val pa = pitchAtArm
                if (t0 != null && pa != null) {
                    val within = (now - t0) <= armWindowMs
                    val delta = (lpPitch - pa) // negativo al ir hacia -∞ (hacia ti)
                    val bigEnough = abs(delta) >= minDeltaDeg
                    val fastEnough = abs(dPitchDt) >= minRiseRateDegPerSec
                    val crossed = lpPitch <= triggerPitchDeg

                    if (within && crossed && bigEnough && fastEnough) {
                        if (holdStart == null) holdStart = now
                        val holding = now - (holdStart ?: now)
                        if (holding >= holdMs) {
                            lastFireTs = now
                            armedAt = null
                            pitchAtArm = null
                            holdStart = null
                            onTrigger()
                        }
                    } else {
                        holdStart = null
                        if (!within) {
                            armedAt = null
                            pitchAtArm = null
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sm.registerListener(listener, rot, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }
}
