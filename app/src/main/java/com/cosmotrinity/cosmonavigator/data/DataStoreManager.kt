
package com.cosmotrinity.cosmonavigator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cosmoDataStore by preferencesDataStore(
    name = "cosmonavigator_preferences"
)

data class StoredAspirantProfile(
    val fullName: String = "",
    val language: String = "English",
    val preparationStage: String = "Beginner",
    val attemptNumber: String = "",
    val profileImageUri: String? = null
)

data class StoredAspirantGoals(
    val targetExamYear: Int = 2027,
    val prelimsDate: String = "23-05-2027",
    val optionalSubject: String = "Not decided yet",
    val studyMedium: String = "English",
    val dailyStudyHours: Int = 8,
    val preparationStartDate: String = "08-06-2026"
)

class DataStoreManager(context: Context) {

    private val dataStore: DataStore<Preferences> =
        context.applicationContext.cosmoDataStore

    private object Keys {
        val FULL_NAME = stringPreferencesKey("full_name")
        val LANGUAGE = stringPreferencesKey("language")
        val PREPARATION_STAGE = stringPreferencesKey("preparation_stage")
        val ATTEMPT_NUMBER = stringPreferencesKey("attempt_number")
        val PROFILE_IMAGE_URI = stringPreferencesKey("profile_image_uri")

        val TARGET_EXAM_YEAR = intPreferencesKey("target_exam_year")
        val PRELIMS_DATE = stringPreferencesKey("prelims_date")
        val OPTIONAL_SUBJECT = stringPreferencesKey("optional_subject")
        val STUDY_MEDIUM = stringPreferencesKey("study_medium")
        val DAILY_STUDY_HOURS = intPreferencesKey("daily_study_hours")
        val PREPARATION_START_DATE =
            stringPreferencesKey("preparation_start_date")

        val ONBOARDING_COMPLETED =
            booleanPreferencesKey("onboarding_completed")
    }

    // Save aspirant profile
    suspend fun saveProfile(profile: StoredAspirantProfile) {
        dataStore.edit { preferences ->
            preferences[Keys.FULL_NAME] = profile.fullName
            preferences[Keys.LANGUAGE] = profile.language
            preferences[Keys.PREPARATION_STAGE] = profile.preparationStage
            preferences[Keys.ATTEMPT_NUMBER] = profile.attemptNumber

            if (profile.profileImageUri != null) {
                preferences[Keys.PROFILE_IMAGE_URI] =
                    profile.profileImageUri
            } else {
                preferences.remove(Keys.PROFILE_IMAGE_URI)
            }
        }
    }

    // Read aspirant profile
    val profileFlow: Flow<StoredAspirantProfile> =
        dataStore.data.map { preferences ->
            StoredAspirantProfile(
                fullName = preferences[Keys.FULL_NAME] ?: "",
                language = preferences[Keys.LANGUAGE] ?: "English",
                preparationStage =
                    preferences[Keys.PREPARATION_STAGE] ?: "Beginner",
                attemptNumber =
                    preferences[Keys.ATTEMPT_NUMBER] ?: "",
                profileImageUri =
                    preferences[Keys.PROFILE_IMAGE_URI]
            )
        }

    // Save UPSC preparation goals
    suspend fun saveGoals(goals: StoredAspirantGoals) {
        dataStore.edit { preferences ->
            preferences[Keys.TARGET_EXAM_YEAR] = goals.targetExamYear
            preferences[Keys.PRELIMS_DATE] = goals.prelimsDate
            preferences[Keys.OPTIONAL_SUBJECT] = goals.optionalSubject
            preferences[Keys.STUDY_MEDIUM] = goals.studyMedium
            preferences[Keys.DAILY_STUDY_HOURS] = goals.dailyStudyHours
            preferences[Keys.PREPARATION_START_DATE] =
                goals.preparationStartDate
        }
    }

    // Read UPSC preparation goals
    val goalsFlow: Flow<StoredAspirantGoals> =
        dataStore.data.map { preferences ->
            StoredAspirantGoals(
                targetExamYear =
                    preferences[Keys.TARGET_EXAM_YEAR] ?: 2027,
                prelimsDate =
                    preferences[Keys.PRELIMS_DATE] ?: "23-05-2027",
                optionalSubject =
                    preferences[Keys.OPTIONAL_SUBJECT] ?: "Sociology",
                studyMedium =
                    preferences[Keys.STUDY_MEDIUM] ?: "English",
                dailyStudyHours =
                    preferences[Keys.DAILY_STUDY_HOURS] ?: 8,
                preparationStartDate =
                    preferences[Keys.PREPARATION_START_DATE] ?: "08-06-2026"
            )
        }

    // Save onboarding completion status
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    // Read onboarding completion status
    val onboardingCompletedFlow: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[Keys.ONBOARDING_COMPLETED] ?: false
        }

    // Clear all saved app preferences
    suspend fun clearAllData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
