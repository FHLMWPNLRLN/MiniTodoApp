package com.example.minitodo.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minitodo.data.TodoDao
import com.example.minitodo.data.TodoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * TodoViewModel - 待办事项业务逻辑层
 * 
 * 职责：
 * 1. 管理待办列表数据（todos、统计信息等）
 * 2. 实现MVVM架构中的ViewModel角色
 * 3. 处理待办的增删改查操作
 * 4. 实现数据排序和分组逻辑
 * 5. 与Room数据库进行交互
 * 6. 通过StateFlow向UI层提供响应式数据
 * 
 * 设计模式：
 * - MVVM架构：分离业务逻辑和UI
 * - Repository模式（通过TodoDao实现）
 * - 观察者模式：通过StateFlow实现
 * - 单一职责原则：仅关注业务逻辑
 */
class TodoViewModel(
    private val todoDao: TodoDao
) : ViewModel() {

    // ===== 数据流定义 =====
    
    /** 待办列表数据，按规则自动排序 */
    private val _todos = MutableStateFlow<List<TodoEntity>>(emptyList())
    val todos: StateFlow<List<TodoEntity>> = _todos.asStateFlow()

    /** 加载状态标志 */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** 错误消息，用于显示用户提示 */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** 已完成的待办数量 */
    private val _completedCount = MutableStateFlow(0)
    val completedCount: StateFlow<Int> = _completedCount.asStateFlow()

    /** 未完成的待办数量 */
    private val _uncompletedCount = MutableStateFlow(0)
    val uncompletedCount: StateFlow<Int> = _uncompletedCount.asStateFlow()

    /** 待办总数 */
    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    /** 是否需要滚动到顶部的标志（仅在添加新待办时为true） */
    private val _shouldScrollToTop = MutableStateFlow(false)
    val shouldScrollToTop: StateFlow<Boolean> = _shouldScrollToTop.asStateFlow()

    init {
        observeTodos()
    }

    private fun observeTodos() {
        viewModelScope.launch {
            try {
                todoDao.getAllTodos().collectLatest { todos ->
                    // 排序规则：
                    // 1. 未完成的优先级高（isDone = false）
                    // 2. 已完成的排在最后（isDone = true）
                    // 3. 在同一完成状态内，按提醒时间排序
                    // 4. 没有提醒时间的按创建时间降序
                    val sortedTodos = todos.sortedWith(
                        compareBy<TodoEntity> { it.isDone }  // 未完成(false) 在前，已完成(true) 在后
                            .thenBy { todo ->
                                // 在同一完成状态内，按提醒时间排序
                                if (todo.remindTime.isEmpty()) {
                                    Long.MAX_VALUE
                                } else {
                                    try {
                                        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA)
                                        dateFormat.parse(todo.remindTime)?.time ?: Long.MAX_VALUE
                                    } catch (e: Exception) {
                                        Long.MAX_VALUE
                                    }
                                }
                            }
                            .thenComparator { todo1, todo2 ->
                                // 当两个待办完成状态相同且都没有提醒时间时，按创建时间降序排列（新的在前）
                                if (todo1.remindTime.isEmpty() && todo2.remindTime.isEmpty()) {
                                    todo2.createdAt.compareTo(todo1.createdAt)
                                } else {
                                    0
                                }
                            }
                    )
                    _todos.value = sortedTodos
                    updateStatistics(sortedTodos)
                }
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Error observing todos", e)
                _errorMessage.value = "Failed to load todos: ${e.message}"
            }
        }
    }

    private fun updateStatistics(todos: List<TodoEntity>) {
        val completed = todos.count { it.isDone }
        val uncompleted = todos.count { !it.isDone }
        val total = todos.size

        _completedCount.value = completed
        _uncompletedCount.value = uncompleted
        _totalCount.value = total
    }

    fun addTodo(title: String) {
        val trimmedTitle = title.trim()

        // Validate input
        if (trimmedTitle.isEmpty()) {
            _errorMessage.value = "Todo title cannot be empty"
            return
        }

        if (trimmedTitle.length > 200) {
            _errorMessage.value = "Todo title is too long (max 200 characters)"
            return
        }

        viewModelScope.launch {
            try {
                _shouldScrollToTop.value = true  // 标记需要滚动到顶部
                val todo = TodoEntity(title = trimmedTitle)
                todoDao.insert(todo)
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Error adding todo", e)
                _errorMessage.value = "Failed to add todo: ${e.message}"
            }
        }
    }

    fun toggleTodo(todo: TodoEntity) {
        viewModelScope.launch {
            try {
                todoDao.update(todo.copy(isDone = !todo.isDone))
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Error toggling todo", e)
                _errorMessage.value = "Failed to update todo: ${e.message}"
            }
        }
    }

    fun updateTodo(todo: TodoEntity) {
        viewModelScope.launch {
            try {
                todoDao.update(todo)
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Error updating todo", e)
                _errorMessage.value = "Failed to update todo: ${e.message}"
            }
        }
    }

    fun deleteTodo(todo: TodoEntity) {
        viewModelScope.launch {
            try {
                todoDao.delete(todo)
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Error deleting todo", e)
                _errorMessage.value = "Failed to delete todo: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearScrollFlag() {
        _shouldScrollToTop.value = false
    }
}
