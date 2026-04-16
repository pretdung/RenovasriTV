package com.example.renovasritv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: androidx.navigation.NavController,
    viewModel: MainViewModel,
    calcViewModel: CalculatorViewModel = viewModel()
) {
    val estimations by viewModel.estimations.collectAsState()
    val uiConfigs by viewModel.uiConfigs.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchEstimations()
    }

    val headerMainConfig = uiConfigs["history_header_main"]
    val headerSubConfig = uiConfigs["history_header_sub"]
    val headerIconConfig = uiConfigs["history_header_icon"]
    val emptyIconConfig = uiConfigs["history_empty_icon"]
    val emptyTextConfig = uiConfigs["history_empty_text"]

    val headerMain = headerMainConfig?.value ?: "RIWAYAT ESTIMASI"
    val headerSub = headerSubConfig?.value ?: "Daftar perhitungan renovasi yang telah Anda simpan"
    val headerIcon = getIconByName(headerIconConfig?.value) ?: Icons.Default.History

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    headerIcon,
                    contentDescription = null,
                    tint = headerIconConfig?.fontColor?.toComposeColor() ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(headerIconConfig?.fontSize.toIconSize())
                )
                Spacer(modifier = Modifier.width(20.dp))
                SectionHeader(
                    mainTitle = headerMain,
                    subTitle = headerSub,
                    mainConfig = headerMainConfig,
                    subConfig = headerSubConfig,
                    screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (estimations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            getIconByName(emptyIconConfig?.value) ?: Icons.Default.Calculate,
                            contentDescription = null,
                            modifier = Modifier
                                .size(emptyIconConfig?.fontSize.toIconSize())
                                .alpha(0.3f),
                            tint = emptyIconConfig?.fontColor?.toComposeColor() ?: Color.White
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            emptyTextConfig?.value ?: "Belum ada riwayat estimasi.",
                            color = emptyTextConfig?.fontColor?.toComposeColor() ?: Color.Gray,
                            fontSize = emptyTextConfig?.fontSize.toFontSize(androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(estimations) { estimation ->
                        EstimationCard(estimation) {
                            calcViewModel.resumeEstimation(estimation)
                            navController.navigate(Screen.Calculator.route) {
                                popUpTo(Screen.Home.route)
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EstimationCard(estimation: CalcEstimation, onClick: () -> Unit) {
    val formattedDate = remember(estimation.createdAt) {
        try {
            // Check if it's already an ISO-8601 string from Supabase
            // format: 2024-05-20T10:00:00.000Z
            val cleanDate = estimation.createdAt?.replace("Z", "")?.split(".")?.get(0) ?: ""
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val date = inputFormat.parse(cleanDate)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            println("DEBUG_LOG: Date parsing error for ${estimation.createdAt}: ${e.message}")
            estimation.createdAt ?: "Unknown Date"
        }
    }

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.1f),
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        formattedDate.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "ESTIMASI TOTAL",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Gray
                    )
                }
                Icon(
                    Icons.Default.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).alpha(0.5f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "${CalculationEngine.formatCurrency(estimation.totalMin)} - ${CalculationEngine.formatCurrency(estimation.totalMax)}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Info (e.g. Dimensions)
            Box(
                modifier = Modifier
                    .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "Status: ${estimation.status.uppercase()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }
    }
}
