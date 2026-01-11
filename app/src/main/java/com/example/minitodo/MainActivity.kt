package com.example.minitodo

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.minitodo.data.TodoDatabase
import com.example.minitodo.data.TodoEntity
import com.example.minitodo.notification.AlarmUtils
import com.example.minitodo.notification.NotificationUtils
import com.example.minitodo.ui.ReminderDialogFragment
import com.example.minitodo.ui.TodoAdapter
import com.example.minitodo.viewmodel.TodoViewModel
import com.example.minitodo.viewmodel.TodoViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var todoViewModel: TodoViewModel
    private lateinit var todoAdapter: TodoAdapter
    private lateinit var emptyStateView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 创建通知渠道
        NotificationUtils.createNotificationChannel(this)

        val inputTodo = findViewById<EditText>(R.id.input_todo)
        val buttonAdd = findViewById<Button>(R.id.button_add)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_todo)
        emptyStateView = findViewById(R.id.empty_state)
        val totalCountView = findViewById<TextView>(R.id.total_count)
        val completedCountView = findViewById<TextView>(R.id.completed_count)
        val uncompletedCountView = findViewById<TextView>(R.id.uncompleted_count)

        // Initialize ViewModel
        val todoDao = TodoDatabase.getDatabase(this).todoDao()
        val factory = TodoViewModelFactory(todoDao)
        todoViewModel = ViewModelProvider(this, factory).get(TodoViewModel::class.java)

        // Initialize RecyclerView
        todoAdapter = TodoAdapter(
            onToggleDone = { todo ->
                // 标记完成时移除提醒
                if (todo.isDone && todo.remindTime.isNotEmpty()) {
                    AlarmUtils.removeAlarm(this, todo.id)
                }
                todoViewModel.toggleTodo(todo)
            },
            onDelete = { todo ->
                // 删除时移除提醒
                if (todo.remindTime.isNotEmpty()) {
                    AlarmUtils.removeAlarm(this, todo.id)
                }
                todoViewModel.deleteTodo(todo)
            },
            onSetReminder = { todo ->
                showReminderDialog(todo)
            }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = todoAdapter
        }

        // Add button click listener
        buttonAdd.setOnClickListener {
            val text = inputTodo.text.toString()
            todoViewModel.addTodo(text)
            inputTodo.text.clear()
        }

        // Observe todos
        lifecycleScope.launch {
            todoViewModel.todos.collect { todos ->
                // 保存当前滚动位置
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                val offset = if (firstVisiblePosition >= 0) {
                    val view = layoutManager.findViewByPosition(firstVisiblePosition)
                    view?.top ?: 0
                } else {
                    0
                }

                todoAdapter.submitList(todos) {
                    // 列表更新完成后，恢复之前的滚动位置
                    if (firstVisiblePosition >= 0 && firstVisiblePosition < todos.size) {
                        layoutManager.scrollToPositionWithOffset(firstVisiblePosition, offset)
                    }
                }
                updateEmptyState(todos.isEmpty())
            }
        }

        // Observe scroll flag
        lifecycleScope.launch {
            todoViewModel.shouldScrollToTop.collect { shouldScroll ->
                if (shouldScroll) {
                    recyclerView.smoothScrollToPosition(0)
                    todoViewModel.clearScrollFlag()
                }
            }
        }

        // Observe error messages
        lifecycleScope.launch {
            todoViewModel.errorMessage.collect { error ->
                if (error != null) {
                    Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                    todoViewModel.clearError()
                }
            }
        }

        // Observe statistics
        lifecycleScope.launch {
            todoViewModel.totalCount.collect { total ->
                totalCountView.text = "总计：$total"
            }
        }

        lifecycleScope.launch {
            todoViewModel.completedCount.collect { completed ->
                completedCountView.text = "已完成：$completed"
            }
        }

        lifecycleScope.launch {
            todoViewModel.uncompletedCount.collect { uncompleted ->
                uncompletedCountView.text = "未完成：$uncompleted"
            }
        }
    }

    private fun showReminderDialog(todo: TodoEntity) {
        val dialog = ReminderDialogFragment()
        dialog.setListener(object : ReminderDialogFragment.OnReminderSetListener {
            override fun onReminderSet(dateTime: String) {
                val updatedTodo = todo.copy(remindTime = dateTime)
                todoViewModel.updateTodo(updatedTodo)
                AlarmUtils.setAlarm(this@MainActivity, dateTime, todo.title, todo.id)
                Toast.makeText(this@MainActivity, "提醒已设置：$dateTime", Toast.LENGTH_SHORT).show()
            }
        })
        dialog.show(supportFragmentManager, "reminder_dialog")
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            emptyStateView.visibility = android.view.View.VISIBLE
        } else {
            emptyStateView.visibility = android.view.View.GONE
        }
    }
}

