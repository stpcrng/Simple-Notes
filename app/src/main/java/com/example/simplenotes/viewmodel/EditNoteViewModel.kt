package com.example.simplenotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.simplenotes.Note
import com.example.simplenotes.model.ChecklistItem
import com.example.simplenotes.model.FileAttachment
import com.example.simplenotes.repository.FileAttachmentRepository
import com.example.simplenotes.repository.NoteRepository
import com.example.simplenotes.utils.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditNoteViewModel(
    private val noteRepository: NoteRepository,
    private val attachmentRepository: FileAttachmentRepository
) : ViewModel() {

    private val _note = MutableStateFlow<Result<Note?>>(Result.Loading)
    val note: StateFlow<Result<Note?>> = _note.asStateFlow()

    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult.asStateFlow()

    private val _attachments = MutableStateFlow<List<FileAttachment>>(emptyList())
    val attachments: StateFlow<List<FileAttachment>> = _attachments.asStateFlow()

    private var currentNoteId: Long = -1
    private var attachmentsJob: Job? = null

    /**
     * Создает новую временную заметку (без сохранения в БД)
     */
    fun createNewNote(noteType: com.example.simplenotes.model.NoteType) {
        currentNoteId = -1 // Временная заметка
        val newNote = Note(
            id = 0,
            title = "",
            content = "",
            type = noteType,
            checklistItems = if (noteType == com.example.simplenotes.model.NoteType.CHECKLIST) 
                listOf(ChecklistItem(text = "")) 
            else emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        _note.value = Result.Success(newNote)
        _attachments.value = emptyList()
    }

    fun loadNote(noteId: Long) {
        currentNoteId = noteId
        viewModelScope.launch {
            _note.value = Result.Loading

            // Загружаем заметку
            when (val result = noteRepository.getNoteById(noteId)) {
                is Result.Success -> {
                    _note.value = Result.Success(result.data)

                    // Загружаем вложения
                    result.data?.let {
                        loadAttachments(it.id)
                    }
                }
                is Result.Error -> {
                    _note.value = Result.Error(result.exception, "Ошибка загрузки заметки")
                }
                else -> {}
            }
        }
    }

    private fun loadAttachments(noteId: Long) {
        // Отменяем предыдущую подписку, если она есть
        attachmentsJob?.cancel()
        
        attachmentsJob = viewModelScope.launch {
            attachmentRepository.getAttachmentsFlow(noteId).collect { files ->
                _attachments.value = files
            }
        }
    }

    fun saveNote(
        note: Note,
        title: String,
        content: String,
        checklistItems: List<ChecklistItem>
    ) {
        viewModelScope.launch {
            try {
                val updatedNote = note.copy(
                    title = title.ifBlank { "Без названия" },
                    content = content,
                    checklistItems = checklistItems,
                    updatedAt = System.currentTimeMillis()
                )

                if (currentNoteId <= 0) {
                    // Создаем новую заметку
                    when (val insertResult = noteRepository.insertNote(updatedNote)) {
                        is Result.Success -> {
                            val savedNoteId = insertResult.data
                            
                            // Сохраняем временные файлы (с noteId = 0)
                            if (savedNoteId > 0) {
                                saveTemporaryAttachments(savedNoteId)
                                currentNoteId = savedNoteId
                                
                                // Обновляем заметку с правильным ID
                                val finalNote = updatedNote.copy(id = savedNoteId)
                                _note.value = Result.Success(finalNote)
                                
                                // Перезагружаем вложения из БД
                                loadAttachments(savedNoteId)
                            }
                            
                            _saveResult.value = SaveResult.Success
                        }
                        is Result.Error -> {
                            _saveResult.value = SaveResult.Error("Ошибка сохранения: ${insertResult.message}")
                        }
                        else -> {}
                    }
                } else {
                    // Обновляем существующую заметку
                    when (val updateResult = noteRepository.updateNote(updatedNote)) {
                        is Result.Success -> {
                            // Сохраняем временные файлы (с noteId = 0)
                            saveTemporaryAttachments(currentNoteId)
                            
                            // Обновляем заметку
                            _note.value = Result.Success(updatedNote)
                            
                            // Перезагружаем вложения из БД
                            loadAttachments(currentNoteId)
                            
                            _saveResult.value = SaveResult.Success
                        }
                        is Result.Error -> {
                            _saveResult.value = SaveResult.Error("Ошибка сохранения: ${updateResult.message}")
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error("Ошибка сохранения: ${e.message}")
            }
        }
    }

    fun addAttachment(attachment: FileAttachment) {
        viewModelScope.launch {
            try {
                // Проверяем ограничение на количество файлов (максимум 10)
                val currentFilesCount = _attachments.value.size
                if (currentFilesCount >= 10) {
                    _saveResult.value = SaveResult.Error("Можно прикрепить максимум 10 файлов")
                    return@launch
                }
                
                // Убеждаемся, что noteId правильный
                val noteId = if (attachment.noteId > 0) attachment.noteId else currentNoteId
                if (noteId <= 0) {
                    // Если заметка еще не сохранена, добавляем файл во временный список
                    // Файл будет сохранен после сохранения заметки
                    val tempAttachment = attachment.copy(noteId = 0)
                    _attachments.value = _attachments.value + tempAttachment
                    return@launch
                }
                
                val attachmentWithCorrectNoteId = attachment.copy(noteId = noteId)
                attachmentRepository.insert(attachmentWithCorrectNoteId)
                // Список обновится автоматически через Flow
            } catch (e: Exception) {
                e.printStackTrace()
                _saveResult.value = SaveResult.Error("Ошибка сохранения файла: ${e.message}")
            }
        }
    }
    
    /**
     * Сохраняет временные файлы (с noteId = 0) после сохранения заметки
     */
    private suspend fun saveTemporaryAttachments(noteId: Long) {
        val temporaryAttachments = _attachments.value.filter { it.noteId == 0L }
        if (temporaryAttachments.isNotEmpty()) {
            temporaryAttachments.forEach { tempAttachment ->
                try {
                    val attachmentWithNoteId = tempAttachment.copy(noteId = noteId)
                    attachmentRepository.insert(attachmentWithNoteId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun deleteAttachment(file: FileAttachment) {
        viewModelScope.launch {
            try {
                attachmentRepository.delete(file)
                // Список обновится автоматически через Flow
            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error("Ошибка удаления файла: ${e.message}")
            }
        }
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }

    class Factory(
        private val noteRepository: NoteRepository,
        private val attachmentRepository: FileAttachmentRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EditNoteViewModel::class.java)) {
                return EditNoteViewModel(noteRepository, attachmentRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

sealed class SaveResult {
    object Success : SaveResult()
    data class Error(val message: String) : SaveResult()
}