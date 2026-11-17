package com.plcoding.spendlesssample.transactions.presentation.create.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.spendlesssample.core.designsystem.icon.SpendLessIcons
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme
import com.plcoding.spendlesssample.transactions.presentation.R

/** Label and placeholder alpha from `2415:6643` (`opacity-60`). */
private const val LabelAlpha = 0.6f

/**
 * The optional note of the create sheet (FR-016). Idle, it is the centred "+ Add Note" row: the
 * `icon/add` glyph at 20dp in `onSurface` (the node's own fill, no opacity) and the label in
 * `bodyMedium` at 60%, 4dp apart. While [isEditing], it is a centred, multi-line field in the same
 * tokens (placeholder "Add Note" at 60%, `primary` caret, `px 8dp`) that grows over as many lines
 * as needed; the 500-character cap is enforced by the ViewModel.
 *
 * The field takes focus when it appears. [onFocusLost] fires only after the field has held focus,
 * so the first composition does not report a loss.
 *
 * Editing state has no Figma frame; user decision 2026-11-13 (research R8).
 *
 * Figma (idle): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-6641
 */
@Composable
internal fun NoteInput(
    value: String,
    isEditing: Boolean,
    onAddNoteClick: () -> Unit,
    onValueChange: (String) -> Unit,
    onFocusLost: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isEditing) {
        NoteField(
            value = value,
            onValueChange = onValueChange,
            onFocusLost = onFocusLost,
            modifier = modifier,
        )
    } else {
        AddNoteRow(onClick = onAddNoteClick, modifier = modifier)
    }
}

/**
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-6641
 */
@Composable
private fun AddNoteRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = SpendLessIcons.Add,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = colors.onSurface,
        )
        Text(
            text = stringResource(R.string.add_note),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface.copy(alpha = LabelAlpha),
            maxLines = 1,
        )
    }
}

/** Editing state. No Figma frame; user decision 2026-11-13 (research R8). */
@Composable
private fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusLost: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        color = colors.onSurface,
        textAlign = TextAlign.Center,
    )
    val focusRequester = remember { FocusRequester() }
    var hasHadFocus by remember { mutableStateOf(false) }
    val isPreview = LocalInspectionMode.current

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focus ->
                if (focus.isFocused) {
                    hasHadFocus = true
                } else if (hasHadFocus) {
                    hasHadFocus = false
                    onFocusLost()
                }
            },
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.add_note),
                        modifier = Modifier.fillMaxWidth(),
                        style = textStyle.copy(color = colors.onSurface.copy(alpha = LabelAlpha)),
                    )
                }
                innerTextField()
            }
        },
    )

    // Compose-internal: the field replaces the row the user just tapped, so it takes focus.
    LaunchedEffect(Unit) {
        if (!isPreview) focusRequester.requestFocus()
    }
}

// region Previews

@Composable
private fun NoteInputPreview(value: String, isEditing: Boolean) {
    SpendLessTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
            NoteInput(
                value = value,
                isEditing = isEditing,
                onAddNoteClick = {},
                onValueChange = {},
                onFocusLost = {},
                modifier = Modifier.width(300.dp),
            )
        }
    }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-6641 */
@Preview
@Composable
private fun NoteInputIdlePreview() = NoteInputPreview(value = "", isEditing = false)

/** No Figma frame (research R8). */
@Preview
@Composable
private fun NoteInputEditingEmptyPreview() = NoteInputPreview(value = "", isEditing = true)

/** No Figma frame (research R8). */
@Preview
@Composable
private fun NoteInputEditingMultiLinePreview() = NoteInputPreview(
    value = "Shared the birthday gift for Maggy with Rick, he paid me back his half in cash.",
    isEditing = true,
)

// endregion
