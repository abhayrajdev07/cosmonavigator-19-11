
package com.cosmotrinity.cosmonavigator.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySleepRecordDao {

    // SAVE SINGLE RECORD
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRecord(record: DailySleepRecordEntity)

    // GET SINGLE RECORD
    @Query(
        """
        SELECT * FROM daily_sleep_records
        WHERE recordDate = :date
        LIMIT 1
        """
    )
    suspend fun getRecord(date: String): DailySleepRecordEntity?

    // OBSERVE SINGLE RECORD
    @Query(
        """
        SELECT * FROM daily_sleep_records
        WHERE recordDate = :date
        LIMIT 1
        """
    )
    fun observeRecord(date: String): Flow<DailySleepRecordEntity?>

    // OBSERVE ALL RECORDS
    @Query(
        """
        SELECT * FROM daily_sleep_records
        ORDER BY recordDate DESC
        """
    )
    fun observeAllRecords(): Flow<List<DailySleepRecordEntity>>

    // OBSERVE RECORDS BETWEEN DATES
    @Query(
        """
        SELECT * FROM daily_sleep_records
        WHERE recordDate BETWEEN :startDate AND :endDate
        ORDER BY recordDate ASC
        """
    )
    fun observeRecordsBetween(
        startDate: String,
        endDate: String
    ): Flow<List<DailySleepRecordEntity>>

    // OBSERVE LAST 30 RECORDS
    @Query(
        """
        SELECT * FROM daily_sleep_records
        ORDER BY recordDate DESC
        LIMIT 30
        """
    )
    fun observeLast30Records(): Flow<List<DailySleepRecordEntity>>

    // DELETE SINGLE RECORD
    @Query(
        """
        DELETE FROM daily_sleep_records
        WHERE recordDate = :date
        """
    )
    suspend fun deleteRecord(date: String)

    // BACKUP: GET ALL RECORDS
    @Query(
        """
        SELECT * FROM daily_sleep_records
        ORDER BY recordDate ASC
        """
    )
    suspend fun getAllRecordsForBackup(): List<DailySleepRecordEntity>

    // RESTORE: INSERT ALL RECORDS
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecordsForRestore(
        records: List<DailySleepRecordEntity>
    )

    // RESTORE: DELETE ALL EXISTING RECORDS
    @Query("DELETE FROM daily_sleep_records")
    suspend fun deleteAllRecordsForRestore()
}
