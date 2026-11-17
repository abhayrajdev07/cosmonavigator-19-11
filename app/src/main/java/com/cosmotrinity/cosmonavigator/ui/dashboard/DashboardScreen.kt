package com.cosmotrinity.cosmonavigator.ui.dashboard

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmotrinity.cosmonavigator.data.DataStoreManager
import com.cosmotrinity.cosmonavigator.data.DailyStudySessionEntity
import com.cosmotrinity.cosmonavigator.data.DailyTimetableEntity
import com.cosmotrinity.cosmonavigator.data.DailyTimetableRepository
import com.cosmotrinity.cosmonavigator.data.DailySleepRecordRepository
import com.cosmotrinity.cosmonavigator.data.StoredAspirantGoals
import com.cosmotrinity.cosmonavigator.data.StoredAspirantProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val DashNavy = Color(0xFFF3F7FF)       // Primary text
private val DashBlue = Color(0xFF9CC2FF)       // Blue accent
private val DashTeal = Color(0xFF75D4CC)       // Teal accent
private val DashGreen = Color(0xFF8BE0A5)
private val DashGreenBg = Color(0xFF183D35)
private val DashAmber = Color(0xFFFFD27A)
private val DashAmberBg = Color(0xFF44371F)
private val DashRed = Color(0xFFFFA1A1)
private val DashRedBg = Color(0xFF48282F)
private val DashMuted = Color(0xFFB8C8DE)      // Secondary text
private val DashBorder = Color(0xFF2B496D)
private val DashWhite = Color(0xFFF8FAFF)      // High-emphasis text
private val DashBackground = Color(0xFF07162E)
private val DashHeader = Color(0xFF0B1E3A)
private val DashCard = Color(0xFF132B4B)
private val DashCardRaised = Color(0xFF19385D)
private val DashCardSoft = Color(0xFF1A3455)
private val DashCountdownBlue = Color(0xFF204A7A)
private val DashCountdownTeal = Color(0xFF15565C)

private fun dashboardTime(minutes: Int): String {
    val normalized = ((minutes % 1440) + 1440) % 1440
    val hour = normalized / 60
    val minute = normalized % 60
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = if (hour % 12 == 0) 12 else hour % 12
    return "%02d:%02d %s".format(displayHour, minute, period)
}

private fun dashboardDuration(seconds: Long): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    return "${hours}h ${minutes}m"
}

private fun scheduledDurationSeconds(activity: DailyTimetableEntity): Long {
    val raw = activity.endTimeMinutes - activity.startTimeMinutes
    val minutes = if (raw < 0) raw + 1440 else raw
    return minutes.coerceAtLeast(0) * 60L
}

private fun findCurrentActivity(
    activities: List<DailyTimetableEntity>,
    currentMinute: Int
): DailyTimetableEntity? {
    return activities.filter { it.isEnabled }.firstOrNull { activity ->
        val start = activity.startTimeMinutes
        val end = activity.endTimeMinutes
        (currentMinute >= start && currentMinute < end) ||
                (end > 1440 && currentMinute < end - 1440)
    }
}

private fun findNextActivity(
    activities: List<DailyTimetableEntity>,
    currentMinute: Int
): DailyTimetableEntity? {
    return activities.filter { it.isEnabled && it.startTimeMinutes > currentMinute }
        .minByOrNull { it.startTimeMinutes }
        ?: activities.filter { it.isEnabled }.minByOrNull { it.startTimeMinutes }
}

private fun activityStudySeconds(
    activityId: Int,
    sessions: List<DailyStudySessionEntity>,
    now: Long
): Long {
    return sessions.filter { it.activityId == activityId }.sumOf { session ->
        if (session.status == DailyTimetableRepository.STATUS_RUNNING) {
            ((now - session.startedAt) / 1000L).coerceAtLeast(0L)
        } else {
            session.durationSeconds.coerceAtLeast(0L)
        }
    }
}

