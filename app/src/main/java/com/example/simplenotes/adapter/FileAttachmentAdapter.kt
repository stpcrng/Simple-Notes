package com.example.simplenotes.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.simplenotes.R
import com.example.simplenotes.model.FileAttachment
import com.example.simplenotes.model.FileType

/**
 * Адаптер для отображения прикреплённых файлов
 */
class FileAttachmentAdapter(
    val files: MutableList<FileAttachment>,
    private val onFileClick: (FileAttachment) -> Unit,
    private val onFileDelete: (FileAttachment) -> Unit
) : RecyclerView.Adapter<FileAttachmentAdapter.FileViewHolder>() {

    inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.fileIcon)
        val thumbnail: ImageView = view.findViewById(R.id.fileThumbnail)
        val name: TextView = view.findViewById(R.id.fileName)
        val size: TextView = view.findViewById(R.id.fileSize)
        val deleteBtn: ImageButton = view.findViewById(R.id.btnDeleteFile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file_attachment, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]

        holder.name.text = file.fileName
        holder.size.text = formatFileSize(file.fileSize)

        val fileType = FileType.fromMimeType(file.mimeType)

        when (fileType) {
            FileType.IMAGE -> {
                holder.icon.visibility = View.GONE
                holder.thumbnail.visibility = View.VISIBLE
                holder.thumbnail.setImageURI(Uri.parse(file.filePath))
            }

            FileType.DOCUMENT -> {
                holder.thumbnail.visibility = View.GONE
                holder.icon.visibility = View.VISIBLE
                holder.icon.setImageResource(android.R.drawable.ic_menu_agenda)
            }

            FileType.AUDIO -> {
                holder.thumbnail.visibility = View.GONE
                holder.icon.visibility = View.VISIBLE
                holder.icon.setImageResource(android.R.drawable.ic_btn_speak_now)
            }

            FileType.VIDEO -> {
                holder.thumbnail.visibility = View.GONE
                holder.icon.visibility = View.VISIBLE
                holder.icon.setImageResource(android.R.drawable.ic_media_play)
            }

            FileType.OTHER -> {
                holder.thumbnail.visibility = View.GONE
                holder.icon.visibility = View.VISIBLE
                holder.icon.setImageResource(android.R.drawable.ic_menu_info_details)
            }
        }

        // Открыть файл
        holder.itemView.setOnClickListener {
            onFileClick(file)
        }

        // Удалить файл
        holder.deleteBtn.setOnClickListener {
            onFileDelete(file)
        }
    }

    override fun getItemCount(): Int = files.size

    fun addFile(file: FileAttachment) {
        files.add(file)
        notifyItemInserted(files.size - 1)
    }

    fun removeFile(file: FileAttachment) {
        val position = files.indexOf(file)
        if (position != -1) {
            files.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateFiles(newFiles: List<FileAttachment>) {
        files.clear()
        files.addAll(newFiles)
        notifyDataSetChanged()
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}
