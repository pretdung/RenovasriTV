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
            
            val builder = ExpressionBuilder(formula)
            // Daftarkan semua kunci variabel yang ada di rumus
            val variableNames = variables.keys.toTypedArray()
            builder.variables(*variableNames)
            
            val expression = builder.build()
            expression.setVariables(variables)
            
            expression.evaluate()
        } catch (e: Exception) {
            println("Error evaluating formula: $formula | Error: ${e.message}")
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
}
