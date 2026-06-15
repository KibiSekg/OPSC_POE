package com.example.opsc_poe

import android.os.Bundle
import android.util.Log // Imported Android Log utility
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.opsc_poe.db.AppDatabase
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlin.concurrent.thread

/**
 * A modal overlay dialog sheet interacting from the base of the user window screen.
 * Queries historical thresholds to pre-fill content fields and delegates newly updated double parameters
 * back to parent callers via structured callback parameters.
 */
class SetBudgetBottomSheet(
    private val onBudgetSaved: (Double) -> Unit
) : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "SetBudgetBottomSheet"
    }

    private lateinit var etBudgetAmount: TextInputEditText
    private lateinit var btnSaveBudget:   MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: Inflating bottom sheet content view components framework.")
        // This inflates your existing layout file directly
        return inflater.inflate(R.layout.bottom_sheet_set_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Binding internal widget components and fetching profile defaults.")

        // Binding to the components inside your existing XML
        etBudgetAmount = view.findViewById(R.id.etBudgetAmount)
        btnSaveBudget  = view.findViewById(R.id.btnSaveBudget)

        // Read the existing budget from the database asynchronously to pre-fill the input field
        thread {
            Log.d(TAG, "Worker Thread -> Fetching previous budget metadata from local SQLite repository.")
            val db = AppDatabase.getDatabase(requireContext())
            val currentBudget = db.budgetDao().getLatestBudget()

            // Return to UI main thread context loop to populate input widgets safely
            activity?.runOnUiThread {
                if (currentBudget != null && currentBudget.monthlyBudget > 0) {
                    Log.d(TAG, "UI Thread -> Pre-filling existing threshold data value: ${currentBudget.monthlyBudget}")
                    etBudgetAmount.setText(currentBudget.monthlyBudget.toString())
                } else {
                    Log.d(TAG, "UI Thread -> No historical active budget data records located. Field remains blank.")
                }
            }
        }

        btnSaveBudget.setOnClickListener {
            val budgetString = etBudgetAmount.text.toString().trim()
            Log.d(TAG, "btnSaveBudget clicked. Evaluating incoming parameter payload: '$budgetString'")

            if (budgetString.isEmpty()) {
                Log.w(TAG, "Validation failed: Input container field was left blank.")
                etBudgetAmount.error = "Please enter an amount"
                return@setOnClickListener
            }

            val budgetAmount = budgetString.toDoubleOrNull()
            if (budgetAmount != null && budgetAmount > 0) {
                Log.i(TAG, "Validation SUCCESS. Formulating data routing sequence with double total value: $budgetAmount")

                // Pass the value back to the callback function in Home.kt
                onBudgetSaved(budgetAmount)
                Toast.makeText(requireContext(), "Monthly budget updated!", Toast.LENGTH_SHORT).show()

                Log.d(TAG, "Dismissing active modal bottom sheet context view stack layer.")
                dismiss()
            } else {
                Log.w(TAG, "Validation failed: Data inputs could not be transformed to positive numerical format types.")
                etBudgetAmount.error = "Please enter a valid positive number"
            }
        }
    }
}