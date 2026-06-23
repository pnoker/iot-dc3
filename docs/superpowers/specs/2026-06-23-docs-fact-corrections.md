# IoT DC3 Docs — 权威事实更正（2026-06-23）

> 经对抗式审查 + 直接读源码核实。**凡与 dossier / factspack / 早前页面冲突，一律以本文件为准。** 修订任何页面时按此更正，并复核同类表述。

## 1. 网关公开路径前缀（最关键，影响所有 curl 示例）

所有对外 HTTP API 都经网关，公开路径形如 `/api/v3/<service>/<后端路径>`，网关 `StripPrefix=2` 去掉 `/api/v3` 再转发（证据 `dc3-common-gateway/.../application-gateway.yml:62-101`）：

- 登录：`POST /api/v3/auth/token/salt`、`POST /api/v3/auth/token/generate`
- 管理中心：`/api/v3/manager/...`（`/api/v3/manager/driver/add`、`/profile/add`、`/point/add`、`/device/add`、`/point_attribute_config/add`）
- 数据中心：`/api/v3/data/...`（`/api/v3/data/point_value/latest`、`/point_value/list`、`/point_command/read`、`/point_command/write`、`/point_command_history/get_by_command_id`）
- 智能中心：`/api/v3/agentic/...`

factspack 早前给的是去前缀的后端 controller 路径（如 `/point_value/latest`），**对外不可直接调用**。所有页面里 `http://localhost:8000/...` 的示例必须带 `/api/v3/<service>/` 前缀。

## 2. PointValueVO 真实字段（无 value / valueTime）

`deviceId, pointId, rawValue(String 原始值), calValue(String 工程值), numValue(Double 数值投影,可空), hasLatestValue(Boolean), driverId, tenantId, createTime(LocalDateTime), operateTime(LocalDateTime)`。时间是本地日期时间（示例 `2025-09-01T12:00:00`，**非带 Z 的 UTC**）。**没有 `value`、没有 `valueTime`**。证据 `PointValueVO.java:51-111`。JSON 示例的值字段用 `calValue`/`numValue`/`rawValue`、时间字段用 `createTime`/`operateTime`。

## 3. PointTypeEnum 是 8 个值

`STRING(0)/BYTE(1)/SHORT(2)/INT(3)/LONG(4)/FLOAT(5)/DOUBLE(6)/BOOLEAN(7)`。证据 `PointTypeEnum.java:36-77`。凡列举位号数据类型必须给全 8 个，不要只写 STRING/INT/FLOAT/DOUBLE。

## 4. 命令回执查询：point_command_history，不是 command_history

point 读/写命令回执：`GET /api/v3/data/point_command_history/get_by_command_id`（按 `command_id`），返回 `PointCommandHistoryVO`（字段 `status`=PointCommandStatusEnum、`responseValue`；**无 executeStatus/executeResult**），落表 `dc3_point_command_history`。
`/command_history`（表 `dc3_command_history`、`CommandHistoryVO`，字段 recordId/resultValues/status）是另一套"自定义命令"历史，**勿与 point 命令混用**。证据 `DataConstant.java:44/46/64`、`PointCommandHistoryVO.java`、`03-iot-dc3-data.sql:733/826`。

## 5. 批处理间隔单位是"秒"非毫秒

`MQTT_BATCH_INTERVAL`、`POINT_BATCH_INTERVAL` 默认 `5`，单位**秒**（Quartz `DateBuilder.IntervalUnit.SECOND`）。`speed = count / interval`。`PointValueJob` 由 Quartz 定时把整个累积缓冲一次性刷出，**没有"批量大小触发"也没有"谁先到谁先刷"**。证据 `ScheduleForDataServiceImpl.java:54`、`PointValueJob.java`、`MqttScheduleServiceImpl.java:52`。

## 6. PointCommandDTO 是普通 record；sealed 的是 payload

`PointCommandDTO` 是普通 `record`（Java record 不能 `sealed`）；`PointCommandPayload` 才是 `sealed interface`，permits `ReadPayload`/`WritePayload`。表述为"强类型 record，其 payload 是 sealed 接口"。证据 `PointCommandDTO.java:36`、`PointCommandPayload.java:37`。

## 7. 命令状态 TIMEOUT 当前无生产者

