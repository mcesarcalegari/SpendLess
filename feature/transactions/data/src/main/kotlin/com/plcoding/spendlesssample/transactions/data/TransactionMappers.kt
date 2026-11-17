package com.plcoding.spendlesssample.transactions.data

import com.plcoding.spendlesssample.core.database.entity.RecurringSeriesEntity
import com.plcoding.spendlesssample.core.database.entity.TransactionEntity
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RecurringSeries
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

fun TransactionEntity.toTransaction(): Transaction = Transaction(
    id = id,
    type = TransactionType.valueOf(type),
    counterparty = counterparty,
    amountMinor = amountMinor,
    note = note,
    category = category?.let(ExpenseCategory::valueOf),
    repeat = RepeatFrequency.valueOf(repeat),
    recordedAt = Instant.ofEpochMilli(recordedAtEpochMs),
    zoneId = ZoneId.of(zoneId),
    seriesId = seriesId,
    occurrenceDate = occurrenceDateEpochDay?.let(LocalDate::ofEpochDay),
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    type = type.name,
    counterparty = counterparty,
    amountMinor = amountMinor,
    note = note,
    category = category?.name,
    repeat = repeat.name,
    recordedAtEpochMs = recordedAt.toEpochMilli(),
    zoneId = zoneId.id,
    seriesId = seriesId,
    occurrenceDateEpochDay = occurrenceDate?.toEpochDay(),
)

fun RecurringSeriesEntity.toRecurringSeries(): RecurringSeries = RecurringSeries(
    seriesId = seriesId,
    frequency = RepeatFrequency.valueOf(frequency),
    anchorDate = LocalDate.ofEpochDay(anchorEpochDay),
    anchorTime = LocalTime.ofSecondOfDay(anchorSecondsOfDay.toLong()),
    zoneId = ZoneId.of(zoneId),
    stoppedOn = stoppedOnEpochDay?.let(LocalDate::ofEpochDay),
    lastMaterializedDate = LocalDate.ofEpochDay(lastMaterializedEpochDay),
)
