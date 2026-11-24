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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsInputHdmi
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.proyectos.tvremote.ui.common.CircularDpad
import com.proyectos.tvremote.ui.common.RockerButton
import com.proyectos.tvremote.ui.common.Touchpad
import com.proyectos.tvremote.ui.common.rememberInsecureImageLoader
import com.proyectos.tvremote.webos.PairingState
import com.proyectos.tvremote.webos.VolumeStatus
import com.proyectos.tvremote.webos.WebOsApp
import com.proyectos.tvremote.webos.WebOsChannel
import com.proyectos.tvremote.webos.WebOsInput

@Composable
fun WebOsScreen(
    host: String,
    displayName: String,
    state: PairingState,
    volume: VolumeStatus?,
    apps: List<WebOsApp>,
    appsLoading: Boolean,
    favoriteIds: Set<String>,
    recentApps: List<WebOsApp>,
    inputs: List<WebOsInput>,
    inputsLoading: Boolean,
    channels: List<WebOsChannel>,
    channelsLoading: Boolean,
    onBack: () -> Unit,
    onButton: (String) -> Unit,
    onPowerOff: () -> Unit,
    onPowerOn: () -> Unit,
    onRetry: () -> Unit,
    onToggleMute: () -> Unit,
    onLaunchApp: (WebOsApp) -> Unit,
    onToggleFavorite: (WebOsApp) -> Unit,
    onSwitchInput: (WebOsInput) -> Unit,
    onSwitchChannel: (WebOsChannel) -> Unit,
    onLoadApps: () -> Unit,
    onLoadInputs: () -> Unit,
    onLoadChannels: () -> Unit,
    onOpenStore: () -> Unit,
    onSendToast: () -> Unit,
    onSendText: (String) -> Unit,
    onSendEnter: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onOpenSettings: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "Ajustes") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                is PairingState.Connecting -> CenteredMessage("Conectando...")
                is PairingState.AwaitingPrompt -> CenteredMessage("Aceptá la conexión en la pantalla de la TV")
                is PairingState.Failed -> CenteredError(state.reason, onRetry)
                is PairingState.Idle -> CenteredMessage("...")
                is PairingState.Paired -> {
                    androidx.compose.material3.ScrollableTabRow(
                        selectedTabIndex = tab,
                        edgePadding = 12.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = tab == 0, onClick = { tab = 0 },
                            icon = { Icon(Icons.Filled.SettingsRemote, contentDescription = null) },
                            text = { Text("Control") }
                        )
                        Tab(
                            selected = tab == 1, onClick = { tab = 1 },
                            icon = { Icon(Icons.Filled.History, contentDescription = null) },
                            text = { Text("Recientes") }
                        )
                        Tab(
                            selected = tab == 2, onClick = { tab = 2; onLoadApps() },
                            icon = { Icon(Icons.Filled.Apps, contentDescription = null) },
                            text = { Text("Apps") }
                        )
                        Tab(
                            selected = tab == 3, onClick = { tab = 3; onLoadInputs() },
                            icon = { Icon(Icons.Filled.SettingsInputHdmi, contentDescription = null) },
                            text = { Text("Entradas") }
                        )
                        Tab(
                            selected = tab == 4, onClick = { tab = 4; onLoadChannels() },
                            icon = { Icon(Icons.Filled.LiveTv, contentDescription = null) },
                            text = { Text("Canales") }
                        )
                    }
                    when (tab) {
                        0 -> ControlTab(
                            volume = volume,
                            onButton = onButton,
                            onPowerOff = onPowerOff,
                            onPowerOn = onPowerOn,
                            onToggleMute = onToggleMute,
                            onSendToast = onSendToast,
                            onSendText = onSendText,
                            onSendEnter = onSendEnter,
                            onMove = onMove
                        )
                        1 -> RecentsTab(apps = recentApps, onLaunchApp = onLaunchApp)
                        2 -> AppsTab(
                            apps = apps,
                            loading = appsLoading,
                            favoriteIds = favoriteIds,
                            onLaunchApp = onLaunchApp,
                            onToggleFavorite = onToggleFavorite,
                            onRetry = onLoadApps,
                            onOpenStore = onOpenStore
                        )
                        3 -> InputsTab(inputs = inputs, loading = inputsLoading, onSwitchInput = onSwitchInput, onRetry = onLoadInputs)
                        4 -> ChannelsTab(channels = channels, loading = channelsLoading, onSwitchChannel = onSwitchChannel, onRetry = onLoadChannels)
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(text)
        }
    }
}

