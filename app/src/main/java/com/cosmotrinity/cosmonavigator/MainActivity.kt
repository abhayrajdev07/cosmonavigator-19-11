
package com.cosmotrinity.cosmonavigator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cosmotrinity.cosmonavigator.data.CosmoDatabase
import com.cosmotrinity.cosmonavigator.data.DataStoreManager
import com.cosmotrinity.cosmonavigator.data.StudyTaskRepository
import com.cosmotrinity.cosmonavigator.data.StoredAspirantGoals
import com.cosmotrinity.cosmonavigator.data.StoredAspirantProfile
import com.cosmotrinity.cosmonavigator.navigation.CosmoNavigatorApp
import com.cosmotrinity.cosmonavigator.ui.onboarding.AspirantGoalsInput
import com.cosmotrinity.cosmonavigator.ui.onboarding.AspirantProfileInput
import com.cosmotrinity.cosmonavigator.ui.onboarding.OnboardingReviewScreen
import com.cosmotrinity.cosmonavigator.ui.onboarding.ProfileSetupScreen
import com.cosmotrinity.cosmonavigator.ui.onboarding.SplashScreen
import com.cosmotrinity.cosmonavigator.ui.onboarding.UPSCGoalsScreen
import com.cosmotrinity.cosmonavigator.ui.onboarding.WelcomeScreen
import com.cosmotrinity.cosmonavigator.ui.planner.StudyTaskViewModelFactory
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicBackground
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicTextPrimary
import com.cosmotrinity.cosmonavigator.ui.theme.CosmoNavigatorTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class LaunchScreen {
    WELCOME,
    PROFILE,
    UPSC_GOALS,
    REVIEW,
    APP
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            CosmoNavigatorTheme {

                // DataStore initialization
                val dataStore = remember {
                    DataStoreManager(applicationContext)
                }

                // Room database initialization
                val taskViewModelFactory = remember {
                    val database = CosmoDatabase.getDatabase(
                        applicationContext
                    )

                    val repository = StudyTaskRepository(
                        database.studyTaskDao()
                    )

                    StudyTaskViewModelFactory(repository)
                }

                val coroutineScope = rememberCoroutineScope()

                var currentScreen by rememberSaveable {
                    mutableStateOf(LaunchScreen.WELCOME)
                }

                var aspirantProfile by remember {
                    mutableStateOf<AspirantProfileInput?>(null)
                }

                var aspirantGoals by remember {
                    mutableStateOf<AspirantGoalsInput?>(null)
                }

                // Splash is shown on every fresh app launch.
                var isSplashFinished by remember {
                    mutableStateOf(false)
                }

                var isStorageLoaded by remember {
                    mutableStateOf(false)
                }

                // Restore saved information while splash is visible.
                LaunchedEffect(Unit) {

                    val savedProfile = dataStore.profileFlow.first()
                    val savedGoals = dataStore.goalsFlow.first()

                    val onboardingCompleted =
                        dataStore.onboardingCompletedFlow.first()

                    if (savedProfile.fullName.isNotBlank()) {
                        aspirantProfile = savedProfile.toProfileInput()
                    }

                    aspirantGoals = savedGoals.toGoalsInput()

                    currentScreen = if (onboardingCompleted) {
                        LaunchScreen.APP
                    } else {
                        LaunchScreen.WELCOME
                    }

                    isStorageLoaded = true
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CosmicBackground)
                ) {

                    // Show the custom logo splash until both the
                    // splash animation and storage loading are complete.
                    if (!isSplashFinished || !isStorageLoaded) {

                        SplashScreen {
                            isSplashFinished = true
                        }

                    } else {

                        when (currentScreen) {

                            LaunchScreen.WELCOME -> {
                                WelcomeScreen {
                                    currentScreen = LaunchScreen.PROFILE
                                }
                            }

                            LaunchScreen.PROFILE -> {
                                ProfileSetupScreen(
                                    initialData = aspirantProfile,

                                    onBack = {
                                        currentScreen = LaunchScreen.WELCOME
                                    },

                                    onContinue = { profile ->

                                        aspirantProfile = profile

                                        coroutineScope.launch {
                                            dataStore.saveProfile(
                                                profile.toStoredProfile()
                                            )

                                            currentScreen =
                                                LaunchScreen.UPSC_GOALS
                                        }
                                    }
                                )
                            }

                            LaunchScreen.UPSC_GOALS -> {
                                UPSCGoalsScreen(
                                    initialData = aspirantGoals,

                                    onBack = {
                                        currentScreen = LaunchScreen.PROFILE
                                    },

                                    onFinish = { goals ->

                                        aspirantGoals = goals

                                        coroutineScope.launch {
                                            dataStore.saveGoals(
                                                goals.toStoredGoals()
                                            )

                                            currentScreen =
                                                LaunchScreen.REVIEW
                                        }
                                    }
                                )
                            }

                            LaunchScreen.REVIEW -> {

                                val profile = aspirantProfile
                                val goals = aspirantGoals

                                if (profile != null && goals != null) {

                                    OnboardingReviewScreen(
                                        profile = profile,
                                        goals = goals,

                                        onBack = {
                                            currentScreen =
                                                LaunchScreen.UPSC_GOALS
                                        },

                                        onEditProfile = {
                                            currentScreen =
                                                LaunchScreen.PROFILE
                                        },

                                        onEditGoals = {
                                            currentScreen =
                                                LaunchScreen.UPSC_GOALS
                                        },

                                        onComplete = {

                                            coroutineScope.launch {

                                                dataStore.setOnboardingCompleted(
                                                    true
                                                )

                                                currentScreen =
                                                    LaunchScreen.APP
                                            }
                                        }
                                    )

                                } else {

                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Profile or UPSC goals are missing.\nPlease complete setup again.",
                                            color = CosmicTextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            LaunchScreen.APP -> {
                                CosmoNavigatorApp(
                                    dataStore = dataStore,
                                    taskViewModelFactory = taskViewModelFactory
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Convert DataStore profile into onboarding profile
private fun StoredAspirantProfile.toProfileInput() =
    AspirantProfileInput(
        fullName = fullName,
        language = language,
        preparationStage = preparationStage,
        attemptNumber = attemptNumber.toIntOrNull(),
        profileImageUri = profileImageUri
    )

// Convert onboarding profile into DataStore profile
private fun AspirantProfileInput.toStoredProfile() =
    StoredAspirantProfile(
        fullName = fullName,
        language = language,
        preparationStage = preparationStage,
        attemptNumber = attemptNumber?.toString() ?: "",
        profileImageUri = profileImageUri
    )

// Convert DataStore goals into onboarding goals
private fun StoredAspirantGoals.toGoalsInput() =
    AspirantGoalsInput(
        targetExamYear = targetExamYear,
        prelimsDate = prelimsDate,
        optionalSubject = optionalSubject,
        studyMedium = studyMedium,
        dailyStudyHours = dailyStudyHours,
        preparationStartDate = preparationStartDate
    )

// Convert onboarding goals into DataStore goals
private fun AspirantGoalsInput.toStoredGoals() =
    StoredAspirantGoals(
        targetExamYear = targetExamYear,
        prelimsDate = prelimsDate,
        optionalSubject = optionalSubject,
        studyMedium = studyMedium,
        dailyStudyHours = dailyStudyHours,
        preparationStartDate = preparationStartDate
    )
