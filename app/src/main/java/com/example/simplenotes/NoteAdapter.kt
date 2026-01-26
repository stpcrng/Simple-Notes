package com.example.simplenotes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.simplenotes.model.NoteType

class NoteAdapter(
    private val notes: MutableList<Note>,
    private val onClick: (Note) -> Unit,
    private val onLongClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.noteTitle)
        val text: TextView = v.findViewById(R.id.noteSnippet)
        val typeIndicator: View = v.findViewById(R.id.typeIndicator)

        init {
            v.setOnClickListener { onClick(notes[adapterPosition]) }
            v.setOnLongClickListener {
                onLongClick(notes[adapterPosition])
                true
            }
        }
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_note, p, false))

    override fun onBindViewHolder(h: VH, i: Int) {
        val note = notes[i]
        
        h.title.text = note.title.ifBlank { "(Без названия)" }

        // Показываем содержимое в зависимости от типа
        when (note.type) {
            NoteType.TEXT -> {
                h.text.text = note.content.take(80)
                h.typeIndicator.setBackgroundColor(
                    h.itemView.context.getColor(R.color.primary)
                )
            }
            NoteType.CHECKLIST -> {
                // Для чеклиста показываем количество выполненных/всего
                val completed = note.checklistItems.count { it.isChecked }
                val total = note.checklistItems.size
                val preview = note.checklistItems
                    .take(2)
                    .joinToString("\n") { "${if (it.isChecked) "☑" else "☐"} ${it.text.take(40)}" }
                
                h.text.text = "$preview\n\n$completed/$total выполнено"
                h.typeIndicator.setBackgroundColor(
                    h.itemView.context.getColor(R.color.accent)
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