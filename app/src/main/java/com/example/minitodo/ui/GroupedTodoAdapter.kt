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

sealed class GroupedListItem {
    data class Header(val categoryId: Int?, val name: String, var expanded: Boolean = true, val count: Int = 0) : GroupedListItem()
    data class TodoItem(val todo: TodoEntity) : GroupedListItem()
}

class GroupedTodoAdapter(
    private val onToggleDone: (TodoEntity) -> Unit,
    private val onDelete: (TodoEntity) -> Unit,
    private val getCategoryColor: (Int?) -> String? = { null },
    private val getCategoryName: (Int?) -> String? = { null },
    private val onToggleHeader: (categoryId: Int?) -> Unit
) : ListAdapter<GroupedListItem, RecyclerView.ViewHolder>(GroupedDiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TODO = 1
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.header_title)
        val toggle: ImageButton = itemView.findViewById(R.id.header_toggle)

        fun bind(item: GroupedListItem.Header) {
            title.text = "${item.name} (${item.count})"
            toggle.rotation = if (item.expanded) 0f else -90f
            toggle.setOnClickListener {
                onToggleHeader(item.categoryId)
            }
        }
    }

    inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.todo_title)
        private val deleteBtn: ImageButton = itemView.findViewById(R.id.todo_delete)
        private val categoryTag: TextView = itemView.findViewById(R.id.todo_category)
        private val checkBox = itemView.findViewById<android.widget.CheckBox>(R.id.todo_checkbox)

        fun bind(todo: TodoEntity) {
            title.text = todo.title
            if (todo.isDone) {
                title.paintFlags = title.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                title.alpha = 0.5f
            } else {
                title.paintFlags = title.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                title.alpha = 1f
            }

            checkBox.isChecked = todo.isDone
            checkBox.setOnCheckedChangeListener { _, _ -> onToggleDone(todo) }
            deleteBtn.setOnClickListener { onDelete(todo) }

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

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is GroupedListItem.Header -> TYPE_HEADER
            is GroupedListItem.TodoItem -> TYPE_TODO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_category_header, parent, false)
            HeaderViewHolder(v)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
            TodoViewHolder(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is GroupedListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is GroupedListItem.TodoItem -> (holder as TodoViewHolder).bind(item.todo)
        }
    }
}

class GroupedDiffCallback : DiffUtil.ItemCallback<GroupedListItem>() {
    override fun areItemsTheSame(oldItem: GroupedListItem, newItem: GroupedListItem): Boolean {
        return if (oldItem is GroupedListItem.Header && newItem is GroupedListItem.Header) {
            oldItem.categoryId == newItem.categoryId
        } else if (oldItem is GroupedListItem.TodoItem && newItem is GroupedListItem.TodoItem) {
            oldItem.todo.id == newItem.todo.id
        } else false
    }

    override fun areContentsTheSame(oldItem: GroupedListItem, newItem: GroupedListItem): Boolean {
        return oldItem == newItem
    }
}
