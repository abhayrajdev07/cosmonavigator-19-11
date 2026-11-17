
package com.cosmotrinity.cosmonavigator.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_tasks")
data class StudyTaskEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Task information
    val title: String,
    val subject: String,
    val taskType: String,

    // Scheduling
    val scheduledDate: String,
    val durationMinutes: Int,

    // Priority: High, Medium, Low
    val priority: String,

    // Completion tracking
    val isCompleted: Boolean = false,

    // Creation timestamp
    val createdAt: Long = System.currentTimeMillis(),

    // Completion timestamp
    val completedAt: Long? = null
)
