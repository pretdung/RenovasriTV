package com.example.renovasritv

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class WizardStep {
    object InputDimensions : WizardStep()
    object SurfaceQuantities : WizardStep()
    data class MaterialSelection(val surfaceIndex: Int) : WizardStep()
    object LaborConfiguration : WizardStep()
    object EstimationSummary : WizardStep()

    val ordinal: Int
        get() = when (this) {
            is InputDimensions -> 0
            is SurfaceQuantities -> 1
            is MaterialSelection -> 2 + surfaceIndex
            is LaborConfiguration -> 100 // Logic will handle total steps
            is EstimationSummary -> 101
        }
}

class CalculatorViewModel : ViewModel() {
    private val _currentStep = MutableStateFlow<WizardStep>(WizardStep.InputDimensions)
    val currentStep: StateFlow<WizardStep> = _currentStep

    val totalSteps = MutableStateFlow(5) // Default minimum

    // Input Dimensi (P, L, T dll dari DB)
    val inputValues = mutableStateMapOf<String, Float>()

    private val _surfaces = MutableStateFlow<List<SurfaceData>>(emptyList())
    val surfaces: StateFlow<List<SurfaceData>> = _surfaces

    private val _laborGroups = MutableStateFlow<Map<String, LaborGroupState>>(emptyMap())
    val laborGroups: StateFlow<Map<String, LaborGroupState>> = _laborGroups

    fun initializeInputs(variables: List<CalcVariable>) {
        if (inputValues.isNotEmpty() || variables.isEmpty()) return
        variables.forEach { variable ->
            inputValues[variable.variableKey] = 3.0f // Default fallback
        }
    }

    fun initializeLaborGroups(configs: List<LaborConfig>, categories: List<CalcLaborCategory>) {
        if (_laborGroups.value.isNotEmpty() || configs.isEmpty()) return
        
        val initialMap = configs.groupBy { it.category }.mapValues { (catKey, items) ->
            val categoryMetadata = categories.find { it.key == catKey }
            val defaultQtys = mutableMapOf<String, Float>()
            items.forEach { item ->
                defaultQtys[item.itemKey] = 0f
            }
            
            LaborGroupState(
                key = catKey,
                isActive = categoryMetadata?.isActiveDefault ?: (catKey == "labor"),
                quantities = defaultQtys
            )
        }
        _laborGroups.value = initialMap
    }

    fun toggleLaborGroup(key: String) {
        val current = _laborGroups.value.toMutableMap()
        val group = current[key] ?: return
        current[key] = group.copy(isActive = !group.isActive)
        _laborGroups.value = current
    }

    fun updateLaborQuantity(groupKey: String, itemKey: String, value: Float) {
        val current = _laborGroups.value.toMutableMap()
        val group = current[groupKey] ?: return
        val qtys = group.quantities.toMutableMap()
        qtys[itemKey] = value
        current[groupKey] = group.copy(quantities = qtys)
        _laborGroups.value = current
    }

    fun updateLaborOverride(groupKey: String, itemKey: String, price: Double) {
        val current = _laborGroups.value.toMutableMap()
        val group = current[groupKey] ?: return
        val overrides = group.overrides.toMutableMap()
        overrides[itemKey] = price
        current[groupKey] = group.copy(overrides = overrides)
        _laborGroups.value = current
    }

    fun nextStep(formulas: List<CalcFormula>, categories: List<CalcCategory>) {
        when (val step = _currentStep.value) {
            is WizardStep.InputDimensions -> {
                // PAKSA PERHITUNGAN SAAT LANJUT DARI DIMENSI
                calculateInitialSurfaces(formulas, categories)
                _currentStep.value = WizardStep.SurfaceQuantities
            }
            is WizardStep.SurfaceQuantities -> {
                // Pastikan slot material disiapkan sebelum masuk ke pemilihan material
                initializeMaterialSlots()
                _currentStep.value = WizardStep.MaterialSelection(0)
            }
            is WizardStep.MaterialSelection -> {
                val nextIndex = step.surfaceIndex + 1
                if (nextIndex < _surfaces.value.size) {
                    _currentStep.value = WizardStep.MaterialSelection(nextIndex)
                } else {
                    _currentStep.value = WizardStep.LaborConfiguration
                }
            }
            is WizardStep.LaborConfiguration -> _currentStep.value = WizardStep.EstimationSummary
            is WizardStep.EstimationSummary -> {}
        }
    }

