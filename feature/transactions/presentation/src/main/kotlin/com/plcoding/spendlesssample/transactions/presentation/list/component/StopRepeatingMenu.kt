package com.plcoding.spendlesssample.transactions.presentation.list.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessShadows
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme
import com.plcoding.spendlesssample.core.designsystem.theme.spendLessShadow
import com.plcoding.spendlesssample.transactions.presentation.R

/** Select-list surface tokens (`2415:10118`): radius 16, `surfaceContainerLowest`, shadow `select`. */
private val MenuShape = RoundedCornerShape(16.dp)

/** One Select "Text" list item (`2415:10072`): 48dp tall, start padding 16, end padding 12. */
private val ItemHeight = 48.dp
private val ItemStartPadding = 16.dp
private val ItemEndPadding = 12.dp

/** The menu sits 4dp below the row, like the Select list below its anchor. */
private val RowGap = 4.dp

/** Room around the surface so the popup window does not clip its drop shadow. */
private val ShadowRoom = 16.dp

/** Select-list state layer: `onSurface` at 12% when pressed or focused, 8% hovered. */
private const val StateLayerAlpha = 0.12f

@OptIn(ExperimentalMaterial3Api::class)
private val MenuRipple = RippleConfiguration(
    rippleAlpha = RippleAlpha(
        pressedAlpha = StateLayerAlpha,
        focusedAlpha = StateLayerAlpha,
        hoveredAlpha = 0.08f,
        draggedAlpha = StateLayerAlpha,
    ),
)

/**
 * The long-press menu of a row in a running recurring series (FR-037): one item, "Stop repeating".
 * It opens as a popup start-aligned with the row it is placed in, 4dp below it, and closes on a tap
 * outside it without changing anything (FR-040).
 *
 * Emit it inside the row's own layout so the popup anchors to that row.
 *
 * No Figma node; composed from DS Select-list `2415:10118` per user decision 2026-11-13 (research R11).
 * Figma (surface tokens): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10118
 */
@Composable
internal fun StopRepeatingMenu(
    onStopRepeatingClick: () -> Unit,
    onDismiss: () -> Unit,
    interactionSource: MutableInteractionSource? = null,
) {
    val density = LocalDensity.current
    val positionProvider = remember(density) {
        BelowRowPositionProvider(
            gapPx = with(density) { RowGap.roundToPx() },
            shadowRoomPx = with(density) { ShadowRoom.roundToPx() },
        )
    }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Box(modifier = Modifier.padding(ShadowRoom)) {
            StopRepeatingMenuSurface(
                onStopRepeatingClick = onStopRepeatingClick,
                interactionSource = interactionSource,
            )
        }
    }
}

/** Places the menu start-aligned with the row, [gapPx] below it, leaving room for the shadow. */
private class BelowRowPositionProvider(
    private val gapPx: Int,
    private val shadowRoomPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.left - shadowRoomPx
        } else {
            anchorBounds.right - popupContentSize.width + shadowRoomPx
        }
        return IntOffset(x = x, y = anchorBounds.bottom + gapPx - shadowRoomPx)
    }
}

/**
 * The menu's surface and its single item. [forcePressed] draws the flat pressed state layer for a
 * static preview, where a ripple never renders (research R15 D10).
 *
 * Figma (surface tokens): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10118
 * Figma (list item): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10072
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StopRepeatingMenuSurface(
    onStopRepeatingClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    forcePressed: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.spendLessShadow(SpendLessShadows.select, MenuShape),
        shape = MenuShape,
        color = colors.surfaceContainerLowest,
        contentColor = colors.onSurface,
    ) {
        CompositionLocalProvider(LocalRippleConfiguration provides MenuRipple) {
            Box(
                modifier = Modifier
                    .height(ItemHeight)
                    .then(
                        if (forcePressed) Modifier.background(colors.onSurface.copy(alpha = StateLayerAlpha)) else Modifier,
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        role = Role.Button,
                        onClick = onStopRepeatingClick,
                    )
                    .padding(start = ItemStartPadding, end = ItemEndPadding),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = stringResource(R.string.stop_repeating),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// region Previews

@Composable
private fun MenuPreview(forcePressed: Boolean) {
    SpendLessTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .width(200.dp),
        ) {
            StopRepeatingMenuSurface(onStopRepeatingClick = {}, forcePressed = forcePressed)
        }
    }
}

/** No Figma node (research R11). Surface tokens: `2415:10118`. */
@Preview
@Composable
private fun StopRepeatingMenuEnabledPreview() = MenuPreview(forcePressed = false)

/** `onSurface` at 12% over the item, the Select-list state layer. No Figma node (research R11). */
@Preview
@Composable
private fun StopRepeatingMenuPressedPreview() = MenuPreview(forcePressed = true)

// endregion
