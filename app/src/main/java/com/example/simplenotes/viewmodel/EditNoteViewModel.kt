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
        viewModelScope.launch {
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
            // Проверка на пустоту
            val hasContent = title.isNotBlank() ||
                    content.isNotBlank() ||
                    checklistItems.any { it.text.isNotBlank() } ||
                    _attachments.value.isNotEmpty()

            if (!hasContent) {
                _saveResult.value = SaveResult.Error("Заметка не может быть пустой")
                return@launch
            }

            // Обновляем заметку
            val updatedNote = note.copy(
                title = title,
                content = content,
                checklistItems = checklistItems,
                updatedAt = System.currentTimeMillis()
            )

            when (val result = noteRepository.updateNote(updatedNote)) {
                is Result.Success -> {
                    _saveResult.value = SaveResult.Success
                }
                is Result.Error -> {
                    _saveResult.value = SaveResult.Error("Ошибка сохранения: ${result.message}")
                }
                else -> {}
            }
        }
    }

    fun addAttachment(attachment: FileAttachment) {
        viewModelScope.launch {
            try {
                val id = attachmentRepository.insert(attachment)
                // Обновляем список вложений
                loadAttachments(currentNoteId)
            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error("Ошибка сохранения файла: ${e.message}")
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