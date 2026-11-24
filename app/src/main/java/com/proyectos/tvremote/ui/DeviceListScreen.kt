@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.proyectos.tvremote.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.proyectos.tvremote.discovery.DeviceType
import com.proyectos.tvremote.discovery.DiscoveredDevice
import com.proyectos.tvremote.ui.discovery.JarvisOrb

@Composable
fun DeviceListScreen(
    devices: List<DiscoveredDevice>,
    scanning: Boolean,
    onScan: () -> Unit,
    onSelect: (DiscoveredDevice) -> Unit,
    onAddBluetooth: () -> Unit,
    onAddAdb: () -> Unit
) {
    var showManualAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("TV Remote") }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            val orbSize by animateDpAsState(
                targetValue = if (scanning) 180.dp else 88.dp,
                animationSpec = tween(durationMillis = 450),
                label = "orbSize"
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                JarvisOrb(
                    scanning = scanning,
                    size = orbSize,
                    primaryColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    secondaryColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                    tertiaryColor = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(Modifier.height(12.dp))

            Button(onClick = onScan, modifier = Modifier.fillMaxWidth(), enabled = !scanning) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text(if (scanning) "  Buscando..." else "  Buscar TVs en la red")
            }
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.OutlinedButton(onClick = { showManualAdd = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("  Agregar por IP (si no aparece sola)")
            }
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.OutlinedButton(onClick = onAddBluetooth, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Bluetooth, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("  Agregar por Bluetooth (modo limitado)")
            }
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.OutlinedButton(onClick = onAddAdb, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Adb, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("  Agregar por ADB (TV box genérica)")
            }
            Spacer(Modifier.height(16.dp))

            if (devices.isEmpty() && !scanning) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin TVs encontradas.\nTocá \"Buscar TVs\" con el celu en la misma WiFi que la TV.")
                }
            } else {
                LazyColumn {
                    items(devices, key = { it.id }) { device ->
                        var visible by remember(device.id) { mutableStateOf(false) }
                        LaunchedEffect(device.id) { visible = true }
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 }
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                onClick = { onSelect(device) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.Surface(
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                iconFor(device.type),
                                                contentDescription = null,
                                                tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(device.name, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                                        Spacer(Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            androidx.compose.material3.AssistChip(
                                                onClick = { onSelect(device) },
                                                label = { Text(typeLabelFor(device.type), style = androidx.compose.material3.MaterialTheme.typography.labelSmall) },
                                                modifier = Modifier.height(24.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                device.host,
                                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.Filled.KeyboardArrowRight,
                                        contentDescription = if (device.type == DeviceType.CAST_ONLY) "Ver" else "Conectar",
                                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManualAdd) {
        ManualAddDialog(
            onDismiss = { showManualAdd = false },
            onAdd = { ip, type ->
                showManualAdd = false
                onSelect(
                    DiscoveredDevice(
                        id = "manual:${type.name}:$ip",
                        name = if (type == DeviceType.WEBOS) "TV webOS ($ip)" else "Android TV ($ip)",
                        host = ip,
                        port = if (type == DeviceType.WEBOS) 3000 else 6467,
                        type = type
                    )
                )
            }
        )
    }
}

@Composable
private fun ManualAddDialog(onDismiss: () -> Unit, onAdd: (ip: String, type: DeviceType) -> Unit) {
    var ip by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(DeviceType.WEBOS) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar TV por IP") },
        text = {
            Column {
                Text("Usá esto si la búsqueda automática no encuentra tu TV (red distinta, firewall, etc).")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP de la TV") },
                    placeholder = { Text("192.168.0.185") },
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == DeviceType.WEBOS, onClick = { type = DeviceType.WEBOS }, label = { Text("LG webOS") })
                    FilterChip(selected = type == DeviceType.ANDROID_TV, onClick = { type = DeviceType.ANDROID_TV }, label = { Text("Android TV") })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (ip.isNotBlank()) onAdd(ip.trim(), type) }, enabled = ip.isNotBlank()) {
                Text("Conectar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun typeLabelFor(type: DeviceType): String = when (type) {
    DeviceType.WEBOS -> "webOS"
    DeviceType.ANDROID_TV -> "Android TV"
    DeviceType.CAST_ONLY -> "Cast"
    DeviceType.ANDROID_ADB -> "ADB"
}

private fun subtitleFor(device: DiscoveredDevice): String = when (device.type) {
    DeviceType.WEBOS -> "LG webOS · ${device.host}"
    DeviceType.ANDROID_TV -> "Android TV · ${device.host}"
    DeviceType.CAST_ONLY -> "Solo Cast (sin control remoto completo) · ${device.host}"
    DeviceType.ANDROID_ADB -> "TV box por ADB · ${device.host}"
}

private fun iconFor(type: DeviceType) = when (type) {
    DeviceType.WEBOS -> Icons.Filled.LiveTv
    DeviceType.ANDROID_TV -> Icons.Filled.SmartDisplay
    DeviceType.CAST_ONLY -> Icons.Filled.Cast
    DeviceType.ANDROID_ADB -> Icons.Filled.Adb
}
