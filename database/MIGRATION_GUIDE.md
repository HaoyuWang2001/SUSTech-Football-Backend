# 数据库迁移脚本执行指南

本文档说明了为实现"赛事球队大名单"功能所需执行的所有数据库迁移脚本及其执行顺序。

## 功能需求

1. 赛事创建时需要指定比赛人数（5人制/7人制/8人制/11人制）
2. 赛事创建时需要指定参赛球队的大名单人数（如25人）
3. 球队接受赛事邀请后，需要从球队成员中选择大名单球员

## 迁移脚本执行顺序

**⚠️ 重要提示：必须按照以下顺序依次执行，否则可能导致数据库错误！**

### 1️⃣ migration_add_match_player_count_and_roster_size.sql

**执行命令：**
```powershell
psql -U your_username -d your_database -f "d:\26Spring\SustechFootball\SUSTech-Football-Backend\database\migration_add_match_player_count_and_roster_size.sql"
```

**作用：**
- 给 `event` 表添加 `match_player_count` 字段（比赛人数：5/7/8/11）
- 给 `event` 表添加 `roster_size` 字段（大名单人数）

**原因：**
- 这是基础字段，必须先添加到主表

---

### 2️⃣ migration_update_zbak_event_structure.sql

**执行命令：**
```powershell
psql -U your_username -d your_database -f "d:\26Spring\SustechFootball\SUSTech-Football-Backend\database\migration_update_zbak_event_structure.sql"
```

**作用：**
- 给备份表 `zbak_event` 添加 `match_player_count` 字段
- 给备份表 `zbak_event` 添加 `roster_size` 字段

**原因：**
- 备份表结构必须与主表一致，否则删除赛事时会报错："INSERT 的表达式多于指定的字段数"
- 必须在步骤1之后立即执行，确保主表和备份表结构同步

---

### 3️⃣ migration_add_event_team_roster.sql

**执行命令：**
```powershell
psql -U your_username -d your_database -f "d:\26Spring\SustechFootball\SUSTech-Football-Backend\database\migration_add_event_team_roster.sql"
```

**作用：**
- 创建 `event_team_roster` 表（存储每个球队在赛事中的大名单）
- 表结构：`event_id`, `team_id`, `player_id`, `number`（球衣号码）
- 添加外键约束，确保数据完整性
- 创建索引优化查询性能

**原因：**
- 这是核心业务表，用于存储球队选择的大名单球员
- 依赖于 `event` 和 `event_team` 表，所以必须在它们之后创建

---

### 4️⃣ fix_backup_function_error.sql

**执行命令：**
```powershell
psql -U your_username -d your_database -f "d:\26Spring\SustechFootball\SUSTech-Football-Backend\database\fix_backup_function_error.sql"
```

**作用：**
- 创建 `zbak_event_team_roster` 备份表
- 更新 `backup_and_delete_event()` 触发器函数，添加对 `event_team_roster` 表的备份和删除逻辑

**原因：**
- 删除赛事时需要先备份大名单数据，然后再删除
- 必须在创建 `event_team_roster` 表之后执行，否则触发器会引用不存在的表

---

### 5️⃣ fix_all_event_cascade_constraints.sql（推荐执行）

**执行命令：**
```powershell
psql -U your_username -d your_database -f "d:\26Spring\SustechFootball\SUSTech-Football-Backend\database\fix_all_event_cascade_constraints.sql"
```

**作用：**
- 给所有与 `event` 表相关的外键添加 `ON DELETE CASCADE` 约束
- 涉及的表：
  - `event_manager`
  - `event_group`
  - `event_referee`
  - `event_referee_request`
  - `event_stage`
  - `event_stage_tag`
  - `event_match`
  - `favorite_event`

**原因：**
- 确保删除赛事时，所有相关数据自动级联删除
- 避免外键约束冲突
- **可选但强烈推荐**，可以简化删除逻辑并提高数据一致性

---

## 快速执行（一次性执行所有脚本）

如果你确定以上脚本都未执行过，可以使用以下命令一次性执行：

```powershell
cd "d:\26Spring\SustechFootball\SUSTech-Football-Backend\database"

# 请替换 your_username 和 your_database 为实际的数据库用户名和数据库名

# 1. 添加赛事表字段
psql -U your_username -d your_database -f migration_add_match_player_count_and_roster_size.sql

# 2. 更新备份表结构
psql -U your_username -d your_database -f migration_update_zbak_event_structure.sql

# 3. 创建大名单表
psql -U your_username -d your_database -f migration_add_event_team_roster.sql

# 4. 修复备份函数
psql -U your_username -d your_database -f fix_backup_function_error.sql

# 5. 修复级联删除约束（推荐）
psql -U your_username -d your_database -f fix_all_event_cascade_constraints.sql

echo "所有迁移脚本执行完成！"
```

---

## 执行后验证

执行完所有脚本后，可以运行以下SQL验证：

```sql
-- 1. 验证 event 表的新字段
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'event' 
  AND column_name IN ('match_player_count', 'roster_size');

-- 2. 验证 zbak_event 表的新字段
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'zbak_event' 
  AND column_name IN ('match_player_count', 'roster_size');

-- 3. 验证 event_team_roster 表是否存在
SELECT table_name 
FROM information_schema.tables 
WHERE table_name = 'event_team_roster';

-- 4. 验证 zbak_event_team_roster 表是否存在
SELECT table_name 
FROM information_schema.tables 
WHERE table_name = 'zbak_event_team_roster';

-- 5. 验证外键级联删除约束
SELECT 
    tc.table_name, 
    kcu.column_name,
    tc.constraint_name,
    rc.delete_rule
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu 
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.referential_constraints rc 
    ON tc.constraint_name = rc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY' 
    AND kcu.column_name = 'event_id'
    AND rc.delete_rule = 'CASCADE'
ORDER BY tc.table_name;
```

---

## 常见问题

### Q1: 如果执行过程中报错怎么办？

**A:** 检查错误信息：
- 如果提示"字段已存在"，说明该脚本已经执行过，可以跳过
- 如果提示"表不存在"，说明执行顺序错误，需要按照上述顺序重新执行

### Q2: 可以重复执行这些脚本吗？

**A:** 大部分脚本使用了 `IF NOT EXISTS` 或 `IF EXISTS` 子句，可以安全地重复执行。但为了避免不必要的问题，建议：
1. 执行前先备份数据库
2. 只执行未执行过的脚本

### Q3: 执行完后需要重启后端吗？

**A:** 是的！执行完所有数据库迁移后，需要：
1. 重启 Spring Boot 后端应用
2. 后端会自动加载新的表结构和实体类映射
3. 测试赛事创建、大名单选择、赛事删除等功能

---

## 其他不需要执行的脚本

以下脚本**不需要**执行，它们是早期的版本或已被更好的方案替代：

- ❌ `fix_cascade_delete_constraints.sql` - 已被 `fix_all_event_cascade_constraints.sql` 替代

---

## 总结

按照以上顺序执行完5个迁移脚本后：

✅ 赛事表支持比赛人数和大名单人数字段  
✅ 备份表结构与主表同步，删除功能正常  
✅ 大名单表创建完成，支持球队选择参赛球员  
✅ 备份函数包含大名单数据的备份逻辑  
✅ 所有外键约束支持级联删除，数据一致性有保障  

现在可以重启后端并进行完整的功能测试！
