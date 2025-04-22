package com.example.financetracker

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.financetracker.databinding.ActivityBackupBinding
import com.example.financetracker.util.BackupUtil
import com.example.financetracker.util.PrefsUtil

class BackupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBackupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup buttons
        setupButtons()
        
        // Setup bottom navigation
        setupBottomNavigation()
    }

    private fun setupButtons() {
        binding.btnExport.setOnClickListener {
            exportData()
        }

        binding.btnImport.setOnClickListener {
            importData()
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigationBackup.selectedItemId = R.id.nav_backup
        
        binding.bottomNavigationBackup.setOnItemSelectedListener { menuItem ->
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
                    // Already on backup screen
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

    private fun exportData() {
        val transactions = PrefsUtil.getTransactions(this)
        
        if (transactions.isEmpty()) {
            binding.tvStatus.text = "No transactions to export"
            Toast.makeText(this, "No transactions to export", Toast.LENGTH_SHORT).show()
            return
        }
        
        BackupUtil.exportTransactions(this, transactions).fold(
            onSuccess = { filePath ->
                binding.tvStatus.text = "✅ Backup created successfully at:\n$filePath"
                Toast.makeText(
                    this,
                    getString(R.string.export_success, filePath),
                    Toast.LENGTH_LONG
                ).show()
            },
            onFailure = { error ->
                binding.tvStatus.text = "❌ Error: ${error.message}"
                Toast.makeText(
                    this,
                    getString(R.string.export_error, error.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun importData() {
        BackupUtil.importTransactions(this).fold(
            onSuccess = { importedTransactions ->
                PrefsUtil.saveTransactions(this, importedTransactions)
                binding.tvStatus.text = "✅ Successfully imported ${importedTransactions.size} transactions"
                Toast.makeText(
                    this,
                    getString(R.string.import_success),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onFailure = { error ->
                binding.tvStatus.text = "❌ Error: ${error.message}"
                Toast.makeText(
                    this,
                    getString(R.string.import_error, error.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}