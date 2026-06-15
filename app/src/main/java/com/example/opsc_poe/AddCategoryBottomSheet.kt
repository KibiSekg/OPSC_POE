package com.example.opsc_poe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AddCategoryBottomSheet(
    private val onCategoryAdded: (String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.layout_add_category_bottom_sheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etName  = view.findViewById<TextInputEditText>(R.id.etNewCategoryName)
        val btnAdd  = view.findViewById<MaterialButton>(R.id.btnBottomSheetAdd)

        btnAdd.setOnClickListener {
            val name = etName.text.toString().trim()
            when {
                name.isEmpty() -> Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show()
                else -> {
                    onCategoryAdded(name)
                    dismiss()
                }
            }
        }
    }
}