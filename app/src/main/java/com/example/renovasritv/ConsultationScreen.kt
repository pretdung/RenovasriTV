package com.example.renovasritv

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.*
import coil.compose.AsyncImage

@Composable
fun ConsultationScreen(navController: NavController, viewModel: MainViewModel) {
    val uiConfigs by viewModel.uiConfigs.collectAsState()
    val screenWidth = LocalConfiguration.current.screenWidthDp

    // CMS Configs with Fallbacks
    val titleConfig = uiConfigs["consultation_title"]
    val descConfig = uiConfigs["consultation_description"]
    val qrConfig = uiConfigs["consultation_qr_image"]
    val contactConfig = uiConfigs["consultation_contact_info"]
    val bannerConfig = uiConfigs["consultation_banner_image"]

    val title = titleConfig?.value ?: "Wujudkan Hunian Impian Anda"
    val description = descConfig?.value ?: "Konsultasikan kebutuhan desain dan renovasi Anda langsung dengan tim ahli kami melalui WhatsApp."
    val qrUrl = qrConfig?.value ?: "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=https://wa.me/6281234567890"
    val contactInfo = contactConfig?.value ?: "+62 812-3456-7890"

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Banner (Optional CMS Driven)
        if (bannerConfig?.isActive != false && bannerConfig?.value != null) {
            AsyncImage(
                model = bannerConfig.value,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.1f
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 80.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Column 1: Text Information
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = title,
                    color = titleConfig?.fontColor?.toComposeColor() ?: MaterialTheme.colorScheme.primary,
                    fontSize = titleConfig?.fontSize.toFontSize(screenWidth),
                    fontWeight = titleConfig?.fontWeight.toFontWeight(),
                    lineHeight = 1.2.sp * titleConfig?.fontSize.toFontSize(screenWidth).value
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = description,
                    color = descConfig?.fontColor?.toComposeColor() ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = descConfig?.fontSize.toFontSize(screenWidth),
                    fontWeight = descConfig?.fontWeight.toFontWeight(),
                    lineHeight = 1.5.sp * descConfig?.fontSize.toFontSize(screenWidth).value
                )

                Spacer(modifier = Modifier.height(48.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = contactInfo,
                        color = contactConfig?.fontColor?.toComposeColor() ?: Color.White,
                        fontSize = contactConfig?.fontSize.toFontSize(screenWidth),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(100.dp))

            // Column 2: QR Code Area
            Column(
                modifier = Modifier.weight(0.8f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(24.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.White,
                        focusedContainerColor = Color.White,
                        contentColor = Color.Black,
                        focusedContentColor = Color.Black
                    ),
                    onClick = {},
                    modifier = Modifier.size(320.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp)) {
                        AsyncImage(
                            model = qrUrl,
                            contentDescription = "Scan to Chat",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "SCAN UNTUK KONSULTASI",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    fontSize = 16.sp
                )
                Text(
                    text = "Gunakan kamera ponsel Anda",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}
