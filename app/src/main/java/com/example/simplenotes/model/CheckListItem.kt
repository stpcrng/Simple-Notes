package com.example.simplenotes.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Элемент чеклиста
 */
data class ChecklistItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    var text: String,
    var isChecked: Boolean = false
)

/**
 * Конвертер для сериализации списка в JSON и обратно
 */
class ChecklistConverter {
    private val gson = Gson()

    @androidx.room.TypeConverter
    fun fromChecklistItems(items: List<ChecklistItem>?): String {
        return gson.toJson(items ?: emptyList<ChecklistItem>())
    }

    @androidx.room.TypeConverter
    fun toChecklistItems(json: String): List<ChecklistItem> {
        val type = object : TypeToken<List<ChecklistItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}