package com.example.minitodo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.minitodo.notification.NotificationUtils

/**
 * 闹钟广播接收器
 * 
 * 作用：接收由AlarmManager发送的闹钟广播，触发通知显示
 * 
 * 工作原理：
 * 1. AlarmUtils设置系统闹钟（AlarmManager）
 * 2. 到达闹钟时间时，系统自动发送广播
 * 3. AlarmReceiver.onReceive()被系统调用
 * 4. 解析Intent中的待办项信息
 * 5. 调用NotificationUtils显示通知
 * 
 * 配置要求：
 * - 需要在AndroidManifest.xml中注册为BroadcastReceiver
 * - 需要声明RECEIVE_BOOT_COMPLETED权限（可选）
 * 广播流程：
 * - 注册Intent Filter: ACTION_ALARM_REMINDER
 * - 系统唤醒应用（如果需要）
 * - onReceive()同步执行，不能执行长耗时操作
 * - 超过10秒会被系统强制停止 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        // 自定义广播Action，用于标识这是闹钟提醒广播
        const val ACTION_ALARM_REMINDER = "com.example.minitodo.ALARM_REMINDER"
        // Intent extra 常量，用于传递待办项ID
        const val EXTRA_TODO_ID = "todo_id"
        // Intent extra 常量，用于传递待办项标题
        const val EXTRA_TODO_TITLE = "todo_title"
        // Intent extra 常量，用于传递提醒时间
        const val EXTRA_REMIND_TIME = "remind_time"
        private const val TAG = "AlarmReceiver"
    }

    /**
     * 接收广播时调用的方法
     * 
     * 功能：处理闹钟广播，显示待办提醒通知
     * 
     * @param context 应用上下文，传递给通知工具
     * @param intent 广播Intent，包含待办项信息
     * 
     * 执行步骤：
     * 1. 记录广播接收日志
     * 2. 检查广播Action是否为ACTION_ALARM_REMINDER
     * 3. 从Intent中提取待办项信息
     *    - todoId：待办项唯一标识
     *    - todoTitle：待办项标题
     *    - remindTime：原始提醒时间
     * 4. 检查通知权限状态（仅日志，不影响显示）
     * 5. 调用NotificationUtils.showReminderNotification()显示通知
     * 6. 异常处理：记录错误日志但继续执行
     * 
     * 性能考虑：
     * - onReceive()必须快速完成（<10秒）
     * - 不能执行数据库操作（已在ViewModel处理）
     * - 只负责显示通知，不修改待办项
     * 
     * 权限处理：
     * - 检查POST_NOTIFICATIONS权限（Android 13+）
     * - 即使权限被禁用也尝试显示通知
     * - 系统会处理权限检查
     * 
     * 日志说明：
     * - "Alarm received: [action]" - 广播接收
     * - "Creating notification for todo: [信息]" - 准备显示通知
     * - "Notifications enabled: [true/false]" - 权限状态
     * - "Notification displayed successfully" - 成功显示
     * - "Failed to show notification" - 显示失败（异常）
     * 
     * 示例流程：
     * 1. 用户设置待办：\"买菜\" 提醒时间 2026-01-15 14:30
     * 2. AlarmUtils调用setAlarm()，设置闹钟为14:20（提前10分钟）
     * 3. 到14:20系统触发闹钟广播
     * 4. onReceive()接收广播，提取信息：
     *    - todoId: 1
     *    - todoTitle: \"买菜\"
     *    - remindTime: \"2026-01-15 14:30\"
     * 5. showReminderNotification()显示通知
     * 6. 用户看到通知，点击打开应用
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm received: ${intent.action}")
        
        if (intent.action == ACTION_ALARM_REMINDER) {
            // 从Intent中提取待办项信息
            val todoId = intent.getIntExtra(EXTRA_TODO_ID, -1)
            val todoTitle = intent.getStringExtra(EXTRA_TODO_TITLE) ?: ""
            val remindTime = intent.getStringExtra(EXTRA_REMIND_TIME) ?: ""

            Log.d(TAG, "Creating notification for todo: id=$todoId, title=$todoTitle, time=$remindTime")

            // 检查通知权限状态（仅用于日志和调试）
            val notificationsEnabled = NotificationUtils.areNotificationsEnabled(context)
            Log.d(TAG, "Notifications enabled: $notificationsEnabled")

            // 显示通知（无论权限状态如何都尝试显示）
            try {
                NotificationUtils.showReminderNotification(context, todoId, todoTitle, remindTime)
                Log.d(TAG, "Notification displayed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show notification", e)
            }
        } else {
            Log.w(TAG, "Unknown action: ${intent.action}")
        }
    }
}
