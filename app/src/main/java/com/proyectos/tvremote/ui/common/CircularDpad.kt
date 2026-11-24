package com.proyectos.tvremote.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * A real-remote-style circular d-pad: four directional wedges around a
 * central OK button, all inside one ring. Looks like a physical TV remote
 * instead of four separate square buttons floating in a cross.
 */
@Composable
fun CircularDpad(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 220.dp,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onCenter: () -> Unit
) {
    val ringColor = MaterialTheme.colorScheme.surfaceVariant
    val centerColor = MaterialTheme.colorScheme.primary
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(ringColor),
        contentAlignment = Alignment.Center
    ) {
        DpadButton(
            modifier = Modifier.align(Alignment.TopCenter).size(size / 3),
            onClick = onUp
        ) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Arriba", tint = iconColor) }

        DpadButton(
            modifier = Modifier.align(Alignment.BottomCenter).size(size / 3),
            onClick = onDown
        ) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Abajo", tint = iconColor) }

        DpadButton(
            modifier = Modifier.align(Alignment.CenterStart).size(size / 3),
            onClick = onLeft
        ) { Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Izquierda", tint = iconColor) }

        DpadButton(
            modifier = Modifier.align(Alignment.CenterEnd).size(size / 3),
            onClick = onRight
        ) { Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Derecha", tint = iconColor) }

        Box(
            modifier = Modifier
                .size(size / 2.6f)
                .clip(CircleShape)
                .background(centerColor)
                .clickable(interactionSource = remember(), indication = null, onClick = onCenter),
            contentAlignment = Alignment.Center
        ) {
            Text("OK", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun remember(): MutableInteractionSource = androidx.compose.runtime.remember { MutableInteractionSource() }

@Composable
private fun DpadButton(modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(interactionSource = remember(), indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
