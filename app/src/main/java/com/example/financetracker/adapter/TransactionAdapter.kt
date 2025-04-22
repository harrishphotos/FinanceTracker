package com.example.financetracker.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.financetracker.R
import com.example.financetracker.model.Transaction
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private val context: Context,
    private var transactions: List<Transaction>,
    private val currencySymbol: String,
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        
        holder.tvTitle.text = transaction.title
        holder.tvCategory.text = transaction.category
        holder.tvDate.text = dateFormat.format(transaction.date)
        
        val formattedAmount = String.format(
            context.getString(R.string.amount_placeholder),
            currencySymbol,
            transaction.amount
        )
        
        if (transaction.isExpense) {
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            holder.tvAmount.text = "-$formattedAmount"
        } else {
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            holder.tvAmount.text = formattedAmount
        }
        
        holder.btnEdit.setOnClickListener {
            onEditClick(transaction)
        }
        
        holder.btnDelete.setOnClickListener {
            onDeleteClick(transaction)
        }
    }

    override fun getItemCount(): Int = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        this.transactions = newTransactions
        notifyDataSetChanged()
    }
} 