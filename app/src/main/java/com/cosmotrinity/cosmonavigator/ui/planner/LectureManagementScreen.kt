
package com.cosmotrinity.cosmonavigator.ui.planner

import android.app.DatePickerDialog
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
import com.cosmotrinity.cosmonavigator.data.UpscLectureEntity
import com.cosmotrinity.cosmonavigator.data.UpscLectureRepository
import com.cosmotrinity.cosmonavigator.data.UpscSubjectRepository
import com.cosmotrinity.cosmonavigator.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private fun parseLectureDate(value: String): Calendar? {
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

// Generate lecture dates while skipping Sundays.
private fun generateLectureDates(
    startDate: String,
    totalLectures: Int
): List<String>? {
    val calendar = parseLectureDate(startDate) ?: return null

    val format = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    val dates = mutableListOf<String>()

    while (dates.size < totalLectures) {
        if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            dates.add(format.format(calendar.time))
        }

        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }

    return dates
}

@Composable
fun LectureManagementScreen(
    subjectId: Int,
    subjectName: String,
    lectureRepository: UpscLectureRepository,
    subjectRepository: UpscSubjectRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val lectures by lectureRepository
        .getLecturesBySubject(subjectId)
        .collectAsState(initial = emptyList())

    var showGenerateDialog by remember {
        mutableStateOf(false)
    }

    var showDeleteAllDialog by remember {
        mutableStateOf(false)
    }

    var showReduceConfirm by remember {
        mutableStateOf(false)
    }

    var totalInput by remember {
        mutableStateOf("45")
    }

    var startDate by remember {
        mutableStateOf(
            SimpleDateFormat(
                "dd-MM-yyyy",
                Locale.getDefault()
            ).format(Calendar.getInstance().time)
        )
    }

    // Default generated lecture status is Completed.
    var generatedLectureStatus by remember {
        mutableStateOf(true)
    }

    var pendingReductionTotal by remember {
        mutableIntStateOf(0)
    }

    var editingLecture by remember {
        mutableStateOf<UpscLectureEntity?>(null)
    }

    var isProcessing by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    val completedCount = lectures.count { it.isCompleted }
    val pendingCount = lectures.size - completedCount

    val progress = if (lectures.isNotEmpty()) {
        completedCount.toFloat() / lectures.size
    } else {
        0f
    }

    // Synchronize subject statistics with Room records.
    suspend fun syncSubjectStats() {
        val total = lectureRepository.getTotalLectureCount(subjectId)
        val completed = lectureRepository.getCompletedLectureCount(subjectId)

        val subject = subjectRepository
            .getSubjectById(subjectId)
            .first()

        if (subject != null) {
            subjectRepository.updateSubject(
                subject.copy(
                    totalLectures = total,
                    completedLectures = completed
                )
            )
        }
    }

    // Generate lectures, assign dates and save selected status.
    suspend fun generateLectureSchedule(total: Int) {
        val dates = generateLectureDates(startDate, total)
            ?: throw IllegalArgumentException("Invalid start date")

        // Generate missing lecture records.
        lectureRepository.generateLectures(subjectId, total)

        // Read latest saved lectures.
        val savedLectures = lectureRepository
            .getLecturesBySubject(subjectId)
            .first()
            .sortedBy { it.lectureNumber }

        // Apply date and selected status together.
        // This preserves both changes in the same Room update.
        savedLectures.forEach { lecture ->
            val index = lecture.lectureNumber - 1

            if (index in dates.indices) {
                lectureRepository.updateLecture(
                    lecture.copy(
                        scheduledDate = dates[index],
                        isCompleted = generatedLectureStatus,
                        completedAt = if (generatedLectureStatus) {
                            System.currentTimeMillis()
                        } else {
                            null
                        }
                    )
                )
            }
        }

        syncSubjectStats()
    }

    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = CosmicTextPrimary
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        "Lecture Management",
                        color = CosmicTextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        subjectName,
                        color = CosmicTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = CosmicPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { padding ->

        // FULL-PAGE SCROLLING CONTENT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // PROGRESS SUMMARY

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CosmicSurface
                )
            ) {
                Column(Modifier.padding(16.dp)) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = null,
                            tint = CosmicSecondary
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            "LECTURE PROGRESS",
                            color = CosmicTextSecondary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LectureSummary(
                            "Total",
                            lectures.size,
                            Color(0xFF75B8FF),
                            Icons.Default.LibraryBooks
                        )

                        LectureSummary(
                            "Completed",
                            completedCount,
                            Color(0xFF55D6A0),
                            Icons.Default.CheckCircle
                        )

                        LectureSummary(
                            "Pending",
                            pendingCount,
                            Color(0xFFFFC857),
                            Icons.Default.Schedule
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Color(0xFF55D6A0),
                        trackColor = CosmicSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "${(progress * 100).toInt()}% completed",
                        color = CosmicTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // GENERATE LECTURES BUTTON

            Button(
                onClick = {
                    totalInput = lectures.size
                        .takeIf { it > 0 }
                        ?.toString() ?: "45"

                    startDate = SimpleDateFormat(
                        "dd-MM-yyyy",
                        Locale.getDefault()
                    ).format(Calendar.getInstance().time)

                    showGenerateDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.PlaylistAdd,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    if (lectures.isEmpty()) {
                        "Generate Lectures"
                    } else {
                        "Update Total Lectures"
                    }
                )
            }

            if (lectures.isNotEmpty()) {
                TextButton(
                    onClick = {
                        showDeleteAllDialog = true
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = CosmicError
                    )

                    Spacer(Modifier.width(5.dp))

                    Text(
                        "Delete all lectures",
                        color = CosmicError
                    )
                }
            }

            Text(
                "YOUR LECTURES (${lectures.size})",
                color = CosmicTextSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            if (message != null) {
                Text(
                    message!!,
                    color = CosmicSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (isProcessing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = CosmicPrimary
                )
            }

            // EMPTY STATE

            if (lectures.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = CosmicTextSecondary,
                            modifier = Modifier.size(52.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "No lectures added yet",
                            color = CosmicTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            "Generate your lecture list to get started.",
                            color = CosmicTextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {

                // LECTURE CARDS

                lectures.forEach { lecture ->

                    val statusColor = if (lecture.isCompleted) {
                        Color(0xFF55D6A0)
                    } else {
                        Color(0xFFFFC857)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = CosmicSurface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Checkbox(
                                checked = lecture.isCompleted,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        try {
                                            lectureRepository.markLectureCompleted(
                                                lecture,
                                                checked
                                            )
                                            syncSubjectStats()
                                            message = "Lecture progress saved."
                                        } catch (exception: Exception) {
                                            message = "Unable to update lecture."
                                        }
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF55D6A0)
                                )
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "LECTURE ${lecture.lectureNumber}",
                                    color = CosmicSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    lecture.title,
                                    color = CosmicTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (lecture.scheduledDate.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = CosmicSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )

                                        Spacer(Modifier.width(4.dp))

                                        Text(
                                            lecture.scheduledDate,
                                            color = CosmicTextSecondary,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                if (lecture.durationMinutes > 0) {
                                    Text(
                                        "${lecture.durationMinutes} minutes",
                                        color = CosmicTextSecondary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                                if (lecture.notes.isNotBlank()) {
                                    Text(
                                        "Notes added",
                                        color = CosmicSecondary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                                Text(
                                    if (lecture.isCompleted) "Completed" else "Pending",
                                    color = statusColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            IconButton(
                                onClick = {
                                    editingLecture = lecture
                                }
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit lecture",
                                    tint = CosmicSecondary
                                )
                            }

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            lectureRepository.deleteLecture(lecture.id)
                                            syncSubjectStats()
                                            message = "Lecture deleted."
                                        } catch (exception: Exception) {
                                            message = "Unable to delete lecture."
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete lecture",
                                    tint = CosmicError
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // GENERATE / UPDATE DIALOG

    if (showGenerateDialog) {
        AlertDialog(
            onDismissRequest = {
                showGenerateDialog = false
            },
            icon = {
                Icon(
                    Icons.Default.PlaylistAdd,
                    contentDescription = null,
                    tint = CosmicPrimary
                )
            },
            title = {
                Text("Generate Lectures")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Enter the total number of lectures for this subject."
                    )

                    OutlinedTextField(
                        value = totalInput,
                        onValueChange = {
                            totalInput = it.filter { char ->
                                char.isDigit()
                            }.take(3)
                        },
                        label = {
                            Text("Total lectures (1–100)")
                        },
                        singleLine = true,
                        isError = totalInput.toIntOrNull()?.let {
                            it > 100
                        } == true
                    )

                    val enteredTotal = totalInput.toIntOrNull()

                    if (enteredTotal != null && enteredTotal > 100) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Information",
                                tint = CosmicSecondary,
                                modifier = Modifier.size(18.dp)
                            )

                            Text(
                                "Please add between 1 and 100 lectures only.",
                                color = CosmicTextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // START DATE

                    Text(
                        "LECTURE START DATE",
                        color = CosmicTextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = startDate,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text("Start date (DD-MM-YYYY)")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val calendar = parseLectureDate(startDate)
                                        ?: Calendar.getInstance()

                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val selected = Calendar.getInstance().apply {
                                                set(year, month, day)
                                            }

                                            startDate = SimpleDateFormat(
                                                "dd-MM-yyyy",
                                                Locale.getDefault()
                                            ).format(selected.time)
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                            ) {
                                Icon(
                                    Icons.Default.Event,
                                    contentDescription = "Choose start date"
                                )
                            }
                        }
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = CosmicSecondary,
                            modifier = Modifier.size(18.dp)
                        )

                        Text(
                            "Sundays will be automatically skipped.",
                            color = CosmicTextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

// LECTURE STATUS
                    Text(
                        "LECTURE STATUS",
                        color = CosmicTextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = CosmicSurface
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = generatedLectureStatus,
                                    onClick = {
                                        generatedLectureStatus = true
                                    },
                                    modifier = Modifier.size(36.dp)
                                )

                                Text(
                                    "Completed",
                                    color = CosmicTextPrimary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = !generatedLectureStatus,
                                    onClick = {
                                        generatedLectureStatus = false
                                    },
                                    modifier = Modifier.size(36.dp)
                                )

                                Text(
                                    "Pending",
                                    color = CosmicTextPrimary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    if (startDate.isNotBlank() && parseLectureDate(startDate) == null) {
                        Text(
                            "Please select a valid start date.",
                            color = CosmicError,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val total = totalInput.toIntOrNull()
                        val validDate = parseLectureDate(startDate) != null

                        if (total != null && total in 1..100 && validDate) {
                            if (total < lectures.size) {
                                pendingReductionTotal = total
                                showGenerateDialog = false
                                showReduceConfirm = true
                            } else {
                                scope.launch {
                                    isProcessing = true
                                    message = null

                                    try {
                                        generateLectureSchedule(total)
                                        message = "Lectures, dates and status saved successfully."
                                    } catch (exception: Exception) {
                                        message = "Unable to generate lecture schedule."
                                    } finally {
                                        isProcessing = false
                                        showGenerateDialog = false
                                    }
                                }
                            }
                        }
                    },
                    enabled = totalInput.toIntOrNull()?.let {
                        it in 1..100
                    } == true && parseLectureDate(startDate) != null
                ) {
                    Text("Generate")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showGenerateDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // CONFIRM REDUCING LECTURE COUNT

    if (showReduceConfirm) {
        AlertDialog(
            onDismissRequest = {
                showReduceConfirm = false
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = CosmicWarning
                )
            },
            title = {
                Text("Reduce lecture count?")
            },
            text = {
                Text(
                    "Lectures numbered above $pendingReductionTotal will be permanently deleted, including their saved notes and completion status."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isProcessing = true

                            try {
                                lectures
                                    .filter {
                                        it.lectureNumber > pendingReductionTotal
                                    }
                                    .forEach {
                                        lectureRepository.deleteLecture(it.id)
                                    }

                                syncSubjectStats()
                                message = "Lecture count updated."
                            } catch (exception: Exception) {
                                message = "Unable to update lecture count."
                            } finally {
                                isProcessing = false
                                showReduceConfirm = false
                            }
                        }
                    }
                ) {
                    Text("Delete extra lectures", color = CosmicError)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showReduceConfirm = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // DELETE ALL CONFIRMATION

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteAllDialog = false
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = CosmicError
                )
            },
            title = {
                Text("Delete all lectures?")
            },
            text = {
                Text(
                    "This will permanently remove all saved lecture records and their notes for this subject."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isProcessing = true

                            try {
                                lectureRepository.deleteAllLectures(subjectId)
                                syncSubjectStats()
                                message = "All lectures deleted."
                            } catch (exception: Exception) {
                                message = "Unable to delete lectures."
                            } finally {
                                isProcessing = false
                                showDeleteAllDialog = false
                            }
                        }
                    }
                ) {
                    Text("Delete all", color = CosmicError)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteAllDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // EDIT LECTURE DIALOG

    editingLecture?.let { lecture ->

        var title by remember(lecture.id) {
            mutableStateOf(lecture.title)
        }

        var date by remember(lecture.id) {
            mutableStateOf(lecture.scheduledDate)
        }

        var duration by remember(lecture.id) {
            mutableStateOf(
                if (lecture.durationMinutes > 0) {
                    lecture.durationMinutes.toString()
                } else {
                    ""
                }
            )
        }

        var notes by remember(lecture.id) {
            mutableStateOf(lecture.notes)
        }

        var showDateError by remember(lecture.id) {
            mutableStateOf(false)
        }

        AlertDialog(
            onDismissRequest = {
                editingLecture = null
            },
            icon = {
                Icon(
                    Icons.Default.EditNote,
                    contentDescription = null,
                    tint = CosmicSecondary
                )
            },
            title = {
                Text("Edit Lecture ${lecture.lectureNumber}")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                        },
                        label = {
                            Text("Lecture title")
                        },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = date,
                        onValueChange = {
                            date = it
                            showDateError = false
                        },
                        label = {
                            Text("Date (DD-MM-YYYY)")
                        },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null
                            )
                        }
                    )

                    TextButton(
                        onClick = {
                            val calendar = parseLectureDate(date)
                                ?: Calendar.getInstance()

                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val selected = Calendar.getInstance().apply {
                                        set(year, month, day)
                                    }

                                    date = SimpleDateFormat(
                                        "dd-MM-yyyy",
                                        Locale.getDefault()
                                    ).format(selected.time)

                                    showDateError = false
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    ) {
                        Icon(
                            Icons.Default.Event,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(6.dp))

                        Text("Choose from calendar")
                    }

                    if (showDateError) {
                        Text(
                            "Enter a valid date in DD-MM-YYYY format.",
                            color = CosmicError,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    OutlinedTextField(
                        value = duration,
                        onValueChange = {
                            duration = it.filter { char ->
                                char.isDigit()
                            }.take(4)
                        },
                        label = {
                            Text("Duration (minutes)")
                        },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                        },
                        label = {
                            Text("Lecture notes")
                        },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val validDate = date.isBlank() ||
                                parseLectureDate(date) != null

                        val validDuration = duration.isBlank() ||
                                (duration.toIntOrNull() ?: 0) > 0

                        if (
                            title.isNotBlank() &&
                            validDate &&
                            validDuration
                        ) {
                            scope.launch {
                                try {
                                    lectureRepository.updateLecture(
                                        lecture.copy(
                                            title = title.trim(),
                                            scheduledDate = date,
                                            durationMinutes =
                                                duration.toIntOrNull() ?: 0,
                                            notes = notes.trim()
                                        )
                                    )

                                    message = "Lecture details saved."
                                    editingLecture = null
                                } catch (exception: Exception) {
                                    message = "Unable to save lecture details."
                                }
                            }
                        } else {
                            showDateError = !validDate
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editingLecture = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LectureSummary(
    label: String,
    value: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
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
