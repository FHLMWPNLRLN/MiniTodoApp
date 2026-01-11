package com.example.minitodo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 待办事项数据访问对象（DAO）
 * 
 * 作用：Room框架的数据库操作接口，提供待办事项的CRUD（增删改查）操作
 * 
 * 设计模式：
 * - 使用Flow<T>实现响应式查询，自动观察数据变化
 * - 使用suspend函数实现协程异步操作
 * 
 * 查询策略：
 * 1. getAllTodos(): 获取所有待办项，用于主列表显示
 * 2. searchTodos(): 全文搜索待办项标题
 * 3. getUncompletedTodos(): 获取未完成的待办项
 * 
 * 修改操作：
 * - insert(): 新增待办项
 * - update(): 更新待办项
 * - delete(): 删除待办项
 */
@Dao
interface TodoDao {
    /**
     * 获取所有待办项
     * 
     * 功能：查询数据库中的所有待办项
     * 返回：Flow流，自动观察数据变化
     * 排序：按创建时间降序（最新的在前）
     * 
     * 使用场景：主列表的数据源，当数据库数据变化时自动通知UI
     */
    @Query("SELECT * FROM todo_table ORDER BY createdAt DESC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    /**
     * 搜索待办项
     * 
     * 功能：模糊匹配待办项标题
     * @param query 搜索关键词
     * 返回：匹配的待办项列表（Flow流）
     * 
     * 实现：使用SQL LIKE模糊匹配，支持中英文搜索
     */
    @Query("SELECT * FROM todo_table WHERE title LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchTodos(query: String): Flow<List<TodoEntity>>

    /**
     * 获取未完成的待办项
     * 
     * 功能：查询所有isDone=false的待办项
     * 返回：未完成待办项的Flow流
     * 
     * 使用场景：用于统计或显示未完成任务列表
     */
    @Query("SELECT * FROM todo_table WHERE isDone = 0 ORDER BY createdAt DESC")
    fun getUncompletedTodos(): Flow<List<TodoEntity>>

    /**
     * 插入新的待办项
     * 
     * 功能：向数据库添加新的待办事项
     * @param todo 待办项对象
     * 
     * 特点：
     * - suspend函数，在协程中执行
     * - onConflict = REPLACE: 遇到主键冲突时覆盖旧记录
     * - Room会自动生成插入的ID并回传
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoEntity)

    /**
     * 删除待办项
     * 
     * 功能：从数据库删除指定的待办项
     * @param todo 待办项对象
     * 
     * 注意：
     * - 也会触发清除相关的闹钟
     * - 级联删除由ViewModel控制
     */
    @Delete
    suspend fun delete(todo: TodoEntity)

    /**
     * 更新待办项
     * 
     * 功能：修改现有的待告项数据
     * @param todo 待办项对象（必须包含有效的id）
     * 
     * 更新场景：
     * 1. 切换完成状态（isDone）
     * 2. 修改标题
     * 3. 改变分类
     * 4. 更新提醒时间
     * 
     * 特点：协程异步操作，不阻塞UI线程
     */
    @Update
    suspend fun update(todo: TodoEntity)
}

