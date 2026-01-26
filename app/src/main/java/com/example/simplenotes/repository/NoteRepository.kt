package com.example.simplenotes.repository

import com.example.simplenotes.Note
import com.example.simplenotes.NoteDao
import com.example.simplenotes.utils.Result
import com.example.simplenotes.utils.safeCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository - единая точка доступа к данным
 * Инкапсулирует логику работы с БД и обработку ошибок
 */
class NoteRepository(private val noteDao: NoteDao) {

    /**
     * Получение всех заметок с автоматическим обновлением через Flow
     */
    fun getAllNotesFlow(): Flow<Result<List<Note>>> {
        return noteDao.getAllFlow()
            .map<List<Note>, Result<List<Note>>> { Result.Success(it) }
            .catch { emit(Result.Error(Exception(it))) }
    }

    /**
     * Получение всех заметок (одноразово)
     */
    suspend fun getAllNotes(): Result<List<Note>> = withContext(Dispatchers.IO) {
        safeCall { noteDao.getAll() }
    }

    /**
     * Получение заметки по ID
     */
    suspend fun getNoteById(id: Long): Result<Note?> = withContext(Dispatchers.IO) {
        safeCall { noteDao.getById(id) }
    }

    /**
     * Создание новой заметки
     */
    suspend fun insertNote(note: Note): Result<Long> = withContext(Dispatchers.IO) {
        safeCall { noteDao.insert(note) }
    }

    /**
     * Обновление заметки
     */
    suspend fun updateNote(note: Note): Result<Unit> = withContext(Dispatchers.IO) {
        safeCall { noteDao.update(note) }
    }

    /**
     * Удаление заметки
     */
    suspend fun deleteNote(note: Note): Result<Unit> = withContext(Dispatchers.IO) {
        safeCall { noteDao.delete(note) }
    }

    /**
     * Удаление заметки по ID
     */
    suspend fun deleteNoteById(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        safeCall { noteDao.deleteById(id) }
    }
}