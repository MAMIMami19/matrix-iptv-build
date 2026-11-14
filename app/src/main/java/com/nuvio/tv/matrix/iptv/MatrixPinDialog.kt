package com.nuvio.tv.matrix.iptv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 4-digit PIN entry dialog for the IPTV lock, TV-remote friendly: an
 * on-screen keypad navigable with DPAD, auto-verifying at 4 digits.
 */
@Composable
fun MatrixPinDialog(
    title: String,
    onVerify: suspend (String) -> Boolean,
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
    /** When false, the dialog cannot be dismissed (whole-screen lock). */
    dismissable: Boolean = true,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val initialFocus = remember { FocusRequester() }

    fun submit(candidate: String) {
        if (isVerifying) return
        isVerifying = true
        scope.launch {
            val ok = onVerify(candidate)
            isVerifying = false
            if (ok) {
                onVerified()
            } else {
                error = true
                pin = ""
            }
        }
    }

    fun onDigit(digit: String) {
        if (pin.length >= 4 || isVerifying) return
        error = false
        val next = pin + digit
        pin = next
        if (next.length == 4) submit(next)
    }

    AlertDialog(
        onDismissRequest = { if (dismissable) onDismiss() },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier.size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        error -> MaterialTheme.colorScheme.error
                                        index < pin.length -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                        )
                    }
                }
                if (error) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Wrong PIN",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "⌫"),
                )
                rows.forEachIndexed { rowIndex, row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 6.dp),
                    ) {
                        row.forEachIndexed { colIndex, key ->
                            if (key.isEmpty()) {
                                Spacer(modifier = Modifier.size(64.dp))
                            } else {
                                val isFirst = rowIndex == 0 && colIndex == 0
                                Box(
                                    modifier = Modifier.size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .then(
                                            if (isFirst) Modifier.focusRequester(initialFocus)
                                            else Modifier
                                        )
                                        .clickable {
                                            if (key == "⌫") {
                                                if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                            } else {
                                                onDigit(key)
                                            }
                                        }
                                        .tvFocusRing(cornerRadius = 32.dp, focusedScale = 1.1f),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        confirmButton = {},
        dismissButton = {
            if (dismissable) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
    // Focus the "1" key once the dialog is composed.
    LaunchedEffect(Unit) {
        delay(200)
        runCatching { initialFocus.requestFocus() }
    }
}
