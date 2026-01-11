package com.example.minitodo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.minitodo.data.TodoDao

/**
 * ViewModel工厂类
 * 
 * 作用：创建带有参数的TodoViewModel实例
 * 
 * 为什么需要工厂：
 * - ViewModel的标准构造函数不能有参数
 * - 但TodoViewModel需要TodoDao依赖注入
 * - 使用ViewModelProvider.Factory模式解决此问题
 * 
 * 工作原理：
 * 1. MainActivity或其他组件请求ViewModel实例
 * 2. Fragment/Activity调用ViewModelProvider(this).get()
 * 3. 系统检查是否有对应的Factory
 * 4. 如果有，调用factory.create()创建ViewModel
 * 5. factory.create()负责创建TodoViewModel并注入依赖
 * 使用示例：
 * ```kotlin
 * val factory = TodoViewModelFactory(database.todoDao())
 * val viewModel = ViewModelProvider(this, factory).get(TodoViewModel::class.java)
 * ```
 * 
 * 好处：
 * - 支持依赖注入（Dao层）
 * - ViewModel可以直接访问数据库
 * - 解耦UI层和数据层
 */
class TodoViewModelFactory(
    // 待办数据访问对象，用于创建TodoViewModel时注入
    private val todoDao: TodoDao
) : ViewModelProvider.Factory {
    
    /**
     * 创建ViewModel实例
     * 
     * 系统调用此方法来创建所请求的ViewModel
     * 
     * @param modelClass 要创建的ViewModel类
     * @return 新创建的ViewModel实例
     * 
     * @throws IllegalArgumentException 如果modelClass不是TodoViewModel
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            // 创建TodoViewModel并注入todoDao
            return TodoViewModel(todoDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
