package com.iridium.feature.reader.impl

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iridium.core.designsystem.IridiumAlertDialog
import com.iridium.core.designsystem.IridiumPrimaryButton
import com.iridium.core.designsystem.IridiumTonalButton

/**
 * State driving [DictionaryPopup]: the selected [word], whether a lookup is
 * in flight, the resolved [definition], or a human-readable [error].
 */
data class DictionaryUiState(
    val word: String,
    val loading: Boolean = false,
    val definition: WordDefinition? = null,
    val error: String? = null,
)

/** Test tags for the dictionary popup. */
internal object DictionaryTestTags {
    const val Dialog = "dictionary_dialog"
    const val CopyButton = "dictionary_copy"
    const val CloseButton = "dictionary_close"
}

/** Entrance scale duration for [DictionaryPopup] (ms). */
internal const val DICTIONARY_ENTER_MS = 220

/** Entrance fade duration for [DictionaryPopup] (ms). */
internal const val DICTIONARY_FADE_MS = 180

/**
 * Lithium-style word lookup popup: word header with phonetic, then short
 * per-part-of-speech definitions — no clutter. Copy puts the word on the
 * clipboard; Close (or tapping outside) dismisses.
 *
 * NOTE: labels are inline English for now — res/values/strings.xml is owned
 * outside this change; the screen owner should promote them to resources.
 */
@Composable
fun DictionaryPopup(
    state: DictionaryUiState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    // Scale/fade entrance: the host keys this popup by word, so every fresh
    // selection replays the reveal once the lookup grace period elapses.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val enterScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.92f,
        animationSpec = tween(DICTIONARY_ENTER_MS, easing = FastOutSlowInEasing),
        label = "dictionaryEnterScale",
    )
    val enterAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(DICTIONARY_FADE_MS),
        label = "dictionaryEnterAlpha",
    )
    IridiumAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = state.word,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            DictionaryPopupBody(state = state)
        },
        confirmButton = {
            IridiumPrimaryButton(
                onClick = { clipboard.setText(AnnotatedString(state.word)) },
                modifier = Modifier.testTag(DictionaryTestTags.CopyButton),
            ) {
                Text("Copy")
            }
        },
        dismissButton = {
            IridiumTonalButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(DictionaryTestTags.CloseButton),
            ) {
                Text("Close")
            }
        },
        modifier = modifier
            .graphicsLayer {
                scaleX = enterScale
                scaleY = enterScale
                alpha = enterAlpha
            }
            .testTag(DictionaryTestTags.Dialog),
    )
}

@Composable
private fun DictionaryPopupBody(
    state: DictionaryUiState,
    modifier: Modifier = Modifier,
) {
    when {
        state.loading -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.fillMaxWidth(),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text(
                    text = "Looking up…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        state.error != null -> {
            Text(
                text = state.error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = modifier.fillMaxWidth(),
            )
        }

        state.definition != null -> {
            DictionaryDefinitionContent(
                definition = state.definition,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun DictionaryDefinitionContent(
    definition: WordDefinition,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (!definition.phonetic.isNullOrBlank()) {
            Text(
                text = definition.phonetic,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        definition.meanings.forEach { meaning ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = meaning.partOfSpeech,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                meaning.definitions.forEachIndexed { index, text ->
                    Text(
                        text = "${index + 1}. $text",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
