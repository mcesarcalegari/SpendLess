package com.plcoding.spendlesssample.transactions.presentation.mapper

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferences
import com.plcoding.spendlesssample.core.domain.format.NegativeStyle
import com.plcoding.spendlesssample.core.presentation.UiText
import com.plcoding.spendlesssample.core.presentation.format.AmountFormatter
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import com.plcoding.spendlesssample.transactions.presentation.R
import com.plcoding.spendlesssample.transactions.presentation.format.DayHeaderFormatter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.jupiter.api.Test

class TransactionUiMapperTest {

    private val mapper = TransactionUiMapper(AmountFormatter(), DayHeaderFormatter())
    private val prefs = AmountFormatPreferences.Default
    private val today = LocalDate.of(2026, 1, 15)

    private fun tx(
        id: Long,
        at: LocalDateTime,
        type: TransactionType = TransactionType.EXPENSE,
        amountMinor: Long = 4_599,
        category: ExpenseCategory? = if (type == TransactionType.EXPENSE) ExpenseCategory.HEALTH_AND_WELLNESS else null,
        note: String? = null,
        zone: ZoneId = ZoneOffset.UTC,
        seriesId: Long? = null,
    ) = Transaction(
        id = id,
        type = type,
        counterparty = "Name $id",
        amountMinor = amountMinor,
        note = note,
        category = category,
        repeat = RepeatFrequency.DOES_NOT_REPEAT,
        recordedAt = at.atZone(zone).toInstant(),
        zoneId = zone,
        seriesId = seriesId,
        occurrenceDate = null,
    )

    @Test
    fun `expense maps to its category emoji, category label and a negative amount`() {
        val ui = mapper.toUi(tx(1, today.atTime(9, 0), note = "Gym"), prefs)

        assertThat(ui.id).isEqualTo(1L)
        assertThat(ui.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(ui.iconEmoji).isEqualTo("❤️")
        assertThat(ui.title).isEqualTo("Name 1")
        assertThat(ui.label).isEqualTo(UiText.StringResource(R.string.category_health_and_wellness))
        assertThat(ui.formattedAmount).isEqualTo("-$45.99")
        assertThat(ui.note).isEqualTo("Gym")
    }

    @Test
    fun `income maps to the money bag, the Income label and a positive amount`() {
        val ui = mapper.toUi(tx(2, today.atTime(9, 0), type = TransactionType.INCOME, amountMinor = 2_000), prefs)

        assertThat(ui.type).isEqualTo(TransactionType.INCOME)
        assertThat(ui.iconEmoji).isEqualTo("💰")
        assertThat(ui.label).isEqualTo(UiText.StringResource(R.string.income))
        assertThat(ui.formattedAmount).isEqualTo("$20.00")
    }

    @Test
    fun `an expense with a missing category falls back to Other`() {
        val ui = mapper.toUi(tx(3, today.atTime(9, 0), category = null), prefs)

        assertThat(ui.iconEmoji).isEqualTo("⚙️")
        assertThat(ui.label).isEqualTo(UiText.StringResource(R.string.category_other))
    }

    @Test
    fun `a missing note stays null and series fields pass through`() {
        val ui = mapper.toUi(tx(4, today.atTime(9, 0), seriesId = 4), prefs)

        assertThat(ui.note).isNull()
        assertThat(ui.seriesId).isEqualTo(4L)
        assertThat(ui.isInRunningSeries).isFalse()
    }

    @Test
    fun `the amount follows the negative style preference`() {
        val parentheses = prefs.copy(negativeStyle = NegativeStyle.PARENTHESES)

        assertThat(mapper.toUi(tx(5, today.atTime(9, 0)), parentheses).formattedAmount).isEqualTo("($45.99)")
    }

    @Test
    fun `no transactions give no groups`() {
        assertThat(mapper.toGroups(emptyList(), today, prefs)).isEmpty()
    }

    @Test
    fun `groups by local day, newest day first, with TODAY, YESTERDAY and dated headers`() {
        val input = listOf(
            tx(1, today.atTime(18, 0)),
            tx(2, today.atTime(8, 0)),
            tx(3, today.minusDays(1).atTime(12, 0)),
            tx(4, LocalDate.of(2026, 1, 9).atTime(12, 0)),
        )

        val groups = mapper.toGroups(input, today, prefs)

        assertThat(groups.map { it.header }).containsExactly(
            UiText.StringResource(R.string.today),
            UiText.StringResource(R.string.yesterday),
            UiText.DynamicString("JANUARY 9"),
        )
        assertThat(groups.map { group -> group.items.map { it.id } })
            .containsExactly(listOf(1L, 2L), listOf(3L), listOf(4L))
        assertThat(groups.map { it.dateKey }).containsExactly(
            today.toEpochDay(),
            today.minusDays(1).toEpochDay(),
            LocalDate.of(2026, 1, 9).toEpochDay(),
        )
    }

    @Test
    fun `sorts by recorded time even when the input is unordered`() {
        val input = listOf(
            tx(1, LocalDate.of(2026, 1, 9).atTime(12, 0)),
            tx(2, today.atTime(8, 0)),
            tx(3, today.atTime(18, 0)),
        )

        val groups = mapper.toGroups(input, today, prefs)

        assertThat(groups.map { group -> group.items.map { it.id } })
            .containsExactly(listOf(3L, 2L), listOf(1L))
    }

    @Test
    fun `uses each transaction's own zone to find its local day`() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        // 00:30 on the 15th in Tokyo is 15:30 on the 14th in UTC.
        val recordedInTokyo = tx(1, today.atTime(0, 30), zone = tokyo)
        val recordedInUtc = tx(2, today.minusDays(1).atTime(16, 0))

        val groups = mapper.toGroups(listOf(recordedInUtc, recordedInTokyo), today, prefs)

        assertThat(groups.map { it.header }).containsExactly(
            UiText.StringResource(R.string.today),
            UiText.StringResource(R.string.yesterday),
        )
        assertThat(groups.map { group -> group.items.map { it.id } })
            .containsExactly(listOf(1L), listOf(2L))
    }
}
