package com.iridium.feature.history.impl

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.iridium.core.designsystem.IridiumEmphasized
import com.iridium.core.designsystem.IridiumEnter
import com.iridium.core.designsystem.IridiumEnterKind
import com.iridium.core.designsystem.LocalAppFonts

/** Spotlight hero card for estimated reading time (the reference §3.4 primary hero). */
@Composable
internal fun ReadingTimeHeroCard(
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 20.dp),
        ) {
            val numberEnter = IridiumEnter.enter(IridiumEnterKind.FADE_THROUGH)
            val numberExit = IridiumEnter.exit(IridiumEnterKind.FADE_THROUGH)
            AnimatedContent(
                targetState = durationMs,
                transitionSpec = { numberEnter togetherWith numberExit },
                label = "heroReadingTime",
            ) { current ->
                HeroDurationText(
                    durationMs = current,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = stringResource(R.string.history_stats_time_estimated),
                style = IridiumEmphasized.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
        }
    }
}

/** Expressive bento companion card with dedicated container colors. */
@Composable
internal fun CompanionStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        tonalElevation = 2.dp,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(contentColor.copy(alpha = 0.12f), shape = CircleShape),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            val numberEnter = IridiumEnter.enter(IridiumEnterKind.FADE_THROUGH)
            val numberExit = IridiumEnter.exit(IridiumEnterKind.FADE_THROUGH)
            AnimatedContent(
                targetState = value,
                transitionSpec = { numberEnter togetherWith numberExit },
                label = "companionValue",
            ) { current ->
                Text(
                    text = current,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = LocalAppFonts.current.displayFlexMedium,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = label,
                style = IridiumEmphasized.labelLarge,
                color = contentColor.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

/** Compact hero duration: 2h 15m / 45m / 30s. */
@Composable
private fun formatHistoryDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0 -> stringResource(R.string.history_duration_hours_minutes, hours, minutes)
        minutes > 0 -> stringResource(R.string.history_duration_minutes, minutes)
        else -> stringResource(R.string.history_duration_seconds, seconds)
    }
}

/**
 * Staggered hero duration: hours in the wide 900 face, minutes in the
 * 800 intermediate face, units in the compact 700 face. Numbers are kept
 * as plain digits (locale-safe); only the faces stagger.
 */
@Composable
private fun HeroDurationText(
    durationMs: Long,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val fonts = LocalAppFonts.current
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val base = MaterialTheme.typography.displayLarge
    val annotated = remember(hours, minutes, seconds, fonts) {
        buildAnnotatedString {
            fun numberPart(value: Long, family: FontFamily, weight: FontWeight) {
                withStyle(SpanStyle(fontFamily = family, fontWeight = weight)) {
                    append(value.toString())
                }
            }
            fun unitPart(unit: String) {
                withStyle(
                    SpanStyle(fontFamily = fonts.displayUnit, fontWeight = FontWeight.Bold),
                ) {
                    append(unit)
                }
            }
            when {
                hours > 0 -> {
                    numberPart(hours, fonts.displayFlex, FontWeight.Black)
                    unitPart("h ")
                    numberPart(minutes, fonts.displayFlexMedium, FontWeight.ExtraBold)
                    unitPart("m")
                }
                minutes > 0 -> {
                    numberPart(minutes, fonts.displayFlexMedium, FontWeight.ExtraBold)
                    unitPart("m")
                }
                else -> {
                    numberPart(seconds, fonts.displayFlexMedium, FontWeight.ExtraBold)
                    unitPart("s")
                }
            }
        }
    }
    Text(
        text = annotated,
        style = base,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier,
    )
}
