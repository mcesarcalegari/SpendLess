package com.plcoding.spendlesssample.core.presentation.format

import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferences

/**
 * Validates amount keystrokes and converts raw input to minor units (FR-013).
 *
 * Raw input holds only digits and at most one preferred decimal separator, with at most
 * [MAX_WHOLE_DIGITS] whole digits and [MAX_DECIMALS] decimals, so the maximum is 999,999,999.99.
 */
class AmountInputParser {

    /** Returns [proposed] when it is valid raw input, or `null` to reject the keystroke. */
    @Suppress("UNUSED_PARAMETER")
    fun accept(current: String, proposed: String, prefs: AmountFormatPreferences): String? {
        val separatorIndex = proposed.indexOf(prefs.decimalSeparator)
        val whole = if (separatorIndex >= 0) proposed.substring(0, separatorIndex) else proposed
        val decimals = if (separatorIndex >= 0) proposed.substring(separatorIndex + 1) else ""
        val valid = whole.all(Char::isAsciiDigit) &&
            decimals.all(Char::isAsciiDigit) &&
            whole.length <= MAX_WHOLE_DIGITS &&
            decimals.length <= MAX_DECIMALS
        return proposed.takeIf { valid }
    }

    /** Minor units of valid raw input, or `null` when it is empty or zero. */
    fun toMinorUnits(raw: String, prefs: AmountFormatPreferences): Long? {
        if (accept(current = "", proposed = raw, prefs = prefs) == null) return null
        val separatorIndex = raw.indexOf(prefs.decimalSeparator)
        val whole = if (separatorIndex >= 0) raw.substring(0, separatorIndex) else raw
        val decimals = if (separatorIndex >= 0) raw.substring(separatorIndex + 1) else ""
        val minor = (whole.ifEmpty { "0" }.toLong() * MINOR_PER_MAJOR) +
            decimals.padEnd(MAX_DECIMALS, '0').toLong()
        return minor.takeIf { it > 0 }
    }

    private companion object {
        const val MAX_WHOLE_DIGITS = 9
        const val MAX_DECIMALS = 2
        const val MINOR_PER_MAJOR = 100L
    }
}

private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
