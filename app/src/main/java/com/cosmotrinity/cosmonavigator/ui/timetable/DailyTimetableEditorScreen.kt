
package com.cosmotrinity.cosmonavigator.ui.timetable

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cosmotrinity.cosmonavigator.data.DailyTimetableEntity
import com.cosmotrinity.cosmonavigator.data.DailyTimetableRepository
import com.cosmotrinity.cosmonavigator.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ----------------------------------------------------
// ACTIVITY TYPES
// ----------------------------------------------------

private data class TimetableActivityType(
    val title: String,
    val icon: ImageVector,
    val countsAsStudy: Boolean
)

private val timetableActivityTypes = listOf(
    TimetableActivityType(
        "Classroom Lecture",
        Icons.Default.School,
        true
    ),
    TimetableActivityType(
        "Notes Revision",
        Icons.Default.History,
        true
    ),
    TimetableActivityType(
        "PYQs",
        Icons.Default.Quiz,
        true
    ),
    TimetableActivityType(
        "Answer Writing",
        Icons.Default.Edit,
        true
    ),
    TimetableActivityType(
        "Current Affairs",
        Icons.Default.Newspaper,
        true
    ),
    TimetableActivityType(
        "Other",
        Icons.Default.MoreHoriz,
        false
    )
)

private fun normalizeActivityType(oldType: String): String {
    return when (oldType) {
        "Lecture" -> "Classroom Lecture"
        "Study", "Self Study", "Revision" -> "Notes Revision"
        "Test" -> "PYQs"
        "Break" -> "Other"
        else -> oldType
    }
}

private fun getActivityType(
    title: String
): TimetableActivityType {
    val normalized = normalizeActivityType(title)

    return timetableActivityTypes.firstOrNull {
        it.title == normalized
    } ?: timetableActivityTypes.first()
}

private fun getActivityIcon(
    activityType: String
): ImageVector {
    return getActivityType(activityType).icon
}

// ----------------------------------------------------
// TIME HELPERS
// ----------------------------------------------------

private data class TimetableTime(
    val hour: Int,
    val minute: Int
)

private fun formatTimetableTime(minutes: Int): String {
    val normalized = ((minutes % 1440) + 1440) % 1440
    val hour = normalized / 60
    val minute = normalized % 60

    val period = if (hour < 12) "AM" else "PM"

    val displayHour = when (val value = hour % 12) {
        0 -> 12
        else -> value
    }

    return "%02d:%02d %s".format(
        displayHour,
        minute,
        period
    )
}

private fun minutesToTime(minutes: Int): TimetableTime {
    val normalized = ((minutes % 1440) + 1440) % 1440

    return TimetableTime(
        hour = normalized / 60,
        minute = normalized % 60
    )
}

private fun showTimetableTimePicker(
    context: Context,
    initialMinutes: Int,
    onSelected: (Int) -> Unit
) {
    val time = minutesToTime(initialMinutes)

    TimePickerDialog(
        context,
        { _, hour, minute ->
            onSelected(hour * 60 + minute)
        },
        time.hour,
        time.minute,
        false
    ).show()
}

// ----------------------------------------------------
// MAIN TIMETABLE EDITOR
// ----------------------------------------------------

