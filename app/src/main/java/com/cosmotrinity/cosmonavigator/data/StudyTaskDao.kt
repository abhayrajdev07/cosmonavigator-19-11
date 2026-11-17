
package com.cosmotrinity.cosmonavigator.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyTaskDao {

    // Insert a new task
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: StudyTaskEntity): Long

    // Insert multiple tasks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<StudyTaskEntity>)

    // Retrieve all tasks
    @Query("SELECT * FROM study_tasks ORDER BY scheduledDate ASC, createdAt DESC")
    fun getAllTasks(): Flow<List<StudyTaskEntity>>

    // Retrieve tasks for a particular date
    @Query("SELECT * FROM study_tasks WHERE scheduledDate = :date ORDER BY createdAt DESC")
    fun getTasksByDate(date: String): Flow<List<StudyTaskEntity>>

    // Retrieve one task by ID
    @Query("SELECT * FROM study_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Int): StudyTaskEntity?

    // Update task details
    @Update
    suspend fun updateTask(task: StudyTaskEntity)

    // Mark task completed or incomplete
    @Query("""
        UPDATE study_tasks
        SET isCompleted = :completed,
            completedAt = :completedAt
        WHERE id = :taskId
    """)
    suspend fun updateTaskCompletion(
        taskId: Int,
        completed: Boolean,
        completedAt: Long?
    )

    // Delete a task
    @Delete
    suspend fun deleteTask(task: StudyTaskEntity)

    // Delete task by ID
    @Query("DELETE FROM study_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Int)

    // Delete all tasks
    @Query("DELETE FROM study_tasks")
    suspend fun deleteAllTasks()

    // Count completed tasks for a date
    @Query("""
        SELECT COUNT(*) FROM study_tasks
        WHERE scheduledDate = :date AND isCompleted = 1
    """)
    fun getCompletedTaskCount(date: String): Flow<Int>

    // Count total tasks for a date
    @Query("""
        SELECT COUNT(*) FROM study_tasks
        WHERE scheduledDate = :date
    """)
    fun getTotalTaskCount(date: String): Flow<Int>

    // Calculate planned study minutes for a date
    @Query("""
        SELECT COALESCE(SUM(durationMinutes), 0)
        FROM study_tasks
        WHERE scheduledDate = :date
    """)
    fun getPlannedMinutes(date: String): Flow<Int>
}
