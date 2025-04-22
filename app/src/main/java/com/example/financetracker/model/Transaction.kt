package com.example.financetracker.model

import java.io.Serializable
import java.util.Date
import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var amount: Double,
    var category: String,
    var date: Date,
    var isExpense: Boolean = true
) : Serializable 