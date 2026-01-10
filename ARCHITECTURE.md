# MiniTodoApp 架构设计文档

## 1. 整体架构图

```
┌──────────────────────────────────────────────────────────────┐
│                     Android Framework                        │
│        (Activity, Fragment, RecyclerView, Resources)         │
└──────────────────────┬───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│                  Presentation Layer (UI)                     │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │         MainActivity                               │    │
│  │  - 管理 UI 组件和用户交互                          │    │
│  │  - 观察 ViewModel 状态变化                         │    │
│  │  - 显示错误提示和空状态                            │    │
│  └────────────┬──────────────────────────────────────┘    │
│               │                                             │
│  ┌────────────▼──────────┐  ┌──────────────────────────┐   │
│  │  TodoAdapter          │  │  CategoryAdapter         │   │
│  │  - ListAdapter        │  │  - ListAdapter           │   │
│  │  - DiffUtil 比对      │  │  - DiffUtil 比对         │   │
│  │  - 项目绑定 & 事件    │  │  - 分类选择              │   │
│  └──────────────────────┘  └──────────────────────────┘   │
│                                                              │
└──────────────────────┬───────────────────────────────────────┘
                       │ (观察 StateFlow)
                       │
┌──────────────────────▼───────────────────────────────────────┐
│           ViewModel Layer (Business Logic)                   │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │         TodoViewModel                              │    │
│  │                                                    │    │
│  │  StateFlow 管理:                                  │    │
│  │  - _todos: List<TodoEntity>                      │    │
│  │  - _categories: List<CategoryEntity>             │    │
│  │  - _selectedCategoryId: Int?                     │    │
│  │  - _searchQuery: String                          │    │
│  │  - _errorMessage: String?                        │    │
│  │                                                    │    │
│  │  业务方法:                                         │    │
│  │  - addTodo(title, categoryId?)                   │    │
│  │  - deleteTodo(todo)                              │    │
│  │  - toggleTodo(todo)                              │    │
│  │  - addCategory(name, color)                      │    │
│  │  - deleteCategory(category)                      │    │
│  │  - setSelectedCategory(id?)                      │    │
│  │  - setSearchQuery(query)                         │    │
│  │                                                    │    │
│  │  私有方法:                                         │    │
│  │  - loadTodos()          [从数据库加载]           │    │
│  │  - loadCategories()     [从数据库加载]           │    │
│  │  - filterTodos()        [应用过滤和搜索]         │    │
│  └────────────┬──────────────────────────────────────┘    │
│               │                                             │
│  ┌────────────▼──────────────────────────────────────┐    │
│  │    TodoViewModelFactory                           │    │
│  │    - 创建 ViewModel 实例                          │    │
│  │    - 注入 DAO 依赖                                │    │
│  └──────────────────────────────────────────────────┘    │
│                                                              │
└──────────────────────┬───────────────────────────────────────┘
                       │ (操作 DAO)
                       │
┌──────────────────────▼───────────────────────────────────────┐
│            Data/Repository Layer (Data Access)               │
│                                                              │
│  ┌──────────────────┐  ┌────────────────────────────────┐  │
│  │   TodoDao        │  │   CategoryDao                  │  │
│  │                  │  │                                │  │
│  │  getAllTodos()   │  │  getAllCategories()            │  │
│  │  getTodosByCategory()  │  getCategoryById()           │  │
│  │  searchTodos()   │  │  insert(category)              │  │
│  │  getUncompletedTodos() │  delete(category)           │  │
│  │  insert(todo)    │  │  update(category)              │  │
│  │  update(todo)    │  │                                │  │
│  │  delete(todo)    │  │                                │  │
│  └────────────┬─────┘  └────────────┬───────────────────┘  │
│               │                     │                       │
│               └──────────┬──────────┘                       │
│                          │ (执行 SQL)                       │
│                          │                                  │
│  ┌──────────────────────▼───────────────────────┐          │
│  │        TodoDatabase (Room)                   │          │
│  │                                              │          │
│  │  abstract fun todoDao(): TodoDao            │          │
│  │  abstract fun categoryDao(): CategoryDao    │          │
│  │                                              │          │
│  │  配置:                                       │          │
│  │  - entities: [TodoEntity, CategoryEntity]  │          │
│  │  - version: 2                               │          │
│  │  - migrations: [MIGRATION_1_2]             │          │
│  └──────────────────────┬──────────────────────┘          │
│                         │                                  │
└─────────────────────────┼──────────────────────────────────┘
                          │
┌─────────────────────────▼──────────────────────────────────┐
│            Persistence Layer (SQLite Database)             │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  todo_table                                          │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │ id (PK)     │ title  │ isDone │ categoryId (FK) │  │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │ 1           │ "学习" │ 0      │ 1               │  │ │
│  │ 2           │ "工作" │ 1      │ 2               │  │ │
│  │ 3           │ "运动" │ 0      │ null            │  │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  category_table                                      │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │ id (PK) │ name     │ color        │ createdAt       │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │ 1       │ "个人"   │ "#FF6200EE"  │ 1704715200      │ │
│  │ 2       │ "工作"   │ "#FFFF6B6B"  │ 1704715200      │ │
│  │ 3       │ "运动"   │ "#FF51CF66"  │ 1704715200      │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

## 2. 数据流向图

```
┌─────────────────────────────────────────────────────────────┐
│                    User Interaction                          │
│  (点击、输入、滑动等用户操作)                              │
└────────────────────┬────────────────────────────────────────┘
                     │
         ┌───────────▼──────────────┐
         │   MainActivity Events    │
         │  onClick, onQueryText等  │
         └───────────┬──────────────┘
                     │
         ┌───────────▼─────────────────────────────┐
         │  ViewModel Method Call                  │
         │  addTodo(), toggleTodo()等              │
         └───────────┬─────────────────────────────┘
                     │
         ┌───────────▼──────────────┐
         │  Input Validation        │
         │  非空、长度检查          │
         └───────────┬──────────────┘
                     │
         ┌───────────▼──────────────┐
         │  DAO Operation           │
         │  insert/update/delete    │
         └───────────┬──────────────┘
                     │
         ┌───────────▼──────────────┐
         │  Room Database           │
         │  Execute SQL             │
         └───────────┬──────────────┘
                     │
         ┌───────────▼──────────────────────────────┐
         │  Flow Emit Data Change                   │
         │  getAllTodos() 推送新数据                │
         └───────────┬──────────────────────────────┘
                     │
         ┌───────────▼──────────────────────────────┐
         │  StateFlow Update                        │
         │  _todos.value = newData                  │
         └───────────┬──────────────────────────────┘
                     │
         ┌───────────▼──────────────────────────────┐
         │  Filter (Category + Search)              │
         │  filterTodos(todos)                      │
         └───────────┬──────────────────────────────┘
                     │
         ┌───────────▼──────────────────────────────┐
         │  MainActivity Collect                    │
         │  lifecycleScope.launch {                 │
         │    todos.collect { todos -> ... }        │
         └───────────┬──────────────────────────────┘
                     │
         ┌───────────▼──────────────────────────────┐
         │  Adapter.submitList()                    │
         │  提交新列表给适配器                      │
         └───────────┬──────────────────────────────┘
                     │
         ┌───────────▼──────────────────────────────┐
         │  DiffUtil Calculation                    │
         │  计算旧列表→新列表的差异                 │
         │  (仅计算变化项)                          │
         └───────────┬──────────────────────────────┘
                     │
         ┌───────────▼──────────────────────────────┐
         │  RecyclerView Update                     │
         │  调用 notifyItemChanged/Inserted/Removed│
         │  (仅更新变化的项)                        │
         └───────────┬──────────────────────────────┘
                     │
         ┌───────────▼──────────────────────────────┐
         │  ViewHolder onBindViewHolder             │
         │  更新视图显示                            │
         └───────────┬──────────────────────────────┘
                     │
         ┌───────────▼──────────────────────────────┐
         │  Screen Render                           │
         │  (用户看到更新后的界面)                  │
         └──────────────────────────────────────────┘
```

## 3. 分层职责划分

### 3.1 表现层 (Presentation)
**文件**: MainActivity.kt, TodoAdapter.kt, CategoryAdapter.kt

**职责**:
- ✅ 显示 UI
- ✅ 处理用户交互
- ✅ 观察 ViewModel 数据变化
- ✅ 显示错误/加载状态

**不能做**:
- ❌ 直接操作数据库
- ❌ 执行复杂业务逻辑
- ❌ 做数据验证

### 3.2 业务逻辑层 (ViewModel)
**文件**: TodoViewModel.kt, TodoViewModelFactory.kt

**职责**:
- ✅ 实现业务逻辑
- ✅ 数据验证
- ✅ 错误处理
- ✅ 数据聚合/转换
- ✅ 管理 UI 状态

**不能做**:
- ❌ 持有 Activity/Fragment 引用
- ❌ 直接操作 UI
- ❌ 长期存储数据

### 3.3 数据访问层 (DAO)
**文件**: TodoDao.kt, CategoryDao.kt

**职责**:
- ✅ 定义数据库操作接口
- ✅ 使用 Room 注解定义 SQL 查询
- ✅ 返回 suspend 函数或 Flow

**不能做**:
- ❌ 实现复杂业务逻辑
- ❌ 错误处理（由 ViewModel 处理）

### 3.4 存储层 (Database)
**文件**: TodoDatabase.kt

**职责**:
- ✅ Room 数据库配置
- ✅ 数据库迁移
- ✅ 提供 DAO 实例

**不能做**:
- ❌ 业务逻辑
- ❌ 复杂查询转换

## 4. 组件交互图

```
┌──────────────────┐
│   MainActivity   │
└────────┬─────────┘
         │ onCreate()
         │ 1. 初始化 ViewModel
         │ 2. 初始化 Adapters
         │ 3. 观察 StateFlow
         │
         ├──────────────────────────────────────────────┐
         │                                              │
         ▼                                              ▼
┌──────────────────────┐                    ┌──────────────────────┐
│   TodoViewModel      │                    │  TodoDatabase        │
│                      │                    │                      │
│  init {              │──── getDatabase ──▶│  - todoDao()         │
│    loadTodos()       │                    │  - categoryDao()     │
│    loadCategories()  │                    │                      │
│  }                   │                    │  - Migration 1→2     │
│                      │                    │  - SQLite Engine     │
└──────────┬───────────┘                    └──────────────────────┘
           │
           │ 1. todoDao.getAllTodos()
           │ 2. categoryDao.getAllCategories()
           │
           ├──────────────────────────────────────┐
           │                                      │
           ▼                                      ▼
    ┌─────────────┐                       ┌─────────────────┐
    │  TodoDao    │                       │  CategoryDao    │
    │             │                       │                 │
    │ getAllTodos │                       │ getAllCategories│
    │ getTodosBy  │                       │ getCategoryById │
    │ Category    │                       │ insert()        │
    │ searchTodos │                       │ update()        │
    │ insert()    │                       │ delete()        │
    │ update()    │                       └─────────────────┘
    │ delete()    │
    └─────────────┘
           │
           │ Flow<List<TodoEntity>>
           │ Flow<List<CategoryEntity>>
           │
           ▼
    ┌─────────────────┐
    │  Room Database  │
    │  (SQLite)       │
    │                 │
    │ todo_table      │
    │ category_table  │
    └─────────────────┘
           │
           │ SQL: SELECT * FROM todo_table ORDER BY createdAt DESC
           │
           ▼
    ┌─────────────────────────────────┐
    │  Flow 推送数据到 ViewModel       │
    │  collectLatest { todos ->        │
    │    _todos.value = filterTodos()  │
    │  }                               │
    └────────────┬────────────────────┘
                 │
                 │ StateFlow 更新
                 │
                 ▼
    ┌──────────────────────┐
    │   MainActivity       │
    │   .collect {         │
    │     adapter.         │
    │       submitList()   │
    │   }                  │
    └──────────┬───────────┘
               │
               │ ListAdapter.submitList()
               │
               ▼
    ┌──────────────────────┐
    │   DiffUtil           │
    │                      │
    │  计算差异:           │
    │  - areItemsTheSame   │
    │  - areContentsTheSame│
    └──────────┬───────────┘
               │
               │ DiffResult
               │
               ▼
    ┌──────────────────────┐
    │  RecyclerView        │
    │                      │
    │  notifyItem*()       │
    │  - ItemChanged       │
    │  - ItemInserted      │
    │  - ItemRemoved       │
    └──────────┬───────────┘
               │
               ▼
    ┌──────────────────────┐
    │  onBindViewHolder()  │
    │                      │
    │  更新 UI 元素        │
    └──────────┬───────────┘
               │
               ▼
    ┌──────────────────────┐
    │  Screen Render       │
    │  (用户看到)          │
    └──────────────────────┘
```

## 5. 搜索和筛选流程

```
SearchView 输入 + 分类点击
    │
    ├─────────────────┬──────────────────┐
    │                 │                  │
    ▼                 ▼                  ▼
setSearchQuery()  setSelectedCategory() (other)
    │                 │
    └────────┬────────┘
             │
    ┌────────▼──────────────────┐
    │  ViewModel 状态更新        │
    │  - _searchQuery            │
    │  - _selectedCategoryId     │
    └────────┬──────────────────┘
             │
    ┌────────▼──────────────────┐
    │  触发 loadTodos()          │
    │  重新加载数据库数据        │
    └────────┬──────────────────┘
             │
    ┌────────▼──────────────────┐
    │  filterTodos(todos)        │
    │                            │
    │  1. if (categoryId != null)│
    │     todos = todos.filter { │
    │       categoryId == selected│
    │     }                       │
    │                            │
    │  2. if (query.isNotEmpty) │
    │     todos = todos.filter { │
    │       title.contains(query)│
    │     }                       │
    │                            │
    │  return filtered todos     │
    └────────┬──────────────────┘
             │
    ┌────────▼──────────────────┐
    │  _todos StateFlow 推送     │
    │  筛选后的结果              │
    └────────┬──────────────────┘
             │
    ┌────────▼──────────────────┐
    │  MainActivity.collect      │
    │  adapter.submitList()      │
    │  RecyclerView 更新         │
    └────────┬──────────────────┘
             │
             ▼
        用户看到筛选结果
```

## 6. 状态管理

```
MutableStateFlow 的生命周期
    │
    ├─ 创建: ViewModel.init() 时初始化
    │   _todos = MutableStateFlow(emptyList())
    │
    ├─ 订阅: MainActivity 中
    │   lifecycleScope.launch {
    │     todos.collect { ... }
    │   }
    │
    ├─ 更新: 任何方法调用时
    │   _todos.value = newData
    │   → 立即通知所有订阅者
    │
    └─ 销毁: MainActivity 销毁时
        lifecycleScope 自动取消订阅
        防止内存泄露
```

## 7. 依赖注入

```
依赖链条:
    │
    MainActivity
        │
        └── ViewModelProvider
            │
            └── TodoViewModelFactory
                │
                ├── TodoViewModel
                │   │
                │   ├── TodoDao
                │   │
                │   └── CategoryDao
                │
                └── TodoDatabase
                    │
                    ├── Room.databaseBuilder()
                    │
                    └── Migration 1→2
```

---

**版本**: 1.0  
**更新时间**: 2026年1月9日
