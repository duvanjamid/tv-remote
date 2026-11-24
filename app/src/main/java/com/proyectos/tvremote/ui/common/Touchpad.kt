package com.proyectos.tvremote.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * A drag surface that mirrors the official webOS Magic Remote pointer:
 * drag to move the on-screen cursor, tap to click/select.
 */
@Composable
fun Touchpad(
    modifier: Modifier = Modifier,
    onMove: (dx: Float, dy: Float) -> Unit,
    onTap: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onMove(dragAmount.x, dragAmount.y)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { onTap() }
            },
        contentAlignment = Alignment.Center
    ) {
        Text("Touchpad · arrastrá para mover, tocá para OK", style = MaterialTheme.typography.bodySmall)
    }
}
