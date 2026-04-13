package com.example.renovasritv

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.tv.material3.*
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun GalleryDetailScreen(id: String, initialImageUrl: String? = null, navController: NavController, viewModel: MainViewModel) {
    val galleryItems by viewModel.galleryItems.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val uiConfigs by viewModel.uiConfigs.collectAsState()
    val uiModules by viewModel.uiModules.collectAsState()
    val themeMap by viewModel.appTheme.collectAsState()
    val screenWidth = LocalConfiguration.current.screenWidthDp
    
    // Separate color and transparency logic
    val sidebarColor = themeMap["sidebar_background"]?.colorHex?.toComposeColor() 
        ?: themeMap["surface"]?.colorHex?.toComposeColor() 
        ?: MaterialTheme.colorScheme.surface

    val transparencyRaw = themeMap["sidebar_transparency"]?.colorHex
    val sidebarAlpha = transparencyRaw?.toFloatOrNull() ?: 0.6f

    val sidebarBg = sidebarColor.copy(alpha = sidebarAlpha)

    // Sidebar logic parity with MainActivity
    val activeModules = remember(uiModules) {
        uiModules.filter { it.isActive }.sortedBy { it.orderIndex }
    }
    
    // Theme background for detail page
    val themeBgUrl = themeMap["bg_detail"]?.colorHex 
        ?: "https://xbeslcqosyhyuyxztpov.supabase.co/storage/v1/object/public/media/renovasri-export-1771905706286.jpg"

    val item = remember(id, galleryItems) { 
        galleryItems.find { it.id == id }
    }
    
    var currentImageUrl by remember(id, initialImageUrl) { 
        mutableStateOf(initialImageUrl ?: item?.imageUrl ?: "") 
    }
    
    LaunchedEffect(item) {
        if (currentImageUrl.isEmpty() && item?.imageUrl != null) {
            currentImageUrl = item.imageUrl
        }
    }

    val isFavorite = favoriteIds.contains(id)
    var isUiVisible by remember { mutableStateOf(true) }
    var showPopup by remember { mutableStateOf(false) }
    var isInspectionMode by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableFloatStateOf(1.2f) }
    var panOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    val focusRequester = remember { FocusRequester() }

    val allMedia = remember(item, currentImageUrl) {
        if (item != null) {
            (listOf(item.imageUrl) + item.mediaGallery).distinct()
        } else if (currentImageUrl.isNotEmpty()) {
            listOf(currentImageUrl)
        } else {
            emptyList()
        }
    }

    // Auto-hide bottom UI logic
    LaunchedEffect(isUiVisible, showPopup, isInspectionMode) {
        if (isUiVisible && !showPopup && !isInspectionMode) {
            delay(10000)
            isUiVisible = false
        }
    }

    LaunchedEffect(id) {
        viewModel.incrementViews(id)
        focusRequester.requestFocus()
    }

    val animatedZoom by animateFloatAsState(
        targetValue = zoomLevel,
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
        label = "ZoomAnimation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "KenBurns")
    val kenBurnsVariation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Variation"
    )
    
    val finalScale = if (isInspectionMode) animatedZoom else animatedZoom + kenBurnsVariation

    // Panning constraints logic
    LaunchedEffect(finalScale, containerSize) {
        if (containerSize.width > 0 && containerSize.height > 0) {
            val maxOffsetX = (containerSize.width * (finalScale - 1f) / 2f).coerceAtLeast(0f)
            val maxOffsetY = (containerSize.height * (finalScale - 1f) / 2f).coerceAtLeast(0f)
            
            val constrainedX = panOffset.x.coerceIn(-maxOffsetX, maxOffsetX)
            val constrainedY = panOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
            
            if (constrainedX != panOffset.x || constrainedY != panOffset.y) {
                panOffset = androidx.compose.ui.geometry.Offset(constrainedX, constrainedY)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) 
            .onSizeChanged { containerSize = it }
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent {
                if (it.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (it.key) {
                        Key.Back -> {
                            if (isInspectionMode) {
                                isInspectionMode = false
                                focusRequester.requestFocus()
                                true
                            } else if (showPopup) {
                                showPopup = false
                                focusRequester.requestFocus()
                                true
                            } else {
                                navController.popBackStack()
                                true
                            }
                        }
                        Key.DirectionCenter, Key.Enter -> {
                            if (isInspectionMode) {
                                isInspectionMode = false
                                true
                            } else if (showPopup) {
                                false 
                            } else if (!isUiVisible) {
                                isUiVisible = true
                                true
                            } else {
                                showPopup = true
                                true
                            }
                        }
                        else -> {
                            if (isInspectionMode) {
                                val moveStep = 120f
                                val maxOffsetX = (containerSize.width * (finalScale - 1f) / 2f).coerceAtLeast(0f)
                                val maxOffsetY = (containerSize.height * (finalScale - 1f) / 2f).coerceAtLeast(0f)

                                when (it.key) {
                                    Key.DirectionUp -> { 
                                        panOffset = panOffset.copy(y = (panOffset.y - moveStep).coerceIn(-maxOffsetY, maxOffsetY))
                                        true 
                                    }
                                    Key.DirectionDown -> { 
                                        panOffset = panOffset.copy(y = (panOffset.y + moveStep).coerceIn(-maxOffsetY, maxOffsetY))
                                        true 
                                    }
                                    Key.DirectionLeft -> { 
                                        panOffset = panOffset.copy(x = (panOffset.x - moveStep).coerceIn(-maxOffsetX, maxOffsetX))
                                        true 
                                    }
                                    Key.DirectionRight -> { 
                                        panOffset = panOffset.copy(x = (panOffset.x + moveStep).coerceIn(-maxOffsetX, maxOffsetX))
                                        true 
                                    }
                                    else -> false
                                }
                            } else {
                                isUiVisible = true
                                false
                            }
                        }
                    }
                } else {
                    false
                }
            }
    ) {
        // LAYER 0: Theme Background (Base Layer)
        AsyncImage(
            model = themeBgUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.4f // Subdued background
        )

        // LAYER 1: Main Image (Foreground Layer)
        if (currentImageUrl.isNotEmpty()) {
            androidx.compose.animation.Crossfade(
                targetState = currentImageUrl,
                animationSpec = tween(1500),
                label = "MainImageTransition"
            ) { targetUrl ->
                Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
                    AsyncImage(
                        model = targetUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = finalScale
                                scaleY = finalScale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Horizontal Scrim (Visual parity with MainActivity)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent),
                        startX = 0f,
                        endX = 1200f
                    )
                )
                .zIndex(9f)
        )

        // LAYER 2: Sidebar (Persistent and Still)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(100.dp)
                .background(sidebarBg)
                .zIndex(10f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                // Logo Rendering
                val logoConfig = uiConfigs["sidebar_logo_text"]
                val logoValue = logoConfig?.value ?: "TC"
                val logoType = logoConfig?.type ?: "text"

                if (viewModel.isModuleVisible("sidebar_logo_text")) {
                    if (logoType == "image" || logoValue.startsWith("http")) {
                        AsyncImage(
                            model = logoValue,
                            contentDescription = "Logo",
                            modifier = Modifier.size(48.dp).padding(bottom = 16.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    } else {
                        Text(
                            text = logoValue,
                            color = logoConfig?.fontColor?.toComposeColor() ?: MaterialTheme.colorScheme.primary,
                            fontWeight = logoConfig?.fontWeight.toFontWeight(),
                            fontSize = logoConfig?.fontSize.toFontSize(screenWidth),
                            fontStyle = if (logoConfig?.fontStyle == "italic") FontStyle.Italic else FontStyle.Normal,
                            modifier = Modifier.padding(bottom = 48.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Navigation Icons (Disabled focus during panning to avoid hijacking D-pad)
                activeModules.forEach { module ->
                    val isSelected = module.key == Screen.Gallery.route
                    Box(modifier = Modifier.focusProperties { canFocus = !isInspectionMode }) {
                        SidebarIcon(
                            iconSource = module.icon,
                            iconType = "icon",
                            defaultIcon = Icons.Default.Circle,
                            selected = isSelected,
                            onClick = {
                                if (module.key != Screen.Gallery.route) {
                                    navController.navigate(module.key) {
                                        popUpTo(Screen.Home.route)
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Profile Section
            if (viewModel.isModuleVisible("sidebar_profile_group")) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    val profileImageUrl = uiConfigs["sidebar_profile_image"]?.value
                        ?: "https://lh3.googleusercontent.com/aida-public/AB6AXuB-3-KdaXozzaFiu9TcFSGrrWTMpGyp5HghXlbyWPPxCzP8tyXAQQbkcK-a5bRLAJjNi01A_GhAvtmSbWaY7Q5SCHYHVcYgZ_2ZfS-dPgXBshy2skkcRkLhYOcKiopNYkWgvo1Zz_L3KSIobPIQ4Zc1jrql1XS9cJsBtCbPkoavZzWu-kbqQb0O2XdHkRPcMnpz7QhWY-gam4TMyC-o3nR0ALlJs8frfzLCbLsUf4SpKAIAhape1k95h8eAro8B9LAioJb8E-mKH0c2"
                    Box(modifier = Modifier.focusProperties { canFocus = !isInspectionMode }) {
                        Surface(
                            onClick = {},
                            shape = ClickableSurfaceDefaults.shape(CircleShape),
                            modifier = Modifier.size(40.dp)
                        ) {
                            AsyncImage(
                                model = profileImageUrl,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        // LAYER 3: Bottom Info Overlay (Stationary)
        AnimatedVisibility(
            visible = isUiVisible && !showPopup,
            enter = fadeIn(tween(800)),
            exit = fadeOut(tween(500)),
            modifier = Modifier.zIndex(5f)
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
                        .padding(start = 148.dp, end = 48.dp, bottom = 40.dp)
                ) {
                    // Media Gallery Filmstrip
                    if (allMedia.isNotEmpty()) {
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
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            items(allMedia.size) { index ->
                                val url = allMedia[index]
                                GalleryThumbnail(
                                    url = url,
                                    isSelected = url == currentImageUrl,
                                    onClick = {
                                        currentImageUrl = url
                                        isUiVisible = true
                                        zoomLevel = 1.2f
                                        panOffset = androidx.compose.ui.geometry.Offset.Zero
                                        isInspectionMode = false
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
                        if (item != null) {
                            SectionHeader(
                                subTitle = item.architectName?.uppercase() ?: "ARCHITECTURAL SNAPSHOT",
                                mainTitle = item.title,
                                subConfig = uiConfigs["detail_architect_label"],
                                mainConfig = uiConfigs["detail_title"],
                                screenWidth = screenWidth
                            )

                            AnimatedVisibility(
                                visible = isFavorite,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally()
                            ) {
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
                        } else {
                            Column {
                                Text("MEMUAT DETAIL...", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Architecture Idea", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    if (item?.description != null) {
                        val descConfig = uiConfigs["detail_description"]
                        val descSize = descConfig?.fontSize.toFontSize(screenWidth)

                        Text(
                            text = item.description,
                            color = descConfig?.fontColor?.toComposeColor() ?: Color.LightGray,
                            fontSize = descSize,
                            fontWeight = descConfig?.fontWeight.toFontWeight(),
                            modifier = Modifier.padding(top = 12.dp).fillMaxWidth(0.6f),
                            lineHeight = descSize * 1.5f,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "Press CENTER to toggle options",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }

        // Inspection Mode Indicator (Top Right)
        AnimatedVisibility(
            visible = isInspectionMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(32.dp).zIndex(10f)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                colors = SurfaceDefaults.colors(containerColor = Color.Black.copy(alpha = 0.7f)),
                border = Border(androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.OpenWith, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("PANNING MODE ACTIVE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Press BACK to exit", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                }
            }
        }

        // LAYER 4: Side Drawer (View Options) - "The Glass Capsule" Derivation
        AnimatedVisibility(
            visible = showPopup,
            enter = slideInHorizontally(initialOffsetX = { it / 2 }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it / 2 }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).zIndex(20f)
        ) {
            val drawerFocusRequester = remember { FocusRequester() }
            LaunchedEffect(showPopup) {
                if (showPopup) {
                    delay(400)
                    drawerFocusRequester.requestFocus()
                }
            }

            Surface(
                shape = RoundedCornerShape(32.dp),
                colors = SurfaceDefaults.colors(
                    containerColor = Color(0xCC121212),
                    contentColor = Color.White
                ),
                border = Border(androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))),
                modifier = Modifier
                    .padding(end = 40.dp) // Floating margin from right
                    .width(320.dp)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = "VIEW OPTIONS",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(bottom = 20.dp)

                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PopupOption(
                            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            label = if (isFavorite) "Remove Favorite" else "Add to Favorite",
                            onClick = { viewModel.toggleFavorite(id) },
                            modifier = Modifier.focusRequester(drawerFocusRequester)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                        Spacer(modifier = Modifier.height(8.dp))

                        PopupOption(
                            icon = Icons.Default.ZoomIn,
                            label = "Zoom In",
                            onClick = { if (zoomLevel < 4.0f) zoomLevel += 0.4f }
                        )

                        PopupOption(
                            icon = Icons.Default.ZoomOut,
                            label = "Zoom Out",
                            onClick = { if (zoomLevel > 1.0f) zoomLevel -= 0.4f }
                        )

                        PopupOption(
                            icon = Icons.Default.OpenWith,
                            label = "Panning Mode",
                            onClick = {
                                isInspectionMode = true
                                showPopup = false
                                isUiVisible = false
                                focusRequester.requestFocus()
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                        Spacer(modifier = Modifier.height(8.dp))

                        PopupOption(
                            icon = Icons.Default.Refresh,
                            label = "Reset View",
                            onClick = {
                                zoomLevel = 1.2f
                                panOffset = androidx.compose.ui.geometry.Offset.Zero
                                isInspectionMode = false
                            }
                        )

                        PopupOption(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            label = "Back to Gallery",
                            onClick = { navController.popBackStack() }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        onClick = { 
                            showPopup = false 
                            focusRequester.requestFocus()
                        },
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color(0x11FFFFFF),
                            focusedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            focusedContentColor = Color.White
                        ),
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("CLOSE MENU", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PopupOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    label: String, 
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        modifier = modifier.fillMaxWidth().height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
