package com.example.simplenotes.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Менеджер для работы с файлами приложения
 * Копирует файлы в internal storage и управляет их жизненным циклом
 */
object FileManager {

    private const val ATTACHMENTS_DIR = "attachments"

    /**
     * Копирует файл из URI в internal storage приложения
     * @return путь к скопированному файлу или null при ошибке
     */
    fun copyFileToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // Получаем оригинальное имя файла
            val originalFileName = getFileName(context, uri)

            // Создаём уникальное имя файла
            val extension = originalFileName.substringAfterLast('.', "")
            val uniqueFileName = "${UUID.randomUUID()}${if (extension.isNotEmpty()) ".$extension" else ""}"

            // Создаём директорию для вложений если её нет
            val attachmentsDir = File(context.filesDir, ATTACHMENTS_DIR)
            if (!attachmentsDir.exists()) {
                attachmentsDir.mkdirs()
            }

            // Создаём файл
            val destinationFile = File(attachmentsDir, uniqueFileName)

            // Копируем содержимое
            inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Возвращаем относительный путь
            "$ATTACHMENTS_DIR/$uniqueFileName"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Получает имя файла из URI
     */
    private fun getFileName(context: Context, uri: Uri): String {
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

    /**
     * Получает полный путь к файлу
     */
    fun getFile(context: Context, relativePath: String): File {
        return File(context.filesDir, relativePath)
    }

    /**
     * Проверяет существование файла
     */
    fun fileExists(context: Context, relativePath: String): Boolean {
        return getFile(context, relativePath).exists()
    }

    /**
     * Удаляет файл
     */
    fun deleteFile(context: Context, relativePath: String): Boolean {
        return try {
            getFile(context, relativePath).delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Получает размер файла в байтах
     */
    fun getFileSize(context: Context, relativePath: String): Long {
        return try {
            getFile(context, relativePath).length()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Форматирует размер файла для отображения
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    /**
     * Определяет MIME тип файла по расширению
     */
    fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            ?: "application/octet-stream"
    }

    /**
     * Проверяет, является ли файл изображением
     */
    fun isImage(fileName: String): Boolean {
        val mimeType = getMimeType(fileName)
        return mimeType.startsWith("image/")
    }

    /**
     * Очищает устаревшие файлы (вызывать при удалении заметок)
     */
    fun cleanupOrphanedFiles(context: Context, usedPaths: Set<String>) {
        try {
            val attachmentsDir = File(context.filesDir, ATTACHMENTS_DIR)
            if (!attachmentsDir.exists()) return

            attachmentsDir.listFiles()?.forEach { file ->
                val relativePath = "$ATTACHMENTS_DIR/${file.name}"
                if (relativePath !in usedPaths) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}