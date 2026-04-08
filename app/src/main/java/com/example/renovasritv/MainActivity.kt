package com.example.renovasritv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.*
import coil.compose.AsyncImage

// Define Colors from the provided design
object AppColors {
    val Background = Color(0xFF121416)
    val SurfaceDim = Color(0xFF121416)
    val Primary = Color(0xFFFFB59E)
    val PrimaryContainer = Color(0xFFE1704B)
    val OnPrimary = Color(0xFF5E1700)
    val Tertiary = Color(0xFF67D7DA)
    val OnSurface = Color(0xFFE2E2E5)
    val OnSurfaceVariant = Color(0xFFDDC0B8)
    val SurfaceContainerHighest = Color(0xFF333537)
}

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Gallery : Screen("gallery")
    object Detail : Screen("detail/{imageUrl}") {
        fun createRoute(imageUrl: String) = "detail/${java.net.URLEncoder.encode(imageUrl, "UTF-8")}"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = AppColors.Primary,
                    background = AppColors.Background,
                    surface = AppColors.Background,
                    onPrimary = AppColors.OnPrimary,
                    onSurface = AppColors.OnSurface,
                    onSurfaceVariant = AppColors.OnSurfaceVariant
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        // Hero Background Image (Behind everything)
        AsyncImage(
            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuAh0nYqyiUrKIiX-zNzcnAGgRuY3x-B2LeenQ4fdUqYeXO7Q3aotOML0wRrbv7Qoxb5G7T3gghfKtNZck16FE_j2raPaTVqq071qiG2ZuTGZvQBhJyCYrbWa3glcvwXyPCuaAvSCvEwyzBgRIPsv_4YFHFiqKGH_P9koUUxxu7ZhHuAsuaPNZlrxriwAuB2Wej3AR_pK3Wk-6KXDIORZu_D_uU26Q0sLGTfKVUurWrhTVpK4CSJ5TcniE807FaHKqiXSgRHiVNyarQU",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.6f
        )
        
        // Gradient Scrims
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(AppColors.Background, Color.Transparent),
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
                        colors = listOf(Color.Transparent, AppColors.Background),
                        startY = 500f
                    )
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            SideNavigation(navController)
            
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(navController = navController)
                }
                composable(Screen.Gallery.route) {
                    GalleryScreen(navController = navController)
                }
                composable(
                    route = Screen.Detail.route,
                    arguments = listOf(androidx.navigation.navArgument("imageUrl") { 
                        type = androidx.navigation.NavType.StringType 
                    })
                ) { backStackEntry ->
                    val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
                    GalleryDetailScreen(imageUrl = imageUrl, navController = navController)
                }
            }
        }
    }
}

