
package com.cosmotrinity.cosmonavigator.ui.planner

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.cosmotrinity.cosmonavigator.data.UpscLectureRepository
import com.cosmotrinity.cosmonavigator.data.UpscSubjectEntity
import com.cosmotrinity.cosmonavigator.data.UpscSubjectRepository
import com.cosmotrinity.cosmonavigator.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class SubjectStatusOption(
    val title: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val statusOptions = listOf(
    SubjectStatusOption(
        "Not Started",
        Color(0xFF75B8FF),
        Icons.Default.RadioButtonUnchecked
    ),
    SubjectStatusOption(
        "In Progress",
        Color(0xFFFFC857),
        Icons.Default.PendingActions
    ),
    SubjectStatusOption(
        "Completed",
        Color(0xFF55D6A0),
        Icons.Default.CheckCircle
    ),
    SubjectStatusOption(
        "On Hold",
        Color(0xFFE9A2D0),
        Icons.Default.PauseCircle
    )
)

private fun parseSubjectDate(value: String): Calendar? {
    if (value.isBlank()) return null

    return try {
        val format = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        format.isLenient = false
        val date = format.parse(value) ?: return null

        Calendar.getInstance().apply {
            time = date
        }
    } catch (_: Exception) {
        null
    }
}

private fun todaySubjectDate(): String {
    return SimpleDateFormat(
        "dd-MM-yyyy",
        Locale.getDefault()
    ).format(Calendar.getInstance().time)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subjectId: Int,
    subjectName: String,
    category: String,
    subjectRepository: UpscSubjectRepository,
    lectureRepository: UpscLectureRepository,
    onBack: () -> Unit,
    onManageLectures: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val subject by subjectRepository
        .getSubjectById(subjectId)
        .collectAsState(initial = null)

    val lectures by lectureRepository
        .getLecturesBySubject(subjectId)
        .collectAsState(initial = emptyList())

    var status by remember(subjectId) {
        mutableStateOf("Not Started")
    }

    var startDate by remember(subjectId) {
        mutableStateOf("")
    }

    var targetDate by remember(subjectId) {
        mutableStateOf("")
    }

    var notes by remember(subjectId) {
        mutableStateOf("")
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var saveMessage by remember {
        mutableStateOf<String?>(null)
    }

    var initialized by remember(subjectId) {
        mutableStateOf(false)
    }

    // Load saved subject data from Room
    LaunchedEffect(subject) {
        val savedSubject = subject ?: return@LaunchedEffect

        if (!initialized) {
            status = savedSubject.status
            startDate = savedSubject.startDate
            targetDate = savedSubject.targetEndDate
            notes = savedSubject.notes
            initialized = true
        }
    }

    val currentStatus = statusOptions.firstOrNull {
        it.title == status
    } ?: statusOptions.first()

    val totalLectures = lectures.size
    val completedLectures = lectures.count { it.isCompleted }
    val pendingLectures = totalLectures - completedLectures

    val progress = if (totalLectures > 0) {
        completedLectures.toFloat() / totalLectures
    } else {
        0f
    }

    fun showDatePicker(
        currentDate: String,
        isStartDate: Boolean,
        onDateSelected: (String) -> Unit
    ) {
        val calendar = parseSubjectDate(currentDate)
            ?: Calendar.getInstance()

        DatePickerDialog(
            context,
            { _, year, month, day ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, day)
                }

                val selectedText = SimpleDateFormat(
                    "dd-MM-yyyy",
                    Locale.getDefault()
                ).format(selected.time)

                onDateSelected(selectedText)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            if (isStartDate) {
                val target = parseSubjectDate(targetDate)
                if (target != null) {
                    datePicker.maxDate = target.timeInMillis
                }
            } else {
                val start = parseSubjectDate(startDate)
                if (start != null) {
                    datePicker.minDate = start.timeInMillis
                }
            }
            show()
        }
    }

    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            subjectName,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            category,
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CosmicBackground,
                    titleContentColor = CosmicTextPrimary,
                    navigationIconContentColor = CosmicTextPrimary
                )
            )
        }
    ) { padding ->

        if (subject == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CosmicPrimary)
            }
        } else {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // COMPACT SUBJECT STATUS

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CosmicSurface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = CosmicSecondary.copy(alpha = 0.13f),
                                shape = RoundedCornerShape(9.dp)
                            ) {
                                Icon(
                                    Icons.Default.TrackChanges,
                                    contentDescription = null,
                                    tint = CosmicSecondary,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .padding(8.dp)
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Text(
                                "SUBJECT STATUS",
                                color = CosmicTextPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }

                        Box {
                            Surface(
                                onClick = { expanded = true },
                                color = currentStatus.color.copy(alpha = 0.13f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    currentStatus.color.copy(alpha = 0.55f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 9.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        currentStatus.icon,
                                        contentDescription = null,
                                        tint = currentStatus.color,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Text(
                                        status,
                                        color = currentStatus.color,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )

                                    Icon(
                                        Icons.Default.ExpandMore,
                                        contentDescription = "Change subject status",
                                        tint = currentStatus.color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                statusOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                option.title,
                                                color = option.color,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                option.icon,
                                                contentDescription = null,
                                                tint = option.color
                                            )
                                        },
                                        onClick = {
                                            status = option.title
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // STUDY TIMELINE

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = CosmicSurface
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {

                        SectionHeading(
                            Icons.Default.CalendarMonth,
                            "STUDY TIMELINE",
                            Color(0xFF75B8FF)
                        )

                        Spacer(Modifier.height(14.dp))

                        DateSettingRow(
                            label = "Start date",
                            date = startDate.ifBlank {
                                "Select start date"
                            },
                            icon = Icons.Default.PlayCircle,
                            color = Color(0xFF55D6A0)
                        ) {
                            showDatePicker(startDate, true) {
                                startDate = it

                                val target = parseSubjectDate(targetDate)
                                val selected = parseSubjectDate(it)

                                if (
                                    target != null &&
                                    selected != null &&
                                    target.before(selected)
                                ) {
                                    targetDate = ""
                                }
                            }
                        }

                        HorizontalDivider(color = CosmicSurfaceVariant)

                        DateSettingRow(
                            label = "Target completion",
                            date = targetDate.ifBlank {
                                "Select target date"
                            },
                            icon = Icons.Default.Flag,
                            color = Color(0xFFFFC857)
                        ) {
                            showDatePicker(targetDate, false) {
                                targetDate = it
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            "Date format: DD-MM-YYYY",
                            color = CosmicTextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // LECTURE OVERVIEW

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = CosmicSurface
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {

                        SectionHeading(
                            Icons.Default.MenuBook,
                            "LECTURE OVERVIEW",
                            Color(0xFFB69CFF)
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LectureStat(
                                "Total",
                                totalLectures,
                                Icons.Default.LibraryBooks,
                                Color(0xFF75B8FF),
                                Modifier.weight(1f)
                            )

                            LectureStat(
                                "Completed",
                                completedLectures,
                                Icons.Default.CheckCircle,
                                Color(0xFF55D6A0),
                                Modifier.weight(1f)
                            )

                            LectureStat(
                                "Pending",
                                pendingLectures,
                                Icons.Default.Schedule,
                                Color(0xFFFFC857),
                                Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Overall progress",
                                color = CosmicTextSecondary
                            )

                            Text(
                                "${(progress * 100).toInt()}%",
                                color = CosmicPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp),
                            color = Color(0xFF55D6A0),
                            trackColor = CosmicSurfaceVariant
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = onManageLectures,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CosmicPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null
                            )

                            Spacer(Modifier.width(8.dp))

                            Text("Manage Lectures")

                            Spacer(Modifier.weight(1f))

                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null
                            )
                        }
                    }
                }

                // SUBJECT NOTES

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = CosmicSurface
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {

                        SectionHeading(
                            Icons.Default.StickyNote2,
                            "SUBJECT NOTES",
                            Color(0xFFFFC857)
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = notes,
                            onValueChange = {
                                notes = it
                                saveMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Add your study notes or targets...")
                            },
                            minLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CosmicTextPrimary,
                                unfocusedTextColor = CosmicTextPrimary,
                                focusedBorderColor = CosmicPrimary,
                                unfocusedBorderColor = CosmicSurfaceVariant
                            )
                        )
                    }
                }

                // SAVE CHANGES

                Button(
                    onClick = {
                        val currentSubject = subject ?: return@Button

                        scope.launch {
                            isSaving = true
                            saveMessage = null

                            try {
                                val updatedSubject = currentSubject.copy(
                                    status = status,
                                    startDate = startDate,
                                    targetEndDate = targetDate,
                                    actualEndDate = if (status == "Completed") {
                                        currentSubject.actualEndDate
                                            ?: todaySubjectDate()
                                    } else {
                                        null
                                    },
                                    totalLectures = totalLectures,
                                    completedLectures = completedLectures,
                                    notes = notes
                                )

                                subjectRepository.updateSubject(updatedSubject)

                                saveMessage = "Changes saved successfully."
                            } catch (exception: Exception) {
                                saveMessage = "Unable to save changes. Please try again."
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = CosmicTextPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Saving...")
                    } else {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Save Changes")
                    }
                }

                saveMessage?.let { message ->
                    Text(
                        message,
                        color = if (message.startsWith("Changes saved")) {
                            Color(0xFF55D6A0)
                        } else {
                            CosmicError
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SectionHeading(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = color.copy(alpha = 0.13f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.padding(9.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Text(
            title,
            color = CosmicTextPrimary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
private fun DateSettingRow(
    label: String,
    date: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    label,
                    color = CosmicTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    date,
                    color = CosmicTextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        IconButton(onClick = onClick) {
            Icon(
                Icons.Default.EditCalendar,
                contentDescription = "Set date",
                tint = color
            )
        }
    }
}

@Composable
private fun LectureStat(
    label: String,
    value: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                vertical = 14.dp,
                horizontal = 6.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(23.dp)
            )

            Text(
                value.toString(),
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                label,
                color = CosmicTextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
