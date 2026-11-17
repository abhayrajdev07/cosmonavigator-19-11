
package com.cosmotrinity.cosmonavigator.ui.progress

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmotrinity.cosmonavigator.data.DataStoreManager
import com.cosmotrinity.cosmonavigator.data.UpscSubjectEntity
import com.cosmotrinity.cosmonavigator.data.UpscSubjectRepository
import com.cosmotrinity.cosmonavigator.data.UpscLectureRepository

private enum class SubjectGroup {
    COMPLETED,
    ONGOING,
    NOT_STARTED
}

@Composable
fun ProgressScreen(
    dataStore: DataStoreManager,
    subjectRepository: UpscSubjectRepository,
    lectureRepository: UpscLectureRepository
){
    val goals by dataStore.goalsFlow.collectAsState(
        initial = com.cosmotrinity.cosmonavigator.data.StoredAspirantGoals()
    )

    val subjects by subjectRepository
        .getSubjectsByYear(goals.targetExamYear)
        .collectAsState(initial = emptyList())

    val completedSubjects = subjects.filter {
        getSubjectGroup(it) == SubjectGroup.COMPLETED
    }

    val ongoingSubjects = subjects.filter {
        getSubjectGroup(it) == SubjectGroup.ONGOING
    }

    val notStartedSubjects = subjects.filter {
        getSubjectGroup(it) == SubjectGroup.NOT_STARTED
    }

    val totalLectures = subjects.sumOf { it.totalLectures }
    val completedLectures = subjects.sumOf {
        it.completedLectures.coerceAtMost(it.totalLectures)
    }
    val pendingLectures = (totalLectures - completedLectures).coerceAtLeast(0)

    val overallProgress = if (totalLectures > 0) {
        completedLectures.toFloat() / totalLectures
    } else {
        0f
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 20.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            ProgressHeader(
                examYear = goals.targetExamYear
            )
        }

        item {
            OverallProgressCard(
                progress = overallProgress,
                completedLectures = completedLectures,
                totalLectures = totalLectures
            )
        }

        item {
            StatisticsGrid(
                totalSubjects = subjects.size,
                completedSubjects = completedSubjects.size,
                ongoingSubjects = ongoingSubjects.size,
                pendingLectures = pendingLectures
            )
        }

        item {
            LectureAnalyticsSection(
                subjects = subjects,
                lectureRepository = lectureRepository
            )
        }


    }
}

private fun getSubjectGroup(
    subject: UpscSubjectEntity
): SubjectGroup {
    if (subject.status.equals("Completed", ignoreCase = true)) {
        return SubjectGroup.COMPLETED
    }

    val status = subject.status.trim().lowercase()

    val explicitlyOngoing = status in listOf(
        "ongoing",
        "in progress",
        "started",
        "active"
    )

    val hasProgress =
        subject.completedLectures > 0 ||
                subject.completedTopics > 0 ||
                subject.startDate.isNotBlank()

    return if (explicitlyOngoing || hasProgress) {
        SubjectGroup.ONGOING
    } else {
        SubjectGroup.NOT_STARTED
    }
}

