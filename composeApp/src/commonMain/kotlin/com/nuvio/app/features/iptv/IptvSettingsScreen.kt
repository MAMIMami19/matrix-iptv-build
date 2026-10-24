package com.nuvio.app.features.iptv

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.nuvio.app.features.profiles.PinEntryDialog
import com.nuvio.app.features.profiles.PinVerifyResult
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * IPTV / Live TV settings, copied from the reference IPTV app's settings.
 *
 * This is a section inside the app's normal (Playback) settings page — not a
 * standalone tab-gear page: the copied parameters live with the regular
 * settings. Deliberately absent (see IptvSettingsRepository): biometric
 * unlock (no biometric API in the app) and in-app PiP (no mini-player
 * exists). The online-subtitle sources are user-filled only, never
 * prefilled, and there may be several of them (not a single login).
 */
@Composable
internal fun IptvSettingsSection(
    modifier: Modifier = Modifier,
) {
    val lockAdult by IptvSettingsRepository.lockAdultCategories.collectAsState()
    val clearOnClose by IptvSettingsRepository.clearHistoryOnClose.collectAsState()
    val lockSettings by IptvSettingsRepository.lockSettings.collectAsState()
    val pin by IptvSettingsRepository.pin.collectAsState()
    val sessionUnlocked by IptvSettingsRepository.sessionUnlocked.collectAsState()
    val systemPip by IptvSettingsRepository.systemPipEnabled.collectAsState()
    val osdDurationMs by IptvSettingsRepository.osdDurationMs.collectAsState()
    val audioLanguage by IptvSettingsRepository.audioLanguage.collectAsState()
    val subtitleLanguage by IptvSettingsRepository.subtitleLanguage.collectAsState()
    val subtitleSources by IptvSettingsRepository.onlineSubtitleSources.collectAsState()
    val preferredLanguages by IptvSettingsRepository.onlineSubtitlePreferredLanguages.collectAsState()
    val playerSettings by PlayerSettingsRepository.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var showSetPin by remember { mutableStateOf(false) }
    var pendingAfterPin by remember { mutableStateOf<(() -> Unit)?>(null) }
    var cacheCleared by remember { mutableStateOf(false) }
    var showUnlockPin by remember { mutableStateOf(false) }
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

    // "Lock settings" gates this section (not the tabs): the tabs keep their
    // own PIN gate, this gate only guards the settings themselves.
    val settingsLocked = lockSettings && pin.isNotEmpty() && !sessionUnlocked

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
    if (showUnlockPin) {
        PinEntryDialog(
            profileName = "IPTV",
            onVerify = { entered -> PinVerifyResult(unlocked = entered == pin) },
            onDismiss = { showUnlockPin = false },
            onVerified = {
                IptvSettingsRepository.setSessionUnlocked(true)
                showUnlockPin = false
            },
        )
    }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text("IPTV", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        if (settingsLocked) {
            Text("IPTV settings are locked")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { showUnlockPin = true }) { Text("Unlock") }
            return@Column
        }

        IptvSettingsSectionTitle("Parental options")
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
        IptvToggleRow(
            label = "Clear watch history on close",
            subtitle = "Remove IPTV watch progress when leaving the tab",
            checked = clearOnClose,
            onCheckedChange = IptvSettingsRepository::setClearHistoryOnClose,
        )
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
        Button(
            onClick = { showSetPin = true },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) { Text("Change lock PIN") }

        IptvSettingsSectionTitle("Player options")
        IptvToggleRow(
            label = "System PiP",
            subtitle = "Allow picture-in-picture when leaving the player",
            checked = systemPip,
            onCheckedChange = IptvSettingsRepository::setSystemPipEnabled,
        )
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
        IptvToggleRow(
            label = "Auto-play series",
            subtitle = "Auto-play the next episode where supported",
            checked = playerSettings.streamAutoPlayNextEpisodeEnabled,
            onCheckedChange = PlayerSettingsRepository::setStreamAutoPlayNextEpisodeEnabled,
        )

        IptvSettingsSectionTitle("Audio & subtitles")
        IptvLanguageDropdown(
            label = "Audio language",
            selectedCode = audioLanguage,
            onCodeSelected = IptvSettingsRepository::setAudioLanguage,
        )
        IptvLanguageDropdown(
            label = "Subtitle language",
            selectedCode = subtitleLanguage,
            onCodeSelected = IptvSettingsRepository::setSubtitleLanguage,
        )

        IptvSettingsSectionTitle("Online subtitles")
        subtitleSources.forEachIndexed { index, source ->
            Text(
                text = "Source ${index + 1}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            OutlinedTextField(
                value = source.username,
                onValueChange = { newUsername ->
                    IptvSettingsRepository.updateOnlineSubtitleSource(
                        index,
                        newUsername,
                        source.password,
                    )
                },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
            OutlinedTextField(
                value = source.password,
                onValueChange = { newPassword ->
                    IptvSettingsRepository.updateOnlineSubtitleSource(
                        index,
                        source.username,
                        newPassword,
                    )
                },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
            TextButton(
                onClick = { IptvSettingsRepository.removeOnlineSubtitleSource(index) },
            ) { Text("Remove source") }
        }
        OutlinedButton(
            onClick = { IptvSettingsRepository.addOnlineSubtitleSource() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) { Text("Add source") }
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
        Text(
            text = "Preferred languages",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        orderedCodes.forEach { code ->
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
private fun IptvSettingsSectionTitle(title: String) {
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
