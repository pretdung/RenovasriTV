package com.example.renovasritv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.*
import coil.compose.AsyncImage

// Define Colors from the provided design
// Removed AppColors object as UI now uses MaterialTheme.colorScheme dynamically.

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Gallery : Screen("gallery")
    object Detail : Screen("detail/{id}") {
        fun createRoute(id: Long) = "detail/$id"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMap by viewModel.appTheme.collectAsState()

            // Resolve dynamic colors with fallbacks
            val primary = themeMap["primary"]?.colorHex?.toComposeColor() ?: Color(0xFFFFB59E)
            val background = themeMap["background"]?.colorHex?.toComposeColor() ?: Color(0xFF121416)
            val surface = themeMap["surface"]?.colorHex?.toComposeColor() ?: background
            val onPrimary = themeMap["on_primary"]?.colorHex?.toComposeColor() ?: Color(0xFF5E1700)
            val onSurface = themeMap["on_surface"]?.colorHex?.toComposeColor() ?: Color(0xFFE2E2E5)
            val onSurfaceVariant = themeMap["on_surface_variant"]?.colorHex?.toComposeColor() ?: Color(0xFFDDC0B8)
            val tertiary = themeMap["tertiary"]?.colorHex?.toComposeColor() ?: Color(0xFF67D7DA)

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = primary,
                    background = background,
                    surface = surface,
                    onPrimary = onPrimary,
                    onSurface = onSurface,
                    onSurfaceVariant = onSurfaceVariant,
                    tertiary = tertiary
                )
            ) {
                TvApp()
            }
        }
    }
}

@Composable
fun TvApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val menuBackgrounds by viewModel.menuBackgrounds.collectAsState()

    // Find the matching background configuration
    val currentConfig = remember(currentRoute, menuBackgrounds) {
        // Strip arguments for pattern matching (e.g., "detail/{imageUrl}" -> "detail")
        val baseRoute = currentRoute?.substringBefore("/")
        menuBackgrounds.find { it.menuKey == currentRoute || it.menuKey == baseRoute }
    }

    val backgroundImage = currentConfig?.imageUrl
        ?: "https://lh3.googleusercontent.com/aida-public/AB6AXuAh0nYqyiUrKIiX-zNzcnAGgRuY3x-B2LeenQ4fdUqYeXO7Q3aotOML0wRrbv7Qoxb5G7T3gghfKtNZck16FE_j2raPaTVqq071qiG2ZuTGZvQBhJyCYrbWa3glcvwXyPCuaAvSCvEwyzBgRIPsv_4YFHFiqKGH_P9koUUxxu7ZhHuAsuaPNZlrxriwAuB2Wej3AR_pK3Wk-6KXDIORZu_D_uU26Q0sLGTfKVUurWrhTVpK4CSJ5TcniE807FaHKqiXSgRHiVNyarQU"
    val backgroundAlpha = currentConfig?.overlayOpacity ?: 0.6f
    val uiConfigs by viewModel.uiConfigs.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Smoothly transition between background images
        Crossfade(
            targetState = backgroundImage,
            animationSpec = tween(durationMillis = 1000),
            label = "BackgroundTransition"
        ) { targetImage ->
            AsyncImage(
                model = targetImage,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = backgroundAlpha
            )
        }
        
        // Gradient Scrims
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(MaterialTheme.colorScheme.background, Color.Transparent),
                        startX = 0f,
                        endX = 1000f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                        startY = 500f
                    )
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            SideNavigation(navController, uiConfigs)
            
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(navController = navController, viewModel = viewModel)
                }
                composable(Screen.Gallery.route) {
                    GalleryScreen(navController = navController, viewModel = viewModel)
                }
                composable(
                    route = Screen.Detail.route,
                    arguments = listOf(androidx.navigation.navArgument("id") { 
                        type = androidx.navigation.NavType.LongType 
                    })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getLong("id") ?: -1L
                    GalleryDetailScreen(id = id, navController = navController, viewModel = viewModel)
                }
            }
        }
    }
}

