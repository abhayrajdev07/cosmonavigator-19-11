
package com.cosmotrinity.cosmonavigator.ui.more

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmotrinity.cosmonavigator.data.DataStoreManager
import com.cosmotrinity.cosmonavigator.data.StoredAspirantGoals
import com.cosmotrinity.cosmonavigator.data.StoredAspirantProfile
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicBackground
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicPrimary
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicSurface
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicTextPrimary
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun ProfileScreen(
    dataStore: DataStoreManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val savedProfile by dataStore.profileFlow.collectAsState(
        initial = StoredAspirantProfile()
    )

    val savedGoals by dataStore.goalsFlow.collectAsState(
        initial = StoredAspirantGoals()
    )

    // Personal details
    var fullName by remember(savedProfile) {
        mutableStateOf(savedProfile.fullName)
    }

    var language by remember(savedProfile) {
        mutableStateOf(savedProfile.language)
    }

    var preparationStage by remember(savedProfile) {
        mutableStateOf(savedProfile.preparationStage)
    }

    var attemptNumber by remember(savedProfile) {
        mutableStateOf(savedProfile.attemptNumber)
    }

    var profileImageUri by remember(savedProfile) {
        mutableStateOf(savedProfile.profileImageUri)
    }

    var profileBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    // UPSC goals
    var targetExamYear by remember(savedGoals) {
        mutableStateOf(savedGoals.targetExamYear.toString())
    }

    var prelimsDate by remember(savedGoals) {
        mutableStateOf(savedGoals.prelimsDate)
    }

    var optionalSubject by remember(savedGoals) {
        mutableStateOf(
            savedGoals.optionalSubject.ifBlank {
                "Sociology"
            }
        )
    }

    var studyMedium by remember(savedGoals) {
        mutableStateOf(savedGoals.studyMedium)
    }

    var dailyStudyHours by remember(savedGoals) {
        mutableStateOf(savedGoals.dailyStudyHours.toString())
    }

    var preparationStartDate by remember(savedGoals) {
        mutableStateOf(savedGoals.preparationStartDate)
    }

    var saving by remember {
        mutableStateOf(false)
    }

    var saveMessage by remember {
        mutableStateOf("")
    }

    // Dropdown options
    val preparationStages = listOf(
        "Beginner",
        "Foundation Building",
        "Syllabus Completion",
        "Revision",
        "Test Series / Exam Practice"
    )

    val languageOptions = listOf(
        "English",
        "Hindi",
        "Both"
    )

    val studyMediumOptions = listOf(
        "English",
        "Hindi",
        "Both"
    )

    val optionalSubjects = listOf(
        "Not decided yet",
        "Agriculture",
        "Animal Husbandry and Veterinary Science",
        "Anthropology",
        "Botany",
        "Chemistry",
        "Civil Engineering",
        "Commerce and Accountancy",
        "Economics",
        "Electrical Engineering",
        "Geography",
        "Geology",
        "History",
        "Law",
        "Management",
        "Mathematics",
        "Mechanical Engineering",
        "Medical Science",
        "Philosophy",
        "Physics",
        "Political Science and International Relations",
        "Psychology",
        "Public Administration",
        "Sociology",
        "Statistics",
        "Zoology",
        "Literature"
    )

    // Load saved profile photo
    LaunchedEffect(profileImageUri) {
        profileBitmap = if (profileImageUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver
                        .openInputStream(Uri.parse(profileImageUri))
                        ?.use {
                            BitmapFactory.decodeStream(it)
                        }
                } catch (exception: Exception) {
                    null
                }
            }
        } else {
            null
        }
    }

    // Gallery picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->

        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                profileImageUri = uri.toString()
                saveMessage = ""

            } catch (exception: Exception) {
                saveMessage = "Unable to access selected photo."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go back",
                    tint = CosmicPrimary
                )
            }

            TextButton(
                onClick = {
                    fullName = savedProfile.fullName
                    language = savedProfile.language
                    preparationStage = savedProfile.preparationStage
                    attemptNumber = savedProfile.attemptNumber
                    profileImageUri = savedProfile.profileImageUri

                    targetExamYear =
                        savedGoals.targetExamYear.toString()

                    prelimsDate = savedGoals.prelimsDate
                    optionalSubject = savedGoals.optionalSubject
                        .ifBlank { "Sociology" }

                    studyMedium = savedGoals.studyMedium

                    dailyStudyHours =
                        savedGoals.dailyStudyHours.toString()

                    preparationStartDate =
                        savedGoals.preparationStartDate

                    saveMessage = ""
                }
            ) {
                Text(
                    text = "Reset",
                    color = CosmicTextSecondary
                )
            }
        }

        Text(
            text = "My Profile",
            style = MaterialTheme.typography.headlineMedium,
            color = CosmicTextPrimary
        )

        Text(
            text = "Manage your personal details and UPSC goals",
            style = MaterialTheme.typography.bodyMedium,
            color = CosmicTextSecondary
        )

        // PROFILE PHOTO
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.BottomEnd
            ) {

                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(
                            CosmicPrimary.copy(alpha = 0.15f)
                        )
                        .border(
                            2.dp,
                            CosmicPrimary,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    if (profileBitmap != null) {

                        Image(
                            bitmap = profileBitmap!!.asImageBitmap(),
                            contentDescription = "Profile picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )

                    } else {

                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default profile picture",
                            tint = CosmicPrimary,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(38.dp)
                        .clickable {
                            imagePicker.launch(arrayOf("image/*"))
                        },
                    shape = CircleShape,
                    color = CosmicPrimary,
                    border = androidx.compose.foundation.BorderStroke(
                        3.dp,
                        CosmicBackground
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change profile picture",
                            tint = CosmicTextPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Change Profile Photo",
                color = CosmicPrimary,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    imagePicker.launch(arrayOf("image/*"))
                }
            )
        }

        // PERSONAL DETAILS
        ProfileSectionTitle("PERSONAL DETAILS")

        ProfileInput(
            label = "Full Name",
            value = fullName,
            onValueChange = { fullName = it }
        )

        ProfileDropdown(
            label = "Preferred Language",
            value = language,
            options = languageOptions,
            onValueChange = { language = it }
        )

        ProfileDropdown(
            label = "Preparation Stage",
            value = preparationStage,
            options = preparationStages,
            onValueChange = { preparationStage = it }
        )

        ProfileInput(
            label = "UPSC Attempt Number (Optional)",
            value = attemptNumber,
            onValueChange = { attemptNumber = it },
            numeric = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // UPSC GOALS
        ProfileSectionTitle("UPSC PREPARATION GOALS")

        ProfileInput(
            label = "Target Exam Year",
            value = targetExamYear,
            onValueChange = { targetExamYear = it },
            numeric = true
        )

        ProfileDatePicker(
            label = "UPSC Prelims Date",
            value = prelimsDate,
            onValueChange = { prelimsDate = it }
        )

        ProfileDropdown(
            label = "Optional Subject",
            value = optionalSubject,
            options = optionalSubjects,
            onValueChange = { optionalSubject = it }
        )

        ProfileDropdown(
            label = "Study Medium",
            value = studyMedium,
            options = studyMediumOptions,
            onValueChange = { studyMedium = it }
        )

        ProfileInput(
            label = "Daily Study Target (Hours)",
            value = dailyStudyHours,
            onValueChange = { dailyStudyHours = it },
            numeric = true
        )

        ProfileDatePicker(
            label = "Preparation Start Date",
            value = preparationStartDate,
            onValueChange = { preparationStartDate = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // SAVE BUTTON
        Button(
            onClick = {

                val year = targetExamYear.toIntOrNull()
                val hours = dailyStudyHours.toIntOrNull()
                val attempt = attemptNumber.trim()
                val attemptValue = attempt.toIntOrNull()

                if (
                    fullName.isBlank() ||
                    year == null ||
                    year !in 2027..2035 ||
                    hours == null ||
                    hours !in 1..24 ||
                    (
                            attempt.isNotEmpty() &&
                                    (attemptValue == null || attemptValue < 1)
                            ) ||
                    prelimsDate.toUtcMillis() == null ||
                    preparationStartDate.toUtcMillis() == null
                ) {

                    saveMessage = "Please check your entered details."

                } else {

                    scope.launch {

                        saving = true

                        try {

                            dataStore.saveProfile(
                                StoredAspirantProfile(
                                    fullName = fullName.trim(),
                                    language = language,
                                    preparationStage = preparationStage,
                                    attemptNumber = attempt,
                                    profileImageUri = profileImageUri
                                )
                            )

                            dataStore.saveGoals(
                                StoredAspirantGoals(
                                    targetExamYear = year,
                                    prelimsDate = prelimsDate,
                                    optionalSubject = optionalSubject,
                                    studyMedium = studyMedium,
                                    dailyStudyHours = hours,
                                    preparationStartDate =
                                        preparationStartDate
                                )
                            )

                            saveMessage = "Profile saved successfully."

                        } catch (exception: Exception) {

                            saveMessage =
                                "Unable to save profile. Please try again."

                        } finally {
                            saving = false
                        }
                    }
                }
            },
            enabled = !saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CosmicPrimary
            )
        ) {
            Text(
                text = if (saving) "Saving..." else "Save Changes",
                color = CosmicTextPrimary
            )
        }

        if (saveMessage.isNotEmpty()) {
            Text(
                text = saveMessage,
                color = if (
                    saveMessage.startsWith("Profile saved")
                ) {
                    CosmicPrimary
                } else {
                    MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// SECTION TITLE
@Composable
private fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = CosmicPrimary,
        modifier = Modifier.padding(
            top = 8.dp,
            bottom = 4.dp
        )
    )
}

// TEXT INPUT
@Composable
private fun ProfileInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    numeric: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) {
                KeyboardType.Number
            } else {
                KeyboardType.Text
            }
        )
    )
}

// DROPDOWN SELECTOR
@Composable
private fun ProfileDropdown(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(modifier = Modifier.fillMaxWidth()) {

        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Select $label",
                    tint = CosmicPrimary
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = CosmicTextPrimary,
                disabledBorderColor = CosmicTextSecondary,
                disabledLabelColor = CosmicTextSecondary,
                disabledTrailingIconColor = CosmicPrimary
            )
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    expanded = true
                }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 320.dp)
                .background(CosmicSurface)
        ) {

            options.forEach { option ->

                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = if (option == value) {
                                CosmicPrimary
                            } else {
                                CosmicTextPrimary
                            }
                        )
                    },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

