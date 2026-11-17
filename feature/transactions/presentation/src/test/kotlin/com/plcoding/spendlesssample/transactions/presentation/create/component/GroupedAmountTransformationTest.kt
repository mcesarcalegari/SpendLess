package com.plcoding.spendlesssample.transactions.presentation.create.component

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import org.junit.jupiter.api.Test

class GroupedAmountTransformationTest {

    private fun transform(raw: String, grouped: String) =
        GroupedAmountTransformation(grouped).filter(AnnotatedString(raw))

    @Test
    fun `shows the grouped text`() {
        assertThat(transform("1234567", "1,234,567").text.text).isEqualTo("1,234,567")
    }

    @Test
    fun `original offsets skip over inserted separators`() {
        val mapping = transform("1234567", "1,234,567").offsetMapping
        assertThat((0..7).map(mapping::originalToTransformed)).isEqualTo(listOf(0, 2, 3, 4, 6, 7, 8, 9))
    }

    @Test
    fun `transformed offsets on a separator map to the digit after it`() {
        val mapping = transform("1234567", "1,234,567").offsetMapping
        assertThat((0..9).map(mapping::transformedToOriginal)).isEqualTo(listOf(0, 1, 1, 2, 3, 4, 4, 5, 6, 7))
    }

    @Test
    fun `decimals after the separator map one to one`() {
        val mapping = transform("1234.5", "1,234.5").offsetMapping
        assertThat(mapping.originalToTransformed(6)).isEqualTo(7)
        assertThat(mapping.originalToTransformed(5)).isEqualTo(6)
        assertThat(mapping.transformedToOriginal(7)).isEqualTo(6)
    }

    @Test
    fun `space as thousands separator and comma decimals`() {
        val result = transform("12000,5", "12 000,5")
        assertThat(result.text.text).isEqualTo("12 000,5")
        assertThat(result.offsetMapping.originalToTransformed(2)).isEqualTo(3)
    }

    @Test
    fun `no grouping needed is identity-like`() {
        val mapping = transform("123", "123").offsetMapping
        assertThat((0..3).map(mapping::originalToTransformed)).isEqualTo(listOf(0, 1, 2, 3))
    }

    @Test
    fun `stale grouped text falls back to the raw text and identity mapping`() {
        // The raw text was just edited (a digit deleted) before the grouped text caught up.
        val result = transform("12345", "1,234")
        assertThat(result.text.text).isEqualTo("12345")
        assertThat(result.offsetMapping).isSameInstanceAs(OffsetMapping.Identity)
    }

    @Test
    fun `every offset stays in range`() {
        val raw = "999999999.99"
        val grouped = "999,999,999.99"
        val mapping = transform(raw, grouped).offsetMapping
        (0..raw.length).forEach { assertThat(mapping.originalToTransformed(it) in 0..grouped.length).isEqualTo(true) }
        (0..grouped.length).forEach { assertThat(mapping.transformedToOriginal(it) in 0..raw.length).isEqualTo(true) }
    }
}
