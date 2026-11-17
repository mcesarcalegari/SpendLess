package com.plcoding.spendlesssample.transactions.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.prop
import com.plcoding.spendlesssample.core.domain.util.Result
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import com.plcoding.spendlesssample.transactions.domain.model.TransactionDraft
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import com.plcoding.spendlesssample.transactions.domain.model.TransactionValidationError
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.jupiter.api.Test

class TransactionValidatorTest {

    private val now = Instant.parse("2026-01-15T10:30:00Z")
    private val zone = ZoneId.of("Europe/Berlin")
    private val validator = TransactionValidator(Clock.fixed(now, zone))

    private fun draft(
        type: TransactionType = TransactionType.EXPENSE,
        name: String = "Netflix",
        amountMinor: Long = 1_599,
        note: String? = null,
        category: ExpenseCategory? = ExpenseCategory.ENTERTAINMENT,
        repeat: RepeatFrequency = RepeatFrequency.DOES_NOT_REPEAT,
    ) = TransactionDraft(type, name, amountMinor, note, category, repeat)

    private fun success(draft: TransactionDraft): Transaction {
        val result = validator.validate(draft)
        assertThat(result).isInstanceOf<Result.Success<Transaction>>()
        return (result as Result.Success).data
    }

    private fun errorOf(draft: TransactionDraft): TransactionValidationError {
        val result = validator.validate(draft)
        assertThat(result).isInstanceOf<Result.Error<TransactionValidationError>>()
        return (result as Result.Error).error
    }

    @Test
    fun `valid expense is stamped with the clock and trimmed`() {
        val tx = success(draft(name = "  Netflix  ", note = "  monthly plan  "))

        assertThat(tx).isEqualTo(
            Transaction(
                id = 0,
                type = TransactionType.EXPENSE,
                counterparty = "Netflix",
                amountMinor = 1_599,
                note = "monthly plan",
                category = ExpenseCategory.ENTERTAINMENT,
                repeat = RepeatFrequency.DOES_NOT_REPEAT,
                recordedAt = now,
                zoneId = zone,
                seriesId = null,
                occurrenceDate = null,
            ),
        )
    }

    @Test
    fun `repeat is carried over from the draft`() {
        assertThat(success(draft(repeat = RepeatFrequency.MONTHLY)))
            .prop(Transaction::repeat).isEqualTo(RepeatFrequency.MONTHLY)
    }

    @Test
    fun `empty name is NAME_BLANK`() {
        assertThat(errorOf(draft(name = ""))).isEqualTo(TransactionValidationError.NAME_BLANK)
    }

    @Test
    fun `whitespace-only name is NAME_BLANK`() {
        assertThat(errorOf(draft(name = "   \t "))).isEqualTo(TransactionValidationError.NAME_BLANK)
    }

    @Test
    fun `single character name is accepted`() {
        assertThat(success(draft(name = "A"))).prop(Transaction::counterparty).isEqualTo("A")
    }

    @Test
    fun `50 characters after trim are accepted`() {
        val name = "a".repeat(50)
        assertThat(success(draft(name = "  $name  "))).prop(Transaction::counterparty).isEqualTo(name)
    }

    @Test
    fun `51 characters after trim are NAME_TOO_LONG`() {
        assertThat(errorOf(draft(name = " " + "a".repeat(51) + " ")))
            .isEqualTo(TransactionValidationError.NAME_TOO_LONG)
    }

    @Test
    fun `amount 0 is AMOUNT_NOT_POSITIVE`() {
        assertThat(errorOf(draft(amountMinor = 0))).isEqualTo(TransactionValidationError.AMOUNT_NOT_POSITIVE)
    }

    @Test
    fun `negative amount is AMOUNT_NOT_POSITIVE`() {
        assertThat(errorOf(draft(amountMinor = -1))).isEqualTo(TransactionValidationError.AMOUNT_NOT_POSITIVE)
    }

