
package com.cosmotrinity.cosmonavigator.data

import kotlinx.coroutines.flow.Flow

class UpscLectureRepository(
    private val lectureDao: UpscLectureDao
) {

    fun getLecturesBySubject(
        subjectId: Int
    ): Flow<List<UpscLectureEntity>> {
        return lectureDao.getLecturesBySubject(subjectId)
    }

    fun getLectureById(
        lectureId: Int
    ): Flow<UpscLectureEntity?> {
        return lectureDao.getLectureById(lectureId)
    }

    suspend fun generateLectures(
        subjectId: Int,
        totalLectures: Int
    ) {
        require(totalLectures >= 0) {
            "Total lectures cannot be negative."
        }

        val existingCount =
            lectureDao.getTotalLectureCount(subjectId)

        if (totalLectures <= existingCount) return

        val newLectures = (existingCount + 1..totalLectures).map { number ->
            UpscLectureEntity(
                subjectId = subjectId,
                lectureNumber = number,
                title = "Lecture $number"
            )
        }

        lectureDao.insertLectures(newLectures)
    }

    suspend fun updateLecture(
        lecture: UpscLectureEntity
    ) {
        lectureDao.updateLecture(
            lecture.copy(
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markLectureCompleted(
        lecture: UpscLectureEntity,
        completed: Boolean
    ) {
        updateLecture(
            lecture.copy(
                isCompleted = completed,
                completedAt = if (completed) {
                    System.currentTimeMillis()
                } else {
                    null
                }
            )
        )
    }

    suspend fun deleteLecture(
        lectureId: Int
    ) {
        lectureDao.deleteLectureById(lectureId)
    }

    suspend fun deleteAllLectures(
        subjectId: Int
    ) {
        lectureDao.deleteLecturesBySubject(subjectId)
    }

    suspend fun getTotalLectureCount(
        subjectId: Int
    ): Int {
        return lectureDao.getTotalLectureCount(subjectId)
    }

    suspend fun getCompletedLectureCount(
        subjectId: Int
    ): Int {
        return lectureDao.getCompletedLectureCount(subjectId)
    }

    suspend fun getPendingLectureCount(
        subjectId: Int
    ): Int {
        return lectureDao.getPendingLectureCount(subjectId)
    }
}
