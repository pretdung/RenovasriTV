package com.example.renovasritv

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CircularProgressIndicator
import androidx.tv.material3.*
import coil.compose.AsyncImage

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AiResultScreen(
    navController: androidx.navigation.NavController,
    viewModel: MainViewModel,
    calcViewModel: CalculatorViewModel
) {
    val aiImageUrl by viewModel.aiVisualizationUrl.collectAsState()
    val aiPrompt by viewModel.aiImagePrompt.collectAsState()
    val uiConfigs by viewModel.uiConfigs.collectAsState()
    
    // We can also get the latest estimation details if needed
    val surfaces by calcViewModel.surfaces.collectAsState()
    val systemConfigs by viewModel.calcSystemConfigs.collectAsState()
    val laborConfigs by viewModel.laborConfigs.collectAsState()
    
    val totals = remember(surfaces, systemConfigs, laborConfigs) {
        calcViewModel.calculateTotalEstimationDetailed(systemConfigs, laborConfigs)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Full screen image background if available
        if (aiImageUrl != null) {
            AsyncImage(
                model = aiImageUrl,
                contentDescription = "AI Visualization",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.4f
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Left Side: AI Image and Prompt Info
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .padding(48.dp)
            ) {
                Text(
                    "HASIL VISUALISASI AI",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = 14.sp
                )
                Text(
                    "The Architect & The Artist",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.DarkGray)
                ) {
                    if (aiImageUrl != null) {
                        AsyncImage(
                            model = aiImageUrl,
                            contentDescription = "AI Result",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Sedang memproses gambar...", color = Color.Gray)
                        }
                    }
                    
                    // Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("4K AI GENERATED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Prompt description
                Surface(
                    onClick = {},
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
                    colors = ClickableSurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("AI ARCHITECT'S PROMPT", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(
                            aiPrompt ?: "Menunggu prompt dari Claude...",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            maxLines = 3
                        )
                    }
                }
            }

            // Right Side: Budget Summary & Actions
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(48.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("RINGKASAN ESTIMASI", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Gray, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(16.dp))

                // Price Card
                Surface(
                    onClick = {},
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
                    colors = ClickableSurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("TOTAL ANGGARAN", color = Color.Black.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            "${CalculationEngine.formatCurrency(totals["grandTotalMin"] ?: 0.0)} - ${CalculationEngine.formatCurrency(totals["grandTotalMax"] ?: 0.0)}",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Breakdown Items
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    BreakdownItem("Material", totals["materialMin"] ?: 0.0, totals["materialMax"] ?: 0.0)
                    BreakdownItem("Jasa & Tenaga", totals["jasaSubtotal"] ?: 0.0)
                    BreakdownItem("Dana Darurat", totals["contingencyTotal"] ?: 0.0)
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(48.dp))

                // Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { /* Download logic */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.colors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Download, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("UNDUH LAPORAN PDF")
                        }
                    }

                    Button(
                        onClick = { /* Share logic */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.colors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("BAGIKAN HASIL")
                        }
                    }

                    Button(
                        onClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.colors(containerColor = Color.Transparent, contentColor = Color.Gray)
                    ) {
                        Text("KEMBALI KE HOME")
                    }
                }
            }
        }
    }
}

@Composable
fun BreakdownItem(label: String, min: Double, max: Double? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        val priceText = if (max != null && max > min) {
            "${CalculationEngine.formatCurrencyShort(min)} - ${CalculationEngine.formatCurrencyShort(max)}"
        } else {
            CalculationEngine.formatCurrencyShort(min)
        }
        Text(priceText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
