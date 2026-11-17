
package com.cosmotrinity.cosmonavigator.data.backup

import com.cosmotrinity.cosmonavigator.data.StoredAspirantProfile
import com.cosmotrinity.cosmonavigator.data.StoredAspirantGoals
import com.cosmotrinity.cosmonavigator.data.UpscSubjectEntity
import com.cosmotrinity.cosmonavigator.data.UpscLectureEntity
import com.cosmotrinity.cosmonavigator.data.DailyHistoryEntity
import com.cosmotrinity.cosmonavigator.data.DailyHistoryActivityEntity
import com.cosmotrinity.cosmonavigator.data.DailyTimetableSettingsEntity
import com.cosmotrinity.cosmonavigator.data.DailyTimetableEntity
import com.cosmotrinity.cosmonavigator.data.DailyTimetableCompletionEntity
import com.cosmotrinity.cosmonavigator.data.DailyStudySessionEntity
import com.cosmotrinity.cosmonavigator.data.DailySleepRecordEntity

/**
 * Complete data structure for CosmoNavigator encrypted backup.
 *
 * Includes:
 * - Aspirant profile and profile image
 * - UPSC preparation goals
 * - Onboarding completion state
 * - UPSC subjects and lectures
 * - Calendar daily history
 * - Calendar activity snapshots
 * - Daily timetable settings
 * - Recurring timetable activities
 * - Daily activity completion records
 * - Actual study sessions
 * - Daily sleep records
 */
data class CosmoBackupData(

    val formatVersion: Int = CURRENT_FORMAT_VERSION,

    val exportedAt: Long = System.currentTimeMillis(),

    val profile: StoredAspirantProfile,

    val goals: StoredAspirantGoals,

    val onboardingCompleted: Boolean,

    val profileImageBase64: String? = null,

    val subjects: List<UpscSubjectEntity>,

    val lectures: List<UpscLectureEntity>,

    // Calendar history
    val dailyHistory: List<DailyHistoryEntity> = emptyList(),

    // Calendar activity snapshots
    val dailyHistoryActivities: List<DailyHistoryActivityEntity> = emptyList(),

    // Daily timetable settings
    val dailyTimetableSettings: DailyTimetableSettingsEntity? = null,

    // All recurring timetable activities
    val dailyTimetableActivities: List<DailyTimetableEntity> = emptyList(),

    // Daily completion records
    val dailyTimetableCompletions: List<DailyTimetableCompletionEntity> = emptyList(),

    // Actual study sessions
    val dailyStudySessions: List<DailyStudySessionEntity> = emptyList(),

    // Daily sleep records
    val dailySleepRecords: List<DailySleepRecordEntity> = emptyList()

) {

    companion object {
        const val CURRENT_FORMAT_VERSION = 4
    }
}
