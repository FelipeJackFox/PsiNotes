package com.example.psinotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val pink = Color(0xFFFF2D6C)
    val bg = MaterialTheme.colorScheme.surface

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(pink),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .offset(y = 48.dp)
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Logo",
                    tint = pink,
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        Spacer(Modifier.height(64.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Bienvenido",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF404040),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuario") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                isError = showErrors && password.isEmpty()
            )

            if (showErrors && (username.isEmpty() || password.isEmpty())) {
                Spacer(Modifier.height(8.dp))
                Text("Ingresa usuario y contraseña",
                    color = Color(0xFFD32F2F),
                    style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    showErrors = true
                    if (username == "psico" && password == "1234") onLoginSuccess()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = pink),
                shape = RoundedCornerShape(18.dp)
            ) { Text("Comenzar", fontWeight = FontWeight.ExtraBold, color = Color.White) }
        }
    }
}
