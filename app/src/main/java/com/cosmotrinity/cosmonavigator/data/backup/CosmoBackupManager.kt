
package com.cosmotrinity.cosmonavigator.data.backup

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import com.cosmotrinity.cosmonavigator.data.CosmoDatabase
import com.cosmotrinity.cosmonavigator.data.DataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class CosmoBackupManager(
    private val context: Context,
    private val database: CosmoDatabase,
    private val dataStore: DataStoreManager
) {

    private val contentResolver = context.contentResolver

    companion object {
        private const val MAX_BACKUP_SIZE = 100 * 1024 * 1024
        private const val PROFILE_IMAGE_DIRECTORY = "profile_images"
        private const val TIMETABLE_BACKUP_VERSION = 3
        private const val SLEEP_BACKUP_VERSION = 4
    }

    // ---------------------------------------------------------
    // EXPORT BACKUP
    // ---------------------------------------------------------

    suspend fun exportBackup(
        destination: Uri,
        password: CharArray
    ) = withContext(Dispatchers.IO) {

        val profile = dataStore.profileFlow.first()
        val goals = dataStore.goalsFlow.first()
        val onboardingCompleted =
            dataStore.onboardingCompletedFlow.first()

        // UPSC DATA
        val subjects =
            database.upscSubjectDao().getAllSubjectsForBackup()

        val lectures =
            database.upscLectureDao().getAllLecturesForBackup()

        // CALENDAR DATA
        val historyDao = database.dailyHistoryDao()

        val dailyHistory = historyDao.getAllHistory()

        val dailyHistoryActivities = dailyHistory.flatMap { history ->
            historyDao.getActivitiesForHistory(history.id)
        }

        // TIMETABLE DATA
        val timetableDao = database.dailyTimetableDao()

        val dailyTimetableSettings = timetableDao.getSettings()

        val dailyTimetableActivities =
            timetableDao.getAllActivities()

        val dailyTimetableCompletions =
            timetableDao.getAllCompletions()

        val dailyStudySessions =
            timetableDao.getAllSessions()

        // DAILY SLEEP DATA
        val sleepDao = database.dailySleepRecordDao()

        val dailySleepRecords =
            sleepDao.getAllRecordsForBackup()

        // PROFILE IMAGE
        val profileImageBase64 = profile.profileImageUri?.let { imageUri ->
            val imageBytes = readProfileImage(Uri.parse(imageUri))

            try {
                Base64.encodeToString(
                    imageBytes,
                    Base64.NO_WRAP
                )
            } finally {
                imageBytes.fill(0)
            }
        }

        // COMPLETE BACKUP DATA
        val backup = CosmoBackupData(
            profile = profile,
            goals = goals,
            onboardingCompleted = onboardingCompleted,
            profileImageBase64 = profileImageBase64,

            subjects = subjects,
            lectures = lectures,

            dailyHistory = dailyHistory,
            dailyHistoryActivities = dailyHistoryActivities,

            dailyTimetableSettings = dailyTimetableSettings,
            dailyTimetableActivities = dailyTimetableActivities,
            dailyTimetableCompletions = dailyTimetableCompletions,
            dailyStudySessions = dailyStudySessions,

            dailySleepRecords = dailySleepRecords
        )

        val plainBytes = CosmoBackupJson.encode(backup)

        try {
            val encryptedBytes = CosmoBackupCrypto.encrypt(
                plainData = plainBytes,
                password = password
            )

            try {
                if (encryptedBytes.size > MAX_BACKUP_SIZE) {
                    throw IOException("Encrypted backup is too large.")
                }

                contentResolver.openOutputStream(destination, "w")?.use {
                    it.write(encryptedBytes)
                    it.flush()
                } ?: throw IOException(
                    "Unable to create the backup file."
                )

            } finally {
                encryptedBytes.fill(0)
            }

        } finally {
            plainBytes.fill(0)
        }
    }

    // ---------------------------------------------------------
    // RESTORE BACKUP
    // ---------------------------------------------------------

    suspend fun restoreBackup(
        source: Uri,
        password: CharArray
    ) = withContext(Dispatchers.IO) {

        // READ ENCRYPTED FILE
        val encryptedBytes = contentResolver
            .openInputStream(source)
            ?.use { input ->
                val bytes = input.readBytes()

                if (bytes.size > MAX_BACKUP_SIZE + 1024) {
                    bytes.fill(0)
                    throw IOException("Backup file is too large.")
                }

                bytes
            } ?: throw IOException(
            "Unable to open the selected backup file."
        )

        // DECRYPT
        val plainBytes = try {
            CosmoBackupCrypto.decrypt(
                encryptedBackup = encryptedBytes,
                password = password
            )
        } finally {
            encryptedBytes.fill(0)
        }

        // DECODE AND VALIDATE
        val backup = try {
            CosmoBackupJson.decode(plainBytes)
        } finally {
            plainBytes.fill(0)
        }

        // PREPARE PROFILE IMAGE
        val restoredImageUri = backup.profileImageBase64?.let {
            saveProfileImage(it)
        }

        val restoredProfile = backup.profile.copy(
            profileImageUri = restoredImageUri
        )

        // Older backups do not contain timetable information.
        val shouldRestoreTimetable =
            backup.formatVersion >= TIMETABLE_BACKUP_VERSION

        // Older backups do not contain sleep records.
        // Preserve existing sleep records when restoring them.
        val shouldRestoreSleepRecords =
            backup.formatVersion >= SLEEP_BACKUP_VERSION

        // -----------------------------------------------------
        // SAVE CURRENT DATA FOR ROLLBACK
        // -----------------------------------------------------

        val oldProfile = dataStore.profileFlow.first()
        val oldGoals = dataStore.goalsFlow.first()

        val oldOnboardingCompleted =
            dataStore.onboardingCompletedFlow.first()

        val subjectDao = database.upscSubjectDao()
        val lectureDao = database.upscLectureDao()
        val historyDao = database.dailyHistoryDao()
        val timetableDao = database.dailyTimetableDao()
        val sleepDao = database.dailySleepRecordDao()

        val oldSubjects = subjectDao.getAllSubjectsForBackup()
        val oldLectures = lectureDao.getAllLecturesForBackup()

        // Calendar rollback data
        val oldDailyHistory = historyDao.getAllHistory()

        val oldDailyHistoryActivities =
            oldDailyHistory.flatMap { history ->
                historyDao.getActivitiesForHistory(history.id)
            }

        // Timetable rollback data
        val oldTimetableSettings = timetableDao.getSettings()
        val oldTimetableActivities = timetableDao.getAllActivities()
        val oldTimetableCompletions = timetableDao.getAllCompletions()
        val oldStudySessions = timetableDao.getAllSessions()

        // Sleep rollback data
        val oldSleepRecords =
            if (shouldRestoreSleepRecords) {
                sleepDao.getAllRecordsForBackup()
            } else {
                emptyList()
            }

        try {

            // -------------------------------------------------
            // RESTORE ROOM DATABASE
            // -------------------------------------------------

            database.withTransaction {

                // UPSC: DELETE OLD DATA
                lectureDao.deleteAllLecturesForRestore()
                subjectDao.deleteAllSubjectsForRestore()

                // CALENDAR: DELETE OLD HISTORY
                historyDao.deleteAllHistory()

                // TIMETABLE: DELETE OLD DATA
                if (shouldRestoreTimetable) {

                    // Delete dependent records first.
                    timetableDao.deleteAllSessions()
                    timetableDao.deleteAllCompletions()

                    // Then recurring activities.
                    timetableDao.deleteAllActivities()

                    // Remove previous settings.
                    timetableDao.deleteSettings()
                }

                // SLEEP: DELETE OLD DATA
                if (shouldRestoreSleepRecords) {
                    sleepDao.deleteAllRecordsForRestore()
                }

                // ---------------------------------------------
                // RESTORE UPSC DATA
                // ---------------------------------------------

                if (backup.subjects.isNotEmpty()) {
                    subjectDao.insertSubjectsForRestore(
                        backup.subjects
                    )
                }

                if (backup.lectures.isNotEmpty()) {
                    lectureDao.insertLecturesForRestore(
                        backup.lectures
                    )
                }

                // ---------------------------------------------
                // RESTORE CALENDAR DATA
                // ---------------------------------------------

                // Preserve original history IDs.
                backup.dailyHistory.forEach { history ->
                    historyDao.insertHistory(history)
                }

                if (backup.dailyHistoryActivities.isNotEmpty()) {
                    historyDao.insertHistoryActivities(
                        backup.dailyHistoryActivities
                    )
                }

                // ---------------------------------------------
                // RESTORE TIMETABLE DATA
                // ---------------------------------------------

                if (shouldRestoreTimetable) {

                    // Restore timetable settings.
                    backup.dailyTimetableSettings?.let {
                        timetableDao.saveSettings(it)
                    }

                    // Restore recurring activities first.
                    // Explicit IDs preserve completion/session links.
                    if (backup.dailyTimetableActivities.isNotEmpty()) {
                        timetableDao.insertActivities(
                            backup.dailyTimetableActivities
                        )
                    }

                    // Restore completion records.
                    if (backup.dailyTimetableCompletions.isNotEmpty()) {
                        timetableDao.saveCompletions(
                            backup.dailyTimetableCompletions
                        )
                    }

                    // Restore actual study sessions.
                    if (backup.dailyStudySessions.isNotEmpty()) {
                        timetableDao.insertSessions(
                            backup.dailyStudySessions
                        )
                    }
                }

                // ---------------------------------------------
                // RESTORE DAILY SLEEP RECORDS
                // ---------------------------------------------

                if (
                    shouldRestoreSleepRecords &&
                    backup.dailySleepRecords.isNotEmpty()
                ) {
                    sleepDao.insertRecordsForRestore(
                        backup.dailySleepRecords
                    )
                }
            }

            // -------------------------------------------------
            // RESTORE DATASTORE
            // -------------------------------------------------

            dataStore.saveProfile(restoredProfile)
            dataStore.saveGoals(backup.goals)

            dataStore.setOnboardingCompleted(
                backup.onboardingCompleted
            )

        } catch (error: Exception) {

            // -------------------------------------------------
            // ROLLBACK DATABASE AND DATASTORE
            // -------------------------------------------------

            try {

                database.withTransaction {

                    // Remove partially restored UPSC data.
                    lectureDao.deleteAllLecturesForRestore()
                    subjectDao.deleteAllSubjectsForRestore()

                    // Remove partially restored Calendar data.
                    historyDao.deleteAllHistory()

                    // Restore previous UPSC data.
                    if (oldSubjects.isNotEmpty()) {
                        subjectDao.insertSubjectsForRestore(
                            oldSubjects
                        )
                    }

                    if (oldLectures.isNotEmpty()) {
                        lectureDao.insertLecturesForRestore(
                            oldLectures
                        )
                    }

                    // Restore previous Calendar data.
                    oldDailyHistory.forEach { history ->
                        historyDao.insertHistory(history)
                    }

                    if (oldDailyHistoryActivities.isNotEmpty()) {
                        historyDao.insertHistoryActivities(
                            oldDailyHistoryActivities
                        )
                    }

                    // Restore previous timetable only if this
                    // operation was allowed to modify it.
                    if (shouldRestoreTimetable) {

                        timetableDao.deleteAllSessions()
                        timetableDao.deleteAllCompletions()
                        timetableDao.deleteAllActivities()
                        timetableDao.deleteSettings()

                        oldTimetableSettings?.let {
                            timetableDao.saveSettings(it)
                        }

                        if (oldTimetableActivities.isNotEmpty()) {
                            timetableDao.insertActivities(
                                oldTimetableActivities
                            )
                        }

                        if (oldTimetableCompletions.isNotEmpty()) {
                            timetableDao.saveCompletions(
                                oldTimetableCompletions
                            )
                        }

                        if (oldStudySessions.isNotEmpty()) {
                            timetableDao.insertSessions(
                                oldStudySessions
                            )
                        }
                    }

                    // Restore previous sleep records.
                    if (shouldRestoreSleepRecords) {

                        sleepDao.deleteAllRecordsForRestore()

                        if (oldSleepRecords.isNotEmpty()) {
                            sleepDao.insertRecordsForRestore(
                                oldSleepRecords
                            )
                        }
                    }
                }

                // Restore previous DataStore values.
                dataStore.saveProfile(oldProfile)
                dataStore.saveGoals(oldGoals)

                dataStore.setOnboardingCompleted(
                    oldOnboardingCompleted
                )

            } catch (rollbackError: Exception) {
                error.addSuppressed(rollbackError)
            }

            throw IOException(
                "Backup restoration failed. Existing data was preserved where possible.",
                error
            )
        }
    }

    // ---------------------------------------------------------
    // READ PROFILE IMAGE
    // ---------------------------------------------------------

    private fun readProfileImage(uri: Uri): ByteArray {

        return when (uri.scheme) {

            "file" -> {
                val file = File(
                    requireNotNull(uri.path) {
                        "Invalid profile image path."
                    }
                )

                file.readBytes()
            }

            else -> {
                contentResolver.openInputStream(uri)?.use {
                    it.readBytes()
                } ?: throw IOException(
                    "Unable to read the profile image."
                )
            }
        }.also { bytes ->

            if (bytes.size > 15 * 1024 * 1024) {
                bytes.fill(0)
                throw IOException("Profile image is too large.")
            }
        }
    }

    // ---------------------------------------------------------
    // SAVE RESTORED PROFILE IMAGE
    // ---------------------------------------------------------

    private fun saveProfileImage(base64: String): String {

        val imageBytes = try {
            Base64.decode(base64, Base64.DEFAULT)
        } catch (error: IllegalArgumentException) {
            throw IOException(
                "Profile image in backup is invalid.",
                error
            )
        }

        if (
            imageBytes.isEmpty() ||
            imageBytes.size > 15 * 1024 * 1024
        ) {
            imageBytes.fill(0)
            throw IOException("Invalid profile image size.")
        }

        val directory = File(
            context.filesDir,
            PROFILE_IMAGE_DIRECTORY
        )

        if (!directory.exists() && !directory.mkdirs()) {
            imageBytes.fill(0)
            throw IOException(
                "Unable to create profile image storage."
            )
        }

        val imageFile = File(
            directory,
            "profile_${UUID.randomUUID()}.jpg"
        )

        try {

            FileOutputStream(imageFile).use { output ->
                output.write(imageBytes)
                output.flush()
            }

            return Uri.fromFile(imageFile).toString()

        } catch (error: Exception) {

            imageFile.delete()

            throw IOException(
                "Unable to restore profile image.",
                error
            )

        } finally {
            imageBytes.fill(0)
        }
    }
}
