package com.example.minitodo.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.minitodo.R
import com.example.minitodo.data.TodoEntity
import com.example.minitodo.notification.AlarmUtils

/**
 * 待办项列表适配器
 * 
 * 作用：管理主列表RecyclerView的数据展示和交互
 * 
 * 功能：
 * 1. 显示待办项列表
 * 2. 支持切换完成状态（复选框）
 * 3. 支持删除待办项
 * 4. 显示和管理提醒时间
 * 5. 支持设置新的提醒时间
 * 
 * 设计模式：
 * - 继承ListAdapter：使用DiffUtil自动计算差异
 * - ViewHolder模式：高效管理视图重用
 * - 回调接口：处理用户交互
 * 
 * 性能优化：
 * - 清除旧监听器（避免ViewHolder重用时的状态混乱）
 * - DiffUtil处理：只更新有变化的项
 * - 支持1000+项目的流畅滚动
 * 
 * 交互流程：
 * - 点击复选框：调用onToggleDone()切换完成状态
 * - 点击删除按钮：调用onDelete()删除待办项
 * - 点击提醒按钮：调用onSetReminder()打开时间选择对话框
 */
class TodoAdapter(
    // 切换完成状态时的回调
    private val onToggleDone: (TodoEntity) -> Unit,
    // 删除待办项时的回调
    private val onDelete: (TodoEntity) -> Unit,
    // 设置提醒时间时的回调
    private val onSetReminder: (TodoEntity) -> Unit
) : ListAdapter<TodoEntity, TodoAdapter.TodoViewHolder>(TodoDiffCallback()) {

    /**
     * 待办项的ViewHolder
     * 
     * 职责：
     * 1. 管理单个待办项的UI控件
     * 2. 绑定待办数据到UI
     * 3. 处理用户交互（完成/删除/提醒）
     * \n     * 注意：
     * - 必须在bind()中清除旧监听器（避免重用时的状态混乱）
     * - 提醒时间颜色根据是否已到达动态改变
     */
    inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 待办项标题
        val title: TextView = itemView.findViewById(R.id.todo_title)
        // 完成状态复选框
        val checkBox: CheckBox = itemView.findViewById(R.id.todo_checkbox)
        // 删除按钮
        val deleteBtn: ImageButton = itemView.findViewById(R.id.todo_delete)
        // 分类标签（如果有的话）
        val categoryTag: TextView = itemView.findViewById(R.id.todo_category)
        // 提醒按钮（可选，item_todo可能不包含）
        val reminderBtn: ImageButton? = itemView.findViewById<ImageButton?>(R.id.todo_reminder)
        // 提醒时间显示（可选）
        val reminderTime: TextView? = itemView.findViewById<TextView?>(R.id.todo_reminder_time)

        /**
         * 绑定待办项数据到UI
         * 
         * 功能：
         * 1. 设置标题和完成状态
         * 2. 设置复选框状态（清除旧监听器避免混乱）
         * 3. 设置删除按钮
         * 4. 显示提醒时间并根据是否已到达改变颜色
         * 5. 设置提醒按钮
         * 
         * @param todo 待办项数据对象
         */
        fun bind(todo: TodoEntity) {
            // 设置标题
            title.text = todo.title
            
            // 根据完成状态设置UI样式
            if (todo.isDone) {
                // 已完成：显示删除线、半透明
                title.paintFlags = title.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                title.alpha = 0.5f
            } else {
                // 未完成：正常显示
                title.paintFlags = title.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                title.alpha = 1f
            }
            
            // 清除旧监听器（重要！避免ViewHolder重用时的状态混乱）
            // 如果不清除，旧的监听器可能仍然绑定到新的数据项
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = todo.isDone
            // 设置新监听器
            checkBox.setOnCheckedChangeListener { _, _ ->
                onToggleDone(todo)
            }
            
            // 设置删除按钮
            deleteBtn.setOnClickListener {
                onDelete(todo)
            }

            // 显示和管理提醒时间
            if (todo.remindTime.isNotEmpty()) {
                reminderTime?.text = todo.remindTime
                reminderTime?.visibility = View.VISIBLE
                
                // 根据提醒时间是否已到达改变显示颜色
                // 已到达：橙色（已过期的提醒）
                // 未到达：蓝色（待提醒）
                if (AlarmUtils.hasReachedTime(todo.remindTime)) {
                    reminderTime?.setTextColor(
                        itemView.context.getColor(android.R.color.holo_orange_light)
                    )
                } else {
                    reminderTime?.setTextColor(
                        itemView.context.getColor(android.R.color.holo_blue_dark)
                    )
                }
            } else {
                // 没有提醒时间，隐藏显示
                reminderTime?.visibility = View.GONE
            }

            // 设置提醒按钮（打开时间选择对话框）
            reminderBtn?.setOnClickListener {
                onSetReminder(todo)
            }
        }
    }

    /**
     * 创建ViewHolder
     * 
     * 在RecyclerView需要新的ViewHolder时调用
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    /**
     * 绑定数据到ViewHolder
     * 
     * 绑定位置position处的待办项数据
     */
    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * DiffUtil回调，用于计算列表差异
     * 
     * ListAdapter使用此回调来确定：
     * 1. 哪些项新增/删除/移动（areItemsTheSame）
     * 2. 哪些项内容变化（areContentsTheSame）
     * 
     * 然后自动计算最小化的更新操作，避免全列表刷新
     */
    class TodoDiffCallback : DiffUtil.ItemCallback<TodoEntity>() {
        /**
         * 检查两个项是否相同（根据ID）
         */
        override fun areItemsTheSame(oldItem: TodoEntity, newItem: TodoEntity): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * 检查两个项的内容是否相同
         */
        override fun areContentsTheSame(oldItem: TodoEntity, newItem: TodoEntity): Boolean {
            return oldItem == newItem
        }
    }
}
