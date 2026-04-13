package com.example.renovasritv

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.*
import coil.compose.AsyncImage

@Composable
fun GalleryScreen(navController: NavController, viewModel: MainViewModel) {
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    val galleryItems by viewModel.galleryItems.collectAsState()
    val uiConfigs by viewModel.uiConfigs.collectAsState()

    val filters = remember(galleryItems) {
        val dynamicCategories = galleryItems
            .map { it.category.trim() }
            .filter { it.isNotBlank() && !it.equals("All", ignoreCase = true) }
            .distinct()
            .sorted()
        listOf("All") + dynamicCategories
    }
    
    val screenWidth = LocalConfiguration.current.screenWidthDp

    val subConfig = uiConfigs["gallery_header_sub"]
    val mainConfig = uiConfigs["gallery_header_main"]
    
    val headerSub = subConfig?.value ?: "CURATED"
    val headerMain = mainConfig?.value ?: "Design Gallery"
    val searchPlaceholder = uiConfigs["gallery_search_placeholder"]?.value ?: "SEARCH COLLECTIONS..."

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(
                subTitle = headerSub,
                mainTitle = headerMain,
                subConfig = subConfig,
                mainConfig = mainConfig,
                screenWidth = screenWidth
            )

            // Interactive Search Bar
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusRequester = remember { FocusRequester() }

            val speechRecognizerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
                    if (!spokenText.isNullOrEmpty()) {
                        searchQuery = spokenText
                        selectedFilter = "All"
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Voice Search Button
                Surface(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search collections...")
                        }
                        try {
                            speechRecognizerLauncher.launch(intent)
                        } catch (e: Exception) {
                            // Ignore if not supported
                        }
                    },
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color(0x33333537),
                        focusedContainerColor = Color(0x66333537)
                    ),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(24.dp)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Search",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text Search Field
                Surface(
                    onClick = { 
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    },
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color(0x33333537),
                        focusedContainerColor = Color(0x66333537)
                    ),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(24.dp)),
                    modifier = Modifier
                        .width(280.dp)
                        .height(48.dp)
                        .onFocusChanged { isSearchFocused = it.isFocused }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchFocused) Color.White else Color(0xFF888888),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { 
                                searchQuery = it 
                                if (it.isNotEmpty()) selectedFilter = "All"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                }
                            ),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = searchPlaceholder,
                                        color = Color(0xFF888888),
                                        fontSize = 14.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(35.dp),
            contentPadding = PaddingValues(start = 48.dp, bottom = 32.dp, end = 48.dp),
        ) {
            items(filters) { filterName ->
                FilterChip(
                    text = filterName,
                    selected = selectedFilter == filterName,
                    onClick = { 
                        selectedFilter = filterName
                        searchQuery = "" // Reset search when filtering by category
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        val favoriteIds by viewModel.favoriteIds.collectAsState()

        // Editorial Grid with Filtering Logic - Occupy remaining space
        Box(modifier = Modifier.weight(1f)) {
            GalleryGrid(navController, galleryItems, selectedFilter, searchQuery, favoriteIds)
        }
    }
}


@Composable
fun GalleryGrid(
    navController: NavController, 
    items: List<GalleryItem>, 
    selectedFilter: String,
    searchQuery: String,
    favoriteIds: Set<String>
) {
    val filteredItems = remember(items, selectedFilter, searchQuery) {
        items.filter { item ->
            val matchesFilter = selectedFilter == "All" || item.category.trim() == selectedFilter
            val matchesSearch = searchQuery.isEmpty() || 
                item.title.contains(searchQuery, ignoreCase = true) || 
                item.location.contains(searchQuery, ignoreCase = true) ||
                item.description?.contains(searchQuery, ignoreCase = true) == true
            
            matchesFilter && matchesSearch
        }
    }

    if (filteredItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No gallery items found matching your criteria.", color = Color.Gray, fontSize = 20.sp)
        }
    } else {
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 58.dp, bottom = 40.dp, end = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(filteredItems) { item ->
                val isFavorite = item.id?.let { favoriteIds.contains(it) } ?: false
                GalleryCard(item, 0, isFavorite, onClick = {
                    item.id?.let { id ->
                        navController.navigate(Screen.Detail.createRoute(id, item.imageUrl))
                    }
                })
            }
        }
    }
}

// GalleryItem removed to Models.kt

