@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.proyectos.tvremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.proyectos.tvremote.bluetooth.BluetoothHidState

@Composable
fun BluetoothScreen(
    state: BluetoothHidState,
    onBack: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth (modo limitado)") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Solo botones básicos", fontWeight = FontWeight.Bold)
                    Text(
                        "Flechas, OK, atrás, home, volumen y power. Sin apps, canales, " +
                            "entradas ni encendido remoto (Wake-on-LAN) - eso necesita WiFi.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                when (state) {
                    is BluetoothHidState.NotSupported -> StatusColumn {
                        Text("Este celular o versión de Android no soporta el modo Bluetooth HID (necesita Android 9+).")
                    }

                    is BluetoothHidState.BluetoothOff -> StatusColumn {
                        Icon(Icons.Filled.BluetoothDisabled, contentDescription = null, modifier = Modifier.height(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Bluetooth apagado")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onEnableBluetooth) { Text("Activar Bluetooth") }
                    }

                    is BluetoothHidState.Registering -> StatusColumn {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Registrando perfil de control remoto...")
                    }

                    is BluetoothHidState.WaitingForConnection -> StatusColumn {
                        Icon(Icons.Filled.Bluetooth, contentDescription = null, modifier = Modifier.height(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Esperando que la TV se conecte", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "1. Hacé visible el celular acá abajo.\n" +
                                "2. En la TV: Configuración > Mandos y accesorios (o Bluetooth) > Agregar dispositivo.\n" +
                                "3. Elegí \"${state.localName}\" en la lista de la TV.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onMakeDiscoverable) { Text("Hacer visible el celular (5 min)") }
                    }

                    // Connected is handled by the host, which swaps in the shared AndroidTvScreen pad.
                    is BluetoothHidState.Connected -> {}

                    is BluetoothHidState.Error -> StatusColumn {
                        Text("Error: ${state.message}")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onRetry) { Text("Reintentar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusColumn(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
}
