package com.example.minitodo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 分类数据访问对象（DAO）
 * 
 * 作用：提供分类的数据库操作接口
 * 
 * 查询特点：
 * - 使用Flow实现响应式查询，数据变化时自动通知
 * - 支持按创建时间排序
 * - 支持按ID查询单个分类
 * 
 * 操作类型：
 * 1. 查询：getAllCategories()、getCategoryById()
 * 2. 新增：insert()
 * 3. 修改：update()
 * 4. 删除：delete()
 */
@Dao
interface CategoryDao {
    /**
     * 获取所有分类
     * 
     * 返回：所有分类的Flow流，自动观察数据变化
     * 排序：按创建时间降序（最新的在前）
     * 
     * 使用场景：分类列表的数据源
     */
    @Query("SELECT * FROM category_table ORDER BY createdAt DESC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    /**
     * 按ID查询单个分类
     * 
     * @param id 分类ID
     * 返回：分类对象，如果不存在则返回null
     * 
     * 特点：suspend函数，在协程中执行
     */
    @Query("SELECT * FROM category_table WHERE id = :id")
    suspend fun getCategoryById(id: Int): CategoryEntity?

    /**
     * 插入新分类
     * 
     * @param category 分类对象
     * 冲突策略：遇到主键冲突时覆盖旧记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    /**
     * 删除分类
     * 
     * @param category 分类对象
     * 
     * 级联处理：
     * - 分类下的待办项categoryId会设为null
     * - 由数据库外键约束自动处理
     */
    @Delete
    suspend fun delete(category: CategoryEntity)

    /**
     * 更新分类
     * 
     * @param category 分类对象（必须包含有效的id）
     * 
     * 更新内容：
     * - 分类名称
     * - 分类颜色
     */
    @Update
    suspend fun update(category: CategoryEntity)
}
