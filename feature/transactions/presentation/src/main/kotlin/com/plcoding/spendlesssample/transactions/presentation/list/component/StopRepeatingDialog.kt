package com.plcoding.spendlesssample.transactions.presentation.list.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.spendlesssample.core.designsystem.component.SpendLessButton
import com.plcoding.spendlesssample.core.designsystem.component.SpendLessButtonStyle
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme
import com.plcoding.spendlesssample.transactions.presentation.R

/** The Figma sheet corner token. */
private val DialogShape = RoundedCornerShape(28.dp)

/**
 * Confirms stopping a recurring series (FR-038): "Stop repeating {name}?" with Stop and Cancel.
 * A tap outside or Back counts as Cancel.
 *
 * No Figma node; composed from DS Button `406:110` per user decision 2026-11-13 (research R11).
 * Figma (Text button): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-213
 */
@Composable
internal fun StopRepeatingDialog(
    name: String,
    onStopClick: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            SpendLessButton(
                text = stringResource(R.string.stop),
                onClick = onStopClick,
                style = SpendLessButtonStyle.Text,
            )
        },
        modifier = modifier,
        dismissButton = {
            SpendLessButton(
                text = stringResource(R.string.cancel),
                onClick = onCancel,
                style = SpendLessButtonStyle.Text,
            )
        },
        title = {
            Text(
                text = stringResource(R.string.stop_repeating_title, name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        shape = DialogShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        // Flat, like every DS surface: no tonal tint over surfaceContainerLowest.
        tonalElevation = 0.dp,
    )
}

// region Previews

/** No Figma node (research R11). */
@Preview
@Composable
private fun StopRepeatingDialogShortNamePreview() {
    SpendLessTheme {
        StopRepeatingDialog(name = "Gym", onStopClick = {}, onCancel = {})
    }
}

/** The 50-character maximum name (FR-012) wraps in the title. No Figma node (research R11). */
@Preview
@Composable
private fun StopRepeatingDialogLongNamePreview() {
    SpendLessTheme {
        StopRepeatingDialog(
            name = "Neighbourhood community garden yearly membership f",
            onStopClick = {},
            onCancel = {},
        )
    }
}

// endregion
