package com.plcoding.spendlesssample.core.data.time

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SystemCurrentDateProviderTest {

    private val zone: ZoneId = ZoneOffset.UTC

    /** A clock that moves with the test scheduler's virtual time, plus a manual offset. */
    private class SchedulerClock(
        private val start: Instant,
        private val scheduler: TestCoroutineScheduler,
        var currentZone: ZoneId,
    ) : Clock() {
        var manualOffsetMillis = 0L
        override fun getZone(): ZoneId = currentZone
        override fun withZone(zone: ZoneId): Clock = SchedulerClock(start, scheduler, zone)
        override fun instant(): Instant =
            start.plusMillis(scheduler.currentTime + manualOffsetMillis)
    }

    private class FakeSignals : DateChangeSignals {
        val flow = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
        override fun signals() = flow
    }

    private fun instantAt(dateTime: LocalDateTime): Instant = dateTime.atZone(zone).toInstant()

    @Test
    fun `emits today immediately`() = runTest {
        val clock = SchedulerClock(instantAt(LocalDateTime.of(2026, 3, 10, 9, 0)), testScheduler, zone)
        val provider = SystemCurrentDateProvider(clock, FakeSignals())

        provider.today.test {
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 3, 10))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits the next date once the midnight delay elapses`() = runTest {
        val clock = SchedulerClock(instantAt(LocalDateTime.of(2026, 3, 10, 23, 59)), testScheduler, zone)
        val provider = SystemCurrentDateProvider(clock, FakeSignals())

        provider.today.test {
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 3, 10))

            advanceTimeBy(59_000)
            runCurrent()
            expectNoEvents()

            advanceTimeBy(1_001)
            runCurrent()
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 3, 11))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keeps ticking at every following midnight`() = runTest {
        val clock = SchedulerClock(instantAt(LocalDateTime.of(2026, 3, 10, 23, 0)), testScheduler, zone)
        val provider = SystemCurrentDateProvider(clock, FakeSignals())

        provider.today.test {
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 3, 10))
            advanceTimeBy(HOUR + 1)
            runCurrent()
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 3, 11))
            advanceTimeBy(DAY)
            runCurrent()
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 3, 12))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `broadcast with an unchanged date emits nothing`() = runTest {
        val clock = SchedulerClock(instantAt(LocalDateTime.of(2026, 3, 10, 9, 0)), testScheduler, zone)
        val signals = FakeSignals()
        val provider = SystemCurrentDateProvider(clock, signals)

        provider.today.test {
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 3, 10))
            signals.flow.emit(Unit)
            runCurrent()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `broadcast after a manual clock change emits the new date`() = runTest {
        val clock = SchedulerClock(instantAt(LocalDateTime.of(2026, 3, 10, 9, 0)), testScheduler, zone)
        val signals = FakeSignals()
        val provider = SystemCurrentDateProvider(clock, signals)

        provider.today.test {
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 3, 10))
            clock.manualOffsetMillis = 3 * DAY
            signals.flow.emit(Unit)
            runCurrent()
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 3, 13))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `broadcast after a time-zone change emits the date in the new zone`() = runTest {
        // 23:30 UTC on Mar 10 is already Mar 11 in Tokyo (UTC+9).
        val clock = SchedulerClock(instantAt(LocalDateTime.of(2026, 3, 10, 23, 30)), testScheduler, zone)
        val signals = FakeSignals()
        val provider = SystemCurrentDateProvider(clock, signals)

        provider.today.test {
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 3, 10))
            clock.currentZone = ZoneId.of("Asia/Tokyo")
            signals.flow.emit(Unit)
            runCurrent()
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 3, 11))
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        const val HOUR = 60 * 60 * 1_000L
        const val DAY = 24 * HOUR
    }
}