`PointCommandStatusEnum` 含 `TIMEOUT`，但全仓无代码把命令置为 `TIMEOUT`（`SUCCESS/FAILED/EXPIRED/DUPLICATE/DEAD` 均有生产者）。状态机图/表应标注 TIMEOUT 为"枚举已预留、当前链路尚不产生"，不要画成与其它终态等价的活跃边（用 `::: info` 注明）。

## 8. add 返回成功码，不返回新建 ID

`BaseService.add` 返回 `void`；Controller 的 add 返回 `R.ok(SuccessCode.ADD)`（成功码 "Added successfully"），**不返回新建实体 ID**。证据 `BaseService.java:38`、`PointController.java:103-108`。golden-path 教程里 add 之后若需要 id 用于后续步骤，应说明"通过对应 list 接口按名称/条件查回新建实体的 id"，不要假装 add 直接返回 id。classDiagram 里 Service 的 `add` 标 `void`，不要标 `Long`。

## 9. 最新值缓存 key 格式

`REAL_TIME_VALUE_KEY_PREFIX + tenantId + "." + deviceId + "." + pointId`（点号分隔、带前缀），**不是** `tenant:device:point` 冒号式。证据 `PointValueLocalCache.java:83-86`。

## 10. PointValueBO 在 dc3-common-repository

`PointValueBO` 定义在 `dc3-common-repository`，**不在** `dc3-common-model`（后者仅含 `PointCommandDTO` 等）。模块地图勿把 PointValueBO 列为 dc3-common-model 代表类。

## 11. 驱动 28 个含 dc3-driver-http；compose 默认内置 8 个

驱动归类必须包含 `dc3-driver-http`（HTTP REST 客户端驱动），别漏。注意区分：**"28" 是全量驱动目录**（`dc3-driver/pom.xml`）；**docker-compose.yml 默认只内置 8 个驱动容器**（`listening-virtual/modbus-tcp/modbus-rtu/mqtt/opc-da/opc-ua/plcs7/virtual`）。讲 compose 的 depends_on 拓扑时用 8，讲驱动目录时用 28，勿混。

## 12. 端口对外性按部署栈区分（services / 部署页关键）

- **app 栈**（`docker-compose.yml`，`make up STACK=app`）：对宿主机**只发布** web `8080/8443` 与 listening-virtual `TCP 6270 / UDP 6271`；网关 `8000` **不发布到宿主机**，外部经 web `8080`（nginx 反代到 `dc3-gateway:8000`）。宿主机 `curl 127.0.0.1:8000` 在 app 栈**不通**。
- **dev 栈**（`docker-compose-dev.yml`，`make up-dev`）：发布网关 `8000`，且同时发布各中心 `8300/9300、8400/9400、8500/9500、8600`。宿主机直连 `8000` 仅在 dev 栈成立。

证据 `docker-compose.yml:58-60/228-231`、`docker-compose-dev.yml:65-66`。涉及"网关唯一对外"的表述要按栈澄清：业务上网关是唯一 API 聚合入口，但 app 栈下宿主机入口是 web 8080。

## 13. 本地 dev profile 端口默认

`POSTGRES_PORT` 在 dev profile 默认回退 `35432`（非 5432；5432 仅 pre/pro/test）。证据 `application-dev.yml:28`。

## 14. Spring AI 版本是里程碑版

pom 实为 `2.0.0-M8`（里程碑预发布），非 GA 2.0。技术栈写 **"Spring AI 2.0.0-M8"**。证据 `pom.xml:72`。

## 15. AGENTIC_MEMORY_SCHEMA_INIT 绑定存疑

该变量经 compose/dev.env 注入，但仓库内未见 `application*.yml` 把它绑定到 Spring AI 的 `initialize-schema`。涉及"设为 always 即自动建表"的描述要加"以代码为准"或弱化，不要当成已接线的可用开关。

## 16. 统一中文术语 + 命令下发归属

- PointValue 全站统一中文用 **位号值**（与 位号=Point 一致）；不要用"点位值 / 测点"。
- **命令下发职责属数据中心**（`PointCommandController` 在 `dc3-common-data`）。心智模型/散文里不要写成"管理中心下发命令"。管理中心只管元数据（驱动/模板/设备/位号）。
