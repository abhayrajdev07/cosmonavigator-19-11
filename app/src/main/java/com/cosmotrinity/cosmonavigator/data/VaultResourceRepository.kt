
package com.cosmotrinity.cosmonavigator.data

import kotlinx.coroutines.flow.Flow

class VaultResourceRepository(
    private val dao: VaultResourceDao
) {

    fun getResources(
        year: Int,
        subjectId: Int,
        folderName: String
    ): Flow<List<VaultResourceEntity>> {
        return dao.getResources(
            year,
            subjectId,
            folderName
        )
    }

    fun getResourcesBySubject(
        year: Int,
        subjectId: Int
    ): Flow<List<VaultResourceEntity>> {
        return dao.getResourcesBySubject(
            year,
            subjectId
        )
    }

    fun searchResources(
        year: Int,
        query: String
    ): Flow<List<VaultResourceEntity>> {
        return dao.searchResources(year, query)
    }

    fun getFavorites(
        year: Int
    ): Flow<List<VaultResourceEntity>> {
        return dao.getFavorites(year)
    }

    fun getRecentResources(
        year: Int
    ): Flow<List<VaultResourceEntity>> {
        return dao.getRecentResources(year)
    }

    suspend fun insertResource(
        resource: VaultResourceEntity
    ): Long {
        return dao.insertResource(resource)
    }

    suspend fun updateResource(
        resource: VaultResourceEntity
    ) {
        dao.updateResource(
            resource.copy(updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun updateFavorite(
        resourceId: Int,
        favorite: Boolean
    ) {
        dao.updateFavorite(resourceId, favorite)
    }

    suspend fun deleteResource(
        resource: VaultResourceEntity
    ) {
        dao.deleteResource(resource)
    }
}
