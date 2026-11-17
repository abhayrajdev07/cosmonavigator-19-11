
package com.cosmotrinity.cosmonavigator.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UpscLectureDao {

    @Query("""
        SELECT * FROM upsc_lectures
        WHERE subjectId = :subjectId
        ORDER BY lectureNumber ASC
    """)
    fun getLecturesBySubject(
        subjectId: Int
    ): Flow<List<UpscLectureEntity>>

    @Query("""
        SELECT * FROM upsc_lectures
        WHERE id = :lectureId
        LIMIT 1
    """)
    fun getLectureById(
        lectureId: Int
    ): Flow<UpscLectureEntity?>

    @Query("""
        SELECT COUNT(*) FROM upsc_lectures
        WHERE subjectId = :subjectId
    """)
    suspend fun getTotalLectureCount(
        subjectId: Int
    ): Int

    @Query("""
        SELECT COUNT(*) FROM upsc_lectures
        WHERE subjectId = :subjectId
        AND isCompleted = 1
    """)
    suspend fun getCompletedLectureCount(
        subjectId: Int
    ): Int

    @Query("""
        SELECT COUNT(*) FROM upsc_lectures
        WHERE subjectId = :subjectId
        AND isCompleted = 0
    """)
    suspend fun getPendingLectureCount(
        subjectId: Int
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLecture(
        lecture: UpscLectureEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLectures(
        lectures: List<UpscLectureEntity>
    ): List<Long>

    @Update
    suspend fun updateLecture(
        lecture: UpscLectureEntity
    )

    @Delete
    suspend fun deleteLecture(
        lecture: UpscLectureEntity
    )

    @Query("""
        DELETE FROM upsc_lectures
        WHERE id = :lectureId
    """)
    suspend fun deleteLectureById(
        lectureId: Int
    )

    @Query("""
        DELETE FROM upsc_lectures
        WHERE subjectId = :subjectId
    """)
    suspend fun deleteLecturesBySubject(
        subjectId: Int
    )

    // BACKUP: Export all lectures across every subject
    @Query("""
        SELECT * FROM upsc_lectures
        ORDER BY subjectId ASC, lectureNumber ASC
    """)
    suspend fun getAllLecturesForBackup(): List<UpscLectureEntity>

    // RESTORE: Insert lectures with their original database IDs
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLecturesForRestore(
        lectures: List<UpscLectureEntity>
    )

    // RESTORE: Clear lecture records before restoring
    @Query("DELETE FROM upsc_lectures")
    suspend fun deleteAllLecturesForRestore()
}
