
package com.cosmotrinity.cosmonavigator.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores one permanent daily study-history snapshot.
 *
 * Date format: DD-MM-YYYY.
 *
 * All durations are stored in seconds.
 */
@Entity(
    tableName = "daily_history",
    indices = [
        Index(value = ["studyDate"], unique = true)
    ]
)
data class DailyHistoryEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val studyDate: String,

    // Total scheduled study time from enabled study activities.
    val targetStudySeconds: Long = 0L,

    // Actual tracked study time.
    val actualStudySeconds: Long = 0L,

    val totalActivities: Int = 0,

    val completedActivities: Int = 0,

    val missedActivities: Int = 0,

    val pendingActivities: Int = 0,

    val completionPercentage: Int = 0,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Stores an independent snapshot of each activity for a date.
 *
 * Activity details are copied so changes to the reusable
 * timetable cannot modify historical records.
 */
@Entity(
    tableName = "daily_history_activities",
    foreignKeys = [
        ForeignKey(
            entity = DailyHistoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["historyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["historyId"]),
        Index(value = ["historyId", "sourceActivityId"], unique = true)
    ]
)
data class DailyHistoryActivityEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val historyId: Int,

    // Original timetable activity ID, retained for reference.
    val sourceActivityId: Int,

    // Copied activity details.
    val title: String,

    val activityType: String,

    val countsAsStudy: Boolean,

    val startTimeMinutes: Int,

    val endTimeMinutes: Int,

    val scheduledDurationSeconds: Long,

    // PENDING, DONE, or NOT_DONE.
    val status: String = "PENDING",

    // Actual time tracked for this activity.
    val actualStudySeconds: Long = 0L,

    val completedAt: Long? = null,

    val notes: String = "",

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)
