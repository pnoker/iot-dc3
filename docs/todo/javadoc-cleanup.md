---
title: 类注释统一完善
---

# 类注释统一完善

## 背景

项目中大量类存在 Javadoc 问题：空模板 `<p></p>`、占位符（`-`）、笼统描述（只写 `Mapper`、`Status`
）、描述与实际职责不符、完全缺失。这次任务逐模块扫描并修正所有 `public` 类/接口/枚举的类级 Javadoc，使其准确、一致、有意义。

## 注释规范

每条类级 Javadoc 应遵循以下格式：

```java
/**
 * 一句话描述类的职责。说明这个类做什么，而不是它怎么实现。
 *
 * <p>可选：补充说明，比如关键设计决策、线程安全、数据源等上下文。
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
```

规则：

1. 第一句话必须完整描述类的职责，不能只写 `Mapper`、`Service` 等类型词。
2. 描述使用英文，与现有代码风格一致。
3. 不暴露实现细节（如具体 SQL、MyBatis-Plus 继承关系），聚焦于这个类在系统中承担的角色。
4. 各层的描述模板参考：

| 层                           | 描述模板                                                             |
|-----------------------------|------------------------------------------------------------------|
| `*DO`                       | Persistence object for the `{table_name}` table.                 |
| `*BO`                       | Business object for `{entity}` operations.                       |
| `*VO`                       | View object returned by the `{entity}` API endpoints.            |
| `*DTO`                      | Data transfer object for `{purpose}`.                            |
| `*Query`                    | Query parameters for `{entity}` listing/filtering.               |
| `*Mapper` (XML-backed)      | Aggregate / custom SQL queries for `{purpose}`.                  |
| `*Mapper` (BaseMapper-only) | MyBatis-Plus mapper for the dc3_{table} table.                   |
| `*Manager`                  | Persistence manager (DAL) for `{entity}` CRUD.                   |
| `*ManagerImpl`              | MyBatis-Plus implementation of `{entity}` persistence.           |
| `*Service`                  | Business service interface for `{entity}` operations.            |
| `*ServiceImpl`              | Business service implementation for `{entity}`.                  |
| `*Controller`               | REST controller exposing `{entity}` CRUD endpoints.              |
| `*Enum`                     | Enumeration of `{purpose}` values.                               |
| `*Config`                   | Spring configuration for `{feature}`.                            |
| `*Constant`                 | Constant definitions for `{scope}`.                              |
| `*Builder`                  | MapStruct builder converting between `{from}` and `{to}`.        |
| `*Receiver`                 | RabbitMQ message receiver for `{event_type}` events.             |
| `*Ext`                      | JSON extension object for `{entity}` metadata and configuration. |

5. 纯 `BaseMapper` 继承且无自定义方法的 Mapper 接口也应有简短描述。
6. 只修改类级 Javadoc，不改方法级注释，不改代码逻辑。

## 执行进度

### Phase 1: dc3-common-auth — DONE

- [x] 修正 29 个空模板 Javadoc（9 DO、10 Manager、10 ManagerImpl）
- [x] 修正 14 个笼统描述（10 Mapper、4 Controller）
- [x] 编译验证通过
- [x] 提交 `98491f5a6`

### Phase 2: dc3-common-manager — DONE

- [x] 修正 19 个空模板 Javadoc（8 DO、8 Manager、3 ManagerImpl）
- [x] 修正 25 个笼统描述（8 Mapper、11 Controller、3 VO、3 BO）
- [x] 编译验证通过
- [x] 提交 `dcfe7befa`

### Phase 3: dc3-common-data — DONE

- [x] 修正 3 个空模板 Javadoc（MessageDO、RuleDO、NotifyDO）
- [x] 修正 3 个笼统描述（MessageMapper、NotifyMapper、RuleMapper）
- [x] 编译验证通过
- [x] 提交 `754087a3f`

### Phase 4: dc3-common-agentic — DONE

- [x] 补充 22 个缺失的类级 Javadoc
- [x] 修正 17 个空模板 Javadoc
- [x] 编译验证通过
- [x] 提交（39 files changed, 213 insertions）

### Phase 5: dc3-common-model + dc3-common-constant — DONE

- [x] 修正 3 个空模板 Javadoc（DriverExt、DeviceExt、DriverAttributeExt）
- [x] 修正 1 个笼统描述（MetadataEventDTO）
- [x] 补充 3 个缺失 Javadoc（CmdParameterDTO、DeviceCommandDTO、DriverCommandDTO）
- [x] 编译验证通过
- [x] 提交 `495c8d7eb`（model Ext）+ 前序提交（model DTO + constant）

### Phase 6: dc3-common-driver + dc3-common-web + dc3-common-rabbitmq + dc3-common-repository + dc3-common-dal — DONE

- [x] 修正 2 个空模板 Javadoc（DriverStatusScheduleJob、DriverSenderServiceImpl）
- [x] 其余模块已有规范注释，无需修改
- [x] 编译验证通过
- [x] 提交

## 验收结果

- [x] 项目中无空 `<p></p>` 模板 Javadoc
- [x] 无 `-`、`Status`（非 Status 类）等占位符描述
- [x] 无笼统的 `Mapper`、`Manager`、`DO` 等单字描述
- [x] 无缺失类级 Javadoc 的 public 类
- [x] `mvn -s .mvn/settings.xml -q -DskipTests compile` 通过
- [x] 每个模块独立提交，commit message 遵循项目 Conventional Commit 规范

## 统计

共修改约 130+ 个文件，分布在 6 个 Phase，7 次提交。
