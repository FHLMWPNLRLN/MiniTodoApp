package com.example.minitodo.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.minitodo.receiver.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.*

/**
 * 系统闹钟管理工具类
 * 
 * 职责：
 * 1. 设置系统闹钟用于待办提醒
 * 2. 移除和取消闹钟
 * 3. 检查提醒时间是否已到达
 * 
 * 核心设计：
 * - 使用AlarmManager系统服务设置精确闹钟
 * - 支持低功耗模式（setAndAllowWhileIdle）
 * - 提前10分钟提醒用户
 * - 使用requestCode=todoId确保闹钟唯一性
 * 
 * Android版本兼容性：
 * - 支持Android 6.0+
 * - Android 12+需要SCHEDULE_EXACT_ALARM权限
 * - FLAG_IMMUTABLE用于高版本安全性
 * 
 * 工作流程：
 * 1. 用户设置提醒时间（格式：yyyy-MM-dd HH:mm）
 * 2. setAlarm()解析时间，创建PendingIntent
 * 3. AlarmManager设置闹钟（提前10分钟）
 * 4. 到时间后系统触发广播给AlarmReceiver
 * 5. AlarmReceiver显示通知给用户
 */
object AlarmUtils {
    private const val TAG = "AlarmUtils"
    // 时间格式化器：yyyy-MM-dd HH:mm，例：2026-01-15 14:30
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    /**
     * 设置提醒闹钟
     * 
     * 功能：根据提醒时间设置系统闹钟，触发时显示通知
     * 
     * @param context 应用上下文，用于获取AlarmManager
     * @param remindTime 提醒时间字符串（格式：yyyy-MM-dd HH:mm，例：2026-01-15 14:30）
     * @param todoTitle 待办事项标题，显示在通知中
     * @param todoId 待办事项ID，用于作为requestCode确保闹钟唯一性
     * 
     * 执行步骤：
     * 1. 获取AlarmManager系统服务
     * 2. 解析remindTime字符串为Calendar对象
     * 3. 验证时间有效性（检查是否已过期）
     * 4. 将闹钟时间提前10分钟（用户友好）
     * 5. 创建Intent发送给AlarmReceiver
     * 6. 创建PendingIntent（FLAG_IMMUTABLE用于安全）
     * 7. 调用setAndAllowWhileIdle()设置闹钟（支持低功耗）
     * 8. 记录日志记录闹钟设置
     * 
     * 特殊处理：
     * - 如果remindTime已过期，自动延后到1分钟后
     * - 提前10分钟提醒，给用户准备时间
     * - 使用FLAG_IMMUTABLE确保Android 12+兼容
     * - setAndAllowWhileIdle()可在低功耗模式下工作
     * 
     * 异常处理：
     * - 时间解析失败时返回
     * - 闹钟设置失败时记录错误日志
     */
    fun setAlarm(
        context: Context,
        remindTime: String,
        todoTitle: String,
        todoId: Int
    ) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // 解析提醒时间字符串
            val calendar = Calendar.getInstance()
            val parsedDate = dateFormat.parse(remindTime)
            calendar.time = parsedDate ?: return
            
            val now = System.currentTimeMillis()
            val reminderTimeMs = calendar.timeInMillis
            
            // 检查时间是否有效，如果已过期则使用当前时间+1分钟
            if (reminderTimeMs <= now) {
                Log.w(TAG, "提醒时间已经过期，将使用当前时间加1分钟: $remindTime")
                calendar.timeInMillis = now + 60000  // 延后到1分钟后
            }
            
            // 提前10分钟提醒，给用户充足的准备时间
            calendar.add(Calendar.MINUTE, -10)
            
            // 创建广播Intent，包含待办项信息
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM_REMINDER
                putExtra(AlarmReceiver.EXTRA_TODO_ID, todoId)
                putExtra(AlarmReceiver.EXTRA_TODO_TITLE, todoTitle)
                putExtra(AlarmReceiver.EXTRA_REMIND_TIME, remindTime)
            }
            
            // 创建PendingIntent，使用todoId作为requestCode确保唯一性
            // FLAG_UPDATE_CURRENT：如果闹钟已存在则更新
            // FLAG_IMMUTABLE：推荐用于Android 12+
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                todoId,  // requestCode 使用 todoId 确保唯一性
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // 设置精确闹钟
            // RTC_WAKEUP: 使用RTC时钟，会唤醒设备
            // setAndAllowWhileIdle: 即使在低功耗模式下也会触发
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            
            // 记录详细日志便于调试
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
     * 移除/取消待办项的提醒闹钟
     * 
     * 功能：取消之前设置的闹钟，防止重复通知
     * 
     * @param context 应用上下文
     * @param todoId 待办事项ID，用于定位要取消的闹钟
     * 
     * 执行步骤：
     * 1. 获取AlarmManager系统服务
     * 2. 创建相同的Intent和PendingIntent（使用相同的todoId）
     * 3. 调用alarmManager.cancel()取消闹钟
     * 4. 记录日志
     * 
     * 使用场景：
     * - 删除待办项时
     * - 移除待办项的提醒时间时
     * - 编辑待办项提醒时间时
     * 
     * 特点：
     * - FLAG_NO_CREATE：不创建新PendingIntent，只查找现有的
     * - 如果闹钟不存在，cancel()安全地返回
     * - 异常时记录日志但不抛出异常
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
     * 
     * 功能：判断提醒时间是否已过，用于UI中改变显示颜色
     * 
     * @param remindTime 提醒时间字符串（格式：yyyy-MM-dd HH:mm）
     * @return true 如果提醒时间已到达或已过期；false 如果还未到达
     * 
     * 逻辑：
     * - 解析remindTime为Date对象
     * - 比较：当前系统时间 >= remindTime时间
     * - 解析失败时返回false（不影响正常流程）
     * 
     * 使用场景：
     * - TodoAdapter中判断提醒时间显示颜色
     *   - 已到达：橙色（holo_orange_light）
     *   - 未到达：蓝色（holo_blue_dark）
     * - 提醒通知的时间判断
     * 
     * 示例：
     * ```
     * if (AlarmUtils.hasReachedTime(todo.remindTime)) {
     *     reminderTime.setTextColor(Color.ORANGE)  // 已过期
     * } else {
     *     reminderTime.setTextColor(Color.BLUE)    // 未到达
     * }
     * ```
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
