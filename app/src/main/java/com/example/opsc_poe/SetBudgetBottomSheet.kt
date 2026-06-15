package com.example.opsc_poe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.opsc_poe.db.AppDatabase
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlin.concurrent.thread

class SetBudgetBottomSheet(
    private val onBudgetSaved: (Double) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var etBudgetAmount: TextInputEditText
    private lateinit var btnSaveBudget:   MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // This inflates your existing layout file directly
        return inflater.inflate(R.layout.bottom_sheet_set_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Binding to the components inside your existing XML
        etBudgetAmount = view.findViewById(R.id.etBudgetAmount)
        btnSaveBudget  = view.findViewById(R.id.btnSaveBudget)

        // Read the existing budget from the database to pre-fill the input field
        thread {
            val db = AppDatabase.getDatabase(requireContext())
            val currentBudget = db.budgetDao().getLatestBudget()

            activity?.runOnUiThread {
                if (currentBudget != null && currentBudget.monthlyBudget > 0) {
                    etBudgetAmount.setText(currentBudget.monthlyBudget.toString())
                }
            }
        }

        btnSaveBudget.setOnClickListener {
            val budgetString = etBudgetAmount.text.toString().trim()

            if (budgetString.isEmpty()) {
                etBudgetAmount.error = "Please enter an amount"
                return@setOnClickListener
            }

            val budgetAmount = budgetString.toDoubleOrNull()
            if (budgetAmount != null && budgetAmount > 0) {
                // Pass the value back to the callback function in Home.kt
                onBudgetSaved(budgetAmount)
                Toast.makeText(requireContext(), "Monthly budget updated!", Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                etBudgetAmount.error = "Please enter a valid positive number"
            }
        }
    }
}