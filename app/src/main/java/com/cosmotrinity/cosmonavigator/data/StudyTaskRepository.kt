
package com.cosmotrinity.cosmonavigator.data

import kotlinx.coroutines.flow.Flow

class StudyTaskRepository(
    private val taskDao: StudyTaskDao
) {

    // Observe all study tasks
    fun getAllTasks(): Flow<List<StudyTaskEntity>> {
        return taskDao.getAllTasks()
    }

    // Observe tasks for a particular date
    fun getTasksByDate(date: String): Flow<List<StudyTaskEntity>> {
        return taskDao.getTasksByDate(date)
    }

    // Get a task using its ID
    suspend fun getTaskById(taskId: Int): StudyTaskEntity? {
        return taskDao.getTaskById(taskId)
    }

    // Add a new task
    suspend fun addTask(task: StudyTaskEntity): Long {
        return taskDao.insertTask(task)
    }

    // Update an existing task
    suspend fun updateTask(task: StudyTaskEntity) {
        taskDao.updateTask(task)
    }

    // Mark a task completed or incomplete
    suspend fun setTaskCompletion(
        taskId: Int,
        completed: Boolean
    ) {
        taskDao.updateTaskCompletion(
            taskId = taskId,
            completed = completed,
            completedAt = if (completed) {
                System.currentTimeMillis()
            } else {
                null
            }
        )
    }

    // Delete a task
    suspend fun deleteTask(taskId: Int) {
        taskDao.deleteTaskById(taskId)
    }

    // Delete all tasks
    suspend fun deleteAllTasks() {
        taskDao.deleteAllTasks()
    }

    // Daily completed task count
    fun getCompletedTaskCount(date: String): Flow<Int> {
        return taskDao.getCompletedTaskCount(date)
    }

    // Daily total task count
    fun getTotalTaskCount(date: String): Flow<Int> {
        return taskDao.getTotalTaskCount(date)
    }

    // Daily planned study minutes
    fun getPlannedMinutes(date: String): Flow<Int> {
        return taskDao.getPlannedMinutes(date)
    }
}
