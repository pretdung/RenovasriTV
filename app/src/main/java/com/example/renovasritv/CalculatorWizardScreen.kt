package com.example.renovasritv

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.tv.material3.*
import java.util.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CalculatorWizardScreen(
    mainViewModel: MainViewModel,
    navController: androidx.navigation.NavController,
    calcViewModel: CalculatorViewModel = viewModel()
) {
    val context = LocalContext.current
    val saveStatus by mainViewModel.saveStatus.collectAsState()

    LaunchedEffect(saveStatus) {
        when (val status = saveStatus) {
            is SaveStatus.Success -> {
                Toast.makeText(context, status.message, Toast.LENGTH_SHORT).show()
                mainViewModel.resetSaveStatus()
                navController.navigate(Screen.AiResult.route)
            }
            is SaveStatus.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                mainViewModel.resetSaveStatus()
            }
            else -> {}
        }
    }

    val currentStep by calcViewModel.currentStep.collectAsState()

    // Overlay Loading if saving
    if (saveStatus is SaveStatus.Loading) {
        Dialog(onDismissRequest = {}) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        (saveStatus as SaveStatus.Loading).message,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    val uiConfigs by mainViewModel.uiConfigs.collectAsState()
    val laborConfigs by mainViewModel.laborConfigs.collectAsState()
    val laborCategories by mainViewModel.calcLaborCategories.collectAsState()
    val surfaces by calcViewModel.surfaces.collectAsState()
    val variables by mainViewModel.calcVariables.collectAsState()

    val nextButtonFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    var isInitialStepLoad by remember { mutableStateOf(true) }

    // Request focus on the most left-upper component whenever the step changes
    LaunchedEffect(currentStep) {
        if (!isInitialStepLoad) {
            try {
                contentFocusRequester.requestFocus()
            } catch (_: Exception) {
                // Ignore if focus requester is not yet attached
            }
        }
        isInitialStepLoad = false
    }

    DisposableEffect(Unit) {
        onDispose {
            isInitialStepLoad = true
        }
    }

    // Initialize inputs when variables arrive
    LaunchedEffect(variables) {
        if (variables.isNotEmpty()) {
            calcViewModel.initializeInputs(variables)
        }
    }

    // Initialize labor groups when configs arrive from DB
    LaunchedEffect(laborConfigs, laborCategories) {
        if (laborConfigs.isNotEmpty() && laborCategories.isNotEmpty()) {
            calcViewModel.initializeLaborGroups(laborConfigs, laborCategories)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Scrim khusus area Header agar teks tetap terbaca jelas di atas background image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 32.dp)) {
            WizardHeader(
                step = currentStep,
                title = when (currentStep) {
                    is WizardStep.InputDimensions -> "Dimensi Ruangan"
                    is WizardStep.SurfaceQuantities -> "Jumlah Material"
                    is WizardStep.MaterialSelection -> {
                        val surface = (currentStep as WizardStep.MaterialSelection).surfaceIndex
                        val name = surfaces.getOrNull(surface)?.name ?: "Material"
                        "Material $name"
                    }
                    is WizardStep.LaborConfiguration -> "Konfigurasi Biaya Jasa"
                    is WizardStep.EstimationSummary -> "Ringkasan Estimasi"
                },
                uiConfigs = uiConfigs,
                totalSurfaces = surfaces.size
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = currentStep,
                    label = "StepTransition"
                ) { step ->
                    when (step) {
                        is WizardStep.InputDimensions -> DimensionInputStep(mainViewModel, calcViewModel, contentFocusRequester)
                        is WizardStep.SurfaceQuantities -> SurfaceQuantityStep(calcViewModel, contentFocusRequester)
                        is WizardStep.MaterialSelection -> MaterialSelectionStep(step.surfaceIndex, mainViewModel, calcViewModel, contentFocusRequester)
                        is WizardStep.LaborConfiguration -> LaborConfigurationStep(mainViewModel, calcViewModel, nextButtonFocusRequester, contentFocusRequester)
                        is WizardStep.EstimationSummary -> SummaryStep(mainViewModel, calcViewModel, contentFocusRequester, nextButtonFocusRequester)
                    }
                }
            }

            val formulas by mainViewModel.calcFormulas.collectAsState()
            val categories by mainViewModel.calcCategories.collectAsState()

            WizardFooter(
                mainViewModel = mainViewModel,
                calcViewModel = calcViewModel,
                currentStep = currentStep,
                onBack = { calcViewModel.previousStep() },
                onNext = { calcViewModel.nextStep(formulas, categories) },
                nextButtonFocusRequester = nextButtonFocusRequester,
                onCancel = { calcViewModel.reset() }
            )
        }
    }
}

