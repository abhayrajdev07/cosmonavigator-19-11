
package com.cosmotrinity.cosmonavigator.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmotrinity.cosmonavigator.data.UpscLectureEntity
import com.cosmotrinity.cosmonavigator.data.UpscLectureRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UpscLectureViewModel(
    private val repository: UpscLectureRepository,
    private val subjectId: Int
) : ViewModel() {

    val lectures: StateFlow<List<UpscLectureEntity>> =
        repository.getLecturesBySubject(subjectId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun generateLectures(totalLectures: Int) {
        viewModelScope.launch {
            repository.generateLectures(
                subjectId = subjectId,
                totalLectures = totalLectures
            )
        }
    }

    fun updateLecture(lecture: UpscLectureEntity) {
        viewModelScope.launch {
            repository.updateLecture(lecture)
        }
    }

    fun toggleLectureCompletion(
        lecture: UpscLectureEntity,
        completed: Boolean
    ) {
        viewModelScope.launch {
            repository.markLectureCompleted(
                lecture = lecture,
                completed = completed
            )
        }
    }

    fun deleteLecture(lectureId: Int) {
        viewModelScope.launch {
            repository.deleteLecture(lectureId)
        }
    }

    fun deleteAllLectures() {
        viewModelScope.launch {
            repository.deleteAllLectures(subjectId)
        }
    }
}
