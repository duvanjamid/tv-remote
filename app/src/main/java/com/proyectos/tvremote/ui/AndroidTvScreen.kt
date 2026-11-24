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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.proyectos.tvremote.androidtv.AndroidTvPairingClient
import com.proyectos.tvremote.androidtv.AndroidTvRemoteState

sealed class AndroidTvUiState {
    // message carries ADB's "aceptá el popup en la TV" hint while the cert flow just says "Conectando...".
    data class Connecting(val message: String? = null) : AndroidTvUiState()
    data class AwaitingPin(val error: String? = null) : AndroidTvUiState()
    object Remote : AndroidTvUiState()
    data class Error(val message: String) : AndroidTvUiState()
}

@Composable
fun AndroidTvScreen(
    host: String,
    uiState: AndroidTvUiState,
    onBack: () -> Unit,
    onSubmitPin: (String) -> Unit,
    onKey: (String) -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Android TV · $host") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (uiState) {
                is AndroidTvUiState.Connecting -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(uiState.message ?: "Conectando...")
                }

                is AndroidTvUiState.AwaitingPin -> PinDialog(error = uiState.error, onSubmit = onSubmitPin)

                is AndroidTvUiState.Remote -> AndroidTvPad(onKey = onKey)

                is AndroidTvUiState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: ${uiState.message}")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRetry) { Text("Reintentar") }
                }
            }
        }
    }
}

@Composable
private fun PinDialog(error: String?, onSubmit: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Código en la TV") },
        text = {
            Column {
                Text("Escribí el código de 6 caracteres que muestra la TV.")
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = pin, onValueChange = { if (it.length <= 6) pin = it }, label = { Text("Código") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(pin) }, enabled = pin.length == 6) { Text("Confirmar") }
        }
    )
}

@Composable
private fun AndroidTvPad(onKey: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            IconButton(onClick = { onKey("HOME") }) { Icon(Icons.Filled.Home, contentDescription = "Home") }
            IconButton(onClick = { onKey("POWER") }) { Icon(Icons.Filled.PowerSettingsNew, contentDescription = "Power") }
        }
        Spacer(Modifier.height(16.dp))

        FilledIconButton(onClick = { onKey("DPAD_UP") }, modifier = Modifier.size(64.dp)) {
            Icon(Icons.Filled.ArrowUpward, contentDescription = "Arriba")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilledIconButton(onClick = { onKey("DPAD_LEFT") }, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Izquierda")
            }
            FilledIconButton(onClick = { onKey("DPAD_CENTER") }, modifier = Modifier.size(64.dp)) {
                Text("OK")
            }
            FilledIconButton(onClick = { onKey("DPAD_RIGHT") }, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Derecha")
            }
        }
        FilledIconButton(onClick = { onKey("DPAD_DOWN") }, modifier = Modifier.size(64.dp)) {
            Icon(Icons.Filled.ArrowDownward, contentDescription = "Abajo")
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { onKey("BACK") }) { Text("Atrás") }
            Button(onClick = { onKey("MENU") }) { Text("Menú") }
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            IconButton(onClick = { onKey("VOLUME_DOWN") }) { Icon(Icons.Filled.VolumeDown, contentDescription = "Vol -") }
            IconButton(onClick = { onKey("VOLUME_MUTE") }) { Icon(Icons.Filled.VolumeOff, contentDescription = "Mute") }
            IconButton(onClick = { onKey("VOLUME_UP") }) { Icon(Icons.Filled.VolumeUp, contentDescription = "Vol +") }
        }
    }
}
