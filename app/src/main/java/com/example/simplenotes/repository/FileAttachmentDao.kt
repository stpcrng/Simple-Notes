package com.example.simplenotes.repository

import androidx.room.*
import com.example.simplenotes.model.FileAttachment
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с файловыми вложениями
 * Добавьте этот DAO в ваш AppDatabase
 */
@Dao
interface FileAttachmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: FileAttachment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attachments: List<FileAttachment>)

    @Update
    suspend fun update(attachment: FileAttachment)

    @Delete
    suspend fun delete(attachment: FileAttachment)

    @Query("DELETE FROM file_attachments WHERE id = :attachmentId")
    suspend fun deleteById(attachmentId: Long)

    @Query("DELETE FROM file_attachments WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: Long)

    @Query("SELECT * FROM file_attachments WHERE noteId = :noteId ORDER BY timestamp ASC")
    fun getAttachmentsForNote(noteId: Long): Flow<List<FileAttachment>>

    @Query("SELECT * FROM file_attachments WHERE noteId = :noteId ORDER BY timestamp ASC")
    suspend fun getAttachmentsForNoteOnce(noteId: Long): List<FileAttachment>

    @Query("SELECT * FROM file_attachments WHERE id = :attachmentId")
    suspend fun getAttachmentById(attachmentId: Long): FileAttachment?

    @Query("SELECT COUNT(*) FROM file_attachments WHERE noteId = :noteId")
    suspend fun getAttachmentCount(noteId: Long): Int

    @Query("SELECT SUM(fileSize) FROM file_attachments WHERE noteId = :noteId")
    suspend fun getTotalSize(noteId: Long): Long?

    @Query("SELECT filePath FROM file_attachments")
    suspend fun getAllFilePaths(): List<String>
}