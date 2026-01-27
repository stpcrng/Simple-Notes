package com.example.simplenotes.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.example.simplenotes.R
import com.example.simplenotes.model.ChecklistItem

/**
 * Адаптер для списка задач с галочками
 */
class ChecklistAdapter(
    val items: MutableList<ChecklistItem>, // Публичный доступ
    private val onItemChanged: () -> Unit,
    private val onDeleteItem: (ChecklistItem) -> Unit
) : RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {

    inner class ChecklistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: android.widget.CheckBox = view.findViewById(R.id.checkboxItem)
        val editText: android.widget.EditText = view.findViewById(R.id.editTextItem)
        val deleteButton: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btnDeleteItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checklist, parent, false)
        return ChecklistViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        val item = items[position]

        // Убираем предыдущие слушатели
        holder.editText.tag = item.id // Используем tag для идентификации
        holder.checkbox.setOnCheckedChangeListener(null)
        holder.editText.removeTextChangedListener(holder.editText.tag as? TextWatcher)

        // Устанавливаем значения
        holder.checkbox.isChecked = item.isChecked
        holder.editText.setText(item.text)

        // Слушатель checkbox
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            item.isChecked = isChecked
            onItemChanged()
        }

        // Слушатель текста
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                item.text = s.toString()
                onItemChanged()
            }
        }
        holder.editText.addTextChangedListener(textWatcher)
        holder.editText.tag = textWatcher

        // Кнопка удаления
        holder.deleteButton.setOnClickListener {
            onDeleteItem(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<ChecklistItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addItem(item: ChecklistItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun removeItem(item: ChecklistItem) {
        val position = items.indexOf(item)
        if (position != -1) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}