@Composable
private fun ProgressHeader(examYear: Int) {
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.BarChart,
                contentDescription = "Progress",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(29.dp)
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = "My Progress",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "UPSC $examYear preparation overview",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun OverallProgressCard(
    progress: Float,
    completedLectures: Int,
    totalLectures: Int
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "overall_progress"
    )

    val percentage = (progress * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "Overall preparation",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "Lecture completion",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                            alpha = 0.75f
                        )
                    )
                }

                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = "Achievement",
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$percentage%",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "$completedLectures / $totalLectures lectures",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                        alpha = 0.8f
                    ),
                    modifier = Modifier.padding(bottom = 7.dp)
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                    alpha = 0.12f
                )
            )

            Text(
                text = if (totalLectures == 0) {
                    "Add your subject lectures to start tracking progress."
                } else {
                    "${totalLectures - completedLectures} lectures remaining"
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun StatisticsGrid(
    totalSubjects: Int,
    completedSubjects: Int,
    ongoingSubjects: Int,
    pendingLectures: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatisticCard(
                title = "Total subjects",
                value = totalSubjects,
                icon = Icons.Filled.School,
                color = Color(0xFF647FEA),
                modifier = Modifier.weight(1f)
            )

            StatisticCard(
                title = "Completed",
                value = completedSubjects,
                icon = Icons.Filled.CheckCircle,
                color = Color(0xFF16A36A),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatisticCard(
                title = "Ongoing",
                value = ongoingSubjects,
                icon = Icons.Filled.Autorenew,
                color = Color(0xFF318CE7),
                modifier = Modifier.weight(1f)
            )

            StatisticCard(
                title = "Pending lectures",
                value = pendingLectures,
                icon = Icons.Filled.PendingActions,
                color = Color(0xFFE49A32),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatisticCard(
    title: String,
    value: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateIntAsState(
        targetValue = value,
        label = "statistic_count"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(23.dp)
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
private fun SectionHeading(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(25.dp)
        )

        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SubjectGroupHeading(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(21.dp)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )

        Spacer(Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(50),
            color = color.copy(alpha = 0.12f)
        ) {
            Text(
                text = count.toString(),
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    horizontal = 9.dp,
                    vertical = 3.dp
                )
            )
        }
    }
}

@Composable
private fun SubjectProgressCard(
    subject: UpscSubjectEntity
) {
    var visible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(
            initialOffsetY = { it / 5 }
        )
    ) {
        val group = getSubjectGroup(subject)

        val statusColor = when (group) {
            SubjectGroup.COMPLETED -> Color(0xFF16A36A)
            SubjectGroup.ONGOING -> Color(0xFF318CE7)
            SubjectGroup.NOT_STARTED -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        val statusText = when (group) {
            SubjectGroup.COMPLETED -> "Completed"
            SubjectGroup.ONGOING -> "Ongoing"
            SubjectGroup.NOT_STARTED -> "Not started"
        }

        val lectureProgress = subject.lectureProgress
        val topicProgress = subject.topicProgress

        val animatedLectureProgress by animateFloatAsState(
            targetValue = lectureProgress,
            label = "subject_lecture_progress"
        )

        val animatedTopicProgress by animateFloatAsState(
            targetValue = topicProgress,
            label = "subject_topic_progress"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = statusColor.copy(alpha = 0.12f)
                    ) {
                        Icon(
                            imageVector = subjectIcon(subject),
                            contentDescription = subject.subjectName,
                            tint = statusColor,
                            modifier = Modifier
                                .padding(11.dp)
                                .size(24.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = subject.subjectName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Text(
                            text = subject.category,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                            )
                        )
                    }
                }

                ProgressLine(
                    title = "Lectures",
                    completed = subject.completedLectures.coerceAtMost(
                        subject.totalLectures
                    ),
                    total = subject.totalLectures,
                    progress = animatedLectureProgress,
                    color = statusColor
                )

                ProgressLine(
                    title = "Topics",
                    completed = subject.completedTopics.coerceAtMost(
                        subject.totalTopics
                    ),
                    total = subject.totalTopics,
                    progress = animatedTopicProgress,
                    color = MaterialTheme.colorScheme.tertiary
                )

                if (subject.targetEndDate.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Event,
                            contentDescription = "Target completion date",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            text = "Target: ${subject.targetEndDate}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressLine(
    title: String,
    completed: Int,
    total: Int,
    progress: Float,
    color: Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "$completed / $total",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private fun subjectIcon(
    subject: UpscSubjectEntity
): ImageVector {
    val name = subject.subjectName.lowercase()
    val category = subject.category.lowercase()

    return when {
        "sociology" in name -> Icons.Filled.Groups
        "history" in name -> Icons.Filled.HistoryEdu
        "geography" in name -> Icons.Filled.Public
        "polity" in name || "governance" in name ->
            Icons.Filled.AccountBalance
        "economy" in name -> Icons.Filled.CurrencyRupee
        "science" in name || "technology" in name ->
            Icons.Filled.Science
        "environment" in name -> Icons.Filled.Eco
        "ethics" in name -> Icons.Filled.Psychology
        "essay" in name -> Icons.Filled.EditNote
        "csat" in name -> Icons.Filled.Calculate
        "current" in name || "affairs" in name ->
            Icons.Filled.Newspaper
        "international" in name || "relations" in name ->
            Icons.Filled.Public
        "disaster" in name -> Icons.Filled.Warning
        "security" in name -> Icons.Filled.Security
        "society" in name -> Icons.Filled.Groups
        "art" in name || "culture" in name ->
            Icons.Filled.Museum
        "gs" in category -> Icons.Filled.School
        else -> Icons.Filled.MenuBook
    }
}

@Composable
private fun LectureOverviewCard(
    total: Int,
    completed: Int,
    pending: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            LectureStatRow(
                title = "Total lectures",
                value = total,
                icon = Icons.Filled.MenuBook,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            LectureStatRow(
                title = "Completed lectures",
                value = completed,
                icon = Icons.Filled.CheckCircle,
                color = Color(0xFF16A36A)
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            LectureStatRow(
                title = "Pending lectures",
                value = pending,
                icon = Icons.Filled.PendingActions,
                color = Color(0xFFE49A32)
            )
        }
    }
}

@Composable
private fun LectureStatRow(
    title: String,
    value: Int,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(23.dp)
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp
        )

        Text(
            text = value.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )
    }
}

@Composable
private fun EmptySubjectsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.School,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "No subjects found",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            Text(
                text = "Your subjects for this UPSC exam year will appear here once added.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ComingSoonCard(
    title: String,
    description: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                        alpha = 0.8f
                    )
                )
            }
        }
    }
}