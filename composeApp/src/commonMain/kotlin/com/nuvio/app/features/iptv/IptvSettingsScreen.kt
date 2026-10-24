package com.nuvio.app.features.iptv

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.player.AvailableLanguageOptions
import com.nuvio.app.features.player.PlayerSettingsRepository
import com.nuvio.app.features.player.SubtitleCacheProvider
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * IPTV / Live TV settings, copied from the reference IPTV app's settings.
 *
 * Deliberately absent (see IptvSettingsRepository): biometric unlock (no
 * biometric API in the app) and in-app PiP (no mini-player exists).
 * The online-subtitles login fields are user-filled only, never prefilled.
 */
@Composable
internal fun IptvSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lockAdult by IptvSettingsRepository.lockAdultCategories.collectAsState()
    val clearOnClose by IptvSettingsRepository.clearHistoryOnClose.collectAsState()
    val lockSettings by IptvSettingsRepository.lockSettings.collectAsState()
    val pin by IptvSettingsRepository.pin.collectAsState()
    val systemPip by IptvSettingsRepository.systemPipEnabled.collectAsState()
    val osdDurationMs by IptvSettingsRepository.osdDurationMs.collectAsState()
    val audioLanguage by IptvSettingsRepository.audioLanguage.collectAsState()
    val subtitleLanguage by IptvSettingsRepository.subtitleLanguage.collectAsState()
    val onlineUsername by IptvSettingsRepository.onlineSubtitleUsername.collectAsState()
    val onlinePassword by IptvSettingsRepository.onlineSubtitlePassword.collectAsState()
    val preferredLanguages by IptvSettingsRepository.onlineSubtitlePreferredLanguages.collectAsState()
    val playerSettings by PlayerSettingsRepository.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var showSetPin by remember { mutableStateOf(false) }
    var pendingAfterPin by remember { mutableStateOf<(() -> Unit)?>(null) }
    var cacheCleared by remember { mutableStateOf(false) }
    // NOTE: these must live here, not inside the LazyColumn scope below —
    // the LazyListScope lambda is not a @Composable context, so remember
    // calls placed between item blocks fail compilation.
    val labelByCode = remember {
        AvailableLanguageOptions.associate { it.code to it.labelRes }
    }
    val orderedCodes = remember {
        (
            listOf("ar", "kk", "hy", "he", "ur", "fa", "ku", "hi", "bn", "ta", "te") +
                AvailableLanguageOptions.map { it.code }
            ).distinct()
    }

    // Synchronous + idempotent: avoids one frame of default values.
    IptvSettingsRepository.ensureLoaded()

    fun requirePin(enable: () -> Unit) {
        if (pin.isEmpty()) {
            pendingAfterPin = enable
            showSetPin = true
        } else {
            enable()
        }
    }

    if (showSetPin) {
        IptvSetPinDialog(
            onDismiss = {
                showSetPin = false
                pendingAfterPin = null
            },
            onSaved = { newPin ->
                IptvSettingsRepository.setPin(newPin)
                showSetPin = false
                pendingAfterPin?.invoke()
                pendingAfterPin = null
            },
        )
    }

    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("Back") }
                Text("IPTV settings", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        item { IptvSettingsSection("Parental options") }
        item {
            IptvToggleRow(
                label = "Lock adult categories",
                subtitle = "Hide adult categories until unlocked",
                checked = lockAdult,
                onCheckedChange = { on ->
                    if (on) {
                        requirePin { IptvSettingsRepository.setLockAdultCategories(true) }
                    } else {
                        IptvSettingsRepository.setLockAdultCategories(false)
                    }
                },
            )
        }
        item {
            IptvToggleRow(
                label = "Clear watch history on close",
                subtitle = "Remove IPTV watch progress when leaving the tab",
                checked = clearOnClose,
                onCheckedChange = IptvSettingsRepository::setClearHistoryOnClose,
            )
        }
        item {
            IptvToggleRow(
                label = "Lock settings",
                subtitle = "Require the PIN to open IPTV settings",
                checked = lockSettings,
                onCheckedChange = { on ->
                    if (on) {
                        requirePin { IptvSettingsRepository.setLockSettings(true) }
                    } else {
                        IptvSettingsRepository.setLockSettings(false)
                    }
                },
            )
        }
        item {
            Button(
                onClick = { showSetPin = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) { Text("Change lock PIN") }
        }

        item { IptvSettingsSection("Player options") }
        item {
            IptvToggleRow(
                label = "System PiP",
                subtitle = "Allow picture-in-picture when leaving the player",
                checked = systemPip,
                onCheckedChange = IptvSettingsRepository::setSystemPipEnabled,
            )
        }
        item {
            Text("OSD display duration", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                listOf(3000 to "3s", 5000 to "5s", 10000 to "10s").forEach { (ms, label) ->
                    val selected = osdDurationMs == ms
                    if (selected) {
                        Button(
                            onClick = { IptvSettingsRepository.setOsdDurationMs(ms) },
                            modifier = Modifier.padding(end = 8.dp),
                        ) { Text(label) }
                    } else {
                        OutlinedButton(
                            onClick = { IptvSettingsRepository.setOsdDurationMs(ms) },
                            modifier = Modifier.padding(end = 8.dp),
                        ) { Text(label) }
                    }
                }
            }
        }
        item {
            IptvToggleRow(
                label = "Auto-play series",
                subtitle = "Auto-play the next episode where supported",
                checked = playerSettings.streamAutoPlayNextEpisodeEnabled,
                onCheckedChange = PlayerSettingsRepository::setStreamAutoPlayNextEpisodeEnabled,
            )
        }

        item { IptvSettingsSection("Audio & subtitles") }
        item {
            IptvLanguageDropdown(
                label = "Audio language",
                selectedCode = audioLanguage,
                onCodeSelected = IptvSettingsRepository::setAudioLanguage,
            )
        }
        item {
            IptvLanguageDropdown(
                label = "Subtitle language",
                selectedCode = subtitleLanguage,
                onCodeSelected = IptvSettingsRepository::setSubtitleLanguage,
            )
        }

        item { IptvSettingsSection("Online subtitles") }
        item {
            OutlinedTextField(
                value = onlineUsername,
                onValueChange = IptvSettingsRepository::setOnlineSubtitleUsername,
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        }
        item {
            OutlinedTextField(
                value = onlinePassword,
                onValueChange = IptvSettingsRepository::setOnlineSubtitlePassword,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        }
        item {
            Button(
                onClick = {
                    scope.launch {
                        SubtitleCacheProvider.clearSubtitleCache()
                        cacheCleared = true
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) { Text("Clear subtitle cache") }
            if (cacheCleared) {
                Text("Subtitle cache cleared", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Text(
                text = "Preferred languages",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        items(orderedCodes, key = { it }) { code ->
            // Kurdish has no string resource in the app; use a literal label.
            val label = if (code == "ku") {
                "Kurdish"
            } else {
                labelByCode[code]?.let { stringResource(it) } ?: code
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    IptvSettingsRepository.setOnlineSubtitlePreferredLanguages(
                        if (code in preferredLanguages) preferredLanguages - code else preferredLanguages + code,
                    )
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = code in preferredLanguages,
                    onCheckedChange = {
                        IptvSettingsRepository.setOnlineSubtitlePreferredLanguages(
                            if (code in preferredLanguages) preferredLanguages - code else preferredLanguages + code,
                        )
                    },
                )
                Text(label)
            }
        }
    }
}

@Composable
private fun IptvSettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun IptvToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IptvLanguageDropdown(
    label: String,
    selectedCode: String,
    onCodeSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember {
        listOf("device" to null) + AvailableLanguageOptions.map { it.code to it.labelRes }
    }
    val selectedLabel = if (selectedCode == "device") {
        "System"
    } else {
        options.firstOrNull { it.first == selectedCode }?.second?.let { stringResource(it) } ?: selectedCode
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (code, labelRes) ->
                DropdownMenuItem(
                    text = {
                        Text(if (labelRes == null) "System" else stringResource(labelRes))
                    },
                    onClick = {
                        onCodeSelected(code)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun IptvSetPinDialog(
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid = pin.isNotEmpty() && pin == confirm
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change lock PIN") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("New PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (pin.isNotEmpty() && confirm.isNotEmpty() && pin != confirm) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("PINs do not match", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSaved(pin) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