    @Test
    fun `amount 1 is accepted`() {
        assertThat(success(draft(amountMinor = 1))).prop(Transaction::amountMinor).isEqualTo(1L)
    }

    @Test
    fun `maximum amount 99_999_999_999 is accepted`() {
        assertThat(success(draft(amountMinor = 99_999_999_999)))
            .prop(Transaction::amountMinor).isEqualTo(99_999_999_999L)
    }

    @Test
    fun `amount 100_000_000_000 is AMOUNT_TOO_LARGE`() {
        assertThat(errorOf(draft(amountMinor = 100_000_000_000)))
            .isEqualTo(TransactionValidationError.AMOUNT_TOO_LARGE)
    }

    @Test
    fun `note of 500 characters is accepted`() {
        val note = "n".repeat(500)
        assertThat(success(draft(note = note))).prop(Transaction::note).isEqualTo(note)
    }

    @Test
    fun `note of 501 characters is NOTE_TOO_LONG`() {
        assertThat(errorOf(draft(note = "n".repeat(501)))).isEqualTo(TransactionValidationError.NOTE_TOO_LONG)
    }

    @Test
    fun `note length is checked after trimming`() {
        val note = "n".repeat(500)
        assertThat(success(draft(note = "   $note   "))).prop(Transaction::note).isEqualTo(note)
    }

    @Test
    fun `whitespace-only note becomes null`() {
        assertThat(success(draft(note = "  \n  "))).prop(Transaction::note).isNull()
    }

    @Test
    fun `empty note becomes null`() {
        assertThat(success(draft(note = ""))).prop(Transaction::note).isNull()
    }

    @Test
    fun `null note stays null`() {
        assertThat(success(draft(note = null))).prop(Transaction::note).isNull()
    }

    @Test
    fun `expense without category is CATEGORY_MISSING_FOR_EXPENSE`() {
        assertThat(errorOf(draft(category = null)))
            .isEqualTo(TransactionValidationError.CATEGORY_MISSING_FOR_EXPENSE)
    }

    @Test
    fun `valid income without category is stamped with the clock and keeps a null category`() {
        val tx = success(draft(type = TransactionType.INCOME, name = " Rick ", amountMinor = 2_000, category = null))

        assertThat(tx).isEqualTo(
            Transaction(
                id = 0,
                type = TransactionType.INCOME,
                counterparty = "Rick",
                amountMinor = 2_000,
                note = null,
                category = null,
                repeat = RepeatFrequency.DOES_NOT_REPEAT,
                recordedAt = now,
                zoneId = zone,
                seriesId = null,
                occurrenceDate = null,
            ),
        )
    }

    @Test
    fun `income with a category is CATEGORY_NOT_ALLOWED_FOR_INCOME`() {
        assertThat(errorOf(draft(type = TransactionType.INCOME, category = ExpenseCategory.HOME)))
            .isEqualTo(TransactionValidationError.CATEGORY_NOT_ALLOWED_FOR_INCOME)
    }

    @Test
    fun `income with the default Other category is CATEGORY_NOT_ALLOWED_FOR_INCOME`() {
        assertThat(errorOf(draft(type = TransactionType.INCOME, category = ExpenseCategory.OTHER)))
            .isEqualTo(TransactionValidationError.CATEGORY_NOT_ALLOWED_FOR_INCOME)
    }

    @Test
    fun `income keeps its repeat frequency`() {
        assertThat(success(draft(type = TransactionType.INCOME, category = null, repeat = RepeatFrequency.WEEKLY)))
            .prop(Transaction::repeat).isEqualTo(RepeatFrequency.WEEKLY)
    }

    @Test
    fun `income name and amount rules still apply`() {
        assertThat(errorOf(draft(type = TransactionType.INCOME, name = "  ", category = null)))
            .isEqualTo(TransactionValidationError.NAME_BLANK)
        assertThat(errorOf(draft(type = TransactionType.INCOME, amountMinor = 0, category = null)))
            .isEqualTo(TransactionValidationError.AMOUNT_NOT_POSITIVE)
    }
}
