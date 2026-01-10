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

    init {
        observeTodos()
    }

    private fun observeTodos() {
        viewModelScope.launch {
            try {
                todoDao.getAllTodos().collectLatest { todos ->
                    _todos.value = todos
                }
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Error observing todos", e)
                _errorMessage.value = "Failed to load todos: ${e.message}"
            }
        }
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
}
