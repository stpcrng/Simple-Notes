package com.example.simplenotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.simplenotes.Note
import com.example.simplenotes.repository.NoteRepository
import com.example.simplenotes.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel для редактирования заметки
 */
class EditNoteViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _note = MutableStateFlow<Result<Note?>>(Result.Loading)
    val note: StateFlow<Result<Note?>> = _note.asStateFlow()

    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult.asStateFlow()

    /**
     * Загрузка заметки по ID
     */
    fun loadNote(noteId: Long) {
        viewModelScope.launch {
            _note.value = Result.Loading
            when (val result = repository.getNoteById(noteId)) {
                is Result.Success -> {
                    if (result.data != null) {
                        _note.value = Result.Success(result.data)
                    } else {
                        _note.value = Result.Error(Exception("Заметка не найдена"))
                    }
                }
                is Result.Error -> {
                    _note.value = Result.Error(result.exception, "Ошибка загрузки заметки")
                }
                else -> {}
            }
        }
    }

    /**
     * Сохранение заметки
     */
    fun saveNote(note: Note, title: String, content: String, checklistItems: List<com.example.simplenotes.model.ChecklistItem>) {
        viewModelScope.launch {
            // Валидация
            if (title.isBlank() && content.isBlank() && checklistItems.all { it.text.isBlank() }) {
                _saveResult.value = SaveResult.Error("Заметка не может быть пустой")
                return@launch
            }

            val updatedNote = note.copy(
                title = title,
                content = content,
                checklistItems = checklistItems,
                updatedAt = System.currentTimeMillis()
            )

            when (val result = repository.updateNote(updatedNote)) {
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

    /**
     * Очистка результата сохранения
     */
    fun clearSaveResult() {
        _saveResult.value = null
    }

    /**
     * Factory для создания ViewModel
     */
    class Factory(private val repository: NoteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EditNoteViewModel::class.java)) {
                return EditNoteViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

/**
 * Результат сохранения
 */
sealed class SaveResult {
    object Success : SaveResult()
    data class Error(val message: String) : SaveResult()
}