// DATE PICKER
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileDatePicker(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var showDatePicker by remember {
        mutableStateOf(false)
    }

    Box(modifier = Modifier.fillMaxWidth()) {

        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = {
                Text(label)
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Choose date",
                    tint = CosmicPrimary
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = CosmicTextPrimary,
                disabledBorderColor = CosmicTextSecondary,
                disabledLabelColor = CosmicTextSecondary,
                disabledTrailingIconColor = CosmicPrimary
            )
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    showDatePicker = true
                }
        )
    }

    if (showDatePicker) {

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = value.toUtcMillis()
        )

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onValueChange(millis.toDisplayDate())
                        }

                        showDatePicker = false
                    }
                ) {
                    Text(
                        "Confirm",
                        color = CosmicPrimary
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) {
                    Text(
                        "Cancel",
                        color = CosmicTextSecondary
                    )
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = true
            )
        }
    }
}

// Convert DD-MM-YYYY to UTC milliseconds
private fun String.toUtcMillis(): Long? {
    return try {
        val formatter = SimpleDateFormat(
            "dd-MM-yyyy",
            Locale.ENGLISH
        ).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }

        formatter.parse(this)?.time
    } catch (exception: Exception) {
        null
    }
}

// Convert UTC milliseconds to DD-MM-YYYY
private fun Long.toDisplayDate(): String {
    val formatter = SimpleDateFormat(
        "dd-MM-yyyy",
        Locale.ENGLISH
    ).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    return formatter.format(Date(this))
}
