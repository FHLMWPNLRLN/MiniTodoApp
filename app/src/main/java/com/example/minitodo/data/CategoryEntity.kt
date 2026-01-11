package com.example.minitodo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 分类实体类
 * 
 * 作用：代表数据库中的一个分类记录，用于对待办项进行分组
 * 
 * 数据库表结构：
 * - category_table: 存储所有分类
 * 
 * 字段说明：
 * - id: 主键，自动递增。id=0时是特殊的"未分类"分类
 * - name: 分类名称
 * - color: 分类颜色（16进制RGB，例：#FF6200EE）
 * - createdAt: 创建时间戳
 * 
 * 特殊逻辑：
 * - id=0 是内置的"未分类"分类，不能删除
 * - 删除分类时，所属待办项的categoryId会设为null（级联规则）
 * - 颜色用于在UI中区分不同的分类标签
 */
@Entity(tableName = "category_table")
data class CategoryEntity(
    /**
     * 分类的唯一标识符
     * - 自动递增
     * - id=0是特殊的"未分类"分类
     */
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    
    /**
     * 分类的名称
     * - 显示在分类列表中
     * - 作为待办项的分类标签
     */
    val name: String,
    
    /**
     * 分类的显示颜色
     * - 16进制ARGB格式（#AARRGGBB）
     * - 默认：#FF6200EE（Material Design Purple）
     * - 用于分类标签的背景色
     */
    val color: String = "#FF6200EE",
    
    /**
     * 分类的创建时间戳
     * - 毫秒级精度
     * - 用于按创建时间排序
     */
    val createdAt: Long = System.currentTimeMillis()
)
