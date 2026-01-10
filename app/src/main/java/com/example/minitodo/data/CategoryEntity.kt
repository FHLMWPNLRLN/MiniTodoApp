package com.example.minitodo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_table")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val color: String = "#FF6200EE", // Material Design Purple default
    val createdAt: Long = System.currentTimeMillis()
)
