package com.cosmotrinity.cosmonavigator.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmotrinity.cosmonavigator.data.DailyHistoryActivityEntity
import com.cosmotrinity.cosmonavigator.data.DailyHistoryRepository
import com.cosmotrinity.cosmonavigator.data.DailyTimetableRepository
import com.cosmotrinity.cosmonavigator.data.DailySleepRecordRepository
import com.cosmotrinity.cosmonavigator.data.DailySleepRecordEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.ZoneId

private val CalendarBlue = Color(0xFF244B79)
private val CalendarGreen = Color(0xFF36B37E)
private val CalendarOrange = Color(0xFFD99A36)
private val CalendarRed = Color(0xFFD9534F)

private enum class DayStatus {
    STUDIED, PARTIAL, MISSED, NONE
}

private val databaseDateFormatter =
    DateTimeFormatter.ofPattern(DailyTimetableRepository.DATE_FORMAT)

private val displayDateFormatter =
    DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")

// Sleep records use ISO dates (yyyy-MM-dd), independently of study history format.
private val sleepDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private val shortDateFormatter =
    DateTimeFormatter.ofPattern("dd MMM")

private fun formatSeconds(seconds: Long): String {
    val safeSeconds = seconds.coerceAtLeast(0L)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60

    return "${hours}h ${minutes}m"
}

private fun getDayStatus(
    date: LocalDate,
    today: LocalDate,
    historyByDate: Map<LocalDate, com.cosmotrinity.cosmonavigator.data.DailyHistoryEntity>
): DayStatus {

    if (date.isAfter(today)) return DayStatus.NONE

    val history = historyByDate[date] ?: return DayStatus.NONE

    return when {
        history.actualStudySeconds > 0L &&
                history.completionPercentage == 100 ->
            DayStatus.STUDIED

        history.actualStudySeconds > 0L ||
                history.completedActivities > 0 ->
            DayStatus.PARTIAL

        history.totalActivities > 0 ->
            DayStatus.MISSED

        else -> DayStatus.NONE
    }
}

