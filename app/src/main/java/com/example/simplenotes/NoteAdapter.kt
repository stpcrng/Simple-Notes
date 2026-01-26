package com.example.simplenotes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NoteAdapter(
    private val notes: MutableList<Note>,
    private val onClick: (Note) -> Unit,
    private val onLongClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.noteTitle)
        val text: TextView = v.findViewById(R.id.noteSnippet)

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
        h.title.text = notes[i].title.ifBlank { "(Без названия)" }
        h.text.text = notes[i].content.take(80)
    }

    override fun getItemCount() = notes.size

    fun update(newNotes: List<Note>) {
        notes.clear()
        notes.addAll(newNotes)
        notifyDataSetChanged()
    }
}
