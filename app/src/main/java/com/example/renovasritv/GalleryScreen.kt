package com.example.renovasritv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.*
import coil.compose.AsyncImage

@Composable
fun GalleryScreen(navController: NavController) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Residential", "Commercial", "Industrial", "Landscape", "Interior")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CURATED",
                    color = AppColors.Primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "Design Gallery",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Search Bar (reused style)
            Surface(
                onClick = {},
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color(0x33333537),
                    focusedContainerColor = Color(0x66333537)
                ),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(24.dp)),
                modifier = Modifier
                    .width(320.dp)
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "SEARCH COLLECTIONS...",
                        color = Color(0xFF888888),
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Filter Chips
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            filters.forEach { filter ->
                FilterChip(
                    text = filter,
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Editorial Grid
        GalleryGrid(navController)
    }
}

@Composable
fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) AppColors.Primary else Color(0x22333537),
            contentColor = if (selected) AppColors.OnPrimary else Color.White,
            focusedContainerColor = if (selected) Color.White else Color(0x44333537),
            focusedContentColor = if (selected) AppColors.OnPrimary else AppColors.Primary
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
fun GalleryGrid(navController: NavController) {
    val items = listOf(
        GalleryItem("https://lh3.googleusercontent.com/aida-public/AB6AXuBM-o1X2R7Z6W9_v5O5zXq_vH3V9H9Y8G8p9_k8v9_f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8", "Minimalist Retreat", "Kyoto, Japan"),
        GalleryItem("https://lh3.googleusercontent.com/aida-public/AB6AXuD-Wklx3B11_76a_UfUzdZeAp2yx7Orz4FXAYPb2DA6bo2G_dDF2XaASOEge6y4C55hdjuVTR2MFmAo6cX_3JXphrh4ANAO5QM7OEotWkU5bAvAQZF84qgm_VKAZTuhsfjKnjYhKD3tF3Ykv38WHvgKAVwN2Yww9BoM1kscYEDwXlN4FRiTdttbBU15D8N2jnjrQ0tflAfnLQ1lb0cISf3Eij7h7RWCaAD1JuIINhne5N6V6LpWTH0s1GFjZtkwX4GMGi2fIM6kFXPq", "Ocean Breeze Villa", "Malibu, CA"),
        GalleryItem("https://lh3.googleusercontent.com/aida-public/AB6AXuDeFPOrzYy2Xez12GB9ZtNedqv3wWM5NkV8lWxVC_MaW0AohXd2hhpJncGU8KHAcbP6pFKyrIup9pM2cSZY6QGJSi1Iy_f3FzHvperaBG85ELm1Iez2ZEXWkhtAc1kFz2O2_Jg8aP_xy1i50dC4WfJEuA-1bKTT1nYlgqLfR8Y-3T696ALCb8hj3RS6yyXUVxGo8uM8UcnP-MZMZ1PBW2lfskY96zoe2kcNck8WHbHMAI26fa75i4gFLFEDZIaq42gdd-sXsoxcv6iy", "Industrial Loft", "Berlin, Germany"),
        GalleryItem("https://lh3.googleusercontent.com/aida-public/AB6AXuCil8JHETLNT_KGcUntLaWJWBG6uQqJUoOnmHvUpWeqjM78iSpVblgOgN-batf9qJE46RIGlMyl6SZI2AxDg3v4iN0jPuQmtScB5N_Z91K9RZqNiBxKjDj31L2ogbZIRhA5qCbnuSJyhD-7-iB1hEG4z0mUMgOvAX2qm926cO1IdtmPqag_luQpLk-EIs-a9MYos0yNrgS_6MJgqj1FZ1tAb8SYT18nDlIdVqz0ImxeW7qXdbBYnCj9LZIcUYfu-r3xCK0QJ8A7YpIk", "Forest Cabin", "Oslo, Norway"),
        GalleryItem("https://lh3.googleusercontent.com/aida-public/AB6AXuAh0nYqyiUrKIiX-zNzcnAGgRuY3x-B2LeenQ4fdUqYeXO7Q3aotOML0wRrbv7Qoxb5G7T3gghfKtNZck16FE_j2raPaTVqq071qiG2ZuTGZvQBhJyCYrbWa3glcvwXyPCuaAvSCvEwyzBgRIPsv_4YFHFiqKGH_P9koUUxxu7ZhHuAsuaPNZlrxriwAuB2Wej3AR_pK3Wk-6KXDIORZu_D_uU26Q0sLGTfKVUurWrhTVpK4CSJ5TcniE807FaHKqiXSgRHiVNyarQU", "Desert Oasis", "Dubai, UAE"),
        GalleryItem("https://lh3.googleusercontent.com/aida-public/AB6AXuB-3-KdaXozzaFiu9TcFSGrrWTMpGyp5HghXlbyWPPxCzP8tyXAQQbkcK-a5bRLAJjNi01A_GhAvtmSbWaY7Q5SCHYHVcYgZ_2ZfS-dPgXBshy2skkcRkLhYOcKiopNYkWgvo1Zz_L3KSIobPIQ4Zc1jrql1XS9cJsBtCbPkoavZzWu-kbqQb0O2XdHkRPcMnpz7QhWY-gam4TMyC-o3nR0ALlJs8frfzLCbLsUf4SpKAIAhape1k95h8eAro8B9LAioJb8E-mKH0c2", "Urban Sanctuary", "New York, NY")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(items.size) { index ->
            GalleryCard(items[index], index, onClick = {
                navController.navigate(Screen.Detail.createRoute(items[index].imageUrl))
            })
        }
    }
}

data class GalleryItem(val imageUrl: String, val title: String, val location: String)

@Composable
fun GalleryCard(item: GalleryItem, index: Int, onClick: () -> Unit) {
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
