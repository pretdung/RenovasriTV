package com.example.renovasritv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*

@Composable
fun NumericStepper(
    label: String,
    value: Float,
    unit: String = "",
    min: Float = 0f,
    max: Float = 100f,
    step: Float = 0.1f,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            // Button Minus
            Surface(
                onClick = { if (value > min) onValueChange(value - step) },
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(20.dp))
                }
            }

            // Value Display
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${String.format("%.1f", value)} $unit",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Button Plus
            Surface(
                onClick = { if (value < max) onValueChange(value + step) },
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun DynamicInputForm(
    variables: List<CalcVariable>,
    inputStates: MutableMap<String, Float>,
    onValuesChanged: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        variables.forEach { variable ->
            var currentValue by remember { mutableFloatStateOf(inputStates[variable.variableKey] ?: variable.minValue) }
            
            NumericStepper(
                label = variable.label,
                value = currentValue,
                unit = variable.unit ?: "",
                min = variable.minValue,
                max = variable.maxValue,
                step = variable.step,
                onValueChange = {
                    currentValue = it
                    inputStates[variable.variableKey] = it
                    onValuesChanged()
                }
            )
        }
    }
}
