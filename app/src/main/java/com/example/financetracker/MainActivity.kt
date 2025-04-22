package com.example.financetracker

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.financetracker.adapter.TransactionAdapter
import com.example.financetracker.databinding.ActivityMainBinding
import com.example.financetracker.model.Transaction
import com.example.financetracker.util.BackupUtil
import com.example.financetracker.util.Constants
import com.example.financetracker.util.NotificationUtil
import com.example.financetracker.util.PrefsUtil
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.Calendar
import java.util.Date

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private var transactions = mutableListOf<Transaction>()
    private lateinit var addTransactionLauncher: ActivityResultLauncher<Intent>
    private lateinit var editTransactionLauncher: ActivityResultLauncher<Intent>
    private lateinit var settingsLauncher: ActivityResultLauncher<Intent>
    private var currency = Constants.DEFAULT_CURRENCY
    private var monthlyBudget = Constants.DEFAULT_BUDGET

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != 
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Initialize notification channel
        NotificationUtil.createNotificationChannel(this)
        
        // Setup activity result launchers
        setupActivityResultLaunchers()
        
        // Set up the transactions RecyclerView
        setupRecyclerView()
        
        // Set up FAB for adding transactions
        binding.fabAddTransaction.setOnClickListener {
            val intent = Intent(this, AddEditTransactionActivity::class.java)
            addTransactionLauncher.launch(intent)
        }
        
        // Set up bottom navigation
        setupBottomNavigation()
        
        // Set up view all transactions action
        binding.tvViewAll.setOnClickListener {
            startActivity(Intent(this, TransactionsActivity::class.java))
        }
        
        // Load saved data from preferences
        loadData()
        
        // Update UI with loaded data
        updateUI()
        
        // Set up card click listeners
        setupCardClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // Ensure the home item is selected when returning to MainActivity
        binding.bottomNavigation.selectedItemId = R.id.nav_home
        // Reload data in case changes were made in other activities (like settings)
        loadData()
        updateUI()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    private fun setupActivityResultLaunchers() {
        // For adding new transactions
        addTransactionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val transaction = result.data?.getSerializableExtra("transaction") as Transaction?
                transaction?.let {
                    transactions.add(it)
                    saveTransactions()
                    updateUI()
                }
            }
        }
        
        // For editing existing transactions
        editTransactionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val editedTransaction = result.data?.getSerializableExtra("transaction") as Transaction?
                editedTransaction?.let { edited ->
                    // Find and replace the transaction with the same ID
                    val index = transactions.indexOfFirst { it.id == edited.id }
                    if (index != -1) {
                        transactions[index] = edited
                        saveTransactions()
                        updateUI()
                    }
                }
            }
        }
        
        // For settings
        settingsLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { 
            // Always reload data when returning from settings
            loadData()
            updateUI()
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            this,
            transactions,
            currency,
            onEditClick = { transaction ->
                val intent = Intent(this, AddEditTransactionActivity::class.java).apply {
                    putExtra("transaction", transaction)
                }
                editTransactionLauncher.launch(intent)
            },
            onDeleteClick = { transaction ->
                showDeleteConfirmationDialog(transaction)
            }
        )
        
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = transactionAdapter
    }

    private fun showDeleteConfirmationDialog(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                transactions.remove(transaction)
                saveTransactions()
                updateUI()
                Toast.makeText(this, getString(R.string.transaction_deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadData() {
        // Load transactions
        transactions = PrefsUtil.getTransactions(this).toMutableList()
        
        // Load budget and currency settings
        monthlyBudget = PrefsUtil.getMonthlyBudget(this)
        currency = PrefsUtil.getCurrencyType(this)
    }

    private fun saveTransactions() {
        PrefsUtil.saveTransactions(this, transactions)
    }

    private fun updateUI() {
        // Update adapter with new data and currency
        transactionAdapter.updateTransactions(transactions)
        
        // Show "No transactions" message if the list is empty
        if (transactions.isEmpty()) {
            binding.tvNoTransactions.visibility = View.VISIBLE
            binding.rvTransactions.visibility = View.GONE
        } else {
            binding.tvNoTransactions.visibility = View.GONE
            binding.rvTransactions.visibility = View.VISIBLE
        }
        
        // Calculate income, expense, and balance
        var totalIncome = 0.0
        var totalExpense = 0.0
        
        // Get transactions from current month only
        val currentMonthTransactions = getCurrentMonthTransactions()
        
        for (transaction in transactions) {
            if (transaction.isExpense) {
                totalExpense += transaction.amount
            } else {
                totalIncome += transaction.amount
            }
        }
        
        val balance = totalIncome - totalExpense
        
        // Update summary card
        binding.tvBalanceAmount.text = formatAmount(balance)
        binding.tvIncomeAmount.text = formatAmount(totalIncome)
        binding.tvExpenseAmount.text = formatAmount(totalExpense)
        
        // Calculate current month expenses for budget tracking
        var currentMonthExpenses = 0.0
        for (transaction in currentMonthTransactions) {
            if (transaction.isExpense) {
                currentMonthExpenses += transaction.amount
            }
        }
        
        // Update budget information
        updateBudgetInfo(currentMonthExpenses)
        
        // Update spending chart
        updateSpendingChart(currentMonthTransactions)
    }

    private fun formatAmount(amount: Double): String {
        return "$currency${String.format("%.2f", amount)}"
    }

    private fun getCurrentMonthTransactions(): List<Transaction> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        return transactions.filter { transaction ->
            val transactionCalendar = Calendar.getInstance().apply {
                time = transaction.date
            }
            transactionCalendar.get(Calendar.MONTH) == currentMonth && 
                transactionCalendar.get(Calendar.YEAR) == currentYear
        }
    }

    private fun updateBudgetInfo(currentMonthExpenses: Double) {
        // Display monthly budget
        binding.tvMonthlyBudget.text = getString(R.string.monthly_budget, currency, monthlyBudget)
        
        // Calculate budget usage percentage
        val budgetPercentage = if (monthlyBudget > 0) {
            (currentMonthExpenses / monthlyBudget) * 100
        } else 0.0
        
        // Update progress bar
        binding.progressBudget.progress = budgetPercentage.toInt().coerceAtMost(100)
        
        // Update progress text
        binding.tvBudgetProgress.text = getString(R.string.budget_progress, budgetPercentage)
        
        // Show warning if over threshold
        if (budgetPercentage >= Constants.BUDGET_WARNING_THRESHOLD * 100) {
            binding.tvBudgetWarning.visibility = View.VISIBLE
            
            // Check if budget is actually exceeded (over 100%)
            if (budgetPercentage > 100) {
                // Calculate the amount over budget
                val overBudgetAmount = currentMonthExpenses - monthlyBudget
                binding.tvBudgetWarning.text = getString(R.string.budget_exceeded, overBudgetAmount, currency)
                
                // Show notification with exceeded amount
                NotificationUtil.showBudgetWarningNotification(
                    this, 
                    budgetPercentage.toFloat(),
                    isExceeded = true,
                    overBudgetAmount = overBudgetAmount
                )
            } else {
                // Just approaching budget limit
                binding.tvBudgetWarning.text = getString(R.string.budget_warning, budgetPercentage)
                
                // Show standard warning notification
                NotificationUtil.showBudgetWarningNotification(this, budgetPercentage.toFloat())
            }
        } else {
            binding.tvBudgetWarning.visibility = View.GONE
        }
    }

    private fun updateSpendingChart(monthTransactions: List<Transaction>) {
        // Skip if no transactions
        if (monthTransactions.isEmpty()) {
            binding.pieChart.setNoDataText("No transactions to display")
            binding.pieChart.invalidate()
            return
        }
        
        // Group expenses by category
        val categoryMap = mutableMapOf<String, Double>()
        
        for (transaction in monthTransactions) {
            if (transaction.isExpense) {
                val currentAmount = categoryMap[transaction.category] ?: 0.0
                categoryMap[transaction.category] = currentAmount + transaction.amount
            }
        }
        
        // Create pie chart entries
        val entries = ArrayList<PieEntry>()
        
        for ((category, amount) in categoryMap) {
            entries.add(PieEntry(amount.toFloat(), category))
        }
        
        // Create pie dataset
        val dataSet = PieDataSet(entries, "Spending Categories")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 14f
        
        // Create and set up pie data
        val pieData = PieData(dataSet)
        
        // Set up chart
        binding.pieChart.apply {
            data = pieData
            description.isEnabled = false
            isDrawHoleEnabled = true
            setDrawEntryLabels(false)
            legend.isEnabled = true
            setUsePercentValues(true)
            invalidate()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Already on home, do nothing
                    true
                }
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    true
                }
                R.id.nav_backup -> {
                    startActivity(Intent(this, BackupActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    settingsLauncher.launch(intent)
                    true
                }
                else -> false
            }
        }
        
        // Set the home item as selected
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // We don't need the options menu anymore since we have bottom navigation
        return false
    }

    private fun setupCardClickListeners() {
        binding.cardChart.setOnClickListener {
            toggleChartVisibility()
        }
    }
    
    private fun toggleChartVisibility() {
        if (binding.llChartContainer.visibility == View.GONE) {
            binding.llChartContainer.visibility = View.VISIBLE
            binding.ivChartToggleArrow.setImageResource(R.drawable.ic_arrow_up)
        } else {
            binding.llChartContainer.visibility = View.GONE
            binding.ivChartToggleArrow.setImageResource(R.drawable.ic_arrow_down)
        }
    }
}