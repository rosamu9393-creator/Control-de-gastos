package com.example.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "AiExpenseParser"

class AiExpenseParser {

    private val expenseSchema = Schema.obj(
        properties = mapOf(
            "amount" to Schema.double(description = "Monto numérico positivo del gasto"),
            "currency" to Schema.enumeration(listOf("PEN", "USD", "EUR"), description = "Moneda del gasto"),
            "category" to Schema.enumeration(
                listOf("comida", "transporte", "supermercado", "servicios", "salidas", "salud", "gustitos", "otros"),
                description = "Categoría del gasto"
            ),
            "description" to Schema.string(description = "Nombre o concepto breve y claro del gasto"),
            "paymentMethod" to Schema.enumeration(
                listOf("yape", "plin", "efectivo", "tarjeta", "transferencia", "otro"),
                description = "Método de pago inferido"
            )
        )
    )

    private val config = generationConfig {
        responseMimeType = "application/json"
        responseSchema = expenseSchema
    }

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    suspend fun parseWithAiOrFallback(input: String, defaultCurrency: String = "PEN"): ParsedExpense {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return LocalExpenseParser.parse(trimmed, defaultCurrency)
        }

        return try {
            withTimeout(15_000L) {
                val prompt = """
                    Eres el asistente de categorización de la app peruana "Gastos Fácil".
                    Interpreta este gasto escrito en lenguaje natural: "$trimmed".
                    Contexto Perú:
                    - Moneda predeterminada: $defaultCurrency (soles PEN). Si mencionan dólares o $, usa USD. Si mencionan euros, EUR.
                    - Métodos comunes: Yape, Plin, efectivo, tarjeta (Visa, Mastercard, BCP, BBVA, etc.), transferencia.
                    - Categorías: comida (menú, chifa, almuerzo, etc.), transporte (taxi, uber, metropolitano, pasaje, gasolina), supermercado (metro, tottus, plaza vea, bodega, mercado), servicios (luz, agua, internet, alquiler), salidas (cine, bar, fiesta), salud (farmacia, pastillas, doctor), gustitos (café, helado, postre), otros.
                    Extrae el monto, moneda, categoría, descripción concisa y método de pago.
                """.trimIndent()

                val response = model.generateContent(prompt)
                val jsonString = response.text
                if (!jsonString.isNullOrBlank()) {
                    val json = JSONObject(jsonString)
                    val amount = json.optDouble("amount", 0.0)
                    val currency = json.optString("currency", defaultCurrency).uppercase()
                    val category = json.optString("category", "otros").lowercase()
                    val description = json.optString("description", trimmed)
                    val paymentMethod = json.optString("paymentMethod", "efectivo").lowercase()
                    val dateIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                    if (amount > 0) {
                        ParsedExpense(
                            amount = amount,
                            currency = if (currency in listOf("PEN", "USD", "EUR")) currency else defaultCurrency,
                            category = category,
                            description = description,
                            paymentMethod = paymentMethod,
                            date = dateIso,
                            confidence = "ai"
                        )
                    } else {
                        LocalExpenseParser.parse(trimmed, defaultCurrency)
                    }
                } else {
                    LocalExpenseParser.parse(trimmed, defaultCurrency)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "AI parsing unavailable or timed out, using smart local parser: ${e.message}")
            LocalExpenseParser.parse(trimmed, defaultCurrency)
        }
    }
}
