
package com.cosmotrinity.cosmonavigator.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UpscSubjectDao {

    // Get all subjects for a particular UPSC exam year
    @Query(
        """
        SELECT * FROM upsc_subject_progress
        WHERE targetExamYear = :year
        ORDER BY category ASC, subjectName ASC
        """
    )
    fun getSubjectsByYear(year: Int): Flow<List<UpscSubjectEntity>>

    // Get subjects belonging to a category
    @Query(
        """
        SELECT * FROM upsc_subject_progress
        WHERE targetExamYear = :year
        AND category = :category
        ORDER BY subjectName ASC
        """
    )
    fun getSubjectsByCategory(
        year: Int,
        category: String
    ): Flow<List<UpscSubjectEntity>>

    // Get one subject by its database ID
    @Query(
        """
        SELECT * FROM upsc_subject_progress
        WHERE id = :subjectId
        LIMIT 1
        """
    )
    fun getSubjectById(subjectId: Int): Flow<UpscSubjectEntity?>

    // Find an existing subject record
    @Query(
        """
        SELECT * FROM upsc_subject_progress
        WHERE targetExamYear = :year
        AND category = :category
        AND subjectName = :subjectName
        LIMIT 1
        """
    )
    suspend fun findSubject(
        year: Int,
        category: String,
        subjectName: String
    ): UpscSubjectEntity?

    // Insert a new subject
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubject(subject: UpscSubjectEntity): Long

    // Update existing subject progress
    @Update
    suspend fun updateSubject(subject: UpscSubjectEntity)

    // Delete a subject
    @Delete
    suspend fun deleteSubject(subject: UpscSubjectEntity)

    // Delete a subject using its ID
    @Query(
        """
        DELETE FROM upsc_subject_progress
        WHERE id = :subjectId
        """
    )
    suspend fun deleteSubjectById(subjectId: Int)

    // Delete all subjects for a particular exam year
    @Query(
        """
        DELETE FROM upsc_subject_progress
        WHERE targetExamYear = :year
        """
    )
    suspend fun deleteSubjectsByYear(year: Int)

    // BACKUP: Read all subjects across all UPSC target years
    @Query("SELECT * FROM upsc_subject_progress ORDER BY id ASC")
    suspend fun getAllSubjectsForBackup(): List<UpscSubjectEntity>

    // RESTORE: Insert subjects with their original database IDs
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubjectsForRestore(
        subjects: List<UpscSubjectEntity>
    )

    // RESTORE: Clear subjects before a complete database restore
    @Query("DELETE FROM upsc_subject_progress")
    suspend fun deleteAllSubjectsForRestore()
}
