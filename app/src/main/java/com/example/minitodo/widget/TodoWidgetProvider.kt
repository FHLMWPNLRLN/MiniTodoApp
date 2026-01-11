package com.example.minitodo.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.minitodo.R

/**
 * App Widget提供者
 *
 * 作用：管理应用的小部件（Widget）
 *
 * 功能：
 * 1. 处理Widget更新事件
 * 2. 响应自定义更新广播
 * 3. 创建Widget UI并设置交互
 *
 * Android Widget概念：
 * - AppWidget是可以嵌入到主屏幕或锁屏的小型应用视图
 * - AppWidgetProvider是Widget的核心组件
 * - RemoteViews用于在进程外显示UI
 *
 * 生命周期：
 * 1. onUpdate(): Widget添加或更新时调用
 * 2. onReceive(): 接收广播（包括自定义广播）
 * 3. onEnabled(): 首次创建Widget时调用
 * 4. onDisabled(): 最后一个Widget删除时调用
 *
 * 配置要求：
 * - 需要在AndroidManifest.xml中注册为BroadcastReceiver
 * - 需要指定app_widget_info.xml配置文件
 * - 需要必要的权限
 *
 * 自定义更新：
 * - 主应用可以发送ACTION_UPDATE_WIDGET广播
 * - Widget会收到并刷新显示内容
 */
class TodoWidgetProvider : AppWidgetProvider() {

    /**
     * 周期性更新或添加Widget时调用
     *
     * 功能：
     * 1. 系统初始化Widget时调用
     * 2. Widget更新周期到达时调用
     * 3. 设备启动时调用
     *
     * @param context 应用上下文
     * @param appWidgetManager Widget管理器，用于更新Widget
     * @param appWidgetIds 要更新的所有Widget ID数组
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    /**
     * 接收广播事件
     *
     * 功能：
     * 1. 系统自动广播（onUpdate）
     * 2. 自定义广播（ACTION_UPDATE_WIDGET）
     *
     * 使用场景：
     * - 当待办项更新时，发送ACTION_UPDATE_WIDGET广播
     * - Widget收到广播后立即刷新显示
     * - 无需等待系统的周期性更新
     *
     * @param context 应用上下文
     * @param intent 广播Intent（包含Action和Extra）
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (intent?.action == ACTION_UPDATE_WIDGET && context != null) {
            // 获取所有Widget ID并逐个更新
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, TodoWidgetProvider::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    companion object {
        // 自定义广播Action，用于触发Widget更新
        const val ACTION_UPDATE_WIDGET = "com.example.minitodo.UPDATE_WIDGET"

        /**
         * 更新单个Widget
         *
         * 功能：
         * 1. 创建RemoteViews对象（Widget UI）
         * 2. 设置Widget外观和内容
         * 3. 设置点击事件
         * 4. 通知AppWidgetManager更新
         *
         * @param context 应用上下文
         * @param appWidgetManager Widget管理器
         * @param appWidgetId 要更新的Widget ID
         */
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // 创建RemoteViews，指定Widget布局
            val views = RemoteViews(context.packageName, R.layout.widget_todo)

            // 创建点击Intent，打开应用的MainActivity
            val intent = Intent(context, com.example.minitodo.MainActivity::class.java)
            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            // 设置Widget根视图的点击事件
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // 通知AppWidgetManager更新此Widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
