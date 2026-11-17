
package com.cosmotrinity.cosmonavigator.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vault_resources",
    indices = [
        Index(
            value = [
                "targetExamYear",
                "category",
                "subjectId",
                "folderName"
            ],
            name = "index_vault_resources_location"
        ),
        Index(
            value = ["targetExamYear", "title"],
            name = "index_vault_resources_title"
        )
    ]
)
data class VaultResourceEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val targetExamYear: Int,

    // Link to existing UPSC subject
    val subjectId: Int,

    val category: String,

    val subjectName: String,

    // Class Notes, PYQs, Short Notes, etc.
    val folderName: String,

    val title: String,

    val originalFileName: String = "",

    // Name of the file stored inside app storage
    val storedFileName: String = "",

    val mimeType: String = "",

    // Relative path inside the app's private files directory
    val relativePath: String = "",

    // FILE or NOTE
    val resourceType: String = "FILE",

    val fileSizeBytes: Long = 0L,

    val isFavorite: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)
