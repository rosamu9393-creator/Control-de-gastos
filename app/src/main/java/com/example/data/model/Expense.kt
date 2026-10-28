package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.NumberFormat
import java.util.Locale

@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val currency: String = "PEN",
    val category: String = "comida",
    val description: String = "",
    val paymentMethod: String = "efectivo",
    val rawInput: String = "",
    val date: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun formattedAmount(): String {
        val symbol = when (currency) {
            "PEN" -> "S/."
            "USD" -> "$"
            "EUR" -> "€"
            else -> currency
        }
        val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$symbol ${numberFormat.format(amount)}"
    }

    fun categoryEmoji(): String = when (category.lowercase()) {
        "comida" -> "🍽️"
        "transporte" -> "🚕"
        "supermercado" -> "🛒"
        "servicios" -> "💡"
        "salidas" -> "🎉"
        "salud" -> "💊"
        "gustitos" -> "☕"
        else -> "📦"
    }

    fun categoryDisplayName(): String = when (category.lowercase()) {
        "comida" -> "Comida"
        "transporte" -> "Transporte"
        "supermercado" -> "Supermercado"
        "servicios" -> "Servicios"
        "salidas" -> "Salidas y Ocio"
        "salud" -> "Salud"
        "gustitos" -> "Gustitos"
        else -> "Otros"
    }

    fun paymentMethodDisplayName(): String = when (paymentMethod.lowercase()) {
        "yape" -> "Yape"
        "plin" -> "Plin"
        "efectivo" -> "Efectivo"
        "tarjeta" -> "Tarjeta"
        "transferencia" -> "Transferencia"
        else -> "Otro"
    }
}
