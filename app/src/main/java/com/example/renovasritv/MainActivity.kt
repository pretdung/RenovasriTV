package com.example.renovasritv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Gallery : Screen("gallery")
    object Favorites : Screen("favorites")
    object Consultation : Screen("consultation")
    object Detail : Screen("detail/{id}?initialImage={initialImage}") {
        fun createRoute(id: String, initialImage: String? = null) = 
            if (initialImage != null) "detail/$id?initialImage=${java.net.URLEncoder.encode(initialImage, "UTF-8")}"
            else "detail/$id"
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
    val backgroundAlpha = currentConfig?.overlayOpacity.toOverlayOpacity()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Always show global background as a fallback/scrim layer
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
            if (currentRoute != Screen.Detail.route) {
                SideNavigation(navController, viewModel)
            }
            
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(navController = navController, viewModel = viewModel)
                }
                composable(Screen.Gallery.route) {
                    RouteGuard(pageKey = "page_gallery", viewModel = viewModel) {
                        GalleryScreen(navController = navController, viewModel = viewModel)
                    }
                }
                composable(Screen.Favorites.route) {
                    RouteGuard(pageKey = "page_favorites", viewModel = viewModel) {
                        FavoritesScreen(navController = navController, viewModel = viewModel)
                    }
                }
                composable(Screen.Consultation.route) {
                    RouteGuard(pageKey = "page_consultation", viewModel = viewModel) {
                        ConsultationScreen(navController = navController, viewModel = viewModel)
                    }
                }
                composable(
                    route = Screen.Detail.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("initialImage") { 
                            type = androidx.navigation.NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: ""
                    val initialImage = backStackEntry.arguments?.getString("initialImage")
                    GalleryDetailScreen(id = id, initialImageUrl = initialImage, navController = navController, viewModel = viewModel)
                }
            }
        }
    }
}


@Composable
fun SideNavigation(navController: NavController, viewModel: MainViewModel) {
    val uiConfigs by viewModel.uiConfigs.collectAsState()
    val uiModules by viewModel.uiModules.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val screenWidth = LocalConfiguration.current.screenWidthDp
    
    // Use the dedicated modules table for the sidebar
    val activeModules = remember(uiModules) {
        uiModules.filter { it.isActive }.sortedBy { it.orderIndex }
    }

    val logoConfig = uiConfigs["sidebar_logo_text"]
    val logoValue = logoConfig?.value ?: "TC"
    val logoType = logoConfig?.type ?: "text"

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(100.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            // Logo Rendering (Modular)
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

            // Dedicated UI Modules for Sidebar Navigation
            activeModules.forEach { module ->
                val itemRoute = module.key
                val isSelected = currentRoute == itemRoute || (currentRoute?.startsWith(itemRoute) == true)
                
                SidebarIcon(
                    iconSource = module.icon,
                    iconType = "icon", // Standard Material Icons
                    defaultIcon = Icons.Default.Circle,
                    selected = isSelected,
                    onClick = {
                        navController.navigate(itemRoute) {
                            if (itemRoute == Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                            launchSingleTop = true
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Profile Section (Modular)
        if (viewModel.isModuleVisible("sidebar_profile_group")) {
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
}
