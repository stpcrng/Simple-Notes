package com.example.simplenotes

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.simplenotes.model.ChecklistConverter
import com.example.simplenotes.model.ChecklistItem
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
    var updatedAt: Long
)