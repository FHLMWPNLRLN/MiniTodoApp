package com.example.minitodo.data

import androidx.room.Embedded
import androidx.room.Relation

data class TodoWithCategory(
    @Embedded val todo: TodoEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?
)
