
package com.cosmotrinity.cosmonavigator.ui.more

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cosmotrinity.cosmonavigator.data.CosmoDatabase
import com.cosmotrinity.cosmonavigator.data.DataStoreManager
import com.cosmotrinity.cosmonavigator.data.DailyTimetableRepository
import com.cosmotrinity.cosmonavigator.data.StoredAspirantGoals
import com.cosmotrinity.cosmonavigator.data.StoredAspirantProfile
import com.cosmotrinity.cosmonavigator.data.backup.CosmoBackupManager
import com.cosmotrinity.cosmonavigator.ui.theme.*
import com.cosmotrinity.cosmonavigator.ui.timetable.DailyTimetableEditorScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class BackupDialog {
    NONE,
    CREATE,
    RESTORE_PASSWORD,
    CONFIRM_RESTORE
}

private const val BACKUP_PASSWORD_LENGTH = 8

private fun isValidBackupPassword(password: String): Boolean {
    return password.length == BACKUP_PASSWORD_LENGTH &&
            password.all { it.isLetterOrDigit() } &&
            password.any { it.isLetter() } &&
            password.any { it.isDigit() }
}

@Composable
fun MoreScreen(
    dataStore: DataStoreManager,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showPassword by remember {
        mutableStateOf(true)
    }

    var showConfirmPassword by remember {
        mutableStateOf(true)
    }

    var showTimetableEditor by remember {
        mutableStateOf(false)
    }

    val profile by dataStore.profileFlow.collectAsState(
        initial = StoredAspirantProfile()
    )

    val goals by dataStore.goalsFlow.collectAsState(
        initial = StoredAspirantGoals()
    )

    var profileBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var dialogMode by remember {
        mutableStateOf(BackupDialog.NONE)
    }

    var password by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var selectedBackupUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var isProcessing by remember {
        mutableStateOf(false)
    }

    val database = remember {
        CosmoDatabase.getDatabase(context)
    }

    val timetableRepository = remember {
        DailyTimetableRepository(
            database.dailyTimetableDao()
        )
    }

    val backupManager = remember {
        CosmoBackupManager(
            context = context,
            database = database,
            dataStore = dataStore
        )
    }

    fun showMessage(message: String) {
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    fun resetBackupDialog() {
        dialogMode = BackupDialog.NONE
        password = ""
        confirmPassword = ""
        selectedBackupUri = null
        showPassword = true
        showConfirmPassword = true
    }

    LaunchedEffect(profile.profileImageUri) {
        profileBitmap = if (profile.profileImageUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver
                        .openInputStream(
                            Uri.parse(profile.profileImageUri)
                        )
                        ?.use {
                            BitmapFactory.decodeStream(it)
                        }
                } catch (_: Exception) {
                    null
                }
            }
        } else {
            null
        }
    }

    // CREATE BACKUP FILE PICKER

    val createBackupLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument(
                "application/octet-stream"
            )
        ) { uri ->

            if (uri != null) {
                val backupPassword = password.toCharArray()

                scope.launch {
                    isProcessing = true

                    try {
                        backupManager.exportBackup(
                            destination = uri,
                            password = backupPassword
                        )

                        showMessage(
                            "Encrypted backup created successfully."
                        )

                    } catch (error: Exception) {
                        showMessage(
                            error.message ?: "Backup creation failed."
                        )

                    } finally {
                        backupPassword.fill('\u0000')
                        isProcessing = false
                        resetBackupDialog()
                    }
                }
            } else {
                resetBackupDialog()
            }
        }

    // RESTORE BACKUP FILE PICKER

    val restoreBackupLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {
                selectedBackupUri = uri
                dialogMode = BackupDialog.CONFIRM_RESTORE
            } else {
                resetBackupDialog()
            }
        }

    // MAIN SCREEN OR TIMETABLE EDITOR

    if (showTimetableEditor) {

        DailyTimetableEditorScreen(
            repository = timetableRepository,
            onBack = {
                showTimetableEditor = false
            }
        )

    } else {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBackground)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 20.dp,
                    vertical = 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {

            // HEADER

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "More",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CosmicTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Manage your CosmoNavigator experience",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CosmicTextSecondary
                )
            }

            // PROFILE CARD

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onProfileClick),
                shape = RoundedCornerShape(20.dp),
                color = CosmicSurface
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                CosmicPrimary.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileBitmap != null) {
                            Image(
                                bitmap = profileBitmap!!.asImageBitmap(),
                                contentDescription = "Aspirant profile photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default profile photo",
                                tint = CosmicPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = profile.fullName.ifBlank {
                                "Complete Your Profile"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = CosmicTextPrimary
                        )

                        Text(
                            text = "UPSC CSE ${goals.targetExamYear}",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicPrimary
                        )

                        Text(
                            text = profile.preparationStage,
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicTextSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Edit profile",
                        tint = CosmicTextSecondary
                    )
                }
            }

            // DAILY ROUTINE

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "DAILY ROUTINE",
                    style = MaterialTheme.typography.labelMedium,
                    color = CosmicTextSecondary
                )

                MoreMenuItem(
                    title = "Daily Timetable",
                    subtitle = "Manage your complete 24-hour routine",
                    icon = Icons.Default.Schedule,
                    onClick = {
                        showTimetableEditor = true
                    }
                )
            }

            // DATA AND BACKUP

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "DATA & BACKUP",
                    style = MaterialTheme.typography.labelMedium,
                    color = CosmicTextSecondary
                )

                MoreMenuItem(
                    title = "Backup & Restore",
                    subtitle = "Create or restore an encrypted backup",
                    icon = Icons.Default.Backup,
                    onClick = {
                        password = ""
                        confirmPassword = ""
                        showPassword = true
                        showConfirmPassword = true
                        dialogMode = BackupDialog.CREATE
                    }
                )
            }

            // APP SECTION

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "APP",
                    style = MaterialTheme.typography.labelMedium,
                    color = CosmicTextSecondary
                )

                MoreMenuItem(
                    title = "Settings",
                    subtitle = "App preferences and customization",
                    icon = Icons.Default.Settings,
                    onClick = {}
                )

                MoreMenuItem(
                    title = "About CosmoNavigator",
                    subtitle = "Application information and version",
                    icon = Icons.Default.Info,
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "COSMONAVIGATOR 2.0",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = CosmicTextSecondary
            )
        }
    }

    // BACKUP AND RESTORE DIALOGS

    when (dialogMode) {

        // CREATE BACKUP

        BackupDialog.CREATE -> {

            AlertDialog(
                onDismissRequest = {
                    if (!isProcessing) resetBackupDialog()
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = CosmicSurface,
                titleContentColor = CosmicTextPrimary,
                textContentColor = CosmicTextSecondary,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = CosmicPrimary,
                            modifier = Modifier.size(28.dp)
                        )

                        Text(
                            text = "Create Encrypted Backup",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        Text(
                            text = "Create an 8-character alphanumeric password. " +
                                    "You will need this password to restore your backup.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                    .filter { char -> char.isLetterOrDigit() }
                                    .take(BACKUP_PASSWORD_LENGTH)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Backup password")
                            },
                            placeholder = {
                                Text("Example: Abhay123")
                            },
                            visualTransformation = if (showPassword) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        showPassword = !showPassword
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (showPassword) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (showPassword) {
                                            "Hide password"
                                        } else {
                                            "Show password"
                                        }
                                    )
                                }
                            },
                            singleLine = true,
                            enabled = !isProcessing
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                    .filter { char -> char.isLetterOrDigit() }
                                    .take(BACKUP_PASSWORD_LENGTH)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Confirm password")
                            },
                            visualTransformation = if (showConfirmPassword) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        showConfirmPassword =
                                            !showConfirmPassword
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (showConfirmPassword) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (showConfirmPassword) {
                                            "Hide password"
                                        } else {
                                            "Show password"
                                        }
                                    )
                                }
                            },
                            singleLine = true,
                            enabled = !isProcessing
                        )

                        Text(
                            text = "Use letters and numbers only. " +
                                    "Your profile, photo, UPSC subjects, lectures, " +
                                    "Calendar and supported timetable data will be included.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicTextSecondary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !isProcessing &&
                                isValidBackupPassword(password) &&
                                isValidBackupPassword(confirmPassword) &&
                                password == confirmPassword,
                        onClick = {
                            if (!isValidBackupPassword(password)) {
                                showMessage(
                                    "Password must contain exactly 8 letters and numbers."
                                )
                            } else if (password != confirmPassword) {
                                showMessage("Passwords do not match.")
                            } else {
                                createBackupLauncher.launch(
                                    "CosmoNavigator_backup.cosmobackup"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Backup")
                    }
                },
                dismissButton = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            enabled = !isProcessing,
                            onClick = {
                                password = ""
                                confirmPassword = ""
                                showPassword = true
                                showConfirmPassword = true
                                dialogMode = BackupDialog.RESTORE_PASSWORD
                            }
                        ) {
                            Text("Restore Existing")
                        }

                        TextButton(
                            enabled = !isProcessing,
                            onClick = {
                                resetBackupDialog()
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }

        // RESTORE PASSWORD

        BackupDialog.RESTORE_PASSWORD -> {

            AlertDialog(
                onDismissRequest = {
                    if (!isProcessing) resetBackupDialog()
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = CosmicSurface,
                titleContentColor = CosmicTextPrimary,
                textContentColor = CosmicTextSecondary,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = CosmicPrimary,
                            modifier = Modifier.size(28.dp)
                        )

                        Text(
                            text = "Restore Backup",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Enter the 8-character password used " +
                                    "when creating this backup."
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                    .filter { char -> char.isLetterOrDigit() }
                                    .take(BACKUP_PASSWORD_LENGTH)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Backup password")
                            },
                            placeholder = {
                                Text("Enter 8 characters")
                            },
                            singleLine = true,
                            enabled = !isProcessing,
                            visualTransformation = if (showPassword) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        showPassword = !showPassword
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (showPassword) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (showPassword) {
                                            "Hide password"
                                        } else {
                                            "Show password"
                                        }
                                    )
                                }
                            }
                        )

                        Text(
                            text = "Password must contain letters and numbers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicTextSecondary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = isValidBackupPassword(password) &&
                                !isProcessing,
                        onClick = {
                            restoreBackupLauncher.launch(
                                arrayOf("*/*")
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Backup File")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            resetBackupDialog()
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // CONFIRM RESTORE

        BackupDialog.CONFIRM_RESTORE -> {

            AlertDialog(
                onDismissRequest = {
                    if (!isProcessing) resetBackupDialog()
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = CosmicSurface,
                titleContentColor = CosmicTextPrimary,
                textContentColor = CosmicTextSecondary,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = CosmicPrimary,
                            modifier = Modifier.size(28.dp)
                        )

                        Text(
                            text = "Replace Existing Data?",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                text = {
                    Text(
                        text = "Restoring this backup will replace your current " +
                                "UPSC subjects, lectures, profile and goals. " +
                                "Make sure you have a backup of your current data."
                    )
                },
                confirmButton = {
                    Button(
                        enabled = !isProcessing,
                        onClick = {
                            val uri = selectedBackupUri

                            if (uri != null) {
                                val restorePassword =
                                    password.toCharArray()

                                scope.launch {
                                    isProcessing = true

                                    try {
                                        backupManager.restoreBackup(
                                            source = uri,
                                            password = restorePassword
                                        )

                                        showMessage(
                                            "Backup restored successfully. " +
                                                    "Restart the app to refresh all screens."
                                        )

                                    } catch (error: Exception) {
                                        showMessage(
                                            error.message
                                                ?: "Backup restoration failed."
                                        )

                                    } finally {
                                        restorePassword.fill('\u0000')
                                        isProcessing = false
                                        resetBackupDialog()
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Restore Backup")
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isProcessing,
                        onClick = {
                            resetBackupDialog()
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        BackupDialog.NONE -> Unit
    }

    // PROCESSING DIALOG

    if (isProcessing) {
        AlertDialog(
            onDismissRequest = {},
            shape = RoundedCornerShape(24.dp),
            containerColor = CosmicSurface,
            title = {
                Text("Please wait")
            },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = CosmicPrimary
                    )

                    Text("Processing your backup...")
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun MoreMenuItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = CosmicSurface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(14.dp),
                color = CosmicPrimary.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(11.dp),
                    tint = CosmicPrimary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    color = CosmicTextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = subtitle,
                    color = CosmicTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = CosmicTextSecondary
            )
        }
    }
}
