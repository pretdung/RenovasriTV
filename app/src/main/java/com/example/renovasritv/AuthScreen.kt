package com.example.renovasritv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.tv.material3.*

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        when (val state = authState) {
            is AuthState.DeviceFlowStarted -> {
                DeviceFlowUI(state.userCode, state.verificationUri)
            }
            is AuthState.Loading -> {
                CircularProgressIndicator()
            }
            is AuthState.Authenticated -> {
                Box(contentAlignment = Alignment.Center) {
                    Text("Successfully Authenticated!", color = Color.Green, fontSize = 24.sp)
                    LaunchedEffect(Unit) {
                        delay(2000)
                        onAuthSuccess()
                    }
                }
            }
            is AuthState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { authViewModel.startGoogleDeviceFlow() }) {
                        Text("Retry")
                    }
                }
            }
            else -> {
                LoginPrompt { authViewModel.startGoogleDeviceFlow() }
            }
        }
    }
}

@Composable
fun LoginPrompt(onLoginClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Welcome to Renovasri AI",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Sign in to sync your AI-generated designs across devices.",
            fontSize = 18.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onLoginClick) {
            Text("Login with Google")
        }
    }
}

@Composable
fun DeviceFlowUI(userCode: String, verificationUri: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .padding(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Sign In via Phone/PC",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "1. Scan the QR code or visit\n$verificationUri",
                fontSize = 18.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "2. Enter this code on your device:",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    userCode,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    letterSpacing = 4.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(48.dp))

        // Placeholder for QR Code
        Box(
            modifier = Modifier
                .size(240.dp)
                .background(Color.White, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = "QR Code Placeholder",
                modifier = Modifier.fillMaxSize(0.8f),
                tint = Color.Black
            )
        }
    }
}
