package com.example.minitodo.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.minitodo.MainActivity
import com.example.minitodo.R

/**
 * 系统通知管理工具类
 * 
 * 职责：
 * 1. 创建和管理通知渠道（Android 8.0+）
 * 2. 显示待办提醒通知
 * 3. 检查通知权限（Android 13+）
 * 
 * 核心概念：
 * - 通知渠道（NotificationChannel）：Android 8.0+要求的分类机制
 * - 通知重要性：控制通知的优先级和行为（振动、声音、灯光等）
 * - PendingIntent：跳转回应用的意图，支持高版本安全性
 * 
 * 版本兼容性：
 * - Android 8.0+（API 26）: 需要创建通知渠道
 * - Android 13+（API 33）: POST_NOTIFICATIONS权限和运行时权限
 * - 低版本：直接显示通知
 * 
 * 流程：
 * 1. AlarmReceiver接收闹钟广播
 * 2. 调用showReminderNotification()显示通知
 * 3. showReminderNotification()先调用createNotificationChannel()
 * 4. 创建通知并通过NotificationManager显示
 * 5. 用户点击通知打开MainActivity
 */
object NotificationUtils {
    // 通知渠道ID：全局唯一标识
    private const val CHANNEL_ID = "todo_reminder_channel"
    // 通知渠道名称：用户在系统设置中看到的名称
    private const val CHANNEL_NAME = "待办提醒"
    // 通知ID：用于识别和更新同一个通知
    const val NOTIFICATION_ID = 1

    /**
     * 创建通知渠道（Android 8.0+必需）
     * 
     * 功能：为应用的通知建立渠道，用户可在系统设置中修改每个渠道的行为
     * 
     * @param context 应用上下文，用于获取NotificationManager
     * 
     * 执行步骤：
     * 1. 检查Android版本（仅在8.0+执行）
     * 2. 创建NotificationChannel对象
     *    - channelId: 在应用内唯一
     *    - name: 用户看到的渠道名
     *    - importance: 通知重要性
     * 3. 配置渠道属性
     *    - description: 渠道描述
     *    - enableVibration: 允许振动
     * 4. 向系统注册渠道
     * 
     * Android版本处理：
     * - SDK_INT < 26: 代码不执行（低版本不需要渠道）
     * - SDK_INT >= 26: 创建并注册渠道
     * 
     * 通知重要性等级：
     * - IMPORTANCE_MIN：静默，无声音无震动
     * - IMPORTANCE_LOW：无声音
     * - IMPORTANCE_DEFAULT：默认声音
     * - IMPORTANCE_HIGH：声音+振动（我们使用）
     * - IMPORTANCE_MAX：紧急，全部启用
     * 
     * 注意：
     * - 渠道创建后不能修改重要性，需重新安装应用
     * - 重复调用此函数是安全的（系统会忽略重复）
     * - 用户可在系统设置中修改每个渠道的行为
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH  // 高重要性：支持声音和振动
            ).apply {
                description = "待办事项提醒通知"
                enableVibration(true)  // 允许振动提醒
            }
            
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 显示待办提醒通知
     * 
     * 功能：根据待办项信息创建并显示系统通知
     * 
     * @param context 应用上下文
     * @param todoId 待办项ID，用于作为通知ID和PendingIntent的requestCode
     * @param todoTitle 待办项标题，显示为通知的主要内容
     * @param remindTime 提醒时间，显示在通知的详细信息中
     * 
     * 执行步骤：
     * 1. 创建通知渠道（如果需要）
     * 2. 创建跳转Intent和PendingIntent
     * 3. 构建通知对象
     * 4. 通过NotificationManager显示通知
     * 
     * 通知布局：
     * - 标题：\"待办提醒\"
     * - 内容：待办项标题
     * - 详细：\"提醒时间：xxx + 待办项标题\"（使用BigTextStyle展示）
     * 
     * 特点：
     * - setAutoCancel(true)：用户点击后自动取消通知
     * - setPriority(PRIORITY_HIGH)：高优先级
     * - setSmallIcon()：显示在状态栏的图标
     * - setContentIntent()：点击通知打开MainActivity
     * 
     * 权限要求：
     * - Android 13+需要POST_NOTIFICATIONS权限
     * - 需要在AndroidManifest.xml中声明
     * - 需要在运行时请求权限
     * 
     * PendingIntent说明：
     * - getActivity()：点击通知打开Activity
     * - FLAG_UPDATE_CURRENT：如果PendingIntent已存在则更新
     * - FLAG_IMMUTABLE：不可变，推荐用于高版本安全
     * 
     * 示例流程：
     * 1. 用户设置提醒时间 2026-01-15 14:30
     * 2. AlarmUtils设置闹钟，提前10分钟
     * 3. 到2026-01-15 14:20时系统触发闹钟
     * 4. AlarmReceiver接收广播，调用showReminderNotification()
     * 5. 通知显示：\"待办提醒 | [待办标题]\"
     * 6. 用户点击打开应用
     */
    fun showReminderNotification(
        context: Context,
        todoId: Int,
        todoTitle: String,
        remindTime: String
    ) {
        android.util.Log.d("NotificationUtils", "showReminderNotification called: id=$todoId, title=$todoTitle")
        // 确保通知渠道存在
        createNotificationChannel(context)
        
        // 创建点击通知时的跳转Intent
        val intent = Intent(context, MainActivity::class.java).apply {
            // FLAG_ACTIVITY_NEW_TASK：在新任务栈中启动Activity
            // FLAG_ACTIVITY_CLEAR_TASK：清空任务栈中其他Activity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        // 创建PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            context,
            todoId,  // requestCode用于区分不同的PendingIntent
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建通知
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)  // 状态栏图标
            .setContentTitle("待办提醒")                      // 通知标题
            .setContentText(todoTitle)                         // 通知简短内容
            // BigTextStyle：展开通知时显示的详细内容
            .setStyle(NotificationCompat.BigTextStyle().bigText("提醒时间：$remindTime\n$todoTitle"))
            .setContentIntent(pendingIntent)                   // 点击打开的Intent
            .setAutoCancel(true)                               // 点击后自动取消
            .setPriority(NotificationCompat.PRIORITY_HIGH)    // 高优先级
            .build()

        // 显示通知
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(todoId, notification)
    }

    /**
     * 检查通知权限是否已启用（Android 13+）
     * 
     * 功能：判断用户是否允许应用发送通知
     * 
     * @param context 应用上下文
     * @return true 如果通知已启用；false 如果被禁用
     * 
     * 版本兼容：
     * - Android 12及以下：总是返回true（权限模型不同）
     * - Android 13+：实际查询系统设置
     * 
     * 使用场景：
     * - 检查权限前的准备工作
     * - 向用户提示需要权限
     * - 调试和日志记录
     * 
     * 权限声明：
     * - 需要POST_NOTIFICATIONS权限（Android 13+）
     * - 需要在AndroidManifest.xml声明
     * - 需要在运行时动态请求（targetSdk>=31）
     * 
     * 注意：
     * - 即使返回false，showReminderNotification()仍会尝试显示
     * - 实际权限检查由系统进行
     * - 本方法仅用于查询状态，不进行权限申请
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: 查询系统的通知权限状态
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.areNotificationsEnabled()
        } else {
            // Android 12及以下: 通知总是启用的
            true
        }
    }
}
