package com.example.simplenotes.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.simplenotes.model.FileAttachment
import com.example.simplenotes.Note
import com.example.simplenotes.repository.FileAttachmentDao
import com.example.simplenotes.NoteDao
import com.example.simplenotes.utils.FileManager
import kotlinx.coroutines.launch

/**
 * ViewModel для экрана с деталями заметки
 *
 * ВАЖНО: Если у вас уже есть ViewModel для заметки,
 * просто добавьте методы из этого класса в ваш существующий ViewModel!
 */
class NoteDetailViewModel(
    application: Application,
    private val noteDao: NoteDao,
    private val attachmentDao: FileAttachmentDao
) : AndroidViewModel(application) {

    private val _currentNote = MutableLiveData<Note?>()
    val currentNote: LiveData<Note?> = _currentNote

    private val _attachments = MutableLiveData<List<FileAttachment>>()
    val attachments: LiveData<List<FileAttachment>> = _attachments

    private val _isUploading = MutableLiveData(false)
    val isUploading: LiveData<Boolean> = _isUploading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Загружает заметку и её вложения
     */
    fun loadNote(noteId: Long) {
        viewModelScope.launch {
            try {
                // Загружаем заметку
                val note = noteDao.getById(noteId)
                _currentNote.value = note

                // Загружаем вложения
                val attachmentsList = attachmentDao.getAttachmentsForNoteOnce(noteId)
                _attachments.value = attachmentsList

            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки: ${e.message}"
            }
        }
    }

    /**
     * Добавляет файл к заметке
     */
    fun addAttachment(uri: Uri) {
        viewModelScope.launch {
            try {
                _isUploading.value = true
                _errorMessage.value = null

                val context = getApplication<Application>()

                // Копируем файл в internal storage
                val filePath = FileManager.copyFileToInternalStorage(context, uri)

                if (filePath == null) {
                    _errorMessage.value = "Не удалось скопировать файл"
                    return@launch
                }

                // Получаем информацию о файле
                val file = FileManager.getFile(context, filePath)
                val fileName = getFileNameFromUri(uri)
                val mimeType = FileManager.getMimeType(fileName)
                val fileSize = file.length()

                // Создаём запись в БД
                val noteId = _currentNote.value?.id ?: run {
                    _errorMessage.value = "Заметка не найдена"
                    return@launch
                }

                val attachment = FileAttachment(
                    noteId = noteId,
                    filePath = filePath,
                    fileName = fileName,
                    mimeType = mimeType,
                    fileSize = fileSize
                )

                attachmentDao.insert(attachment)

                // Обновляем список вложений
                loadNote(noteId)

            } catch (e: Exception) {
                _errorMessage.value = "Ошибка добавления файла: ${e.message}"
            } finally {
                _isUploading.value = false
            }
        }
    }

    /**
     * Удаляет вложение
     */
    fun deleteAttachment(attachment: FileAttachment) {
        viewModelScope.launch {
            try {
                // Удаляем файл из файловой системы
                FileManager.deleteFile(getApplication(), attachment.filePath)

                // Удаляем запись из БД
                attachmentDao.delete(attachment)

                // Обновляем список вложений
                _currentNote.value?.id?.let { noteId ->
                    loadNote(noteId)
                }

            } catch (e: Exception) {
                _errorMessage.value = "Ошибка удаления: ${e.message}"
            }
        }
    }

    /**
     * Обновляет заметку
     */
    fun updateNote(title: String, content: String) {
        viewModelScope.launch {
            try {
                _currentNote.value?.let { note ->
                    val updatedNote = note.copy(
                        title = title,
                        content = content,
                        // Добавьте другие поля если они есть в вашей модели Note
                        // например: updatedAt = System.currentTimeMillis()
                    )
                    noteDao.update(updatedNote)
                    _currentNote.value = updatedNote
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка сохранения: ${e.message}"
            }
        }
    }

    /**
     * Удаляет заметку со всеми вложениями
     */
    fun deleteNote(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                _currentNote.value?.let { note ->
                    // Получаем все вложения
                    val attachments = attachmentDao.getAttachmentsForNoteOnce(note.id)

                    // Удаляем файлы
                    attachments.forEach { attachment ->
                        FileManager.deleteFile(getApplication(), attachment.filePath)
                    }

                    // Удаляем заметку (вложения удалятся каскадно благодаря ForeignKey)
                    noteDao.delete(note)

                    onDeleted()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка удаления: ${e.message}"
            }
        }
    }

    /**
     * Очищает сообщение об ошибке
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Получает имя файла из URI
     */
    private fun getFileNameFromUri(uri: Uri): String {
        val context = getApplication<Application>()
        var fileName = "file"

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }

        return fileName
    }
}