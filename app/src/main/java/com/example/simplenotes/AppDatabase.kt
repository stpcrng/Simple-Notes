package com.example.simplenotes

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.simplenotes.model.ChecklistConverter
import com.example.simplenotes.model.NoteTypeConverter

@Database(entities = [Note::class], version = 2, exportSchema = false)
@TypeConverters(NoteTypeConverter::class, ChecklistConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Миграция с версии 1 на версию 2
         * Добавляет поля type и checklistItems
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Добавляем поле type (по умолчанию TEXT)
                database.execSQL("ALTER TABLE notes ADD COLUMN type TEXT NOT NULL DEFAULT 'TEXT'")
                
                // Добавляем поле checklistItems (по умолчанию пустой список)
                database.execSQL("ALTER TABLE notes ADD COLUMN checklistItems TEXT NOT NULL DEFAULT '[]'")
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "simplenotes.db"
                )
                    .addMigrations(MIGRATION_1_2) // Добавляем миграцию
                    .fallbackToDestructiveMigration() // На случай других миграций
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}