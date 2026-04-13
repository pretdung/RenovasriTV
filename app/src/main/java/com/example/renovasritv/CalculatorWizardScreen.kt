package com.example.renovasritv

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import java.util.Locale

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CalculatorWizardScreen(
    mainViewModel: MainViewModel,
    calcViewModel: CalculatorViewModel = viewModel()
) {
    val currentStep by calcViewModel.currentStep.collectAsState()
    val selectedCategory by calcViewModel.selectedCategory.collectAsState()
    val uiConfigs by mainViewModel.uiConfigs.collectAsState()
    val screenWidth = LocalConfiguration.current.screenWidthDp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)) // Deep dark background for TV
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(48.dp)) {
            // HEADER: Progress & Title
            WizardHeader(
                step = currentStep,
                title = selectedCategory?.name ?: "Pilih Kategori",
                uiConfigs = uiConfigs,
                screenWidth = screenWidth
            )

            Spacer(modifier = Modifier.height(32.dp))

            // CONTENT: Dynamic based on Step
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                        }
                    }, label = "StepTransition"
                ) { step ->
                    when (step) {
                        WizardStep.SELECT_CATEGORY -> CategorySelectionStep(mainViewModel, calcViewModel, uiConfigs, screenWidth)
                        WizardStep.INPUT_VARIABLES -> VariableInputStep(mainViewModel, calcViewModel)
                        WizardStep.SELECT_MATERIAL -> MaterialSelectionStep(mainViewModel, calcViewModel)
                        WizardStep.SUMMARY -> SummaryStep(calcViewModel)
                    }
                }
            }

            // FOOTER: Navigation Buttons
            WizardFooter(
                currentStep = currentStep,
                onBack = { calcViewModel.previousStep() },
                onNext = { calcViewModel.nextStep() }
            )
        }
    }
}

