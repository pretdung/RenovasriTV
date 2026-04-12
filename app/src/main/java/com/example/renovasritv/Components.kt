package com.example.renovasritv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.*
import coil.compose.AsyncImage

@Composable
fun RouteGuard(
    pageKey: String,
    viewModel: MainViewModel,
    content: @Composable () -> Unit
) {
    val uiConfigs by viewModel.uiConfigs.collectAsState()
    val isVisible = uiConfigs.isModuleVisible(pageKey)
    
    if (isVisible) {
        content()
    } else {
        MaintenanceScreen(viewModel)
    }
}

@Composable
fun MaintenanceScreen(viewModel: MainViewModel) {
    val uiConfigs by viewModel.uiConfigs.collectAsState()
    val screenWidth = LocalConfiguration.current.screenWidthDp
    
    val title = uiConfigs["maintenance_title"]?.value ?: "Fitur Sedang Diperbarui"
    val description = uiConfigs["maintenance_description"]?.value ?: "Kami sedang melakukan peningkatan pada layanan ini. Mohon kembali lagi nanti."

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SettingsSuggest,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
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

            val iconColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

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

fun getIconByName(name: String?): ImageVector? {
    return when (name) {
        "Home" -> Icons.Default.Home
        "Architecture" -> Icons.Default.Architecture
        "Favorite" -> Icons.Default.Favorite
        "SupportAgent" -> Icons.Default.SupportAgent
        "Chat" -> Icons.AutoMirrored.Filled.Chat
        "Settings" -> Icons.Default.Settings
        "Search" -> Icons.Default.Search
        "Notifications" -> Icons.Default.Notifications
        "AccountCircle" -> Icons.Default.AccountCircle
        "AddCircle" -> Icons.Default.AddCircle
        else -> null
    }
}


@Composable
fun GalleryCard(item: GalleryItem, index: Int, isFavorite: Boolean, onClick: () -> Unit) {
    // Asymmetric height based on index
    val height = when (index % 3) {
        0 -> 300.dp
        1 -> 400.dp
        else -> 350.dp
    }

    Column {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            shape = CardDefaults.shape(RoundedCornerShape(12.dp))
        ) {
            Box {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                if (isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(20.dp)
                    )
                }

                // Overlay for title on focus (simplified for now)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Text(text = item.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = item.location, color = Color.LightGray, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectCard(project: ProjectItem) {
    Surface(
        onClick = { },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0x22333537),
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        ),
        modifier = Modifier.width(420.dp).height(240.dp)
    ) {
        Box {
            AsyncImage(
                model = project.imageUrl,
                contentDescription = project.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Bottom gradient/overlay for text
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 100f
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        text = project.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = project.location,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else Color(0x22333537),
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
            focusedContainerColor = if (selected) Color.White else Color(0x44333537),
            focusedContentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun SectionHeader(
    subTitle: String,
    mainTitle: String,
    subConfig: UIConfig? = null,
    mainConfig: UIConfig? = null,
    screenWidth: Int
) {
    Column {
        Text(
            text = subTitle,
            color = subConfig?.fontColor?.toComposeColor() ?: MaterialTheme.colorScheme.primary,
            fontSize = subConfig?.fontSize.toFontSize(screenWidth),
            fontWeight = subConfig?.fontWeight.toFontWeight(),
            fontStyle = if (subConfig?.fontStyle == "italic") FontStyle.Italic else FontStyle.Normal,
            letterSpacing = 4.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = mainTitle,
            color = mainConfig?.fontColor?.toComposeColor() ?: Color.White,
            fontSize = mainConfig?.fontSize.toFontSize(screenWidth),
            fontWeight = mainConfig?.fontWeight.toFontWeight(),
            fontStyle = if (mainConfig?.fontStyle == "italic") FontStyle.Italic else FontStyle.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
