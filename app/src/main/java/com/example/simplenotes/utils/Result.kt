package com.example.simplenotes.utils

/**
 * Sealed class для обработки результатов операций
 * Позволяет явно обрабатывать успех и ошибки
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception, val message: String? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

/**
 * Extension функция для безопасного выполнения операций
 */
suspend fun <T> safeCall(
    call: suspend () -> T
): Result<T> {
    return try {
        Result.Success(call())
    } catch (e: Exception) {
        Result.Error(e, e.message)
    }
}