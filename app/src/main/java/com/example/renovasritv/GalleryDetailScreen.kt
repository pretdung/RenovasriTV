package com.example.renovasritv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.*
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun GalleryDetailScreen(imageUrl: String, navController: NavController) {
    var isUiVisible by remember { mutableStateOf(true) }
    var showPopup by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableStateOf(1.2f) }
    var panOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    
    val focusRequester = remember { FocusRequester() }

    // Auto-hide UI after inactivity
    LaunchedEffect(isUiVisible, showPopup) {
        if (isUiVisible && !showPopup) {
            delay(8000)
            isUiVisible = false
        }
    }

    // Request focus for key events
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Animation for Ken Burns effect
    val infiniteTransition = rememberInfiniteTransition(label = "KenBurns")
    val animatedScale by infiniteTransition.animateFloat(
        initialValue = zoomLevel,
        targetValue = zoomLevel + 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent {
                if (it.key == Key.DirectionCenter || it.key == Key.Enter) {
                    if (it.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP) {
                        showPopup = true
                        isUiVisible = true
                    }
                    true
                } else {
                    false
                }
            }
    ) {
        // Fullscreen Image
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .scale(animatedScale)
                .offset(x = panOffset.x.dp, y = panOffset.y.dp),
            contentScale = ContentScale.Crop
        )

        // Project Info Overlay (Bottom)
        AnimatedVisibility(
            visible = isUiVisible && !showPopup,
            enter = fadeIn(tween(800)),
            exit = fadeOut(tween(500))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 600f
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(48.dp)
                ) {
                    Text(
                        text = "ARCHITECTURAL SNAPSHOT",
                        color = AppColors.Primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )
                    Text(
                        text = "Minimalist Sanctuary",
                        color = Color.White,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // POPUP BOX
        if (showPopup) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    colors = NonInteractiveSurfaceDefaults.colors(
                        containerColor = Color(0xFF1E1E1E),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .width(420.dp)
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "VIEW OPTIONS",
                            fontSize = 24.sp,
                            color = AppColors.Primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        PopupOption(
                            icon = Icons.Default.ZoomIn,
                            label = "Zoom In & Pan",
                            onClick = {
                                zoomLevel += 0.3f
                                panOffset = panOffset.copy(x = panOffset.x - 40f, y = panOffset.y - 20f)
                            }
                        )

                        PopupOption(
                            icon = Icons.Default.ZoomOut,
                            label = "Reset View",
                            onClick = {
                                zoomLevel = 1.2f
                                panOffset = androidx.compose.ui.geometry.Offset.Zero
                            }
                        )

                        PopupOption(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            label = "Back to Gallery",
                            onClick = {
                                navController.popBackStack()
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { showPopup = false },
                            colors = ButtonDefaults.colors(
                                containerColor = Color(0x22FFFFFF),
                                contentColor = Color.LightGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dismiss", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PopupOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0x11FFFFFF),
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        modifier = Modifier.fillMaxWidth().height(64.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}
