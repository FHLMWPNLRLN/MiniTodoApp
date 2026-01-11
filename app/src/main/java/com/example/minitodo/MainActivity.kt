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

/**
 * MiniTodoApp主界面Activity
 * 
 * 功能职责：
 * 1. 展示待办列表并支持上下滑动
 * 2. 提供待办的增删改操作入口
 * 3. 显示待办统计信息（总数、已完成、未完成）
 * 4. 管理提醒时间设置对话框
 * 5. 响应ViewModel数据变化并更新UI
 * 
 * 架构说明：
 * - 使用MVVM架构，业务逻辑在TodoViewModel中
 * - 使用Room数据库进行数据持久化
 * - 使用RecyclerView显示列表，支持1000+项流畅滑动
 * - 使用StateFlow实现响应式数据绑定
 */
class MainActivity : AppCompatActivity() {

    // ViewModel管理业务逻辑和数据
    private lateinit var todoViewModel: TodoViewModel
    
    // RecyclerView适配器，用于展示待办列表
    private lateinit var todoAdapter: TodoAdapter
    
    // 空状态提示文本（无待办时显示）
    private lateinit var emptyStateView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 创建Android 8.0+所需的通知渠道
        NotificationUtils.createNotificationChannel(this)

        // 获取UI控件引用
        val inputTodo = findViewById<EditText>(R.id.input_todo)
        val buttonAdd = findViewById<Button>(R.id.button_add)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_todo)
        emptyStateView = findViewById(R.id.empty_state)
        val totalCountView = findViewById<TextView>(R.id.total_count)
        val completedCountView = findViewById<TextView>(R.id.completed_count)
        val uncompletedCountView = findViewById<TextView>(R.id.uncompleted_count)

        // 初始化ViewModel和数据库
        val todoDao = TodoDatabase.getDatabase(this).todoDao()
        val factory = TodoViewModelFactory(todoDao)
        todoViewModel = ViewModelProvider(this, factory).get(TodoViewModel::class.java)

        // 初始化RecyclerView和适配器
        todoAdapter = TodoAdapter(
            // 完成按钮回调：标记完成时移除已设置的提醒
            onToggleDone = { todo ->
                // 如果标记为完成且有提醒时间，则移除闹钟
                if (todo.isDone && todo.remindTime.isNotEmpty()) {
                    AlarmUtils.removeAlarm(this, todo.id)
                }
                // 切换完成状态
                todoViewModel.toggleTodo(todo)
            },
            // 删除按钮回调：删除时清理关联资源
            onDelete = { todo ->
                // 删除前移除关联的闹钟
                if (todo.remindTime.isNotEmpty()) {
                    AlarmUtils.removeAlarm(this, todo.id)
                }
                // 从数据库删除待办
                todoViewModel.deleteTodo(todo)
            },
            // 提醒按钮回调：打开时间选择对话框
            onSetReminder = { todo ->
                showReminderDialog(todo)
            }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = todoAdapter
        }

        // 添加按钮点击监听：创建新待办
        buttonAdd.setOnClickListener {
            val text = inputTodo.text.toString()
            // 调用ViewModel添加待办
            todoViewModel.addTodo(text)
            // 清空输入框
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

