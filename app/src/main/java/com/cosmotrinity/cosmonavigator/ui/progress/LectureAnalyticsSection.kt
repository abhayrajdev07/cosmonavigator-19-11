package com.cosmotrinity.cosmonavigator.ui.progress

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmotrinity.cosmonavigator.data.UpscLectureEntity
import com.cosmotrinity.cosmonavigator.data.UpscLectureRepository
import com.cosmotrinity.cosmonavigator.data.UpscSubjectEntity
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

private enum class AnalyticsPeriod(val label: String, val days: Int) {
    WEEK("7 days", 7),
    MONTH("30 days", 30),
    YEAR("12 months", 365)
}

private data class LectureAnalytics(
    val completed: Int,
    val pending: Int,
    val overdue: Int,
    val dueToday: Int,
    val upcoming: Int,
    val unscheduled: Int,
    val completionsByDay: List<Int>,
    val completionsInPeriod: Int,
    val averagePerWeek: Float
)

private fun startOfDay(calendar: Calendar): Calendar =
    (calendar.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

private fun parseScheduledDate(value: String): Calendar? {
    if (value.isBlank()) return null

    return try {
        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        formatter.isLenient = false
        val parsed = formatter.parse(value) ?: return null
        startOfDay(Calendar.getInstance().apply { time = parsed })
    } catch (_: Exception) {
        null
    }
}

private fun calculateAnalytics(
    lectures: List<UpscLectureEntity>,
    period: AnalyticsPeriod
): LectureAnalytics {
    val today = startOfDay(Calendar.getInstance()).timeInMillis
    val tomorrow = today + TimeUnit.DAYS.toMillis(1)
    val pendingLectures = lectures.filterNot { it.isCompleted }

    val overdue = pendingLectures.count { lecture ->
        parseScheduledDate(lecture.scheduledDate)?.timeInMillis?.let { it < today } == true
    }
    val dueToday = pendingLectures.count { lecture ->
        parseScheduledDate(lecture.scheduledDate)?.timeInMillis == today
    }
    val upcoming = pendingLectures.count { lecture ->
        parseScheduledDate(lecture.scheduledDate)?.timeInMillis?.let { it >= tomorrow } == true
    }
    val unscheduled = pendingLectures.count {
        parseScheduledDate(it.scheduledDate) == null
    }

    val periodStart = today -
            TimeUnit.DAYS.toMillis((period.days - 1).toLong())

    val completedInPeriod = lectures.count { lecture ->
        lecture.isCompleted && lecture.completedAt?.let {
            it >= periodStart && it < tomorrow
        } == true
    }

    val dailyCounts = (0 until 7).map { offset ->
        val dayStart = today - TimeUnit.DAYS.toMillis((6 - offset).toLong())
        val dayEnd = dayStart + TimeUnit.DAYS.toMillis(1)
        lectures.count { lecture ->
            lecture.isCompleted && lecture.completedAt?.let {
                it >= dayStart && it < dayEnd
            } == true
        }
    }

    val completedWithTimestamp = lectures.filter {
        it.isCompleted && it.completedAt != null
    }
    val firstCompletion = completedWithTimestamp.minOfOrNull { it.completedAt!! }
    val elapsedWeeks = firstCompletion?.let {
        ((today - it).toFloat() / TimeUnit.DAYS.toMillis(7).toFloat())
            .coerceAtLeast(1f)
    } ?: 0f
    val averagePerWeek = if (elapsedWeeks > 0f) {
        completedWithTimestamp.size / elapsedWeeks
    } else {
        0f
    }

    return LectureAnalytics(
        completed = lectures.count { it.isCompleted },
        pending = pendingLectures.size,
        overdue = overdue,
        dueToday = dueToday,
        upcoming = upcoming,
        unscheduled = unscheduled,
        completionsByDay = dailyCounts,
        completionsInPeriod = completedInPeriod,
        averagePerWeek = averagePerWeek
    )
}

private fun subjectStatus(subject: UpscSubjectEntity): String {
    val status = subject.status.trim().lowercase(Locale.ROOT)
    val completed = status == "completed" ||
            (subject.totalLectures > 0 &&
                    subject.completedLectures >= subject.totalLectures)

    if (completed) return "Completed"

    val started = subject.completedLectures > 0 ||
            subject.completedTopics > 0 ||
            subject.startDate.isNotBlank() ||
            status in listOf("ongoing", "in progress", "started", "active")

    return if (started) "Ongoing" else "Not started"
}

private fun categoryOrder(category: String): Int {
    val normalized = category.lowercase(Locale.ROOT)
        .filter { it.isLetterOrDigit() }

    return when {
        normalized.contains("gspaperi") || normalized == "gsi" ||
                normalized == "gs1" -> 0
        normalized.contains("gspaperii") || normalized == "gsii" ||
                normalized == "gs2" -> 1
        normalized.contains("gspaperiii") || normalized == "gsiii" ||
                normalized == "gs3" -> 2
        normalized.contains("gspaperiv") || normalized == "gsiv" ||
                normalized == "gs4" -> 3
        normalized.contains("currentaffair") -> 4
        normalized.contains("csat") -> 5
        normalized.contains("essay") -> 6
        normalized.contains("optional") || normalized.contains("sociology") -> 7
        normalized.contains("language") -> 8
        normalized.contains("interview") || normalized.contains("personality") -> 9
        else -> 10
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LectureAnalyticsSection(
    subjects: List<UpscSubjectEntity>,
    lectureRepository: UpscLectureRepository
) {
    var selectedPeriod by remember { mutableStateOf(AnalyticsPeriod.WEEK) }

    val lectureFlows = remember(subjects, lectureRepository) {
        subjects.map { lectureRepository.getLecturesBySubject(it.id) }
    }

    val lecturesFlow = remember(lectureFlows) {
        if (lectureFlows.isEmpty()) {
            flowOf(emptyList<UpscLectureEntity>())
        } else {
            combine(lectureFlows) { lists -> lists.flatMap { it } }
        }
    }

    val allLectures by lecturesFlow.collectAsState(initial = emptyList())
    val analytics = remember(allLectures, selectedPeriod) {
        calculateAnalytics(allLectures, selectedPeriod)
    }
    val locale = remember { Locale.getDefault() }
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "Lecture analytics",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your recorded lecture progress",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnalyticsMetricCard(
                modifier = Modifier.weight(1f),
                title = "Completed",
                value = analytics.completed,
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF16A36A)
            )
            AnalyticsMetricCard(
                modifier = Modifier.weight(1f),
                title = "Pending",
                value = analytics.pending,
                icon = Icons.Default.PendingActions,
                color = Color(0xFFE49A32)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnalyticsMetricCard(
                modifier = Modifier.weight(1f),
                title = "Overdue",
                value = analytics.overdue,
                icon = Icons.Default.Warning,
                color = Color(0xFFD95757)
            )
            AnalyticsMetricCard(
                modifier = Modifier.weight(1f),
                title = "Upcoming",
                value = analytics.upcoming,
                icon = Icons.Default.Event,
                color = Color(0xFF318CE7)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(15.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Due today",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = analytics.dueToday.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Unscheduled",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = analytics.unscheduled.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = "Completion trends",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            AnalyticsPeriod.values().forEachIndexed { index, period ->
                SegmentedButton(
                    selected = selectedPeriod == period,
                    onClick = { selectedPeriod = period },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = AnalyticsPeriod.values().size
                    )
                ) {
                    Text(text = period.label, fontSize = 11.sp)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${analytics.completionsInPeriod} completions",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Recorded in the selected period",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    text = "Daily completions — last 7 days",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                WeeklyCompletionChart(analytics.completionsByDay)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Average completion pace",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = String.format(
                        locale,
                        "%.1f lectures / week",
                        analytics.averagePerWeek
                    ),
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )

                val estimatedWeeks = if (analytics.averagePerWeek > 0f) {
                    ceil(analytics.pending / analytics.averagePerWeek).toInt()
                } else {
                    null
                }

                val estimateText = when {
                    analytics.pending == 0 ->
                        "All recorded lectures are completed."
                    estimatedWeeks == null ->
                        "Complete more lectures to calculate an estimated finish date."
                    else -> {
                        val finish = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, estimatedWeeks * 7)
                        }
                        val formatted = SimpleDateFormat(
                            "dd MMM yyyy",
                            locale
                        ).format(finish.time)
                        "At this pace, your current backlog may finish around $formatted."
                    }
                }

                Text(
                    text = estimateText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        Text(
            text = "UPSC category-wise progress",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        val categories = subjects
            .groupBy { it.category.ifBlank { "Other" } }
            .toList()
            .sortedWith(
                compareBy<Pair<String, List<UpscSubjectEntity>>> {
                    categoryOrder(it.first)
                }.thenBy { it.first.lowercase(locale) }
            )

        if (categories.isEmpty()) {
            Text(
                text = "No subjects added yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        } else {
            categories.forEach { (category, categorySubjects) ->
                key(category) {
                    val completedCount = categorySubjects.count {
                        subjectStatus(it) == "Completed"
                    }
                    val ongoingCount = categorySubjects.count {
                        subjectStatus(it) == "Ongoing"
                    }
                    val notStartedCount = categorySubjects.size -
                            completedCount - ongoingCount
                    val expanded = expandedCategories[category] ?: false

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedCategories[category] = !expanded
                                    }
                                    .padding(15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = category,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = buildString {
                                            if (completedCount > 0) {
                                                append("$completedCount completed")
                                            }
                                            if (ongoingCount > 0) {
                                                if (isNotEmpty()) append(" • ")
                                                append("$ongoingCount ongoing")
                                            }
                                            if (notStartedCount > 0) {
                                                if (isNotEmpty()) append(" • ")
                                                append("$notStartedCount not started")
                                            }
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "$completedCount/${categorySubjects.size}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(6.dp))
                                androidx.compose.material3.Icon(
                                    imageVector = if (expanded) {
                                        Icons.Default.ExpandLess
                                    } else {
                                        Icons.Default.ExpandMore
                                    },
                                    contentDescription = if (expanded) {
                                        "Collapse category"
                                    } else {
                                        "Expand category"
                                    },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (expanded) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )

                                categorySubjects.forEach { subject ->
                                    val total = subject.totalLectures.coerceAtLeast(0)
                                    val completed = subject.completedLectures
                                        .coerceIn(0, total)
                                    val progress = if (total > 0) {
                                        completed.toFloat() / total.toFloat()
                                    } else {
                                        0f
                                    }
                                    val status = subjectStatus(subject)
                                    val statusColor = when (status) {
                                        "Completed" -> Color(0xFF16A36A)
                                        "Ongoing" -> Color(0xFF318CE7)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 15.dp,
                                                vertical = 8.dp
                                            ),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = subject.subjectName,
                                                modifier = Modifier.weight(1f),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = status,
                                                color = statusColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "$completed/$total",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(3.dp),
                                            color = statusColor,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
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

@Composable
private fun AnalyticsMetricCard(
    modifier: Modifier,
    title: String,
    value: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    val animatedValue by animateIntAsState(
        targetValue = value,
        label = title
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color
            )
            Text(
                text = animatedValue.toString(),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyCompletionChart(values: List<Int>) {
    val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val labels = listOf("6d", "5d", "4d", "3d", "2d", "1d", "Today")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { index, value ->
            val animatedHeight by animateFloatAsState(
                targetValue = value.toFloat() / maxValue,
                label = "weekly_bar_$index"
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = value.toString(),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height((90f * animatedHeight).dp.coerceAtLeast(4.dp))
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(
                                topStart = 6.dp,
                                topEnd = 6.dp
                            )
                        )
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = labels.getOrElse(index) { "" },
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
