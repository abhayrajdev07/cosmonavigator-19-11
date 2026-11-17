
package com.cosmotrinity.cosmonavigator.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cosmotrinity.cosmonavigator.data.StudyTaskRepository

class StudyTaskViewModelFactory(
    private val repository: StudyTaskRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(StudyTaskViewModel::class.java)) {
            return StudyTaskViewModel(repository) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
