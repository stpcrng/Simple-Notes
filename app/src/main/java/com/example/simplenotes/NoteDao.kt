package com.example.simplenotes

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    /**
     * Flow автоматически обновляет UI при изменении данных в БД
     */
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAll(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): Note?

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}