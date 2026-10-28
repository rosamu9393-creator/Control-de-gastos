package com.example.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Expense

val allCategories = listOf(
    "comida" to "🍽️ Comida",
    "transporte" to "🚕 Transporte",
    "supermercado" to "🛒 Supermercado",
    "servicios" to "💡 Servicios",
    "salidas" to "🎉 Salidas",
    "salud" to "💊 Salud",
    "gustitos" to "☕ Gustitos",
    "otros" to "📦 Otros"
)

val allPaymentMethods = listOf(
    "yape" to "Yape",
    "plin" to "Plin",
    "efectivo" to "Efectivo",
    "tarjeta" to "Tarjeta",
    "transferencia" to "Transferencia"
)

@Composable
fun EditExpenseDialog(
    expense: Expense,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    var amountText by remember { mutableStateOf(expense.amount.toString()) }
    var description by remember { mutableStateOf(expense.description) }
    var selectedCategory by remember { mutableStateOf(expense.category) }
    var selectedMethod by remember { mutableStateOf(expense.paymentMethod) }
    var selectedCurrency by remember { mutableStateOf(expense.currency) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar Gasto",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Currency & Amount
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Monto") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_expense_amount_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_expense_desc_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Categoría:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allCategories.forEach { (catKey, catLabel) ->
                        FilterChip(
                            selected = selectedCategory.equals(catKey, ignoreCase = true),
                            onClick = { selectedCategory = catKey },
                            label = { Text(catLabel) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Método de Pago:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allPaymentMethods.forEach { (methodKey, methodLabel) ->
                        FilterChip(
                            selected = selectedMethod.equals(methodKey, ignoreCase = true),
                            onClick = { selectedMethod = methodKey },
                            label = { Text(methodLabel) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amountText.replace(',', '.').toDoubleOrNull() ?: expense.amount
                    onSave(
                        expense.copy(
                            amount = amountVal,
                            description = description,
                            category = selectedCategory,
                            paymentMethod = selectedMethod,
                            currency = selectedCurrency
                        )
                    )
                },
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