@Composable
fun DailyTimetableEditorScreen(
    repository: DailyTimetableRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val activities by repository
        .observeActivities()
        .collectAsState(initial = emptyList())

    val settings by repository
        .observeSettings()
        .collectAsState(initial = null)

    var wakeUpMinutes by remember {
        mutableIntStateOf(300)
    }

    var sleepMinutes by remember {
        mutableIntStateOf(1350)
    }

    var settingsInitialized by remember {
        mutableStateOf(false)
    }

    var showActivityDialog by remember {
        mutableStateOf(false)
    }

    var editingActivity by remember {
        mutableStateOf<DailyTimetableEntity?>(null)
    }

    var showDeleteDialog by remember {
        mutableStateOf<DailyTimetableEntity?>(null)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    var isSavingSettings by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(settings) {
        if (!settingsInitialized) {
            settings?.let {
                wakeUpMinutes = it.wakeUpTimeMinutes
                sleepMinutes = it.sleepTimeMinutes
            }

            settingsInitialized = true
        }
    }

    fun saveDaySettings() {
        scope.launch {
            isSavingSettings = true

            try {
                repository.saveSettings(
                    wakeUpTime = DailyTimetableRepository.formatTime(
                        wakeUpMinutes
                    ),
                    sleepTime = DailyTimetableRepository.formatTime(
                        sleepMinutes
                    )
                )

                message = "Daily timings saved."

            } catch (e: Exception) {
                message = e.message ?: "Unable to save timings."

            } finally {
                isSavingSettings = false
            }
        }
    }

    Scaffold(
        containerColor = CosmicBackground,

        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicBackground)
                    .padding(
                        horizontal = 12.dp,
                        vertical = 14.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = CosmicTextPrimary
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Daily Timetable",
                        color = CosmicTextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Build your complete daily routine",
                        color = CosmicTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = CosmicPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingActivity = null
                    showActivityDialog = true
                },
                containerColor = CosmicPrimary,
                contentColor = CosmicBackground
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add activity"
                )
            }
        }

    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBackground)
                .padding(padding)
                .padding(horizontal = 16.dp),

            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = 100.dp
            ),

            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // WAKE UP AND SLEEP SETTINGS

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CosmicSurface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        Text(
                            "Your Daily Routine",
                            color = CosmicTextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.WbSunny,
                                        contentDescription = null,
                                        tint = CosmicPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(Modifier.width(6.dp))

                                    Text(
                                        "Wake up",
                                        color = CosmicTextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        showTimetableTimePicker(
                                            context,
                                            wakeUpMinutes
                                        ) {
                                            wakeUpMinutes = it
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        formatTimetableTime(wakeUpMinutes),
                                        color = CosmicTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Bedtime,
                                        contentDescription = null,
                                        tint = CosmicSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(Modifier.width(6.dp))

                                    Text(
                                        "Sleep",
                                        color = CosmicTextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        showTimetableTimePicker(
                                            context,
                                            sleepMinutes
                                        ) {
                                            sleepMinutes = it
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        formatTimetableTime(sleepMinutes),
                                        color = CosmicTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (sleepMinutes <= wakeUpMinutes) {
                                "Sleep is scheduled for the next day."
                            } else {
                                "Wake-up and sleep are on the same day."
                            },
                            color = CosmicTextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Button(
                            onClick = { saveDaySettings() },
                            enabled = !isSavingSettings,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CosmicPrimary,
                                contentColor = CosmicBackground
                            )
                        ) {
                            if (isSavingSettings) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = CosmicBackground,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Save Day Timings")
                            }
                        }
                    }
                }
            }

            // ACTIVITY SUMMARY

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Your Activities",
                        color = CosmicTextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "${activities.count { it.isEnabled }} active • ${activities.count { !it.isEnabled }} disabled",
                        color = CosmicTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // EMPTY STATE

            if (activities.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = CosmicSurface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),

                            horizontalAlignment = Alignment.CenterHorizontally,

                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = CosmicPrimary,
                                modifier = Modifier.size(34.dp)
                            )

                            Text(
                                "Your timetable is empty",
                                color = CosmicTextPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                "Add your first lecture, study session or daily activity.",
                                color = CosmicTextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )

                            FilledTonalButton(
                                onClick = {
                                    editingActivity = null
                                    showActivityDialog = true
                                }
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Add First Activity")
                            }
                        }
                    }
                }
            }

            // ACTIVITY LIST

            items(
                items = activities,
                key = { it.id }
            ) { activity ->

                TimetableActivityCard(
                    activity = activity,

                    onEdit = {
                        editingActivity = activity
                        showActivityDialog = true
                    },

                    onDelete = {
                        showDeleteDialog = activity
                    },

                    onToggle = { enabled ->
                        scope.launch {
                            try {
                                repository.setActivityEnabled(
                                    activityId = activity.id,
                                    enabled = enabled
                                )
                            } catch (e: Exception) {
                                message = e.message
                                    ?: "Unable to update activity."
                            }
                        }
                    }
                )
            }

            item {
                Text(
                    "Your timetable repeats every day. Individual-day changes will be supported separately.",
                    color = CosmicTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    // ADD OR EDIT DIALOG

    if (showActivityDialog) {
        TimetableActivityDialog(
            activity = editingActivity,

            onDismiss = {
                showActivityDialog = false
                editingActivity = null
            },

            onSave = { activity ->
                scope.launch {
                    try {
                        repository.saveActivity(activity)

                        message = if (activity.id == 0) {
                            "Activity added successfully."
                        } else {
                            "Activity updated successfully."
                        }

                        showActivityDialog = false
                        editingActivity = null

                    } catch (e: Exception) {
                        message = e.message
                            ?: "Unable to save activity."
                    }
                }
            }
        )
    }

    // DELETE CONFIRMATION

    showDeleteDialog?.let { activity ->

        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = null
            },

            title = {
                Text("Delete Activity?")
            },

            text = {
                Text(
                    "Do you want to delete ${activity.title} from your timetable?"
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                repository.deleteActivity(activity.id)
                                message = "Activity deleted."
                            } catch (e: Exception) {
                                message = e.message
                                    ?: "Unable to delete activity."
                            }

                            showDeleteDialog = null
                        }
                    }
                ) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // FEEDBACK

    message?.let { currentMessage ->

        LaunchedEffect(currentMessage) {
            delay(2500)
            if (message == currentMessage) {
                message = null
            }
        }

        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = CosmicSurfaceVariant
        ) {
            Text(
                currentMessage,
                color = CosmicTextPrimary
            )
        }
    }
}

