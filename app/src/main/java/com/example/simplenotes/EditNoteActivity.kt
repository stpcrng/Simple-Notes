package com.example.simplenotes

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class EditNoteActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var note: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        db = AppDatabase.get(this)

        val title = findViewById<EditText>(R.id.editTitle)
        val content = findViewById<EditText>(R.id.editContent)
        val save = findViewById<Button>(R.id.btnSave)

        val id = intent.getLongExtra("note_id", -1)
        note = db.noteDao().getAll().find { it.id == id }

        note?.let {
            title.setText(it.title)
            content.setText(it.content)
        }

        save.setOnClickListener {
            note?.apply {
                this.title = title.text.toString()
                this.content = content.text.toString()
                this.updatedAt = System.currentTimeMillis()
                db.noteDao().update(this)
            }
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}
