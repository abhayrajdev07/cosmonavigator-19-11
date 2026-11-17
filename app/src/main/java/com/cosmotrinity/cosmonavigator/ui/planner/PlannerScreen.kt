package com.cosmotrinity.cosmonavigator.ui.planner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import com.cosmotrinity.cosmonavigator.data.DataStoreManager
import com.cosmotrinity.cosmonavigator.data.StoredAspirantGoals
import com.cosmotrinity.cosmonavigator.data.UpscLectureRepository
import com.cosmotrinity.cosmonavigator.data.UpscSubjectEntity
import com.cosmotrinity.cosmonavigator.data.UpscSubjectRepository
import com.cosmotrinity.cosmonavigator.ui.theme.*
import kotlinx.coroutines.launch

private val categoryIcons = listOf(
    Icons.Default.AccountBalance,
    Icons.Default.Gavel,
    Icons.Default.Public,
    Icons.Default.MenuBook,
    Icons.Default.Psychology,
    Icons.Default.EditNote,
    Icons.Default.School,
    Icons.Default.Newspaper,
    Icons.Default.Translate
)

@Composable
fun PlannerScreen(
    taskViewModelFactory: StudyTaskViewModelFactory,
    dataStore: DataStoreManager,
    subjectRepository: UpscSubjectRepository,
    lectureRepository: UpscLectureRepository
) {
    val goals by dataStore.goalsFlow.collectAsState(
        initial = StoredAspirantGoals()
    )

    val scope = rememberCoroutineScope()

    var selectedCategory by remember {
        mutableStateOf<UpscSubjectCategory?>(null)
    }

    var selectedSubject by remember {
        mutableStateOf<String?>(null)
    }

    var selectedSubjectId by remember {
        mutableIntStateOf(0)
    }

    var showLectureManagement by remember {
        mutableStateOf(false)
    }

    var isCreatingSubject by remember {
        mutableStateOf(false)
    }

    // Selected target UPSC year
    val targetExamYear = goals.targetExamYear

    // Register every syllabus subject for this target year.
    // Existing Room records are preserved by getOrCreateSubject().
    LaunchedEffect(targetExamYear) {
        val syllabusSubjects = UpscSubjectCatalog.categories.flatMap { category ->
            category.subjects.map { subjectName ->
                category.title to subjectName
            }
        }

        subjectRepository.initializeSubjectsForYear(
            year = targetExamYear,
            subjects = syllabusSubjects
        )
    }

    // Handle Android system Back gesture
    BackHandler(
        enabled = showLectureManagement ||
                selectedSubject != null ||
                selectedCategory != null
    ) {
        when {
            showLectureManagement -> {
                showLectureManagement = false
            }

            selectedSubject != null -> {
                selectedSubject = null
                selectedSubjectId = 0
            }

            selectedCategory != null -> {
                selectedCategory = null
            }
        }
    }

    // Screen 1: Lecture Management
    if (
        showLectureManagement &&
        selectedSubject != null
    ) {
        LectureManagementScreen(
            subjectId = selectedSubjectId,
            subjectName = selectedSubject!!,
            lectureRepository = lectureRepository,
            subjectRepository = subjectRepository,
            onBack = {
                showLectureManagement = false
            }
        )
        return
    }

    // Screen 2: Subject Detail
    if (
        selectedSubject != null &&
        selectedCategory != null
    ) {
        SubjectDetailScreen(
            subjectId = selectedSubjectId,
            subjectName = selectedSubject!!,
            category = selectedCategory!!.title,
            subjectRepository = subjectRepository,
            lectureRepository = lectureRepository,
            onBack = {
                selectedSubject = null
                selectedSubjectId = 0
            },
            onManageLectures = {
                showLectureManagement = true
            }
        )
        return
    }

    // Screen 3: Category Detail
    if (selectedCategory != null) {
        CategoryDetailScreen(
            category = selectedCategory!!,
            targetExamYear = targetExamYear,
            subjectRepository = subjectRepository,
            onBack = {
                selectedCategory = null
            },
            onSubjectClick = { subject ->
                scope.launch {
                    isCreatingSubject = true

                    try {
                        val entity = subjectRepository.getOrCreateSubject(
                            year = targetExamYear,
                            category = selectedCategory!!.title,
                            subjectName = subject
                        )

                        selectedSubjectId = entity.id
                        selectedSubject = subject
                    } finally {
                        isCreatingSubject = false
                    }
                }
            }
        )
        return
    }

    // Screen 4: UPSC Path Home
    Box(modifier = Modifier.fillMaxSize()) {
        UPSCPathHomeScreen(
            targetExamYear = targetExamYear,
            subjectRepository = subjectRepository,
            onCategoryClick = {
                selectedCategory = it
            }
        )

        if (isCreatingSubject) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CosmicBackground.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = CosmicPrimary
                )
            }
        }
    }
}

