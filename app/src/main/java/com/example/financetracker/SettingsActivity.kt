package com.example.financetracker

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.financetracker.databinding.ActivitySettingsBinding
import com.example.financetracker.util.Constants
import com.example.financetracker.util.NotificationUtil
import com.example.financetracker.util.PrefsUtil

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var budgetAlertsEnabled = true
    private var dailyReminderEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load current settings
        loadSettings()

        // Set up button click listeners
        binding.btnSaveSettings.setOnClickListener {
            saveSettings()
        }
        
        // Set up bottom navigation
        setupBottomNavigation()
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigationSettings.selectedItemId = R.id.nav_settings
        
        binding.bottomNavigationSettings.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    finish()
                    true
                }
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_backup -> {
                    startActivity(Intent(this, BackupActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    // Already on settings screen
                    true
                }
                else -> false
            }
        }
    }

    private fun loadSettings() {
        // Load budget
        val currentBudget = PrefsUtil.getMonthlyBudget(this)
        val currentCurrency = PrefsUtil.getCurrencyType(this)

        // Display current values
        binding.tvCurrentBudget.text = getString(
            R.string.current_budget,
            currentCurrency,
            currentBudget
        )

        binding.tvCurrentCurrency.text = getString(
            R.string.current_currency,
            currentCurrency
        )

        // Pre-fill fields with current values
        binding.etBudget.setText(currentBudget.toString())
        binding.etCurrency.setText(currentCurrency)

        // Set up notification switches
        binding.switchBudgetAlerts.isChecked = budgetAlertsEnabled
        binding.switchDailyReminder.isChecked = dailyReminderEnabled
    }

    private fun saveSettings() {
        // Validate input
        val budgetText = binding.etBudget.text.toString()
        val currencyText = binding.etCurrency.text.toString()

        if (budgetText.isBlank()) {
            binding.etBudget.error = getString(R.string.amount_required)
            return
        }

        if (currencyText.isBlank()) {
            binding.etCurrency.error = getString(R.string.currency_required)
            return
        }

        try {
            // Save budget and currency
            val budget = budgetText.toDouble()
            PrefsUtil.saveMonthlyBudget(this, budget)
            PrefsUtil.saveCurrencyType(this, currencyText)

            // Save notification preferences and update scheduler
            budgetAlertsEnabled = binding.switchBudgetAlerts.isChecked
            dailyReminderEnabled = binding.switchDailyReminder.isChecked
            
            // Schedule or cancel daily reminder based on the switch state
            NotificationUtil.scheduleDailyReminder(this, dailyReminderEnabled)

            Toast.makeText(
                this,
                getString(R.string.settings_saved),
                Toast.LENGTH_SHORT
            ).show()

            finish()
        } catch (e: NumberFormatException) {
            binding.etBudget.error = getString(R.string.amount_required)
        }
    }
} 