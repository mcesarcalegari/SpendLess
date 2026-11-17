package com.plcoding.spendlesssample.core.data.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * System clock whose zone is the device's **current** default zone.
 *
 * `Clock.systemDefaultZone()` captures the zone once, at creation, so a singleton built from it
 * would keep the old zone after the user travels or changes the time-zone setting. This clock
 * re-reads [ZoneId.systemDefault] on every call, so local dates and the zone recorded with a
 * transaction always follow the device (FR-002).
 */
class SystemDefaultZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()

    override fun millis(): Long = System.currentTimeMillis()
}