@Composable
fun CalendarScreen(
    historyRepository: DailyHistoryRepository,
    sleepRecordRepository: DailySleepRecordRepository
) {

    val today = remember { LocalDate.now() }

    var displayedMonth by remember {
        mutableStateOf(YearMonth.from(today))
    }

    var selectedDate by remember {
        mutableStateOf(today)
    }

    // Compact day navigation is the default; expand to browse the full month.
    var isMonthExpanded by remember { mutableStateOf(false) }

    val selectedDateString = remember(selectedDate) {
        selectedDate.format(databaseDateFormatter)
    }

    // Observe all saved history for monthly indicators.
    val allHistory by historyRepository
        .observeAllHistory()
        .collectAsState(initial = emptyList())

    val historyByDate = remember(allHistory) {
        allHistory.mapNotNull { history ->
            try {
                LocalDate.parse(
                    history.studyDate,
                    databaseDateFormatter
                ) to history
            } catch (_: Exception) {
                null
            }
        }.toMap()
    }

    // Observe selected date study and sleep summaries.
    val dailyHistory by historyRepository
        .observeHistoryForDate(selectedDateString)
        .collectAsState(initial = null)

    val sleepDateString = remember(selectedDate) {
        selectedDate.format(sleepDateFormatter)
    }

    val sleepRecord by sleepRecordRepository
        .observeRecord(sleepDateString)
        .collectAsState(initial = null)

    // Sleep markers are also shown in the expanded monthly calendar.
    val allSleepRecords by sleepRecordRepository
        .observeAllRecords()
        .collectAsState(initial = emptyList())

    val sleepDates = remember(allSleepRecords) {
        allSleepRecords.mapNotNull { record ->
            runCatching { LocalDate.parse(record.recordDate, sleepDateFormatter) }.getOrNull()
        }.toSet()
    }

    // Observe activity snapshots belonging to selected history.
    val historyId = dailyHistory?.id

    val activityFlow = remember(historyId) {
        if (historyId == null) {
            flowOf(emptyList())
        } else {
            historyRepository.observeActivitiesForHistory(historyId)
        }
    }

    val dailyActivities by activityFlow.collectAsState(
        initial = emptyList()
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 20.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Calendar",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                TextButton(
                    onClick = {
                        selectedDate = today
                        displayedMonth = YearMonth.from(today)
                        isMonthExpanded = false
                    }
                ) {
                    Text("Today")
                }
            }
        }

        // MONTH CALENDAR
        item {
            CalendarMonthCard(
                month = displayedMonth,
                selectedDate = selectedDate,
                today = today,
                historyByDate = historyByDate,
                sleepDates = sleepDates,
                expanded = isMonthExpanded,
                onToggleExpanded = { isMonthExpanded = !isMonthExpanded },
                onPrevious = {
                    if (isMonthExpanded) {
                        displayedMonth = displayedMonth.minusMonths(1)
                    } else {
                        selectedDate = selectedDate.minusDays(1)
                        displayedMonth = YearMonth.from(selectedDate)
                    }
                },
                onNext = {
                    if (isMonthExpanded) {
                        displayedMonth = displayedMonth.plusMonths(1)
                    } else {
                        selectedDate = selectedDate.plusDays(1)
                        displayedMonth = YearMonth.from(selectedDate)
                    }
                },
                onDateSelected = { date ->
                    selectedDate = date
                    displayedMonth = YearMonth.from(date)
                }
            )
        }

        // LEGEND
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendItem(CalendarGreen, "Studied")
                LegendItem(CalendarOrange, "Partial")
                LegendItem(CalendarRed, "Missed")
                LegendItem(Color(0xFF4AA9C7), "Sleep")
            }
        }

        // SELECTED DATE
        item {
            Text(
                text = selectedDate.format(displayDateFormatter),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // SLEEP SUMMARY - independent of study history.
        item {
            SleepSummaryCard(
                record = sleepRecord,
                repository = sleepRecordRepository
            )
        }

        // DAILY SUMMARY
        if (dailyHistory != null) {

            val history = dailyHistory!!

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    SummaryCard(
                        title = "Study time",
                        value = formatSeconds(history.actualStudySeconds),
                        modifier = Modifier.weight(1f),
                        valueColor = CalendarGreen
                    )

                    SummaryCard(
                        title = "Daily target",
                        value = formatSeconds(history.targetStudySeconds),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    SummaryCard(
                        title = "Completed",
                        value = "${history.completedActivities} / ${history.totalActivities}",
                        modifier = Modifier.weight(1f)
                    )

                    SummaryCard(
                        title = "Completion",
                        value = "${history.completionPercentage}%",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ACTIVITY HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Daily activities",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = selectedDate.format(shortDateFormatter),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }

            // ACTIVITY LIST
            items(
                items = dailyActivities,
                key = { it.id }
            ) { activity ->
                ActivityHistoryCard(activity)
            }

            // EXTRA DAILY STATISTICS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryCard(
                        title = "Missed",
                        value = history.missedActivities.toString(),
                        modifier = Modifier.weight(1f),
                        valueColor = CalendarRed
                    )

                    SummaryCard(
                        title = "Pending",
                        value = history.pendingActivities.toString(),
                        modifier = Modifier.weight(1f),
                        valueColor = CalendarOrange
                    )
                }
            }

        } else {

            // NO HISTORY
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.5f
                        )
                    )
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        Text(
                            text = "No study records yet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Your saved timetable and study progress for this date will appear here once a daily history snapshot is created.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Calendar data is loaded from your saved daily history.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalendarMonthCard(
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    historyByDate: Map<LocalDate, com.cosmotrinity.cosmonavigator.data.DailyHistoryEntity>,
    sleepDates: Set<LocalDate>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(if (expanded) 14.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (expanded) 12.dp else 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (expanded) {
                            month.month.name.lowercase().replaceFirstChar { it.uppercase() } + " ${month.year}"
                        } else "Selected day",
                        fontSize = if (expanded) 18.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!expanded) {
                        Text(
                            text = selectedDate.format(displayDateFormatter),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (!expanded) {
                    IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        MonthButton("‹", onPrevious)
                        MonthButton("›", onNext)
                    }
                }

                TextButton(onClick = onToggleExpanded, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Minimize calendar" else "Expand calendar",
                        modifier = Modifier.size(18.dp)
                    )
                    Text(if (expanded) "Less" else "Month", fontSize = 12.sp)
                }
            }

            if (expanded) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                val firstDayOffset = month.atDay(1).dayOfWeek.value - 1
                val totalCells = ((firstDayOffset + month.lengthOfMonth() + 6) / 7) * 7
                for (week in 0 until totalCells / 7) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (day in 0..6) {
                            val cell = week * 7 + day
                            val dayNumber = cell - firstDayOffset + 1
                            Box(
                                modifier = Modifier.weight(1f).aspectRatio(1.25f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNumber in 1..month.lengthOfMonth()) {
                                    val date = month.atDay(dayNumber)
                                    val isSelected = date == selectedDate
                                    val isToday = date == today
                                    val status = getDayStatus(date, today, historyByDate)
                                    Column(
                                        modifier = Modifier.fillMaxSize().clickable { onDateSelected(date) },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier.size(30.dp).background(
                                                color = if (isSelected) CalendarBlue else Color.Transparent,
                                                shape = CircleShape
                                            ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayNumber.toString(),
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                        }
                                        // Show exactly one indicator below each date.
                                        // Sleep takes priority; otherwise show study status.
                                        val statusColor = when (status) {
                                            DayStatus.STUDIED -> CalendarGreen
                                            DayStatus.PARTIAL -> CalendarOrange
                                            DayStatus.MISSED -> CalendarRed
                                            DayStatus.NONE -> Color.Transparent
                                        }
                                        val indicatorColor = if (date in sleepDates) {
                                            Color(0xFF4AA9C7)
                                        } else {
                                            statusColor
                                        }
                                        if (indicatorColor != Color.Transparent) {
                                            Box(
                                                Modifier
                                                    .padding(top = 2.dp)
                                                    .size(4.dp)
                                                    .background(indicatorColor, CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun SleepSummaryCard(
    record: DailySleepRecordEntity?,
    repository: DailySleepRecordRepository
) {
    val duration = record?.let {
        repository.calculateSleepDurationMinutes(it)
    }

    val durationText = repository.formatSleepDuration(duration)

    val zoneId = remember { ZoneId.systemDefault() }

    val bedtimeDateTime = record?.actualSleepAt?.let {
        Instant.ofEpochMilli(it).atZone(zoneId)
    }

    val wakeUpDateTime = record?.actualWakeUpAt?.let {
        Instant.ofEpochMilli(it).atZone(zoneId)
    }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy")
    }

    val bedtime = bedtimeDateTime?.format(
        DateTimeFormatter.ofPattern("hh:mm a")
    ) ?: "—"

    val wakeUp = wakeUpDateTime?.format(
        DateTimeFormatter.ofPattern("hh:mm a")
    ) ?: "—"

    val bedtimeDate = bedtimeDateTime?.format(dateFormatter) ?: "—"
    val wakeUpDate = wakeUpDateTime?.format(dateFormatter) ?: "—"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Sleep",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = durationText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SleepTimeValue(
                    modifier = Modifier.weight(1f),
                    label = "Wake-up",
                    value = wakeUp,
                    date = wakeUpDate,
                    icon = {
                        Icon(
                            Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = Color(0xFFE5A943),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                SleepTimeValue(
                    modifier = Modifier.weight(1f),
                    label = "Bedtime",
                    value = bedtime,
                    date = bedtimeDate,
                    icon = {
                        Icon(
                            Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = Color(0xFF7886D8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            if (record == null) {
                Text(
                    text = "No sleep record saved for this date.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SleepTimeValue(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    date: String,
    icon: @Composable () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        icon()

        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = date,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun MonthButton(
    text: String,
    onClick: () -> Unit
) {

    Surface(
        modifier = Modifier
            .size(36.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 23.sp
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )

        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.55f
            )
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun ActivityHistoryCard(
    activity: DailyHistoryActivityEntity
) {

    val statusColor = when (activity.status) {
        "DONE" -> CalendarGreen
        "NOT_DONE" -> CalendarRed
        else -> CalendarOrange
    }

    val statusLabel = when (activity.status) {
        "DONE" -> "Completed"
        "NOT_DONE" -> "Missed"
        else -> "Pending"
    }

    val plannedDuration =
        formatSeconds(activity.scheduledDurationSeconds)

    val actualDuration =
        formatSeconds(activity.actualStudySeconds)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.45f
            )
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                Text(
                    text = activity.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )

                Text(
                    text = if (activity.countsAsStudy) {
                        "Actual: $actualDuration  •  Planned: $plannedDuration"
                    } else {
                        "Scheduled: $plannedDuration"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${DailyTimetableRepository.formatTime(activity.startTimeMinutes)} - " +
                            DailyTimetableRepository.formatTime(
                                activity.endTimeMinutes
                            ),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = statusColor.copy(alpha = 0.13f)
            ) {

                Text(
                    text = statusLabel,
                    modifier = Modifier.padding(
                        horizontal = 11.dp,
                        vertical = 6.dp
                    ),
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

