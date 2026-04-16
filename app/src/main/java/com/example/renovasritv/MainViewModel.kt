package com.example.renovasritv

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.renovasritv.ai.ClaudeOrchestrator
import com.example.renovasritv.ai.GeminiArtist
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val estimationFile = File(getApplication<Application>().filesDir, "estimations_cache.json")
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

    private val _providerConfigs = MutableStateFlow<List<ProviderConfig>>(emptyList())
    val providerConfigs: StateFlow<List<ProviderConfig>> = _providerConfigs

    private val _securityPin = MutableStateFlow<String?>(null)
    val securityPin: StateFlow<String?> = _securityPin

    private val claudeOrchestrator by lazy { ClaudeOrchestrator(this) }
    private val geminiArtist by lazy { GeminiArtist(this) }
    
    private val _aiImagePrompt = MutableStateFlow<String?>(null)
    val aiImagePrompt: StateFlow<String?> = _aiImagePrompt

    private val _aiVisualizationUrl = MutableStateFlow<String?>(null)
    val aiVisualizationUrl: StateFlow<String?> = _aiVisualizationUrl

    // --- CALCULATION ENGINE STATES (FASE 1) ---
    private val _calcCategories = MutableStateFlow<List<CalcCategory>>(emptyList())
    val calcCategories: StateFlow<List<CalcCategory>> = _calcCategories

    private val _calcLaborCategories = MutableStateFlow<List<CalcLaborCategory>>(emptyList())
    val calcLaborCategories: StateFlow<List<CalcLaborCategory>> = _calcLaborCategories

    private val _calcVariables = MutableStateFlow<List<CalcVariable>>(emptyList())
    val calcVariables: StateFlow<List<CalcVariable>> = _calcVariables

    private val _calcFormulas = MutableStateFlow<List<CalcFormula>>(emptyList())
    val calcFormulas: StateFlow<List<CalcFormula>> = _calcFormulas

    private val _calcMaterials = MutableStateFlow<List<CalcMaterial>>(emptyList())
    val calcMaterials: StateFlow<List<CalcMaterial>> = _calcMaterials

    private val _calcFormulaVariables = MutableStateFlow<List<CalcFormulaVariable>>(emptyList())
    val calcFormulaVariables: StateFlow<List<CalcFormulaVariable>> = _calcFormulaVariables

    private val _laborConfigs = MutableStateFlow<List<LaborConfig>>(emptyList())
    val laborConfigs: StateFlow<List<LaborConfig>> = _laborConfigs

    private val _calcSystemConfigs = MutableStateFlow<Map<String, CalcSystemConfig>>(getDefaultCalcSystemConfigs())
    val calcSystemConfigs: StateFlow<Map<String, CalcSystemConfig>> = _calcSystemConfigs

    private val _estimations = MutableStateFlow<List<CalcEstimation>>(emptyList())
    val estimations: StateFlow<List<CalcEstimation>> = _estimations

    init {
        loadEstimationsFromLocal()
        fetchGalleryData()
        fetchMenuBackgrounds()
        fetchUIConfigs()
        fetchUIModules()
        fetchAppTheme()
        fetchFavorites()
        
        // Fetch Calculator Data
        fetchCalculatorData()
        fetchProviderConfigs()
        
        setupRealtimeSync()
    }

    fun setSecurityPin(pin: String) {
        _securityPin.value = pin
    }

    fun fetchProviderConfigs() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("provider_configs").select().decodeList<ProviderConfig>()
                _providerConfigs.value = result
            } catch (e: Exception) {
                println("Supabase ProviderConfigs Error: ${e.message}")
            }
        }
    }

    fun saveProviderConfig(provider: String, apiKey: String) {
        val pin = _securityPin.value
        if (pin == null) {
            _saveStatus.value = SaveStatus.Error("Security PIN required for encryption")
            return
        }

        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Loading()
            try {
                val encryptedKey = SecurityUtils.encrypt(apiKey, pin)
                val user = SupabaseConfig.client.auth.currentUserOrNull()
                val config = ProviderConfig(
                    userId = user?.id,
                    providerName = provider,
                    apiKeyEncrypted = encryptedKey,
                    isActive = true
                )
                SupabaseConfig.client.from("provider_configs").insert(config)
                fetchProviderConfigs()
                _saveStatus.value = SaveStatus.Success("API Key for $provider secured and saved!")
            } catch (e: Exception) {
                _saveStatus.value = SaveStatus.Error("Encryption failed: ${e.message}")
            }
        }
    }

    /**
     * Retrieves and decrypts the API key for a specific provider.
     * Required for Phase 4 & 5.
     */
    fun getDecryptedKey(provider: String): String? {
        val pin = _securityPin.value ?: return null
        val config = _providerConfigs.value.find { it.providerName == provider && it.isActive } ?: return null
        return try {
            SecurityUtils.decrypt(config.apiKeyEncrypted, pin)
        } catch (e: Exception) {
            null
        }
    }

    private fun getDefaultModules(): List<UIModule> {
        return listOf(
            UIModule(key = "home", label = "Home", icon = "Home", orderIndex = 0),
            UIModule(key = "gallery", label = "Gallery", icon = "Architecture", orderIndex = 1),
            UIModule(key = "favorites", label = "Favorites", icon = "Favorite", orderIndex = 2),
            UIModule(key = "calculator", label = "Calculator", icon = "Calculate", orderIndex = 3),
            UIModule(key = "history", label = "History", icon = "History", orderIndex = 4),
            UIModule(key = "consultation", label = "Consultation", icon = "SupportAgent", orderIndex = 5)
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
            UIConfig(key = "calc_item_desc", value = "Item Desc", type = "style", pageId = "page_calculator", fontSize = "body", fontWeight = "normal", fontColor = "#AAAAAA"),
            UIConfig(key = "calc_input_label", value = "Input Label", type = "style", pageId = "page_calculator", fontSize = "body", fontWeight = "bold", fontColor = "#AAAAAA"),
            UIConfig(key = "calc_input_value", value = "Input Value", type = "style", pageId = "page_calculator", fontSize = "title", fontWeight = "black", fontColor = "#FFFFFF")
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
        fetchCalcLaborCategories()
        fetchCalcVariables()
        fetchCalcFormulas()
        fetchCalcMaterials()
        fetchCalcFormulaVariables()
        fetchLaborConfigs()
        fetchCalcSystemConfigs()
        fetchEstimations()
    }

    private fun loadEstimationsFromLocal() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (estimationFile.exists()) {
                    val json = estimationFile.readText()
                    val localList = Json.decodeFromString<List<CalcEstimation>>(json)
                    _estimations.update { current ->
                        (current + localList).distinctBy { it.id ?: it.createdAt }.sortedByDescending { it.createdAt }
                    }
                    println("DEBUG_LOG: Loaded ${localList.size} estimations from local storage")
                }
            } catch (e: Exception) {
                println("DEBUG_LOG: Error loading local estimations: ${e.message}")
            }
        }
    }

    private fun saveEstimationsToLocal(list: List<CalcEstimation>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = Json.encodeToString(list)
                estimationFile.writeText(json)
                println("DEBUG_LOG: Saved ${list.size} estimations to local storage")
            } catch (e: Exception) {
                println("DEBUG_LOG: Error saving local estimations: ${e.message}")
            }
        }
    }

    fun fetchEstimations() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("calc_estimations").select().decodeList<CalcEstimation>()
                _estimations.update { current ->
                    (current + result).distinctBy { it.id ?: it.createdAt }.sortedByDescending { it.createdAt }
                }
                saveEstimationsToLocal(_estimations.value)
                println("DEBUG_LOG: Fetched ${result.size} estimations from DB")
            } catch (e: Exception) {
                println("DEBUG_LOG: Error fetching estimations: ${e.message}")
            }
        }
    }

    private fun fetchCalcLaborCategories() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("calc_labor_categories").select().decodeList<CalcLaborCategory>()
                if (result.isEmpty()) {
                    _calcLaborCategories.value = getFallbackLaborCategories()
                } else {
                    _calcLaborCategories.value = result.sortedBy { it.orderIndex }
                }
            } catch (e: Exception) {
                println("Supabase CalcLaborCategories Error: ${e.message}")
                _calcLaborCategories.value = getFallbackLaborCategories()
            }
        }
    }

    private fun getFallbackLaborCategories(): List<CalcLaborCategory> {
        return listOf(
            CalcLaborCategory("labor", "Tenaga Kerja", "Biaya harian tukang ahli dan pembantu", "Groups", 0, true),
            CalcLaborCategory("preparation", "Persiapan", "Pembersihan lahan dan pembongkaran", "Construction", 1, false),
            CalcLaborCategory("mep", "Listrik & Air", "Instalasi titik lampu dan pipa", "ElectricalServices", 2, false),
            CalcLaborCategory("contingency", "Dana Darurat", "Biaya tak terduga (10% disarankan)", "ReportProblem", 3, true)
        )
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
            CalcVariable(id = "var_p", variableKey = "P", label = "Panjang", unit = "m", minValue = 0f, maxValue = 100f, step = 0.1f),
            CalcVariable(id = "var_l", variableKey = "L", label = "Lebar", unit = "m", minValue = 0f, maxValue = 100f, step = 0.1f),
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
            // LANTAI (10 items)
            CalcMaterial(id = "mat_l1", categoryId = "cat_lantai", surfaceType = "Lantai", name = "Granite 60x60 Premium", unitType = "m2", basePrice = 250000f),
            CalcMaterial(id = "mat_l2", categoryId = "cat_lantai", surfaceType = "Lantai", name = "Granite 60x60 Standard", unitType = "m2", basePrice = 180000f),
            CalcMaterial(id = "mat_l3", categoryId = "cat_lantai", surfaceType = "Lantai", name = "Keramik 40x40 Putih", unitType = "m2", basePrice = 85000f),
            CalcMaterial(id = "mat_l4", categoryId = "cat_lantai", surfaceType = "Lantai", name = "Keramik 50x50 Motif", unitType = "m2", basePrice = 110000f),
            CalcMaterial(id = "mat_l5", categoryId = "cat_lantai", surfaceType = "Lantai", name = "Parket Kayu Jati", unitType = "m2", basePrice = 450000f),
            CalcMaterial(id = "mat_l6", categoryId = "cat_lantai", surfaceType = "Lantai", name = "Parket Kayu Oak", unitType = "m2", basePrice = 380000f),
            CalcMaterial(id = "mat_l7", categoryId = "cat_lantai", surfaceType = "Lantai", name = "Vinyl Flooring 3mm", unitType = "m2", basePrice = 150000f),
            CalcMaterial(id = "mat_l8", categoryId = "cat_lantai", surfaceType = "Lantai", name = "Vinyl Click System", unitType = "m2", basePrice = 220000f),
            CalcMaterial(id = "mat_l9", categoryId = "cat_lantai", surfaceType = "Lantai", name = "Marmer Statuario", unitType = "m2", basePrice = 1200000f),
            CalcMaterial(id = "mat_l10", categoryId = "cat_lantai", surfaceType = "Lantai", name = "Marmer Local Lampung", unitType = "m2", basePrice = 650000f),

            // DINDING (10 items)
            CalcMaterial(id = "mat_d1", categoryId = "cat_dinding", surfaceType = "Dinding", name = "Cat Interior Premium", unitType = "m2", basePrice = 45000f),
            CalcMaterial(id = "mat_d2", categoryId = "cat_dinding", surfaceType = "Dinding", name = "Cat Interior Standard", unitType = "m2", basePrice = 25000f),
            CalcMaterial(id = "mat_d3", categoryId = "cat_dinding", surfaceType = "Dinding", name = "Wallpaper Vinyl Motif", unitType = "m2", basePrice = 65000f),
            CalcMaterial(id = "mat_d4", categoryId = "cat_dinding", surfaceType = "Dinding", name = "Wallpaper Foam 3D", unitType = "m2", basePrice = 35000f),
            CalcMaterial(id = "mat_d5", categoryId = "cat_dinding", surfaceType = "Dinding", name = "Cat Eksterior Weatherproof", unitType = "m2", basePrice = 85000f),
            CalcMaterial(id = "mat_d6", categoryId = "cat_dinding", surfaceType = "Dinding", name = "Batu Alam Candi", unitType = "m2", basePrice = 175000f),
            CalcMaterial(id = "mat_d7", categoryId = "cat_dinding", surfaceType = "Dinding", name = "Batu Alam Palimanan", unitType = "m2", basePrice = 195000f),
            CalcMaterial(id = "mat_d8", categoryId = "cat_dinding", surfaceType = "Dinding", name = "Wood Panel Wall", unitType = "m2", basePrice = 320000f),
            CalcMaterial(id = "mat_d9", categoryId = "cat_dinding", surfaceType = "Dinding", name = "Keramik Dinding 25x40", unitType = "m2", basePrice = 95000f),
            CalcMaterial(id = "mat_d10", categoryId = "cat_dinding", surfaceType = "Dinding", name = "WPC Wall Panel", unitType = "m2", basePrice = 145000f),

            // PLAFON (10 items)
            CalcMaterial(id = "mat_p1", categoryId = "cat_plafon", surfaceType = "Plafon", name = "Gypsum Board 9mm Elephant", unitType = "m2", basePrice = 85000f),
            CalcMaterial(id = "mat_p2", categoryId = "cat_plafon", surfaceType = "Plafon", name = "Gypsum Board 9mm Knauf", unitType = "m2", basePrice = 82000f),
            CalcMaterial(id = "mat_p3", categoryId = "cat_plafon", surfaceType = "Plafon", name = "Plafon PVC High Gloss", unitType = "m2", basePrice = 125000f),
            CalcMaterial(id = "mat_p4", categoryId = "cat_plafon", surfaceType = "Plafon", name = "Plafon PVC Wood Grain", unitType = "m2", basePrice = 135000f),
            CalcMaterial(id = "mat_p5", categoryId = "cat_plafon", surfaceType = "Plafon", name = "GRC Board 4mm", unitType = "m2", basePrice = 65000f),
            CalcMaterial(id = "mat_p6", categoryId = "cat_plafon", surfaceType = "Plafon", name = "Plafon Akustik 60x60", unitType = "m2", basePrice = 155000f),
            CalcMaterial(id = "mat_p7", categoryId = "cat_plafon", surfaceType = "Plafon", name = "List Gypsum Profil 10cm", unitType = "m", basePrice = 25000f),
            CalcMaterial(id = "mat_p8", categoryId = "cat_plafon", surfaceType = "Plafon", name = "List PVC Modern", unitType = "m", basePrice = 35000f),
            CalcMaterial(id = "mat_p9", categoryId = "cat_plafon", surfaceType = "Plafon", name = "Plafon Kayu Lambersering", unitType = "m2", basePrice = 275000f),
            CalcMaterial(id = "mat_p10", categoryId = "cat_plafon", surfaceType = "Plafon", name = "Gypsum Water Resistant", unitType = "m2", basePrice = 115000f)
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

    private fun fetchLaborConfigs() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("calc_labor_configs").select().decodeList<LaborConfig>()
                if (result.isEmpty()) {
                    _laborConfigs.value = getFallbackLaborConfigs()
                } else {
                    _laborConfigs.value = result
                }
            } catch (e: Exception) {
                println("Supabase LaborConfigs Error: ${e.message}")
                _laborConfigs.value = getFallbackLaborConfigs()
            }
        }
    }

    private fun getFallbackLaborConfigs(): List<LaborConfig> {
        return listOf(
            LaborConfig("l1", "labor", "daily_expert", "Tukang Ahli", "hari", 200000.0, "Tenaga ahli untuk finishing dan detail", false),
            LaborConfig("l2", "labor", "daily_helper", "Kenek / Pembantu", "hari", 120000.0, "Membantu persiapan dan angkut material", false),
            LaborConfig("p1", "preparation", "demolition", "Bongkar Pasangan Lama", "m2", 35000.0, "Membongkar lantai/dinding lama", true),
            LaborConfig("p2", "preparation", "waste_disposal", "Buang Puing", "trip", 250000.0, "Sewa pickup untuk pembuangan limbah", false),
            LaborConfig("m1", "mep", "light_point", "Titik Lampu", "titik", 150000.0, "Instalasi kabel dan fitting lampu", false),
            LaborConfig("m2", "mep", "power_outlet", "Stopkontak", "titik", 175000.0, "Instalasi kabel dan box stopkontak", false)
        )
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

    private fun getDefaultCalcSystemConfigs(): Map<String, CalcSystemConfig> {
        return emptyMap()
    }

    private fun fetchCalcSystemConfigs() {
        viewModelScope.launch {
            try {
                val result = SupabaseConfig.client.from("calc_system_configs").select().decodeList<CalcSystemConfig>()
                val merged = getDefaultCalcSystemConfigs().toMutableMap()
                result.forEach { merged[it.key] = it }
                _calcSystemConfigs.value = merged
            } catch (e: Exception) {
                println("Supabase CalcSystemConfigs Error: ${e.message}")
            }
        }
    }

    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    fun resetSaveStatus() {
        _saveStatus.value = SaveStatus.Idle
    }

    fun saveEstimation(estimation: CalcEstimation) {
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Loading("Menyimpan Estimasi...")
            
            // Generate a local ID and timestamp if missing
            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
            val localEstimation = estimation.copy(
                id = estimation.id ?: java.util.UUID.randomUUID().toString(),
                createdAt = estimation.createdAt ?: timestamp
            )

            // Save locally first for offline availability
            _estimations.update { current ->
                (current + localEstimation).distinctBy { it.id }.sortedByDescending { it.createdAt }
            }
            saveEstimationsToLocal(_estimations.value)

            try {
                SupabaseConfig.client.from("calc_estimations").insert(localEstimation)
                println("DEBUG_LOG: Estimation saved to DB")
                
                // --- Phase 4: Trigger AI Orchestration ---
                _saveStatus.value = SaveStatus.Loading("The Architect sedang berpikir...")
                val prompt = claudeOrchestrator.generateImagePrompt(localEstimation)
                _aiImagePrompt.value = prompt
                
                // --- Phase 5: Trigger AI Visualization ---
                if (prompt != null) {
                    _saveStatus.value = SaveStatus.Loading("The Artist sedang melukis...")
                    val imageUrl = geminiArtist.generateVisualization(prompt)
                    _aiVisualizationUrl.value = imageUrl
                    
                    if (imageUrl != null) {
                        _saveStatus.value = SaveStatus.Success("Estimasi & Visualisasi AI Selesai!")
                    } else {
                        _saveStatus.value = SaveStatus.Success("Estimasi Selesai (Gagal membuat Gambar)")
                    }
                } else {
                    _saveStatus.value = SaveStatus.Success("Estimasi Berhasil (AI Prompt Gagal)")
                }

                fetchEstimations() // Sync with DB
            } catch (e: Exception) {
                println("DEBUG_LOG: DB Save failed, keeping local: ${e.message}")
                // Still show success because it's saved locally
                _saveStatus.value = SaveStatus.Success("Estimasi disimpan di perangkat (Offline)")
            }
        }
    }
}
