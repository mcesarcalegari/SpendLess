package com.plcoding.spendlesssample.transactions.presentation.mapper

import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferences
import com.plcoding.spendlesssample.core.presentation.format.AmountFormatter
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import com.plcoding.spendlesssample.transactions.presentation.format.DayHeaderFormatter
import com.plcoding.spendlesssample.transactions.presentation.list.model.DayGroupUi
import com.plcoding.spendlesssample.transactions.presentation.list.model.TransactionUi
import java.time.LocalDate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** Maps domain transactions to list rows and day groups (FR-001 to FR-005). */
class TransactionUiMapper(
    private val amountFormatter: AmountFormatter,
    private val dayHeaderFormatter: DayHeaderFormatter,
) {

    fun toUi(transaction: Transaction, prefs: AmountFormatPreferences): TransactionUi {
        val isExpense = transaction.type == TransactionType.EXPENSE
        // An expense always has a category (data-model §1); OTHER is the defensive fallback.
        val category = transaction.category ?: ExpenseCategory.Default
        return TransactionUi(
            id = transaction.id,
            type = transaction.type,
            iconEmoji = if (isExpense) CategoryUiMapper.emoji(category) else CategoryUiMapper.INCOME_EMOJI,
            title = transaction.counterparty,
            label = if (isExpense) CategoryUiMapper.label(category) else CategoryUiMapper.incomeLabel,
            formattedAmount = amountFormatter.format(transaction.amountMinor, isNegative = isExpense, prefs = prefs),
            note = transaction.note,
            seriesId = transaction.seriesId,
            // Running-series state is wired by the stop-repeating story (US6).
            isInRunningSeries = false,
        )
    }

    /**
     * Groups by each transaction's local date in its own recorded zone, newest day first and newest
     * `recordedAt` first within a day.
     */
    fun toGroups(
        transactions: List<Transaction>,
        today: LocalDate,
        prefs: AmountFormatPreferences,
    ): ImmutableList<DayGroupUi> = transactions
        .sortedByDescending { it.recordedAt }
        .groupBy { it.recordedAt.atZone(it.zoneId).toLocalDate() }
        .toSortedMap(compareByDescending { it })
        .map { (date, dayTransactions) ->
            DayGroupUi(
                dateKey = date.toEpochDay(),
                header = dayHeaderFormatter.format(date, today),
                items = dayTransactions.map { toUi(it, prefs) }.toImmutableList(),
            )
        }
        .toImmutableList()
}
