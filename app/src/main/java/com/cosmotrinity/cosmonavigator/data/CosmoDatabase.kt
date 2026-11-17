
package com.cosmotrinity.cosmonavigator.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        StudyTaskEntity::class,
        UpscSubjectEntity::class,
        UpscLectureEntity::class,
        DailyTimetableSettingsEntity::class,
        DailyTimetableEntity::class,
        DailyTimetableCompletionEntity::class,
        DailyStudySessionEntity::class,
        DailyHistoryEntity::class,
        DailyHistoryActivityEntity::class,
        DailySleepRecordEntity::class,

        // COSMOVault
        VaultResourceEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class CosmoDatabase : RoomDatabase() {

    abstract fun studyTaskDao(): StudyTaskDao

    abstract fun upscSubjectDao(): UpscSubjectDao

    abstract fun upscLectureDao(): UpscLectureDao

    abstract fun dailyTimetableDao(): DailyTimetableDao

    abstract fun dailyHistoryDao(): DailyHistoryDao

    abstract fun dailySleepRecordDao(): DailySleepRecordDao

    // COSMOVault DAO
    abstract fun vaultResourceDao(): VaultResourceDao

    companion object {

        @Volatile
        private var INSTANCE: CosmoDatabase? = null

        // VERSION 1 -> 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `upsc_subject_progress` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `targetExamYear` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `subjectName` TEXT NOT NULL,
                        `startDate` TEXT NOT NULL,
                        `targetEndDate` TEXT NOT NULL,
                        `actualEndDate` TEXT,
                        `totalLectures` INTEGER NOT NULL,
                        `completedLectures` INTEGER NOT NULL,
                        `totalTopics` INTEGER NOT NULL,
                        `completedTopics` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_upsc_subject_progress_targetExamYear_category_subjectName`
                    ON `upsc_subject_progress`
                    (`targetExamYear`, `category`, `subjectName`)
                    """.trimIndent()
                )
            }
        }

        // VERSION 2 -> 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `upsc_lectures` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `subjectId` INTEGER NOT NULL,
                        `lectureNumber` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `scheduledDate` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        `durationMinutes` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`subjectId`)
                        REFERENCES `upsc_subject_progress`(`id`)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_upsc_lectures_subjectId`
                    ON `upsc_lectures` (`subjectId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_upsc_lectures_subjectId_lectureNumber`
                    ON `upsc_lectures`
                    (`subjectId`, `lectureNumber`)
                    """.trimIndent()
                )
            }
        }

        // VERSION 3 -> 4
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    ALTER TABLE `upsc_subject_progress`
                    ADD COLUMN `status` TEXT NOT NULL
                    DEFAULT 'Not Started'
                    """.trimIndent()
                )
            }
        }

        // VERSION 4 -> 5
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_timetable_settings` (
                        `id` INTEGER NOT NULL,
                        `wakeUpTimeMinutes` INTEGER NOT NULL,
                        `sleepTimeMinutes` INTEGER NOT NULL,
                        `sleepNextDay` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_timetable_activities` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `startTimeMinutes` INTEGER NOT NULL,
                        `endTimeMinutes` INTEGER NOT NULL,
                        `activityType` TEXT NOT NULL,
                        `countsAsStudy` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_daily_timetable_activities_sortOrder`
                    ON `daily_timetable_activities` (`sortOrder`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_daily_timetable_activities_isEnabled`
                    ON `daily_timetable_activities` (`isEnabled`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_study_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `activityId` INTEGER NOT NULL,
                        `studyDate` TEXT NOT NULL,
                        `startedAt` INTEGER NOT NULL,
                        `endedAt` INTEGER,
                        `durationSeconds` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_daily_study_sessions_activityId`
                    ON `daily_study_sessions` (`activityId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_daily_study_sessions_studyDate`
                    ON `daily_study_sessions` (`studyDate`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_daily_study_sessions_activityId_studyDate`
                    ON `daily_study_sessions`
                    (`activityId`, `studyDate`)
                    """.trimIndent()
                )
            }
        }

        // VERSION 5 -> 6
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_timetable_completions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `activityId` INTEGER NOT NULL,
                        `studyDate` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `completedAt` INTEGER,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_daily_timetable_completions_activityId_studyDate`
                    ON `daily_timetable_completions`
                    (`activityId`, `studyDate`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_daily_timetable_completions_studyDate`
                    ON `daily_timetable_completions` (`studyDate`)
                    """.trimIndent()
                )
            }
        }

        // VERSION 6 -> 7
        // Adds permanent daily history and activity snapshots.
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `studyDate` TEXT NOT NULL,
                        `targetStudySeconds` INTEGER NOT NULL,
                        `actualStudySeconds` INTEGER NOT NULL,
                        `totalActivities` INTEGER NOT NULL,
                        `completedActivities` INTEGER NOT NULL,
                        `missedActivities` INTEGER NOT NULL,
                        `pendingActivities` INTEGER NOT NULL,
                        `completionPercentage` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_daily_history_studyDate`
                    ON `daily_history` (`studyDate`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_history_activities` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `historyId` INTEGER NOT NULL,
                        `sourceActivityId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `activityType` TEXT NOT NULL,
                        `countsAsStudy` INTEGER NOT NULL,
                        `startTimeMinutes` INTEGER NOT NULL,
                        `endTimeMinutes` INTEGER NOT NULL,
                        `scheduledDurationSeconds` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `actualStudySeconds` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        `notes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`historyId`)
                        REFERENCES `daily_history`(`id`)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_daily_history_activities_historyId`
                    ON `daily_history_activities` (`historyId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_daily_history_activities_historyId_sourceActivityId`
                    ON `daily_history_activities`
                    (`historyId`, `sourceActivityId`)
                    """.trimIndent()
                )
            }
        }

        // VERSION 7 -> 8
        // Adds daily wake-up and sleep tracking records.
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_sleep_records` (
                        `recordDate` TEXT NOT NULL,
                        `plannedWakeUpTimeMinutes` INTEGER NOT NULL,
                        `plannedSleepTimeMinutes` INTEGER NOT NULL,
                        `actualSleepAt` INTEGER,
                        `actualWakeUpAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`recordDate`)
                    )
                    """.trimIndent()
                )
            }
        }

        // VERSION 8 -> 9
        // Adds CosmoVault resource metadata storage.
        private val MIGRATION_8_9 = object : Migration(8, 9) {

            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `vault_resources` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `targetExamYear` INTEGER NOT NULL,
                        `subjectId` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `subjectName` TEXT NOT NULL,
                        `folderName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `originalFileName` TEXT NOT NULL,
                        `storedFileName` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `relativePath` TEXT NOT NULL,
                        `resourceType` TEXT NOT NULL,
                        `fileSizeBytes` INTEGER NOT NULL,
                        `isFavorite` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_vault_resources_location`
                    ON `vault_resources`
                    (`targetExamYear`, `category`, `subjectId`, `folderName`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_vault_resources_title`
                    ON `vault_resources`
                    (`targetExamYear`, `title`)
                    """.trimIndent()
                )
            }
        }

        // DATABASE INITIALIZATION
        fun getDatabase(context: Context): CosmoDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CosmoDatabase::class.java,
                    "cosmonavigator_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
