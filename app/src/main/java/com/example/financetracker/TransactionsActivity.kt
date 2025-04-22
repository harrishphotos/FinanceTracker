package com.example.financetracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.financetracker.adapter.TransactionAdapter
import com.example.financetracker.databinding.ActivityTransactionsBinding
import com.example.financetracker.model.Transaction
import com.example.financetracker.util.PrefsUtil
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import java.util.Calendar
import java.util.Date
import androidx.core.content.ContextCompat
import android.app.DatePickerDialog
import android.widget.Toast

class TransactionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionsBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private var allTransactions = listOf<Transaction>()
    private var filteredTransactions = listOf<Transaction>() // This is a var, not val
    private var currentTab = 0 // 0: All, 1: Income, 2: Expense
    private var selectedCategory: String? = null // Added for category filter
    private var selectedDate: Calendar? = null // Added for date filter
    private var currency = "$"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load transactions from preferences
        allTransactions = PrefsUtil.getTransactions(this)
        currency = PrefsUtil.getCurrencyType(this)

        // Setup category chips
        setupCategoryChips()

        // Setup tabs
        setupTabs()

        // Setup RecyclerView
        setupRecyclerView()

        // Setup FAB for adding transactions
        binding.fabAddTransactionAll.setOnClickListener {
            val intent = Intent(this, AddEditTransactionActivity::class.java)
            startActivity(intent)
        }

        // Setup bottom navigation
        setupBottomNavigation()

        // Apply initial filter
        applyFilter()

        // Setup FAB for date filter
        binding.fabDateFilter.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    // Handle the selected date here
                    selectedDate = Calendar.getInstance()
                    selectedDate?.set(year, month, dayOfMonth)
                    
                    // Show visual indication that date filter is active
                    binding.fabDateFilter.backgroundTintList = 
                        ContextCompat.getColorStateList(this, R.color.primary)
                    
                    // Apply the date filter
                    applyFilter()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Long press to clear date filter
        binding.fabDateFilter.setOnLongClickListener {
            clearDateFilter()
            Toast.makeText(this, "Date filter cleared", Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this screen
        allTransactions = PrefsUtil.getTransactions(this)
        currency = PrefsUtil.getCurrencyType(this)
        applyFilter()
    }

    private fun setupCategoryChips() {
        val categories = mutableSetOf<String>()
        allTransactions.forEach { categories.add(it.category) }

        val chipGroup = binding.chipGroupCategories
        chipGroup.removeAllViews()

        // Add "All" chip first
        val allChip = createCategoryChip(getString(R.string.filter_all), true)
        chipGroup.addView(allChip)
        chipGroup.check(allChip.id) // Check "All" by default

        // Add chips for each unique category
        categories.sorted().forEach { category ->
            chipGroup.addView(createCategoryChip(category))
        }

        // Listener for chip selection
        chipGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedChip = group.findViewById<Chip>(checkedId)
            selectedCategory = if (selectedChip?.text == getString(R.string.filter_all)) {
                null // No category filter
            } else {
                selectedChip?.text?.toString()
            }
            applyFilter()
        }
    }

    private fun createCategoryChip(categoryName: String, shouldBeChecked: Boolean = false): Chip {
        val chip = Chip(this)
        chip.text = categoryName
        chip.isCheckable = true
        chip.isChecked = shouldBeChecked
        chip.chipBackgroundColor = ContextCompat.getColorStateList(this, R.color.accent)
        return chip
    }

    private fun setupTabs() {
        with(binding.tabLayout) {
            addTab(newTab().setText(getString(R.string.tab_all)))
            addTab(newTab().setText(getString(R.string.tab_income)))
            addTab(newTab().setText(getString(R.string.tab_expense)))

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    if (currentTab != tab.position) {
                        currentTab = tab.position
                        applyFilter()
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    // Do nothing
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                    // Do nothing
                }
            })
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            this,
            filteredTransactions,
            currency,
            onEditClick = { transaction ->
                val intent = Intent(this, AddEditTransactionActivity::class.java).apply {
                    putExtra("transaction", transaction)
                }
                startActivity(intent)
            },
            onDeleteClick = { transaction ->
                deleteTransaction(transaction)
            }
        )

        binding.rvAllTransactions.apply {
            layoutManager = LinearLayoutManager(this@TransactionsActivity)
            adapter = transactionAdapter
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationTransactions.selectedItemId = R.id.nav_transactions

        binding.bottomNavigationTransactions.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    finish()
                    true
                }
                R.id.nav_transactions -> {
                    // Already on transactions screen
                    true
                }
                R.id.nav_backup -> {
                    startActivity(Intent(this, BackupActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun applyFilter() {
        // Start with all transactions
        var currentList = allTransactions
        
        // Filter by selected category (if not "All")
        selectedCategory?.let {
            currentList = currentList.filter { transaction -> transaction.category == it }
        }
        
        // Filter by selected date (if date filter is applied)
        selectedDate?.let { date ->
            currentList = currentList.filter { transaction ->
                val transactionCalendar = Calendar.getInstance()
                transactionCalendar.time = transaction.date
                
                transactionCalendar.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                transactionCalendar.get(Calendar.MONTH) == date.get(Calendar.MONTH) &&
                transactionCalendar.get(Calendar.DAY_OF_MONTH) == date.get(Calendar.DAY_OF_MONTH)
            }
        }
        
        // Then filter by transaction type (tab)
        val newFilteredList = when (currentTab) {
            1 -> currentList.filter { !it.isExpense } // Income
            2 -> currentList.filter { it.isExpense } // Expense
            else -> currentList // All
        }
        
        // Update adapter with the new list
        updateUI(newFilteredList)
    }
    
    private fun updateUI(newFilteredList: List<Transaction>) {
        if (newFilteredList.isEmpty()) {
            binding.tvNoTransactionsAll.visibility = View.VISIBLE
            binding.rvAllTransactions.visibility = View.GONE
        } else {
            binding.tvNoTransactionsAll.visibility = View.GONE
            binding.rvAllTransactions.visibility = View.VISIBLE
            transactionAdapter.updateTransactions(newFilteredList)
        }
    }

    private fun deleteTransaction(transaction: Transaction) {
        val mutableTransactions = allTransactions.toMutableList()
        mutableTransactions.remove(transaction)
        allTransactions = mutableTransactions

        // Save updated transactions
        PrefsUtil.saveTransactions(this, allTransactions)

        // Re-apply filter
        applyFilter()
    }

    // Add a method to clear the date filter
    private fun clearDateFilter() {
        selectedDate = null
        binding.fabDateFilter.backgroundTintList = 
            ContextCompat.getColorStateList(this, R.color.accent)
        applyFilter()
    }
}