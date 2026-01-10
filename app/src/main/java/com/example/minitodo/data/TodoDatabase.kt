package com.example.minitodo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TodoEntity::class, CategoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null

        // 数据库迁移：从版本1到版本2，添加分类表并修改 todo_table 以包含外键和索引
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1) 创建新的 category_table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `category_table` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `color` TEXT NOT NULL DEFAULT '#FF6200EE',
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // 2) 创建一个新的 todo_table_new，包含外键约束和索引（与实体定义一致）
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `todo_table_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `isDone` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `categoryId` INTEGER,
                        FOREIGN KEY(`categoryId`) REFERENCES `category_table`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                // 3) 将旧表数据拷贝到新表，确保为 createdAt 提供非空默认值
                // 检查旧表是否包含 createdAt 列；如果没有则使用常量 0
                val cursor = database.query("PRAGMA table_info(`todo_table`)")
                var hasCreatedAt = false
                try {
                    while (cursor.moveToNext()) {
                        val colName = cursor.getString(1)
                        if (colName == "createdAt") {
                            hasCreatedAt = true
                            break
                        }
                    }
                } finally {
                    cursor.close()
                }

                if (hasCreatedAt) {
                    database.execSQL(
                        """
                        INSERT INTO `todo_table_new` (id, title, isDone, createdAt, categoryId)
                        SELECT id, title, isDone, COALESCE(createdAt, 0) as createdAt, NULL as categoryId
                        FROM `todo_table`;
                        """.trimIndent()
                    )
                } else {
                    database.execSQL(
                        """
                        INSERT INTO `todo_table_new` (id, title, isDone, createdAt, categoryId)
                        SELECT id, title, isDone, 0 as createdAt, NULL as categoryId
                        FROM `todo_table`;
                        """.trimIndent()
                    )
                }

                // 4) 删除旧表并将新表重命名为 todo_table
                database.execSQL("DROP TABLE IF EXISTS `todo_table`")
                database.execSQL("ALTER TABLE `todo_table_new` RENAME TO `todo_table`")

                // 5) 创建索引，Room 期望存在此索引
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_table_categoryId` ON `todo_table` (`categoryId` ASC)")
            }
        }

        fun getDatabase(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "todo_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

