package com.example.renovasritv

import net.objecthunter.exp4j.ExpressionBuilder
import java.text.NumberFormat
import java.util.*

object CalculationEngine {

    /**
     * Mengevaluasi rumus string dengan variabel dinamis.
     * Contoh: formula="(P*L)", variables={"P": 5.0, "L": 4.0} -> 20.0
     */
    fun evaluateFormula(formula: String, variables: Map<String, Double>): Double {
        return try {
            if (formula.isBlank()) return 0.0
            
            // Normalisasi rumus dan variabel ke UPPERCASE untuk menghindari case-sensitivity
            val normalizedFormula = formula.uppercase()
            val normalizedVariables = variables.mapKeys { it.key.uppercase() }
            
            val builder = ExpressionBuilder(normalizedFormula)
            
            // Daftarkan hanya variabel yang benar-benar ada dalam rumus untuk menghindari error exp4j
            val variableNames = normalizedVariables.keys.toTypedArray()
            if (variableNames.isNotEmpty()) {
                builder.variables(*variableNames)
            }
            
            val expression = builder.build()
            expression.setVariables(normalizedVariables)
            
            val result = expression.evaluate()
            // Pastikan tidak mengembalikan nilai negatif atau NaN
            if (result.isNaN() || result < 0) 0.0 else result
        } catch (e: Exception) {
            println("Error evaluating formula: $formula | Variables: $variables | Error: ${e.message}")
            0.0
        }
    }

    /**
     * Memformat angka ke mata uang berdasarkan konfigurasi global.
     */
    fun formatCurrency(
        amount: Double, 
        currencyCode: String = "IDR", 
        locale: Locale = Locale("id", "ID")
    ): String {
        return try {
            val format = NumberFormat.getCurrencyInstance(locale)
            format.currency = Currency.getInstance(currencyCode)
            format.maximumFractionDigits = 0
            format.format(amount)
        } catch (e: Exception) {
            "$currencyCode ${String.format("%,.0f", amount)}"
        }
    }

    /**
     * Memformat mata uang ke bentuk pendek (misal: 1.2jt atau 500rb)
     */
    fun formatCurrencyShort(amount: Double): String {
        return when {
            amount >= 1_000_000 -> String.format(Locale.US, "%.1f", amount / 1_000_000.0) + "jt"
            amount >= 1_000 -> String.format(Locale.US, "%.0f", amount / 1_000.0) + "rb"
            else -> formatCurrency(amount)
        }
    }
}
