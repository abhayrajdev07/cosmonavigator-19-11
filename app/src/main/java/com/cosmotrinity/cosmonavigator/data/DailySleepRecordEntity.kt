
package com.cosmotrinity.cosmonavigator.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores actual sleep and wake-up records.
 *
 * Each record is associated with the date on which
 * the user wakes up.
 *
 * The sleep timestamp may belong to the previous day.
 * Date format: YYYY-MM-DD.
 */
@Entity(tableName = "daily_sleep_records")
data class DailySleepRecordEntity(

    @PrimaryKey
    val recordDate: String,

    // Planned timings captured for this sleep cycle.
    val plannedWakeUpTimeMinutes: Int,
    val plannedSleepTimeMinutes: Int,

    // Actual timestamps in milliseconds.
    val actualSleepAt: Long? = null,
    val actualWakeUpAt: Long? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
