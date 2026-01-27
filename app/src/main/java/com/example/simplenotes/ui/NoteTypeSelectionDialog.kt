package com.example.simplenotes.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.example.simplenotes.R
import com.example.simplenotes.model.NoteType
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Bottom Sheet для выбора типа заметки
 */
class NoteTypeSelectionDialog(
    private val onTypeSelected: (NoteType) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_note_type_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textNoteOption = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.optionTextNote)
        val checklistOption = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.optionChecklist)

        textNoteOption.setOnClickListener {
            onTypeSelected(NoteType.TEXT)
            dismiss()
        }

        checklistOption.setOnClickListener {
            onTypeSelected(NoteType.CHECKLIST)
            dismiss()
        }
    }
}