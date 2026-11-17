
package com.cosmotrinity.cosmonavigator.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "upsc_lectures",
    foreignKeys = [
        ForeignKey(
            entity = UpscSubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("subjectId"),
        Index(
            value = ["subjectId", "lectureNumber"],
            unique = true
        )
    ]
)
data class UpscLectureEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val subjectId: Int,

    val lectureNumber: Int,

    val title: String,

    val scheduledDate: String = "",

    val isCompleted: Boolean = false,

    val completedAt: Long? = null,

    val durationMinutes: Int = 0,

    val notes: String = "",

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)
