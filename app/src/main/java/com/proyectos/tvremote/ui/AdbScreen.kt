@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.proyectos.tvremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

sealed class AdbUiState {
    object EnterAddress : AdbUiState()
    object Connecting : AdbUiState()
    data class AwaitingAuthorization(val attempt: Int) : AdbUiState()
    // Reached once, then the host saves the device and navigates to the shared AndroidTvScreen -
    // the pad itself lives there now, not in this screen.
    object Remote : AdbUiState()
    data class Error(val message: String) : AdbUiState()
}

@Composable
fun AdbScreen(
    uiState: AdbUiState,
    initialHost: String,
    initialPort: String,
    onBack: () -> Unit,
    onConnect: (host: String, port: Int) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agregar por ADB") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.TopCenter) {
            when (uiState) {
                is AdbUiState.EnterAddress -> AddressForm(initialHost, initialPort, onConnect)

                is AdbUiState.Connecting -> StatusColumn {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Conectando...")
                }

                is AdbUiState.AwaitingAuthorization -> StatusColumn {
                    Icon(Icons.Filled.Adb, contentDescription = null, modifier = Modifier.height(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Aceptá el popup en la pantalla de la TV", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "\"¿Permitir depuración USB?\" - tocá Permitir/Aceptar ahí.\n" +
                            "Esperando autorización (intento ${uiState.attempt})...",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onCancel) { Text("Cancelar") }
                }

                is AdbUiState.Remote -> StatusColumn {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Conectado, abriendo control remoto...")
                }

                is AdbUiState.Error -> StatusColumn {
                    Text("Error: ${uiState.message}", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRetry) { Text("Reintentar") }
                }
            }
        }
    }
}

@Composable
private fun StatusColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
}

@Composable
private fun AddressForm(initialHost: String, initialPort: String, onConnect: (String, Int) -> Unit) {
    var host by remember { mutableStateOf(initialHost) }
    var port by remember { mutableStateOf(initialPort) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Para TV boxes genéricas (AliExpress, etc.)", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Usá esto si la TV no aparece por red ni por Bluetooth. Activá \"Depuración USB\" " +
                        "y \"Depuración inalámbrica\" en Opciones de desarrollador de la TV box, y anotá " +
                        "la IP que muestra ahí (normalmente queda escuchando en el puerto 5555).",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("IP de la TV box") },
            placeholder = { Text("192.168.0.172") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = port,
            onValueChange = { port = it.filter(Char::isDigit) },
            label = { Text("Puerto") },
            placeholder = { Text("5555") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val p = port.toIntOrNull() ?: 5555
                if (host.isNotBlank()) onConnect(host.trim(), p)
            },
            enabled = host.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Conectar") }
    }
}
