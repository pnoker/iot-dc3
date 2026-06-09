# 故障排查

本页汇总 IoT DC3 本地开发和运行时的常见问题。除非特别说明，命令都在仓库根目录执行。

## Maven 构建很慢

**原因**：并行度未生效或 JVM 堆内存不足。

**处理方式**：仓库已配置默认 Maven 参数：

- `.mvn/maven.config` 包含 `-T 1C`
- `.mvn/jvm.config` 包含 `-Xms512m -Xmx1024m`

如果仍然较慢，可以适当增加 JVM 堆内存，或减少本机后台 CPU 占用。

## Java 版本错误

**现象**：出现 unsupported class file major version 或 Maven Enforcer 报错。

**原因**：项目要求 JDK 21。

**处理方式**：

```bash
java -version
mvn -version
```

确认 Maven 使用的 Java 版本也是 21。

## 端口被占用

**现象**：应用启动失败，提示 `8000`、`8300`、`8400`、`8500`、`8600`、`9300`、`9400`、`9500` 等端口已占用。

**处理方式**：通过环境变量或根目录 `.env` 覆盖端口。

常见变量：

- `SERVER_PORT`
- `GRPC_SERVER_PORT`
- `DC3_GATEWAY_PORT`
- `DC3_AUTH_PORT`
- `DC3_MANAGER_PORT`
- `DC3_DATA_PORT`
- `DC3_AGENTIC_PORT`

## 数据库连接失败

**原因**：PostgreSQL 容器未启动、健康检查未通过，或 `.env` 中发布端口被改过。

**处理方式**：

```bash
make compose-ps STACK=db
make compose-file STACK=db
```

确认容器状态和发布端口与应用环境变量一致。源码运行时应先执行：

```bash
source dc3/env/dev.env.sh
```

## RabbitMQ 连接失败

**原因**：RabbitMQ 未就绪、虚拟主机或账号密码不一致。

**处理方式**：等待健康检查通过后重启依赖服务，并查看日志：

```bash
make compose-logs STACK=db
```

源码运行时检查 `RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD` 和 `RABBITMQ_VIRTUAL_HOST`。

## pre/pro profile 出现 Nacos 错误

**原因**：`pre` / `pro` profile 面向注册中心部署，通常期望 Nacos 可用。

**处理方式**：本地源码调试优先使用 `dev` profile。只有在验证注册中心部署时才使用 `pre` / `pro`。

## Gateway 返回 401 或 403

**原因**：请求访问了需要认证的接口，但没有携带有效 token。

**处理方式**：先调用 `/api/v3/auth/token/...` 获取 token，再在后续请求中携带租户、登录名和 token 相关请求头。Swagger UI 的认证方式见 [API 文档](../development/api-documentation.md)。

## 驱动无法注册

**原因**：Manager Center 未运行、gRPC 目标地址错误、驱动编码重复或 RabbitMQ 未就绪。

**处理方式**：

1. 按 Gateway -> Auth -> Manager -> Data -> Agentic -> Driver 顺序启动。
2. 确认本地源码运行已加载 `dc3/env/dev.env.sh`。
3. 查看 Manager Center 和驱动日志。
4. 确认 `dc3.driver.code` 唯一且稳定。

## Docker 镜像构建失败

**原因**：镜像构建过程中 Maven 打包失败，或依赖没有提前构建成功。

**处理方式**：

```bash
make package
make build
```

先在宿主机确认 Maven 构建通过，再构建镜像。

## 镜像源选择不符合预期

**原因**：`REGISTRY` 被设置为 `cn` 或 `global`。

**处理方式**：

```bash
make dev-db REGISTRY=global
make dev-db REGISTRY=cn
```

`global` 使用默认镜像仓库，`cn` 使用中国大陆镜像仓库。

## 希望更快调试

可以使用 `dc3-center-single` 在单 JVM 中运行 Auth、Manager 和 Data 相关能力：

```bash
source dc3/env/dev.env.sh
java -jar dc3-center/dc3-center-single/target/dc3-center-single.jar
```

单进程模式适合本地调试，不代表生产部署形态。
