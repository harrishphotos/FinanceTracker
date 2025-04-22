package com.example.financetracker.util

object Constants {
    // SharedPreferences Keys
    const val PREFS_NAME = "finance_tracker_prefs"
    const val KEY_MONTHLY_BUDGET = "monthly_budget"
    const val KEY_CURRENCY_TYPE = "currency_type"
    const val KEY_TRANSACTIONS = "transactions"
    
    // Categories
    val EXPENSE_CATEGORIES = listOf(
        "Food", 
        "Transport", 
        "Bills", 
        "Entertainment", 
        "Shopping", 
        "Health", 
        "Education", 
        "Other"
    )
    
    val INCOME_CATEGORIES = listOf(
        "Salary", 
        "Freelance", 
        "Gifts", 
        "Investments", 
        "Other"
    )
    
    // Budget warning threshold
    const val BUDGET_WARNING_THRESHOLD = 0.8 // 80%
    
    // Default values
    const val DEFAULT_CURRENCY = "$"
    const val DEFAULT_BUDGET = 1000.0
} 