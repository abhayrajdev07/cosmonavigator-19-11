
package com.cosmotrinity.cosmonavigator.data

import kotlinx.coroutines.flow.Flow

class UpscSubjectRepository(
    private val dao: UpscSubjectDao
) {

    fun getSubjectsByYear(year: Int): Flow<List<UpscSubjectEntity>> {
        return dao.getSubjectsByYear(year)
    }

    fun getSubjectsByCategory(
        year: Int,
        category: String
    ): Flow<List<UpscSubjectEntity>> {
        return dao.getSubjectsByCategory(year, category)
    }

    fun getSubjectById(id: Int): Flow<UpscSubjectEntity?> {
        return dao.getSubjectById(id)
    }

    suspend fun getOrCreateSubject(
        year: Int,
        category: String,
        subjectName: String
    ): UpscSubjectEntity {
        val existing = dao.findSubject(year, category, subjectName)

        if (existing != null) {
            return existing
        }

        val subject = UpscSubjectEntity(
            targetExamYear = year,
            category = category,
            subjectName = subjectName
        )

        val newId = dao.insertSubject(subject)

        return subject.copy(id = newId.toInt())
    }

    /**
     * Registers all syllabus subjects for a target UPSC year.
     *
     * Existing records are preserved.
     * Missing subjects are created automatically.
     */
    suspend fun initializeSubjectsForYear(
        year: Int,
        subjects: List<Pair<String, String>>
    ) {
        subjects.distinct().forEach { (category, subjectName) ->
            getOrCreateSubject(
                year = year,
                category = category,
                subjectName = subjectName
            )
        }
    }

    suspend fun updateSubject(subject: UpscSubjectEntity) {
        dao.updateSubject(
            subject.copy(updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteSubject(id: Int) {
        dao.deleteSubjectById(id)
    }

    suspend fun deleteSubjectsByYear(year: Int) {
        dao.deleteSubjectsByYear(year)
    }
}
