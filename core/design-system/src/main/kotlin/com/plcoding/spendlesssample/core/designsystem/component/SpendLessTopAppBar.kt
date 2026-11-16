package com.plcoding.spendlesssample.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.spendlesssample.core.designsystem.icon.SpendLessIcons
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme

private val TopBarHeight = 64.dp
private val TopBarIconSize = 24.dp

/** M3 pads the title slot by 4dp; Figma puts the title right after the 48dp nav icon. */
private val TitleSlotPadding = 4.dp

/**
 * The SpendLess top app bar: 64dp, transparent, `titleLarge` title in `onSurface`. Built on M3
 * [TopAppBar] with no window insets; the caller handles insets. With a navigation icon the bar has
 * 4dp horizontal padding; title-only bars start the title at 16dp.
 *
 * The kit master `406:2552` cannot be fetched, so this follows the screen instances (research R6, R15).
 *
 * Figma (leading navigation): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12066
 * Figma (title + close action): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-6632
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendLessTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Title at 52dp after a nav icon, as in 2417:12066 (4dp padding + 48dp icon).
                modifier = if (navigationIcon != null) Modifier.offset(x = -TitleSlotPadding) else Modifier,
            )
        },
        modifier = modifier,
        navigationIcon = { navigationIcon?.invoke() },
        actions = actions,
        expandedHeight = TopBarHeight,
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            navigationIconContentColor = colors.onSurface,
            titleContentColor = colors.onSurface,
            actionIconContentColor = colors.onSurface,
        ),
    )
}

/**
 * Top-bar icon button: 48dp touch target, 40dp circular state layer, 24dp `onSurface` icon. The
 * state layer uses the icon colour at 12%, the M3 kit rule for standard icon buttons, since the kit
 * master `406:2443` cannot be fetched (research R6, R15).
 *
 * Figma (back): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12066
 * Figma (close): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-6632
 */
@Composable
fun SpendLessTopAppBarIconButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = MaterialTheme.colorScheme
    ProvideStateLayer(color = colors.onSurface) {
        IconButton(
            onClick = onClick,
            modifier = modifier.previewStateLayer(colors.onSurface, CircleShape, inset = 4.dp),
            colors = IconButtonDefaults.iconButtonColors(contentColor = colors.onSurface),
            interactionSource = interactionSource,
            shape = CircleShape,
        ) {
            Icon(painter = icon, contentDescription = contentDescription, modifier = Modifier.size(TopBarIconSize))
        }
    }
}

// region Previews

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12066 */
@Preview(widthDp = 412, showBackground = true)
@Composable
private fun SpendLessTopAppBarWithBackPreview() {
    SpendLessTheme {
        SpendLessTopAppBar(
            title = "All Transactions",
            navigationIcon = {
                SpendLessTopAppBarIconButton(icon = SpendLessIcons.ArrowBack, contentDescription = "Back", onClick = {})
            },
        )
    }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-6632 */
@Preview(widthDp = 412, showBackground = true)
@Composable
private fun SpendLessTopAppBarTitleAndClosePreview() {
    SpendLessTheme {
        SpendLessTopAppBar(
            title = "Create Transaction",
            actions = {
                SpendLessTopAppBarIconButton(icon = SpendLessIcons.Close, contentDescription = "Close", onClick = {})
            },
        )
    }
}

@Preview(widthDp = 412, showBackground = true)
@Composable
private fun SpendLessTopAppBarTitleOnlyPreview() {
    SpendLessTheme {
        SpendLessTopAppBar(title = "Create Transaction")
    }
}

@Preview(widthDp = 412, showBackground = true)
@Composable
private fun SpendLessTopAppBarLongTitlePreview() {
    SpendLessTheme {
        SpendLessTopAppBar(
            title = "A really long screen title that has to be truncated at the end",
            navigationIcon = {
                SpendLessTopAppBarIconButton(icon = SpendLessIcons.ArrowBack, contentDescription = "Back", onClick = {})
            },
            actions = {
                SpendLessTopAppBarIconButton(icon = SpendLessIcons.Close, contentDescription = "Close", onClick = {})
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SpendLessTopAppBarIconButtonEnabledPreview() {
    SpendLessTheme {
        SpendLessTopAppBarIconButton(icon = SpendLessIcons.Close, contentDescription = "Close", onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SpendLessTopAppBarIconButtonPressedPreview() {
    SpendLessTheme {
        WithPreviewInteraction(PreviewInteraction.Pressed) {
            SpendLessTopAppBarIconButton(icon = SpendLessIcons.Close, contentDescription = "Close", onClick = {})
        }
    }
}

// endregion
