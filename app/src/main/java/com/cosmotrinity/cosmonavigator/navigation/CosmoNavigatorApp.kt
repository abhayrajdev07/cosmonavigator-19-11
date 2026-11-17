
package com.cosmotrinity.cosmonavigator.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp

import com.cosmotrinity.cosmonavigator.data.CosmoDatabase
import com.cosmotrinity.cosmonavigator.data.DataStoreManager
import com.cosmotrinity.cosmonavigator.data.DailyHistoryRepository
import com.cosmotrinity.cosmonavigator.data.DailyTimetableRepository
import com.cosmotrinity.cosmonavigator.data.UpscLectureRepository
import com.cosmotrinity.cosmonavigator.data.UpscSubjectRepository
import com.cosmotrinity.cosmonavigator.ui.calendar.CalendarScreen
import com.cosmotrinity.cosmonavigator.ui.components.CosmoBottomBar
import com.cosmotrinity.cosmonavigator.ui.dashboard.DashboardScreen
import com.cosmotrinity.cosmonavigator.ui.more.MoreScreen
import com.cosmotrinity.cosmonavigator.ui.more.ProfileScreen
import com.cosmotrinity.cosmonavigator.ui.planner.PlannerScreen
import com.cosmotrinity.cosmonavigator.ui.planner.StudyTaskViewModelFactory
import com.cosmotrinity.cosmonavigator.data.DailySleepRecordRepository
import com.cosmotrinity.cosmonavigator.ui.progress.ProgressScreen

private const val SWIPE_THRESHOLD = 100f

@Composable
fun CosmoNavigatorApp(
    dataStore: DataStoreManager,
    taskViewModelFactory: StudyTaskViewModelFactory
) {

    val context = LocalContext.current.applicationContext

    // DATABASE
    val database = remember {
        CosmoDatabase.getDatabase(context)
    }

    // EXISTING REPOSITORIES
    val subjectRepository = remember(database) {
        UpscSubjectRepository(database.upscSubjectDao())
    }

    val lectureRepository = remember(database) {
        UpscLectureRepository(database.upscLectureDao())
    }

    // DAILY HISTORY REPOSITORY
    val dailyHistoryRepository = remember(database) {
        DailyHistoryRepository(
            historyDao = database.dailyHistoryDao(),
            timetableDao = database.dailyTimetableDao()
        )
    }

    // DAILY TIMETABLE REPOSITORY
    val timetableRepository = remember(
        database,
        dailyHistoryRepository
    ) {
        DailyTimetableRepository(
            dao = database.dailyTimetableDao(),
            historyRepository = dailyHistoryRepository
        )
    }

    // WAKE & SLEEP TRACKER REPOSITORY
    val sleepRecordRepository = remember(database) {
        DailySleepRecordRepository(
            dao = database.dailySleepRecordDao()
        )
    }

    // CAPTURE TODAY'S HISTORY WHEN THE APP OPENS
    LaunchedEffect(dailyHistoryRepository) {
        runCatching {
            dailyHistoryRepository.captureDailyHistory(
                DailyTimetableRepository.currentDate()
            )
        }
    }

    // NAVIGATION STATE
    var selectedDestination by rememberSaveable {
        mutableStateOf(AppDestination.HOME)
    }

    var showProfile by rememberSaveable {
        mutableStateOf(false)
    }

    val destinations = AppDestination.entries

    BackHandler(enabled = showProfile) {
        showProfile = false
    }

    Scaffold(
        bottomBar = {
            CosmoBottomBar(
                selectedDestination = selectedDestination,
                onDestinationSelected = {
                    selectedDestination = it
                    showProfile = false
                }
            )
        }
    ) { innerPadding ->

        var dragAmount by remember {
            mutableFloatStateOf(0f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(
                    selectedDestination,
                    showProfile
                ) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, drag ->
                            dragAmount += drag
                        },
                        onDragEnd = {

                            if (!showProfile) {

                                val index = destinations.indexOf(
                                    selectedDestination
                                )

                                if (
                                    dragAmount < -SWIPE_THRESHOLD &&
                                    index < destinations.lastIndex
                                ) {
                                    selectedDestination =
                                        destinations[index + 1]

                                } else if (
                                    dragAmount > SWIPE_THRESHOLD &&
                                    index > 0
                                ) {
                                    selectedDestination =
                                        destinations[index - 1]
                                }
                            }

                            dragAmount = 0f
                        },
                        onDragCancel = {
                            dragAmount = 0f
                        }
                    )
                }
        ) {

            if (showProfile) {

                ProfileScreen(
                    dataStore = dataStore,
                    onBack = {
                        showProfile = false
                    }
                )

            } else {

                DestinationContent(
                    destination = selectedDestination,
                    dataStore = dataStore,
                    taskViewModelFactory = taskViewModelFactory,
                    subjectRepository = subjectRepository,
                    lectureRepository = lectureRepository,
                    timetableRepository = timetableRepository,
                    dailyHistoryRepository = dailyHistoryRepository,
                    sleepRecordRepository = sleepRecordRepository,
                    onProfileClick = {
                        showProfile = true
                    }
                )
            }
        }
    }
}

@Composable
private fun DestinationContent(
    destination: AppDestination,
    dataStore: DataStoreManager,
    taskViewModelFactory: StudyTaskViewModelFactory,
    subjectRepository: UpscSubjectRepository,
    lectureRepository: UpscLectureRepository,
    timetableRepository: DailyTimetableRepository,
    dailyHistoryRepository: DailyHistoryRepository,
    sleepRecordRepository: DailySleepRecordRepository,
    onProfileClick: () -> Unit
) {

    when (destination) {

        AppDestination.HOME -> {
            DashboardScreen(
                dataStore = dataStore,
                timetableRepository = timetableRepository,
                sleepRecordRepository = sleepRecordRepository
            )
        }

        AppDestination.PLANNER -> {
            PlannerScreen(
                taskViewModelFactory = taskViewModelFactory,
                dataStore = dataStore,
                subjectRepository = subjectRepository,
                lectureRepository = lectureRepository
            )
        }

        AppDestination.CALENDAR -> {
            CalendarScreen(
                historyRepository = dailyHistoryRepository,
                sleepRecordRepository = sleepRecordRepository
            )
        }



        AppDestination.MORE -> {
            MoreScreen(
                dataStore = dataStore,
                onProfileClick = onProfileClick
            )
        }

        AppDestination.PROGRESS -> {
            ProgressScreen(
                dataStore = dataStore,
                subjectRepository = subjectRepository,
                lectureRepository = lectureRepository
            )
        }

        else -> {
            // Keep your existing fallback here.
        }
    }
}
