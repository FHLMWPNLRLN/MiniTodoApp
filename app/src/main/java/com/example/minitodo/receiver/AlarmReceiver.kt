package com.example.minitodo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.minitodo.notification.NotificationUtils

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ALARM_REMINDER = "com.example.minitodo.ALARM_REMINDER"
        const val EXTRA_TODO_ID = "todo_id"
        const val EXTRA_TODO_TITLE = "todo_title"
        const val EXTRA_REMIND_TIME = "remind_time"
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm received: ${intent.action}")
        
        if (intent.action == ACTION_ALARM_REMINDER) {
            val todoId = intent.getIntExtra(EXTRA_TODO_ID, -1)
            val todoTitle = intent.getStringExtra(EXTRA_TODO_TITLE) ?: ""
            val remindTime = intent.getStringExtra(EXTRA_REMIND_TIME) ?: ""

            Log.d(TAG, "Creating notification for todo: id=$todoId, title=$todoTitle, time=$remindTime")

            // 检查通知权限（仅作日志，不影响通知显示）
            val notificationsEnabled = NotificationUtils.areNotificationsEnabled(context)
            Log.d(TAG, "Notifications enabled: $notificationsEnabled")

            // 显示通知（无论权限如何都显示）
            try {
                NotificationUtils.showReminderNotification(context, todoId, todoTitle, remindTime)
                Log.d(TAG, "Notification displayed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show notification", e)
            }
        } else {
            Log.w(TAG, "Unknown action received: ${intent.action}")
        }
    }
}