@Composable
private fun UPSCPathHomeScreen(
    targetExamYear: Int,
    subjectRepository: UpscSubjectRepository,
    onCategoryClick: (UpscSubjectCategory) -> Unit
) {
    val savedSubjects by subjectRepository
        .getSubjectsByYear(targetExamYear)
        .collectAsState(initial = emptyList())

    val totalSubjects = UpscSubjectCatalog.categories.sumOf { it.subjects.size }

    // Overall completion is measured by fully completed subjects.
    // Lecture counts determine whether a saved subject is completed or in progress.
    val completedSubjects = savedSubjects.count { subject ->
        subject.totalLectures > 0 &&
                subject.completedLectures >= subject.totalLectures
    }

    val inProgressSubjects = savedSubjects.count { subject ->
        subject.totalLectures > 0 &&
                subject.completedLectures < subject.totalLectures
    }

    val pendingLectures = savedSubjects.sumOf { subject ->
        (subject.totalLectures - subject.completedLectures).coerceAtLeast(0)
    }

    val overallProgress = if (totalSubjects > 0) {
        completedSubjects.toFloat() / totalSubjects.toFloat()
    } else {
        0f
    }

    val overallPercent = (overallProgress * 100).toInt()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    "UPSC Path",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CosmicTextPrimary
                )

                Text(
                    "Your complete UPSC $targetExamYear preparation journey",
                    color = CosmicTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CosmicSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Overall Preparation",
                        color = CosmicTextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "$overallPercent%",
                            color = CosmicPrimary,
                            style = MaterialTheme.typography.headlineLarge
                        )

                        Text(
                            "$completedSubjects / $totalSubjects subjects completed",
                            color = CosmicTextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    LinearProgressIndicator(
                        progress = { overallProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = CosmicPrimary,
                        trackColor = CosmicSurfaceVariant
                    )

                    Text(
                        "$completedSubjects of $totalSubjects subjects fully completed. Subject completion is calculated from lecture records.",
                        color = CosmicTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    HorizontalDivider(
                        color = CosmicSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OverviewStat(completedSubjects.toString(), "Completed")
                        OverviewStat(inProgressSubjects.toString(), "In progress")
                        OverviewStat(pendingLectures.toString(), "Pending lectures")
                    }
                }
            }
        }

        item {
            Text(
                "UPSC Subject Categories",
                style = MaterialTheme.typography.titleLarge,
                color = CosmicTextPrimary
            )
        }

        items(UpscSubjectCatalog.categories) { category ->
            val index = UpscSubjectCatalog.categories.indexOf(category)

            SubjectCategoryCard(
                category = category,
                icon = categoryIcons.getOrElse(index) {
                    Icons.Default.MenuBook
                },
                onClick = {
                    onCategoryClick(category)
                }
            )
        }
    }
}

@Composable
private fun OverviewStat(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            value,
            color = CosmicTextPrimary,
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            label,
            color = CosmicTextSecondary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun SubjectCategoryCard(
    category: UpscSubjectCategory,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = CosmicSurface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = CosmicPrimary,
                modifier = Modifier.size(28.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    category.title,
                    color = CosmicTextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    category.description,
                    color = CosmicTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    "${category.subjects.size} subjects / areas",
                    color = CosmicSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open category",
                tint = CosmicTextSecondary
            )
        }
    }
}

@Composable
private fun CategoryDetailScreen(
    category: UpscSubjectCategory,
    targetExamYear: Int,
    subjectRepository: UpscSubjectRepository,
    onBack: () -> Unit,
    onSubjectClick: (String) -> Unit
) {
    val savedSubjects by subjectRepository
        .getSubjectsByCategory(targetExamYear, category.title)
        .collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = CosmicTextPrimary
                )
            }

            Column {
                Text(
                    category.title,
                    color = CosmicTextPrimary,
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    category.description,
                    color = CosmicTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text(
            "Subjects",
            color = CosmicTextPrimary,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(category.subjects) { subject ->
                val savedSubject = savedSubjects.firstOrNull {
                    it.subjectName == subject
                }

                SubjectProgressCard(
                    subjectName = subject,
                    subject = savedSubject,
                    onClick = {
                        onSubjectClick(subject)
                    }
                )
            }
        }
    }
}

@Composable
private fun SubjectProgressCard(
    subjectName: String,
    subject: UpscSubjectEntity?,
    onClick: () -> Unit
) {
    val totalLectures = subject?.totalLectures ?: 0
    val completedLectures = subject?.completedLectures ?: 0

    val progress = if (totalLectures > 0) {
        (completedLectures.toFloat() / totalLectures.toFloat())
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    val progressPercent = (progress * 100).toInt()
    val status = subject?.status ?: "Not Started"

    val statusColor = when (status) {
        "Completed" -> Color(0xFF55D6A0)
        "In Progress" -> Color(0xFFFFC857)
        "On Hold" -> Color(0xFFE9A2D0)
        else -> Color(0xFF75B8FF)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CosmicSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    subjectName,
                    modifier = Modifier.weight(1f),
                    color = CosmicTextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    "$progressPercent%",
                    color = statusColor,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.width(8.dp))

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Open subject",
                    tint = CosmicTextSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.13f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        status,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 3.dp
                        ),
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    if (totalLectures > 0) {
                        "$completedLectures / $totalLectures lectures"
                    } else {
                        "No lectures added"
                    },
                    color = CosmicTextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp),
                color = statusColor,
                trackColor = CosmicSurfaceVariant
            )
        }
    }
}