@Composable
fun DashboardScreen(
    dataStore: DataStoreManager,
    timetableRepository: DailyTimetableRepository,
    sleepRecordRepository: DailySleepRecordRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSleepWakeDialog by remember { mutableStateOf(false) }

    val profile by dataStore.profileFlow.collectAsState(initial = StoredAspirantProfile())
    val goals by dataStore.goalsFlow.collectAsState(initial = StoredAspirantGoals())

    var profileBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(profile.profileImageUri) {
        profileBitmap = if (!profile.profileImageUri.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(Uri.parse(profile.profileImageUri))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                } catch (_: Exception) {
                    null
                }
            }
        } else null
    }

    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    var currentTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            currentTimestamp = System.currentTimeMillis()
            delay(1000)
        }
    }

    val today = LocalDate.now()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.getDefault()) }
    val todayString = remember(today) { today.format(dateFormatter) }
    val displayDate = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.getDefault()))
    }
    val displayTime = remember(currentTime) {
        currentTime.format(DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.getDefault()))
    }

    val prelimsDate = remember(goals.prelimsDate) {
        runCatching { LocalDate.parse(goals.prelimsDate, dateFormatter) }.getOrNull()
    }
    val preparationStartDate = remember(goals.preparationStartDate) {
        runCatching { LocalDate.parse(goals.preparationStartDate, dateFormatter) }.getOrNull()
    }
    val daysRemaining = prelimsDate?.let { ChronoUnit.DAYS.between(today, it).coerceAtLeast(0) }
    val preparationDays = preparationStartDate?.let { ChronoUnit.DAYS.between(it, today).coerceAtLeast(0) }

    val allActivities by timetableRepository.observeActivities().collectAsState(initial = emptyList())
    val activities = remember(allActivities) { allActivities.filter { it.isEnabled }.sortedBy { it.startTimeMinutes } }
    val sessions by timetableRepository.observeSessionsForDate(todayString).collectAsState(initial = emptyList())
    val completions by timetableRepository.observeCompletionsForDate(todayString).collectAsState(initial = emptyList())

    // Count study time separately for each scheduled activity.
    // A timer can remain RUNNING after the app is closed or the user forgets
    // to stop it. Cap each activity at its own scheduled duration so one
    // forgotten timer cannot inflate the dashboard beyond the daily timetable.
    //
    // Done: credit the scheduled duration, unless less time was scheduled
    // than was already tracked (the cap still applies).
    // Pending/Missed: count tracked time, capped at the activity duration.
    val actualStudySeconds = activities
        .filter { it.countsAsStudy }
        .sumOf { activity ->
            val scheduledSeconds = scheduledDurationSeconds(activity)
            val trackedSeconds = activityStudySeconds(
                activity.id,
                sessions,
                currentTimestamp
            )

            val isDone = completions.firstOrNull { it.activityId == activity.id }?.status ==
                    DailyTimetableRepository.COMPLETION_DONE

            val creditedSeconds = if (isDone) {
                maxOf(scheduledSeconds, trackedSeconds)
            } else {
                trackedSeconds
            }

            creditedSeconds.coerceIn(0L, scheduledSeconds)
        }

    val currentMinute = currentTime.hour * 60 + currentTime.minute
    val currentActivity = remember(activities, currentMinute) { findCurrentActivity(activities, currentMinute) }
    val nextActivity = remember(activities, currentMinute) { findNextActivity(activities, currentMinute) }

    val runningSession = sessions.firstOrNull { it.status == DailyTimetableRepository.STATUS_RUNNING }
    val pausedSession = currentActivity?.let { activity ->
        sessions.firstOrNull {
            it.activityId == activity.id && it.status == DailyTimetableRepository.STATUS_PAUSED
        }
    }

    val doneCount = activities.count { activity ->
        completions.firstOrNull { it.activityId == activity.id }?.status ==
                DailyTimetableRepository.COMPLETION_DONE
    }
    val missedCount = activities.count { activity ->
        completions.firstOrNull { it.activityId == activity.id }?.status ==
                DailyTimetableRepository.COMPLETION_NOT_DONE
    }
    val pendingCount = (activities.size - doneCount - missedCount).coerceAtLeast(0)
    val dailyGoalSeconds = activities
        .filter { it.countsAsStudy }
        .sumOf { scheduledDurationSeconds(it) }
    val goalProgress = if (dailyGoalSeconds > 0L) {
        (actualStudySeconds.toFloat() / dailyGoalSeconds).coerceIn(0f, 1f)
    } else 0f

    var actionMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DashBackground)
    ) {
        // Fixed header: this area stays in place while the dashboard content scrolls.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DashHeader)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (profileBitmap != null) {
                    Image(
                        bitmap = profileBitmap!!.asImageBitmap(),
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(DashCardRaised)
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = DashCardRaised,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, DashBlue.copy(alpha = 0.55f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = DashBlue, modifier = Modifier.size(29.dp))
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (profile.fullName.isNotBlank()) "Hello, ${profile.fullName}" else "Hello, Aspirant",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashNavy,
                        maxLines = 1
                    )
                    Text(
                        "UPSC CSE ${goals.targetExamYear} • ${profile.preparationStage}",
                        fontSize = 11.sp,
                        color = DashMuted,
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Event, contentDescription = null, tint = DashBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(displayDate, fontSize = 11.sp, color = DashMuted, modifier = Modifier.weight(1f))
                Text(displayTime, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DashNavy)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CountdownCard(
                    modifier = Modifier.weight(1f),
                    heading = "UPSC PRELIMS",
                    value = daysRemaining?.toString() ?: "--",
                    caption = "Days remaining",
                    date = goals.prelimsDate,
                    container = DashCountdownBlue
                )
                CountdownCard(
                    modifier = Modifier.weight(1f),
                    heading = "YOUR JOURNEY",
                    value = preparationDays?.toString() ?: "--",
                    caption = "Days passed",
                    date = goals.preparationStartDate,
                    container = DashCountdownTeal
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            item {
                Card(
                    onClick = { showSleepWakeDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DashCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DashBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 66.dp)
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = DashCardRaised
                        ) {
                            Icon(
                                Icons.Default.WbSunny,
                                contentDescription = "Wake-up",
                                tint = DashAmber,
                                modifier = Modifier.padding(9.dp).size(21.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Sleep & Wake Tracker",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DashNavy
                            )
                            Text(
                                "Add sleep and wake-up time to track",
                                fontSize = 10.sp,
                                color = DashMuted
                            )
                        }
                        Icon(
                            Icons.Default.Event,
                            contentDescription = "Open sleep tracker",
                            tint = DashBlue,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DashCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(12.dp), color = DashCardRaised) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = DashBlue, modifier = Modifier.padding(9.dp).size(22.dp))
                            }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text("STUDY TIME TODAY", fontSize = 10.sp, color = DashMuted, fontWeight = FontWeight.Bold)
                                Text(dashboardDuration(actualStudySeconds), fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = DashNavy)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("SCHEDULED STUDY", fontSize = 10.sp, color = DashMuted, fontWeight = FontWeight.Bold)
                                Text(dashboardDuration(dailyGoalSeconds), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = DashTeal)
                            }
                        }
                        LinearProgressIndicator(
                            progress = { goalProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = DashTeal,
                            trackColor = DashBorder
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${(goalProgress * 100).toInt()}% of daily target", fontSize = 11.sp, color = DashMuted)
                            Text(
                                if (dailyGoalSeconds > 0L && actualStudySeconds >= dailyGoalSeconds) "Target achieved!" else "Keep going",
                                fontSize = 11.sp, color = DashTeal, fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Daily target is calculated from the scheduled durations of all study activities in your timetable.",
                            fontSize = 10.sp,
                            color = DashMuted
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DashCard)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        Text("DAILY OVERVIEW", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DashNavy)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusSummary("DONE", doneCount, DashGreen, DashGreenBg, Modifier.weight(1f))
                            StatusSummary("PENDING", pendingCount, DashAmber, DashAmberBg, Modifier.weight(1f))
                            StatusSummary("MISSED", missedCount, DashRed, DashRedBg, Modifier.weight(1f))
                        }
                        LinearProgressIndicator(
                            progress = { if (activities.isNotEmpty()) doneCount.toFloat() / activities.size else 0f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = DashGreen,
                            trackColor = DashBorder
                        )
                        Text("${activities.size} scheduled activities", fontSize = 10.sp, color = DashMuted)
                    }
                }
            }

            item {
                Text("Today's Timetable", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DashNavy)
            }

            if (activities.isEmpty()) {
                item { EmptyDashboardCard("No activities added yet. Add your routine from More → Daily Timetable.") }
            }

            itemsIndexed(activities, key = { _, activity -> activity.id }) { index, activity ->
                val completion = completions.firstOrNull { it.activityId == activity.id }
                val status = completion?.status ?: DailyTimetableRepository.COMPLETION_PENDING
                val isDone = status == DailyTimetableRepository.COMPLETION_DONE
                val isMissed = status == DailyTimetableRepository.COMPLETION_NOT_DONE
                val taskSeconds = activityStudySeconds(activity.id, sessions, currentTimestamp)
                val cardBackground = when {
                    isDone -> Color.Transparent
                    isMissed -> DashRedBg
                    else -> DashCard
                }
                val borderColor = when {
                    isDone -> DashGreen.copy(alpha = 0.55f)
                    isMissed -> DashRed.copy(alpha = 0.75f)
                    else -> DashBorder
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(17.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Column(Modifier.padding(horizontal = 11.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DashCardRaised
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    color = DashBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(Modifier.width(7.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    isDone -> DashGreenBg
                                    isMissed -> DashRedBg
                                    else -> DashCardSoft
                                }
                            ) {
                                Icon(
                                    when {
                                        isDone -> Icons.Default.CheckCircle
                                        isMissed -> Icons.Default.Close
                                        else -> Icons.Default.Schedule
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        isDone -> DashGreen
                                        isMissed -> DashRed
                                        else -> DashMuted
                                    },
                                    modifier = Modifier.padding(6.dp).size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    activity.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isDone -> DashGreen
                                        isMissed -> DashRed
                                        else -> DashWhite
                                    }
                                )
                                Text(
                                    "${dashboardTime(activity.startTimeMinutes)} – ${dashboardTime(activity.endTimeMinutes)}",
                                    fontSize = 10.sp,
                                    color = DashMuted
                                )
                            }
                            StatusPill(
                                label = when {
                                    isDone -> "DONE"
                                    isMissed -> "MISSED"
                                    else -> "PENDING"
                                },
                                foreground = when {
                                    isDone -> DashGreen
                                    isMissed -> DashRed
                                    else -> DashAmber
                                },
                                background = when {
                                    isDone -> Color.Transparent
                                    isMissed -> DashRedBg
                                    else -> DashAmberBg
                                }
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(activity.activityType, fontSize = 10.sp, color = DashMuted)
                                Text(
                                    if (activity.countsAsStudy) "Total time: ${dashboardDuration(scheduledDurationSeconds(activity))}" else "Non-study activity",
                                    fontSize = 11.sp,
                                    color = if (isMissed) DashRed else DashMuted,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (isDone) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                timetableRepository.resetActivityStatus(activity.id, todayString)
                                                actionMessage = "${activity.title} moved to Pending."
                                            } catch (e: Exception) { actionMessage = e.message }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DashMuted)
                                ) { Text("Undo", fontSize = 11.sp) }
                            } else if (isMissed) {
                                // A missed activity can only be restored to Pending.
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                timetableRepository.resetActivityStatus(activity.id, todayString)
                                                actionMessage = "${activity.title} moved to Pending."
                                            } catch (e: Exception) { actionMessage = e.message }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DashMuted)
                                ) { Text("Undo", fontSize = 11.sp) }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    timetableRepository.markActivityNotDone(activity.id, todayString)
                                                    actionMessage = "${activity.title} marked Missed."
                                                } catch (e: Exception) { actionMessage = e.message }
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DashRed)
                                    ) { Text("Missed", fontSize = 11.sp) }

                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    timetableRepository.markActivityDone(activity.id, todayString)
                                                    actionMessage = "${activity.title} marked Done."
                                                } catch (e: Exception) { actionMessage = e.message }
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                                        colors = ButtonDefaults.textButtonColors(contentColor = DashGreen)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp), tint = DashGreen)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Done", fontSize = 11.sp, color = DashGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(10.dp)) }
        }

        if (showSleepWakeDialog) {
            SleepWakeEditorDialog(
                repository = sleepRecordRepository,
                onDismiss = { showSleepWakeDialog = false }
            )
        }
    }

}

@Composable
private fun CountdownCard(
    modifier: Modifier = Modifier,
    heading: String,
    value: String,
    caption: String,
    date: String,
    container: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 12.dp)) {
            Text(heading, color = DashWhite.copy(alpha = 0.72f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, color = DashWhite, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
            Text(caption, color = DashWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(date, color = DashWhite.copy(alpha = 0.72f), fontSize = 9.sp)
        }
    }
}

@Composable
private fun StatusSummary(
    label: String,
    count: Int,
    foreground: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(13.dp), color = background) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), color = foreground, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = foreground, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusPill(label: String, foreground: Color, background: Color) {
    Surface(shape = CircleShape, color = background) {
        Text(label, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = foreground, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun EmptyDashboardCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DashCard)
    ) {
        Text(message, modifier = Modifier.padding(16.dp), color = DashMuted, fontSize = 12.sp)
    }
}