
package com.cosmotrinity.cosmonavigator.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultResourceDao {

    // Get all resources inside a subject folder
    @Query(
        """
        SELECT * FROM vault_resources
        WHERE targetExamYear = :year
        AND subjectId = :subjectId
        AND folderName = :folderName
        ORDER BY createdAt DESC
        """
    )
    fun getResources(
        year: Int,
        subjectId: Int,
        folderName: String
    ): Flow<List<VaultResourceEntity>>

    // Get all resources belonging to a subject
    @Query(
        """
        SELECT * FROM vault_resources
        WHERE targetExamYear = :year
        AND subjectId = :subjectId
        ORDER BY createdAt DESC
        """
    )
    fun getResourcesBySubject(
        year: Int,
        subjectId: Int
    ): Flow<List<VaultResourceEntity>>

    // Search all resources in an exam year
    @Query(
        """
        SELECT * FROM vault_resources
        WHERE targetExamYear = :year
        AND (
            title LIKE '%' || :query || '%' COLLATE NOCASE
            OR originalFileName LIKE '%' || :query || '%' COLLATE NOCASE
            OR subjectName LIKE '%' || :query || '%' COLLATE NOCASE
            OR category LIKE '%' || :query || '%' COLLATE NOCASE
            OR folderName LIKE '%' || :query || '%' COLLATE NOCASE
        )
        ORDER BY createdAt DESC
        """
    )
    fun searchResources(
        year: Int,
        query: String
    ): Flow<List<VaultResourceEntity>>

    // Get favorite resources
    @Query(
        """
        SELECT * FROM vault_resources
        WHERE targetExamYear = :year
        AND isFavorite = 1
        ORDER BY updatedAt DESC
        """
    )
    fun getFavorites(
        year: Int
    ): Flow<List<VaultResourceEntity>>

    // Get recently added resources
    @Query(
        """
        SELECT * FROM vault_resources
        WHERE targetExamYear = :year
        ORDER BY createdAt DESC
        LIMIT 20
        """
    )
    fun getRecentResources(
        year: Int
    ): Flow<List<VaultResourceEntity>>

    // Insert resource
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertResource(
        resource: VaultResourceEntity
    ): Long

    // Update resource
    @Update
    suspend fun updateResource(
        resource: VaultResourceEntity
    )

    // Toggle favorite
    @Query(
        """
        UPDATE vault_resources
        SET isFavorite = :favorite,
            updatedAt = :updatedAt
        WHERE id = :resourceId
        """
    )
    suspend fun updateFavorite(
        resourceId: Int,
        favorite: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    // Delete resource metadata
    @Delete
    suspend fun deleteResource(
        resource: VaultResourceEntity
    )

    // Delete by ID
    @Query(
        "DELETE FROM vault_resources WHERE id = :resourceId"
    )
    suspend fun deleteResourceById(
        resourceId: Int
    )
}
