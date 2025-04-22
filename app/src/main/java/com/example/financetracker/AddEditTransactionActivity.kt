package com.example.financetracker

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.financetracker.databinding.ActivityAddEditTransactionBinding
import com.example.financetracker.model.Transaction
import com.example.financetracker.util.Constants
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddEditTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTransactionBinding
    private var calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private var categoryList = Constants.EXPENSE_CATEGORIES
    private var isEdit = false
    private var editTransaction: Transaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if it's an edit or add operation
        if (intent.hasExtra("transaction")) {
            isEdit = true
            editTransaction = intent.getSerializableExtra("transaction") as Transaction
            setupEditMode()
        } else {
            setupAddMode()
        }

        // Set up date picker
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        // Setup transaction type selection (Expense/Income)
        binding.rgTransactionType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbExpense) {
                categoryList = Constants.EXPENSE_CATEGORIES
            } else {
                categoryList = Constants.INCOME_CATEGORIES
            }
            setupCategorySpinner()
        }

        // Set up save button
        binding.btnSave.setOnClickListener {
            saveTransaction()
        }

        // Set up cancel button
        binding.btnCancel.setOnClickListener {
            finish()
        }

        // Set up category spinner
        setupCategorySpinner()

        // Set initial date
        updateDateButton()
    }

    private fun setupAddMode() {
        binding.tvTitle.text = getString(R.string.add_new_transaction)
        binding.btnSelectDate.text = dateFormat.format(calendar.time)
        binding.rbExpense.isChecked = true
    }

    private fun setupEditMode() {
        binding.tvTitle.text = getString(R.string.edit_transaction)
        
        editTransaction?.let { transaction ->
            binding.etTitle.setText(transaction.title)
            binding.etAmount.setText(transaction.amount.toString())
            
            // Set transaction type
            if (transaction.isExpense) {
                binding.rbExpense.isChecked = true
                categoryList = Constants.EXPENSE_CATEGORIES
            } else {
                binding.rbIncome.isChecked = true
                categoryList = Constants.INCOME_CATEGORIES
            }
            
            // Set date
            calendar.time = transaction.date
            updateDateButton()
            
            // Update spinner
            setupCategorySpinner()
            
            // Set category selection
            val categoryIndex = categoryList.indexOf(transaction.category)
            if (categoryIndex >= 0) {
                binding.spinnerCategory.setSelection(categoryIndex)
            }
        }
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categoryList
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
        
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.tvCategoryError.visibility = View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateButton()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateButton() {
        binding.btnSelectDate.text = dateFormat.format(calendar.time)
    }

    private fun saveTransaction() {
        // Validate inputs
        val title = binding.etTitle.text.toString().trim()
        val amountText = binding.etAmount.text.toString().trim()
        val selectedPosition = binding.spinnerCategory.selectedItemPosition

        var valid = true

        if (title.isEmpty()) {
            binding.tilTitle.error = getString(R.string.title_required)
            valid = false
        } else {
            binding.tilTitle.error = null
        }

        if (amountText.isEmpty() || amountText.toDoubleOrNull() == null || amountText.toDouble() <= 0) {
            binding.tilAmount.error = getString(R.string.amount_required)
            valid = false
        } else {
            binding.tilAmount.error = null
        }

        if (selectedPosition < 0) {
            binding.tvCategoryError.visibility = View.VISIBLE
            valid = false
        } else {
            binding.tvCategoryError.visibility = View.GONE
        }

        if (!valid) return

        // Create transaction object
        val amount = amountText.toDouble()
        val category = categoryList[selectedPosition]
        val isExpense = binding.rbExpense.isChecked
        val date = calendar.time

        val transaction = if (isEdit && editTransaction != null) {
            // Update existing transaction
            editTransaction!!.apply {
                this.title = title
                this.amount = amount
                this.category = category
                this.date = date
                this.isExpense = isExpense
            }
        } else {
            // Create new transaction
            Transaction(
                title = title,
                amount = amount,
                category = category,
                date = date,
                isExpense = isExpense
            )
        }

        // Return the transaction to the caller
        val resultIntent = Intent().apply {
            putExtra("transaction", transaction)
        }
        setResult(RESULT_OK, resultIntent)
        
        Toast.makeText(this, getString(R.string.transaction_saved), Toast.LENGTH_SHORT).show()
        finish()
    }
} 