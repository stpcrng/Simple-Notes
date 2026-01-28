package com.example.simplenotes.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.simplenotes.Note

/**
 * Модель файлового вложения
 * Хранит локальный путь вместо URI для надёжности
 */
@Entity(
    tableName = "file_attachments",
    foreignKeys = [
        ForeignKey(
            entity = Note::class, // Используйте вашу существующую модель Note
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"])]
)
data class FileAttachment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val noteId: Long,

    /**
     * Относительный путь к файлу в internal storage
     * Например: "attachments/uuid.jpg"
     */
    val filePath: String,

    /**
     * Оригинальное имя файла (для отображения пользователю)
     */
    val fileName: String,

    /**
     * MIME тип файла
     */
    val mimeType: String,

    /**
     * Размер файла в байтах
     */
    val fileSize: Long,

    /**
     * Временная метка добавления
     */
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Проверяет, является ли файл изображением
     */
    fun isImage(): Boolean {
        return mimeType.startsWith("image/")
    }

    /**
     * Получает расширение файла
     */
    fun getExtension(): String {
        return fileName.substringAfterLast('.', "").uppercase()
    }
}