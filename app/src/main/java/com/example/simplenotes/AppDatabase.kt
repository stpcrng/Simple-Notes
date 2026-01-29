package com.example.simplenotes

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.simplenotes.model.ChecklistConverter
import com.example.simplenotes.model.NoteTypeConverter
import com.example.simplenotes.repository.FileAttachmentDao

@Database(
    entities = [Note::class, com.example.simplenotes.model.FileAttachment::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(NoteTypeConverter::class, ChecklistConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun fileAttachmentDao(): FileAttachmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN type TEXT NOT NULL DEFAULT 'TEXT'")
                db.execSQL("ALTER TABLE notes ADD COLUMN checklistItems TEXT NOT NULL DEFAULT '[]'")
            }
        }

    //    private val MIGRATION_2_3 = object : Migration(2, 3) {
    //        override fun migrate(db: SupportSQLiteDatabase) {
    //            db.execSQL("ALTER TABLE notes ADD COLUMN attachedFiles TEXT NOT NULL DEFAULT '[]'")
    //        }
    //    }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "simplenotes.db"
                )
                    .addMigrations(MIGRATION_1_2/*, MIGRATION_2_3*/)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
