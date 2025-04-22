package com.example.financetracker.util

import android.content.Context
import android.content.SharedPreferences
import com.example.financetracker.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.ArrayList

object PrefsUtil {

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Monthly Budget
    fun saveMonthlyBudget(context: Context, budget: Double) {
        getPrefs(context).edit().putFloat(Constants.KEY_MONTHLY_BUDGET, budget.toFloat()).apply()
    }

    fun getMonthlyBudget(context: Context): Double {
        return getPrefs(context).getFloat(Constants.KEY_MONTHLY_BUDGET, Constants.DEFAULT_BUDGET.toFloat()).toDouble()
    }

    // Currency type
    fun saveCurrencyType(context: Context, currency: String) {
        getPrefs(context).edit().putString(Constants.KEY_CURRENCY_TYPE, currency).apply()
    }

    fun getCurrencyType(context: Context): String {
        return getPrefs(context).getString(Constants.KEY_CURRENCY_TYPE, Constants.DEFAULT_CURRENCY) ?: Constants.DEFAULT_CURRENCY
    }

    // Transactions (stored as JSON string)
    fun saveTransactions(context: Context, transactions: List<Transaction>) {
        val gson = Gson()
        val json = gson.toJson(transactions)
        getPrefs(context).edit().putString(Constants.KEY_TRANSACTIONS, json).apply()
    }

    fun getTransactions(context: Context): List<Transaction> {
        val gson = Gson()
        val json = getPrefs(context).getString(Constants.KEY_TRANSACTIONS, null)
        
        if (json.isNullOrEmpty()) {
            return ArrayList()
        }
        
        val type: Type = object : TypeToken<List<Transaction>>() {}.type
        return gson.fromJson(json, type)
    }
} 