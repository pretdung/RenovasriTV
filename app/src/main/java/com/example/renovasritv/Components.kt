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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.tv.material3.*
import java.util.Locale
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
    var isFocused by remember { mutableStateOf(false) }
    // Scale up if focused OR if this is the currently active page (selected)
    val iconSize = if (isFocused || selected) 42.dp else 21.dp

    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color(0x22FFFFFF)
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        modifier = Modifier
            .size(56.dp)
            .onFocusChanged { isFocused = it.isFocused }
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
                            modifier = Modifier.size(iconSize)
                        )
                    } else if (source.startsWith("http")) {
                        AsyncImage(
                            model = source,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                            colorFilter = if (iconType == "image") null else androidx.compose.ui.graphics.ColorFilter.tint(iconColor),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = defaultIcon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
                is ImageVector -> {
                    Icon(
                        imageVector = source,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(iconSize)
                    )
                }
                else -> {
                    Icon(
                        imageVector = defaultIcon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(iconSize)
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
        "Calculate" -> Icons.Default.Calculate
        "History" -> Icons.Default.History
        "ReceiptLong" -> Icons.Default.ReceiptLong
        else -> null
    }
}


@Composable
fun GalleryCard(item: GalleryItem, index: Int, isFavorite: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0x22333537),
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        ),
        modifier = Modifier
            .width(400.dp)
            .aspectRatio(16f / 9f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Center-focused scrim for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.7f)),
                            radius = 600f
                        )
                    )
            )

            if (isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(24.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.title.uppercase(),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.location,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NumericStepper(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    unit: String = "",
    step: Float = 0.1f,
    min: Float = 0f,
    max: Float = 100f,
    labelConfig: UIConfig? = null,
    valueConfig: UIConfig? = null,
    enabled: Boolean = true,
    containerColor: Color = Color.White,
    firstButtonFocusRequester: androidx.compose.ui.focus.FocusRequester? = null
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    
    val contentColor = Color.Black
    val focusedContainerColor = MaterialTheme.colorScheme.primary
    val focusedContentColor = Color.White

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (label != null && label.isNotEmpty()) {
            Text(
                text = label,
                color = Color.Black,
                fontSize = labelConfig?.fontSize.toFontSize(screenWidth),
                fontWeight = labelConfig?.fontWeight.toFontWeight() ?: FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(containerColor, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            // Button Minus
            Surface(
                onClick = { 
                    val newValue = (value - step).coerceAtLeast(min)
                    onValueChange(String.format(Locale.US, "%.1f", newValue).toFloat())
                },
                enabled = enabled && value > min,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = contentColor,
                    focusedContainerColor = focusedContainerColor,
                    focusedContentColor = focusedContentColor,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = Color.Gray
                ),
                modifier = Modifier
                    .size(48.dp)
                    .then(if (firstButtonFocusRequester != null) Modifier.focusRequester(firstButtonFocusRequester) else Modifier)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(24.dp))
                }
            }

            // Value Display
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${String.format(Locale.US, if (step >= 1f) "%.0f" else "%.1f", value)}${if (unit.isNotEmpty()) " $unit" else ""}",
                    color = Color.Black,
                    fontSize = valueConfig?.fontSize.toFontSize(screenWidth).takeIf { it != 0.sp } ?: 18.sp,
                    fontWeight = valueConfig?.fontWeight.toFontWeight() ?: FontWeight.Black,
                    maxLines = 1
                )
            }

            // Button Plus
            Surface(
                onClick = { 
                    val newValue = (value + step).coerceAtMost(max)
                    onValueChange(String.format(Locale.US, "%.1f", newValue).toFloat())
                },
                enabled = enabled && value < max,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = contentColor,
                    focusedContainerColor = focusedContainerColor,
                    focusedContentColor = focusedContentColor,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = Color.Gray
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(24.dp))
                }
            }
        }
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
