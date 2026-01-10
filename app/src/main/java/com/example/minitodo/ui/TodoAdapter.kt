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

class TodoAdapter(
    private val onToggleDone: (TodoEntity) -> Unit,
    private val onDelete: (TodoEntity) -> Unit
) : ListAdapter<TodoEntity, TodoAdapter.TodoViewHolder>(TodoDiffCallback()) {

    inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.todo_title)
        val checkBox: CheckBox = itemView.findViewById(R.id.todo_checkbox)
        val deleteBtn: ImageButton = itemView.findViewById(R.id.todo_delete)
        val categoryTag: TextView = itemView.findViewById(R.id.todo_category)

        fun bind(todo: TodoEntity) {
            title.text = todo.title
            // Apply strikethrough if done
            if (todo.isDone) {
                title.paintFlags = title.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                title.alpha = 0.5f
            } else {
                title.paintFlags = title.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                title.alpha = 1f
            }
            
            checkBox.isChecked = todo.isDone
            checkBox.setOnCheckedChangeListener { _, _ ->
                onToggleDone(todo)
            }
            
            deleteBtn.setOnClickListener {
                onDelete(todo)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TodoDiffCallback : DiffUtil.ItemCallback<TodoEntity>() {
        override fun areItemsTheSame(oldItem: TodoEntity, newItem: TodoEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TodoEntity, newItem: TodoEntity): Boolean {
            return oldItem == newItem
        }
    }
}