@Composable
fun WizardHeader(step: WizardStep, title: String, uiConfigs: Map<String, UIConfig>, screenWidth: Int) {
    val stepConfig = uiConfigs["calc_step_label"]
    val titleConfig = uiConfigs["calc_title"]

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(
                text = "${stepConfig?.value ?: "LANGKAH"} ${step.ordinal + 1} DARI 4",
                color = stepConfig?.fontColor?.toComposeColor() ?: MaterialTheme.colorScheme.primary,
                fontSize = stepConfig?.fontSize.toFontSize(screenWidth),
                fontWeight = stepConfig?.fontWeight.toFontWeight(),
                letterSpacing = 2.sp
            )
            Text(
                text = title,
                color = titleConfig?.fontColor?.toComposeColor() ?: Color.White,
                fontSize = titleConfig?.fontSize.toFontSize(screenWidth),
                fontWeight = titleConfig?.fontWeight.toFontWeight()
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        // Progress Bar Simple
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WizardStep.entries.forEach { 
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .background(
                            if (it.ordinal <= step.ordinal) MaterialTheme.colorScheme.primary 
                            else Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun CategorySelectionStep(
    mainViewModel: MainViewModel, 
    calcViewModel: CalculatorViewModel,
    uiConfigs: Map<String, UIConfig>,
    screenWidth: Int
) {
    val categories by mainViewModel.calcCategories.collectAsState()
    val itemTitleConfig = uiConfigs["calc_item_title"]
    val itemDescConfig = uiConfigs["calc_item_desc"]
    
    if (categories.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // TV Material3 might not have CircularProgressIndicator in some versions, or requires a different import
                // Using a simple text for now or verifying imports
                Text("Memuat kategori...", color = Color.Gray)
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(categories) { category ->
                var isFocused by remember { mutableStateOf(false) }

                Surface(
                    onClick = { calcViewModel.selectCategory(category) },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.05f),
                        focusedContainerColor = Color.White,
                        focusedContentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .onFocusChanged { isFocused = it.isFocused }
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category, 
                            contentDescription = null,
                            tint = if (isFocused) Color.Black else (itemTitleConfig?.fontColor?.toComposeColor() ?: MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = category.name, 
                                fontWeight = itemTitleConfig?.fontWeight.toFontWeight(),
                                fontSize = itemTitleConfig?.fontSize.toFontSize(screenWidth),
                                color = if (isFocused) Color.Black else (itemTitleConfig?.fontColor?.toComposeColor() ?: Color.White)
                            )
                            category.description?.let { 
                                Text(
                                    text = it, 
                                    fontSize = itemDescConfig?.fontSize.toFontSize(screenWidth),
                                    fontWeight = itemDescConfig?.fontWeight.toFontWeight(),
                                    color = if (isFocused) Color.DarkGray else (itemDescConfig?.fontColor?.toComposeColor() ?: Color.Gray)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VariableInputStep(mainViewModel: MainViewModel, calcViewModel: CalculatorViewModel) {
    val allVariables by mainViewModel.calcVariables.collectAsState()
    val allFormulas by mainViewModel.calcFormulas.collectAsState()
    val formulaVariables by mainViewModel.calcFormulaVariables.collectAsState()
    val selectedCategory by calcViewModel.selectedCategory.collectAsState()

    // Find formulas for this category
    val relevantFormula = allFormulas.find { it.categoryId == selectedCategory?.id }
    
    // Find required variables for those formulas
    val requiredVarIds = formulaVariables
        .filter { fv -> fv.formulaId == relevantFormula?.id }
        .map { it.variableId }
    
    val filteredVariables = allVariables.filter { it.id in requiredVarIds }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Masukkan dimensi area untuk perhitungan yang akurat.",
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        DynamicInputForm(
            variables = filteredVariables,
            inputStates = calcViewModel.inputValues,
            onValuesChanged = {
                relevantFormula?.let { 
                    calcViewModel.updateCalculation(it.expression) 
                }
            }
        )
    }
}

@Composable
fun MaterialSelectionStep(mainViewModel: MainViewModel, calcViewModel: CalculatorViewModel) {
    val materials by mainViewModel.calcMaterials.collectAsState()
    val selectedCategory by calcViewModel.selectedCategory.collectAsState()
    val filteredMaterials = materials.filter { it.categoryId == selectedCategory?.id }

    Column {
        Text(
            text = "Pilih material yang ingin Anda gunakan.",
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            lazyRowItems(filteredMaterials) { material ->
                Surface(
                    onClick = { calcViewModel.selectMaterial(material) },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.05f),
                        focusedContainerColor = Color.White,
                        focusedContentColor = Color.Black
                    ),
                    modifier = Modifier.width(280.dp).height(360.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(Color.DarkGray, RoundedCornerShape(8.dp))
                        ) // Image Placeholder
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(text = material.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = CalculationEngine.formatCurrency(material.basePrice.toDouble()) + " / " + material.unitType,
                            color = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color.DarkGray else MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "PILIH MATERIAL", fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryStep(calcViewModel: CalculatorViewModel) {
    val result by calcViewModel.calculationResult.collectAsState()
    val totalPrice by calcViewModel.totalPrice.collectAsState()
    val material by calcViewModel.selectedMaterial.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "ESTIMASI TOTAL", color = Color.Gray, letterSpacing = 2.sp)
        Text(
            text = CalculationEngine.formatCurrency(totalPrice),
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
            SummaryItem("Luas Area", String.format(Locale.getDefault(), "%.2f m²", result))
            SummaryItem("Material", material?.name ?: "-")
            SummaryItem("Harga Satuan", CalculationEngine.formatCurrency(material?.basePrice?.toDouble() ?: 0.0))
        }

        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = { calcViewModel.saveEstimation() },
            modifier = Modifier.height(64.dp).width(300.dp),
            shape = ButtonDefaults.shape(RoundedCornerShape(32.dp))
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("SIMPAN ESTIMASI", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WizardFooter(currentStep: WizardStep, onBack: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (currentStep != WizardStep.SELECT_CATEGORY) {
            Button(
                onClick = onBack,
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("KEMBALI")
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }

        if (currentStep != WizardStep.SUMMARY) {
            Button(
                onClick = onNext,
                modifier = Modifier.height(48.dp)
            ) {
                Text("LANJUT")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}
