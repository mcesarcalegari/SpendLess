package com.plcoding.spendlesssample.core.presentation

import com.plcoding.spendlesssample.core.domain.util.DataError

/** User-facing message for a storage failure (spec FR-022a; copy approved by the owner). */
fun DataError.toUiText(): UiText = when (this) {
    DataError.Local.DISK_FULL -> UiText.StringResource(R.string.error_disk_full)
    DataError.Local.NOT_FOUND,
    DataError.Local.UNKNOWN,
    -> UiText.StringResource(R.string.error_save_failed)
}
