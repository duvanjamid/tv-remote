package com.proyectos.tvremote.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * A physical-remote-style vertical rocker: one tall pill, top half is "+"
 * (increment), bottom half is "-" (decrement), split by a thin divider.
 * This is how volume/channel controls work on a real TV remote - much
 * easier to hit reliably than a thin slider.
 */
@Composable
fun RockerButton(
    modifier: Modifier = Modifier,
    label: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
        Column(
            modifier = Modifier
                .width(64.dp)
                .height(160.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onIncrement),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = "$label +", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 1.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDecrement),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "$label -", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
