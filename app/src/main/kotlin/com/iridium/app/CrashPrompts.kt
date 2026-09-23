package com.iridium.app

import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

/**
 * Consent-first crash reporting. Nothing is collected until the user says yes;
 * when a report exists it is shared only through a user-initiated share sheet.
 */
@Composable
internal fun CrashPrompts(
    asked: Boolean,
    enabled: Boolean,
    hasPendingReport: Boolean,
    onEnable: () -> Unit,
    onDecline: () -> Unit,
    onShared: () -> Unit,
    onDiscard: () -> Unit,
    reportText: suspend () -> String?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (!asked) {
        AlertDialog(
            onDismissRequest = onDecline,
            title = { Text("Send crash reports?") },
            text = {
                Text(
                    "If Iridium crashes, a report is saved on this device. Sharing it " +
                        "is optional and always your choice — nothing is ever sent " +
                        "automatically, and your books are never included.",
                )
            },
            confirmButton = {
                TextButton(onClick = onEnable) { Text("Enable") }
            },
            dismissButton = {
                TextButton(onClick = onDecline) { Text("Not now") }
            },
        )
    } else if (enabled && hasPendingReport) {
        AlertDialog(
            onDismissRequest = onDiscard,
            title = { Text("Iridium closed unexpectedly") },
            text = { Text("A crash report is available. Share it to help fix the problem?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val text = reportText()
                        if (text != null) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Iridium crash report")
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(intent, "Share crash report"),
                                )
                            }
                        }
                        onShared()
                    }
                }) { Text("Share") }
            },
            dismissButton = {
                TextButton(onClick = onDiscard) { Text("Discard") }
            },
        )
    }
}
