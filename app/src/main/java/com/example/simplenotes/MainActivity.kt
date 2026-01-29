package com.example.simplenotes

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.simplenotes.adapter.NoteAdapter
import com.example.simplenotes.model.NoteType
import com.example.simplenotes.repository.NoteRepository
import com.example.simplenotes.ui.NoteTypeSelectionDialog
import com.example.simplenotes.utils.Result
import com.example.simplenotes.viewmodel.NoteViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: NoteViewModel
    private lateinit var adapter: NoteAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAdd: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = AppDatabase.get(this)
        val repository = NoteRepository(db.noteDao())

        viewModel = ViewModelProvider(
            this,
            NoteViewModel.Factory(repository)
        )[NoteViewModel::class.java]

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        recyclerView = findViewById(R.id.recyclerView)
        btnAdd = findViewById(R.id.btnAdd)
        progressBar = findViewById(R.id.progressBar)
        emptyState = findViewById(R.id.emptyState)

        adapter = NoteAdapter(
            notes = mutableListOf(),
            onNoteClick = { note ->
                openEditActivity(note.id)
            },
            onNoteLongClick = { note ->
                showDeleteDialog(note)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnAdd.setOnClickListener {
            showNoteTypeDialog()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.notes.collectLatest { result ->
                when (result) {
                    is Result.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        emptyState.visibility = View.GONE
                    }
                    is Result.Success -> {
                        progressBar.visibility = View.GONE

                        if (result.data.isEmpty()) {
                            emptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        } else {
                            emptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                            adapter.update(result.data)
                        }
                    }
                    is Result.Error -> {
                        progressBar.visibility = View.GONE
                        showError("Ошибка загрузки: ${result.message}")
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.operationResult.collectLatest { result ->
                result?.let {
                    when (it) {
                        is com.example.simplenotes.viewmodel.OperationResult.Success -> {
                            Snackbar.make(findViewById(android.R.id.content), it.message, Snackbar.LENGTH_SHORT).show()
                        }
                        is com.example.simplenotes.viewmodel.OperationResult.Error -> {
                            showError(it.message)
                        }
                    }
                    viewModel.clearOperationResult()
                }
            }
        }
    }

    private fun showNoteTypeDialog() {
        val dialog = NoteTypeSelectionDialog { noteType ->
            createNote(noteType)
        }
        dialog.show(supportFragmentManager, "NoteTypeDialog")
    }

    private fun createNote(noteType: NoteType) {
        viewModel.createNote(noteType) { noteId ->
            openEditActivity(noteId)
        }
    }

    private fun openEditActivity(noteId: Long) {
        val intent = Intent(this, EditNoteActivity::class.java)
        intent.putExtra("note_id", noteId)
        startActivity(intent)
    }

    private fun showDeleteDialog(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Удалить заметку?")
            .setMessage("Вы уверены, что хотите удалить \"${note.title.ifBlank { "(Без названия)" }}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteNote(note)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
}