// ----------------------------------------------------
// ACTIVITY CARD
// ----------------------------------------------------

@Composable
private fun TimetableActivityCard(
    activity: DailyTimetableEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val duration = DailyTimetableRepository.durationMinutes(
        activity.startTimeMinutes,
        activity.endTimeMinutes
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CosmicSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = CosmicPrimary.copy(alpha = 0.14f)
                ) {
                    Icon(
                        getActivityIcon(activity.activityType),
                        contentDescription = activity.activityType,
                        tint = CosmicPrimary,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        activity.title,
                        color = if (activity.isEnabled) {
                            CosmicTextPrimary
                        } else {
                            CosmicTextSecondary
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        if (activity.isEnabled) {
                            normalizeActivityType(activity.activityType)
                        } else {
                            "${normalizeActivityType(activity.activityType)} • Disabled"
                        },
                        color = if (activity.isEnabled) {
                            CosmicPrimary
                        } else {
                            CosmicTextSecondary
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = activity.isEnabled,
                    onCheckedChange = onToggle
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = CosmicSecondary,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    "${formatTimetableTime(activity.startTimeMinutes)} – ${
                        formatTimetableTime(activity.endTimeMinutes)
                    }",
                    color = CosmicTextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.weight(1f))

                Text(
                    "${duration / 60}h ${duration % 60}m",
                    color = CosmicTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (activity.endTimeMinutes >= 1440) {
                Text(
                    "Ends on the next day",
                    color = CosmicSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit activity",
                        tint = CosmicPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete activity",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// DECORATED SECTION HEADER
// ----------------------------------------------------

@Composable
private fun TimetableSectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(10.dp),
            color = CosmicPrimary.copy(alpha = 0.15f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = CosmicPrimary,
                modifier = Modifier.padding(9.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                title,
                color = CosmicTextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                subtitle,
                color = CosmicTextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// ----------------------------------------------------
// ADD / EDIT ACTIVITY DIALOG
// ----------------------------------------------------

@Composable
private fun TimetableActivityDialog(
    activity: DailyTimetableEntity?,
    onDismiss: () -> Unit,
    onSave: (DailyTimetableEntity) -> Unit
) {
    val context = LocalContext.current

    var title by remember(activity) {
        mutableStateOf(activity?.title ?: "")
    }

    var activityType by remember(activity) {
        mutableStateOf(
            getActivityType(
                activity?.activityType ?: "Self Study"
            ).title
        )
    }

    var startTime by remember(activity) {
        mutableIntStateOf(activity?.startTimeMinutes ?: 360)
    }

    var endTime by remember(activity) {
        mutableIntStateOf(
            activity?.endTimeMinutes?.rem(1440) ?: 420
        )
    }

    var categoryExpanded by remember {
        mutableStateOf(false)
    }

    var titleError by remember {
        mutableStateOf(false)
    }

    val selectedType = getActivityType(activityType)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = CosmicSurface,

        title = {
            Text(
                if (activity == null) "Add Activity" else "Edit Activity",
                color = CosmicTextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },

        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // ACTIVITY NAME
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = titleError,
                    label = { Text("Activity name") },
                    placeholder = { Text("e.g. Sociology Lecture") },
                    shape = RoundedCornerShape(12.dp)
                )

                if (titleError) {
                    Text(
                        "Enter activity name",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // CATEGORY DROPDOWN
                Box(modifier = Modifier.fillMaxWidth()) {

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                categoryExpanded = true
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = CosmicBackground,
                        border = BorderStroke(
                            1.dp,
                            CosmicPrimary.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                selectedType.icon,
                                contentDescription = null,
                                tint = CosmicPrimary
                            )

                            Spacer(Modifier.width(10.dp))

                            Text(
                                selectedType.title,
                                modifier = Modifier.weight(1f),
                                color = CosmicTextPrimary,
                                fontWeight = FontWeight.Medium
                            )

                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = "Select category",
                                tint = CosmicPrimary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = {
                            categoryExpanded = false
                        },
                        modifier = Modifier
                            .widthIn(min = 180.dp, max = 250.dp)
                            .background(CosmicSurface)
                    ) {
                        timetableActivityTypes.forEach { type ->

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        type.title,
                                        color = CosmicTextPrimary,
                                        maxLines = 1
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        type.icon,
                                        contentDescription = null,
                                        tint = CosmicPrimary
                                    )
                                },
                                trailingIcon = {
                                    if (activityType == type.title) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = CosmicPrimary
                                        )
                                    }
                                },
                                onClick = {
                                    activityType = type.title
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // START AND END TIME
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showTimetableTimePicker(
                                context,
                                startTime
                            ) {
                                startTime = it
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(
                            horizontal = 8.dp,
                            vertical = 8.dp
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "START",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                formatTimetableTime(startTime),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showTimetableTimePicker(
                                context,
                                endTime
                            ) {
                                endTime = it
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(
                            horizontal = 8.dp,
                            vertical = 8.dp
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "END",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                formatTimetableTime(endTime),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

            }
        },

        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }

                    val normalizedEnd = if (endTime <= startTime) {
                        endTime + 1440
                    } else {
                        endTime
                    }

                    if (normalizedEnd - startTime > 1440) {
                        return@Button
                    }

                    onSave(
                        DailyTimetableEntity(
                            id = activity?.id ?: 0,
                            title = title.trim(),
                            startTimeMinutes = startTime,
                            endTimeMinutes = endTime,
                            activityType = activityType,
                            countsAsStudy = selectedType.countsAsStudy,
                            sortOrder = activity?.sortOrder ?: startTime,
                            notes = activity?.notes ?: "",
                            isEnabled = activity?.isEnabled ?: true,
                            createdAt = activity?.createdAt
                                ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicPrimary,
                    contentColor = CosmicBackground
                )
            ) {
                Text("Save")
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CosmicTextSecondary)
            }
        }
    )
}
