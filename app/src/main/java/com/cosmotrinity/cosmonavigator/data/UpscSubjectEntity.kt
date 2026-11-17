
package com.cosmotrinity.cosmonavigator.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "upsc_subject_progress",
    indices = [
        Index(
            value = ["targetExamYear", "category", "subjectName"],
            unique = true
        )
    ]
)
data class UpscSubjectEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val targetExamYear: Int = 2027,

    val category: String,

    val subjectName: String,

    @ColumnInfo(defaultValue = "'Not Started'")
    val status: String = "Not Started",

    val startDate: String = "",

    val targetEndDate: String = "",

    val actualEndDate: String? = null,

    val totalLectures: Int = 0,

    val completedLectures: Int = 0,

    val totalTopics: Int = 0,

    val completedTopics: Int = 0,

    val notes: String = "",

    val updatedAt: Long = System.currentTimeMillis()

) {

    val pendingLectures: Int
        get() = (totalLectures - completedLectures).coerceAtLeast(0)

    val lectureProgress: Float
        get() = if (totalLectures > 0) {
            (completedLectures.toFloat() / totalLectures)
                .coerceIn(0f, 1f)
        } else {
            0f
        }

    val topicProgress: Float
        get() = if (totalTopics > 0) {
            (completedTopics.toFloat() / totalTopics)
                .coerceIn(0f, 1f)
        } else {
            0f
        }

    val isCompleted: Boolean
        get() = status == "Completed"
}
