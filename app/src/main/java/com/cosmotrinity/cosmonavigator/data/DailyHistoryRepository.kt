
package com.cosmotrinity.cosmonavigator.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Locale

class DailyHistoryRepository(
    private val historyDao: DailyHistoryDao,
    private val timetableDao: DailyTimetableDao
) {

    companion object {

        private const val DATE_FORMAT = "dd-MM-yyyy"

        private fun isValidDate(date: String): Boolean {
            return try {
                val formatter = SimpleDateFormat(
                    DATE_FORMAT,
                    Locale.getDefault()
                )
                formatter.isLenient = false
                formatter.parse(date) != null
            } catch (_: Exception) {
                false
            }
        }

        private fun durationSeconds(
            activity: DailyTimetableEntity
        ): Long {
            return (
                    activity.endTimeMinutes -
                            activity.startTimeMinutes
                    ).coerceAtLeast(0) * 60L
        }

        // Identifies the same activity even if it was deleted
        // and recreated with a different database ID.
        private fun activityKey(
            title: String,
            activityType: String,
            startTimeMinutes: Int,
            endTimeMinutes: Int
        ): String {
            return "${title.trim().lowercase(Locale.ROOT)}|" +
                    "${activityType.trim().lowercase(Locale.ROOT)}|" +
                    "$startTimeMinutes|$endTimeMinutes"
        }

        private fun activityKey(
            activity: DailyTimetableEntity
        ): String {
            return activityKey(
                title = activity.title,
                activityType = activity.activityType,
                startTimeMinutes = activity.startTimeMinutes,
                endTimeMinutes = activity.endTimeMinutes
            )
        }

        private fun activityKey(
            activity: DailyHistoryActivityEntity
        ): String {
            return activityKey(
                title = activity.title,
                activityType = activity.activityType,
                startTimeMinutes = activity.startTimeMinutes,
                endTimeMinutes = activity.endTimeMinutes
            )
        }
    }

    // ---------------------------------------------------------
    // OBSERVE AND RETRIEVE HISTORY
    // ---------------------------------------------------------

    fun observeHistoryForDate(
        date: String
    ): Flow<DailyHistoryEntity?> {

        require(isValidDate(date)) {
            "Date must use DD-MM-YYYY format."
        }

        return historyDao.observeHistoryForDate(date)
    }

    fun observeAllHistory(): Flow<List<DailyHistoryEntity>> {
        return historyDao.observeAllHistory()
    }

    fun observeActivitiesForHistory(
        historyId: Int
    ): Flow<List<DailyHistoryActivityEntity>> {
        return historyDao.observeActivitiesForHistory(historyId)
    }

    suspend fun getHistoryForDate(
        date: String
    ): DailyHistoryEntity? {

        require(isValidDate(date)) {
            "Date must use DD-MM-YYYY format."
        }

        return historyDao.getHistoryForDate(date)
    }

    suspend fun getActivitiesForHistory(
        historyId: Int
    ): List<DailyHistoryActivityEntity> {
        return historyDao.getActivitiesForHistory(historyId)
    }

    suspend fun getAllHistory(): List<DailyHistoryEntity> {
        return historyDao.getAllHistory()
    }

    // ---------------------------------------------------------
    // CAPTURE DAILY HISTORY SNAPSHOT
    // ---------------------------------------------------------

    /**
     * Refreshes today's Calendar snapshot from the current
     * enabled timetable.
     *
     * Existing activity records are matched by database ID first.
     * If an activity was deleted and recreated, it is matched by
     * title, type and scheduled time to prevent duplicates.
     *
     * Past dates are never modified by this method.
     */
    suspend fun captureDailyHistory(
        date: String
    ): DailyHistoryEntity {

        require(isValidDate(date)) {
            "Date must use DD-MM-YYYY format."
        }

        require(date == DailyTimetableRepository.currentDate()) {
            "Daily history can only be captured for today."
        }

        val now = System.currentTimeMillis()

        val existingHistory = historyDao.getHistoryForDate(date)

        val existingActivities = existingHistory?.let {
            historyDao.getActivitiesForHistory(it.id)
        } ?: emptyList()

        // Old snapshots indexed by their activity identity.
        val existingByActivityId = existingActivities.associateBy {
            it.sourceActivityId
        }

        val existingByKey = existingActivities.groupBy {
            activityKey(it)
        }

        // Current enabled timetable is the source of truth.
        // Duplicate entries with identical identity are included once.
        val currentActivities = timetableDao.getAllActivities()
            .filter { it.isEnabled }
            .distinctBy { activityKey(it) }
            .sortedBy { it.startTimeMinutes }

        val completions = timetableDao.getCompletionsForDate(date)
            .associateBy { it.activityId }

        // Only sessions belonging to this exact date are considered.
        val sessions = timetableDao.getSessionsForDate(date)

        val actualSecondsByActivity = sessions
            .groupBy { it.activityId }
            .mapValues { (_, activitySessions) ->

                activitySessions.sumOf { session ->

                    if (session.status == "RUNNING") {

                        val elapsed = (
                                (now - session.startedAt) / 1000L
                                ).coerceAtLeast(0L)

                        session.durationSeconds + elapsed

                    } else {
                        session.durationSeconds
                    }
                }
            }

        // Build today's snapshot ONLY from current activities.
        // This prevents deleted/recreated activities from appearing twice.
        val usedOldActivityIds = mutableSetOf<Int>()

        val historyActivities = currentActivities.map { activity ->

            // First match using the original database ID.
            // If the ID changed, fall back to activity identity.
            val oldActivity = existingByActivityId[activity.id]
                ?: existingByKey[activityKey(activity)]
                    ?.firstOrNull {
                        it.sourceActivityId !in usedOldActivityIds
                    }

            oldActivity?.let {
                usedOldActivityIds.add(it.sourceActivityId)
            }

            val completion = completions[activity.id]

            /*
             * IMPORTANT FIX:
             *
             * Actual study time comes ONLY from sessions recorded
             * for the selected date.
             *
             * Never carry forward actualStudySeconds from an old
             * activity snapshot. This prevents yesterday's study
             * time from appearing on a new day.
             */
            val recordedSeconds =
                actualSecondsByActivity[activity.id] ?: 0L

            val latestStatus = completion?.status
                ?: oldActivity?.status
                ?: "PENDING"

            /*
             * Completed study activities receive at least
             * their scheduled duration.
             *
             * Timer-recorded duration is retained up to
             * the scheduled duration.
             *
             * Pending and missed activities receive only
             * their recorded timer duration.
             */

            val creditedStudySeconds = if (activity.countsAsStudy) {

                val credited = if (latestStatus == "DONE") {
                    maxOf(
                        recordedSeconds,
                        durationSeconds(activity)
                    )
                } else {
                    recordedSeconds
                }

                credited.coerceIn(
                    0L,
                    durationSeconds(activity)
                )

            } else {
                0L
            }

            DailyHistoryActivityEntity(
                historyId = existingHistory?.id ?: 0,
                sourceActivityId = activity.id,
                title = activity.title,
                activityType = activity.activityType,
                countsAsStudy = activity.countsAsStudy,
                startTimeMinutes = activity.startTimeMinutes,
                endTimeMinutes = activity.endTimeMinutes,
                scheduledDurationSeconds = durationSeconds(activity),
                status = latestStatus,
                actualStudySeconds = creditedStudySeconds,
                completedAt = completion?.completedAt
                    ?: oldActivity?.completedAt,
                notes = activity.notes,
                createdAt = oldActivity?.createdAt ?: now,
                updatedAt = now
            )
        }

        // Daily scheduled study target.
        val targetSeconds = historyActivities
            .filter { it.countsAsStudy }
            .sumOf { it.scheduledDurationSeconds }

        // Count only study activities in Calendar study time.
        val actualSeconds = historyActivities
            .filter { it.countsAsStudy }
            .sumOf { it.actualStudySeconds }

        // Daily activity statistics.
        val completedCount = historyActivities.count {
            it.status == "DONE"
        }

        val missedCount = historyActivities.count {
            it.status == "NOT_DONE"
        }

        val pendingCount = historyActivities.count {
            it.status == "PENDING"
        }

        val totalActivities = historyActivities.size

        val completionPercentage = if (totalActivities > 0) {
            (completedCount * 100) / totalActivities
        } else {
            0
        }

        // Preserve original history creation time.
        val history = DailyHistoryEntity(
            id = existingHistory?.id ?: 0,
            studyDate = date,
            targetStudySeconds = targetSeconds,
            actualStudySeconds = actualSeconds,
            totalActivities = totalActivities,
            completedActivities = completedCount,
            missedActivities = missedCount,
            pendingActivities = pendingCount,
            completionPercentage = completionPercentage,
            createdAt = existingHistory?.createdAt ?: now,
            updatedAt = now
        )

        // Save parent and child snapshots atomically.
        val historyId = historyDao.saveHistoryWithActivities(
            history = history,
            activities = historyActivities
        )

        return history.copy(id = historyId)
    }

    // ---------------------------------------------------------
    // HISTORY STATISTICS
    // ---------------------------------------------------------

    suspend fun getHistoryDayCount(): Int {
        return historyDao.getHistoryDayCount()
    }

    suspend fun getTotalRecordedStudySeconds(): Long {
        return historyDao.getTotalRecordedStudySeconds()
    }

    // ---------------------------------------------------------
    // DELETE HISTORY
    // ---------------------------------------------------------

    suspend fun deleteHistoryForDate(date: String) {

        require(isValidDate(date)) {
            "Date must use DD-MM-YYYY format."
        }

        historyDao.deleteHistoryForDate(date)
    }

    suspend fun deleteAllHistory() {
        historyDao.deleteAllHistory()
    }
}
