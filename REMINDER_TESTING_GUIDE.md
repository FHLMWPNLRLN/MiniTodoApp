# 提醒功能测试指南

## 概述
本文档提供了测试提醒功能的完整步骤和故障排查指南。

## 快速测试（5分钟）

### 步骤 1: 启用通知权限
在模拟器/设备上：
1. 打开"设置" → "应用和通知" → "应用权限" → "通知"
2. 找到"MiniTodoApp"并启用通知权限
3. 返回应用

### 步骤 2: 创建带有提醒的待办事项
1. 打开应用
2. 添加新待办事项（例如："测试提醒"）
3. 按下🕐 提醒按钮
4. 选择日期和时间（选择当前时间后的一分钟）
5. 点击"确定"

### 步骤 3: 验证提醒
**预期行为：**
- 应该看到toast消息："提醒设置成功"
- 待办事项显示蓝色提醒时间（表示未来的提醒）
- 当到达提醒时间时，应收到通知

**实际行为：**
1. 打开开发者选项（设置 → 关于手机 → 点击"版本号"7次）
2. 启用"显示日志"（如果可用）
3. 打开Android Studio → Logcat
4. 过滤日志：搜索 "AlarmReceiver" 和 "NotificationUtils"
5. 等待提醒时间到达
6. 查看日志输出：
   ```
   AlarmReceiver: Creating notification for todo: id=X, title=测试提醒, time=...
   NotificationUtils: showReminderNotification called: id=X, title=测试提醒
   AlarmReceiver: Notification displayed successfully
   ```

## 故障排查

### 问题 1: 通知权限被拒绝
**症状：**
- Logcat中出现: `Notifications enabled: false`
- 没有看到通知

**解决方案：**
1. 进入设置 → 应用和通知 → 高级选项 → 权限管理
2. 找到MiniTodoApp
3. 启用"发送通知"权限

### 问题 2: AlarmReceiver 没有触发
**症状：**
- Logcat中看不到 "Creating notification for todo" 消息
- 即使到达提醒时间也没有日志

**解决方案：**
1. 检查AlarmReceiver是否在AndroidManifest.xml中注册（应该有）
2. 验证意图过滤器: `android:name="com.example.minitodo.ALARM_REMINDER"`
3. 尝试增加提醒时间（确保不是过去的时间）
4. 重启应用

### 问题 3: 通知显示但无声音/振动
**症状：**
- 收到通知但没有声音或振动

**解决方案：**
1. 检查设备音量设置
2. 检查通知渠道设置：
   ```
   设置 → 应用和通知 → MiniTodoApp → 通知 → 待办提醒
   ```
3. 启用"声音"和"振动"选项

### 问题 4: 提醒设置不被保存
**症状：**
- 设置提醒时看到成功消息，但关闭应用后丢失

**解决方案：**
1. 检查数据库版本: 应该是 3（支持remindTime字段）
2. 清除应用数据并重新创建待办事项：
   ```
   设置 → 应用和通知 → MiniTodoApp → 存储 → 清除缓存/清除数据
   ```

## 日志参考

### 成功的提醒设置日志
```
D/AlarmUtils: 设置闹钟，待办事项 ID: 1
D/AlarmUtils: 当前时间: 2024-01-20 10:00:00
D/AlarmUtils: 提醒时间: 2024-01-20 10:05:00
D/AlarmUtils: 为待办事项1设置闹钟，时间：Sat Jan 20 10:05:00 GMT 2024
```

### 成功的提醒触发日志
```
D/AlarmReceiver: Alarm received: com.example.minitodo.ALARM_REMINDER
D/AlarmReceiver: Creating notification for todo: id=1, title=测试提醒, time=2024-01-20 10:05:00
D/AlarmReceiver: Notifications enabled: true
D/NotificationUtils: showReminderNotification called: id=1, title=测试提醒
D/AlarmReceiver: Notification displayed successfully
```

## 完整的端到端流程

1. **设置提醒**
   - 创建待办事项
   - 点击🕐按钮
   - 选择未来的日期和时间
   - 看到"提醒设置成功"toast

2. **待办显示更新**
   - 待办事项显示蓝色提醒时间（表示未来）

3. **系统闹钟触发**
   - AlarmManager在指定时间触发
   - Android系统调用AlarmReceiver.onReceive()

4. **通知显示**
   - AlarmReceiver创建通知intent
   - NotificationUtils显示通知
   - 用户看到系统通知

5. **用户交互**
   - 用户点击通知
   - 打开MainActivity
   - 待办事项保持标记（取决于UI设计）

## 性能和电池考虑

- **AlarmManager**: 使用`setAndAllowWhileIdle()`在低功耗模式下工作
- **Notification Channel**: 使用IMPORTANCE_HIGH以确保可见性
- **PendingIntent**: 使用FLAG_IMMUTABLE以符合Android 12+要求

## 已知限制

1. **系统休眠**: 如果设备进入深度睡眠，某些闹钟可能延迟
2. **电池优化**: 某些设备制造商可能限制后台唤醒
3. **应用卸载**: 闹钟会在应用卸载后自动清除

## 调试工具

### 使用adb命令
```bash
# 查看所有已设置的闹钟
adb shell dumpsys alarm

# 查看特定应用的权限
adb shell dumpsys package com.example.minitodo | grep -A 10 "Permissions:"

# 清除应用数据
adb shell pm clear com.example.minitodo
```

### 使用Android Studio Logcat
1. 过滤 `AlarmReceiver|NotificationUtils|AlarmUtils`
2. 设置日志级别为 "Verbose"
3. 搜索特定的待办事项ID

## 反馈和报告问题

如果提醒功能仍不工作，请提供以下信息：
1. Android版本号
2. 完整的Logcat输出（包含AlarmReceiver和NotificationUtils日志）
3. 待办事项详情（标题、提醒时间）
4. 通知权限状态

---

**最后更新**: 2024年1月
**版本**: 1.0