@Composable
fun DimensionInputStep(
    mainViewModel: MainViewModel,
    calcViewModel: CalculatorViewModel, 
    focusRequester: FocusRequester
) {
    val variables by mainViewModel.calcVariables.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Masukkan dimensi ruangan secara akurat:", 
            color = Color.Gray, 
            modifier = Modifier.padding(bottom = 24.dp),
            fontSize = 14.sp
        )
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(variables) { index, variable ->
                val value = calcViewModel.inputValues[variable.variableKey] ?: 3f
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(variable.label, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black)
                            Text("Satuan: ${variable.unit ?: "m"}", color = Color.Gray, fontSize = 12.sp)
                        }

                        NumericStepper(
                            label = variable.label,
                            value = value,
                            unit = variable.unit ?: "m",
                            min = variable.minValue,
                            max = variable.maxValue,
                            step = if (variable.unit == "m") 0.1f else variable.step,
                            onValueChange = { calcViewModel.inputValues[variable.variableKey] = it },
                            firstButtonFocusRequester = if (index == 0) focusRequester else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SurfaceQuantityStep(
    calcViewModel: CalculatorViewModel, 
    focusRequester: FocusRequester
) {
    val surfaces by calcViewModel.surfaces.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Tentukan jumlah material per permukaan:", color = Color.Gray.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            surfaces.forEachIndexed { index, surface ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    surface.name.contains("Lantai", true) -> Icons.Default.Layers
                                    surface.name.contains("Dinding", true) -> Icons.Default.VerticalSplit
                                    surface.name.contains("Plafon", true) -> Icons.Default.Roofing
                                    else -> Icons.Default.SquareFoot
                                },
                                contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(surface.name, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.Black)
                            Text("Luas area: ${String.format(Locale.US, "%.2f", surface.area)} m²", color = Color.Gray, fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Jumlah Material", modifier = Modifier.padding(end = 20.dp), color = Color.DarkGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            NumericStepper(
                                label = "",
                                value = surface.materialCount.toFloat(),
                                unit = "",
                                min = 1f,
                                max = 5f,
                                step = 1f,
                                onValueChange = { calcViewModel.updateSurfaceMaterialCount(index, it.toInt()) },
                                firstButtonFocusRequester = if (index == 0) focusRequester else null
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MaterialSelectionStep(
    surfaceIndex: Int, 
    mainViewModel: MainViewModel, 
    calcViewModel: CalculatorViewModel,
    focusRequester: FocusRequester
) {
    val surfaces by calcViewModel.surfaces.collectAsState()
    val allMaterials by mainViewModel.calcMaterials.collectAsState()

    if (surfaceIndex >= surfaces.size) return
    val surface = surfaces[surfaceIndex]
    
    val filteredMaterials = remember(allMaterials, surface) {
        allMaterials.filter { it.categoryId == surface.categoryId }
            .ifEmpty { allMaterials.filter { it.surfaceType?.contains(surface.name, ignoreCase = true) == true } }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Pilih Material untuk ${surface.name}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
            Text("Jenis Material", modifier = Modifier.weight(1.2f), color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Persentase Luas (%)", modifier = Modifier.weight(1f), color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            itemsIndexed(surface.selectedMaterials) { slotIndex, mc ->
                val totalOtherPercentage = surface.selectedMaterials.filterIndexed { i, _ -> i != slotIndex }
                    .sumOf { it?.coveragePercentage?.toDouble() ?: 0.0 }
                val maxAllowed = (100.0 - totalOtherPercentage).coerceAtLeast(10.0).toFloat()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    var showDropdown by remember { mutableStateOf(false) }
                    
                    if (showDropdown) {
                        BackHandler { showDropdown = false }
                    }

                    Box(modifier = Modifier.weight(1.2f)) {
                        Surface(
                            onClick = { showDropdown = true },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.White,
                                focusedContainerColor = MaterialTheme.colorScheme.primary,
                                focusedContentColor = Color.Black
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (slotIndex == 0) Modifier.focusRequester(focusRequester) else Modifier)
                        ) {
                            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        mc?.material?.name ?: "Pilih Material Slot ${slotIndex + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.Black
                                    )
                                    if (mc != null) {
                                        Text(
                                            "Estimasi: " + CalculationEngine.formatCurrency(mc.material.basePrice * mc.material.priceMinFactor.toDouble()) +
                                                    " - " + CalculationEngine.formatCurrency(mc.material.basePrice * mc.material.priceMaxFactor.toDouble()) + " / unit",
                                            fontSize = 12.sp, color = Color.DarkGray
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.UnfoldMore, contentDescription = null, tint = Color.Black)
                            }
                        }

                        if (showDropdown) {
                            Dialog(
                                onDismissRequest = { showDropdown = false },
                                properties = DialogProperties(usePlatformDefaultWidth = false)
                            ) {
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).padding(64.dp), contentAlignment = Alignment.Center) {
                                    Surface(
                                        modifier = Modifier.width(600.dp).fillMaxHeight(0.8f),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = SurfaceDefaults.colors(containerColor = Color(0xFF1A1A1A))
                                    ) {
                                        Column(modifier = Modifier.padding(24.dp)) {
                                            Text(
                                                "Pilih Material ${surface.name}", 
                                                fontWeight = FontWeight.Black, 
                                                fontSize = 24.sp, 
                                                color = Color.White,
                                                modifier = Modifier.padding(bottom = 16.dp)
                                            )
                                            
                                            if (filteredMaterials.isEmpty()) {
                                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Text("Tidak ada material tersedia untuk kategori ini.", color = Color.White.copy(alpha = 0.6f))
                                                }
                                            } else {
                                                val dialogFirstItemFocusRequester = remember { FocusRequester() }
                                                
                                                LaunchedEffect(showDropdown) {
                                                    if (showDropdown) {
                                                        try {
                                                            dialogFirstItemFocusRequester.requestFocus()
                                                        } catch (_: Exception) {}
                                                    }
                                                }

                                                LazyColumn(
                                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    itemsIndexed(filteredMaterials) { mIndex, material ->
                                                        Surface(
                                                            onClick = {
                                                                calcViewModel.setMaterialAtSlot(surfaceIndex, slotIndex, material)
                                                                if (mc == null) {
                                                                    calcViewModel.updateMaterialCoverage(surfaceIndex, slotIndex, 100f / surface.materialCount)
                                                                }
                                                                showDropdown = false
                                                            },
                                                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                                                            colors = ClickableSurfaceDefaults.colors(
                                                                containerColor = Color.White,
                                                                contentColor = Color.Black,
                                                                focusedContainerColor = MaterialTheme.colorScheme.primary,
                                                                focusedContentColor = Color.White
                                                            ),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .then(if (mIndex == 0) Modifier.focusRequester(dialogFirstItemFocusRequester) else Modifier)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(20.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        material.name, 
                                                                        fontWeight = FontWeight.Bold, 
                                                                        fontSize = 18.sp
                                                                    )
                                                                    Text(
                                                                        "Estimasi: ${CalculationEngine.formatCurrency(material.basePrice.toDouble())} / ${material.unitType}",
                                                                        fontSize = 14.sp, 
                                                                        color = Color.Gray
                                                                    )
                                                                }
                                                                if (mc?.material?.id == material.id) {
                                                                    Icon(Icons.Default.CheckCircle, contentDescription = "Terpilih", tint = MaterialTheme.colorScheme.primary)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = { showDropdown = false },
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                Text("BATAL")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    NumericStepper(
                        label = "",
                        value = mc?.coveragePercentage ?: 0f,
                        unit = "%",
                        min = 10f,
                        max = maxAllowed,
                        step = 5f,
                        onValueChange = { if (mc != null) calcViewModel.updateMaterialCoverage(surfaceIndex, slotIndex, it) },
                        firstButtonFocusRequester = if (slotIndex == 0 && surface.selectedMaterials.getOrNull(0)?.material == null) focusRequester else null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LaborConfigurationStep(
    mainViewModel: MainViewModel, 
    calcViewModel: CalculatorViewModel,
    nextButtonFocusRequester: FocusRequester,
    focusRequester: FocusRequester
) {
    val laborGroups by calcViewModel.laborGroups.collectAsState()
    val laborConfigs by mainViewModel.laborConfigs.collectAsState()
    val calcLaborCategories by mainViewModel.calcLaborCategories.collectAsState()
    
    var selectedGroupKey by remember { mutableStateOf("") }
    
    LaunchedEffect(laborGroups) {
        if (selectedGroupKey.isEmpty() && laborGroups.isNotEmpty()) {
            selectedGroupKey = laborGroups.keys.first()
        }
    }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        Column(modifier = Modifier.weight(1.2f)) {
            Text("Pilih kategori biaya untuk dikonfigurasi:", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items = laborGroups.keys.toList()) { key ->
                    val groupState = laborGroups[key] ?: return@items
                    val categoryMeta = calcLaborCategories.find { it.key == key }
                    
                    val label = categoryMeta?.label ?: key.replaceFirstChar { it.uppercase() }
                    
                    val icon = when(categoryMeta?.iconName) {
                        "Groups" -> Icons.Default.Groups
                        "Construction" -> Icons.Default.Construction
                        "ElectricalServices" -> Icons.Default.ElectricalServices
                        "Shield", "ReportProblem" -> Icons.Default.Shield
                        else -> Icons.Default.Payments
                    }

                    Surface(
                        onClick = { selectedGroupKey = key },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.White,
                            focusedContainerColor = MaterialTheme.colorScheme.primary,
                            focusedContentColor = Color.Black
                        ),
                        modifier = (if (key == laborGroups.keys.first()) Modifier.focusRequester(focusRequester) else Modifier)
                            .fillMaxWidth()
                            .onFocusChanged { if (it.isFocused) selectedGroupKey = key }
                            .focusProperties { 
                                if (key == laborGroups.keys.last()) {
                                    down = nextButtonFocusRequester
                                }
                            }
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).background(
                                    if (groupState.isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), 
                                    tint = if (groupState.isActive) MaterialTheme.colorScheme.primary else Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black)
                                Text(
                                    if (groupState.isActive) "Status: AKTIF" else "Status: NON-AKTIF",
                                    fontSize = 12.sp,
                                    color = if (groupState.isActive) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (groupState.overrides.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("CUSTOM", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
        }

        val activeGroup = laborGroups[selectedGroupKey]
        val categoryMeta = calcLaborCategories.find { it.key == selectedGroupKey }

        Column(
            modifier = Modifier.weight(1f).background(Color.White, RoundedCornerShape(24.dp)).padding(24.dp)
        ) {
            if (activeGroup != null) {
                Text(
                    (categoryMeta?.label ?: selectedGroupKey).uppercase(), 
                    fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp
                )
                
                Text(
                    categoryMeta?.description ?: "Pengaturan biaya tambahan untuk kategori ini.",
                    fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )

                Surface(
                    onClick = { calcViewModel.toggleLaborGroup(selectedGroupKey) },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (activeGroup.isActive) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.1f),
                        focusedContainerColor = if (activeGroup.isActive) Color.White else MaterialTheme.colorScheme.primary,
                        focusedContentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .focusRequester(focusRequester)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            if (activeGroup.isActive) "AKTIF (Klik untuk Matikan)" else "AKTIFKAN GRUP INI",
                            fontWeight = FontWeight.Black,
                        )
                    }
                }

                if (activeGroup.isActive) {
                    Spacer(modifier = Modifier.height(24.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val itemsInGroup = laborConfigs.filter { it.category == selectedGroupKey }
                        
                        if (itemsInGroup.isEmpty() && selectedGroupKey == "contingency") {
                            item(key = "contingency_percent") {
                                NumericStepper(
                                    label = "Persentase Darurat (%)",
                                    value = activeGroup.quantities["percent"] ?: 10f,
                                    unit = "%",
                                    min = 0f,
                                    max = 20f,
                                    step = 1f,
                                    onValueChange = { calcViewModel.updateLaborQuantity("contingency", "percent", it) }
                                )
                            }
                        } else {
                            items(items = itemsInGroup, key = { it.itemKey }) { config ->
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val currentValue = activeGroup.quantities[config.itemKey] ?: 0f
                                    NumericStepper(
                                        label = if (config.isBorongan) "${config.name} (Auto)" else config.name,
                                        value = currentValue,
                                        unit = config.unit,
                                        min = 0f,
                                        max = 1000f,
                                        step = 1f,
                                        onValueChange = { calcViewModel.updateLaborQuantity(selectedGroupKey, config.itemKey, it) }
                                    )
                                    
                                    val currentPrice = activeGroup.overrides[config.itemKey] ?: config.defaultPrice
                                    if (selectedGroupKey == "labor") {
                                        NumericStepper(
                                            label = "Upah ${config.name} / ${config.unit} (K)",
                                            value = (currentPrice / 1000.0).toFloat(),
                                            unit = "K",
                                            min = 10f,
                                            max = 1000f,
                                            step = 5f,
                                            onValueChange = { calcViewModel.updateLaborOverride(selectedGroupKey, config.itemKey, it.toDouble() * 1000.0) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalTvMaterial3Api::class)
fun SummaryStep(
    mainViewModel: MainViewModel, 
    calcViewModel: CalculatorViewModel,
    focusRequester: FocusRequester,
    nextButtonFocusRequester: FocusRequester
) {
    val surfaces by calcViewModel.surfaces.collectAsState()
    val systemConfigs by mainViewModel.calcSystemConfigs.collectAsState()
    val laborConfigs by mainViewModel.laborConfigs.collectAsState()

    val estimationTotals = remember(surfaces, systemConfigs, laborConfigs) {
        calcViewModel.calculateTotalEstimationDetailed(systemConfigs, laborConfigs)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("RINCIAN MATERIAL", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Gray, letterSpacing = 1.sp)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(surfaces) { surface ->
                        Surface(
                            onClick = {},
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.White.copy(alpha = 0.05f),
                                contentColor = Color.White,
                                focusedContainerColor = Color.White,
                                focusedContentColor = Color.Black
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(surface.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                surface.selectedMaterials.filterNotNull().forEach { mc ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${mc.material.name} (${mc.coveragePercentage.toInt()}%)", 
                                            color = LocalContentColor.current.copy(alpha = 0.7f), 
                                            fontSize = 12.sp
                                        )
                                        val min = (surface.area * mc.coveragePercentage / 100.0) * mc.material.basePrice * mc.material.priceMinFactor
                                        val max = (surface.area * mc.coveragePercentage / 100.0) * mc.material.basePrice * mc.material.priceMaxFactor
                                        Text(
                                            "${CalculationEngine.formatCurrencyShort(min)} - ${CalculationEngine.formatCurrencyShort(max)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("BIAYA JASA & LAINNYA", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Gray, letterSpacing = 1.sp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryRow("Biaya Jasa Tukang", estimationTotals["laborTotal"] ?: 0.0, Color.White)
                    
                    estimationTotals.filter { it.key.startsWith("other_") }.forEach { (key, amount) ->
                        val catKey = key.removePrefix("other_")
                        val label = mainViewModel.calcLaborCategories.value.find { it.key == catKey }?.label ?: catKey
                        SummaryRow(label, amount, Color.White)
                    }

                    val contingencyPctValue = (estimationTotals["contingencyPct"] ?: 0.0) * 100.0
                    SummaryRow("Dana Darurat (${contingencyPctValue.toInt()}%)", estimationTotals["contingencyTotal"] ?: 0.0, Color.White)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("BIAYA LAIN (PAJAK/FEE)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    
                    estimationTotals.filter { it.key.startsWith("fee_") }.forEach { (key, amount) ->
                        val feeKey = key.removePrefix("fee_")
                        val label = systemConfigs[feeKey]?.labelDisplay ?: feeKey
                        SummaryRow(label, amount, Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black,
                focusedContainerColor = Color.White,
                focusedContentColor = Color.Black
            )
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("TOTAL ESTIMASI BIAYA", fontWeight = FontWeight.Bold, color = LocalContentColor.current.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(
                        "${CalculationEngine.formatCurrency(estimationTotals["grandTotalMin"] ?: 0.0)} - ${CalculationEngine.formatCurrency(estimationTotals["grandTotalMax"] ?: 0.0)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    )
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, amount: Double, color: Color = Color.Unspecified, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        val textColor = if (color == Color.Unspecified) LocalContentColor.current else color
        Text(label, fontSize = 13.sp, color = textColor, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(CalculationEngine.formatCurrency(amount), fontSize = 13.sp, color = textColor, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
fun WizardHeader(
    step: WizardStep, 
    title: String, 
    uiConfigs: Map<String, UIConfig>, 
    totalSurfaces: Int
) {
    val stepIndex = when (step) {
        is WizardStep.InputDimensions -> 1
        is WizardStep.SurfaceQuantities -> 2
        is WizardStep.MaterialSelection -> 3 + step.surfaceIndex
        is WizardStep.LaborConfiguration -> 3 + totalSurfaces
        is WizardStep.EstimationSummary -> 4 + totalSurfaces
    }
    
    val totalSteps = 4 + totalSurfaces

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "LANGKAH $stepIndex DARI $totalSteps",
                color = uiConfigs["calc_step_label"]?.fontColor?.toComposeColor() ?: Color(0xFFFFB59E),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
        
        Text(
            text = title,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WizardFooter(
    mainViewModel: MainViewModel,
    calcViewModel: CalculatorViewModel,
    currentStep: WizardStep,
    onBack: () -> Unit,
    onNext: () -> Unit,
    nextButtonFocusRequester: FocusRequester,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.colors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
            shape = ButtonDefaults.shape(RoundedCornerShape(12.dp))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("BATAL")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (currentStep !is WizardStep.InputDimensions) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.colors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White),
                    shape = ButtonDefaults.shape(RoundedCornerShape(12.dp))
                ) {
                    Text("KEMBALI")
                }
            }

            if (currentStep is WizardStep.EstimationSummary) {
                Button(
                    onClick = {
                        val estimation = calcViewModel.generateEstimation(
                            mainViewModel.calcSystemConfigs.value,
                            mainViewModel.laborConfigs.value
                        )
                        mainViewModel.saveEstimation(estimation)
                    },
                    modifier = Modifier.focusRequester(nextButtonFocusRequester),
                    colors = ButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = Color.Black),
                    shape = ButtonDefaults.shape(RoundedCornerShape(12.dp))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SIMPAN ESTIMASI", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = onNext,
                    modifier = Modifier.focusRequester(nextButtonFocusRequester),
                    colors = ButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                    shape = ButtonDefaults.shape(RoundedCornerShape(12.dp))
                ) {
                    Text("LANJUTKAN")
                }
            }
        }
    }
}
