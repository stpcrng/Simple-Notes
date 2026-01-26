package com.example.simplenotes

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.simplenotes.adapter.ChecklistAdapter
import com.example.simplenotes.model.ChecklistItem
import com.example.simplenotes.model.NoteType
import com.example.simplenotes.repository.NoteRepository
import com.example.simplenotes.utils.Result
import com.example.simplenotes.viewmodel.EditNoteViewModel
import com.example.simplenotes.viewmodel.SaveResult
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class EditNoteActivity : AppCompatActivity() {

    private lateinit var viewModel: EditNoteViewModel
    private lateinit var titleEdit: EditText
    private lateinit var contentEdit: EditText
    private lateinit var checklistRecycler: RecyclerView
    private lateinit var textNoteLayout: LinearLayout
    private lateinit var checklistLayout: ConstraintLayout
    private lateinit var saveBtn: Button
    private lateinit var addItemBtn: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private var currentNote: Note? = null
    private lateinit var checklistAdapter: ChecklistAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        // Инициализация ViewModel
        val db = AppDatabase.get(this)
        val repository = NoteRepository(db.noteDao())
        viewModel = ViewModelProvider(this, EditNoteViewModel.Factory(repository))
            .get(EditNoteViewModel::class.java)

        setupUI()
        loadNote()
        observeViewModel()
    }

    private fun setupUI() {
        titleEdit = findViewById(R.id.editTitle)
        contentEdit = findViewById(R.id.editContent)
        checklistRecycler = findViewById(R.id.recyclerChecklist)
        textNoteLayout = findViewById(R.id.layoutTextNote)
        checklistLayout = findViewById(R.id.layoutChecklist)
        saveBtn = findViewById(R.id.btnSave)
        addItemBtn = findViewById(R.id.fabAddItem)
        progressBar = findViewById(R.id.progressBar)

        // Настройка RecyclerView для чеклиста
        checklistAdapter = ChecklistAdapter(
            items = mutableListOf(),
            onItemChanged = {
                // Автоматически сохраняем при изменении
            },
            onDeleteItem = { item ->
                checklistAdapter.removeItem(item)
            }
        )
        checklistRecycler.layoutManager = LinearLayoutManager(this)
        checklistRecycler.adapter = checklistAdapter

        // Кнопка добавления нового элемента в чеклист
        addItemBtn.setOnClickListener {
            val newItem = ChecklistItem(text = "")
            checklistAdapter.addItem(newItem)
            checklistRecycler.smoothScrollToPosition(checklistAdapter.itemCount - 1)
        }

        saveBtn.setOnClickListener {
            saveNote()
        }
    }

    private fun loadNote() {
        val noteId = intent.getLongExtra("note_id", -1)
        if (noteId != -1L) {
            viewModel.loadNote(noteId)
        } else {
            showError("Ошибка: ID заметки не найден")
            finish()
        }
    }

    private fun observeViewModel() {
        // Наблюдение за загрузкой заметки
        lifecycleScope.launch {
            viewModel.note.collect { result ->
                when (result) {
                    is Result.Loading -> {
                        showLoading(true)
                    }
                    is Result.Success -> {
                        showLoading(false)
                        result.data?.let { note ->
                            currentNote = note
                            displayNote(note)
                        }
                    }
                    is Result.Error -> {
                        showLoading(false)
                        showError("Ошибка загрузки: ${result.message}")
                        finish()
                    }
                }
            }
        }

        // Наблюдение за результатом сохранения
        lifecycleScope.launch {
            viewModel.saveResult.collect { result ->
                result?.let {
                    when (it) {
                        is SaveResult.Success -> {
                            Toast.makeText(this@EditNoteActivity, "Сохранено", Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                        is SaveResult.Error -> {
                            showError(it.message)
                        }
                    }
                    viewModel.clearSaveResult()
                }
            }
        }
    }

    private fun displayNote(note: Note) {
        titleEdit.setText(note.title)

        // Показываем нужный layout в зависимости от типа
        when (note.type) {
            NoteType.TEXT -> {
                textNoteLayout.visibility = View.VISIBLE
                checklistLayout.visibility = View.GONE
                contentEdit.setText(note.content)
                
                if (note.title.isEmpty() && note.content.isEmpty()) {
                    titleEdit.requestFocus()
                }
            }
            NoteType.CHECKLIST -> {
                textNoteLayout.visibility = View.GONE
                checklistLayout.visibility = View.VISIBLE
                checklistAdapter.updateItems(note.checklistItems)
                
                if (note.title.isEmpty()) {
                    titleEdit.requestFocus()
                }
            }
        }
    }

    private fun saveNote() {
        val note = currentNote ?: return
        val title = titleEdit.text.toString()

        when (note.type) {
            NoteType.TEXT -> {
                val content = contentEdit.text.toString()
                viewModel.saveNote(note, title, content, emptyList())
            }
            NoteType.CHECKLIST -> {
                val items = checklistAdapter.items.toList()
                viewModel.saveNote(note, title, "", items)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        titleEdit.isEnabled = !isLoading
        contentEdit.isEnabled = !isLoading
        saveBtn.isEnabled = !isLoading
        addItemBtn.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        // Автосохранение при выходе
        saveNote()
        super.onBackPressed()
    }
}