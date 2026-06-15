package com.example.opsc_poe

import android.os.Bundle
import android.util.Log
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

    // Companion object to hold the logging tag specific to this class
    companion object {
        private const val TAG = "AddCategoryBottomSheet"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: Inflating bottom sheet layout")
        return inflater.inflate(R.layout.layout_add_category_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components from the inflated layout
        val etName  = view.findViewById<TextInputEditText>(R.id.etNewCategoryName)
        val btnAdd  = view.findViewById<MaterialButton>(R.id.btnBottomSheetAdd)

        // Handle the "Add" button click action
        btnAdd.setOnClickListener {
            val name = etName.text.toString().trim()

            Log.d(TAG, "btnAdd clicked. Input value: '$name'")

            when {
                // Validation: Check if the user left the input blank
                name.isEmpty() -> {
                    Log.e(TAG, "Validation failed: Category name is empty.")
                    Toast.makeText(
                        requireContext(),
                        "Please enter a category name",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                // Success: Input is valid
                else -> {
                    Log.d(TAG, "Validation passed. Adding category: '$name'")

                    // Trigger the callback function to pass data back to the parent
                    onCategoryAdded(name)

                    // Close the bottom sheet
                    Log.d(TAG, "Dismissing bottom sheet.")
                    dismiss()
                }
            }
        }
    }
}