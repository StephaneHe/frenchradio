package com.steph.frenchradio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.steph.frenchradio.player.AudioBoostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioBoostSheet(
    boostPercent: Int,
    onBoostChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        AudioBoostContent(boostPercent = boostPercent, onBoostChange = onBoostChange)
    }
}

@Composable
fun AudioBoostContent(
    boostPercent: Int,
    onBoostChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 32.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.GraphicEq, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Booster le son",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.weight(1f))
            val gainDb = AudioBoostController.percentToGainDb(boostPercent)
            Text(
                text = if (gainDb > 0) "+$gainDb dB" else "0 dB",
                style = MaterialTheme.typography.titleMedium,
                color = if (gainDb > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Slider(
            value = boostPercent.toFloat(),
            onValueChange = { onBoostChange(it.toInt()) },
            valueRange = 0f..100f,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "$boostPercent %",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { onBoostChange(0) }) {
                Text("Réinitialiser")
            }
        }
    }
}
