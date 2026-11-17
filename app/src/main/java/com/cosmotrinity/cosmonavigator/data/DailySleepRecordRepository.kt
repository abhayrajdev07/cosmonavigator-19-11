
package com.cosmotrinity.cosmonavigator.data

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.Flow

class DailySleepRecordRepository(
    private val dao: DailySleepRecordDao
) {
    private val zoneId: ZoneId
        get() = ZoneId.systemDefault()

    private val dateFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    private val timeFormatter =
        DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

    private fun formatDate(date: LocalDate): String {
        return date.format(dateFormatter)
    }

    private fun toTimestamp(
        date: LocalDate,
        time: LocalTime
    ): Long {
        return date.atTime(time)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    // Selected date = wake-up date.
    // If bedtime is earlier than wake-up time, both are on the same date.
    // Otherwise, bedtime belongs to the previous date.
    private fun getBedtimeDate(
        date: LocalDate,
        sleepTime: LocalTime,
        wakeUpTime: LocalTime
    ): LocalDate {
        return if (sleepTime.isBefore(wakeUpTime)) {
            date
        } else {
            date.minusDays(1)
        }
    }

    suspend fun saveRecordForDate(
        date: LocalDate,
        sleepTime: LocalTime,
        wakeUpTime: LocalTime
    ): DailySleepRecordEntity {

        val dateString = formatDate(date)
        val existingRecord = dao.getRecord(dateString)

        val bedtimeDate = getBedtimeDate(
            date,
            sleepTime,
            wakeUpTime
        )

        val sleepTimestamp = toTimestamp(
            bedtimeDate,
            sleepTime
        )

        val wakeTimestamp = toTimestamp(
            date,
            wakeUpTime
        )

        val record = DailySleepRecordEntity(
            recordDate = dateString,

            plannedWakeUpTimeMinutes =
                wakeUpTime.hour * 60 + wakeUpTime.minute,

            plannedSleepTimeMinutes =
                sleepTime.hour * 60 + sleepTime.minute,

            actualSleepAt = sleepTimestamp,
            actualWakeUpAt = wakeTimestamp,

            createdAt = existingRecord?.createdAt
                ?: System.currentTimeMillis(),

            updatedAt = System.currentTimeMillis()
        )

        dao.saveRecord(record)
        return record
    }

    suspend fun getRecord(
        date: LocalDate
    ): DailySleepRecordEntity? {
        return dao.getRecord(formatDate(date))
    }

    fun observeRecord(
        date: String
    ): Flow<DailySleepRecordEntity?> {
        return dao.observeRecord(date)
    }

    fun observeAllRecords(): Flow<List<DailySleepRecordEntity>> {
        return dao.observeAllRecords()
    }

    fun observeRecordsBetween(
        startDate: String,
        endDate: String
    ): Flow<List<DailySleepRecordEntity>> {
        return dao.observeRecordsBetween(startDate, endDate)
    }

    fun observeLast30Records(): Flow<List<DailySleepRecordEntity>> {
        return dao.observeLast30Records()
    }

    suspend fun deleteRecord(date: LocalDate) {
        dao.deleteRecord(formatDate(date))
    }

    // Calculate sleep duration using the actual stored dates.
    fun calculateSleepDurationMinutes(
        record: DailySleepRecordEntity
    ): Long? {

        val sleepTimestamp =
            record.actualSleepAt ?: return null

        val wakeTimestamp =
            record.actualWakeUpAt ?: return null

        val selectedDate = try {
            LocalDate.parse(record.recordDate, dateFormatter)
        } catch (_: Exception) {
            return null
        }

        val sleepDateTime = Instant.ofEpochMilli(sleepTimestamp)
            .atZone(zoneId)

        val wakeDateTime = Instant.ofEpochMilli(wakeTimestamp)
            .atZone(zoneId)

        // Wake-up must belong to the selected date.
        if (wakeDateTime.toLocalDate() != selectedDate) {
            return null
        }

        // Bedtime may be on the selected date or the previous date.
        val bedtimeDate = sleepDateTime.toLocalDate()

        if (
            bedtimeDate != selectedDate &&
            bedtimeDate != selectedDate.minusDays(1)
        ) {
            return null
        }

        val duration = Duration.between(
            sleepDateTime.toInstant(),
            wakeDateTime.toInstant()
        ).toMinutes()

        return duration.takeIf { it in 1..1440 }
    }

    fun formatSleepDuration(
        durationMinutes: Long?
    ): String {

        if (durationMinutes == null || durationMinutes <= 0L) {
            return "—"
        }

        val hours = durationMinutes / 60L
        val minutes = durationMinutes % 60L

        return "${hours}h ${minutes}m"
    }

    // Always display 12-hour time with AM/PM.
    fun formatTimestamp(timestamp: Long?): String {
        if (timestamp == null) return "—"

        return Instant.ofEpochMilli(timestamp)
            .atZone(zoneId)
            .toLocalTime()
            .format(timeFormatter)
    }
}
