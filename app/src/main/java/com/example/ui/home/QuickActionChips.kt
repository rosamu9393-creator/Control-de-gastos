package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class QuickPreset(
    val label: String,
    val textToSubmit: String,
    val iconEmoji: String
)

val defaultQuickPresets = listOf(
    QuickPreset("Menú S/ 15", "15 menú almuerzo", "🍽️"),
    QuickPreset("Taxi S/ 12", "taxi 12", "🚕"),
    QuickPreset("Café S/ 8", "café 8", "☕"),
    QuickPreset("Yape S/ 20", "yape 20", "💜"),
    QuickPreset("Pasaje S/ 3.50", "pasaje metropolitano 3.50", "🚌"),
    QuickPreset("Panadería S/ 4", "panadería 4", "🥖"),
    QuickPreset("Plin S/ 15", "plin 15", "💙"),
    QuickPreset("Bodega S/ 10", "bodega compras 10", "🛒")
)

@Composable
fun QuickActionChips(
    onPresetClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Rápidos:",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp)
        )

        defaultQuickPresets.forEach { preset ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onPresetClicked(preset.textToSubmit) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("quick_preset_${preset.label.replace(" ", "_")}"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = preset.iconEmoji, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = preset.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
    }
}