    fun previousStep() {
        when (val step = _currentStep.value) {
            is WizardStep.InputDimensions -> {}
            is WizardStep.SurfaceQuantities -> _currentStep.value = WizardStep.InputDimensions
            is WizardStep.MaterialSelection -> {
                if (step.surfaceIndex > 0) {
                    _currentStep.value = WizardStep.MaterialSelection(step.surfaceIndex - 1)
                } else {
                    _currentStep.value = WizardStep.SurfaceQuantities
                }
            }
            is WizardStep.LaborConfiguration -> {
                if (_surfaces.value.isNotEmpty()) {
                    _currentStep.value = WizardStep.MaterialSelection(_surfaces.value.size - 1)
                } else {
                    _currentStep.value = WizardStep.SurfaceQuantities
                }
            }
            is WizardStep.EstimationSummary -> _currentStep.value = WizardStep.LaborConfiguration
        }
    }

    private fun calculateInitialSurfaces(formulas: List<CalcFormula>, categories: List<CalcCategory>) {
        // Ambil data terbaru dari SnapshotStateMap ke Map reguler
        // Konversi key ke Uppercase untuk sinkronisasi dengan CalculationEngine
        val currentInputs = inputValues.toMap().mapKeys { it.key.uppercase() }
        val variables = currentInputs.mapValues { it.value.toDouble() }
        
        println("DEBUG: Calculating surfaces with inputs: $variables")
        
        val newSurfaces = formulas.mapNotNull { formula ->
            val category = categories.find { it.id == formula.categoryId }
            if (category != null) {
                val area = CalculationEngine.evaluateFormula(formula.expression, variables)
                println("DEBUG: Formula ${formula.expression} for ${category.name} resulted in area: $area")
                
                SurfaceData(
                    categoryId = category.id,
                    name = category.name,
                    formula = formula.expression,
                    area = area,
                    materialCount = category.materialCount
                )
            } else {
                println("DEBUG: Category not found for formula ${formula.id}")
                null
            }
        }
        
        _surfaces.value = newSurfaces
    }

    private fun initializeMaterialSlots() {
        val current = _surfaces.value.map { surface ->
            // If slots not matching materialCount, re-initialize
            if (surface.selectedMaterials.size != surface.materialCount) {
                val defaultPct = 100f / surface.materialCount
                surface.copy(
                    selectedMaterials = List(surface.materialCount) { null }
                )
            } else surface
        }
        _surfaces.value = current
    }

