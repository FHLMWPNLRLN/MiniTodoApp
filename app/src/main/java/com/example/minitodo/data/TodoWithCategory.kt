package com.example.minitodo.data

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 待办项与分类关联数据模型
 * 
 * 作用：在一次查询中同时获取待办项和其分类信息
 * 
 * 设计原理：
 * - 使用@Embedded嵌入TodoEntity的所有字段
 * - 使用@Relation自动关联CategoryEntity
 * - Room会自动执行JOIN操作关联两个表
 * 
 * 字段说明：
 * - todo: 待办项对象，包含所有待办字段
 * - category: 分类对象（nullable），当categoryId为null时category为null
 * 
 * 使用场景：
 * 1. 分组显示（按分类展示待办项）
 * 2. 显示待办项时需要分类信息（名称、颜色）
 * 3. 复杂查询涉及多个表
 * 
 * 关键设计：
 * - 关联条件：todo.categoryId = category.id
 * - 支持null值关联（未分类的待办项）
 * - 自动级联加载，提高性能
 */
data class TodoWithCategory(
    /**
     * 嵌入的待办项对象
     * - 包含id、title、isDone、categoryId、createdAt、remindTime等字段
     * - 直接作为结果的属性
     */
    @Embedded val todo: TodoEntity,
    
    /**
     * 关联的分类对象
     * - 根据todo.categoryId自动关联CategoryEntity
     * - 如果categoryId为null则category为null
     * - 用于获取分类名称和颜色
     */
    @Relation(
        parentColumn = "categoryId",    // 待办表的外键列
        entityColumn = "id"              // 分类表的主键列
    )
    val category: CategoryEntity?
)
