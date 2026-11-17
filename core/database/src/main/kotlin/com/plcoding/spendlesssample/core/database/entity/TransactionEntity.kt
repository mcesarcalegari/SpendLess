package com.plcoding.spendlesssample.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One stored transaction: a one-time entry, a recurring original, or a materialised occurrence.
 *
 * Enum columns hold the domain enum names as text; `:core:database` cannot see the domain enums,
 * so mapping happens in `:feature:transactions:data`.
 *
 * The unique `(series_id, occurrence_date_epoch_day)` index guarantees each occurrence exists at
 * most once. SQLite treats NULLs as distinct, so one-time rows and originals never collide.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["recorded_at_epoch_ms"]),
        Index(value = ["series_id"]),
        Index(value = ["series_id", "occurrence_date_epoch_day"], unique = true),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "counterparty")
    val counterparty: String,
    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,
    @ColumnInfo(name = "note")
    val note: String?,
    @ColumnInfo(name = "category")
    val category: String?,
    @ColumnInfo(name = "repeat")
    val repeat: String,
    @ColumnInfo(name = "recorded_at_epoch_ms")
    val recordedAtEpochMs: Long,
    @ColumnInfo(name = "zone_id")
    val zoneId: String,
    @ColumnInfo(name = "series_id")
    val seriesId: Long?,
    @ColumnInfo(name = "occurrence_date_epoch_day")
    val occurrenceDateEpochDay: Long?,
)
