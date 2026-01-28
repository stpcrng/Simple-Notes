package com.example.simplenotes

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.simplenotes.adapter.ChecklistAdapter
import com.example.simplenotes.adapter.FileAttachmentAdapter
import com.example.simplenotes.model.ChecklistItem
import com.example.simplenotes.model.FileAttachment
import com.example.simplenotes.model.FileType
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
    private lateinit var filesRecycler: RecyclerView
    private lateinit var textNoteLayout: android.view.View
    private lateinit var checklistLayout: android.view.View
    private lateinit var saveBtn: Button
    private lateinit var addItemBtn: FloatingActionButton
    private lateinit var attachFileBtn: Button
    private lateinit var progressBar: android.widget.ProgressBar
    private var currentNote: Note? = null
    private lateinit var checklistAdapter: ChecklistAdapter
    private lateinit var fileAdapter: FileAttachmentAdapter

    // Launcher для выбора файлов
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

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
        filesRecycler = findViewById(R.id.recyclerFiles)
        textNoteLayout = findViewById(R.id.layoutTextNote)
        checklistLayout = findViewById(R.id.layoutChecklist)
        saveBtn = findViewById(R.id.btnSave)
        addItemBtn = findViewById(R.id.fabAddItem)
        attachFileBtn = findViewById(R.id.btnAttachFile)
        progressBar = findViewById(R.id.progressBar)

        // Адаптер для чеклиста
        checklistAdapter = ChecklistAdapter(
            items = mutableListOf(),
            onItemChanged = {},
            onDeleteItem = { item -> checklistAdapter.removeItem(item) }
        )
        checklistRecycler.layoutManager = LinearLayoutManager(this)
        checklistRecycler.adapter = checklistAdapter

        // Адаптер для файлов
        fileAdapter = FileAttachmentAdapter(
            files = mutableListOf(),
            onFileClick = { file -> openFile(file) },
            onFileDelete = { file -> fileAdapter.removeFile(file) }
        )
        filesRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        filesRecycler.adapter = fileAdapter

        addItemBtn.setOnClickListener {
            val newItem = ChecklistItem(text = "")
            checklistAdapter.addItem(newItem)
            checklistRecycler.smoothScrollToPosition(checklistAdapter.itemCount - 1)
        }

        attachFileBtn.setOnClickListener {
            filePickerLauncher.launch("*/*")
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
        lifecycleScope.launch {
            viewModel.note.collect { result ->
                when (result) {
                    is Result.Loading -> showLoading(true)
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

        lifecycleScope.launch {
            viewModel.saveResult.collect { result ->
                result?.let {
                    when (it) {
                        is SaveResult.Success -> {
                            Toast.makeText(this@EditNoteActivity, "Сохранено", Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                        is SaveResult.Error -> showError(it.message)
                    }
                    viewModel.clearSaveResult()
                }
            }
        }
    }

    private fun displayNote(note: Note) {
        titleEdit.setText(note.title)
        fileAdapter.updateFiles(note.attachedFiles)

        when (note.type) {
            NoteType.TEXT -> {
                textNoteLayout.visibility = View.VISIBLE
                checklistLayout.visibility = View.GONE
                contentEdit.setText(note.content)
                textNoteLayout.alpha = 0f
                textNoteLayout.animate().alpha(1f).setDuration(200).start()
                if (note.title.isEmpty() && note.content.isEmpty()) {
                    titleEdit.requestFocus()
                }
            }
            NoteType.CHECKLIST -> {
                textNoteLayout.visibility = View.GONE
                checklistLayout.visibility = View.VISIBLE
                checklistAdapter.updateItems(note.checklistItems)
                checklistLayout.alpha = 0f
                checklistLayout.animate().alpha(1f).setDuration(200).start()
                if (note.title.isEmpty()) {
                    titleEdit.requestFocus()
                }
            }
        }
    }

    private fun handleSelectedFile(uri: Uri) {
        try {
            val note = currentNote ?: return

            val fileName = getFileName(uri)
            val fileSize = getFileSize(uri)
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

            // сохраняем доступ
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            val attachment = FileAttachment(
                noteId = note.id,
                filePath = uri.toString(),
                fileName = fileName,
                mimeType = mimeType,
                fileSize = fileSize
            )

            fileAdapter.addFile(attachment)
            Toast.makeText(this, "Файл добавлен: $fileName", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            showError("Ошибка добавления файла: ${e.message}")
        }
    }


    private fun getFileName(uri: Uri): String {
        var name = "unknown"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
        return name
    }

    private fun getFileSize(uri: Uri): Long {
        var size = 0L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }
        return size
    }

    private fun openFile(file: FileAttachment) {
        try {
            val uri = Uri.parse(file.filePath)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, file.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Открыть файл"))
        } catch (e: Exception) {
            showError("Не удалось открыть файл")
        }
    }


    private fun saveNote() {
        val note = currentNote ?: return
        val title = titleEdit.text.toString()
        val files = fileAdapter.files.toList()

        when (note.type) {
            NoteType.TEXT -> {
                val content = contentEdit.text.toString()
                viewModel.saveNote(note, title, content, emptyList(), files)
            }
            NoteType.CHECKLIST -> {
                val items = checklistAdapter.items.toList()
                viewModel.saveNote(note, title, "", items, files)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        titleEdit.isEnabled = !isLoading
        contentEdit.isEnabled = !isLoading
        saveBtn.isEnabled = !isLoading
        addItemBtn.isEnabled = !isLoading
        attachFileBtn.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        saveNote()
        super.onBackPressed()
    }
}