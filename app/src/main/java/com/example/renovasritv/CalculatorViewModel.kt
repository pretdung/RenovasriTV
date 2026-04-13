package com.example.renovasritv

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class WizardStep {
    SELECT_CATEGORY,
    INPUT_VARIABLES,
    SELECT_MATERIAL,
    SUMMARY
}

class CalculatorViewModel : ViewModel() {
    // Navigasi
    private val _currentStep = MutableStateFlow(WizardStep.SELECT_CATEGORY)
    val currentStep: StateFlow<WizardStep> = _currentStep

    // Data Pilihan User
    private val _selectedCategory = MutableStateFlow<CalcCategory?>(null)
    val selectedCategory: StateFlow<CalcCategory?> = _selectedCategory

    private val _selectedMaterial = MutableStateFlow<CalcMaterial?>(null)
    val selectedMaterial: StateFlow<CalcMaterial?> = _selectedMaterial

    // Input Variabel (P, L, T, dll)
    val inputValues = mutableStateMapOf<String, Float>()

    // Hasil Perhitungan
    private val _calculationResult = MutableStateFlow(0.0)
    val calculationResult: StateFlow<Double> = _calculationResult

    private val _totalPrice = MutableStateFlow(0.0)
    val totalPrice: StateFlow<Double> = _totalPrice

    fun nextStep() {
        val next = when (_currentStep.value) {
            WizardStep.SELECT_CATEGORY -> WizardStep.INPUT_VARIABLES
            WizardStep.INPUT_VARIABLES -> WizardStep.SELECT_MATERIAL
            WizardStep.SELECT_MATERIAL -> WizardStep.SUMMARY
            WizardStep.SUMMARY -> WizardStep.SUMMARY
        }
        _currentStep.value = next
    }

    fun previousStep() {
        val prev = when (_currentStep.value) {
            WizardStep.SELECT_CATEGORY -> WizardStep.SELECT_CATEGORY
            WizardStep.INPUT_VARIABLES -> WizardStep.SELECT_CATEGORY
            WizardStep.SELECT_MATERIAL -> WizardStep.INPUT_VARIABLES
            WizardStep.SUMMARY -> WizardStep.SELECT_MATERIAL
        }
        _currentStep.value = prev
    }

    fun selectCategory(category: CalcCategory) {
        _selectedCategory.value = category
        // Reset input saat kategori berubah
        inputValues.clear()
        nextStep()
    }

    fun selectMaterial(material: CalcMaterial) {
        _selectedMaterial.value = material
        nextStep()
    }

    fun updateCalculation(formula: String, materialPrice: Double = 0.0) {
        val vars = inputValues.mapValues { it.value.toDouble() }
        val result = CalculationEngine.evaluateFormula(formula, vars)
        _calculationResult.value = result
        _totalPrice.value = result * materialPrice
    }

    fun saveEstimation() {
        val category = _selectedCategory.value ?: return
        val material = _selectedMaterial.value ?: return
        
        val snapshot = CalcSnapshot(
            id = java.util.UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            categoryName = category.name,
            materialName = material.name,
            totalArea = _calculationResult.value,
            totalPrice = _totalPrice.value,
            inputs = inputValues.toMap()
        )
        
        // TODO: Implementasi penyimpanan ke Room atau DataStore
        println("DEBUG_LOG: Saved Snapshot: $snapshot")
    }
}
