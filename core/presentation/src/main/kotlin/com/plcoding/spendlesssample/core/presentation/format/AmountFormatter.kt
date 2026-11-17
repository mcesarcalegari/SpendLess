package com.plcoding.spendlesssample.core.presentation.format

import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferences
import com.plcoding.spendlesssample.core.domain.format.NegativeStyle

/** An amount split for display: `prefix` + `number` + `suffix`, for example `($` `45.99` `)`. */
data class AmountParts(
    val prefix: String,
    val number: String,
    val suffix: String,
    val isPlaceholder: Boolean,
)

/**
 * Formats amounts held as positive `Long` minor units (FR-032). Never uses floating point.
 * The currency symbol always comes before the number; the sign comes from [NegativeStyle].
 */
class AmountFormatter {

    fun format(amountMinor: Long, isNegative: Boolean, prefs: AmountFormatPreferences): String {
        val parts = parts(amountMinor, isNegative, prefs)
        return parts.prefix + parts.number + parts.suffix
    }

    fun parts(amountMinor: Long?, isNegative: Boolean, prefs: AmountFormatPreferences): AmountParts {
        val prefix: String
        val suffix: String
        when {
            !isNegative -> {
                prefix = prefs.currencySymbol
                suffix = ""
            }
            prefs.negativeStyle == NegativeStyle.PARENTHESES -> {
                prefix = "(" + prefs.currencySymbol
                suffix = ")"
            }
            else -> {
                prefix = "-" + prefs.currencySymbol
                suffix = ""
            }
        }
        return if (amountMinor == null) {
            AmountParts(prefix, "00${prefs.decimalSeparator}00", suffix, isPlaceholder = true)
        } else {
            AmountParts(prefix, number(amountMinor, prefs), suffix, isPlaceholder = false)
        }
    }

    /** Groups the whole part of raw field input, keeping exactly what was typed after the separator. */
    fun groupInput(raw: String, prefs: AmountFormatPreferences): String {
        val separatorIndex = raw.indexOf(prefs.decimalSeparator)
        val whole = if (separatorIndex >= 0) raw.substring(0, separatorIndex) else raw
        val rest = if (separatorIndex >= 0) raw.substring(separatorIndex) else ""
        return group(whole, prefs.thousandsSeparator) + rest
    }

    private fun number(amountMinor: Long, prefs: AmountFormatPreferences): String {
        val whole = amountMinor / MINOR_PER_MAJOR
        val cents = amountMinor % MINOR_PER_MAJOR
        return group(whole.toString(), prefs.thousandsSeparator) +
            prefs.decimalSeparator +
            cents.toString().padStart(2, '0')
    }

    private fun group(digits: String, separator: Char): String =
        digits.reversed().chunked(GROUP_SIZE).joinToString(separator.toString()).reversed()

    private companion object {
        const val MINOR_PER_MAJOR = 100L
        const val GROUP_SIZE = 3
    }
}
