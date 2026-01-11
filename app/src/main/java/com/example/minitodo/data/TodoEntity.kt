package com.example.minitodo.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todo_table",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("categoryId")  // 添加索引以提高查询性能
    ]
)
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val isDone: Boolean = false,
    val categoryId: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val remindTime: String = ""  // 提醒时间 (格式: yyyy-MM-dd HH:mm)
)
