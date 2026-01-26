package com.example.simplenotes

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: NoteAdapter
    private val notes = mutableListOf<Note>()

    private val editorLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            loadNotes()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.get(this)

        val recycler = findViewById<RecyclerView>(R.id.recyclerView)
        val addBtn = findViewById<Button>(R.id.btnAdd)

        adapter = NoteAdapter(notes,
            onClick = {
                val i = Intent(this, EditNoteActivity::class.java)
                i.putExtra("note_id", it.id)
                editorLauncher.launch(i)
            },
            onLongClick = {
                AlertDialog.Builder(this)
                    .setTitle("Удалить заметку?")
                    .setPositiveButton("Удалить") { _, _ ->
                        db.noteDao().delete(it)
                        loadNotes()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            })

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        addBtn.setOnClickListener {
            val id = db.noteDao().insert(
                Note(
                    title = "",
                    content = "",
                    updatedAt = System.currentTimeMillis()
                )

            )
            val i = Intent(this, EditNoteActivity::class.java)
            i.putExtra("note_id", id)
            editorLauncher.launch(i)
        }

        loadNotes()
    }

    private fun loadNotes() {
        notes.clear()
        notes.addAll(db.noteDao().getAll())
        adapter.update(notes)
    }
}
