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

class TodoViewModel(
    private val todoDao: TodoDao
) : ViewModel() {

    private val _todos = MutableStateFlow<List<TodoEntity>>(emptyList())
    val todos: StateFlow<List<TodoEntity>> = _todos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _completedCount = MutableStateFlow(0)
    val completedCount: StateFlow<Int> = _completedCount.asStateFlow()

    private val _uncompletedCount = MutableStateFlow(0)
    val uncompletedCount: StateFlow<Int> = _uncompletedCount.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

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
