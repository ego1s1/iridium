package com.iridium.feature.library.impl

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iridium.core.designsystem.BookCoverArt
import com.iridium.core.designsystem.IridiumEmphasized
import com.iridium.core.designsystem.IridiumFilledTonalIconButton
import com.iridium.core.designsystem.IridiumHaptic
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumMotion
import com.iridium.core.designsystem.IridiumPrimaryButton
import com.iridium.core.designsystem.IridiumProgressBar
import com.iridium.core.designsystem.LocalAppFonts
import com.iridium.core.designsystem.rememberIridiumHaptics
import com.iridium.core.model.Book

/**
 * Expressive Hero Spotlight Card for the book currently being read.
 * Anchored at the top of the library when a book is in progress.
 * Progress speaks in percentages: reflowable books have no page counts.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NowReadingHeroCard(
    book: Book,
    onResume: (Book) -> Unit,
    onDetails: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberIridiumHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = IridiumMotion.defaultSpatialSpec(),
        label = "heroScale",
    )

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag(LibraryTestTags.NowReadingHero)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics(IridiumHaptic.PrimaryAction)
                    onResume(book)
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Book cover with tactile spine and 16dp rounded corners.
            Box(
                modifier = Modifier
                    .width(82.dp)
                    .aspectRatio(2f / 3f)
                    .clip(MaterialTheme.shapes.large),
            ) {
                BookCoverArt(
                    coverPath = book.coverPath,
                    contentDescription = null,
                )

                // Subtle physical book spine crease/highlight along the left edge.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                0.0f to Color.Black.copy(alpha = 0.28f),
                                0.03f to Color.White.copy(alpha = 0.12f),
                                0.08f to Color.Transparent,
                            ),
                        ),
                )

                if (book.bookmarked) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f)),
                    ) {
                        Icon(
                            imageVector = IridiumIcons.Bookmark,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            // Hero Details Column.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Eyebrow badge.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.library_hero_eyebrow),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = LocalAppFonts.current.displaySoft,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }

                    Text(
                        text = "${(book.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Title in prominent display font.
                Text(
                    text = book.title,
                    style = IridiumEmphasized.titleMedium.copy(
                        fontFamily = LocalAppFonts.current.displaySoft,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                book.author?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IridiumProgressBar(
                    progress = { book.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                )

                // Actions row.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    IridiumPrimaryButton(
                        onClick = { onResume(book) },
                        haptic = IridiumHaptic.PrimaryAction,
                        modifier = Modifier
                            .height(36.dp)
                            .testTag(LibraryTestTags.HeroResume),
                    ) {
                        Icon(
                            imageVector = IridiumIcons.Play,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.library_menu_resume),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }

                    IridiumFilledTonalIconButton(
                        onClick = { onDetails(book) },
                        haptic = IridiumHaptic.Select,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag(LibraryTestTags.HeroDetails),
                    ) {
                        Icon(
                            imageVector = IridiumIcons.Info,
                            contentDescription = stringResource(R.string.library_menu_details),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
