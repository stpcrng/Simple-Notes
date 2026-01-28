package com.example.simplenotes

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.simplenotes.model.ChecklistConverter
import com.example.simplenotes.model.ChecklistItem
import com.example.simplenotes.model.FileAttachment
import com.example.simplenotes.model.NoteType
import com.example.simplenotes.model.NoteTypeConverter

@Entity(tableName = "notes")
@TypeConverters(NoteTypeConverter::class, ChecklistConverter::class)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var title: String,
    var content: String, // Используется для TEXT заметок
    var type: NoteType = NoteType.TEXT,
    var checklistItems: List<ChecklistItem> = emptyList(), // Используется для CHECKLIST
    @Ignore // Room не может хранить списки объектов - загружаем через FileAttachmentDao
    var attachedFiles: List<FileAttachment> = emptyList(), // Прикрепленные файлы
    var updatedAt: Long
) {
    // Конструктор без @Ignore поля (требуется Room)
    constructor(
        id: Long = 0,
        title: String,
        content: String,
        type: NoteType = NoteType.TEXT,
        checklistItems: List<ChecklistItem> = emptyList(),
        updatedAt: Long
    ) : this(id, title, content, type, checklistItems, emptyList(), updatedAt)
}