package com.example.minitodo.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.minitodo.R
import com.example.minitodo.data.TodoEntity

/**
 * 分组列表项的密封类
 *
 * 用途：表示分组列表中的两种不同类型项：
 * 1. Header - 分类标题和展开/折叠按钮
 * 2. TodoItem - 具体的待办项
 *
 * 密封类优点：
 * - 类型安全
 * - 编译器检查when语句完整性
 * - 避免使用instanceof
 */
sealed class GroupedListItem {
    /**
     * 分类标题项
     *
     * @param categoryId 分类ID（null表示"未分类"）
     * @param name 分类名称
     * @param expanded 是否展开（显示该分类下的待办项）
     * @param count 该分类下的待办项数量
     */
    data class Header(val categoryId: Int?, val name: String, var expanded: Boolean = true, val count: Int = 0) : GroupedListItem()
    
    /**
     * 待办项
     *
     * @param todo 待办项数据对象
     */
    data class TodoItem(val todo: TodoEntity) : GroupedListItem()
}

/**
 * 分组待办列表适配器
 *
 * 作用：管理按分类分组显示的待办项列表
 *
 * 功能：
 * 1. 支持按分类分组显示待办项
 * 2. 支持分类的展开/折叠
 * 3. 每个分类显示项目数量
 * 4. 支持同一列表中的多个ViewHolder类型
 * 5. 支持待办项的增删改操作
 *
 * 设计模式：
 * - 继承ListAdapter：使用DiffUtil计算差异
 * - 多ViewHolder类型：Header和TodoItem
 * - 密封类：安全的类型管理
 *
 * 适配场景：
 * - 分类视图中显示"已完成"、"未完成"、"有提醒"等分组
 * - 每个分组可以独立展开/折叠
 * - 列表项混合Header和Item
 */
