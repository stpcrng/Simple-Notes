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
 * ViewModel для управления состоянием списка заметок
 */
class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _notes = MutableStateFlow<Result<List<Note>>>(Result.Loading)
    val notes: StateFlow<Result<List<Note>>> = _notes.asStateFlow()

    private val _operationResult = MutableStateFlow<OperationResult?>(null)
    val operationResult: StateFlow<OperationResult?> = _operationResult.asStateFlow()

    init {
        loadNotes()
    }

    /**
     * Загрузка заметок
     */
    fun loadNotes() {
        viewModelScope.launch {
            _notes.value = Result.Loading
            // Используем Flow для автоматического обновления
            repository.getAllNotesFlow().collect { result ->
                _notes.value = result
            }
        }
    }

    /**
     * Создание новой заметки
     */
    fun createNote(noteType: com.example.simplenotes.model.NoteType, onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            val note = Note(
                title = "",
                content = "",
                type = noteType,
                checklistItems = if (noteType == com.example.simplenotes.model.NoteType.CHECKLIST) 
                    listOf(com.example.simplenotes.model.ChecklistItem(text = "")) 
                else emptyList(),
                updatedAt = System.currentTimeMillis()
            )
            
            when (val result = repository.insertNote(note)) {
                is Result.Success -> {
                    _operationResult.value = OperationResult.Success("Заметка создана")
                    onSuccess(result.data)
                }
                is Result.Error -> {
                    _operationResult.value = OperationResult.Error("Ошибка создания: ${result.message}")
                }
                else -> {}
            }
        }
    }

    /**
     * Удаление заметки
     */
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            when (val result = repository.deleteNote(note)) {
                is Result.Success -> {
                    _operationResult.value = OperationResult.Success("Заметка удалена")
                }
                is Result.Error -> {
                    _operationResult.value = OperationResult.Error("Ошибка удаления: ${result.message}")
                }
                else -> {}
            }
        }
    }

    /**
     * Очистка результата операции (после показа Toast/Snackbar)
     */
    fun clearOperationResult() {
        _operationResult.value = null
    }

    /**
     * Factory для создания ViewModel с параметрами
     */
    class Factory(private val repository: NoteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
                return NoteViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

/**
 * Результат операций для показа пользователю
 */
sealed class OperationResult {
    data class Success(val message: String) : OperationResult()
    data class Error(val message: String) : OperationResult()
}