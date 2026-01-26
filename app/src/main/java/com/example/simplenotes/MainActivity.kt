package com.example.simplenotes

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.simplenotes.repository.NoteRepository
import com.example.simplenotes.utils.Result
import com.example.simplenotes.viewmodel.NoteViewModel
import com.example.simplenotes.viewmodel.OperationResult
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: NoteViewModel
    private lateinit var adapter: NoteAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    private val editorLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Автоматически обновляется через Flow, ничего делать не нужно
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация ViewModel
        val db = AppDatabase.get(this)
        val repository = NoteRepository(db.noteDao())
        viewModel = ViewModelProvider(this, NoteViewModel.Factory(repository))
            .get(NoteViewModel::class.java)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        val addBtn = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnAdd)

        adapter = NoteAdapter(
            notes = mutableListOf(),
            onClick = { note ->
                val intent = Intent(this, EditNoteActivity::class.java)
                intent.putExtra("note_id", note.id)
                editorLauncher.launch(intent)
            },
            onLongClick = { note ->
                showDeleteDialog(note)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        addBtn.setOnClickListener {
            // Показываем диалог выбора типа заметки
            val dialog = com.example.simplenotes.ui.NoteTypeSelectionDialog { noteType ->
                viewModel.createNote(noteType) { noteId ->
                    val intent = Intent(this, EditNoteActivity::class.java)
                    intent.putExtra("note_id", noteId)
                    editorLauncher.launch(intent)
                }
            }
            dialog.show(supportFragmentManager, "NoteTypeSelection")
        }
    }

    private fun observeViewModel() {
        // Наблюдение за списком заметок
        lifecycleScope.launch {
            viewModel.notes.collect { result ->
                when (result) {
                    is Result.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    }
                    is Result.Success -> {
                        progressBar.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        adapter.update(result.data)
                    }
                    is Result.Error -> {
                        progressBar.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        showError("Ошибка загрузки заметок: ${result.message}")
                    }
                }
            }
        }

        // Наблюдение за результатами операций
        lifecycleScope.launch {
            viewModel.operationResult.collect { result ->
                result?.let {
                    when (it) {
                        is OperationResult.Success -> {
                            showSuccess(it.message)
                        }
                        is OperationResult.Error -> {
                            showError(it.message)
                        }
                    }
                    viewModel.clearOperationResult()
                }
            }
        }
    }

    private fun showDeleteDialog(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Удалить заметку?")
            .setMessage(if (note.title.isNotBlank()) note.title else "(Без названия)")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteNote(note)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(recyclerView, message, Snackbar.LENGTH_LONG)
            .setAction("Повторить") { viewModel.loadNotes() }
            .show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}