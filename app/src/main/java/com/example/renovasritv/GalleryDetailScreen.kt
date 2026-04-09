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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
fun GalleryDetailScreen(id: Long, navController: NavController, viewModel: MainViewModel) {
    val galleryItems by viewModel.galleryItems.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val item = remember(id, galleryItems) { galleryItems.find { it.id == id } }
    
    val isFavorite = item?.id?.let { favoriteIds.contains(it) } ?: false
    
    var currentImageUrl by remember(item) { mutableStateOf(item?.imageUrl ?: "") }
    var isUiVisible by remember { mutableStateOf(true) }
    var showPopup by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableStateOf(1.2f) }
    var panOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    
    val focusRequester = remember { FocusRequester() }

    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading project details...", color = Color.White)
        }
        return
    }

    val allMedia = remember(item) {
        listOf(item.imageUrl) + item.mediaGallery
    }

    // Auto-hide UI after inactivity
    LaunchedEffect(isUiVisible, showPopup) {
        if (isUiVisible && !showPopup) {
            delay(10000) // Longer delay for browsing gallery
            isUiVisible = false
        }
    }

    // Increment view count and request focus for key events
    LaunchedEffect(id) {
        viewModel.incrementViews(id)
        focusRequester.requestFocus()
    }

    // Animation for Ken Burns effect
    val infiniteTransition = rememberInfiniteTransition(label = "KenBurns")
    val animatedScale by infiniteTransition.animateFloat(
        initialValue = zoomLevel,
        targetValue = zoomLevel + 0.05f, // Subtle movement
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
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
                if (it.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (it.key) {
                        Key.DirectionCenter, Key.Enter -> {
                            showPopup = true
                            isUiVisible = true
                            true
                        }
                        Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight -> {
                            isUiVisible = true
                            false // let focus handle it
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        // Fullscreen Image with Crossfade
        androidx.compose.animation.Crossfade(
            targetState = currentImageUrl,
            animationSpec = tween(1500),
            label = "MainImageTransition"
        ) { targetUrl ->
            AsyncImage(
                model = targetUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(animatedScale)
                    .offset(x = panOffset.x.dp, y = panOffset.y.dp),
                contentScale = ContentScale.Crop
            )
        }

        // Project Info & Gallery Overlay (Bottom)
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
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 400f
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 48.dp, vertical = 40.dp)
                ) {
                    // Media Gallery Filmstrip
                    if (allMedia.size > 1) {
                        Text(
                            text = "MEDIA GALLERY",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 32.dp),
                            modifier = Modifier.focusRequester(remember { FocusRequester() }) // Optional: helps with direct D-pad entry
                        ) {
                            items(allMedia.size) { index ->
                                val url = allMedia[index]
                                GalleryThumbnail(
                                    url = url,
                                    isSelected = url == currentImageUrl,
                                    onClick = {
                                        currentImageUrl = url
                                        isUiVisible = true // keep UI visible while browsing
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = item.architectName?.uppercase() ?: "ARCHITECTURAL SNAPSHOT",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp
                            )
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontSize = 56.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Status/Badge for favorites in the main UI
                        if (isFavorite) {
                            Surface(
                                shape = RoundedCornerShape(32.dp),
                                colors = SurfaceDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("FAVORITE", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                            }
                        }
                    }
                    if (item.description != null) {
                        Text(
                            text = item.description,
                            color = Color.LightGray,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(top = 12.dp).fillMaxWidth(0.6f),
                            lineHeight = 28.sp
                        )
                    }
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
                    colors = SurfaceDefaults.colors(
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
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        PopupOption(
                            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            label = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                            onClick = {
                                item.id?.let { viewModel.toggleFavorite(it) }
                                // No automatic dismissal here, let user toggle back and forth if they want
                            }
                        )

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
                            onClick = { 
                                showPopup = false 
                                focusRequester.requestFocus() // return focus to main box
                            },
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

@Composable
fun GalleryThumbnail(url: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary))
        ),
        modifier = Modifier
            .width(160.dp)
            .height(90.dp)
    ) {
        Box {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                )
            }
        }
    }
}
