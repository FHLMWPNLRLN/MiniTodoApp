package com.example.minitodo.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.minitodo.receiver.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.*

object AlarmUtils {
    private const val TAG = "AlarmUtils"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    /**
     * 设置提醒闹钟
     * @param context 应用上下文
     * @param remindTime 提醒时间字符串 (格式: yyyy-MM-dd HH:mm)
     * @param todoTitle 待办事项标题
     * @param todoId 待办事项ID
     */
    fun setAlarm(
        context: Context,
        remindTime: String,
        todoTitle: String,
        todoId: Int
    ) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // 解析提醒时间
            val calendar = Calendar.getInstance()
            val parsedDate = dateFormat.parse(remindTime)
            calendar.time = parsedDate ?: return
            
            val now = System.currentTimeMillis()
            val reminderTimeMs = calendar.timeInMillis
            
            // 检查时间是否有效
            if (reminderTimeMs <= now) {
                Log.w(TAG, "提醒时间已经过期，将使用当前时间加1分钟: $remindTime")
                calendar.timeInMillis = now + 60000  // 延后到1分钟后
            }
            
            // 提前10分钟提醒
            calendar.add(Calendar.MINUTE, -10)
            
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM_REMINDER
                putExtra(AlarmReceiver.EXTRA_TODO_ID, todoId)
                putExtra(AlarmReceiver.EXTRA_TODO_TITLE, todoTitle)
                putExtra(AlarmReceiver.EXTRA_REMIND_TIME, remindTime)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                todoId,  // requestCode 使用 todoId 确保唯一性
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // 设置精确闹钟
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            
            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date(now))
            val alarmTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(calendar.time)
            Log.d(TAG, "Alarm set for todo $todoId\n" +
                    "  Current: $currentTime\n" +
                    "  Alarm (提前10分钟): $alarmTime\n" +
                    "  Original reminder: $remindTime\n" +
                    "  Title: $todoTitle")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set alarm", e)
        }
    }

    /**
     * 移除提醒闹钟
     * @param context 应用上下文
     * @param todoId 待办事项ID
     */
    fun removeAlarm(context: Context, todoId: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                todoId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                Log.d(TAG, "Alarm cancelled for todo $todoId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove alarm", e)
        }
    }

    /**
     * 检查提醒时间是否已经到达
     */
    fun hasReachedTime(remindTime: String): Boolean {
        return try {
            val remindDate = dateFormat.parse(remindTime) ?: return false
            System.currentTimeMillis() >= remindDate.time
        } catch (e: Exception) {
            false
        }
    }
}
