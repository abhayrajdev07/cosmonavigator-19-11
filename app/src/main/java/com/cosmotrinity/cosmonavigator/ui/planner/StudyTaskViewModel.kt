
package com.cosmotrinity.cosmonavigator.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmotrinity.cosmonavigator.data.StudyTaskEntity
import com.cosmotrinity.cosmonavigator.data.StudyTaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudyTaskViewModel(
    private val repository: StudyTaskRepository
) : ViewModel() {

    // All saved tasks
    val tasks: StateFlow<List<StudyTaskEntity>> =
        repository.getAllTasks().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Add a new task
    fun addTask(task: StudyTaskEntity) {
        viewModelScope.launch {
            repository.addTask(task)
        }
    }

    // Update an existing task
    fun updateTask(task: StudyTaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    // Complete or uncomplete a task
    fun toggleTaskCompletion(task: StudyTaskEntity) {
        viewModelScope.launch {
            repository.setTaskCompletion(
                taskId = task.id,
                completed = !task.isCompleted
            )
        }
    }

    // Delete a task
    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    // Daily statistics
    fun getDailyCompletedCount(date: String) =
        repository.getCompletedTaskCount(date)

    fun getDailyTotalCount(date: String) =
        repository.getTotalTaskCount(date)

    fun getDailyPlannedMinutes(date: String) =
        repository.getPlannedMinutes(date)
}
