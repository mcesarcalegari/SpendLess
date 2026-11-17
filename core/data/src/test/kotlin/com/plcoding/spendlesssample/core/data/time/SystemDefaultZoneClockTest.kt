package com.plcoding.spendlesssample.core.data.time

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SystemDefaultZoneClockTest {

    private lateinit var originalZone: TimeZone

    @BeforeEach
    fun setUp() {
        originalZone = TimeZone.getDefault()
    }

    @AfterEach
    fun tearDown() {
        TimeZone.setDefault(originalZone)
    }

    @Test
    fun `zone follows a change of the system default zone`() {
        val clock = SystemDefaultZoneClock()

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
        assertThat(clock.zone).isEqualTo(ZoneId.of("Asia/Tokyo"))

        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
        assertThat(clock.zone).isEqualTo(ZoneId.of("America/Sao_Paulo"))
    }

    @Test
    fun `instant is the current system time`() {
        val before = Instant.now()
        val now = SystemDefaultZoneClock().instant()
        val after = Instant.now()

        assertThat(now).isBetween(before, after)
    }

    @Test
    fun `withZone returns a clock fixed to that zone`() {
        val tokyo = ZoneId.of("Asia/Tokyo")

        assertThat(SystemDefaultZoneClock().withZone(tokyo).zone).isEqualTo(tokyo)
    }
}
