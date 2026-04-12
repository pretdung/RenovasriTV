package com.example.renovasritv

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavController, 
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val homeCurations by viewModel.homeCurations.collectAsState()
    val uiConfigs by viewModel.uiConfigs.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        verticalArrangement = Arrangement.spacedBy(25.dp), // Even tighter spacing between sections
        contentPadding = PaddingValues(top = 32.dp, bottom = 48.dp)
    ) {
        // Sinkronisasi dengan Prefix AREA_ - Sekarang sebagai bagian dari List agar scroll lancar
        if (uiConfigs.isModuleVisible("AREA_top_bar")) {
            item {
                TopBar(uiConfigs)
            }
        }
        
        if (uiConfigs.isModuleVisible("AREA_hero_section")) {
            item {
                HeroSection(navController, uiConfigs)
            }
        }
        
        if (uiConfigs.isModuleVisible("AREA_categories_section")) {
            item {
                FeaturedCategoriesSection(navController, homeCurations, uiConfigs)
            }
        }
    }
}

@Composable
fun TopBar(uiConfigs: Map<String, UIConfig> = emptyMap()) {
    // Seluruh elemen TopBar dihapus sesuai permintaan
    Box(modifier = Modifier.fillMaxWidth().height(1.dp))
}

@Composable
fun HeroSection(navController: NavController, uiConfigs: Map<String, UIConfig> = emptyMap()) {
    val config1 = uiConfigs["hero_title_1"]
    val config2 = uiConfigs["hero_title_2"]
    val descConfig = uiConfigs["hero_description"]
    val screenWidth = LocalConfiguration.current.screenWidthDp

    val title1 = config1?.value ?: "Redefine your"
    val title2 = config2?.value ?: "spatial narrative."
    val description = descConfig?.value ?: "Experience architectural excellence through our curated AI-driven design engine. Transform any space into a masterpiece of modern living."
    
    // Aligned with DB: BTN_ prefix
    val primaryButtonLabel = uiConfigs["BTN_hero_primary"]?.value ?: "New Project"
    val secondaryButtonLabel = uiConfigs["BTN_hero_secondary"]?.value ?: "Explore Gallery"

    val fontSize1 = config1?.fontSize.toFontSize(screenWidth)
    val fontSize2 = config2?.fontSize.toFontSize(screenWidth)
    val fontSizeDesc = descConfig?.fontSize.toFontSize(screenWidth)

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth(0.85f)) {
        Text(
            text = title1,
            color = config1?.fontColor?.toComposeColor() ?: Color.White,
            fontSize = fontSize1,
            fontWeight = config1?.fontWeight.toFontWeight(),
            fontStyle = if (config1?.fontStyle == "italic") FontStyle.Italic else FontStyle.Normal,
            lineHeight = fontSize1,
            letterSpacing = (-2).sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = title2,
            color = config2?.fontColor?.toComposeColor() ?: MaterialTheme.colorScheme.primary,
            fontSize = fontSize2,
            fontWeight = config2?.fontWeight.toFontWeight(),
            fontStyle = if (config2?.fontStyle == "italic") FontStyle.Italic else FontStyle.Normal,
            lineHeight = fontSize2,
            letterSpacing = (-2).sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(12.dp)) // Reduced from 24.dp

        // Scrollable Description with D-Pad Support and Scrollbar
        val canScroll = scrollState.maxValue > 0
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 160.dp)
                .onFocusChanged { state -> isFocused = state.isFocused }
                .then(if (canScroll) Modifier.focusable() else Modifier)
                .onKeyEvent { event ->
                    if (canScroll && event.type == KeyEventType.KeyDown) {
                        when (event.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (scrollState.value < scrollState.maxValue) {
                                    coroutineScope.launch {
                                        scrollState.animateScrollBy(100f)
                                    }
                                    true
                                } else false // Allow focus to move to buttons below
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                if (scrollState.value > 0) {
                                    coroutineScope.launch {
                                        scrollState.animateScrollBy(-100f)
                                    }
                                    true
                                } else false
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            Text(
                text = description,
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(end = 5.dp),
                color = descConfig?.fontColor?.toComposeColor() ?: MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = fontSizeDesc,
                fontWeight = descConfig?.fontWeight.toFontWeight(),
                fontStyle = if (descConfig?.fontStyle == "italic") FontStyle.Italic else FontStyle.Normal,
                lineHeight = fontSizeDesc * 1.6f,
            )

            // Dynamic Scrollbar Status
            val scrollFraction by remember {
                derivedStateOf {
                    if (scrollState.maxValue > 0) {
                        scrollState.value.toFloat() / scrollState.maxValue.toFloat()
                    } else 0f
                }
            }

            if (canScroll) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            if (isFocused) Color.White.copy(alpha = 0.2f) 
                            else Color.White.copy(alpha = 0.05f), 
                            RoundedCornerShape(2.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.3f) // Thumb size
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationY = scrollFraction * (size.height * 0.7f)
                            }
                            .background(
                                if (isFocused) MaterialTheme.colorScheme.primary 
                                else Color.White.copy(alpha = 0.4f), 
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun FeaturedCategoriesSection(
    navController: NavController, 
    curations: List<HomeCuration>, 
    uiConfigs: Map<String, UIConfig> = emptyMap()
) {
    val sectionTitle = uiConfigs["categories_section_title"]?.value ?: "Design Categories"
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = sectionTitle,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(15.dp)) // Reduced from 12.dp to bring gallery even closer to title
        
        if (curations.isEmpty()) {
            Surface(
                onClick = { },
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.05f),
                    focusedContainerColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Refreshing curated gallery...", color = Color.Gray)
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(40.dp),
                contentPadding = PaddingValues(start = 17.dp, end = 32.dp) // Start set to 0 to move thumbnails as far left as possible
            ) {
                items(curations) { curation ->
                    Surface(
                        onClick = { 
                            val targetId = curation.targetId ?: curation.id?.toString() ?: ""
                            if (targetId.isNotEmpty()) {
                                navController.navigate(Screen.Detail.createRoute(targetId, curation.imageUrl))
                            }
                        },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)), 
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color(0x22333537),
                            focusedContainerColor = Color.White,
                            focusedContentColor = Color.Black
                        ),
                        modifier = Modifier
                            .width(320.dp) // Slightly wider for better aspect ratio
                            .height(180.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = curation.imageUrl,
                                contentDescription = curation.caption,
                                contentScale = ContentScale.Crop, // Fully filled
                                modifier = Modifier.fillMaxSize(),
                                placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery),
                                error = androidx.compose.ui.res.painterResource(android.R.drawable.stat_notify_error)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                            ) {
                                Text(
                                    text = curation.caption ?: "Inspiring Space",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "See Details",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