@Composable
fun SideNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
            Text(
                text = "TC",
                color = AppColors.Primary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            SidebarIcon(
                icon = Icons.Default.Home,
                selected = currentRoute == Screen.Home.route,
                onClick = { navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                    launchSingleTop = true
                }}
            )
            Spacer(modifier = Modifier.height(24.dp))
            SidebarIcon(
                icon = Icons.Default.Architecture,
                selected = currentRoute == Screen.Gallery.route,
                onClick = { navController.navigate(Screen.Gallery.route) {
                    launchSingleTop = true
                }}
            )
            Spacer(modifier = Modifier.height(24.dp))
            SidebarIcon(icon = Icons.AutoMirrored.Filled.TrendingUp, selected = false, onClick = {})
            Spacer(modifier = Modifier.height(24.dp))
            SidebarIcon(icon = Icons.Default.Settings, selected = false, onClick = {})
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Surface(
                onClick = {},
                shape = ClickableSurfaceDefaults.shape(CircleShape),
                modifier = Modifier.size(40.dp)
            ) {
                AsyncImage(
                    model = "https://lh3.googleusercontent.com/aida-public/AB6AXuB-3-KdaXozzaFiu9TcFSGrrWTMpGyp5HghXlbyWPPxCzP8tyXAQQbkcK-a5bRLAJjNi01A_GhAvtmSbWaY7Q5SCHYHVcYgZ_2ZfS-dPgXBshy2skkcRkLhYOcKiopNYkWgvo1Zz_L3KSIobPIQ4Zc1jrql1XS9cJsBtCbPkoavZzWu-kbqQb0O2XdHkRPcMnpz7QhWY-gam4TMyC-o3nR0ALlJs8frfzLCbLsUf4SpKAIAhape1k95h8eAro8B9LAioJb8E-mKH0c2",
                    contentDescription = "Profile",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun SidebarIcon(icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
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
                        .background(AppColors.Tertiary, RoundedCornerShape(2.dp))
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) AppColors.Primary else Color(0xFF888888),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun HomeScreen(navController: NavController, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        TopBar()
        Spacer(modifier = Modifier.height(60.dp))
        HeroSection(navController)
        Spacer(modifier = Modifier.weight(1f))
        RecentProjectsSection(navController)
    }
}

@Composable
fun TopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "THE CINEMATIC CURATOR",
            color = Color(0xFFBBBBBB),
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 2.sp
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
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
                        imageVector = Icons.Default.Search,
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
                        imageVector = Icons.Default.Notifications,
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
                        imageVector = Icons.Default.AccountCircle,
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
fun HeroSection(navController: NavController) {
    Column(modifier = Modifier.fillMaxWidth(0.5f)) {
        Text(
            text = "Redefine your",
            color = Color.White,
            fontSize = 72.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 72.sp,
            letterSpacing = (-2).sp
        )
        Text(
            text = "spatial narrative.",
            color = AppColors.Primary,
            fontSize = 72.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Light,
            lineHeight = 72.sp,
            letterSpacing = (-2).sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Experience architectural excellence through our curated AI-driven design engine. Transform any space into a masterpiece of modern living.",
            color = AppColors.OnSurfaceVariant,
            fontSize = 20.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.height(40.dp))
        Row {
            Button(
                onClick = { },
                colors = ButtonDefaults.colors(
                    containerColor = AppColors.Primary,
                    focusedContainerColor = Color.White,
                    contentColor = AppColors.OnPrimary
                ),
                shape = ButtonDefaults.shape(RoundedCornerShape(32.dp)),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "New",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "New Project", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                Text(text = "Explore Gallery", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun RecentProjectsSection(navController: NavController) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Recent ",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = "Projects",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                onClick = { navController.navigate(Screen.Gallery.route) },
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = Color(0x2267D7DA)
                ),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = "VIEW ALL",
                    color = AppColors.Tertiary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        val projects = listOf(
            ProjectItem("Bel Air Penthouse", "Modified 2 hours ago • Modernist Kitchen", "Draft", AppColors.Tertiary, "https://lh3.googleusercontent.com/aida-public/AB6AXuD-Wklx3B11_76a_UfUzdZeAp2yx7Orz4FXAYPb2DA6bo2G_dDF2XaASOEge6y4C55hdjuVTR2MFmAo6cX_3JXphrh4ANAO5QM7OEotWkU5bAvAQZF84qgm_VKAZTuhsfjKnjYhKD3tF3Ykv38WHvgKAVwN2Yww9BoM1kscYEDwXlN4FRiTdttbBU15D8N2jnjrQ0tflAfnLQ1lb0cISf3Eij7h7RWCaAD1JuIINhne5N6V6LpWTH0s1GFjZtkwX4GMGi2fIM6kFXPq"),
            ProjectItem("Serenity Suite", "Modified yesterday • Japandi Bedroom", "Rendering", AppColors.Primary, "https://lh3.googleusercontent.com/aida-public/AB6AXuDeFPOrzYy2Xez12GB9ZtNedqv3wWM5NkV8lWxVC_MaW0AohXd2hhpJncGU8KHAcbP6pFKyrIup9pM2cSZY6QGJSi1Iy_f3FzHvperaBG85ELm1Iez2ZEXWkhtAc1kFz2O2_Jg8aP_xy1i50dC4WfJEuA-1bKTT1nYlgqLfR8Y-3T696ALCb8hj3RS6yyXUVxGo8uM8UcnP-MZMZ1PBW2lfskY96zoe2kcNck8WHbHMAI26fa75i4gFLFEDZIaq42gdd-sXsoxcv6iy"),
            ProjectItem("The Executive Study", "Modified 3 days ago • Dark Academia", "Archived", Color.Gray, "https://lh3.googleusercontent.com/aida-public/AB6AXuCil8JHETLNT_KGcUntLaWJWBG6uQqJUoOnmHvUpWeqjM78iSpVblgOgN-batf9qJE46RIGlMyl6SZI2AxDg3v4iN0jPuQmtScB5N_Z91K9RZqNiBxKjDj31L2ogbZIRhA5qCbnuSJyhD-7-iB1hEG4z0mUMgOvAX2qm926cO1IdtmPqag_luQpLk-EIs-a9MYos0yNrgS_6MJgqj1FZ1tAb8SYT18nDlIdVqz0ImxeW7qXdbBYnCj9LZIcUYfu-r3xCK0QJ8A7YpIk")
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            items(projects) { project ->
                ProjectCard(project)
            }
        }
    }
}

data class ProjectItem(val title: String, val subtitle: String, val status: String, val statusColor: Color, val imageUrl: String)

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
                        containerColor = project.statusColor.copy(alpha = 0.2f),
                        contentColor = project.statusColor
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
        Text(text = project.subtitle, color = Color.Gray, fontSize = 14.sp)
    }
}