fun getIconByName(name: String?): ImageVector? {
    return when (name) {
        "Home" -> Icons.Default.Home
        "Architecture" -> Icons.Default.Architecture
        "TrendingUp" -> Icons.AutoMirrored.Filled.TrendingUp
        "Settings" -> Icons.Default.Settings
        "Search" -> Icons.Default.Search
        "Notifications" -> Icons.Default.Notifications
        "AccountCircle" -> Icons.Default.AccountCircle
        "AddCircle" -> Icons.Default.AddCircle
        else -> null
    }
}

@Composable
fun SideNavigation(navController: NavController, uiConfigs: Map<String, UIConfig> = emptyMap()) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val logoConfig = uiConfigs["sidebar_logo_text"]
    val logoValue = logoConfig?.value ?: "TC"
    val logoType = logoConfig?.type ?: "text"

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(100.dp)
            .background(Color(0xCC1B1B1C)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp)
        ) {
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
                    fontSize = (logoConfig?.fontSize ?: 28).sp,
                    fontStyle = if (logoConfig?.fontStyle == "italic") FontStyle.Italic else FontStyle.Normal,
                    modifier = Modifier.padding(bottom = 48.dp)
                )
            }

            val homeConfig = uiConfigs["sidebar_home_icon"]
            SidebarIcon(
                iconSource = homeConfig?.value,
                iconType = homeConfig?.type,
                defaultIcon = Icons.Default.Home,
                selected = currentRoute == Screen.Home.route,
                onClick = { navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                    launchSingleTop = true
                }}
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            val galleryConfig = uiConfigs["sidebar_gallery_icon"]
            SidebarIcon(
                iconSource = galleryConfig?.value,
                iconType = galleryConfig?.type,
                defaultIcon = Icons.Default.Architecture,
                selected = currentRoute == Screen.Gallery.route,
                onClick = { navController.navigate(Screen.Gallery.route) {
                    launchSingleTop = true
                }}
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            val trendingConfig = uiConfigs["sidebar_trending_icon"]
            SidebarIcon(
                iconSource = trendingConfig?.value,
                iconType = trendingConfig?.type,
                defaultIcon = Icons.AutoMirrored.Filled.TrendingUp,
                selected = false,
                onClick = {}
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            val settingsConfig = uiConfigs["sidebar_settings_icon"]
            SidebarIcon(
                iconSource = settingsConfig?.value,
                iconType = settingsConfig?.type,
                defaultIcon = Icons.Default.Settings,
                selected = false,
                onClick = {}
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            val profileImageUrl = uiConfigs["sidebar_profile_image"]?.value
                ?: "https://lh3.googleusercontent.com/aida-public/AB6AXuB-3-KdaXozzaFiu9TcFSGrrWTMpGyp5HghXlbyWPPxCzP8tyXAQQbkcK-a5bRLAJjNi01A_GhAvtmSbWaY7Q5SCHYHVcYgZ_2ZfS-dPgXBshy2skkcRkLhYOcKiopNYkWgvo1Zz_L3KSIobPIQ4Zc1jrql1XS9cJsBtCbPkoavZzWu-kbqQb0O2XdHkRPcMnpz7QhWY-gam4TMyC-o3nR0ALlJs8frfzLCbLsUf4SpKAIAhape1k95h8eAro8B9LAioJb8E-mKH0c2"
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

@Composable
fun SidebarIcon(
    iconSource: Any?,
    iconType: String? = null,
    defaultIcon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color(0x22FFFFFF)
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        modifier = Modifier.size(56.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .height(32.dp)
                        .width(4.dp)
                        .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(2.dp))
                )
            }

            val iconColor = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF888888)

            when (val source = iconSource) {
                is String -> {
                    val materialIcon = getIconByName(source)
                    if (materialIcon != null) {
                        Icon(
                            imageVector = materialIcon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(28.dp)
                        )
                    } else if (source.startsWith("http")) {
                        AsyncImage(
                            model = source,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            colorFilter = if (iconType == "image") null else androidx.compose.ui.graphics.ColorFilter.tint(iconColor),
                            contentScale = if (iconType == "image") ContentScale.Crop else ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = defaultIcon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                is ImageVector -> {
                    Icon(
                        imageVector = source,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = defaultIcon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    navController: NavController, 
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val trendingItems by viewModel.trendingItems.collectAsState()
    val uiConfigs by viewModel.uiConfigs.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        TopBar(uiConfigs)
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(80.dp),
            contentPadding = PaddingValues(top = 60.dp, bottom = 48.dp)
        ) {
            item {
                HeroSection(navController, uiConfigs)
            }
            if (trendingItems.isNotEmpty()) {
                item {
                    TrendingProjectsSection(navController, trendingItems, uiConfigs)
                }
            }
            item {
                RecentProjectsSection(navController, projects, uiConfigs)
            }
            item {
                FeaturedCategoriesSection(categories, uiConfigs)
            }
        }
    }
}

@Composable
fun TrendingProjectsSection(navController: NavController, items: List<GalleryItem>, uiConfigs: Map<String, UIConfig> = emptyMap()) {
    val sectionTitle = uiConfigs["trending_section_title"]?.value ?: "Trending Now"
    val sectionIcon = getIconByName(uiConfigs["trending_section_icon"]?.value) ?: Icons.AutoMirrored.Filled.TrendingUp
    
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = sectionIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = sectionTitle,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(24.dp), contentPadding = PaddingValues(end = 48.dp)) {
            items(items.size) { index ->
                val item = items[index]
                TrendingCard(item, rank = index + 1) {
                    item.id?.let { id ->
                        navController.navigate(Screen.Detail.createRoute(id))
                    }
                }
            }
        }
    }
}

@Composable
fun TrendingCard(item: GalleryItem, rank: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        modifier = Modifier.width(440.dp).height(240.dp)
    ) {
        Box {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Rank Badge
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                            startY = 200f
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.category.uppercase(),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RemoveRedEye,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.views}",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturedCategoriesSection(categories: List<Category>, uiConfigs: Map<String, UIConfig> = emptyMap()) {
    val sectionTitle = uiConfigs["categories_section_title"]?.value ?: "Design Categories"
    Column {
        Text(
            text = sectionTitle,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            items(categories) { category ->
                Surface(
                    onClick = { },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color(0x22333537),
                        focusedContainerColor = Color.White,
                        focusedContentColor = Color.Black
                    ),
                    modifier = Modifier.width(240.dp).height(140.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(text = category.name.uppercase(), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TopBar(uiConfigs: Map<String, UIConfig> = emptyMap()) {
    val config = uiConfigs["top_bar_title"]
    val titleText = config?.value ?: "THE CINEMATIC CURATOR"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = titleText,
            color = config?.fontColor?.toComposeColor() ?: Color(0xFFBBBBBB),
            fontSize = (config?.fontSize ?: 14).sp,
            fontWeight = config?.fontWeight.toFontWeight(),
            fontStyle = if (config?.fontStyle == "italic") FontStyle.Italic else FontStyle.Normal,
            letterSpacing = 2.sp
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            val searchIcon = getIconByName(uiConfigs["top_bar_search_icon"]?.value) ?: Icons.Default.Search
            val notificationIcon = getIconByName(uiConfigs["top_bar_notifications_icon"]?.value) ?: Icons.Default.Notifications
            val accountIcon = getIconByName(uiConfigs["top_bar_account_icon"]?.value) ?: Icons.Default.AccountCircle

            Surface(
                onClick = {},
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color(0x33333537),
                    focusedContainerColor = Color(0x66333537)
                ),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(24.dp)),
                modifier = Modifier
                    .width(280.dp)
                    .height(40.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = searchIcon,
                        contentDescription = "Search",
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "SEARCH SPACES...",
                        color = Color(0xFF888888),
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(32.dp))
            Surface(
                onClick = { },
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = Color(0x22FFFFFF)
                ),
                shape = ClickableSurfaceDefaults.shape(CircleShape),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = notificationIcon,
                        contentDescription = "Notifications",
                        tint = Color(0xFFDDDDDD),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Surface(
                onClick = { },
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = Color(0x22FFFFFF)
                ),
                shape = ClickableSurfaceDefaults.shape(CircleShape),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = accountIcon,
                        contentDescription = "User",
                        tint = Color(0xFFDDDDDD),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HeroSection(navController: NavController, uiConfigs: Map<String, UIConfig> = emptyMap()) {
    val config1 = uiConfigs["hero_title_1"]
    val config2 = uiConfigs["hero_title_2"]
    val descConfig = uiConfigs["hero_description"]

    val title1 = config1?.value ?: "Redefine your"
    val title2 = config2?.value ?: "spatial narrative."
    val description = descConfig?.value ?: "Experience architectural excellence through our curated AI-driven design engine. Transform any space into a masterpiece of modern living."
    
    val primaryButton = uiConfigs["hero_primary_button"]?.value ?: "New Project"
    val secondaryButton = uiConfigs["hero_secondary_button"]?.value ?: "Explore Gallery"

    Column(modifier = Modifier.fillMaxWidth(0.5f)) {
        Text(
            text = title1,
            color = config1?.fontColor?.toComposeColor() ?: Color.White,
            fontSize = (config1?.fontSize ?: 72).sp,
            fontWeight = config1?.fontWeight.toFontWeight(),
            fontStyle = if (config1?.fontStyle == "italic") FontStyle.Italic else FontStyle.Normal,
            lineHeight = (config1?.fontSize ?: 72).sp,
            letterSpacing = (-2).sp
        )
        Text(
            text = title2,
            color = config2?.fontColor?.toComposeColor() ?: MaterialTheme.colorScheme.primary,
            fontSize = (config2?.fontSize ?: 72).sp,
            fontWeight = config2?.fontWeight.toFontWeight(),
            fontStyle = if (config2?.fontStyle == "italic") FontStyle.Italic else FontStyle.Normal,
            lineHeight = (config2?.fontSize ?: 72).sp,
            letterSpacing = (-2).sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = description,
            color = descConfig?.fontColor?.toComposeColor() ?: MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = (descConfig?.fontSize ?: 20).sp,
            fontWeight = descConfig?.fontWeight.toFontWeight(),
            fontStyle = if (descConfig?.fontStyle == "italic") FontStyle.Italic else FontStyle.Normal,
            lineHeight = ((descConfig?.fontSize ?: 20) * 1.6).sp,
        )
        Spacer(modifier = Modifier.height(40.dp))
        Row {
            Button(
                onClick = { },
                colors = ButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = ButtonDefaults.shape(RoundedCornerShape(32.dp)),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val primaryIconName = uiConfigs["hero_primary_icon"]?.value ?: "AddCircle"
                    val primaryIcon = getIconByName(primaryIconName) ?: Icons.Default.AddCircle
                    Icon(
                        imageVector = primaryIcon,
                        contentDescription = "New",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = primaryButton, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Button(
                onClick = { navController.navigate(Screen.Gallery.route) },
                colors = ButtonDefaults.colors(
                    containerColor = Color(0x33333537),
                    focusedContainerColor = Color(0x66333537),
                    contentColor = Color.White
                ),
                shape = ButtonDefaults.shape(RoundedCornerShape(32.dp)),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(text = secondaryButton, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun RecentProjectsSection(navController: NavController, projects: List<ProjectItem>, uiConfigs: Map<String, UIConfig> = emptyMap()) {
    val titlePart1 = uiConfigs["recent_projects_title_1"]?.value ?: "Recent "
    val titlePart2 = uiConfigs["recent_projects_title_2"]?.value ?: "Projects"
    val viewAllText = uiConfigs["recent_projects_view_all"]?.value ?: "VIEW ALL"

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = titlePart1,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = titlePart2,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                onClick = { navController.navigate(Screen.Gallery.route) },
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                ),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = viewAllText,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            items(projects) { project ->
                ProjectCard(project)
            }
        }
    }
}

// data class ProjectItem removed to Models.kt

@Composable
fun ProjectCard(project: ProjectItem) {
    Column(modifier = Modifier.width(400.dp)) {
        Card(
            onClick = { },
            shape = CardDefaults.shape(RoundedCornerShape(16.dp)),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = project.imageUrl,
                    contentDescription = project.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Status Badge
                Surface(
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = project.statusColor.toComposeColor().copy(alpha = 0.2f),
                        contentColor = project.statusColor.toComposeColor()
                    ),
                    onClick = {},
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(
                            text = project.status.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = project.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = project.subtitle ?: "", color = Color.Gray, fontSize = 14.sp)
    }
}
