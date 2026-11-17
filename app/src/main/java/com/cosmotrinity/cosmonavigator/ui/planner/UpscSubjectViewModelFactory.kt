
package com.cosmotrinity.cosmonavigator.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cosmotrinity.cosmonavigator.data.UpscSubjectRepository

class UpscSubjectViewModelFactory(
    private val repository: UpscSubjectRepository,
    private val targetExamYear: Int
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(UpscSubjectViewModel::class.java)) {
            return UpscSubjectViewModel(
                repository = repository,
                targetExamYear = targetExamYear
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
