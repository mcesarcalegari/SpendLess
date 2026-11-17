package com.plcoding.spendlesssample.core.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.plcoding.spendlesssample.core.domain.util.DataError
import org.junit.jupiter.api.Test

class DataErrorUiTextTest {

    @Test
    fun `disk full maps to the storage-full message`() {
        assertThat(DataError.Local.DISK_FULL.toUiText())
            .isInstanceOf(UiText.StringResource::class)
            .prop(UiText.StringResource::id)
            .isEqualTo(R.string.error_disk_full)
    }

    @Test
    fun `not found maps to the generic save message`() {
        assertThat(DataError.Local.NOT_FOUND.toUiText())
            .isInstanceOf(UiText.StringResource::class)
            .prop(UiText.StringResource::id)
            .isEqualTo(R.string.error_save_failed)
    }

    @Test
    fun `unknown maps to the generic save message`() {
        assertThat(DataError.Local.UNKNOWN.toUiText())
            .isInstanceOf(UiText.StringResource::class)
            .prop(UiText.StringResource::id)
            .isEqualTo(R.string.error_save_failed)
    }

    @Test
    fun `string resources with the same id and args are equal`() {
        assertThat(UiText.StringResource(R.string.error_disk_full, arrayOf("a", 1)))
            .isEqualTo(UiText.StringResource(R.string.error_disk_full, arrayOf("a", 1)))
    }
}
