package com.example.renovasritv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _galleryItems = MutableStateFlow<List<GalleryItem>>(emptyList())
    val galleryItems: StateFlow<List<GalleryItem>> = _galleryItems
    val projects: StateFlow<List<GalleryItem>> = _galleryItems

    private val _menuBackgrounds = MutableStateFlow<List<MenuBackground>>(emptyList())
    val menuBackgrounds: StateFlow<List<MenuBackground>> = _menuBackgrounds

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds

    private val _uiConfigs = MutableStateFlow<Map<String, UIConfig>>(getDefaultConfigs())
    val uiConfigs: StateFlow<Map<String, UIConfig>> = _uiConfigs

    private val _uiModules = MutableStateFlow<List<UIModule>>(getDefaultModules())
    val uiModules: StateFlow<List<UIModule>> = _uiModules

    private val _homeCurations = MutableStateFlow<List<HomeCuration>>(emptyList())
    val homeCurations: StateFlow<List<HomeCuration>> = _homeCurations

    private val _appTheme = MutableStateFlow<Map<String, ThemeColor>>(emptyMap())
    val appTheme: StateFlow<Map<String, ThemeColor>> = _appTheme

    init {
        fetchGalleryData()
        fetchMenuBackgrounds()
        fetchCategories()
        fetchUIConfigs()
        fetchUIModules()
        fetchHomeCurations()
        fetchAppTheme()
        fetchFavorites()
        setupRealtimeSync()
    }

    private fun getDefaultModules(): List<UIModule> {
        return listOf(
            UIModule(key = "home", label = "Home", icon = "Home", orderIndex = 0),
            UIModule(key = "gallery", label = "Gallery", icon = "Architecture", orderIndex = 1),
            UIModule(key = "favorites", label = "Favorites", icon = "Favorite", orderIndex = 2),
            UIModule(key = "consultation", label = "Consultation", icon = "SupportAgent", orderIndex = 3)
        )
    }

    private fun getDefaultConfigs(): Map<String, UIConfig> {
        return listOf(
            UIConfig(key = "AREA_top_bar", value = "true", type = "visibility", isActive = true),
            UIConfig(key = "AREA_hero_section", value = "true", type = "visibility", isActive = true),
            UIConfig(key = "AREA_categories_section", value = "true", type = "visibility", isActive = true),
            UIConfig(key = "sidebar_logo_text", value = "RENOVASRI", type = "text", isActive = true)
        ).associateBy { it.key }
    }

    fun isModuleVisible(key: String): Boolean {
        // Gunakan extension function dari Models.kt yang mendukung rekursi parent_key
        return _uiConfigs.value.isModuleVisible(key)
    }

    fun getPageConfigs(pageId: String): List<UIConfig> {
        return _uiConfigs.value.getConfigsByPage(pageId)
    }

    private fun setupRealtimeSync() {
        viewModelScope.launch {
            try {
                val moduleChannel = SupabaseConfig.client.channel("ui_modules_realtime")
                moduleChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "ui_modules"
                }.onEach { fetchUIModules() }.launchIn(viewModelScope)
                moduleChannel.subscribe()

                val uiChannel = SupabaseConfig.client.channel("ui_config_realtime")
                uiChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "ui_config"
                }.onEach { fetchUIConfigs() }.launchIn(viewModelScope)
                uiChannel.subscribe()

                val curationChannel = SupabaseConfig.client.channel("home_curations_realtime")
                curationChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "home_curations"
                }.onEach { fetchHomeCurations() }.launchIn(viewModelScope)
                curationChannel.subscribe()
            } catch (e: Exception) {
                println("Supabase Realtime Error: ${e.message}")
            }
        }
    }

    fun fetchHomeCurations() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("home_curations").select().decodeList<HomeCuration>()
                if (result.isEmpty()) {
                    println("DEBUG_LOG: Home Curations - DB returned EMPTY. Applying safety fallback.")
                    // Aggressive fallback with multiple items to ensure LazyRow renders
                    _homeCurations.value = listOf(
                        HomeCuration(id = 1L, imageUrl = "https://xbeslcqosyhyuyxztpov.supabase.co/storage/v1/object/public/media/renovasri-export-1771905706286.jpg", caption = "Modern Living Room", orderIndex = 1, targetId = "550e8400-e29b-41d4-a716-446655440000"),
                        HomeCuration(id = 2L, imageUrl = "https://xbeslcqosyhyuyxztpov.supabase.co/storage/v1/object/public/media/put-together-a-perfect-guest-room-1976987-hero-223e3e8f697e4b13b62ad4fe898d492d.jpg", caption = "Scandinavian Loft", orderIndex = 2, targetId = "550e8400-e29b-41d4-a716-446655440001"),
                        HomeCuration(id = 3L, imageUrl = "https://xbeslcqosyhyuyxztpov.supabase.co/storage/v1/object/public/media/9.jpeg", caption = "Industrial Workspace", orderIndex = 3, targetId = "550e8400-e29b-41d4-a716-446655440002")
                    )
                } else {
                    _homeCurations.value = result.sortedBy { it.orderIndex }
                    println("DEBUG_LOG: Home Curations - Successfully loaded ${result.size} items from DB")
                    result.forEach { 
                        println("DEBUG_LOG: Item ID: ${it.id}, URL: ${it.imageUrl}, Target: ${it.targetId}, Active: ${it.isActive}")
                    }
                }
            } catch (e: Exception) {
                println("DEBUG_LOG: Home Curations ERROR - ${e.message}")
                // Fallback on error too
                _homeCurations.value = listOf(
                    HomeCuration(id = 1L, imageUrl = "https://xbeslcqosyhyuyxztpov.supabase.co/storage/v1/object/public/media/renovasri-export-1771905706286.jpg", caption = "Modern Living Room", orderIndex = 1, targetId = "550e8400-e29b-41d4-a716-446655440000")
                )
            }
        }
    }

    fun fetchUIConfigs() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("ui_config").select().decodeList<UIConfig>()
                // Always start with defaults, then overwrite with DB values
                val merged = getDefaultConfigs().toMutableMap()
                result.forEach { config ->
                    merged[config.key] = config
                }
                _uiConfigs.value = merged
                println("UI Configs updated: ${merged.size} items")
            } catch (e: Exception) {
                println("Supabase UIConfigs Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun fetchUIModules() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("ui_modules").select().decodeList<UIModule>()
                if (result.isNotEmpty()) {
                    // Only use DB modules if they are marked active
                    val active = result.filter { it.isActive }.sortedBy { it.orderIndex }
                    if (active.isNotEmpty()) {
                        _uiModules.value = active
                    }
                }
            } catch (e: Exception) {
                println("Supabase UIModules Error: ${e.message}")
            }
        }
    }

    fun fetchGalleryData() {
        viewModelScope.launch {
            try {
                println("DEBUG_LOG: Fetching gallery data from ${SupabaseConfig.SUPABASE_URL}")
                val result = SupabaseConfig.client.from("gallery_items").select().decodeList<GalleryItem>()
                if (result.isEmpty()) {
                    println("DEBUG_LOG: Gallery - DB returned EMPTY. Applying safety fallback.")
                    _galleryItems.value = getFallbackGalleryItems()
                } else {
                    _galleryItems.value = result
                    println("DEBUG_LOG: Gallery - Successfully loaded ${result.size} items")
                }
            } catch (e: Exception) {
                println("DEBUG_LOG: Gallery ERROR - ${e.message}")
                e.printStackTrace()
                _galleryItems.value = getFallbackGalleryItems()
            }
        }
    }

    private fun getFallbackGalleryItems(): List<GalleryItem> {
        return listOf(
            GalleryItem(id = "550e8400-e29b-41d4-a716-446655440000", title = "Modern Glass House", location = "Jakarta", imageUrl = "https://xbeslcqosyhyuyxztpov.supabase.co/storage/v1/object/public/media/renovasri-export-1771905706286.jpg", description = "A stunning modern glass house with open concepts.", category = "Modern"),
            GalleryItem(id = "550e8400-e29b-41d4-a716-446655440001", title = "Scandinavian Loft", location = "Bandung", imageUrl = "https://xbeslcqosyhyuyxztpov.supabase.co/storage/v1/object/public/media/put-together-a-perfect-guest-room-1976987-hero-223e3e8f697e4b13b62ad4fe898d492d.jpg", description = "Clean lines and minimalist design in this cozy loft.", category = "Scandinavian"),
            GalleryItem(id = "550e8400-e29b-41d4-a716-446655440002", title = "Industrial Warehouse", location = "Surabaya", imageUrl = "https://xbeslcqosyhyuyxztpov.supabase.co/storage/v1/object/public/media/9.jpeg", description = "Raw materials and open spaces define this unique home.", category = "Industrial")
        )
    }

    fun fetchMenuBackgrounds() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("menu_backgrounds").select().decodeList<MenuBackground>()
                _menuBackgrounds.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchCategories() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("categories").select().decodeList<Category>()
                _categories.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchAppTheme() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("active_theme_view").select().decodeList<ThemeColor>()
                _appTheme.value = result.associateBy { it.key }
            } catch (e: Exception) {
                println("Supabase AppTheme Error: ${e.message}")
            }
        }
    }

    fun fetchFavorites() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("favorites").select().decodeList<Favorite>()
                _favoriteIds.value = result.map { it.itemId }.toSet()
            } catch (e: Exception) {
                println("Supabase Favorites Error: ${e.message}")
            }
        }
    }

    fun toggleFavorite(itemId: String) {
        viewModelScope.launch {
            val current = _favoriteIds.value
            try {
                if (current.contains(itemId)) {
                    SupabaseConfig.client.from("favorites").delete { 
                        filter { eq("item_id", itemId) } 
                    }
                    _favoriteIds.value = current - itemId
                    println("DEBUG_LOG: Removed $itemId from favorites")
                } else {
                    SupabaseConfig.client.from("favorites").insert(Favorite(itemId = itemId))
                    _favoriteIds.value = current + itemId
                    println("DEBUG_LOG: Added $itemId to favorites")
                }
            } catch (e: Exception) {
                println("DEBUG_LOG: Supabase Toggle Favorite Error: ${e.message}")
            }
        }
    }

    fun incrementViews(itemId: String) {
        viewModelScope.launch {
            try {
                val item = _galleryItems.value.find { it.id == itemId }
                if (item != null) {
                    val newViews = item.views + 1
                    
                    // Optimistic update
                    _galleryItems.value = _galleryItems.value.map {
                        if (it.id == itemId) it.copy(views = newViews) else it
                    }

                    // Try updating DB, but catch if column is missing (PGRST204)
                    try {
                        SupabaseConfig.client.from("gallery_items").update(mapOf("views" to newViews)) {
                            filter { eq("id", itemId) }
                        }
                    } catch (e: Exception) {
                        println("DEBUG_LOG: DB incrementViews failed (missing column?): ${e.message}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
