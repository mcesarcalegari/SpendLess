package com.plcoding.spendlesssample.transactions.presentation.mapper

import com.plcoding.spendlesssample.core.presentation.UiText
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.presentation.R

/** Emoji and display names of the fixed expense categories, and the income equivalents (FR-026). */
object CategoryUiMapper {

    const val INCOME_EMOJI = "💰"

    val incomeLabel: UiText = UiText.StringResource(R.string.income)

    fun emoji(category: ExpenseCategory): String = when (category) {
        ExpenseCategory.CLOTHING_AND_ACCESSORIES -> "👔"
        ExpenseCategory.EDUCATION -> "🎓"
        ExpenseCategory.ENTERTAINMENT -> "💻"
        ExpenseCategory.FOOD_AND_GROCERIES -> "🍕"
        ExpenseCategory.HEALTH_AND_WELLNESS -> "❤️"
        ExpenseCategory.HOME -> "🏠"
        ExpenseCategory.OTHER -> "⚙️"
    }

    fun label(category: ExpenseCategory): UiText = UiText.StringResource(
        when (category) {
            ExpenseCategory.CLOTHING_AND_ACCESSORIES -> R.string.category_clothing_and_accessories
            ExpenseCategory.EDUCATION -> R.string.category_education
            ExpenseCategory.ENTERTAINMENT -> R.string.category_entertainment
            ExpenseCategory.FOOD_AND_GROCERIES -> R.string.category_food_and_groceries
            ExpenseCategory.HEALTH_AND_WELLNESS -> R.string.category_health_and_wellness
            ExpenseCategory.HOME -> R.string.category_home
            ExpenseCategory.OTHER -> R.string.category_other
        },
    )
}