@Composable
private fun CenteredError(reason: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Error: $reason")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Reintentar") }
        }
    }
}

@Composable
private fun ControlTab(
    volume: VolumeStatus?,
    onButton: (String) -> Unit,
    onPowerOff: () -> Unit,
    onPowerOn: () -> Unit,
    onToggleMute: () -> Unit,
    onSendToast: () -> Unit,
    onSendText: (String) -> Unit,
    onSendEnter: () -> Unit,
    onMove: (Float, Float) -> Unit
) {
    var showKeypad by remember { mutableStateOf(false) }
    var showTouchpad by remember { mutableStateOf(false) }
    var showTextKeyboard by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Power row
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onPowerOn, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Encender")
            }
            Button(onClick = onPowerOff, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Apagar")
            }
        }

        Spacer(Modifier.height(20.dp))

        // Small utility row - frequent-but-not-huge actions. Home/Back get their own big
        // dedicated buttons below instead (those are the ones you tap constantly).
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            IconButton(onClick = { showTouchpad = !showTouchpad }) { Icon(Icons.Filled.Mouse, contentDescription = "Touchpad") }
            IconButton(onClick = { showKeypad = !showKeypad }) { Icon(Icons.Filled.Dialpad, contentDescription = "Teclado numérico") }
            IconButton(onClick = { showTextKeyboard = true }) { Icon(Icons.Filled.Keyboard, contentDescription = "Escribir texto") }
            IconButton(onClick = onToggleMute) {
                Icon(
                    if (volume?.muted == true) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Mute"
                )
            }
        }

        if (showTextKeyboard) {
            TextKeyboardDialog(
                onDismiss = { showTextKeyboard = false },
                onSend = onSendText,
                onEnter = onSendEnter
            )
        }

        Spacer(Modifier.height(16.dp))

        if (showTouchpad) {
            Touchpad(onMove = onMove, onTap = { onButton("ENTER") })
            Spacer(Modifier.height(16.dp))
        }

        // Real-remote layout: volume rocker (left) · d-pad (center) · channel rocker (right)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            RockerButton(
                label = "VOL",
                onIncrement = { onButton("VOLUMEUP") },
                onDecrement = { onButton("VOLUMEDOWN") }
            )

            CircularDpad(
                onUp = { onButton("UP") },
                onDown = { onButton("DOWN") },
                onLeft = { onButton("LEFT") },
                onRight = { onButton("RIGHT") },
                onCenter = { onButton("ENTER") }
            )

            RockerButton(
                label = "CH",
                onIncrement = { onButton("CHANNELUP") },
                onDecrement = { onButton("CHANNELDOWN") }
            )
        }

        Spacer(Modifier.height(20.dp))

        // Back and Home are the two most-tapped buttons on a real remote (Google TV remote
        // included) - big, side by side, right under the d-pad, not buried in a chip row.
        BackHomeRow(onBack = { onButton("BACK") }, onHome = { onButton("HOME") })

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AssistChip(onClick = { onButton("EXIT") }, label = { Text("Salir") })
            AssistChip(onClick = { onButton("MENU") }, label = { Text("Menú") })
        }

        Spacer(Modifier.height(24.dp))

        MediaTransportBar(onButton = onButton)

        Spacer(Modifier.height(20.dp))
        AssistChip(onClick = onSendToast, label = { Text("Enviar mensaje a la TV") })

        if (showKeypad) {
            Spacer(Modifier.height(20.dp))
            NumericKeypad(onDigit = { onButton(it) })
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** Small read-out under the volume rocker: mute toggle + current level, no fiddly slider. */
@Composable
private fun MuteAndLevel(volume: VolumeStatus?, onToggleMute: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val muted = volume?.muted == true
        val level = volume?.volume ?: 0
        IconButton(onClick = onToggleMute, modifier = Modifier.size(36.dp)) {
            Icon(
                if (muted) Icons.AutoMirrored.Filled.VolumeOff
                else if (level < 30) Icons.AutoMirrored.Filled.VolumeDown
                else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Mute",
                modifier = Modifier.size(18.dp)
            )
        }
        Text("$level", style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Back and Home are the two buttons a real TV remote gets you reaching for
 * constantly (same idea as the Google TV remote's two big top buttons), so
 * they get a dedicated, oversized row of their own right under the d-pad
 * instead of competing for space with Exit/Menu in a small chip row.
 */
@Composable
private fun BackHomeRow(onBack: () -> Unit, onHome: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        BigActionButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            label = "Atrás",
            modifier = Modifier.weight(1f),
            onClick = onBack
        )
        BigActionButton(
            icon = Icons.Filled.Home,
            label = "Home",
            modifier = Modifier.weight(1f),
            onClick = onHome
        )
    }
}

@Composable
private fun BigActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.height(64.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.size(8.dp))
            Text(label, color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun QuickActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label) }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/** A single elevated pill with evenly-spaced transport buttons, like a real remote's media row. */
@Composable
private fun MediaTransportBar(onButton: (String) -> Unit) {
    // The TV never reports play/paused state over SSAP, so this toggle is optimistic:
    // it flips locally on tap and sends the matching command, starting from "playing".
    var isPlaying by remember { mutableStateOf(true) }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportButton(Icons.Filled.FastRewind, "Retroceder") { onButton("REWIND") }
            TransportButton(
                icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                description = if (isPlaying) "Pausa" else "Play"
            ) {
                isPlaying = !isPlaying
                onButton(if (isPlaying) "PLAY" else "PAUSE")
            }
            TransportButton(Icons.Filled.Stop, "Stop") { onButton("STOP") }
            TransportButton(Icons.Filled.FastForward, "Adelantar") { onButton("FASTFORWARD") }
        }
    }
}

