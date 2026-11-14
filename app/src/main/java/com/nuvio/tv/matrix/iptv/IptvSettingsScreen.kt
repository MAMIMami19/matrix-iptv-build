package com.nuvio.tv.matrix.iptv

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * IPTV / Live TV settings, ported from the 0.6.3-based Matrix build and
 * reduced to what the 1.2.0 player supports (see [IptvSettingsRepository]):
 * parental options + PIN only. Audio/subtitle languages, autoplay and
 * subtitles now live in the app's global Playback settings and subtitle
 * addons — this screen says so instead of offering dead toggles.
 *
 * In the old build this was a section inside the app's Playback settings
 * page; in 3.0 it is the third tab of the IPTV destination.
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

    var showSetPin by remember { mutableStateOf(false) }
    var pendingAfterPin by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showUnlockPin by remember { mutableStateOf(false) }

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
        MatrixPinDialog(
            title = "IPTV",
            onVerify = { entered -> entered == pin },
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
            subtitle = "Remove IPTV watch progress when leaving IPTV",
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
        Text(
            text = "Audio language, subtitle language and autoplay are set in " +
                "Settings → Playback and apply to IPTV playback too. Subtitles " +
                "come from the installed subtitle addons.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
