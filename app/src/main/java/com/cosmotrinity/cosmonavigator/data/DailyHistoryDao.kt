
package com.cosmotrinity.cosmonavigator.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyHistoryDao {

    // DAILY HISTORY

    @Query("""
        SELECT * FROM daily_history
        WHERE studyDate = :date
        LIMIT 1
    """)
    suspend fun getHistoryForDate(date: String): DailyHistoryEntity?

    @Query("""
        SELECT * FROM daily_history
        WHERE studyDate = :date
        LIMIT 1
    """)
    fun observeHistoryForDate(
        date: String
    ): Flow<DailyHistoryEntity?>

    @Query("""
        SELECT * FROM daily_history
        ORDER BY createdAt DESC
    """)
    fun observeAllHistory(): Flow<List<DailyHistoryEntity>>

    @Query("""
        SELECT * FROM daily_history
        ORDER BY createdAt DESC
    """)
    suspend fun getAllHistory(): List<DailyHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(
        history: DailyHistoryEntity
    ): Long

    @Query("""
        DELETE FROM daily_history
        WHERE studyDate = :date
    """)
    suspend fun deleteHistoryForDate(date: String)

    @Query("DELETE FROM daily_history")
    suspend fun deleteAllHistory()


    // DAILY HISTORY ACTIVITIES

    @Query("""
        SELECT * FROM daily_history_activities
        WHERE historyId = :historyId
        ORDER BY startTimeMinutes ASC
    """)
    suspend fun getActivitiesForHistory(
        historyId: Int
    ): List<DailyHistoryActivityEntity>

    @Query("""
        SELECT * FROM daily_history_activities
        WHERE historyId = :historyId
        ORDER BY startTimeMinutes ASC
    """)
    fun observeActivitiesForHistory(
        historyId: Int
    ): Flow<List<DailyHistoryActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryActivities(
        activities: List<DailyHistoryActivityEntity>
    )

    @Query("""
        DELETE FROM daily_history_activities
        WHERE historyId = :historyId
    """)
    suspend fun deleteActivitiesForHistory(
        historyId: Int
    )

    @Query("""
        SELECT COUNT(*) FROM daily_history
    """)
    suspend fun getHistoryDayCount(): Int

    @Query("""
        SELECT COALESCE(SUM(actualStudySeconds), 0)
        FROM daily_history
    """)
    suspend fun getTotalRecordedStudySeconds(): Long


    // ATOMIC DAILY HISTORY SAVE

    @Transaction
    suspend fun saveHistoryWithActivities(
        history: DailyHistoryEntity,
        activities: List<DailyHistoryActivityEntity>
    ): Int {

        deleteHistoryForDate(history.studyDate)

        val historyId = insertHistory(history).toInt()

        insertHistoryActivities(
            activities.map { activity ->
                activity.copy(historyId = historyId)
            }
        )

        return historyId
    }
}
