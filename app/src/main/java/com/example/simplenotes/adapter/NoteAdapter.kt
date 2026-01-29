package com.example.simplenotes.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.simplenotes.R
import com.example.simplenotes.Note
import com.example.simplenotes.model.NoteType

class NoteAdapter(
    private val notes: MutableList<Note>,
    private val onNoteClick: (Note) -> Unit,
    private val onNoteLongClick: ((Note) -> Unit)? = null
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.noteTitle)
        val snippet: TextView = view.findViewById(R.id.noteSnippet)
        val typeIndicator: View = view.findViewById(R.id.typeIndicator)

        init {
            view.setOnClickListener {
                onNoteClick(notes[adapterPosition])
            }

            onNoteLongClick?.let { callback ->
                view.setOnLongClickListener {
                    callback(notes[adapterPosition])
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]

        // Заголовок
        holder.title.text = note.title.ifBlank { "(Без названия)" }

        // Контент в зависимости от типа
        when (note.type) {
            NoteType.TEXT -> {
                holder.snippet.text = note.content.take(100)
                holder.typeIndicator.setBackgroundColor(
                    holder.itemView.context.getColor(R.color.primary)
                )
            }
            NoteType.CHECKLIST -> {
                val completed = note.checklistItems.count { it.isChecked }
                val total = note.checklistItems.size

                val preview = if (note.checklistItems.isNotEmpty()) {
                    note.checklistItems
                        .take(2)
                        .joinToString("\n") {
                            "${if (it.isChecked) "☑" else "☐"} ${it.text.take(40)}"
                        } + "\n\n$completed/$total выполнено"
                } else {
                    "Пустой список"
                }

                holder.snippet.text = preview
                holder.typeIndicator.setBackgroundColor(
                    holder.itemView.context.getColor(R.color.accent)
                )
            }
        }
    }

    override fun getItemCount() = notes.size

    fun update(newNotes: List<Note>) {
        notes.clear()
        notes.addAll(newNotes)
        notifyDataSetChanged()
    }
}