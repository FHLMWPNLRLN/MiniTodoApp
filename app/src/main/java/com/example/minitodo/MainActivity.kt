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

        val inputTodo = findViewById<EditText>(R.id.input_todo)
        val buttonAdd = findViewById<Button>(R.id.button_add)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_todo)
        emptyStateView = findViewById(R.id.empty_state)

        // Initialize ViewModel
        val todoDao = TodoDatabase.getDatabase(this).todoDao()
        val factory = TodoViewModelFactory(todoDao)
        todoViewModel = ViewModelProvider(this, factory).get(TodoViewModel::class.java)

        // Initialize RecyclerView
        todoAdapter = TodoAdapter(
            onToggleDone = { todo ->
                todoViewModel.toggleTodo(todo)
            },
            onDelete = { todo ->
                todoViewModel.deleteTodo(todo)
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
                todoAdapter.submitList(todos)
                updateEmptyState(todos.isEmpty())
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
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            emptyStateView.visibility = android.view.View.VISIBLE
        } else {
            emptyStateView.visibility = android.view.View.GONE
        }
    }
}
