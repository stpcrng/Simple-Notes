package com.example.simplenotes.model

/**
 * Типы заметок в приложении
 */
enum class NoteType {
    TEXT,      // Обычная текстовая заметка
    CHECKLIST  // Список с галочками
}

/**
 * Конвертеры для Room Database
 */
class NoteTypeConverter {
    @androidx.room.TypeConverter
    fun fromNoteType(type: NoteType): String {
        return type.name
    }

    @androidx.room.TypeConverter
    fun toNoteType(value: String): NoteType {
        return try {
            NoteType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            NoteType.TEXT // По умолчанию TEXT
        }
    }
}