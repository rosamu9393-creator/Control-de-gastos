package com.example.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Expense
import com.example.ui.theme.CategoryComida
import com.example.ui.theme.CategoryGustitos
import com.example.ui.theme.CategoryOtros
import com.example.ui.theme.CategorySalidas
import com.example.ui.theme.CategorySalud
import com.example.ui.theme.CategoryServicios
import com.example.ui.theme.CategorySupermercado
import com.example.ui.theme.CategoryTransporte
import java.util.Locale

@Composable
fun AnalyticsScreen(
    expenses: List<Expense>,
    modifier: Modifier = Modifier
) {
    val totalAmount = expenses.sumOf { it.amount }
    val expenseCount = expenses.size

    // Group by category
    val categoryTotals = expenses
        .groupBy { it.category.lowercase() }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    // Group by payment method
    val methodCounts = expenses
        .groupBy { it.paymentMethod.lowercase() }
        .mapValues { entry -> entry.value.size }
        .toList()
        .sortedByDescending { it.second }

    val topCategory = categoryTotals.firstOrNull()
    val topMethod = methodCounts.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("analytics_screen")
    ) {
        Text(
            text = "Tus Hábitos y Resumen",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Información neutra y clara para entender a dónde va tu dinero",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Summary Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Total registrado",
                value = "S/. ${String.format(Locale.US, "%.2f", totalAmount)}",
                subtitle = "$expenseCount movimientos",
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Promedio por gasto",
                value = if (expenseCount > 0) "S/. ${String.format(Locale.US, "%.2f", totalAmount / expenseCount)}" else "S/. 0.00",
                subtitle = "Ritmo habitual",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Kind & Neutral Habit Observation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Observación de hábitos",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val habitText = if (expenses.isEmpty()) {
                        "Aún no hay gastos registrados en este período. Escribe tu primer gasto en la barra de abajo."
                    } else if (topCategory != null && totalAmount > 0) {
                        val pct = ((topCategory.second / totalAmount) * 100).toInt()
                        val topCatName = getCategoryName(topCategory.first)
                        val topMethodName = topMethod?.first?.replaceFirstChar { it.uppercase() } ?: "efectivo"
                        "Tu categoría con mayor volumen es $topCatName ($pct% del total). Además, sueles pagar con $topMethodName con frecuencia. ¡Conocer tus números es el primer paso!"
                    } else {
                        "¡Buen registro! Cada gasto que anotas te da mayor claridad."
                    }
                    Text(
                        text = habitText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Category Breakdown
        Text(
            text = "Distribución por Categorías",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (categoryTotals.isEmpty()) {
            Text(
                text = "No hay datos para mostrar en este período.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    categoryTotals.forEach { (catKey, amount) ->
                        val pct = if (totalAmount > 0) (amount / totalAmount).toFloat() else 0f
                        val color = getCategoryColor(catKey)
                        val emoji = getCategoryEmoji(catKey)
                        val name = getCategoryName(catKey)

                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = emoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                                Text(
                                    text = "S/. ${String.format(Locale.US, "%.2f", amount)} (${(pct * 100).toInt()}%)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Payment Method Breakdown
        if (methodCounts.isNotEmpty()) {
            Text(
                text = "Métodos de Pago Más Usados",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    methodCounts.take(4).forEach { (method, count) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = method.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

private fun getCategoryEmoji(cat: String): String = when (cat.lowercase()) {
    "comida" -> "🍽️"
    "transporte" -> "🚕"
    "supermercado" -> "🛒"
    "servicios" -> "💡"
    "salidas" -> "🎉"
    "salud" -> "💊"
    "gustitos" -> "☕"
    else -> "📦"
}

private fun getCategoryName(cat: String): String = when (cat.lowercase()) {
    "comida" -> "Comida"
    "transporte" -> "Transporte"
    "supermercado" -> "Supermercado"
    "servicios" -> "Servicios"
    "salidas" -> "Salidas"
    "salud" -> "Salud"
    "gustitos" -> "Gustitos"
    else -> "Otros"
}

private fun getCategoryColor(cat: String): Color = when (cat.lowercase()) {
    "comida" -> CategoryComida
    "transporte" -> CategoryTransporte
    "supermercado" -> CategorySupermercado
    "servicios" -> CategoryServicios
    "salidas" -> CategorySalidas
    "salud" -> CategorySalud
    "gustitos" -> CategoryGustitos
    else -> CategoryOtros
}
