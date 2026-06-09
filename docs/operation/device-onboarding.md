# 设备接入流程

设备接入的目标是让某个驱动能够识别设备配置、采集点位值，并把数据写入 Data Center。

## 前置条件

- 已启动 PostgreSQL、RabbitMQ 和核心中心服务。
- 已加载本地源码运行环境变量：`source dc3/env/dev.env.sh`。
- 至少启动一个驱动，例如 `dc3-driver-virtual`。
- 能访问 Gateway API 或 Web UI。

## 1. 启动驱动

虚拟驱动适合验证完整链路：

```bash
java -jar dc3-driver/dc3-driver-virtual/target/dc3-driver-virtual.jar
```

驱动启动后会向 Manager Center 注册自己的驱动编码、设备属性和点位属性。真实协议驱动的配置项由各驱动的 `application.yml` 定义。

## 2. 确认驱动注册

在管理侧查看驱动列表，确认目标驱动处于可用状态。调试时也可以查看 Manager Center 和驱动日志，确认注册请求没有因 gRPC 地址、租户或驱动编码冲突失败。

常见注册失败原因：

| 问题 | 处理方式 |
|------|----------|
| Manager Center 未启动 | 先启动 Manager，再重启驱动 |
| `CENTER_MANAGER_HOST` 不正确 | 检查 `dc3/env/dev.env.sh` 或 IDE 环境变量 |
| 驱动编码重复 | 保持 `dc3.driver.code` 唯一且稳定 |
| RabbitMQ 未就绪 | 等待健康检查通过后重启驱动 |

## 3. 创建模板

为同类设备创建 Profile，并在模板中定义点位。点位应使用稳定、可读的名称，并把协议细节放在点位属性中。

示例：

| 字段 | 示例 |
|------|------|
| Profile | `virtual-motor` |
| Point | `temperature` |
| Data type | `DOUBLE` |
| Unit | `celsius` |
| Point Attribute | 由驱动定义，例如地址、随机值范围或对象标识 |

## 4. 创建设备

创建设备时绑定目标驱动和模板，并填写驱动属性。真实设备通常需要 host、port、站号、认证信息或 topic 等配置。

设备创建后，驱动会通过元数据事件感知变更。多数情况下不需要重启驱动；如果驱动实现仍依赖启动时加载缓存，可按驱动说明重启。

## 5. 绑定或校准点位

检查设备下的点位配置是否完整。对于工业协议，重点确认：

- 寄存器、地址、对象 ID 或 topic 是否正确。
- 数据类型、字节序、倍率和单位是否符合现场设备。
- 读写方向是否与设备能力一致。
- 采集周期是否和设备性能匹配。

## 6. 验证状态和数据

等待一个采集周期后，在数据页面或 Data Center API 中查看点位值。如果没有数据，按以下顺序排查：

1. 驱动日志是否有协议连接错误。
2. RabbitMQ 是否有积压或队列绑定异常。
3. Data Center 是否收到点位值消息。
4. 设备、模板、点位的租户是否一致。
5. 点位属性是否缺失或格式不符合驱动期望。

## 下一步

- 查看 [数据与命令](data-commands.md) 验证采集和写入。
- 开发新协议驱动时阅读 [驱动开发](../development/driver-authoring.md)。
- 需要完整模块关系时阅读 [模块与依赖](../architecture/modules.md)。
