package com.example.simplenotes.repository

import com.example.simplenotes.model.FileAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class FileAttachmentRepository(private val dao: FileAttachmentDao) {

    /**
     * Получение вложений для заметки через Flow (автообновление)
     */
    fun getAttachmentsFlow(noteId: Long): Flow<List<FileAttachment>> {
        return dao.getAttachmentsForNote(noteId)
            .catch { emit(emptyList()) }
            .map { it.sortedBy { file -> file.createdAt } }
    }

    /**
     * Получение вложений единоразово
     */
    suspend fun getAttachmentsOnce(noteId: Long): List<FileAttachment> {
        return withContext(Dispatchers.IO) {
            dao.getAttachmentsForNoteOnce(noteId)
        }
    }

    /**
     * Добавление вложения
     * @return id добавленного вложения
     */
    suspend fun insert(attachment: FileAttachment): Long = withContext(Dispatchers.IO) {
        dao.insert(attachment)
    }

    /**
     * Добавление нескольких вложений
     */
    suspend fun insertAll(attachments: List<FileAttachment>) = withContext(Dispatchers.IO) {
        dao.insertAll(attachments)
    }

    /**
     * Удаление вложения
     */
    suspend fun delete(attachment: FileAttachment) = withContext(Dispatchers.IO) {
        dao.delete(attachment)
        // Удаляем физический файл
        try {
            File(attachment.filePath).delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Удаление всех вложений заметки
     */
    suspend fun deleteByNoteId(noteId: Long) = withContext(Dispatchers.IO) {
        // Сначала получаем все файлы для удаления
        val attachments = dao.getAttachmentsForNoteOnce(noteId)

        // Удаляем из БД
        dao.deleteByNoteId(noteId)

        // Удаляем физические файлы
        attachments.forEach { attachment ->
            try {
                File(attachment.filePath).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Получение количества вложений
     */
    suspend fun getAttachmentCount(noteId: Long): Int = withContext(Dispatchers.IO) {
        dao.getAttachmentCount(noteId)
    }

    /**
     * Получение общего размера вложений
     */
    suspend fun getTotalSize(noteId: Long): Long = withContext(Dispatchers.IO) {
        dao.getTotalSize(noteId) ?: 0L
    }

    /**
     * Очистка неиспользуемых файлов
     */
    suspend fun cleanupOrphanedFiles(context: android.content.Context) = withContext(Dispatchers.IO) {
        try {
            val allFilePaths = dao.getAllFilePaths().toSet()
            val attachmentsDir = File(context.filesDir, "attachments")

            if (!attachmentsDir.exists()) return@withContext

            attachmentsDir.listFiles()?.forEach { file ->
                if (file.absolutePath !in allFilePaths) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}