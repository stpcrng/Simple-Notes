package com.example.simplenotes.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.simplenotes.Note

@Entity(
    tableName = "file_attachments",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class FileAttachment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long,
    val filePath: String,   // абсолютный путь в internal storage
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis()
)
