package com.proyectos.tvremote.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DeviceSettingsDialog(
    currentName: String,
    discoveredMacs: List<String>,
    themeMode: String,
    keepScreenOn: Boolean,
    hapticsEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onForgetDevice: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var confirmForget by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustes") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Esta TV", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") })
                Spacer(Modifier.height(12.dp))
                Text("MAC (para encender por WiFi)", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                if (discoveredMacs.isEmpty()) {
                    Text(
                        "Todavía no detectada. Conectate una vez con la TV encendida y queda guardada sola.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    discoveredMacs.forEach { mac -> Text(mac, style = MaterialTheme.typography.bodyMedium) }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Detectada sola, no hace falta escribirla. Activá \"Turn on via WiFi\"/\"Quick Start+\" en la TV.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(8.dp))
                if (confirmForget) {
                    Text("¿Olvidar esta TV? Se borra el emparejamiento, caché y favoritos guardados.", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { confirmForget = false; onForgetDevice() }) { Text("Sí, olvidar") }
                        TextButton(onClick = { confirmForget = false }) { Text("No") }
                    }
                } else {
                    TextButton(onClick = { confirmForget = true }) { Text("Olvidar esta TV") }
                }

                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))

                Text("General (toda la app)", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Text("Tema", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = themeMode == "system", onClick = { onThemeModeChange("system") }, label = { Text("Sistema") })
                    FilterChip(selected = themeMode == "light", onClick = { onThemeModeChange("light") }, label = { Text("Claro") })
                    FilterChip(selected = themeMode == "dark", onClick = { onThemeModeChange("dark") }, label = { Text("Oscuro") })
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Mantener pantalla encendida", modifier = Modifier.weight(1f))
                    Switch(checked = keepScreenOn, onCheckedChange = onKeepScreenOnChange)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Vibrar al tocar botones", modifier = Modifier.weight(1f))
                    Switch(checked = hapticsEnabled, onCheckedChange = onHapticsChange)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

