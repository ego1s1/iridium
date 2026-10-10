package com.iridium.app

import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    val shareSubject = stringResource(R.string.crash_share_subject)
    val shareChooser = stringResource(R.string.crash_share_chooser)

    if (!asked) {
        AlertDialog(
            onDismissRequest = onDecline,
            title = { Text(stringResource(R.string.crash_consent_title)) },
            text = {
                Text(stringResource(R.string.crash_consent_body))
            },
            confirmButton = {
                TextButton(onClick = onEnable) { Text(stringResource(R.string.crash_consent_enable)) }
            },
            dismissButton = {
                TextButton(onClick = onDecline) { Text(stringResource(R.string.crash_consent_not_now)) }
            },
        )
    } else if (enabled && hasPendingReport) {
        AlertDialog(
            onDismissRequest = onDiscard,
            title = { Text(stringResource(R.string.crash_pending_title)) },
            text = { Text(stringResource(R.string.crash_pending_body)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val text = reportText()
                        if (text != null) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(intent, shareChooser),
                                )
                            }
                        }
                        onShared()
                    }
                }) { Text(stringResource(R.string.crash_share)) }
            },
            dismissButton = {
                TextButton(onClick = onDiscard) { Text(stringResource(R.string.crash_discard)) }
            },
        )
    }
}
