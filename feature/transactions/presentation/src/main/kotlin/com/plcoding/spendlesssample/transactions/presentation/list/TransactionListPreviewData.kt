package com.plcoding.spendlesssample.transactions.presentation.list

import com.plcoding.spendlesssample.core.presentation.UiText
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import com.plcoding.spendlesssample.transactions.presentation.list.model.DayGroupUi
import com.plcoding.spendlesssample.transactions.presentation.list.model.TransactionUi
import com.plcoding.spendlesssample.transactions.presentation.mapper.CategoryUiMapper
import kotlinx.collections.immutable.persistentListOf

/** Fixed sample data for previews only, mirroring the Figma list frame `2417:12042`. */
internal object TransactionListPreviewData {

    private var nextId = 1L

    fun expense(
        title: String,
        category: ExpenseCategory,
        amount: String,
        note: String? = null,
    ) = TransactionUi(
        id = nextId++,
        type = TransactionType.EXPENSE,
        iconEmoji = CategoryUiMapper.emoji(category),
        title = title,
        label = CategoryUiMapper.label(category),
        formattedAmount = amount,
        note = note,
        seriesId = null,
        isInRunningSeries = false,
    )

    fun income(title: String, amount: String, note: String? = null) = TransactionUi(
        id = nextId++,
        type = TransactionType.INCOME,
        iconEmoji = CategoryUiMapper.INCOME_EMOJI,
        title = title,
        label = CategoryUiMapper.incomeLabel,
        formattedAmount = amount,
        note = note,
        seriesId = null,
        isInRunningSeries = false,
    )

    val amazon = expense("Amazon", ExpenseCategory.HOME, "-$45.99")
    val starbucks = expense("Starbucks", ExpenseCategory.FOOD_AND_GROCERIES, "-$7.50")
    val ricksShare = income("Rick’s share - Birthday M.", "$20.00")
    /** The note drawn in `2414:5142`. */
    private const val STARBUCKS_NOTE = "Enjoyed a coffee and a snack at Starbucks with Rick and M."
    val starbucksWithNote = expense("Starbucks", ExpenseCategory.FOOD_AND_GROCERIES, "-$7.50", STARBUCKS_NOTE)
    val ricksShareWithNote = income("Rick’s share - Birthday M.", "$20.00", STARBUCKS_NOTE)

    /** A note at the 500-character limit (data-model §1). */
    val longNote = ("Paint, brushes, tape and two new shelves for the hallway. " .repeat(9)).take(500)

    val netflix = expense("Netflix", ExpenseCategory.ENTERTAINMENT, "-$15.99")
    /** A row of a running monthly series: it looks like any other row (FR-037). */
    val gym = expense("Gym - Monthly Membership", ExpenseCategory.HEALTH_AND_WELLNESS, "-$45.99")
        .let { it.copy(seriesId = it.id, isInRunningSeries = true) }

    /** The four day groups of `2417:12042`; the rows drawn with a note badge carry a note. */
    val groups = persistentListOf(
        DayGroupUi(
            dateKey = 4,
            header = UiText.DynamicString("TODAY"),
            items = persistentListOf(amazon, starbucksWithNote),
        ),
        DayGroupUi(
            dateKey = 3,
            header = UiText.DynamicString("YESTERDAY"),
            items = persistentListOf(ricksShareWithNote, netflix, gym),
        ),
        DayGroupUi(
            dateKey = 2,
            header = UiText.DynamicString("JANUARY 9"),
            items = persistentListOf(
                expense("Home Depot", ExpenseCategory.HOME, "-$35.60"),
                expense("Starbucks", ExpenseCategory.FOOD_AND_GROCERIES, "-$7.50", STARBUCKS_NOTE),
                expense("Netflix", ExpenseCategory.ENTERTAINMENT, "-$15.99"),
                expense("Starbucks", ExpenseCategory.FOOD_AND_GROCERIES, "-$7.50"),
            ),
        ),
        DayGroupUi(
            dateKey = 1,
            header = UiText.DynamicString("DECEMBER 28"),
            items = persistentListOf(
                income("Rick’s share - Birthday M.", "$20.00", STARBUCKS_NOTE),
                expense("Netflix", ExpenseCategory.ENTERTAINMENT, "-$15.99"),
                expense("Gym - Monthly Membership", ExpenseCategory.HEALTH_AND_WELLNESS, "-$45.99"),
            ),
        ),
    )
}
