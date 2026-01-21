# MiniTodoApp

一款简洁高效的Android待办事项管理应用。

## 📱 功能特性

### ✨ 核心功能

- **待办事项管理**
  - 快速添加待办项
  - 标记完成/未完成状态
  - 删除待办项
  - 列表展示和滚动体验

- **提醒功能**
  - 为待办项设置具体的提醒时间（精确到分钟）
  - 使用系统AlarmManager实现精确闹钟
  - 提醒时弹出系统通知
  - 点击通知快速返回应用

- **智能排序**
  - 优先显示未完成的待办项
  - 同状态下按提醒时间升序排列
  - 无提醒时间的按创建时间降序排列

- **数据统计**
  - 实时显示已完成/未完成待办项数量
  - 显示待办项总数

## 技术栈

### 开发语言
- **Kotlin** - 主要开发语言

### 架构与设计
- **MVVM** - Model-View-ViewModel架构
- **Room** - 本地数据库

### UI与列表
- **RecyclerView** - 列表展示
- **ListAdapter** - 列表适配器
- **DiffUtil** - 计算列表差异更新
- **ViewBinding** - 视图绑定
- **Material Design** - Material Design组件库

### 系统功能
- **AlarmManager** - 系统级闹钟管理
- **BroadcastReceiver** - 系统广播接收
- **NotificationManager** - 系统通知管理

## 📝 使用说明

### 添加待办项
1. 在输入框中输入待办项内容
2. 点击"添加"按钮
3. 新建的待办项将出现在列表顶部

### 标记完成
- 点击待办项左侧的复选框标记为已完成
- 已完成的项目会显示删除线样式并排在列表下方

### 设置提醒
1. 点击待办项的"提醒"按钮
2. 在弹出的对话框中选择提醒日期和时间
3. 确认后系统将在指定时间前10分钟触发闹钟

### 删除待办项
- 点击待办项右侧的"删除"按钮
- 待办项将被永久删除

### 查看统计
- 在应用顶部可以看到实时的统计数据：
  - 已完成数量
  - 未完成数量
  - 待办项总数

## 🔧 开发说明

### 核心类说明

#### UI层
- **MainActivity** - 应用主界面
- **TodoFragment** - 待办列表展示Fragment
- **ReminderDialogFragment** - 提醒时间选择对话框

#### ViewModel层
- **TodoViewModel** - 待办项业务逻辑处理
  - 管理待办项列表
  - 处理增删改查操作
  - 自动排序和统计

#### 数据层
- **TodoEntity** - 待办项数据模型
- **TodoDao** - 数据库访问对象
- **TodoRepository** - 数据访问抽象层
- **TodoDatabase** - Room数据库

#### 工具类
- **AlarmUtils** - 闹钟管理工具
- **NotificationUtils** - 通知管理工具
- **AlarmReceiver** - 闹钟广播接收器

### 关键功能实现流程

#### 提醒流程
```
用户设置提醒时间
    ↓
ReminderDialogFragment显示日期/时间选择
    ↓
TodoViewModel.updateTodoReminder()更新数据库
    ↓
AlarmUtils.setAlarm()注册系统闹钟(提前10分钟)
    ↓
[到达闹钟时间]
    ↓
AlarmReceiver接收系统广播
    ↓
NotificationUtils.showReminderNotification()显示通知
    ↓
用户点击通知返回应用
```

#### 排序规则
```
第一优先级: isDone (false > true)
    └─ 未完成优先于已完成

第二优先级: remindTime (时间升序)
    └─ 有提醒时间的按时间升序

第三优先级: createdAt (时间降序)
    └─ 无提醒时间的按创建时间降序
```
