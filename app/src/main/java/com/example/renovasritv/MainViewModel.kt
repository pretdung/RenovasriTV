package com.example.renovasritv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _projects = MutableStateFlow<List<ProjectItem>>(emptyList())
    val projects: StateFlow<List<ProjectItem>> = _projects

    private val _galleryItems = MutableStateFlow<List<GalleryItem>>(emptyList())
    val galleryItems: StateFlow<List<GalleryItem>> = _galleryItems

    private val _menuBackgrounds = MutableStateFlow<List<MenuBackground>>(emptyList())
    val menuBackgrounds: StateFlow<List<MenuBackground>> = _menuBackgrounds

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds

    private val _uiConfigs = MutableStateFlow<Map<String, UIConfig>>(emptyMap())
    val uiConfigs: StateFlow<Map<String, UIConfig>> = _uiConfigs

    private val _appTheme = MutableStateFlow<Map<String, ThemeColor>>(emptyMap())
    val appTheme: StateFlow<Map<String, ThemeColor>> = _appTheme

    val trendingItems = _galleryItems.map { items ->
        items.filter { it.isTrending }.sortedByDescending { it.views }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        fetchHomeData()
        fetchGalleryData()
        fetchMenuBackgrounds()
        fetchCategories()
        fetchUIConfigs()
        fetchAppTheme()
        fetchFavorites()
        setupRealtimeSync()
    }

    private fun setupRealtimeSync() {
        viewModelScope.launch {
            try {
                // Realtime for ui_config
                val uiChannel = SupabaseConfig.client.channel("ui_config_realtime")
                uiChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "ui_config"
                }.onEach { fetchUIConfigs() }.launchIn(viewModelScope)
                uiChannel.subscribe()

                // Realtime for theme switching
                val themeChannel = SupabaseConfig.client.channel("theme_realtime")
                
                // Listen for changes in theme definitions (switching active theme)
                themeChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "theme_definitions"
                }.onEach { fetchAppTheme() }.launchIn(viewModelScope)

                // Listen for changes in colors of the active theme
                themeChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "theme_colors"
                }.onEach { fetchAppTheme() }.launchIn(viewModelScope)
                
                themeChannel.subscribe()
                
            } catch (e: Exception) {
                println("Supabase Realtime Error: ${e.message}")
            }
        }
    }

    fun fetchAppTheme() {
        viewModelScope.launch {
            try {
                // Fetch from the active_theme_view which always returns the currently active palette
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

    fun fetchUIConfigs() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("ui_config").select().decodeList<UIConfig>()
                println("Supabase Debug: Fetched ${result.size} configs")
                _uiConfigs.value = result.associateBy { it.key }
            } catch (e: Exception) {
                println("Supabase Debug Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun fetchHomeData() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("projects").select().decodeList<ProjectItem>()
                _projects.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchGalleryData() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("gallery_items").select().decodeList<GalleryItem>()
                _galleryItems.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

    fun toggleFavorite(itemId: Long) {
        viewModelScope.launch {
            val current = _favoriteIds.value
            try {
                if (current.contains(itemId)) {
                    SupabaseConfig.client.from("favorites").delete {
                        filter {
                            eq("item_id", itemId)
                        }
                    }
                    _favoriteIds.value = current - itemId
                } else {
                    SupabaseConfig.client.from("favorites").insert(Favorite(itemId = itemId))
                    _favoriteIds.value = current + itemId
                }
            } catch (e: Exception) {
                println("Supabase Toggle Favorite Error: ${e.message}")
            }
        }
    }

    fun incrementViews(itemId: Long) {
        viewModelScope.launch {
            try {
                // Fetch current item to get views
                val item = _galleryItems.value.find { it.id == itemId }
                if (item != null) {
                    val newViews = item.views + 1
                    SupabaseConfig.client.from("gallery_items").update(mapOf("views" to newViews)) {
                        filter {
                            eq("id", itemId)
                        }
                    }
                    // Update local state for immediate UI feedback (optional)
                    _galleryItems.value = _galleryItems.value.map {
                        if (it.id == itemId) it.copy(views = newViews) else it
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
