package com.example.simplenotes.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Прикрепленный файл к заметке
 */
data class FileAttachment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: String, // URI файла
    val name: String, // Имя файла
    val type: FileType, // Тип файла
    val size: Long = 0L, // Размер в байтах
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Типы файлов
 */
enum class FileType {
    IMAGE,      // Изображения
    DOCUMENT,   // Документы (PDF, DOC, TXT)
    AUDIO,      // Аудио
    VIDEO,      // Видео
    OTHER;      // Другие файлы

    companion object {
        fun fromMimeType(mimeType: String?): FileType {
            return when {
                mimeType == null -> OTHER
                mimeType.startsWith("image/") -> IMAGE
                mimeType.startsWith("audio/") -> AUDIO
                mimeType.startsWith("video/") -> VIDEO
                mimeType == "application/pdf" ||
                        mimeType.startsWith("application/msword") ||
                        mimeType.startsWith("application/vnd.openxmlformats") ||
                        mimeType == "text/plain" -> DOCUMENT
                else -> OTHER
            }
        }
    }
}

/**
 * Конвертер для сериализации списка файлов в JSON
 */
class FileAttachmentConverter {
    private val gson = Gson()

    @androidx.room.TypeConverter
    fun fromFileAttachments(files: List<FileAttachment>?): String {
        return gson.toJson(files ?: emptyList<FileAttachment>())
    }

    @androidx.room.TypeConverter
    fun toFileAttachments(json: String): List<FileAttachment> {
        val type = object : TypeToken<List<FileAttachment>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}