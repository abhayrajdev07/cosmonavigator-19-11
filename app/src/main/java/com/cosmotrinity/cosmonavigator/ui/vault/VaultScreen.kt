package com.cosmotrinity.cosmonavigator.ui.vault

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.cosmotrinity.cosmonavigator.data.DataStoreManager
import com.cosmotrinity.cosmonavigator.data.UpscSubjectEntity
import com.cosmotrinity.cosmonavigator.data.UpscSubjectRepository
import com.cosmotrinity.cosmonavigator.data.VaultResourceEntity
import com.cosmotrinity.cosmonavigator.data.VaultResourceRepository
import com.cosmotrinity.cosmonavigator.data.VaultFileManager

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private val VaultBlue = Color(0xFF45A9E8)

private val vaultFolders = listOf(
    "Class Notes",
    "Short Notes",
    "PYQs",
    "Test Papers",
    "Self-Made Notes",
    "Reference Material"
)

private data class VaultSearchResult(
    val title: String,
    val location: String,
    val category: String? = null,
    val subject: UpscSubjectEntity? = null,
    val folder: String? = null,
    val resource: VaultResourceEntity? = null
)

@Composable
fun VaultScreen(
    dataStore: DataStoreManager,
    subjectRepository: UpscSubjectRepository,
    resourceRepository: VaultResourceRepository,
    fileManager: VaultFileManager
) {
    val goals by dataStore.goalsFlow.collectAsState(initial = null)
    val targetExamYear = goals?.targetExamYear

    if (targetExamYear == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val subjects by subjectRepository
        .getSubjectsByYear(targetExamYear)
        .collectAsState(initial = emptyList())

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedSubject by remember { mutableStateOf<UpscSubjectEntity?>(null) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }
    var resourcePendingDelete by remember { mutableStateOf<VaultResourceEntity?>(null) }

    val categories = remember(subjects) {
        val priority = listOf(
            "GS Paper I",
            "GS Paper II",
            "GS Paper III",
            "GS Paper IV"
        )
        val available = subjects.map { it.category }.distinct()

        priority.filter { it in available } +
                available.filterNot { it in priority }.sorted()
    }

    val categorySubjects = remember(subjects, selectedCategory) {
        subjects.filter { it.category == selectedCategory }
    }

    val resourcesFlow = remember(
        targetExamYear,
        selectedSubject?.id,
        selectedFolder
    ) {
        if (selectedSubject != null && selectedFolder != null) {
            resourceRepository.getResources(
                year = targetExamYear,
                subjectId = selectedSubject!!.id,
                folderName = selectedFolder!!
            )
        } else {
            flowOf(emptyList())
        }
    }

    val folderResources by resourcesFlow.collectAsState(initial = emptyList())

    // Global database search for actual saved resources.
    val resourceSearchResults by resourceRepository
        .searchResources(targetExamYear, searchQuery.trim())
        .collectAsState(initial = emptyList())

    val isSearching = searchQuery.isNotBlank()

    val searchResults = remember(
        subjects,
        categories,
        searchQuery,
        resourceSearchResults
    ) {
        val query = searchQuery.trim()

        if (query.isBlank()) {
            emptyList()
        } else {
            buildList {
                categories.forEach { category ->
                    if (category.contains(query, ignoreCase = true)) {
                        add(
                            VaultSearchResult(
                                title = category,
                                location = "UPSC $targetExamYear",
                                category = category
                            )
                        )
                    }
                }

                subjects.forEach { subject ->
                    if (subject.subjectName.contains(query, ignoreCase = true)) {
                        add(
                            VaultSearchResult(
                                title = subject.subjectName,
                                location = subject.category,
                                category = subject.category,
                                subject = subject
                            )
                        )
                    }
                }

                resourceSearchResults.forEach { resource ->
                    add(
                        VaultSearchResult(
                            title = resource.title,
                            location = "${resource.category} / ${resource.subjectName} / ${resource.folderName}",
                            category = resource.category,
                            subject = subjects.find { it.id == resource.subjectId },
                            folder = resource.folderName,
                            resource = resource
                        )
                    )
                }
            }.distinctBy {
                "${it.category}_${it.subject?.id}_${it.folder}_${it.resource?.id}_${it.title}"
            }
        }
    }

    // Android document picker.
    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->

        if (uri != null) {
            val subject = selectedSubject
            val folder = selectedFolder

            if (subject != null && folder != null) {
                scope.launch {
                    importing = true

                    try {
                        val imported = fileManager.importFile(
                            uri = uri,
                            targetExamYear = targetExamYear,
                            category = subject.category,
                            subjectId = subject.id,
                            folderName = folder
                        )

                        val resource = VaultResourceEntity(
                            targetExamYear = targetExamYear,
                            subjectId = subject.id,
                            category = subject.category,
                            subjectName = subject.subjectName,
                            folderName = folder,
                            title = imported.title,
                            originalFileName = imported.originalFileName,
                            storedFileName = imported.storedFileName,
                            mimeType = imported.mimeType,
                            relativePath = imported.relativePath,
                            resourceType = "FILE",
                            fileSizeBytes = imported.fileSizeBytes
                        )

                        resourceRepository.insertResource(resource)

                        snackbarHostState.showSnackbar(
                            "Imported ${imported.originalFileName}"
                        )

                    } catch (exception: Exception) {
                        snackbarHostState.showSnackbar(
                            "Import failed: ${exception.localizedMessage ?: "Unknown error"}"
                        )
                    } finally {
                        importing = false
                    }
                }
            }
        }
    }

    fun openVaultResource(resource: VaultResourceEntity) {
        try {
            val file = fileManager.getFile(resource.relativePath)
            if (!file.exists()) {
                scope.launch {
                    snackbarHostState.showSnackbar("File not found in Vault storage")
                }
                return
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    contentUri,
                    resource.mimeType.ifBlank { "application/octet-stream" }
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(openIntent)
        } catch (exception: ActivityNotFoundException) {
            scope.launch {
                snackbarHostState.showSnackbar("No app found to open this file type")
            }
        } catch (exception: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Unable to open file: ${exception.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun deleteVaultResource(resource: VaultResourceEntity) {
        scope.launch {
            try {
                // Remove the physical file first, then its Room metadata.
                fileManager.deleteFile(resource.relativePath)
                resourceRepository.deleteResource(resource)
                snackbarHostState.showSnackbar("${resource.originalFileName} deleted")
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    "Delete failed: ${exception.localizedMessage ?: "Unknown error"}"
                )
            } finally {
                resourcePendingDelete = null
            }
        }
    }

    val isInsideVault = selectedCategory != null ||
            selectedSubject != null ||
            selectedFolder != null

    BackHandler(enabled = isInsideVault) {
        when {
            selectedFolder != null -> selectedFolder = null
            selectedSubject != null -> selectedSubject = null
            selectedCategory != null -> selectedCategory = null
        }
        searchQuery = ""
    }

    val pageTitle = when {
        selectedFolder != null -> selectedFolder!!
        selectedSubject != null -> selectedSubject!!.subjectName
        selectedCategory != null -> selectedCategory!!
        else -> "CosmoVault"
    }

    resourcePendingDelete?.let { resource ->
        AlertDialog(
            onDismissRequest = { resourcePendingDelete = null },
            title = { Text("Delete resource?") },
            text = {
                Text("${resource.originalFileName} will be permanently deleted from this Vault.")
            },
            confirmButton = {
                TextButton(
                    onClick = { deleteVaultResource(resource) }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { resourcePendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {

            // HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isInsideVault) {
                    IconButton(
                        onClick = {
                            when {
                                selectedFolder != null -> selectedFolder = null
                                selectedSubject != null -> selectedSubject = null
                                selectedCategory != null -> selectedCategory = null
                            }
                            searchQuery = ""
                        }
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pageTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "UPSC $targetExamYear",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // BREADCRUMB
            if (selectedCategory != null) {
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vault",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clickable {
                            selectedCategory = null
                            selectedSubject = null
                            selectedFolder = null
                            searchQuery = ""
                        }
                    )

                    Text(
                        text = " / $selectedCategory",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )

                    if (selectedSubject != null) {
                        Text(
                            text = " / ${selectedSubject!!.subjectName}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // GLOBAL SEARCH
            if (selectedFolder == null) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    placeholder = {
                        Text("Search folders, subjects and files...")
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            when {

                // GLOBAL SEARCH RESULTS
                isSearching -> {
                    Text(
                        text = "${searchResults.size} results found",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (searchResults.isEmpty()) {
                        EmptyVaultMessage("No results found for \"$searchQuery\"")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(
                                items = searchResults,
                                key = {
                                    "${it.category}_${it.subject?.id}_${it.folder}_${it.resource?.id}_${it.title}"
                                }
                            ) { result ->
                                SearchResultTile(result) {
                                    selectedCategory = result.category
                                    selectedSubject = result.subject
                                    selectedFolder = result.folder
                                    searchQuery = ""
                                }
                            }
                        }
                    }
                }

                // RESOURCE CONTENT
                selectedFolder != null -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${folderResources.size} items",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                documentPicker.launch(arrayOf("*/*"))
                            },
                            enabled = !importing
                        ) {
                            Icon(
                                Icons.Filled.UploadFile,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Import")
                        }
                    }

                    if (importing) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (folderResources.isEmpty()) {
                        EmptyVaultMessage(
                            "This folder is empty. Tap Import to add your first resource."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(
                                items = folderResources,
                                key = { it.id }
                            ) { resource ->
                                ResourceFileTile(
                                    resource = resource,
                                    onOpen = { openVaultResource(resource) },
                                    onDelete = { resourcePendingDelete = resource }
                                )
                            }
                        }
                    }
                }

                // SUBJECT RESOURCE FOLDERS
                selectedSubject != null -> {
                    FolderGrid(vaultFolders) {
                        selectedFolder = it
                    }
                }

                // SUBJECTS INSIDE CATEGORY
                selectedCategory != null -> {
                    if (categorySubjects.isEmpty()) {
                        EmptyVaultMessage("No subjects found")
                    } else {
                        SubjectGrid(categorySubjects) {
                            selectedSubject = it
                        }
                    }
                }

                // ROOT CATEGORY FOLDERS
                else -> {
                    if (categories.isEmpty()) {
                        EmptyVaultMessage(
                            "No subjects found for UPSC $targetExamYear. Add subjects in Planner first."
                        )
                    } else {
                        FolderGrid(categories) {
                            selectedCategory = it
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultTile(
    result: VaultSearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = VaultBlue,
                modifier = Modifier.size(32.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = result.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ResourceFileTile(
    resource: VaultResourceEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.FolderOpen,
                contentDescription = null,
                tint = VaultBlue,
                modifier = Modifier.size(30.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = resource.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = resource.originalFileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formatFileSize(resource.fileSizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onOpen) {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = "View file",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete file",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(
            java.util.Locale.getDefault(),
            "%.2f MB",
            bytes / (1024.0 * 1024.0)
        )
    }
}

@Composable
private fun FolderGrid(
    folders: List<String>,
    onFolderClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        gridItems(folders) { folder ->
            FolderTile(folder) {
                onFolderClick(folder)
            }
        }
    }
}

@Composable
private fun SubjectGrid(
    subjects: List<UpscSubjectEntity>,
    onSubjectClick: (UpscSubjectEntity) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        gridItems(
            items = subjects,
            key = { it.id }
        ) { subject ->
            FolderTile(subject.subjectName) {
                onSubjectClick(subject)
            }
        }
    }
}

@Composable
private fun FolderTile(
    name: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = VaultBlue,
            modifier = Modifier.size(42.dp)
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun EmptyVaultMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
