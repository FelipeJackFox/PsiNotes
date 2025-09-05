package com.example.psinotes.ui.theme.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

/**
 * Usa TYPE_GRAVITY para detectar:
 *  - Boca arriba: z >= +7  → onFaceUp()
 *  - Boca abajo: z <= -7   → onFaceDown()
 */
@Composable
fun FaceUpDownListener(
    context: Context,
    onFaceUp: () -> Unit,
    onFaceDown: () -> Unit,
    threshold: Float = 7f,
    debounceMs: Long = 700L
) {
    val sm = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val grav = remember { sm.getDefaultSensor(Sensor.TYPE_GRAVITY) }

    var lastEventTs = 0L
    var lastState: Int? = null // 1=up, -1=down

    DisposableEffect(grav) {
        if (grav == null) return@DisposableEffect onDispose { }

        val lis = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val now = System.currentTimeMillis()
                if (now - lastEventTs < debounceMs) return
                val z = e.values[2]

                when {
                    z >= threshold && lastState != 1 -> {
                        lastState = 1
                        lastEventTs = now
                        onFaceUp()
                    }
                    z <= -threshold && lastState != -1 -> {
                        lastState = -1
                        lastEventTs = now
                        onFaceDown()
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sm.registerListener(lis, grav, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(lis) }
    }
}
