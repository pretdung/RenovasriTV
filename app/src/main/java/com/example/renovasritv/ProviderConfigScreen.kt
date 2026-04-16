package com.example.renovasritv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*

@Composable
fun ProviderConfigScreen(viewModel: MainViewModel) {
    var pin by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("Claude") }
    var apiKey by remember { mutableStateOf("") }
    val saveStatus by viewModel.saveStatus.collectAsState()
    val configs by viewModel.providerConfigs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("AI Provider Settings (BYOK)", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text("Your keys are encrypted locally using your Security PIN.", color = Color.Gray, fontSize = 16.sp)
        
        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            // Configuration Form
            Column(modifier = Modifier.weight(1f)) {
                Text("1. Enter Security PIN", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                // In a real TV app, we'd use a custom PIN entry, but for now we'll use a Button to set it
                // and a state to hold it. 
                Text("This PIN is used to derive your 256-bit AES key.", color = Color.LightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Simulated PIN Entry
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (pin.length > i) Text("●", color = Color.White)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("2. Provider", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("Claude", "Gemini").forEach { p ->
                        FilterChip(
                            selected = provider == p,
                            onClick = { provider = p }
                        ) { Text(p) }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text("3. API Key", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                // Real implementation would use a TextField with VisualTransformation.Password
                Text(if (apiKey.isEmpty()) "Not Set" else "********", color = Color.White)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(onClick = { 
                    viewModel.setSecurityPin("1234") // Simulated PIN entry
                    viewModel.saveProviderConfig(provider, "sk-test-key-12345") // Simulated key
                }) {
                    Text("Secure & Save Key")
                }
                
                if (saveStatus is SaveStatus.Success) {
                    Text((saveStatus as SaveStatus.Success).message, color = Color.Green, modifier = Modifier.padding(top = 8.dp))
                }
            }

            // Status List
            Column(modifier = Modifier.weight(1f)) {
                Text("Stored Configurations", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                configs.forEach { config ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(config.providerName, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Encrypted", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
