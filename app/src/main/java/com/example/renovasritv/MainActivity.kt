package com.example.renovasritv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Gallery : Screen("gallery")
    object Favorites : Screen("favorites")
    object Consultation : Screen("consultation")
    object Calculator : Screen("calculator")
    object History : Screen("history")
    object Auth : Screen("auth")
    object Settings : Screen("settings")
    object AiResult : Screen("ai_result")
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
    val calcViewModel: CalculatorViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val menuBackgrounds by viewModel.menuBackgrounds.collectAsState()
    val uiConfigs by viewModel.uiConfigs.collectAsState()
    val themeMap by viewModel.appTheme.collectAsState()

    // Map null route to Home to ensure we always have a config if possible
    val effectiveRoute = currentRoute ?: Screen.Home.route

    // 1. Try to find background from the Theme system (theme_colors table)
    val baseRoute = effectiveRoute.substringBefore("/")
    val themeBgKey = "bg_$baseRoute"
    val themeBgUrl = themeMap[themeBgKey]?.colorHex?.takeIf { it.isNotBlank() && it.startsWith("http") }

    // 2. Try to find background from UI Configs (Page specific banners like consultation_banner_image)
    val uiConfigBg = uiConfigs["${baseRoute}_banner_image"]?.value?.takeIf { it.isNotBlank() && it.startsWith("http") }

    // 3. Fallback to global menu_backgrounds
    val currentConfig = remember(effectiveRoute, menuBackgrounds) {
        menuBackgrounds.find { it.menuKey == effectiveRoute || it.menuKey == baseRoute }
    }
    val menuBgUrl = currentConfig?.imageUrl?.takeIf { it.isNotBlank() && it.startsWith("http") }

    val backgroundImage = themeBgUrl 
        ?: uiConfigBg
        ?: menuBgUrl
        ?: "https://xbeslcqosyhyuyxztpov.supabase.co/storage/v1/object/public/media/renovasri-export-1771905706286.jpg"
    
    // Ensure background is always visible with a higher minimum alpha
    val rawAlpha = currentConfig?.overlayOpacity.toOverlayOpacity()
    val backgroundAlpha = if (rawAlpha < 0.4f) 0.5f else rawAlpha

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background Image with explicit key for Crossfade
        Crossfade(
            targetState = backgroundImage,
            animationSpec = tween(durationMillis = 800),
            label = "BackgroundTransition"
        ) { targetImage ->
            AsyncImage(
                model = targetImage,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = backgroundAlpha,
                error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image)
            )
        }
        
        // Very Soft Gradient Scrims
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent),
                        startX = 0f,
                        endX = 1200f // Wider gradient for smoother transition
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                        startY = 600f
                    )
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            if (currentRoute != Screen.Detail.route) {
                SideNavigation(navController, viewModel, authViewModel)
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
                composable(Screen.Calculator.route) {
                    RouteGuard(pageKey = "page_calculator", viewModel = viewModel) {
                        CalculatorWizardScreen(mainViewModel = viewModel, navController = navController, calcViewModel = calcViewModel)
                    }
                }
                composable(Screen.History.route) {
                    RouteGuard(pageKey = "page_history", viewModel = viewModel) {
                        HistoryScreen(navController = navController, viewModel = viewModel, calcViewModel = calcViewModel)
                    }
                }
                composable(Screen.Auth.route) {
                    AuthScreen(onAuthSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    })
                }
                composable(Screen.Settings.route) {
                    ProviderConfigScreen(viewModel = viewModel)
                }
                composable(Screen.AiResult.route) {
                    AiResultScreen(navController = navController, viewModel = viewModel, calcViewModel = calcViewModel)
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
fun SideNavigation(navController: NavController, viewModel: MainViewModel, authViewModel: AuthViewModel) {
    val uiConfigs by viewModel.uiConfigs.collectAsState()
    val uiModules by viewModel.uiModules.collectAsState()
    val themeMap by viewModel.appTheme.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    var isExpanded by remember { mutableStateOf(false) }
    val sidebarWidth by animateDpAsState(
        targetValue = if (isExpanded) 280.dp else 100.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "SidebarWidth"
    )

    val sidebarColor = themeMap["sidebar_background"]?.colorHex?.toComposeColor() 
        ?: themeMap["surface"]?.colorHex?.toComposeColor() 
        ?: MaterialTheme.colorScheme.surface

    val transparencyRaw = themeMap["sidebar_transparency"]?.colorHex
    val sidebarAlpha = transparencyRaw?.toFloatOrNull() ?: 0.7f
    val sidebarBg = sidebarColor.copy(alpha = sidebarAlpha)

    val activeModules = remember(uiModules) {
        uiModules.filter { it.isActive }.sortedBy { it.orderIndex }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(sidebarWidth)
            .background(sidebarBg)
            .onFocusChanged { isExpanded = it.hasFocus }
            .padding(horizontal = 16.dp),
        horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
    ) {
        // Logo Section
        Column(
            horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            if (viewModel.isModuleVisible("sidebar_logo_text")) {
                val logoConfig = uiConfigs["sidebar_logo_text"]
                val logoValue = logoConfig?.value ?: "R"
                
                Box(
                    modifier = Modifier.height(80.dp).fillMaxWidth(),
                    contentAlignment = if (isExpanded) Alignment.CenterStart else Alignment.Center
                ) {
                    Text(
                        text = if (isExpanded) (logoConfig?.value ?: "RENOVASRI") else logoValue.take(1),
                        color = logoConfig?.fontColor?.toComposeColor() ?: MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        fontSize = if (isExpanded) 24.sp else 32.sp,
                        letterSpacing = if (isExpanded) 2.sp else 0.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Navigation Items (Scrollable area)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
        ) {
            activeModules.forEach { module ->
                val itemRoute = module.key
                val isSelected = currentRoute == itemRoute || (currentRoute?.startsWith(itemRoute) == true)
                
                SidebarItem(
                    label = module.label,
                    iconSource = module.icon,
                    selected = isSelected,
                    isExpanded = isExpanded,
                    onClick = {
                        navController.navigate(itemRoute) {
                            if (itemRoute == Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                            launchSingleTop = true
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Identity / Profile Node (Fixed at bottom)
        Spacer(modifier = Modifier.height(16.dp))
        ProfileNode(
            isExpanded = isExpanded, 
            viewModel = viewModel, 
            navController = navController, 
            authViewModel = authViewModel
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}
