
package com.cosmotrinity.cosmonavigator.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyTimetableRepository(
    private val dao: DailyTimetableDao,
    private val historyRepository: DailyHistoryRepository? = null
) {

    companion object {

        const val DATE_FORMAT = "dd-MM-yyyy"
        const val TIME_FORMAT = "HH:mm"

        const val STATUS_RUNNING = "RUNNING"
        const val STATUS_PAUSED = "PAUSED"
        const val STATUS_COMPLETED = "COMPLETED"

        const val COMPLETION_PENDING = "PENDING"
        const val COMPLETION_DONE = "DONE"
        const val COMPLETION_NOT_DONE = "NOT_DONE"

        fun currentDate(): String {
            return SimpleDateFormat(
                DATE_FORMAT,
                Locale.getDefault()
            ).format(Date())
        }

        fun formatDate(date: Date): String {
            return SimpleDateFormat(
                DATE_FORMAT,
                Locale.getDefault()
            ).format(date)
        }

        fun formatTime(minutes: Int): String {
            val normalized = minutes % 1440
            val hour = normalized / 60
            val minute = normalized % 60

            return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                hour,
                minute
            )
        }

        fun timeToMinutes(time: String): Int {
            val parts = time.split(":")

            require(parts.size == 2) {
                "Time must use HH:mm format."
            }

            val hour = parts[0].toIntOrNull()
                ?: throw IllegalArgumentException("Invalid hour.")

            val minute = parts[1].toIntOrNull()
                ?: throw IllegalArgumentException("Invalid minute.")

            require(hour in 0..23 && minute in 0..59) {
                "Enter a valid 24-hour time."
            }

            return hour * 60 + minute
        }

        fun normalizeEndTime(
            startTimeMinutes: Int,
            endTimeMinutes: Int
        ): Int {

            require(startTimeMinutes in 0..1439) {
                "Invalid start time."
            }

            require(endTimeMinutes in 0..1439) {
                "Invalid end time."
            }

            val normalizedEnd = if (
                endTimeMinutes <= startTimeMinutes
            ) {
                endTimeMinutes + 1440
            } else {
                endTimeMinutes
            }

            require(
                normalizedEnd - startTimeMinutes <= 1440
            ) {
                "Activity duration cannot exceed 24 hours."
            }

            return normalizedEnd
        }

        fun durationMinutes(
            startTimeMinutes: Int,
            endTimeMinutes: Int
        ): Int {
            return endTimeMinutes - startTimeMinutes
        }

        fun formatDuration(seconds: Long): String {
            val safeSeconds = seconds.coerceAtLeast(0)

            val hours = safeSeconds / 3600
            val minutes = (safeSeconds % 3600) / 60

            return "${hours}h ${minutes}m"
        }

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
    }

    // ---------------------------------------------------------
    // DAILY HISTORY
    // ---------------------------------------------------------

    /**
     * Refreshes today's saved history after completion or
     * study-session changes.
     *
     * Historical dates are intentionally not regenerated from
     * the current recurring timetable.
     */
    private suspend fun refreshTodayHistory(date: String) {
        if (date != currentDate()) return

        historyRepository?.captureDailyHistory(date)
    }

    // ---------------------------------------------------------
    // DAILY TIMETABLE SETTINGS
    // ---------------------------------------------------------

    fun observeSettings():
            Flow<DailyTimetableSettingsEntity?> {
        return dao.observeSettings()
    }

    suspend fun getSettings():
            DailyTimetableSettingsEntity? {
        return dao.getSettings()
    }

    suspend fun saveSettings(
        wakeUpTime: String,
        sleepTime: String
    ) {

        val wakeMinutes = timeToMinutes(wakeUpTime)
        val sleepMinutes = timeToMinutes(sleepTime)

        val sleepNextDay = sleepMinutes <= wakeMinutes

        dao.saveSettings(
            DailyTimetableSettingsEntity(
                id = 1,
                wakeUpTimeMinutes = wakeMinutes,
                sleepTimeMinutes = sleepMinutes,
                sleepNextDay = sleepNextDay,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveSettings(
        settings: DailyTimetableSettingsEntity
    ) {

        require(settings.wakeUpTimeMinutes in 0..1439) {
            "Invalid wake-up time."
        }

        require(settings.sleepTimeMinutes in 0..1439) {
            "Invalid sleep time."
        }

        dao.saveSettings(
            settings.copy(
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    // ---------------------------------------------------------
    // RECURRING TIMETABLE ACTIVITIES
    // ---------------------------------------------------------

    fun observeActivities(): Flow<List<DailyTimetableEntity>> {
        return dao.observeActivities()
    }

    suspend fun getAllActivities(): List<DailyTimetableEntity> {
        return dao.getAllActivities()
    }

    suspend fun getActivityById(
        activityId: Int
    ): DailyTimetableEntity? {
        return dao.getActivityById(activityId)
    }

    suspend fun saveActivity(
        activity: DailyTimetableEntity
    ): Long {

        require(activity.title.isNotBlank()) {
            "Activity title cannot be empty."
        }

        val normalizedEnd = normalizeEndTime(
            activity.startTimeMinutes,
            activity.endTimeMinutes % 1440
        )

        val normalizedActivity = activity.copy(
            title = activity.title.trim(),
            endTimeMinutes = normalizedEnd,
            updatedAt = System.currentTimeMillis()
        )

        validateNoOverlap(normalizedActivity)

        return if (activity.id == 0) {
            dao.insertActivity(normalizedActivity)
        } else {
            dao.updateActivity(normalizedActivity)
            activity.id.toLong()
        }
    }

    suspend fun deleteActivity(activityId: Int) {
        dao.deleteActivityById(activityId)
    }

    suspend fun deleteAllActivities() {
        dao.deleteAllActivities()
    }

    suspend fun setActivityEnabled(
        activityId: Int,
        enabled: Boolean
    ) {
        dao.setActivityEnabled(
            activityId = activityId,
            enabled = enabled
        )
    }

    private suspend fun validateNoOverlap(
        activity: DailyTimetableEntity
    ) {

        if (!activity.isEnabled) return

        val existingActivities = dao.getAllActivities()
            .filter {
                it.isEnabled && it.id != activity.id
            }

        for (existing in existingActivities) {

            val overlaps = listOf(-1440, 0, 1440).any { shift ->

                val existingStart =
                    existing.startTimeMinutes + shift

                val existingEnd =
                    existing.endTimeMinutes + shift

                activity.startTimeMinutes < existingEnd &&
                        activity.endTimeMinutes > existingStart
            }

            require(!overlaps) {
                "This activity overlaps with ${existing.title}."
            }
        }
    }

    // ---------------------------------------------------------
    // DAILY ACTIVITY COMPLETION
    // ---------------------------------------------------------

    fun observeCompletionsForDate(
        date: String = currentDate()
    ): Flow<List<DailyTimetableCompletionEntity>> {

        require(isValidDate(date)) {
            "Date must use DD-MM-YYYY format."
        }

        return dao.observeCompletionsForDate(date)
    }

    suspend fun getCompletionsForDate(
        date: String = currentDate()
    ): List<DailyTimetableCompletionEntity> {

        require(isValidDate(date)) {
            "Date must use DD-MM-YYYY format."
        }

        return dao.getCompletionsForDate(date)
    }

    suspend fun getActivityCompletion(
        activityId: Int,
        date: String = currentDate()
    ): DailyTimetableCompletionEntity? {

        require(isValidDate(date)) {
            "Date must use DD-MM-YYYY format."
        }

        return dao.getCompletion(activityId, date)
    }

    /**
     * Marks an activity as completed for a specific date.
     */
    suspend fun markActivityDone(
        activityId: Int,
        date: String = currentDate()
    ) {

        require(isValidDate(date)) {
            "Date must use DD-MM-YYYY format."
        }

        require(dao.getActivityById(activityId) != null) {
            "Activity not found."
        }

        val now = System.currentTimeMillis()

        dao.saveCompletion(
            DailyTimetableCompletionEntity(
                activityId = activityId,
                studyDate = date,
                status = COMPLETION_DONE,
                completedAt = now,
                updatedAt = now
            )
        )

        refreshTodayHistory(date)
    }

    /**
     * Marks an activity as not done for a specific date.
     */
    suspend fun markActivityNotDone(
        activityId: Int,
        date: String = currentDate()
    ) {

        require(isValidDate(date)) {
            "Date must use DD-MM-YYYY format."
        }

        require(dao.getActivityById(activityId) != null) {
            "Activity not found."
        }

        val now = System.currentTimeMillis()

        dao.saveCompletion(
            DailyTimetableCompletionEntity(
                activityId = activityId,
                studyDate = date,
                status = COMPLETION_NOT_DONE,
                completedAt = null,
                updatedAt = now
            )
        )

        refreshTodayHistory(date)
    }

    /**
     * Resets an activity to Pending.
     */
    suspend fun resetActivityStatus(
        activityId: Int,
        date: String = currentDate()
    ) {

        require(isValidDate(date)) {
            "Date must use DD-MM-YYYY format."
        }

        val now = System.currentTimeMillis()

        dao.saveCompletion(
            DailyTimetableCompletionEntity(
                activityId = activityId,
                studyDate = date,
                status = COMPLETION_PENDING,
                completedAt = null,
                updatedAt = now
            )
        )

        refreshTodayHistory(date)
    }

    suspend fun getDoneActivityCount(
        date: String = currentDate()
    ): Int {

        require(isValidDate(date)) {
            "Date must use DD-MM-YYYY format."
        }

        return dao.getDoneActivityCount(date)
    }

    suspend fun getNotDoneActivityCount(
        date: String = currentDate()
    ): Int {

        require(isValidDate(date)) {
            "Date must use DD-MM-YYYY format."
        }

        return dao.getNotDoneActivityCount(date)
    }

    // ---------------------------------------------------------
    // ACTUAL STUDY SESSION TRACKING
    // ---------------------------------------------------------

    fun observeSessionsForDate(
        date: String
    ): Flow<List<DailyStudySessionEntity>> {

        require(isValidDate(date)) {
            "Date must use DD-MM-YYYY format."
        }

        return dao.observeSessionsForDate(date)
    }

    suspend fun getSessionsForDate(
        date: String
    ): List<DailyStudySessionEntity> {

        require(isValidDate(date)) {
            "Date must use DD-MM-YYYY format."
        }

        return dao.getSessionsForDate(date)
    }

    /**
     * Starts a new actual study session.
     */
    suspend fun startStudySession(
        activityId: Int,
        studyDate: String = currentDate()
    ): Long {

        require(isValidDate(studyDate)) {
            "Date must use DD-MM-YYYY format."
        }

        val activity = dao.getActivityById(activityId)
            ?: throw IllegalArgumentException("Activity not found.")

        require(activity.countsAsStudy) {
            "This activity is not marked as a study activity."
        }

        require(activity.isEnabled) {
            "This activity is disabled."
        }

        require(dao.getRunningSession() == null) {
            "Another study session is already running."
        }

        val now = System.currentTimeMillis()

        val sessionId = dao.insertSession(
            DailyStudySessionEntity(
                activityId = activityId,
                studyDate = studyDate,
                startedAt = now,
                endedAt = null,
                durationSeconds = 0L,
                status = STATUS_RUNNING,
                createdAt = now,
                updatedAt = now
            )
        )

        refreshTodayHistory(studyDate)

        return sessionId
    }

    /**
     * Pauses a running session and records its actual duration.
     */
    suspend fun pauseStudySession() {

        val session = dao.getRunningSession()
            ?: throw IllegalStateException(
                "No study session is currently running."
            )

        val now = System.currentTimeMillis()

        val elapsedSeconds = (
                (now - session.startedAt) / 1000L
                ).coerceAtLeast(0L)

        dao.updateSession(
            session.copy(
                endedAt = now,
                durationSeconds = elapsedSeconds,
                status = STATUS_PAUSED,
                updatedAt = now
            )
        )

        refreshTodayHistory(session.studyDate)
    }

    /**
     * Resumes a paused activity by creating a new tracking segment.
     */
    suspend fun resumeStudySession(
        activityId: Int
    ): Long {

        require(dao.getRunningSession() == null) {
            "Another study session is already running."
        }

        val activity = dao.getActivityById(activityId)
            ?: throw IllegalArgumentException("Activity not found.")

        require(activity.countsAsStudy) {
            "This activity is not marked as a study activity."
        }

        val date = currentDate()
        val sessions = dao.getSessionsForDate(date)

        require(
            sessions.any {
                it.activityId == activityId &&
                        it.status == STATUS_PAUSED
            }
        ) {
            "No paused session found for this activity today."
        }

        val now = System.currentTimeMillis()

        val sessionId = dao.insertSession(
            DailyStudySessionEntity(
                activityId = activityId,
                studyDate = date,
                startedAt = now,
                endedAt = null,
                durationSeconds = 0L,
                status = STATUS_RUNNING,
                createdAt = now,
                updatedAt = now
            )
        )

        refreshTodayHistory(date)

        return sessionId
    }

    /**
     * Completes the current or most recently paused segment.
     */
    suspend fun completeStudySession(
        activityId: Int,
        studyDate: String = currentDate()
    ) {

        require(isValidDate(studyDate)) {
            "Date must use DD-MM-YYYY format."
        }

        val runningSession = dao.getRunningSession()

        if (
            runningSession != null &&
            runningSession.activityId == activityId
        ) {

            require(runningSession.studyDate == studyDate) {
                "The running session belongs to another date."
            }

            val now = System.currentTimeMillis()

            val elapsedSeconds = (
                    (now - runningSession.startedAt) / 1000L
                    ).coerceAtLeast(0L)

            dao.updateSession(
                runningSession.copy(
                    endedAt = now,
                    durationSeconds = elapsedSeconds,
                    status = STATUS_COMPLETED,
                    updatedAt = now
                )
            )

            refreshTodayHistory(studyDate)
            return
        }

        val sessions = dao.getSessionsForDate(studyDate)

        val pausedSession = sessions
            .filter {
                it.activityId == activityId &&
                        it.status == STATUS_PAUSED
            }
            .maxByOrNull {
                it.startedAt
            } ?: throw IllegalStateException(
            "No active or paused session found."
        )

        dao.updateSession(
            pausedSession.copy(
                status = STATUS_COMPLETED,
                updatedAt = System.currentTimeMillis()
            )
        )

        refreshTodayHistory(studyDate)
    }

    // ---------------------------------------------------------
    // STUDY TIME CALCULATIONS
    // ---------------------------------------------------------

    suspend fun getRecordedStudySeconds(
        date: String
    ): Long {

        require(isValidDate(date)) {
            "Date must use DD-MM-YYYY format."
        }

        return dao.getActualStudySecondsForDate(date)
    }

    /**
     * Includes elapsed time from a currently running session.
     */
    suspend fun getActualStudySeconds(
        date: String
    ): Long {

        require(isValidDate(date)) {
            "Date must use DD-MM-YYYY format."
        }

        val recordedSeconds = getRecordedStudySeconds(date)

        val runningSession = dao.getRunningSession()

        if (
            runningSession == null ||
            runningSession.studyDate != date
        ) {
            return recordedSeconds
        }

        val currentElapsed = (
                (System.currentTimeMillis() -
                        runningSession.startedAt) / 1000L
                ).coerceAtLeast(0L)

        return recordedSeconds + currentElapsed
    }

    suspend fun getActualStudyDuration(
        date: String
    ): String {

        return formatDuration(
            getActualStudySeconds(date)
        )
    }

    suspend fun getCompletedSessionsForDate(
        date: String
    ): List<DailyStudySessionEntity> {

        require(isValidDate(date)) {
            "Date must use DD-MM-YYYY format."
        }

        return dao.getCompletedSessionsForDate(date)
    }
}
