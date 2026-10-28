package com.example.data.ai

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ParsedExpense(
    val amount: Double,
    val currency: String,
    val category: String,
    val description: String,
    val paymentMethod: String,
    val date: String,
    val confidence: String = "local"
)

object LocalExpenseParser {

    private val numberRegex = Regex("""(?i)(?:s\/\.?|soles|sol|\$|usd|dolares|€|eur|euros)?\s*(\d+(?:[.,]\d{1,2})?)\s*(?:s\/\.?|soles|sol|\$|usd|dolares|€|eur|euros)?""")
    private val dateIsoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun parse(input: String, defaultCurrency: String = "PEN"): ParsedExpense {
        val trimmed = input.trim()
        val lower = trimmed.lowercase()

        // 1. Detect Currency
        val currency = when {
            lower.contains("$") || lower.contains("usd") || lower.contains("dolares") || lower.contains("dólares") -> "USD"
            lower.contains("€") || lower.contains("eur") || lower.contains("euro") || lower.contains("euros") -> "EUR"
            else -> defaultCurrency
        }

        // 2. Detect Amount
        var amount = 0.0
        val numberMatch = numberRegex.findAll(lower).firstOrNull()
        if (numberMatch != null) {
            val numStr = numberMatch.groupValues[1].replace(',', '.')
            amount = numStr.toDoubleOrNull() ?: 0.0
        }

        // 3. Detect Payment Method
        val paymentMethod = when {
            lower.contains("yape") || lower.contains("yapeé") || lower.contains("yapee") -> "yape"
            lower.contains("plin") -> "plin"
            lower.contains("visa") || lower.contains("mastercard") || lower.contains("tarjeta") ||
            lower.contains("bcp") || lower.contains("bbva") || lower.contains("interbank") ||
            lower.contains("scotiabank") || lower.contains("debito") || lower.contains("débito") ||
            lower.contains("credito") || lower.contains("crédito") -> "tarjeta"
            lower.contains("transferencia") || lower.contains("transfe") -> "transferencia"
            lower.contains("efectivo") || lower.contains("cash") || lower.contains("sencillo") -> "efectivo"
            else -> "efectivo" // Default in Peru for everyday small transactions
        }

        // 4. Detect Category
        val category = when {
            containsAny(lower, "taxi", "uber", "didi", "cabify", "metropolitano", "corredor", "pasaje", "gasolina", "grifo", "peaje", "colectivo", "combi", "tren", "linea 1", "estacionamiento", "parqueo") -> "transporte"
            containsAny(lower, "metro", "plaza vea", "tottus", "wong", "mercado", "bodega", "tambo", "oxxo", "vivanda", "supermercado", "compras del mes", "abarrotes") -> "supermercado"
            containsAny(lower, "luz", "agua", "internet", "sedapal", "enel", "pluz", "claro", "movistar", "entel", "alquiler", "mantenimiento", "gas", "calidda", "cable", "recibo", "factura") -> "servicios"
            containsAny(lower, "farmacia", "inkafarma", "mifarma", "pastillas", "medicina", "doctor", "consulta", "dentista", "clinica", "clínica", "salud", "remedio", "analisis", "análisis") -> "salud"
            containsAny(lower, "cine", "cineplanet", "cinemark", "bar", "cerveza", "discoteca", "jockey", "salida", "tragos", "amigos", "karaoke", "fiesta", "chelas", "concierto") -> "salidas"
            containsAny(lower, "café", "cafe", "starbucks", "helado", "postre", "gustito", "snack", "chocman", "chocolate", "antojo", "galletas", "dulce", "bubble tea", "boba") -> "gustitos"
            containsAny(lower, "menú", "menu", "almuerzo", "cena", "desayuno", "pollo", "ceviche", "chicha", "comida", "pizza", "hamburguesa", "chifa", "makis", "sushi", "sangucheria", "pan", "panaderia", "chicharron", "emoliente", "anticucho", "restaurante") -> "comida"
            else -> "otros"
        }

        // 5. Clean Description
        var cleanDesc = trimmed
        // Remove detected amount string
        if (numberMatch != null) {
            cleanDesc = cleanDesc.replace(numberMatch.value, "", ignoreCase = true)
        }
        // Remove currency words and method words if trailing/leading
        val stopWords = listOf("soles", "sol", "s/.", "s/", "usd", "dolares", "dólares", "euros", "en efectivo", "por yape", "con yape", "yape", "plin", "con visa", "en visa", "tarjeta", "efectivo")
        for (w in stopWords) {
            cleanDesc = Regex("(?i)\\b" + Regex.escape(w) + "\\b").replace(cleanDesc, "")
        }
        cleanDesc = cleanDesc.replace(Regex("""\s+"""), " ").trim()
        if (cleanDesc.isBlank()) {
            cleanDesc = when (category) {
                "comida" -> "Gasto de comida"
                "transporte" -> "Transporte"
                "supermercado" -> "Compras"
                "servicios" -> "Servicio"
                "salidas" -> "Salida"
                "salud" -> "Salud"
                "gustitos" -> "Gustito"
                else -> "Gasto general"
            }
        } else {
            cleanDesc = cleanDesc.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }

        return ParsedExpense(
            amount = amount,
            currency = currency,
            category = category,
            description = cleanDesc,
            paymentMethod = paymentMethod,
            date = dateIsoFormat.format(Date()),
            confidence = "local"
        )
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { kw ->
            text.contains(kw)
        }
    }
}
