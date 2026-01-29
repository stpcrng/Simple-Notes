package com.example.simplenotes

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.EditText
import android.widget.TextView
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
import com.example.simplenotes.model.NoteType
import com.example.simplenotes.repository.FileAttachmentRepository
import com.example.simplenotes.repository.NoteRepository
import com.example.simplenotes.utils.Result
import com.example.simplenotes.viewmodel.EditNoteViewModel
import com.example.simplenotes.viewmodel.SaveResult
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class EditNoteActivity : AppCompatActivity() {

    private lateinit var viewModel: EditNoteViewModel

    private lateinit var edtTitle: EditText
    private lateinit var edtContent: EditText
    private lateinit var checklistRecycler: RecyclerView
    private lateinit var fileRecycler: RecyclerView
    private lateinit var addFileBtn: ExtendedFloatingActionButton
    private lateinit var fabAddItem: FloatingActionButton
    private lateinit var tvFileCount: TextView

    // ИСПРАВЛЕНО: layoutTextNote и layoutChecklist это View, не LinearLayout!
    private lateinit var layoutTextNote: View
    private lateinit var layoutChecklist: View

    private lateinit var checklistAdapter: ChecklistAdapter
    private lateinit var fileAdapter: FileAttachmentAdapter

    private var currentNote: Note? = null

    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { handleSelectedFile(it) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        val noteId = intent.getLongExtra("note_id", -1)
        val noteTypeName = intent.getStringExtra("note_type")

        val db = AppDatabase.get(this)
        val noteRepo = NoteRepository(db.noteDao())
        val attachmentRepo = FileAttachmentRepository(db.fileAttachmentDao())

        viewModel = ViewModelProvider(
            this,
            EditNoteViewModel.Factory(noteRepo, attachmentRepo)
        )[EditNoteViewModel::class.java]

        setupUI()
        observeViewModel()

        // Загрузка заметки или создание новой
        if (noteId == -1L && noteTypeName != null) {
            // Создаем новую заметку (без сохранения в БД)
            val noteType = try {
                NoteType.valueOf(noteTypeName)
            } catch (e: Exception) {
                NoteType.TEXT
            }
            viewModel.createNewNote(noteType)
        } else if (noteId > 0) {
            // Загружаем существующую заметку
            viewModel.loadNote(noteId)
        } else {
            finish()
            return
        }
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        // Меню уже загружено через app:menu в XML
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_save -> {
                    saveNote()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupUI() {
        setupToolbar()
        
        edtTitle = findViewById(R.id.editTitle)
        edtContent = findViewById(R.id.editContent)
        checklistRecycler = findViewById(R.id.recyclerChecklist)
        fileRecycler = findViewById(R.id.recyclerFiles)
        addFileBtn = findViewById(R.id.btnAttachFile)
        fabAddItem = findViewById(R.id.fabAddItem)
        tvFileCount = findViewById(R.id.tvFileCount)
        
        // Настройка обработки клавиатуры для кнопки прикрепления файла
        setupKeyboardListener()

        // ИСПРАВЛЕНО: используем View вместо LinearLayout
        layoutTextNote = findViewById(R.id.layoutTextNote)
        layoutChecklist = findViewById(R.id.layoutChecklist)

        // Адаптер для чеклиста
        checklistAdapter = ChecklistAdapter(
            items = mutableListOf(),
            onItemChanged = {},
            onDeleteItem = { item ->
                checklistAdapter.removeItem(item)
            }
        )

        checklistRecycler.layoutManager = LinearLayoutManager(this)
        checklistRecycler.adapter = checklistAdapter

        // Адаптер для файлов
        fileAdapter = FileAttachmentAdapter(
            files = mutableListOf(),
            onFileClick = { openFile(it) },
            onFileDelete = { file ->
                // Удаляем из адаптера сразу
                fileAdapter.removeFile(file)
                // Обновляем счетчик
                updateFileCount(fileAdapter.files.size)
                
                // Если файл уже был сохранён в БД (есть id и noteId > 0), удаляем из БД
                if (file.id > 0 && file.noteId > 0) {
                    viewModel.deleteAttachment(file)
                } else {
                    // Если файл еще не сохранен в БД, просто удаляем физический файл
                    try {
                        File(file.filePath).delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
        fileAdapter.setPackageName(packageName)

        fileRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        fileRecycler.adapter = fileAdapter

        // Кнопка добавления файла
        addFileBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            pickFileLauncher.launch(intent)
        }

        // Кнопка добавления элемента чеклиста
        fabAddItem.setOnClickListener {
            val newItem = ChecklistItem(text = "")
            checklistAdapter.addItem(newItem)
        }

        // ИСПРАВЛЕНО: Используем toolbar вместо кнопки сохранения
        // Сохранение через меню или автоматически при onPause
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.note.collectLatest { result ->
                when (result) {
                    is Result.Loading -> {
                        // Показываем прогресс
                    }
                    is Result.Success -> {
                        currentNote = result.data
                        currentNote?.let { note ->
                            edtTitle.setText(note.title)
                            edtContent.setText(note.content)

                            // Показываем нужный layout в зависимости от типа
                            when (note.type) {
                                NoteType.TEXT -> {
                                    layoutTextNote.visibility = View.VISIBLE
                                    layoutChecklist.visibility = View.GONE
                                }
                                NoteType.CHECKLIST -> {
                                    layoutTextNote.visibility = View.GONE
                                    layoutChecklist.visibility = View.VISIBLE
                                    checklistAdapter.updateItems(note.checklistItems)
                                }
                            }
                        }
                    }
                    is Result.Error -> showError("Ошибка загрузки заметки: ${result.message}")
                }
            }
        }

        lifecycleScope.launch {
            viewModel.attachments.collectLatest { files ->
                // Всегда обновляем адаптер, так как Flow может отправлять одинаковые списки
                // но с обновленными объектами (например, после сохранения в БД с новым id)
                fileAdapter.updateFiles(files)
                updateFileCount(files.size)
            }
        }

        lifecycleScope.launch {
            viewModel.saveResult.collectLatest { result ->
                result?.let {
                    when (it) {
                        is SaveResult.Success -> {
                            Toast.makeText(this@EditNoteActivity, "Заметка сохранена", Toast.LENGTH_SHORT).show()
                            // НЕ закрываем экран - остаемся на экране редактирования
                        }
                        is SaveResult.Error -> showError(it.message)
                    }
                    viewModel.clearSaveResult()
                }
            }
        }
    }

    private fun handleSelectedFile(uri: Uri) {
        currentNote?.let { note ->
            try {
                // Проверяем ограничение на количество файлов
                val currentFilesCount = fileAdapter.files.size
                if (currentFilesCount >= 10) {
                    showError("Можно прикрепить максимум 10 файлов")
                    return
                }
                
                val fileName = queryName(uri)
                val fileSize = querySize(uri)
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

                // Копируем файл в internal storage
                val localPath = copyToInternalStorage(uri)

                // Создаём объект вложения
                val attachment = FileAttachment(
                    id = 0, // Временный id, будет установлен после сохранения в БД
                    noteId = note.id, // Используем текущий noteId (может быть 0 для новой заметки)
                    filePath = localPath,
                    fileName = fileName,
                    mimeType = mimeType,
                    fileSize = fileSize
                )

                // Добавляем файл (ViewModel обработает сохранение)
                viewModel.addAttachment(attachment)
                
                // Обновляем счетчик файлов
                updateFileCount(currentFilesCount + 1)

                Toast.makeText(this, "Файл добавлен (${currentFilesCount + 1}/10)", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                showError("Ошибка добавления файла: ${e.message}")
            }
        } ?: run {
            showError("Заметка не загружена")
        }
    }

    private fun copyToInternalStorage(uri: Uri): String {
        val dir = File(filesDir, "attachments")
        dir.mkdirs()

        // Получаем оригинальное имя файла
        val originalFileName = queryName(uri)
        val extension = originalFileName.substringAfterLast('.', "")

        // Создаём уникальное имя файла
        val uniqueFileName = "${UUID.randomUUID()}${if (extension.isNotEmpty()) ".$extension" else ""}"

        val file = File(dir, uniqueFileName)
        contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }

    private fun openFile(file: FileAttachment) {
        try {
            val realFile = File(file.filePath)
            if (!realFile.exists()) {
                showError("Файл не найден")
                return
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                realFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, file.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Проверяем, есть ли приложение для открытия
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(intent, "Открыть файл"))
            } else {
                showError("Нет приложения для открытия этого типа файлов")
            }
        } catch (e: Exception) {
            showError("Не удалось открыть файл: ${e.message}")
        }
    }

    private fun queryName(uri: Uri): String =
        contentResolver.query(uri, null, null, null, null)?.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && index != -1) it.getString(index) else "file"
        } ?: "file"

    private fun querySize(uri: Uri): Long =
        contentResolver.query(uri, null, null, null, null)?.use {
            val index = it.getColumnIndex(OpenableColumns.SIZE)
            if (it.moveToFirst() && index != -1) it.getLong(index) else 0L
        } ?: 0L

    private fun showError(msg: String) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        // При нажатии назад просто закрываем экран без сохранения
        super.onBackPressed()
    }

    private fun saveNote() {
        currentNote?.let { note ->
            val title = edtTitle.text.toString()
            val content = edtContent.text.toString()
            val checklistItems = if (note.type == NoteType.CHECKLIST) {
                checklistAdapter.items.toList()
            } else {
                emptyList()
            }

            viewModel.saveNote(
                note = note,
                title = title,
                content = content,
                checklistItems = checklistItems
            )
        } ?: run {
            showError("Заметка не загружена")
        }
    }
    
    private fun setupKeyboardListener() {
        // Используем WindowInsets для правильной обработки клавиатуры
        val rootView = findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
            val keypadHeight = screenHeight - rect.bottom
            
            // Если клавиатура открыта (keypadHeight > 200dp), обновляем отступ кнопки
            val layoutParams = addFileBtn.layoutParams as? androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
            if (layoutParams != null) {
                layoutParams.bottomMargin = if (keypadHeight > 200) {
                    keypadHeight + 16 // отступ от клавиатуры
                } else {
                    16 // обычный отступ
                }
                addFileBtn.layoutParams = layoutParams
            }
        }
    }
    
    private fun updateFileCount(count: Int) {
        if (count > 0) {
            tvFileCount.text = "Файлов: $count/10"
            tvFileCount.visibility = View.VISIBLE
        } else {
            tvFileCount.visibility = View.GONE
        }
        
        // Обновляем текст кнопки
        if (count >= 10) {
            addFileBtn.text = "Максимум файлов достигнут"
            addFileBtn.isEnabled = false
            addFileBtn.alpha = 0.6f
        } else {
            addFileBtn.text = "Прикрепить файл"
            addFileBtn.isEnabled = true
            addFileBtn.alpha = 1.0f
        }
    }
}