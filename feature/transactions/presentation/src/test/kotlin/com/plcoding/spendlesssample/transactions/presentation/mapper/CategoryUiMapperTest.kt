package com.plcoding.spendlesssample.transactions.presentation.mapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.plcoding.spendlesssample.core.presentation.UiText
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.presentation.R
import java.util.stream.Stream
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class CategoryUiMapperTest {

    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("categories")
    fun `each category maps to its emoji and name`(category: ExpenseCategory, emoji: String, nameRes: Int) {
        assertThat(CategoryUiMapper.emoji(category)).isEqualTo(emoji)
        assertThat(CategoryUiMapper.label(category)).isEqualTo(UiText.StringResource(nameRes))
    }

    @Test
    fun `income uses the money bag emoji and the Income label`() {
        assertThat(CategoryUiMapper.INCOME_EMOJI).isEqualTo("💰")
        assertThat(CategoryUiMapper.incomeLabel).isEqualTo(UiText.StringResource(R.string.income))
    }

    companion object {
        // ❤️ and ⚙️ carry the U+FE0F variation selector (FR-026).
        @JvmStatic
        fun categories(): Stream<Arguments> = Stream.of(
            Arguments.of(ExpenseCategory.CLOTHING_AND_ACCESSORIES, "👔", R.string.category_clothing_and_accessories),
            Arguments.of(ExpenseCategory.EDUCATION, "🎓", R.string.category_education),
            Arguments.of(ExpenseCategory.ENTERTAINMENT, "💻", R.string.category_entertainment),
            Arguments.of(ExpenseCategory.FOOD_AND_GROCERIES, "🍕", R.string.category_food_and_groceries),
            Arguments.of(ExpenseCategory.HEALTH_AND_WELLNESS, "❤️", R.string.category_health_and_wellness),
            Arguments.of(ExpenseCategory.HOME, "🏠", R.string.category_home),
            Arguments.of(ExpenseCategory.OTHER, "⚙️", R.string.category_other),
        )
    }
}
