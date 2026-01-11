package com.example.minitodo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 待办事项实体类
 * 
 * 作用：代表数据库中的一条待办记录，Room会自动将其映射到todo_table表
 * 
 * 数据库表结构：
 * - todo_table: 存储所有待办事项
 * 
 * 字段说明：
 * - id: 主键，自动递增
 * - title: 待办标题
 * - isDone: 是否完成状态
 * - createdAt: 创建时间戳（毫秒）
 * - remindTime: 提醒时间（格式：yyyy-MM-dd HH:mm）
 */
@Entity(tableName = "todo_table")
data class TodoEntity(
    /**
     * 待办项的唯一标识符
     * - 自动递增
     * - 用于定位、更新和删除待办项
     */
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    
    /**
     * 待办项的标题/描述
     * - 用户输入的待办内容
     * - 显示在列表和详情中
     */
    val title: String,
    
    /**
     * 完成状态标志
     * - true: 已完成，UI显示删除线
     * - false: 未完成，正常显示
     * - 用于排序和统计
     */
    val isDone: Boolean = false,
    
    /**
     * 创建时间戳
     * - 毫秒级精度
     * - 用于按创建时间排序
     * - 默认为当前时间
     */
    val createdAt: Long = System.currentTimeMillis(),
    
    /**
     * 提醒时间
     * - 格式：yyyy-MM-dd HH:mm（例：2026-01-15 14:30）
     * - 空字符串表示无提醒
     * - AlarmUtils使用此时间设置系统闹钟
     * - 提醒时间会在UI中显示，并根据是否已到达改变颜色
     */
    val remindTime: String = ""
)
