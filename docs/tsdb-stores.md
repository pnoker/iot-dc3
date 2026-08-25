# 时序存储选型指南

DC3 的设备位值历史存放在可插拔的时序存储后面（端口定义见
[docs/design/tsdb-abstraction.md](./design/tsdb-abstraction.md)）。每个存储 一个适配器模块，由 `dc3.tsdb.type`
选择，同时只激活一个；每个适配器都 通过同一套 24 例库中立契约套件（`dc3-tsdb-tck`）认证。

## 能力矩阵（按适配器实际声明发布，非预估）

| 能力                     | timescale                           | tdengine                      | influxdb (3.x)                             | iotdb (2.x)                                    |
|--------------------------|-------------------------------------|-------------------------------|--------------------------------------------|------------------------------------------------|
| 部署形态                 | 内嵌 PG（默认）/外置                | 外置                          | 外置                                       | 外置                                           |
| 序列模型                 | 行列 (tenant,device,point)          | 超级表+tags+确定性子表        | measurement+tags                           | 树路径 `root.dc3.t*.d*.p*`                     |
| 去重 (序列,时间)         | upsert 后写胜                       | 后写胜                        | 后写胜                                     | 后写胜                                         |
| 微秒精度                 | ✅                                  | ✅（库级 `PRECISION 'us'`）   | ✅（ns 存储）                              | ✅（需 `timestamp_precision=us`）              |
| 分桶聚合                 | ✅ time_bucket                      | ✅ INTERVAL                   | ✅ date_bin                                | ✅ GROUP BY 窗口                               |
| 空桶补零                 | ❌（省略空桶，面板端补零）          | ❌（省略空桶）                | ❌（省略空桶）                             | ❌（省略空桶）                                 |
| PERCENTILE               | ✅ 精确                             | ✅ 精确（单表限制→子表直查）  | ❌（近似→声明 false，门面精算）            | ❌（无此函数→声明 false）                      |
| FIRST/LAST (M4)          | ✅                                  | ✅                            | ✅（ordered array_agg）                    | ✅ FIRST_VALUE/LAST_VALUE                      |
| 租户分析面 S13-①②③⑤      | ✅                                  | ✅                            | ✅                                         | ✅（driver 维度除外，见下）                    |
| 按 driver 分组计数 S13-② | ✅                                  | ✅                            | ✅                                         | ❌（driver 是 measurement 非路径层——如实拒绝） |
| 延迟直方图 S13-④         | ✅                                  | ❌ 声明 false（面板零桶降级） | ✅                                         | ❌ 声明 false                                  |
| rollup 分级 S16          | ✅ NATIVE（共享 cagg，见设计 §9.6；无扩展的纯 PG 部署启动时协商降级 NONE，读走原始扫描） | NONE（诚实原始扫描）          | NONE                                       | NONE                                           |
| 范围删除                 | ✅                                  | ✅（含上界-1µs 换算）         | ❌（Core 无行删除→声明 false，走分区工具） | ✅（deleteData）                               |
| 相关系数 S19             | ✅ SQL 端                           | ❌ 声明 false（门面桶化自算） | ❌ 声明 false                              | ❌ 声明 false                                  |
| 契约套件                 | 24/24                               | 24/24（2 跳过）               | 24/24（2 跳过）                            | 24/24（3 跳过）                                |

跳过数=该适配器如实声明为不支持、由套件按能力门控跳过的用例数。跳过不是 缺陷：不支持的能力由数据中心门面降级（如延迟直方图返回零桶、相关系数与
精确分位数由 S19 门面从有界拉取中自算），绝不给错数据。

## 换型操作

1. `make up STACK=optional SERVICES="tdengine"`（或 influxdb / iotdb）启动目标 存储；IoTDB 首启会自动挂载
   `dc3/dependencies/iotdb/iotdb-system.properties`
   （微秒精度 + 全接口 RPC，两行配置， **必需**）。
2. 主栈环境：`DC3_TSDB_TYPE=tdengine`（默认 `timescale`），按需覆盖连接变量：
    - tdengine：`DC3_TSDB_TDENGINE_URL`（默认 `jdbc:TAOS-RS://dc3-tdengine:6041/`）
    - influxdb：`DC3_TSDB_INFLUXDB_URL` / `DC3_TSDB_INFLUXDB_TOKEN`（admin token 自建）
    - iotdb：`DC3_TSDB_IOTDB_HOST` / `DC3_TSDB_IOTDB_PORT`（root/root 默认）
3. Maven 侧把所选适配器加入数据中心依赖（默认只打包 timescale）：

```xml
<dependency>
    <groupId>io.github.pnoker</groupId>
    <artifactId>dc3-tsdb-tdengine</artifactId> <!-- 或 dc3-tsdb-influxdb / dc3-tsdb-iotdb -->
</dependency>
```

> 与 MQ 家族同款约定（[docs/mq-brokers.md](./mq-brokers.md)）：默认适配器随
> 服务打包，其余按部署显式引入——没人应为不用的存储付出 jar 与启动开销。

## 各存储落地要点（适配器 javadoc 有完整清单）

- **timescale**：内嵌模式复用主 PG 的 `history` 数据源；S16 三级生命周期 （raw 30 天 → 1 分钟层 1 年 → 1 小时层永久）与
  Grafana 共享同一套 real-time cagg。无 timescaledb 扩展的纯 PG 部署在启动时把 rollup 能力协商为 NONE（日志可见），分桶读自动走原始扫描；分桶聚合不补零
  （`gapFill=false`，空桶省略，消费端自行补零）。
- **tdengine**：时间戳全程 epoch-µs 字面量（REST 驱动的 Timestamp 序列化按 JVM 时区、服务端按 UTC 解析——字符串形态必漂移）；PERCENTILE
  单表限制走 确定性子表直查，子表未建（尚无样本）时按空序列处理而非报错；保留天数可配（`dc3.tsdb.tdengine.retention-days`，默认 180，仅影响新建库）。
- **influxdb**：v3 HTTP API 直连（行协议写入 + query_sql CSV 读取，零客户端 依赖）；整型字段必须带 `i` 后缀（否则列绑 Float64
  永久丢 ns 精度）；行协议 字符串字段转义反斜杠/双引号/换行/回车（裸换行会截断行、腐蚀整批写入）；行删除 不存在，租户注销走分区工具。
- **iotdb**：路径节点不能纯数字（`t1/d10/p20` 前缀）；`WHERE time` 的裸数字按 **毫秒**解释（与库精度无关，µs 字面量会静默匹配空集）——一律用
  `2026-08-20T12:59:59+00:00` 形态；租户级 last/history 通过通配符枚举序列 如实支持（`tenantWideScan=true`）；保留天数可配
  （`dc3.tsdb.iotdb.ttl-days`，默认 180）；session 池化且 必须关重定向 （`enableRedirection(false)`，节点发现会发容器内地址）。

## 认证复跑

```bash
export DOCKER_HOST="unix://$(podman info --format '{{.Host.RemoteSocket.Path}}')"
export TESTCONTAINERS_RYUK_DISABLED=true
mvn -s .mvn/settings.xml -f dc3-tsdb/pom.xml -DskipTests install
mvn -s .mvn/settings.xml -pl dc3-tsdb/dc3-tsdb-tck test -Dtest=TimescaleContractTest
# 其余三库需显式开启（镜像较重）:
export DC3_TSDB_TCK=true
mvn -s .mvn/settings.xml -pl dc3-tsdb/dc3-tsdb-tck test -Dtest=TdengineContractTest   # 或 Influxdb / Iotdb
```
