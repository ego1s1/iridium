package com.iridium.feature.history.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iridium.core.designsystem.IridiumEmphasized
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.LocalAppFonts

/** Streak journey card with current and longest records. */
@Composable
internal fun HistoryStreakCard(
    streak: HistoryStreak,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.history_stats_streak),
                    style = IridiumEmphasized.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(10.dp),
                        ),
                ) {
                    Icon(
                        imageVector = IridiumIcons.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                text = pluralStringResource(
                    R.plurals.history_stats_streak_days,
                    streak.current,
                    streak.current,
                ),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = LocalAppFonts.current.displayFlex,
                    fontWeight = FontWeight.Black,
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.history_stats_streak_longest,
                    streak.longest,
                    streak.longest,
                ),
                style = IridiumEmphasized.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