class GroupedTodoAdapter(
    // 切换完成状态时的回调
    private val onToggleDone: (TodoEntity) -> Unit,
    // 删除待办项时的回调
    private val onDelete: (TodoEntity) -> Unit,
    // 获取分类颜色的回调（返回16进制颜色字符串）
    private val getCategoryColor: (Int?) -> String? = { null },
    // 获取分类名称的回调
    private val getCategoryName: (Int?) -> String? = { null },
    // 切换分组展开/折叠时的回调
    private val onToggleHeader: (categoryId: Int?) -> Unit,
    // 设置提醒时间时的回调
    private val onSetReminder: (TodoEntity) -> Unit = {}
) : ListAdapter<GroupedListItem, RecyclerView.ViewHolder>(GroupedDiffCallback()) {

    companion object {
        // ViewHolder类型：分类标题
        private const val TYPE_HEADER = 0
        // ViewHolder类型：待办项
        private const val TYPE_TODO = 1
    }

    /**
     * 分类标题的ViewHolder
     *
     * 显示分类名称和项目数，支持展开/折叠
     */
    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.header_title)
        val toggle: ImageButton = itemView.findViewById(R.id.header_toggle)

        /**
         * 绑定分类标题数据
         *
         * @param item 分类标题项
         */
        fun bind(item: GroupedListItem.Header) {
            // 显示分类名称和项目数："已完成 (5)"
            title.text = "${item.name} (${item.count})"
            // 根据展开状态设置箭头方向
            // 展开：向下(0°)，折叠：向左(-90°)
            toggle.rotation = if (item.expanded) 0f else -90f
            toggle.setOnClickListener {
                onToggleHeader(item.categoryId)
            }
        }
    }

    /**
     * 待办项的ViewHolder
     *
     * 显示待办项详情和交互按钮
     */
    inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.todo_title)
        private val deleteBtn: ImageButton = itemView.findViewById(R.id.todo_delete)
        private val categoryTag: TextView = itemView.findViewById(R.id.todo_category)
        private val checkBox = itemView.findViewById<android.widget.CheckBox>(R.id.todo_checkbox)
        private val reminderBtn: ImageButton? = itemView.findViewById<ImageButton?>(R.id.todo_reminder)
        private val reminderTime: TextView? = itemView.findViewById<TextView?>(R.id.todo_reminder_time)

        /**
         * 绑定待办项数据
         *
         * 功能：
         * 1. 设置标题和完成状态
         * 2. 绑定交互回调
         * 3. 显示提醒时间（带颜色）
         * 4. 显示分类标签
         *
         * @param todo 待办项数据对象
         */
        fun bind(todo: TodoEntity) {
            title.text = todo.title
            // 根据完成状态设置UI样式
            if (todo.isDone) {
                title.paintFlags = title.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                title.alpha = 0.5f
            } else {
                title.paintFlags = title.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                title.alpha = 1f
            }

            // 清除旧监听器，避免ViewHolder重用时的混乱
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = todo.isDone
            checkBox.setOnCheckedChangeListener { _, _ -> onToggleDone(todo) }
            deleteBtn.setOnClickListener { onDelete(todo) }

            // 设置提醒时间显示
            if (todo.remindTime.isNotEmpty()) {
                reminderTime?.text = todo.remindTime
                reminderTime?.visibility = View.VISIBLE
                // 根据提醒时间是否已到达改变颜色
                if (com.example.minitodo.notification.AlarmUtils.hasReachedTime(todo.remindTime)) {
                    reminderTime?.setTextColor(
                        itemView.context.getColor(android.R.color.holo_orange_light)
                    )
                } else {
                    reminderTime?.setTextColor(
                        itemView.context.getColor(android.R.color.holo_blue_dark)
                    )
                }
            } else {
                reminderTime?.visibility = View.GONE
            }

            reminderBtn?.setOnClickListener {
                onSetReminder(todo)
            }

            // 设置分类标签显示
            val name = getCategoryName(todo.categoryId)
            val color = getCategoryColor(todo.categoryId)
            if (name != null) {
                categoryTag.visibility = View.VISIBLE
                categoryTag.text = name
                if (color != null) {
                    try { categoryTag.setBackgroundColor(Color.parseColor(color)) } catch (_: Exception) {}
                }
            } else {
                categoryTag.visibility = View.GONE
            }
        }
    }

    /**
     * 获取ViewHolder的类型
     *
     * 根据列表项类型返回对应的ViewHolder类型常数
     */
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is GroupedListItem.Header -> TYPE_HEADER
            is GroupedListItem.TodoItem -> TYPE_TODO
        }
    }

    /**
     * 创建ViewHolder
     *
     * 根据类型创建不同的ViewHolder（Header或Todo）
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_category_header, parent, false)
            HeaderViewHolder(v)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
            TodoViewHolder(v)
        }
    }

    /**
     * 绑定数据到ViewHolder
     *
     * 根据项的类型调用对应ViewHolder的bind()方法
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is GroupedListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is GroupedListItem.TodoItem -> (holder as TodoViewHolder).bind(item.todo)
        }
    }
}

/**
 * DiffUtil回调，用于计算分组列表的差异
 *
 * 支持两种类型的项（Header和TodoItem）的差异计算
 */
class GroupedDiffCallback : DiffUtil.ItemCallback<GroupedListItem>() {
    /**
     * 检查两个项是否相同
     *
     * - Header：比较categoryId
     * - TodoItem：比较todo id
     */
    override fun areItemsTheSame(oldItem: GroupedListItem, newItem: GroupedListItem): Boolean {
        return if (oldItem is GroupedListItem.Header && newItem is GroupedListItem.Header) {
            oldItem.categoryId == newItem.categoryId
        } else if (oldItem is GroupedListItem.TodoItem && newItem is GroupedListItem.TodoItem) {
            oldItem.todo.id == newItem.todo.id
        } else false
    }

    /**
     * 检查两个项的内容是否相同
     */
    override fun areContentsTheSame(oldItem: GroupedListItem, newItem: GroupedListItem): Boolean {
        return oldItem == newItem
    }
}