@Composable
private fun TransportButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.size(52.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description)
        }
    }
}

@Composable
private fun TextKeyboardDialog(onDismiss: () -> Unit, onSend: (String) -> Unit, onEnter: () -> Unit) {
    var text by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escribir en la TV") },
        text = {
            Column {
                Text("Tocá un campo de texto en la TV (buscador, usuario, clave) y escribí acá.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        onSend(it)
                    },
                    label = { Text("Texto") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onEnter) {
                Icon(Icons.Filled.KeyboardReturn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text("Enter")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun NumericKeypad(onDigit: (String) -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "")
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { digit ->
                    if (digit.isEmpty()) {
                        Spacer(Modifier.size(56.dp))
                    } else {
                        Surface(
                            onClick = { onDigit(digit) },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            modifier = Modifier.size(56.dp),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text(digit) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * There's no public TV API for "continue watching" (that's internal to each streaming
 * app), so this is our own "recently opened from this remote" history instead - still
 * handy as a shortcut, just not per-episode.
 */
@Composable
private fun RecentsTab(apps: List<WebOsApp>, onLaunchApp: (WebOsApp) -> Unit) {
    val imageLoader = rememberInsecureImageLoader()
    if (apps.isEmpty()) {
        CenteredMessage("Todavía no abriste ninguna app desde acá.\nLo que abras va a aparecer en esta lista.")
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        items(apps, key = { it.id }) { app ->
            Surface(
                onClick = { onLaunchApp(app) },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (app.iconUrl != null) {
                                AsyncImage(
                                    model = app.iconUrl,
                                    contentDescription = app.title,
                                    imageLoader = imageLoader,
                                    modifier = Modifier.size(40.dp)
                                )
                            } else {
                                Icon(Icons.Filled.Tv, contentDescription = app.title, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "Abierta " + timeAgo(app.launchedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Abrir", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        }
    }
}

/** We can't know WHAT was playing (no public "continue watching" API - see mediacontroller
 * 404 over SSAP), only WHEN this remote last opened the app - so that's what we show. */
private fun timeAgo(whenMillis: Long): String {
    if (whenMillis <= 0L) return "recién"
    val diffMin = (System.currentTimeMillis() - whenMillis) / 60_000
    return when {
        diffMin < 1 -> "recién"
        diffMin < 60 -> "hace ${diffMin}m"
        diffMin < 60 * 24 -> "hace ${diffMin / 60}h"
        else -> "hace ${diffMin / (60 * 24)}d"
    }
}

@Composable
private fun AppsTab(
    apps: List<WebOsApp>,
    loading: Boolean,
    favoriteIds: Set<String>,
    onLaunchApp: (WebOsApp) -> Unit,
    onToggleFavorite: (WebOsApp) -> Unit,
    onRetry: () -> Unit,
    onOpenStore: () -> Unit
) {
    val imageLoader = rememberInsecureImageLoader()

    if (loading && apps.isEmpty()) {
        CenteredMessage("Cargando apps...")
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(onClick = onOpenStore, label = { Text("Buscar e instalar apps") })
            IconButton(onClick = onRetry) { Icon(Icons.Filled.Refresh, contentDescription = "Actualizar") }
        }
        if (apps.isEmpty()) {
            CenteredError("Sin apps (o la TV no respondió)", onRetry)
            return
        }

        val favorites = apps.filter { it.id in favoriteIds }
        val rest = apps.filterNot { it.id in favoriteIds }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            if (favorites.isNotEmpty()) {
                SectionLabel("Favoritas")
                // Favorites get real estate: 2 big columns, easy to hit, icon-forward.
                LazyVerticalGridFixedHeight(favorites, columns = 2) { app ->
                    AppCard(
                        app, isFavorite = true, imageLoader = imageLoader, iconHeight = 108.dp,
                        onClick = { onLaunchApp(app) }, onToggleFavorite = { onToggleFavorite(app) }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            SectionLabel(if (favorites.isEmpty()) "Todas las apps" else "Más apps")
            // Everything else: small 4-per-row grid, so a big catalog doesn't dwarf favorites.
            LazyVerticalGridFixedHeight(rest, columns = 4) { app ->
                AppCard(
                    app, isFavorite = false, imageLoader = imageLoader, iconHeight = 56.dp,
                    onClick = { onLaunchApp(app) }, onToggleFavorite = { onToggleFavorite(app) }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

/** A fixed-row-height grid (not scrollable on its own) so it can sit inside an outer scroll column. */
@Composable
private fun LazyVerticalGridFixedHeight(apps: List<WebOsApp>, columns: Int, content: @Composable (WebOsApp) -> Unit) {
    val rows = (apps.size + columns - 1) / columns
    Column(Modifier.padding(horizontal = 8.dp)) {
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    Box(modifier = Modifier.weight(1f).padding(4.dp)) {
                        if (index < apps.size) content(apps[index])
                    }
                }
            }
        }
    }
}

/** App tile - icon, title, and a tap-to-favorite star. `iconHeight` lets favorites
 * (big, 2-column) and the rest (small, 4-column) share one consistent look. */
@Composable
private fun AppCard(
    app: WebOsApp,
    isFavorite: Boolean,
    imageLoader: coil.ImageLoader,
    iconHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box {
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(if (iconHeight > 80.dp) 24.dp else 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(iconHeight)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (app.iconUrl != null) {
                        AsyncImage(
                            model = app.iconUrl,
                            contentDescription = app.title,
                            imageLoader = imageLoader,
                            modifier = Modifier.size(iconHeight * 0.55f)
                        )
                    } else {
                        Icon(Icons.Filled.Tv, contentDescription = app.title, modifier = Modifier.size(iconHeight * 0.4f))
                    }
                }
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.align(Alignment.TopEnd).size(if (iconHeight > 80.dp) 32.dp else 22.dp)
            ) {
                Icon(
                    if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "Favorita",
                    tint = if (isFavorite) androidx.compose.ui.graphics.Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(if (iconHeight > 80.dp) 20.dp else 14.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            app.title,
            style = if (iconHeight > 80.dp) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun InputsTab(
    inputs: List<WebOsInput>,
    loading: Boolean,
    onSwitchInput: (WebOsInput) -> Unit,
    onRetry: () -> Unit
) {
    val imageLoader = rememberInsecureImageLoader()

    if (loading && inputs.isEmpty()) {
        CenteredMessage("Cargando entradas...")
        return
    }
    if (inputs.isEmpty()) {
        CenteredError("Sin entradas externas detectadas", onRetry)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(inputs, key = { it.id }) { input ->
            Surface(
                onClick = { onSwitchInput(input) },
                shape = RoundedCornerShape(24.dp),
                color = if (input.connected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = if (input.connected) 4.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (input.iconUrl != null) {
                            AsyncImage(
                                model = input.iconUrl,
                                contentDescription = input.label,
                                imageLoader = imageLoader,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Icon(Icons.Filled.SettingsInputHdmi, contentDescription = input.label, modifier = Modifier.size(40.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        input.label,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    if (input.connected) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                "Conectado",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Text(
                            "Sin señal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

private enum class ChannelFilter { ALL, TV, RADIO }

@Composable
private fun ChannelsTab(
    channels: List<WebOsChannel>,
    loading: Boolean,
    onSwitchChannel: (WebOsChannel) -> Unit,
    onRetry: () -> Unit
) {
    if (loading && channels.isEmpty()) {
        CenteredMessage("Cargando canales...")
        return
    }
    if (channels.isEmpty()) {
        CenteredError("Sin canales (hacé el escaneo de canales en la TV primero)", onRetry)
        return
    }

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ChannelFilter.ALL) }

    val filtered = channels.filter { ch ->
        val matchesType = when (filter) {
            ChannelFilter.ALL -> true
            ChannelFilter.TV -> !ch.isRadio
            ChannelFilter.RADIO -> ch.isRadio
        }
        val matchesQuery = query.isBlank() ||
            ch.name.contains(query, ignoreCase = true) ||
            ch.number.contains(query, ignoreCase = true)
        matchesType && matchesQuery
    }

    Column(Modifier.fillMaxSize()) {
        androidx.compose.material3.OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Buscar canal") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = filter == ChannelFilter.ALL, onClick = { filter = ChannelFilter.ALL }, label = { Text("Todos") })
            FilterChip(
                selected = filter == ChannelFilter.TV,
                onClick = { filter = ChannelFilter.TV },
                label = { Text("TV") },
                leadingIcon = { Icon(Icons.Filled.LiveTv, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = filter == ChannelFilter.RADIO,
                onClick = { filter = ChannelFilter.RADIO },
                label = { Text("Radio") },
                leadingIcon = { Icon(Icons.Filled.Radio, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }
        Spacer(Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            CenteredMessage("Sin resultados")
            return
        }

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            items(filtered, key = { it.id }) { channel ->
                Surface(
                    onClick = { onSwitchChannel(channel) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (channel.isRadio) MaterialTheme.colorScheme.tertiaryContainer
                                    else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    channel.number,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (channel.isRadio) MaterialTheme.colorScheme.onTertiaryContainer
                                            else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.size(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                channel.name.ifBlank { "Canal ${channel.number}" },
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (channel.isRadio) "Radio" else "TV",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            if (channel.isRadio) Icons.Filled.Radio else Icons.Filled.LiveTv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
