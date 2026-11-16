package com.plcoding.spendlesssample.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** Schedule state of a recurring series. [seriesId] is the original transaction's id. */
@Entity(
    tableName = "recurring_series",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["series_id"],
        ),
    ],
)
data class RecurringSeriesEntity(
    @PrimaryKey
    @ColumnInfo(name = "series_id")
    val seriesId: Long,
    @ColumnInfo(name = "frequency")
    val frequency: String,
    @ColumnInfo(name = "anchor_epoch_day")
    val anchorEpochDay: Long,
    @ColumnInfo(name = "anchor_seconds_of_day")
    val anchorSecondsOfDay: Int,
    @ColumnInfo(name = "zone_id")
    val zoneId: String,
    @ColumnInfo(name = "stopped_on_epoch_day")
    val stoppedOnEpochDay: Long?,
    @ColumnInfo(name = "last_materialized_epoch_day")
    val lastMaterializedEpochDay: Long,
)
