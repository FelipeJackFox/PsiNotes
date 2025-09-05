package com.example.psinotes

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.psinotes.ui.HomeScreen
import com.example.psinotes.ui.LoginScreen
import com.example.psinotes.ui.NotesScreen
import com.example.psinotes.ui.PatientDetailScreen
import com.example.psinotes.ui.theme.RecordingsScreen
import com.example.psinotes.ui.theme.ImagesScreen

// imports NUEVOS:
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.psinotes.ui.theme.PsiNotesTheme            // ya existente:contentReference[oaicite:1]{index=1}
import com.example.psinotes.ui.theme.sensors.rememberAmbientDarkMode
import com.example.psinotes.data.ThemeMode
import com.example.psinotes.data.ThemePrefs
import com.example.psinotes.ui.theme.SettingsScreen


// imports nuevos
import com.example.psinotes.ui.theme.PsiNotesTheme
import com.example.psinotes.ui.theme.sensors.rememberAmbientDarkMode

import com.example.psinotes.ui.theme.sensors.AmbientDarkDefaults

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 1) Preferencia persistida
            val ctx = LocalContext.current
            val prefMode by ThemePrefs.modeFlow(ctx).collectAsState(initial = ThemeMode.AUTO)

            // 2) Sensor de luz (solo si AUTO)
            val autoDark = rememberAmbientDarkMode(
                enterDarkLux = AmbientDarkDefaults.ENTER_DARK_LUX,
                exitDarkLux  = AmbientDarkDefaults.EXIT_DARK_LUX,
                emaAlpha     = AmbientDarkDefaults.EMA_ALPHA,
                fallbackDark = false
            )

            val dark = when (prefMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK  -> true
                ThemeMode.AUTO  -> autoDark.value
            }

            PsiNotesTheme(darkTheme = dark, dynamicColor = true) {      // usa tu Theme existente:contentReference[oaicite:2]{index=2}
                AppNav()
            }
        }
    }
}

@Composable
private fun AppNav() {
    val navController = rememberNavController()
    val ctx = LocalContext.current

    NavHost(navController = navController, startDestination = "login") {

        // LOGIN
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    Toast.makeText(ctx, "Ingreso exitoso", Toast.LENGTH_SHORT).show()
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // HOME
        composable("home") {
            HomeScreen(
                onToast = { msg -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() },
                onOpenPatient = { id -> navController.navigate("detail/$id") },
                onOpenSettings = { navController.navigate("settings") }   // <-- AQUI
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // DETAIL
        composable(
            route = "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L

            PatientDetailScreen(
                patientId = id,
                onBack = { navController.popBackStack() },
                onDeleted = {
                    Toast.makeText(ctx, "Paciente eliminado", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                onToast = { msg -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() },

                // Navegación por ROUTE COMPLETA (con ?newNote=0/1)
                onOpenNotesRoute = { route ->
                    navController.navigate(route)
                }
            )
        }

        // NOTES (con query param newNote)
        composable(
            route = "notes/{patientId}/{patientName}?newNote={newNote}",
            arguments = listOf(
                navArgument("patientId") { type = NavType.LongType },
                navArgument("patientName") { type = NavType.StringType; defaultValue = "" },
                navArgument("newNote") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getLong("patientId") ?: 0L
            val patientName = backStackEntry.arguments?.getString("patientName")
            val startWithNewNote = backStackEntry.arguments?.getInt("newNote") == 1

            NotesScreen(
                patientId = patientId,
                patientName = patientName,
                startWithNewNote = startWithNewNote,
                onBack = { navController.popBackStack() },
                onToast = { msg -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() }
            )
        }

        composable(
            route = "recordings/{patientId}/{patientName}",
            arguments = listOf(
                navArgument("patientId") { type = NavType.LongType },
                navArgument("patientName") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getLong("patientId") ?: 0L
            val patientName = backStackEntry.arguments?.getString("patientName")
            RecordingsScreen(
                patientId = patientId,
                patientName = patientName,
                onBack = { navController.popBackStack() },
                onToast = { msg -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() } // <-- .show()
            )
        }

        composable(
            route = "images/{patientId}/{patientName}",
            arguments = listOf(
                navArgument("patientId") { type = NavType.LongType },
                navArgument("patientName") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getLong("patientId") ?: 0L
            val patientName = backStackEntry.arguments?.getString("patientName")
            ImagesScreen(
                patientId = patientId,
                patientName = patientName,
                onBack = { navController.popBackStack() },
                onToast = { msg -> android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show() }
            )
        }
    }
}
