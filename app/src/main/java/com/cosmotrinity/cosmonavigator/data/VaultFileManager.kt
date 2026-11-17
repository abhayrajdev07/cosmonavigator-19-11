
package com.cosmotrinity.cosmonavigator.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

data class ImportedVaultFile(
    val title: String,
    val originalFileName: String,
    val storedFileName: String,
    val mimeType: String,
    val relativePath: String,
    val fileSizeBytes: Long
)

class VaultFileManager(
    private val context: Context
) {

    suspend fun importFile(
        uri: Uri,
        targetExamYear: Int,
        category: String,
        subjectId: Int,
        folderName: String
    ): ImportedVaultFile = withContext(Dispatchers.IO) {

        val resolver = context.contentResolver

        // Get original filename
        val originalName = resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )

                if (index >= 0) cursor.getString(index) else null
            } else {
                null
            }
        } ?: "Imported_${System.currentTimeMillis()}"

        // Get MIME type
        val mimeType = resolver.getType(uri)
            ?: "application/octet-stream"

        // Sanitize directory names
        fun safeName(value: String): String {
            return value.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        }

        val safeCategory = safeName(category)
        val safeFolder = safeName(folderName)

        val relativeDirectory =
            "cosmovault/$targetExamYear/$safeCategory/$subjectId/$safeFolder"

        val destinationDirectory = File(
            context.filesDir,
            relativeDirectory
        )

        if (!destinationDirectory.exists()) {
            if (!destinationDirectory.mkdirs()) {
                throw IOException("Unable to create Vault directory")
            }
        }

        // Generate unique stored filename
        val extension = originalName
            .substringAfterLast('.', "")
            .takeIf { it.isNotBlank() }
            ?.let { ".$it" }
            ?: ""

        val storedFileName = "${UUID.randomUUID()}$extension"

        val destinationFile = File(
            destinationDirectory,
            storedFileName
        )

        try {
            val inputStream = resolver.openInputStream(uri)
                ?: throw IOException("Unable to open selected file")

            inputStream.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!destinationFile.exists() || destinationFile.length() == 0L) {
                throw IOException("Imported file is empty or unavailable")
            }

            val relativePath =
                "$relativeDirectory/$storedFileName"

            val title = originalName.substringBeforeLast(
                '.',
                originalName
            )

            ImportedVaultFile(
                title = title,
                originalFileName = originalName,
                storedFileName = storedFileName,
                mimeType = mimeType,
                relativePath = relativePath,
                fileSizeBytes = destinationFile.length()
            )

        } catch (exception: Exception) {
            destinationFile.delete()
            throw exception
        }
    }

    fun getFile(relativePath: String): File {
        val root = context.filesDir.canonicalFile
        val file = File(root, relativePath).canonicalFile

        require(
            file.path.startsWith(root.path + File.separator)
        ) {
            "Invalid Vault file path"
        }

        return file
    }

    suspend fun deleteFile(relativePath: String) {
        withContext(Dispatchers.IO) {
            getFile(relativePath).delete()
        }
    }
}
