package com.example.financetracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.financetracker.util.NotificationUtil

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationUtil.showDailyReminderNotification(context)
    }
} 