    fun updateSurfaceMaterialCount(index: Int, count: Int) {
        val current = _surfaces.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(materialCount = count)
            _surfaces.value = current
        }
    }

    fun setMaterialAtSlot(surfaceIndex: Int, slotIndex: Int, material: CalcMaterial) {
        val current = _surfaces.value.toMutableList()
        if (surfaceIndex !in current.indices) return
        
        val surface = current[surfaceIndex]
        val materials = surface.selectedMaterials.toMutableList()
        
        if (slotIndex in materials.indices) {
            val defaultPct = 100f / surface.materialCount
            materials[slotIndex] = MaterialCoverage(material, defaultPct)
            current[surfaceIndex] = surface.copy(selectedMaterials = materials)
            _surfaces.value = current
        }
    }

    fun updateMaterialCoverage(surfaceIndex: Int, slotIndex: Int, newPct: Float) {
        val current = _surfaces.value.toMutableList()
        if (surfaceIndex !in current.indices) return
        
        val surface = current[surfaceIndex]
        val materials = surface.selectedMaterials.toMutableList()
        if (slotIndex in materials.indices) {
            val mc = materials[slotIndex]
            if (mc != null) {
                materials[slotIndex] = mc.copy(coveragePercentage = newPct)
                current[surfaceIndex] = surface.copy(selectedMaterials = materials)
                _surfaces.value = current
            }
        }
    }

    fun calculateTotalEstimation(
        laborCostFactor: Double = 0.0,
        taxFactor: Double = 0.0
    ): Triple<Double, Double, Double> {
        var minTotal = 0.0
        var maxTotal = 0.0
        
        _surfaces.value.forEach { surface ->
            surface.selectedMaterials.filterNotNull().forEach { mc ->
                val materialVolume = surface.area * (mc.coveragePercentage / 100.0)
                val basePrice = mc.material.basePrice.toDouble()
                minTotal += materialVolume * basePrice * mc.material.priceMinFactor
                maxTotal += materialVolume * basePrice * mc.material.priceMaxFactor
            }
        }
        
        val laborAvg = ((minTotal + maxTotal) / 2.0) * laborCostFactor
        val subtotal = (minTotal + maxTotal) / 2.0 + laborAvg
        val taxAmount = subtotal * taxFactor
        
        return Triple(minTotal, maxTotal, laborAvg)
    }

    fun calculateTotalEstimationDetailed(
        systemConfigs: Map<String, CalcSystemConfig>,
        laborConfigs: List<LaborConfig> = emptyList()
    ): Map<String, Double> {
        var materialMinTotal = 0.0
        var materialMaxTotal = 0.0
        
        _surfaces.value.forEach { surface ->
            surface.selectedMaterials.forEach { mc ->
                if (mc != null) {
                    val portion = (surface.area * mc.coveragePercentage / 100.0)
                    materialMinTotal += portion * mc.material.basePrice * mc.material.priceMinFactor
                    materialMaxTotal += portion * mc.material.basePrice * mc.material.priceMaxFactor
                }
            }
        }
        
        var laborTotal = 0.0
        val otherCategoriesTotal = mutableMapOf<String, Double>()
        
        _laborGroups.value.forEach { (catKey, state) ->
            if (state.isActive && catKey != "contingency") {
                var groupSum = 0.0
                state.quantities.forEach { (itemKey, qty) ->
                    val config = laborConfigs.find { it.itemKey == itemKey }
                    val unitPrice = state.overrides[itemKey] ?: config?.defaultPrice ?: 0.0
                    groupSum += qty.toDouble() * unitPrice
                }
                if (catKey == "labor") laborTotal = groupSum
                else otherCategoriesTotal[catKey] = groupSum
            }
        }
        
        val jasaSubtotal = laborTotal + otherCategoriesTotal.values.sum()
        
        val contingencyState = _laborGroups.value["contingency"]
        val contingencyPct = if (contingencyState?.isActive == true) (contingencyState.quantities["percent"] ?: 10f) / 100f else 0f
        val currentSubtotalAvg = (materialMinTotal + materialMaxTotal) / 2.0 + jasaSubtotal
        val contingencyTotal = currentSubtotalAvg * contingencyPct

        var grandTotalMin = materialMinTotal + jasaSubtotal + contingencyTotal
        var grandTotalMax = materialMaxTotal + jasaSubtotal + contingencyTotal
        
        val fees = mutableMapOf<String, Double>()
        systemConfigs.values.filter { it.isVisible }.forEach { config ->
            val feeAmount = if (config.configType == "factor") {
                val factor = config.value.toDoubleOrNull() ?: 0.0
                ((grandTotalMin + grandTotalMax) / 2.0) * factor
            } else {
                config.value.toDoubleOrNull() ?: 0.0
            }
            if (feeAmount > 0) {
                fees[config.key] = feeAmount
                grandTotalMin += feeAmount
                grandTotalMax += feeAmount
            }
        }
        
        return mapOf(
            "materialMin" to materialMinTotal,
            "materialMax" to materialMaxTotal,
            "laborTotal" to laborTotal,
            "jasaSubtotal" to jasaSubtotal,
            "contingencyTotal" to contingencyTotal,
            "contingencyPct" to contingencyPct.toDouble(),
            "grandTotalMin" to grandTotalMin,
            "grandTotalMax" to grandTotalMax
        ) + otherCategoriesTotal.mapKeys { "other_${it.key}" } + fees.mapKeys { "fee_${it.key}" }
    }

    fun saveEstimation(mainViewModel: MainViewModel) {
        val estimation = generateEstimation(mainViewModel.calcSystemConfigs.value)
        mainViewModel.saveEstimation(estimation)
    }

    fun generateEstimation(
        systemConfigs: Map<String, CalcSystemConfig> = emptyMap(),
        laborConfigs: List<LaborConfig> = emptyList()
    ): CalcEstimation {
        // Prepare JSON snapshots of the current state
        val roomDimensionsJson = Json.encodeToString(inputValues.toMap())
        val surfacesJson = Json.encodeToString(_surfaces.value)
        val laborJson = Json.encodeToString(_laborGroups.value)

        // Calculate totals using existing detailed logic
        val totals = calculateTotalEstimationDetailed(systemConfigs, laborConfigs)
        
        return CalcEstimation(
            totalMin = totals["grandTotalMin"] ?: 0.0,
            totalMax = totals["grandTotalMax"] ?: 0.0,
            roomDimensions = roomDimensionsJson,
            surfacesJson = surfacesJson,
            laborJson = laborJson
        )
    }

    fun resumeEstimation(estimation: CalcEstimation) {
        try {
            // 1. Resume Dimensions
            val dimensions: Map<String, Float> = Json.decodeFromString(estimation.roomDimensions)
            inputValues.clear()
            inputValues.putAll(dimensions)

            // 2. Resume Surfaces
            val surfaces: List<SurfaceData> = Json.decodeFromString(estimation.surfacesJson)
            _surfaces.value = surfaces

            // 3. Resume Labor Groups
            val laborGroups: Map<String, LaborGroupState> = Json.decodeFromString(estimation.laborJson)
            _laborGroups.value = laborGroups

            // 4. Move to Summary Step
            _currentStep.value = WizardStep.EstimationSummary
            
        } catch (e: Exception) {
            println("DEBUG_LOG: Error resuming estimation: ${e.message}")
            e.printStackTrace()
        }
    }

    fun reset() {
        _currentStep.value = WizardStep.InputDimensions
        inputValues.clear()
        _surfaces.value = emptyList()
        _laborGroups.value = emptyMap()
        // Note: initializeInputs and initializeLaborGroups will be re-triggered by LaunchedEffect in the UI
    }
}
