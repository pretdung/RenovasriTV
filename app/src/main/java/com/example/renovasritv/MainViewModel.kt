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

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds

    private val _uiConfigs = MutableStateFlow<Map<String, UIConfig>>(getDefaultConfigs())
    val uiConfigs: StateFlow<Map<String, UIConfig>> = _uiConfigs

    private val _uiModules = MutableStateFlow<List<UIModule>>(getDefaultModules())
    val uiModules: StateFlow<List<UIModule>> = _uiModules

    private val _randomGalleryItems = MutableStateFlow<List<GalleryItem>>(emptyList())
    val randomGalleryItems: StateFlow<List<GalleryItem>> = _randomGalleryItems

    private val _appTheme = MutableStateFlow<Map<String, ThemeColor>>(emptyMap())
    val appTheme: StateFlow<Map<String, ThemeColor>> = _appTheme

    // --- CALCULATION ENGINE STATES (FASE 1) ---
    private val _calcCategories = MutableStateFlow<List<CalcCategory>>(emptyList())
    val calcCategories: StateFlow<List<CalcCategory>> = _calcCategories

    private val _calcVariables = MutableStateFlow<List<CalcVariable>>(emptyList())
    val calcVariables: StateFlow<List<CalcVariable>> = _calcVariables

    private val _calcFormulas = MutableStateFlow<List<CalcFormula>>(emptyList())
    val calcFormulas: StateFlow<List<CalcFormula>> = _calcFormulas

    private val _calcMaterials = MutableStateFlow<List<CalcMaterial>>(emptyList())
    val calcMaterials: StateFlow<List<CalcMaterial>> = _calcMaterials

    private val _calcFormulaVariables = MutableStateFlow<List<CalcFormulaVariable>>(emptyList())
    val calcFormulaVariables: StateFlow<List<CalcFormulaVariable>> = _calcFormulaVariables

    private val _calcSystemConfigs = MutableStateFlow<Map<String, CalcSystemConfig>>(emptyMap())
    val calcSystemConfigs: StateFlow<Map<String, CalcSystemConfig>> = _calcSystemConfigs

    init {
        fetchGalleryData()
        fetchMenuBackgrounds()
        fetchUIConfigs()
        fetchUIModules()
        fetchAppTheme()
        fetchFavorites()
        
        // Fetch Calculator Data
        fetchCalculatorData()
        
        setupRealtimeSync()
    }

    private fun getDefaultModules(): List<UIModule> {
        return listOf(
            UIModule(key = "home", label = "Home", icon = "Home", orderIndex = 0),
            UIModule(key = "gallery", label = "Gallery", icon = "Architecture", orderIndex = 1),
            UIModule(key = "favorites", label = "Favorites", icon = "Favorite", orderIndex = 2),
            UIModule(key = "calculator", label = "Calculator", icon = "Calculate", orderIndex = 3),
            UIModule(key = "consultation", label = "Consultation", icon = "SupportAgent", orderIndex = 4)
        )
    }

    private fun getDefaultConfigs(): Map<String, UIConfig> {
        return listOf(
            UIConfig(key = "AREA_top_bar", value = "true", type = "visibility", isActive = true),
            UIConfig(key = "AREA_hero_section", value = "true", type = "visibility", isActive = true),
            UIConfig(key = "AREA_categories_section", value = "true", type = "visibility", isActive = true),
            UIConfig(key = "sidebar_logo_text", value = "RENOVASRI", type = "text", isActive = true),
            UIConfig(key = "page_calculator", value = "true", type = "visibility", isActive = true),
            
            // Calculator UI Configs
            UIConfig(key = "calc_step_label", value = "LANGKAH", type = "text", pageId = "page_calculator", fontSize = "caption", fontWeight = "bold", fontColor = "#FFB59E"),
            UIConfig(key = "calc_title", value = "Kalkulator Renovasi", type = "text", pageId = "page_calculator", fontSize = "headline", fontWeight = "extrabold", fontColor = "#FFFFFF"),
            UIConfig(key = "calc_item_title", value = "Item Title", type = "style", pageId = "page_calculator", fontSize = "subtitle", fontWeight = "bold", fontColor = "#FFFFFF"),
            UIConfig(key = "calc_item_desc", value = "Item Desc", type = "style", pageId = "page_calculator", fontSize = "body", fontWeight = "normal", fontColor = "#AAAAAA")
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
            } catch (e: Exception) {
                println("Supabase Realtime Error: ${e.message}")
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
                // Merge logic: use defaults as base, update/add from DB
                val merged = getDefaultModules().associateBy { it.key }.toMutableMap()
                result.forEach { module ->
                    merged[module.key] = module
                }
                _uiModules.value = merged.values.filter { it.isActive }.sortedBy { it.orderIndex }
                println("UI Modules updated: ${_uiModules.value.size} active modules")
            } catch (e: Exception) {
                println("Supabase UIModules Error: ${e.message}")
                // Fallback to defaults on error
                _uiModules.value = getDefaultModules().filter { it.isActive }.sortedBy { it.orderIndex }
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
                    val fallback = getFallbackGalleryItems()
                    _galleryItems.value = fallback
                    _randomGalleryItems.value = fallback.shuffled().take(5)
                } else {
                    _galleryItems.value = result
                    // Pick 5 random featured items for Home
                    val featured = result.filter { it.isFeatured }
                    val displayItems = if (featured.isNotEmpty()) featured else result
                    _randomGalleryItems.value = displayItems.shuffled().take(5)
                    println("DEBUG_LOG: Gallery - Loaded ${result.size} items. Random Home: ${_randomGalleryItems.value.size}")
                }
            } catch (e: Exception) {
                println("DEBUG_LOG: Gallery ERROR - ${e.message}")
                e.printStackTrace()
                val fallback = getFallbackGalleryItems()
                _galleryItems.value = fallback
                _randomGalleryItems.value = fallback.shuffled().take(5)
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

    // --- CALCULATION ENGINE FETCHERS (FASE 1) ---

    fun fetchCalculatorData() {
        fetchCalcCategories()
        fetchCalcVariables()
        fetchCalcFormulas()
        fetchCalcMaterials()
        fetchCalcFormulaVariables()
        fetchCalcSystemConfigs()
    }

    private fun fetchCalcCategories() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("calc_categories").select().decodeList<CalcCategory>()
                if (result.isEmpty()) {
                    _calcCategories.value = getFallbackCalcCategories()
                } else {
                    _calcCategories.value = result.sortedBy { it.orderIndex }
                }
            } catch (e: Exception) {
                println("Supabase CalcCategories Error: ${e.message}")
                _calcCategories.value = getFallbackCalcCategories()
            }
        }
    }

    private fun getFallbackCalcCategories(): List<CalcCategory> {
        return listOf(
            CalcCategory(id = "cat_lantai", name = "Pemasangan Lantai", description = "Hitung kebutuhan ubin atau parket", orderIndex = 0, isCalculable = true),
            CalcCategory(id = "cat_dinding", name = "Pengecatan Dinding", description = "Hitung kebutuhan cat interior/eksterior", orderIndex = 1, isCalculable = true),
            CalcCategory(id = "cat_plafon", name = "Plafon Gypsum", description = "Hitung kebutuhan papan gypsum dan rangka", orderIndex = 2, isCalculable = true)
        )
    }

    private fun fetchCalcVariables() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("calc_variables").select().decodeList<CalcVariable>()
                if (result.isEmpty()) {
                    _calcVariables.value = getFallbackCalcVariables()
                } else {
                    _calcVariables.value = result
                }
            } catch (e: Exception) {
                println("Supabase CalcVariables Error: ${e.message}")
                _calcVariables.value = getFallbackCalcVariables()
            }
        }
    }

    private fun getFallbackCalcVariables(): List<CalcVariable> {
        return listOf(
            CalcVariable(id = "var_p", variableKey = "P", label = "Panjang", unit = "m", minValue = 0f, maxValue = 100f, step = 0.5f),
            CalcVariable(id = "var_l", variableKey = "L", label = "Lebar", unit = "m", minValue = 0f, maxValue = 100f, step = 0.5f),
            CalcVariable(id = "var_t", variableKey = "T", label = "Tinggi", unit = "m", minValue = 0f, maxValue = 10f, step = 0.1f)
        )
    }

    private fun fetchCalcFormulas() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("calc_formulas").select().decodeList<CalcFormula>()
                if (result.isEmpty()) {
                    _calcFormulas.value = getFallbackCalcFormulas()
                } else {
                    _calcFormulas.value = result.filter { it.isActive }
                }
            } catch (e: Exception) {
                println("Supabase CalcFormulas Error: ${e.message}")
                _calcFormulas.value = getFallbackCalcFormulas()
            }
        }
    }

    private fun getFallbackCalcFormulas(): List<CalcFormula> {
        return listOf(
            CalcFormula(id = "form_lantai", categoryId = "cat_lantai", formulaName = "Luas Lantai", expression = "P * L"),
            CalcFormula(id = "form_dinding", categoryId = "cat_dinding", formulaName = "Luas Dinding", expression = "2 * (P + L) * T"),
            CalcFormula(id = "form_plafon", categoryId = "cat_plafon", formulaName = "Luas Plafon", expression = "P * L")
        )
    }

    private fun fetchCalcMaterials() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("calc_materials").select().decodeList<CalcMaterial>()
                if (result.isEmpty()) {
                    _calcMaterials.value = getFallbackCalcMaterials()
                } else {
                    _calcMaterials.value = result.filter { it.isActive }
                }
            } catch (e: Exception) {
                println("Supabase CalcMaterials Error: ${e.message}")
                _calcMaterials.value = getFallbackCalcMaterials()
            }
        }
    }

    private fun getFallbackCalcMaterials(): List<CalcMaterial> {
        return listOf(
            CalcMaterial(id = "mat_granit", categoryId = "cat_lantai", name = "Granite 60x60", unitType = "m2", basePrice = 250000f),
            CalcMaterial(id = "mat_cat_premium", categoryId = "cat_dinding", name = "Cat Premium", unitType = "m2", basePrice = 45000f),
            CalcMaterial(id = "mat_gypsum", categoryId = "cat_plafon", name = "Gypsum Board 9mm", unitType = "m2", basePrice = 85000f)
        )
    }

    private fun fetchCalcFormulaVariables() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("calc_formula_variables").select().decodeList<CalcFormulaVariable>()
                if (result.isEmpty()) {
                    _calcFormulaVariables.value = getFallbackCalcFormulaVariables()
                } else {
                    _calcFormulaVariables.value = result.sortedBy { it.orderIndex }
                }
            } catch (e: Exception) {
                println("Supabase CalcFormulaVariables Error: ${e.message}")
                _calcFormulaVariables.value = getFallbackCalcFormulaVariables()
            }
        }
    }

    private fun getFallbackCalcFormulaVariables(): List<CalcFormulaVariable> {
        return listOf(
            CalcFormulaVariable(id = "fv1", formulaId = "form_lantai", variableId = "var_p", orderIndex = 0),
            CalcFormulaVariable(id = "fv2", formulaId = "form_lantai", variableId = "var_l", orderIndex = 1),
            CalcFormulaVariable(id = "fv3", formulaId = "form_dinding", variableId = "var_p", orderIndex = 0),
            CalcFormulaVariable(id = "fv4", formulaId = "form_dinding", variableId = "var_l", orderIndex = 1),
            CalcFormulaVariable(id = "fv5", formulaId = "form_dinding", variableId = "var_t", orderIndex = 2)
        )
    }

    private fun fetchCalcSystemConfigs() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("calc_system_configs").select().decodeList<CalcSystemConfig>()
                _calcSystemConfigs.value = result.associateBy { it.key }
            } catch (e: Exception) {
                println("Supabase CalcSystemConfigs Error: ${e.message}")
            }
        }
    }
}
