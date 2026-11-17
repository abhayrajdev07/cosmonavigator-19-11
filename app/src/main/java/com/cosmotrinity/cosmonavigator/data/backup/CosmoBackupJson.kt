
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
import org.json.JSONArray
import org.json.JSONObject

object CosmoBackupJson {

    fun encode(backup: CosmoBackupData): ByteArray {
        validate(backup)

        val root = JSONObject().apply {
            put("formatVersion", backup.formatVersion)
            put("exportedAt", backup.exportedAt)
            put("onboardingCompleted", backup.onboardingCompleted)
            put("profileImageBase64", backup.profileImageBase64 ?: JSONObject.NULL)

            put("profile", JSONObject().apply {
                put("fullName", backup.profile.fullName)
                put("language", backup.profile.language)
                put("preparationStage", backup.profile.preparationStage)
                put("attemptNumber", backup.profile.attemptNumber)
                put("profileImageUri", backup.profile.profileImageUri ?: JSONObject.NULL)
            })

            put("goals", JSONObject().apply {
                put("targetExamYear", backup.goals.targetExamYear)
                put("prelimsDate", backup.goals.prelimsDate)
                put("optionalSubject", backup.goals.optionalSubject)
                put("studyMedium", backup.goals.studyMedium)
                put("dailyStudyHours", backup.goals.dailyStudyHours)
                put("preparationStartDate", backup.goals.preparationStartDate)
            })

            // UPSC SUBJECTS
            put("subjects", JSONArray().apply {
                backup.subjects.forEach { subject ->
                    put(JSONObject().apply {
                        put("id", subject.id)
                        put("targetExamYear", subject.targetExamYear)
                        put("category", subject.category)
                        put("subjectName", subject.subjectName)
                        put("status", subject.status)
                        put("startDate", subject.startDate)
                        put("targetEndDate", subject.targetEndDate)
                        put("actualEndDate", subject.actualEndDate ?: JSONObject.NULL)
                        put("totalLectures", subject.totalLectures)
                        put("completedLectures", subject.completedLectures)
                        put("totalTopics", subject.totalTopics)
                        put("completedTopics", subject.completedTopics)
                        put("notes", subject.notes)
                        put("updatedAt", subject.updatedAt)
                    })
                }
            })

            // UPSC LECTURES
            put("lectures", JSONArray().apply {
                backup.lectures.forEach { lecture ->
                    put(JSONObject().apply {
                        put("id", lecture.id)
                        put("subjectId", lecture.subjectId)
                        put("lectureNumber", lecture.lectureNumber)
                        put("title", lecture.title)
                        put("scheduledDate", lecture.scheduledDate)
                        put("isCompleted", lecture.isCompleted)
                        put("completedAt", lecture.completedAt ?: JSONObject.NULL)
                        put("durationMinutes", lecture.durationMinutes)
                        put("notes", lecture.notes)
                        put("createdAt", lecture.createdAt)
                        put("updatedAt", lecture.updatedAt)
                    })
                }
            })

            // CALENDAR DAILY HISTORY
            put("dailyHistory", JSONArray().apply {
                backup.dailyHistory.forEach { history ->
                    put(JSONObject().apply {
                        put("id", history.id)
                        put("studyDate", history.studyDate)
                        put("targetStudySeconds", history.targetStudySeconds)
                        put("actualStudySeconds", history.actualStudySeconds)
                        put("totalActivities", history.totalActivities)
                        put("completedActivities", history.completedActivities)
                        put("missedActivities", history.missedActivities)
                        put("pendingActivities", history.pendingActivities)
                        put("completionPercentage", history.completionPercentage)
                        put("createdAt", history.createdAt)
                        put("updatedAt", history.updatedAt)
                    })
                }
            })

            // CALENDAR ACTIVITY SNAPSHOTS
            put("dailyHistoryActivities", JSONArray().apply {
                backup.dailyHistoryActivities.forEach { activity ->
                    put(JSONObject().apply {
                        put("historyId", activity.historyId)
                        put("sourceActivityId", activity.sourceActivityId)
                        put("title", activity.title)
                        put("activityType", activity.activityType)
                        put("countsAsStudy", activity.countsAsStudy)
                        put("startTimeMinutes", activity.startTimeMinutes)
                        put("endTimeMinutes", activity.endTimeMinutes)
                        put("scheduledDurationSeconds", activity.scheduledDurationSeconds)
                        put("status", activity.status)
                        put("actualStudySeconds", activity.actualStudySeconds)
                        put("completedAt", activity.completedAt ?: JSONObject.NULL)
                        put("notes", activity.notes)
                        put("createdAt", activity.createdAt)
                        put("updatedAt", activity.updatedAt)
                    })
                }
            })

            // DAILY TIMETABLE SETTINGS
            put(
                "dailyTimetableSettings",
                backup.dailyTimetableSettings?.let { settings ->
                    JSONObject().apply {
                        put("id", settings.id)
                        put("wakeUpTimeMinutes", settings.wakeUpTimeMinutes)
                        put("sleepTimeMinutes", settings.sleepTimeMinutes)
                        put("sleepNextDay", settings.sleepNextDay)
                        put("updatedAt", settings.updatedAt)
                    }
                } ?: JSONObject.NULL
            )

            // RECURRING TIMETABLE ACTIVITIES
            put("dailyTimetableActivities", JSONArray().apply {
                backup.dailyTimetableActivities.forEach { activity ->
                    put(JSONObject().apply {
                        put("id", activity.id)
                        put("title", activity.title)
                        put("startTimeMinutes", activity.startTimeMinutes)
                        put("endTimeMinutes", activity.endTimeMinutes)
                        put("activityType", activity.activityType)
                        put("countsAsStudy", activity.countsAsStudy)
                        put("sortOrder", activity.sortOrder)
                        put("notes", activity.notes)
                        put("isEnabled", activity.isEnabled)
                        put("createdAt", activity.createdAt)
                        put("updatedAt", activity.updatedAt)
                    })
                }
            })

            // DAILY COMPLETION RECORDS
            put("dailyTimetableCompletions", JSONArray().apply {
                backup.dailyTimetableCompletions.forEach { completion ->
                    put(JSONObject().apply {
                        put("id", completion.id)
                        put("activityId", completion.activityId)
                        put("studyDate", completion.studyDate)
                        put("status", completion.status)
                        put("completedAt", completion.completedAt ?: JSONObject.NULL)
                        put("updatedAt", completion.updatedAt)
                    })
                }
            })

            // ACTUAL STUDY SESSIONS
            put("dailyStudySessions", JSONArray().apply {
                backup.dailyStudySessions.forEach { session ->
                    put(JSONObject().apply {
                        put("id", session.id)
                        put("activityId", session.activityId)
                        put("studyDate", session.studyDate)
                        put("startedAt", session.startedAt)
                        put("endedAt", session.endedAt ?: JSONObject.NULL)
                        put("durationSeconds", session.durationSeconds)
                        put("status", session.status)
                        put("createdAt", session.createdAt)
                        put("updatedAt", session.updatedAt)
                    })
                }
            })

            // DAILY SLEEP RECORDS
            put("dailySleepRecords", JSONArray().apply {
                backup.dailySleepRecords.forEach { record ->
                    put(JSONObject().apply {
                        put("recordDate", record.recordDate)
                        put("plannedWakeUpTimeMinutes", record.plannedWakeUpTimeMinutes)
                        put("plannedSleepTimeMinutes", record.plannedSleepTimeMinutes)
                        put("actualSleepAt", record.actualSleepAt ?: JSONObject.NULL)
                        put("actualWakeUpAt", record.actualWakeUpAt ?: JSONObject.NULL)
                        put("createdAt", record.createdAt)
                        put("updatedAt", record.updatedAt)
                    })
                }
            })
        }

        return root.toString().toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): CosmoBackupData {
        try {
            val root = JSONObject(String(bytes, Charsets.UTF_8))

            val version = root.getInt("formatVersion")

            require(version in 1..CosmoBackupData.CURRENT_FORMAT_VERSION) {
                "Unsupported backup data version: $version"
            }

            val profileJson = root.getJSONObject("profile")
            val goalsJson = root.getJSONObject("goals")

            val profile = StoredAspirantProfile(
                fullName = profileJson.getString("fullName"),
                language = profileJson.getString("language"),
                preparationStage = profileJson.getString("preparationStage"),
                attemptNumber = profileJson.getString("attemptNumber"),
                profileImageUri = profileJson.optionalString("profileImageUri")
            )

            val goals = StoredAspirantGoals(
                targetExamYear = goalsJson.getInt("targetExamYear"),
                prelimsDate = goalsJson.getString("prelimsDate"),
                optionalSubject = goalsJson.getString("optionalSubject"),
                studyMedium = goalsJson.getString("studyMedium"),
                dailyStudyHours = goalsJson.getInt("dailyStudyHours"),
                preparationStartDate = goalsJson.getString("preparationStartDate")
            )

            // UPSC SUBJECTS
            val subjectsJson = root.getJSONArray("subjects")

            val subjects = buildList {
                for (i in 0 until subjectsJson.length()) {
                    val item = subjectsJson.getJSONObject(i)

                    add(
                        UpscSubjectEntity(
                            id = item.getInt("id"),
                            targetExamYear = item.getInt("targetExamYear"),
                            category = item.getString("category"),
                            subjectName = item.getString("subjectName"),
                            status = item.getString("status"),
                            startDate = item.getString("startDate"),
                            targetEndDate = item.getString("targetEndDate"),
                            actualEndDate = item.optionalString("actualEndDate"),
                            totalLectures = item.getInt("totalLectures"),
                            completedLectures = item.getInt("completedLectures"),
                            totalTopics = item.getInt("totalTopics"),
                            completedTopics = item.getInt("completedTopics"),
                            notes = item.getString("notes"),
                            updatedAt = item.getLong("updatedAt")
                        )
                    )
                }
            }

            // UPSC LECTURES
            val lecturesJson = root.getJSONArray("lectures")

            val lectures = buildList {
                for (i in 0 until lecturesJson.length()) {
                    val item = lecturesJson.getJSONObject(i)

                    add(
                        UpscLectureEntity(
                            id = item.getInt("id"),
                            subjectId = item.getInt("subjectId"),
                            lectureNumber = item.getInt("lectureNumber"),
                            title = item.getString("title"),
                            scheduledDate = item.getString("scheduledDate"),
                            isCompleted = item.getBoolean("isCompleted"),
                            completedAt = item.optionalLong("completedAt"),
                            durationMinutes = item.getInt("durationMinutes"),
                            notes = item.getString("notes"),
                            createdAt = item.getLong("createdAt"),
                            updatedAt = item.getLong("updatedAt")
                        )
                    )
                }
            }

            // CALENDAR HISTORY
            val historyJson = root.optJSONArray("dailyHistory") ?: JSONArray()

            val dailyHistory = buildList {
                for (i in 0 until historyJson.length()) {
                    val item = historyJson.getJSONObject(i)

                    add(
                        DailyHistoryEntity(
                            id = item.getInt("id"),
                            studyDate = item.getString("studyDate"),
                            targetStudySeconds = item.getLong("targetStudySeconds"),
                            actualStudySeconds = item.getLong("actualStudySeconds"),
                            totalActivities = item.getInt("totalActivities"),
                            completedActivities = item.getInt("completedActivities"),
                            missedActivities = item.getInt("missedActivities"),
                            pendingActivities = item.getInt("pendingActivities"),
                            completionPercentage = item.getInt("completionPercentage"),
                            createdAt = item.getLong("createdAt"),
                            updatedAt = item.getLong("updatedAt")
                        )
                    )
                }
            }

            // CALENDAR ACTIVITY SNAPSHOTS
            val historyActivitiesJson =
                root.optJSONArray("dailyHistoryActivities") ?: JSONArray()

            val dailyHistoryActivities = buildList {
                for (i in 0 until historyActivitiesJson.length()) {
                    val item = historyActivitiesJson.getJSONObject(i)

                    add(
                        DailyHistoryActivityEntity(
                            historyId = item.getInt("historyId"),
                            sourceActivityId = item.getInt("sourceActivityId"),
                            title = item.getString("title"),
                            activityType = item.getString("activityType"),
                            countsAsStudy = item.getBoolean("countsAsStudy"),
                            startTimeMinutes = item.getInt("startTimeMinutes"),
                            endTimeMinutes = item.getInt("endTimeMinutes"),
                            scheduledDurationSeconds = item.getLong("scheduledDurationSeconds"),
                            status = item.getString("status"),
                            actualStudySeconds = item.getLong("actualStudySeconds"),
                            completedAt = item.optionalLong("completedAt"),
                            notes = item.getString("notes"),
                            createdAt = item.getLong("createdAt"),
                            updatedAt = item.getLong("updatedAt")
                        )
                    )
                }
            }

            // TIMETABLE SETTINGS
            val settingsJson = root.optJSONObject("dailyTimetableSettings")

            val dailyTimetableSettings = settingsJson?.let { item ->
                DailyTimetableSettingsEntity(
                    id = item.getInt("id"),
                    wakeUpTimeMinutes = item.getInt("wakeUpTimeMinutes"),
                    sleepTimeMinutes = item.getInt("sleepTimeMinutes"),
                    sleepNextDay = item.getBoolean("sleepNextDay"),
                    updatedAt = item.getLong("updatedAt")
                )
            }

            // RECURRING TIMETABLE ACTIVITIES
            val timetableActivitiesJson =
                root.optJSONArray("dailyTimetableActivities") ?: JSONArray()

            val dailyTimetableActivities = buildList {
                for (i in 0 until timetableActivitiesJson.length()) {
                    val item = timetableActivitiesJson.getJSONObject(i)

                    add(
                        DailyTimetableEntity(
                            id = item.getInt("id"),
                            title = item.getString("title"),
                            startTimeMinutes = item.getInt("startTimeMinutes"),
                            endTimeMinutes = item.getInt("endTimeMinutes"),
                            activityType = item.getString("activityType"),
                            countsAsStudy = item.getBoolean("countsAsStudy"),
                            sortOrder = item.getInt("sortOrder"),
                            notes = item.getString("notes"),
                            isEnabled = item.getBoolean("isEnabled"),
                            createdAt = item.getLong("createdAt"),
                            updatedAt = item.getLong("updatedAt")
                        )
                    )
                }
            }

            // DAILY COMPLETIONS
            val completionsJson =
                root.optJSONArray("dailyTimetableCompletions") ?: JSONArray()

            val dailyTimetableCompletions = buildList {
                for (i in 0 until completionsJson.length()) {
                    val item = completionsJson.getJSONObject(i)

                    add(
                        DailyTimetableCompletionEntity(
                            id = item.getInt("id"),
                            activityId = item.getInt("activityId"),
                            studyDate = item.getString("studyDate"),
                            status = item.getString("status"),
                            completedAt = item.optionalLong("completedAt"),
                            updatedAt = item.getLong("updatedAt")
                        )
                    )
                }
            }

            // ACTUAL STUDY SESSIONS
            val sessionsJson =
                root.optJSONArray("dailyStudySessions") ?: JSONArray()

            val dailyStudySessions = buildList {
                for (i in 0 until sessionsJson.length()) {
                    val item = sessionsJson.getJSONObject(i)

                    add(
                        DailyStudySessionEntity(
                            id = item.getInt("id"),
                            activityId = item.getInt("activityId"),
                            studyDate = item.getString("studyDate"),
                            startedAt = item.getLong("startedAt"),
                            endedAt = item.optionalLong("endedAt"),
                            durationSeconds = item.getLong("durationSeconds"),
                            status = item.getString("status"),
                            createdAt = item.getLong("createdAt"),
                            updatedAt = item.getLong("updatedAt")
                        )
                    )
                }
            }

            // DAILY SLEEP RECORDS
            // Optional so older backups remain compatible.
            val sleepRecordsJson =
                root.optJSONArray("dailySleepRecords") ?: JSONArray()

            val dailySleepRecords = buildList {
                for (i in 0 until sleepRecordsJson.length()) {
                    val item = sleepRecordsJson.getJSONObject(i)

                    add(
                        DailySleepRecordEntity(
                            recordDate = item.getString("recordDate"),
                            plannedWakeUpTimeMinutes =
                                item.getInt("plannedWakeUpTimeMinutes"),
                            plannedSleepTimeMinutes =
                                item.getInt("plannedSleepTimeMinutes"),
                            actualSleepAt = item.optionalLong("actualSleepAt"),
                            actualWakeUpAt = item.optionalLong("actualWakeUpAt"),
                            createdAt = item.getLong("createdAt"),
                            updatedAt = item.getLong("updatedAt")
                        )
                    )
                }
            }

            val backup = CosmoBackupData(
                formatVersion = version,
                exportedAt = root.getLong("exportedAt"),
                profile = profile,
                goals = goals,
                onboardingCompleted = root.getBoolean("onboardingCompleted"),
                profileImageBase64 = root.optionalString("profileImageBase64"),
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

            validate(backup)
            return backup

        } catch (error: Exception) {
            throw IllegalArgumentException(
                "Backup contents are invalid or incomplete.",
                error
            )
        }
    }

    private fun validate(backup: CosmoBackupData) {

        require(
            backup.formatVersion in 1..CosmoBackupData.CURRENT_FORMAT_VERSION
        ) {
            "Unsupported backup version."
        }

        require(backup.profile.fullName.isNotBlank()) {
            "Profile name is missing."
        }

        require(backup.goals.targetExamYear in 2020..2100) {
            "Invalid target exam year."
        }

        require(backup.goals.dailyStudyHours in 1..24) {
            "Invalid daily study target."
        }

        // UPSC VALIDATION
        require(backup.subjects.all { it.id > 0 }) {
            "Invalid subject ID."
        }

        require(backup.lectures.all { it.id > 0 }) {
            "Invalid lecture ID."
        }

        require(backup.subjects.map { it.id }.distinct().size == backup.subjects.size) {
            "Duplicate subject IDs found."
        }

        require(backup.lectures.map { it.id }.distinct().size == backup.lectures.size) {
            "Duplicate lecture IDs found."
        }

        val subjectIds = backup.subjects.map { it.id }.toSet()

        require(backup.lectures.all { it.subjectId in subjectIds }) {
            "A lecture refers to a subject that does not exist."
        }

        require(
            backup.subjects
                .map { Triple(it.targetExamYear, it.category, it.subjectName) }
                .distinct()
                .size == backup.subjects.size
        ) {
            "Duplicate subjects found."
        }

        require(
            backup.lectures
                .map { it.subjectId to it.lectureNumber }
                .distinct()
                .size == backup.lectures.size
        ) {
            "Duplicate lecture numbers found."
        }

        require(
            backup.subjects.all {
                it.totalLectures >= 0 &&
                        it.completedLectures in 0..it.totalLectures &&
                        it.totalTopics >= 0 &&
                        it.completedTopics in 0..it.totalTopics
            }
        ) {
            "Invalid subject progress values."
        }

        require(
            backup.lectures.all {
                it.lectureNumber > 0 && it.durationMinutes >= 0
            }
        ) {
            "Invalid lecture information."
        }

        // CALENDAR VALIDATION
        require(
            backup.dailyHistory.all {
                it.id >= 0 &&
                        it.studyDate.isNotBlank() &&
                        it.targetStudySeconds >= 0 &&
                        it.actualStudySeconds >= 0 &&
                        it.totalActivities >= 0 &&
                        it.completedActivities >= 0 &&
                        it.missedActivities >= 0 &&
                        it.pendingActivities >= 0 &&
                        it.completionPercentage in 0..100
            }
        ) {
            "Invalid Calendar history information."
        }

        require(
            backup.dailyHistory.map { it.studyDate }.distinct().size ==
                    backup.dailyHistory.size
        ) {
            "Duplicate Calendar history dates found."
        }

        val historyIds = backup.dailyHistory.map { it.id }.toSet()

        require(
            backup.dailyHistoryActivities.all {
                it.historyId in historyIds &&
                        it.sourceActivityId >= 0 &&
                        it.title.isNotBlank() &&
                        it.startTimeMinutes >= 0 &&
                        it.endTimeMinutes >= 0 &&
                        it.scheduledDurationSeconds >= 0 &&
                        it.actualStudySeconds >= 0 &&
                        it.status in setOf("DONE", "NOT_DONE", "PENDING")
            }
        ) {
            "Invalid Calendar activity snapshot."
        }

        // TIMETABLE SETTINGS VALIDATION
        backup.dailyTimetableSettings?.let { settings ->
            require(
                settings.id == 1 &&
                        settings.wakeUpTimeMinutes in 0..1439 &&
                        settings.sleepTimeMinutes in 0..1439
            ) {
                "Invalid timetable settings."
            }
        }

        // TIMETABLE ACTIVITY VALIDATION
        require(
            backup.dailyTimetableActivities.all {
                it.id > 0 &&
                        it.title.isNotBlank() &&
                        it.startTimeMinutes in 0..1439 &&
                        it.endTimeMinutes in 0..1439
            }
        ) {
            "Invalid timetable activity."
        }

        val timetableActivityIds =
            backup.dailyTimetableActivities.map { it.id }.toSet()

        require(
            timetableActivityIds.size == backup.dailyTimetableActivities.size
        ) {
            "Duplicate timetable activity IDs found."
        }

        // COMPLETION VALIDATION
        require(
            backup.dailyTimetableCompletions.all {
                it.id > 0 &&
                        it.activityId in timetableActivityIds &&
                        it.studyDate.isNotBlank() &&
                        it.status in setOf("PENDING", "DONE", "NOT_DONE")
            }
        ) {
            "Invalid timetable completion record."
        }

        require(
            backup.dailyTimetableCompletions
                .map { it.activityId to it.studyDate }
                .distinct()
                .size == backup.dailyTimetableCompletions.size
        ) {
            "Duplicate timetable completion records found."
        }

        // STUDY SESSION VALIDATION
        require(
            backup.dailyStudySessions.all {
                it.id > 0 &&
                        it.activityId in timetableActivityIds &&
                        it.studyDate.isNotBlank() &&
                        it.startedAt > 0 &&
                        it.durationSeconds >= 0 &&
                        it.status in setOf("RUNNING", "PAUSED", "COMPLETED")
            }
        ) {
            "Invalid study session."
        }

        require(
            backup.dailyStudySessions.map { it.id }.distinct().size ==
                    backup.dailyStudySessions.size
        ) {
            "Duplicate study session IDs found."
        }

        // SLEEP RECORD VALIDATION
        require(
            backup.dailySleepRecords.all {
                it.recordDate.isNotBlank() &&
                        it.plannedWakeUpTimeMinutes in 0..1439 &&
                        it.plannedSleepTimeMinutes in 0..1439 &&
                        (it.actualSleepAt == null || it.actualSleepAt > 0L) &&
                        (it.actualWakeUpAt == null || it.actualWakeUpAt > 0L) &&
                        it.createdAt > 0L &&
                        it.updatedAt > 0L
            }
        ) {
            "Invalid sleep record."
        }

        require(
            backup.dailySleepRecords
                .map { it.recordDate }
                .distinct()
                .size == backup.dailySleepRecords.size
        ) {
            "Duplicate sleep record dates found."
        }
    }

    private fun JSONObject.optionalString(key: String): String? {
        return if (isNull(key)) null else getString(key)
    }

    private fun JSONObject.optionalLong(key: String): Long? {
        return if (isNull(key)) null else getLong(key)
    }
}
