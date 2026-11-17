
package com.cosmotrinity.cosmonavigator.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores the user's recurring daily timetable settings.
 * Time is stored as minutes from midnight.
 */
@Entity(tableName = "daily_timetable_settings")
data class DailyTimetableSettingsEntity(
    @PrimaryKey
    val id: Int = 1,

    val wakeUpTimeMinutes: Int = 300,

    val sleepTimeMinutes: Int = 1350,

    val sleepNextDay: Boolean = true,

    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Stores reusable daily timetable activities.
 */
@Entity(
    tableName = "daily_timetable_activities",
    indices = [
        Index(value = ["sortOrder"]),
        Index(value = ["isEnabled"])
    ]
)
data class DailyTimetableEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,

    val startTimeMinutes: Int,

    val endTimeMinutes: Int,

    val activityType: String = "Study",

    val countsAsStudy: Boolean = true,

    val sortOrder: Int = 0,

    val notes: String = "",

    val isEnabled: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Stores daily completion status for each timetable activity.
 *
 * Status:
 * PENDING  = Not yet completed
 * DONE     = Completed
 * NOT_DONE = Explicitly marked as missed
 *
 * Each activity can have a different status on different dates.
 */
@Entity(
    tableName = "daily_timetable_completions",
    indices = [
        Index(
            value = ["activityId", "studyDate"],
            unique = true
        ),
        Index(value = ["studyDate"])
    ]
)
data class DailyTimetableCompletionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val activityId: Int,

    // Date format: DD-MM-YYYY.
    val studyDate: String,

    // PENDING, DONE, or NOT_DONE.
    val status: String = "PENDING",

    // Exact time when marked DONE.
    val completedAt: Long? = null,

    // Exact time when the status was last changed.
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Stores actual study-time tracking segments.
 *
 * Every pause ends the current segment.
 * Resuming creates another segment for the same activity.
 */
@Entity(
    tableName = "daily_study_sessions",
    indices = [
        Index(value = ["activityId"]),
        Index(value = ["studyDate"]),
        Index(value = ["activityId", "studyDate"])
    ]
)
data class DailyStudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val activityId: Int,

    // Date format: DD-MM-YYYY.
    val studyDate: String,

    val startedAt: Long,

    val endedAt: Long? = null,

    // Actual duration of this segment, in seconds.
    val durationSeconds: Long = 0L,

    // RUNNING, PAUSED, or COMPLETED.
    val status: String = "RUNNING",

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)
