package com.plcoding.spendlesssample.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.spendlesssample.core.designsystem.icon.SpendLessIcons
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessShadows
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme
import com.plcoding.spendlesssample.core.designsystem.theme.spendLessShadow

private val FabShape = RoundedCornerShape(20.dp)
private val FabSize = 64.dp
private val FabIconSize = 28.dp

/**
 * The SpendLess floating action button: 64dp, radius 20, `secondaryContainer`, a 28dp centred icon
 * and the `fab` drop shadow. Built on M3 [FloatingActionButton] with all elevations at 0.
 *
 * Figma Pressed is a frozen ripple frame; the Compose ripple is its animated equivalent
 * (research R15 D2).
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=410-2338
 * Figma (Enabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=410-2375
 * Figma (Focused): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=410-2369
 * Figma (Pressed): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=410-2365
 */
@Composable
fun SpendLessFloatingActionButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = MaterialTheme.colorScheme
    ProvideStateLayer(color = colors.onSecondaryContainer) {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier
                .size(FabSize)
                .spendLessShadow(SpendLessShadows.fab, FabShape)
                .previewStateLayer(LocalPreviewInteraction.current, colors.onSecondaryContainer, FabShape),
            shape = FabShape,
            containerColor = colors.secondaryContainer,
            contentColor = colors.onSecondaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            interactionSource = interactionSource,
        ) {
            Icon(painter = icon, contentDescription = contentDescription, modifier = Modifier.size(FabIconSize))
        }
    }
}

// region Previews

@Composable
private fun FabPreview(interaction: PreviewInteraction? = null) {
    SpendLessTheme {
        WithPreviewInteraction(interaction) {
            SpendLessFloatingActionButton(
                icon = SpendLessIcons.Add,
                contentDescription = "Create transaction",
                onClick = {},
                // Room for the shadow inside the preview bounds.
                modifier = Modifier.padding(28.dp),
            )
        }
    }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=410-2375 */
@Preview(showBackground = true)
@Composable
private fun SpendLessFloatingActionButtonEnabledPreview() = FabPreview()

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=410-2369 */
@Preview(showBackground = true)
@Composable
private fun SpendLessFloatingActionButtonFocusedPreview() = FabPreview(PreviewInteraction.Focused)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=410-2365 */
@Preview(showBackground = true)
@Composable
private fun SpendLessFloatingActionButtonPressedPreview() = FabPreview(PreviewInteraction.Pressed)

// endregion
