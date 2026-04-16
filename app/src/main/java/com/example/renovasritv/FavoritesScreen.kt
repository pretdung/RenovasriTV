package com.example.renovasritv

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.*
import coil.compose.AsyncImage

@Composable
fun FavoritesScreen(navController: NavController, viewModel: MainViewModel) {
    val galleryItems by viewModel.galleryItems.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val uiConfigs by viewModel.uiConfigs.collectAsState()
    val screenWidth = LocalConfiguration.current.screenWidthDp

    // Filter items based on the favorites set
    val favoritedItems = remember(galleryItems, favoriteIds) {
        galleryItems.filter { it.id in favoriteIds }
    }

    // CMS Configs for the header
    val subConfig = uiConfigs["favorites_header_sub"]
    val mainConfig = uiConfigs["favorites_header_main"]
    val emptyStateConfig = uiConfigs["favorites_empty_text"]
    val iconConfig = uiConfigs["favorites_header_icon"]
    
    val headerSub = subConfig?.value ?: "YOUR COLLECTION"
    val headerMain = mainConfig?.value ?: "Saved Projects"
    val emptyText = emptyStateConfig?.value ?: "You haven't saved any projects yet. Explore the gallery to find inspiration!"
    val headerIcon = getIconByName(iconConfig?.value) ?: Icons.Default.Favorite

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        // Header Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = headerIcon,
                contentDescription = null,
                tint = iconConfig?.fontColor?.toComposeColor() ?: MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(iconConfig?.fontSize.toIconSize())
            )
            Spacer(modifier = Modifier.width(20.dp))
            SectionHeader(
                subTitle = headerSub,
                mainTitle = headerMain,
                subConfig = subConfig,
                mainConfig = mainConfig,
                screenWidth = screenWidth
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        if (favoritedItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = emptyText,
                        color = Color.Gray,
                        fontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth(0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                    Button(
                        onClick = { navController.navigate(Screen.Gallery.route) },
                        colors = ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = Color.White,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Explore Gallery", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(favoritedItems.size) { index ->
                    val item = favoritedItems[index]
                    // Reuse GalleryCard but force isFavorite to true
                    GalleryCard(
                        item = item,
                        index = index,
                        isFavorite = true,
                        onClick = {
                            item.id?.let { id ->
                                navController.navigate(Screen.Detail.createRoute(id))
                            }
                        }
                    )
                }
            }
        }
    }
}
