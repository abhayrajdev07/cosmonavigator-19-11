
package com.cosmotrinity.cosmonavigator.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTimetableDao {

    // DAILY TIMETABLE SETTINGS

    @Query("SELECT * FROM daily_timetable_settings WHERE id = 1")
    fun observeSettings(): Flow<DailyTimetableSettingsEntity?>

    @Query("SELECT * FROM daily_timetable_settings WHERE id = 1")
    suspend fun getSettings(): DailyTimetableSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: DailyTimetableSettingsEntity)
    @Query("DELETE FROM daily_timetable_settings WHERE id = 1")
    suspend fun deleteSettings()


    // RECURRING TIMETABLE ACTIVITIES

    @Query("""
        SELECT * FROM daily_timetable_activities
        ORDER BY startTimeMinutes ASC, sortOrder ASC
    """)
    fun observeActivities(): Flow<List<DailyTimetableEntity>>

    @Query("""
        SELECT * FROM daily_timetable_activities
        ORDER BY startTimeMinutes ASC, sortOrder ASC
    """)
    suspend fun getAllActivities(): List<DailyTimetableEntity>

    @Query("""
        SELECT * FROM daily_timetable_activities
        WHERE id = :activityId
        LIMIT 1
    """)
    suspend fun getActivityById(activityId: Int): DailyTimetableEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: DailyTimetableEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<DailyTimetableEntity>)

    @Update
    suspend fun updateActivity(activity: DailyTimetableEntity)

    @Delete
    suspend fun deleteActivity(activity: DailyTimetableEntity)

    @Query("""
        DELETE FROM daily_timetable_activities
        WHERE id = :activityId
    """)
    suspend fun deleteActivityById(activityId: Int)

    @Query("DELETE FROM daily_timetable_activities")
    suspend fun deleteAllActivities()

    @Query("""
        UPDATE daily_timetable_activities
        SET isEnabled = :enabled,
            updatedAt = :updatedAt
        WHERE id = :activityId
    """)
    suspend fun setActivityEnabled(
        activityId: Int,
        enabled: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )


    // DAILY ACTIVITY COMPLETION

    @Query("""
        SELECT * FROM daily_timetable_completions
        WHERE studyDate = :date
        ORDER BY id ASC
    """)
    fun observeCompletionsForDate(
        date: String
    ): Flow<List<DailyTimetableCompletionEntity>>

    @Query("""
        SELECT * FROM daily_timetable_completions
        WHERE studyDate = :date
        ORDER BY id ASC
    """)
    suspend fun getCompletionsForDate(
        date: String
    ): List<DailyTimetableCompletionEntity>

    // BACKUP: Get all completion records across all dates.
    @Query("""
        SELECT * FROM daily_timetable_completions
        ORDER BY studyDate ASC, id ASC
    """)
    suspend fun getAllCompletions(): List<DailyTimetableCompletionEntity>

    // BACKUP: Delete all completion records.
    @Query("DELETE FROM daily_timetable_completions")
    suspend fun deleteAllCompletions()

    @Query("""
        SELECT * FROM daily_timetable_completions
        WHERE activityId = :activityId
          AND studyDate = :date
        LIMIT 1
    """)
    suspend fun getCompletion(
        activityId: Int,
        date: String
    ): DailyTimetableCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCompletion(
        completion: DailyTimetableCompletionEntity
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCompletions(
        completions: List<DailyTimetableCompletionEntity>
    )

    @Query("""
        UPDATE daily_timetable_completions
        SET status = :status,
            completedAt = :completedAt,
            updatedAt = :updatedAt
        WHERE activityId = :activityId
          AND studyDate = :date
    """)
    suspend fun updateCompletionStatus(
        activityId: Int,
        date: String,
        status: String,
        completedAt: Long?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        DELETE FROM daily_timetable_completions
        WHERE studyDate = :date
    """)
    suspend fun deleteCompletionsForDate(date: String)

    @Query("""
        SELECT COUNT(*) FROM daily_timetable_completions
        WHERE studyDate = :date
          AND status = 'DONE'
    """)
    suspend fun getDoneActivityCount(date: String): Int

    @Query("""
        SELECT COUNT(*) FROM daily_timetable_completions
        WHERE studyDate = :date
          AND status = 'NOT_DONE'
    """)
    suspend fun getNotDoneActivityCount(date: String): Int


    // ACTUAL STUDY SESSIONS

    @Query("""
        SELECT * FROM daily_study_sessions
        WHERE studyDate = :date
        ORDER BY startedAt ASC
    """)
    fun observeSessionsForDate(
        date: String
    ): Flow<List<DailyStudySessionEntity>>

    @Query("""
        SELECT * FROM daily_study_sessions
        WHERE studyDate = :date
        ORDER BY startedAt ASC
    """)
    suspend fun getSessionsForDate(
        date: String
    ): List<DailyStudySessionEntity>

    // BACKUP: Get all study sessions across all dates.
    @Query("""
        SELECT * FROM daily_study_sessions
        ORDER BY startedAt ASC, id ASC
    """)
    suspend fun getAllSessions(): List<DailyStudySessionEntity>

    @Query("""
        SELECT * FROM daily_study_sessions
        WHERE activityId = :activityId
        ORDER BY startedAt ASC
    """)
    fun observeSessionsForActivity(
        activityId: Int
    ): Flow<List<DailyStudySessionEntity>>

    @Query("""
        SELECT * FROM daily_study_sessions
        WHERE id = :sessionId
        LIMIT 1
    """)
    suspend fun getSessionById(
        sessionId: Int
    ): DailyStudySessionEntity?

    @Query("""
        SELECT * FROM daily_study_sessions
        WHERE status = 'RUNNING'
        ORDER BY startedAt DESC
        LIMIT 1
    """)
    suspend fun getRunningSession(): DailyStudySessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(
        session: DailyStudySessionEntity
    ): Long

    // BACKUP: Insert multiple sessions while preserving IDs.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(
        sessions: List<DailyStudySessionEntity>
    )

    // BACKUP: Delete all study sessions.
    @Query("DELETE FROM daily_study_sessions")
    suspend fun deleteAllSessions()

    @Update
    suspend fun updateSession(
        session: DailyStudySessionEntity
    )

    @Delete
    suspend fun deleteSession(
        session: DailyStudySessionEntity
    )

    @Query("""
        DELETE FROM daily_study_sessions
        WHERE studyDate = :date
    """)
    suspend fun deleteSessionsForDate(date: String)

    @Query("""
        SELECT COALESCE(SUM(durationSeconds), 0)
        FROM daily_study_sessions
        WHERE studyDate = :date
          AND activityId IN (
              SELECT id FROM daily_timetable_activities
              WHERE countsAsStudy = 1
          )
    """)
    suspend fun getActualStudySecondsForDate(
        date: String
    ): Long

    @Query("""
        SELECT * FROM daily_study_sessions
        WHERE studyDate = :date
          AND status = 'COMPLETED'
        ORDER BY startedAt ASC
    """)
    suspend fun getCompletedSessionsForDate(
        date: String
    ): List<DailyStudySessionEntity>
}
