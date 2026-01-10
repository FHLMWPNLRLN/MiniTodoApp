package com.example.minitodo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_table ORDER BY createdAt DESC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todo_table WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    fun getTodosByCategory(categoryId: Int): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todo_table WHERE title LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchTodos(query: String): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todo_table WHERE isDone = 0 ORDER BY createdAt DESC")
    fun getUncompletedTodos(): Flow<List<TodoEntity>>

    @Transaction
    @Query("SELECT * FROM todo_table ORDER BY createdAt DESC")
    fun getAllTodosWithCategory(): Flow<List<TodoWithCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoEntity)

    @Delete
    suspend fun delete(todo: TodoEntity)

    @Update
    suspend fun update(todo: TodoEntity)
}

