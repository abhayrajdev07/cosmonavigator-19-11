
package com.cosmotrinity.cosmonavigator.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmotrinity.cosmonavigator.data.UpscSubjectEntity
import com.cosmotrinity.cosmonavigator.data.UpscSubjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UpscSubjectViewModel(
    private val repository: UpscSubjectRepository,
    private val targetExamYear: Int = 2027
) : ViewModel() {

    val subjects: StateFlow<List<UpscSubjectEntity>> =
        repository.getSubjectsByYear(targetExamYear)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    init {
        initializeSubjects()
    }

    private fun initializeSubjects() {
        viewModelScope.launch {
            UpscSubjectCatalog.categories.forEach { category ->
                category.subjects.forEach { subject ->
                    repository.getOrCreateSubject(
                        year = targetExamYear,
                        category = category.title,
                        subjectName = subject
                    )
                }
            }
        }
    }

    fun updateSubject(subject: UpscSubjectEntity) {
        viewModelScope.launch {
            repository.updateSubject(subject)
        }
    }

    fun deleteSubject(subjectId: Int) {
        viewModelScope.launch {
            repository.deleteSubject(subjectId)
        }
    }

    fun getCategoryProgress(category: String): Float {
        val categorySubjects = subjects.value.filter {
            it.category == category
        }

        if (categorySubjects.isEmpty()) return 0f

        val totalLectures = categorySubjects.sumOf {
            it.totalLectures
        }

        val completedLectures = categorySubjects.sumOf {
            it.completedLectures
        }

        return if (totalLectures > 0) {
            completedLectures.toFloat() / totalLectures
        } else {
            0f
        }
    }
}
