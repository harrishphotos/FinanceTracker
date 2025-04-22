package com.example.financetracker.util

import android.content.Context
import com.example.financetracker.R
import com.example.financetracker.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.reflect.Type

object BackupUtil {

    fun exportTransactions(context: Context, transactions: List<Transaction>): Result<String> {
        return try {
            val gson = Gson()
            val json = gson.toJson(transactions)
            
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            val backupFile = File(backupDir, context.getString(R.string.backup_filename))
            FileWriter(backupFile).use { it.write(json) }
            
            Result.success(backupFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun importTransactions(context: Context): Result<List<Transaction>> {
        return try {
            val backupDir = File(context.filesDir, "backups")
            val backupFile = File(backupDir, context.getString(R.string.backup_filename))
            
            if (!backupFile.exists()) {
                return Result.failure(Exception("Backup file not found"))
            }
            
            val json = BufferedReader(FileReader(backupFile)).use { it.readText() }
            val gson = Gson()
            val type: Type = object : TypeToken<List<Transaction>>() {}.type
            val transactions: List<Transaction> = gson.fromJson(json, type)
            
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 