# Docs 审查发现清单（2026-06-23，35 页全审）

> 修复时：定位你负责页面的小节，逐条修掉 factErrors 与 contractIssues/linkIssues；并对照
> 2026-06-23-docs-fact-corrections.md 复核同类问题。

## 概况

- 有事实错误的页：27
- major: 25
- minor: 5

## index.md  [minor]

**事实错误：**

- 原文：技术栈写 “[Spring AI 2.0](https://spring.io/projects/spring-ai)”，暗示已发布的 2.0 正式版
    - 实情：实际依赖版本是里程碑预发布 2.0.0-M8（milestone），并非 GA 2.0。写成裸 “2.0” 会让读者以为用的是正式版。建议写
      “Spring AI 2.0.0-M8（里程碑）” 或至少 “Spring AI 2.0 (M)”。注：dossier/factspack 也沿用了不精确的 “Spring AI
      2.0”，属继承性误差
    - 证据：/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/pom.xml:72 (<spring-ai.version>2.0.0-M8</spring-ai.version>)

**契约问题：**

- 缺“延伸阅读”小节（契约 §1/§7 要求普通页都有）。本页是 VitePress home 布局，feature 卡片承担了跨链接导航职责，按 §7
  “纯清单/社区页除外” 的精神可豁免；仅作提示，非硬违规。
- 技术栈一节为纯并列条目（Java 21 · Spring Boot … · …），前面无散文铺垫，紧贴在 mermaid 图后。home 页性质下可接受，但严格按
  §1“段落优先于表格/列表，先散文后参考”略显裸列。

## introduction/index.md  [minor]

**契约问题：**

- 术语不一致（§6/§7）：页面三处（line 3/31/89）用"位号值"指代 `PointValue`，但 §6 术语表规定 PointValue 的中文是"点位值"
  （位号=Point，点位值=PointValue）。页面把 Point 的中文"位号"+"值"拼成"位号值"，与术语表的"点位值"
  不一致。页面内部三处自洽，仅与全站术语表偏离，属轻度问题。
- （边界性，非错误）line 74 "Spring AI 2.0"：POM 实为 `2.0.0-M8`（pom.xml:72，里程碑版而非 GA）。但 dossier/factspack
  全站均写作"Spring AI 2.0"，此为既定事实底座约定，非本页引入；保留记录供全站统一时一并处理。

## introduction/concepts.md  [major]

**事实错误：**

- 原文：一句话心智模型（L9）：「数据中心存值，管理中心下发命令。」以及 L11 散文「管理中心下发命令」
    - 实情：命令下发不是管理中心（Manager Center）的职责，而是数据中心（Data Center）。命令入口 PointCommandController 位于
      dc3-common-data（数据中心），命令链路是 网关→数据中心→RabbitMQ→驱动→设备。页面自身 L84 的命令流图也画的是「数据中心」，与
      L9 的一句话心智模型自相矛盾。这是全页最显眼的标题级断言，却把下发命令的中心说错了。
    -
  证据：dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/controller/PointCommandController.java（命令入口在
  Data Center）；dossier §B.5；本页 L84 命令流图
- 原文：ER 图（L34）：pointTypeFlag "STRING/INT/FLOAT/DOUBLE"，把位号数据类型呈现为这 4 个值的封闭集合
    - 实情：PointTypeEnum 实际有 8 个值：STRING(0)/BYTE(1)/SHORT(2)/INT(3)/LONG(4)/FLOAT(5)/DOUBLE(6)/BOOLEAN(7)
      。页面把数据类型集合表述为仅 STRING/INT/FLOAT/DOUBLE，漏掉 BYTE/SHORT/LONG/BOOLEAN（注：factspack L160
      同样写错，页面照搬了，但与真实源码不符）。
    - 证据：dc3-common/dc3-common-constant/src/main/java/io/github/pnoker/common/enums/PointTypeEnum.java:41-76

**契约问题：**

- 术语不一致（契约 §6 词表）：PointValue 全站统一中文名应为「点位值」（契约 L108），但本页 L11、L15 用「位号值」。与词表冲突。
- 术语混用（契约 §6 L119「不要混用点位/测点/位号——统一位号(Point)」）：L56「一个测点/数据项」用了被明确禁止的「测点」。

## architecture/services.md  [major]

**事实错误：**

- 原文：端口表（第70行）：网关 `dc3-gateway` HTTP `8000`，对外列='是（唯一）'，环境变量='DC3_GATEWAY_PORT'；并多处称『只有网关的
  HTTP 8000 对外』（第27行）。页面把整篇围绕 docker-compose.yml 展开，启动指引用 `make up STACK=app`（解析到
  docker-compose.yml）。
    - 实情：在 docker-compose.yml（app 栈）里 gateway 服务块根本没有 `ports:` 映射，网关 8000 不发布到宿主机，DC3_GATEWAY_PORT
      在该文件里完全未被引用。该文件里唯一被发布到宿主机的端口只有 web（8080/8443）和 listening-virtual（6270/6271）。网关
      8000 仅在 docker-compose-dev.yml 中通过 DC3_GATEWAY_PORT 发布（且 dev 栈同时还把各中心
      8300/9300、8400/9400、8500/9500、8600 全部发布到宿主机）。因此 app 栈里真正的宿主机 HTTP 入口是 web 的 8080（nginx 内部反代到
      dc3-gateway:8000），网关 8000 只在容器网络内可达。
    - 证据：dc3/docker-compose.yml:80-110（gateway 块无 ports:）；dc3/docker-compose.yml:58-60（web ports
      8080/8443）、228-231（listening-virtual 6270/6271）——全文件仅这两处 ports:；DC3_GATEWAY_PORT 仅出现在
      dc3/docker-compose-dev.yml:66；Makefile:87,90（STACK=app→docker-compose.yml）
- 原文：第159行：起 app 栈后让用户在宿主机执行 `curl -fsS http://127.0.0.1:8000/actuator/health/readiness`，并称『只有它通过，对外
  8000 才真正可用』。
    - 实情：在 docker-compose.yml（即 `make up STACK=app` / `podman compose -f dc3/docker-compose.yml up -d`
      ，正是本节给出的命令）下，网关 8000 未发布到宿主机，宿主机 `curl 127.0.0.1:8000` 会连接失败。该 curl 只在 dev
      栈（docker-compose-dev.yml）下成立。容器内 healthcheck 用 127.0.0.1:8000 是对的，但那是容器内、不是宿主机。
    - 证据：dc3/docker-compose.yml:80-110（gateway 无 ports）对比 dc3/docker-compose-dev.yml:65-66（gateway 才发布
      8000）；本节命令见 docs/architecture/services.md:135,147
- 原文：第78行 warning：『唯一被映射到宿主机的后端端口是 listening-virtual 驱动的设备入站口 TCP 6270 / UDP 6271，以及 Web
  前端。任何要从外部访问业务 API 的请求都必须走网关 8000。』
    - 实情：这段自相矛盾：其端口枚举（只有 listening-virtual + web）正确地隐含了网关 8000 未被发布到宿主机，但紧接着又要求外部请求『走网关
      8000』。在 app 栈里外部请求实际走 web 8080，由 nginx 内部反代到 dc3-gateway:8000；宿主机无法直连 8000。
    - 证据：dc3/docker-compose.yml 仅有 web(58-60) 与 listening-virtual(228-231) 两处 host 端口映射，gateway(80-110)
      无映射；web 通过 APP_API_HOST=dc3-gateway/APP_API_PORT=8000 内部反代，见 docker-compose.yml:62-63

**契约问题：**

- 诚实标注/事实准确（契约 §5/§7）：拓扑图（第31-62行）被明确定义为『docker-compose.yml 里的 depends_on
  健康依赖』拓扑（见第3、29行），但图中驱动标注为『(28 个)』。docker-compose.yml 实际只内置 8
  个驱动服务（listening-virtual、modbus-tcp、modbus-rtu、mqtt、opc-da、opc-ua、plcs7、virtual），depends_on 图里就是 8 个驱动依赖
  manager。28 是全量驱动目录总数（dc3-driver/pom.xml），不是该 compose 文件的 depends_on 拓扑节点数。在『读完你能看懂
  docker-compose.yml 里每一条 depends_on』的语境下，把 28 放进该拓扑图会误导读者以为 compose 里有 28
  个驱动容器。证据：dc3/docker-compose.yml:219-368 仅 8 个 driver 服务块。
- 约束与边界小节（第166-171行）重申『网关是唯一对外面』『除网关 8000…外所有后端端口只在容器网络内可达』，延续了上面的端口对外性事实错误，未区分
  app 栈（网关 8000 不发布、web 8080 才是宿主机入口）与 dev 栈（网关 8000 发布、但各中心端口同样全发布）。

## architecture/facade-modes.md  [minor]

**事实错误：**

- 原文：约束与边界第一条：『环境变量目录里把 local 描述成"REST"是不准确的』(line 100)
    - 实情：没有任何已发布页面把 local 描述成 REST。已发布的环境变量页 quickstart/environment.md（标题《环境变量详解》）第157行把
      DC3_FACADE_MODE 描述为『facade 协议模式』，并非『REST』。唯一出现 grpc or REST 字样的是内部 superpowers 规格
      factspack.md:270，且它描述的是 DC3_FACADE_MODE 整体而非把 local 等同 REST。该页在反驳一个已发布文档中不存在的说法，等于隐式引用了
      superpowers 内部资料。
    - 证据：quickstart/environment.md:157 (描述为『facade 协议模式』);
      docs/superpowers/specs/2026-06-22-docs-overhaul-factspack.md:270 (唯一的 'grpc or REST' 出处，属内部规格)

**契约问题：**

- §7 暴露/借用 superpowers 内部资料：line 100 的『环境变量目录里把 local 描述成 REST 是不准确的』所反驳的 'REST' 说法只存在于内部
  factspack.md，已发布的《环境变量详解》页并无此措辞，等于隐式以内部草稿为靶子。建议删去该括号反驳，或改为针对『facade
  协议模式』这一已发布措辞做澄清。

## architecture/data-plane.md  [major]

**事实错误：**

- 原文：PointValueVO 对外暴露 value/valueTime 字段；JSON 示例为 { "deviceId": 1024, "pointId": 2048, "value": "23.5", "
  valueTime": "..." }（classDiagram line 95-96、正文 line 111、JSON 示例 line 209、tip box line 216 均如此）
    - 实情：实际 PointValueVO 没有 value 或 valueTime 字段。它暴露的是
      deviceId/pointId/rawValue/calValue/numValue/hasLatestValue/driverId/tenantId/createTime/operateTime。读者照抄这个
      JSON 字段名会拿不到数据。tip box 虽然写了"以实际响应为准"，但仍把错误字段名当示例打出来。
    - 证据：dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/entity/vo/PointValueVO.java:55-111（字段为
      deviceId/pointId/rawValue/calValue/numValue/hasLatestValue/driverId/tenantId/createTime/operateTime，无
      value/valueTime）
- 原文：速率超过阈值时改交 PointValueJob 批处理，攒到批量大小或间隔（POINT_BATCH_INTERVAL 默认 5 ms）触发，谁先到谁先刷（line
  50）
    - 实情：批处理没有"批量大小"触发，也不存在"谁先到谁先刷"竞争。PointValueJob.executeInternal 由 Quartz
      定时触发，每次把整个累积缓冲一次性刷出，与缓冲大小无关。POINT_BATCH_INTERVAL(interval, 默认 5)
      不是毫秒、也不是刷新间隔，而是速率计算的除数：speed = VALUE_COUNT/interval。该"count-or-interval whichever-first"措辞是从
      MQTT 批处理误植到点位值路径。
    - 证据：dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/job/PointValueJob.java:
      122-146（executeInternal 整缓冲刷出，无 size 触发；line 124 speed=count/interval）；PointBatchProperties speed=100
      interval=5（无毫秒语义）
- 原文：Caffeine 最新值缓存 key=tenant:device:point（mermaid line 17、正文 line 57，冒号分隔）
    - 实情：实际缓存 key 用点号分隔且带前缀：REAL_TIME_VALUE_KEY_PREFIX + tenantId + "." + deviceId + "." +
      pointId，不是冒号分隔的 tenant:device:point。结构（租户/设备/位号）对，分隔符与前缀错。
    - 证据：dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/cache/PointValueLocalCache.java:
      83-86（buildKey 用 SymbolConstant.DOT 拼接，带 PrefixConstant.REAL_TIME_VALUE_KEY_PREFIX）

## architecture/command-plane.md  [major]

**事实错误：**

- 原文：跨 RabbitMQ 传输的...是一个强类型的 `sealed` record `PointCommandDTO`（第 60 行，及代码注释"强类型的 sealed
  record"）
    - 实情：`PointCommandDTO` 是普通 `record`，没有 `sealed` 修饰；Java 中 record 也不能声明为 sealed。真正 `sealed` 的是其字段类型
      `PointCommandPayload`（sealed interface，permits ReadPayload/WritePayload）。把 DTO 说成 "sealed record"
      是错误的，应表述为"强类型 record，其 payload 是 sealed 接口"。
    - 证据：dc3-common/dc3-common-model/src/main/java/io/github/pnoker/common/entity/dto/PointCommandDTO.java:36 (
      `public record PointCommandDTO(` 无 sealed)；对比 PointCommandPayload.java:37 (
      `public sealed interface PointCommandPayload`)

**契约问题：**

- TIMEOUT 被当成已实现的活跃终态：状态机图 `SENT --> TIMEOUT: 应用层超时`（第108行）与状态表 `TIMEOUT | 4 | 应用层超时`
  （第126行）都把它呈现为可达终态，但全仓库没有任何代码把命令置为 PointCommandStatusEnum.TIMEOUT（grep 仅命中 enum 定义本身；DEAD
  有 PointCommandDeadReceiver 设置，SUCCESS/FAILED/EXPIRED/DUPLICATE 均有，唯独 TIMEOUT 无生产者）。按写作契约
  §4（已实现/受开关/未实现要诚实区分），应标注 TIMEOUT 为枚举已预留、当前链路尚不产生，而非画成与其他终态等价的活跃边。
- 第158行散文只把命令队列入死信解释为"驱动侧 30 秒内没被消费（TTL 到期）"，遗漏了基本校验失败时驱动 reject 直接入 DLX
  这一路径；流程图标签 `超时/reject`（第151行）虽覆盖了两者，但正文与图不一致，正文偏窄。属轻微完整性问题。

## architecture/auth-rbac.md  [major]

**事实错误：**

- 原文：`dc3_resource`（资源即权限码，如 `device:add`、`point_command:list`）— 同时 RBAC flowchart 节点 RES 标注
  `resource_code 权限码`，正文称资源码形如 device:add
    - 实情：实际 resource_code 是三段式 `{spring.application.name}:{domain}:{scope}`，由 @perm.can 在运行时拼成（如
      `dc3-center-manager:device:add`、`dc3-center-data:point_command:list`）。页面给的两段式 `device:add`/
      `point_command:list` 既不是 @PreAuthorize 校验的字符串，也不是 dc3_resource 中存储的 resource_code 形态（seed 里 API
      级权限根本未单独建行，默认管理员走通配 `*`）。读者照此去配权限会配错。
    - 证据：PermissionMethods.java:74 `String resourceCode = serviceName + ":" + domain + ":" + scope;` 及类注释
      `{spring.application.name}:{domain}:{scope}`；DeviceController.java:97 `@perm.can('device', 'add')`
      ；PointCommandController.java:65 `@perm.can('point_command', 'list')`；02-iot-dc3-auth.sql:2082 默认管理员资源为通配
      `'*'`，无 `device:add` 行
- 原文：注销时把 `(tenantCode:loginName)` 写入 Caffeine 注销名单（denylist）；约束表第 8 行同义
    - 实情：denylist 实际键是 `tenantCode:principalId`，不是 loginName。tryCancelToken 把 principalId 当作 markLogout
      的第一个形参传入（该形参虽命名 loginName，但传的是 principalId）；checkValid 的校验侧同样用
      principalId。TokenDenylistCache 自身的 javadoc/形参名是 loginName 属误导，页面与 dossier 都沿用了这个错误。
    - 证据：TokenServiceImpl.java:139-140
      `String principalKey = String.valueOf(credential.getPrincipalId()); tokenDenylistCache.markLogout(principalKey, tenantCode, ...)`
      ；TokenServiceImpl.java:165-169 读取侧同样用 principalKey=principalId；TokenDenylistCache.java:88-89 `buildKey` =
      `tenantCode + COLON + loginName`（实际传入的是 principalId）
- 原文：校验链步骤 5：密码 — 用 Argon2id 校验密码哈希
    - 实情：校验不是固定走 Argon2id，而是按存储哈希的算法分派（ARGON2ID 或 BCRYPT）。默认 seed 用户 dc3 的口令以 BCRYPT(
      cost=12) 存储，故黄金路径登录实际用 BCrypt 校验，并非 Argon2id。只有新口令的 encode() 才优先 Argon2id（不可用时回退
      bcrypt）。
    - 证据：PasswordUtil.java:67-76 verify() 按 algorithmOfHash 分派 ARGON2ID/BCRYPT；02-iot-dc3-auth.sql:1869-1872 默认用户
      dc3 存储为 `$2b$12$...`，password_algorithm=`BCRYPT`，注释 `stored as bcrypt(raw password), cost factor 12`

**契约问题：**

- ER 图 LOCAL_CREDENTIAL 把 `password_hash "ARGON2ID / BCRYPT"` 作为哈希列注释，但算法实际记录在独立列
  password_algorithm（02-iot-dc3-auth.sql:272 DEFAULT 'ARGON2ID'，CHECK IN ARGON2ID/BCRYPT），password_hash
  只存哈希串。属图示标注精度问题，非硬错误。

## architecture/modules.md  [major]

**事实错误：**

- 原文：驱动归类表只覆盖 27 个驱动，缺 dc3-driver-http；但全页（导语 line 3「28 个驱动如何按协议归类」、line 105「28
  个驱动是平台协议广度的载体」、line 107）声称组织全部 28 个
    - 实情：pom 的 28 个模块里包含 dc3-driver-http（HTTP REST 客户端驱动），factspack 把它归在 IoT & Wireless 类。页面表格 +
      「另含」prose 列出的并集是 27 个，HTTP 驱动被整体漏掉，与「28 个」自相矛盾
    - 证据：dc3-driver/pom.xml:187 列出 <module>dc3-driver-http</module>
      ；docs/superpowers/specs/2026-06-22-docs-overhaul-factspack.md:110 dc3-driver-http=HTTP REST；页面内 grep "http" 命中
      0 次
- 原文：line 124 `::: danger` 块：「每个驱动启动时用 `dc3.driver.code` 向管理中心注册，值/命令的 RabbitMQ routing key
  也据此派生。改它等于换了路由身份」
    - 实情：值/命令的 routing key 实际由 driverProperties.getService()（服务名 dc3.driver.service）拼接派生，而非
      dc3.driver.code。code 与 service 是 DriverProperties 上两个独立字段（code 用于注册标识，service「typically composed
      from tenant and application name」）。把 routing key 说成据 code 派生不准确
    - 证据：DriverSenderServiceImpl.java:168
      `String routingKey = RabbitConstant.ROUTING_POINT_VALUE_PREFIX + driverProperties.getService();`（命令结果同理 line
      192/201）；DriverProperties.java:78 `private String code;` 与 :131 `private String service;` 为两个字段
- 原文：line 177：「`DriverValidator` 负责校验与（virtual 驱动用到的）合成值生成」，把 simulate 生成的合成值归给 virtual 驱动
    - 实情：virtual 驱动的合成值由其自身 read() 用 ThreadLocalRandom 随机生成，并不调用 DriverValidator.simulate()
      ；simulate() 是一个独立的「确定性」合成值生成器，其 Javadoc 明确写「Unlike the virtual driver's random generator, this
      produces stable outputs」。把 simulate 说成「virtual 驱动用到的」与源码相反
    - 证据：VirtualDriverCustomServiceImpl.java:203,206 用 ThreadLocalRandom 在 read 内生成随机值；DriverValidator.java:
      37-38 Javadoc「Unlike the virtual driver's random generator, this produces stable outputs」，simulate 默认实现 line
      109-119 返回固定 fixture 值

## quickstart/index.md  [major]

**事实错误：**

- 原文：curl -s -X POST http://localhost:8000/api/v3/token/salt … 和 …/api/v3/token/generate （第 125、130 行登录黄金路径的两条
  curl）
    - 实情：网关实际路由是 /api/v3/auth/token/** （Path=/api/v3/auth/token/**，StripPrefix=2），不存在 /api/v3/token/**
      路由。页面给出的 URL 缺少 /auth 段，照抄会 404。正确应为 http://localhost:8000/api/v3/auth/token/salt 与
      …/api/v3/auth/token/generate。
    - 证据：dc3-common/dc3-common-gateway/src/main/resources/application-gateway.yml:64-67 (auth_route_token:
      Path=/api/v3/auth/token/**, StripPrefix=2)；grep 全仓无 /api/v3/token/** 路由，只有 /api/v3/auth/token/**

## quickstart/environment.md  [major]

**事实错误：**

- 原文：`REGISTRY` | `auto` | Makefile 镜像源选择器（`auto`/`cn`/主机名）
    - 实情：Makefile 只接受三个枚举值 auto|global|cn，其它任何值（含主机名）都会触发 $(error) 直接报错。页面既漏掉了真实存在的
      `global`，又虚构了被拒绝的「主机名」。正确表述应为 `auto`/`global`/`cn`。
    - 证据：/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/Makefile:75-85 （ifeq auto/global/cn，else $(error) Use
      REGISTRY=auto|global|cn）

**契约问题：**

- §4 诚实标注存疑：页面把 `AGENTIC_MEMORY_ENABLED` 默认值写为 `false`、`AGENTIC_ATTACHMENT_STORAGE_PATH` 写为
  `dc3/data/agentic/attachments`（取自 .env.example）。但代码内 application-agentic.yml:20/27 的回退默认是
  `${AGENTIC_MEMORY_ENABLED:true}` 与 `dc3/data/upload/agentic/attachment`——即未设环境变量时记忆默认开启、路径不同。页面遵循
  .env.example 工作流（cp+source 会显式注入 false）尚可自洽，但 factspack line316 已明确点出此「双默认」需要 reconcile，页面未加任何
  `::: info 以代码为准` 之类提示，读者若不走 .env.example 路径会被误导。建议就这两个值加一句标注。
- §5 机制描述无源码支撑：页面（line 194/200-202）称 `AGENTIC_MEMORY_SCHEMA_INIT=always` 会「让 Spring AI 建表」。该变量仅在
  docker-compose*.yml 以 environment 注入容器（docker-compose-dev.yml:200），但全仓 grep 不到任何 yml/属性把它绑定到 Spring
  AI 的 `spring.ai.chat.memory.repository.jdbc.initialize-schema`，记忆表实际由 initdb 06-iot-dc3-agentic.sql
  预建。该变量的「触发建表」机制未经源码验证，属臆测，建议软化或核实绑定点。

## quickstart/first-device.md  [major]

**事实错误：**

- 原文：第9步用 GET /command_history/get_by_record_id?recordId=<commandId> 轮询 /point_command/write 的写命令回执，并在
  mermaid 图中标 'GET /command_history/get_by_record_id'
    - 实情：/command_history 是『自定义命令(CommandCall, dc3.e.command)』命名空间，由 CommandHistoryController 提供，配对的是
      POST /command_history/call，查的是 CommandHistoryVO（另一张表/另一类 ID）。点位写命令 /point_command/write 返回的
      commandId 应去『点位命令历史』端点轮询：GET /point_command_history/get_by_command_id?commandId=<id>，返回
      PointCommandHistoryVO。两者是 dossier B.5/B.6 明确区分的不同 namespace，page 把写命令的回执指到了错误的端点与查询参数名（recordId
      vs commandId）
    -
  证据：dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/controller/PointCommandHistoryController.java:
  56,76-80 (@RequestMapping POINT_COMMAND_HISTORY_URL_PREFIX=/point_command_history, @GetMapping /get_by_command_id,
  参数 commandId)；CommandHistoryController.java:59,83,109 (/command_history 的 /call + /get_by_record_id 属
  CommandCall)；DataConstant.java:46,64
- 原文：回执示例 JSON 的字段为 commandCode/executeStatus/executeResult/executeTime，且正文称『轮询回执直到 executeStatus
  进入终态』
    - 实情：executeStatus/executeResult/executeTime 三个字段在两个 VO 上都不存在。点位命令历史 PointCommandHistoryVO
      的状态字段是 status（PointCommandStatusEnum），值字段是 requestValue/responseValue，时间是
      occurTime/sendTime/finishTime/expireTime；自定义命令 CommandHistoryVO 状态字段同样是 status，结果是
      resultValues。page 杜撰了 execute* 字段名
    - 证据：dc3-common/.../vo/PointCommandHistoryVO.java:73-103 (requestValue/responseValue/status/finishTime…，无
      execute*)；dc3-common/.../vo/CommandHistoryVO.java:69-105 (commandCode/resultValues/status，无
      executeStatus/executeResult/executeTime)
- 原文：第8步称 PointValueVO『含 value（字符串形态的值）和 valueTime』，示例 JSON 为 {"value":"25.0","valueTime":"
  2026-06-22T10:30:00Z"}
    - 实情：PointValueVO 没有 value 或 valueTime
      字段。实际字段：rawValue（原始值）、calValue（工程值，字符串）、numValue（数值投影）、hasLatestValue、driverId、tenantId、createTime、operateTime。值应取
      calValue/rawValue，时间取 createTime/operateTime
    - 证据：dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/entity/vo/PointValueVO.java:54-111
- 原文：/point_value/latest 与 /driver/list 的请求体把分页写成顶层 {…,"current":1,"size":10}（或 size:
  20），并正文称『current / size 分页』
    - 实情：这两个端点分别收 PointValueQuery / DriverQuery，分页参数在嵌套对象 page（类型 Pages，内含
      current/size）里，不是请求体顶层。顶层只有 deviceId/pointId（或 driverName）等过滤字段。当前请求体形态会丢失分页
    - 证据：dc3-common/.../query/PointValueQuery.java:57-58 (private Pages page;)；dc3-common/.../query/DriverQuery.java:
      53-54 (private Pages page;)；Pages.java:48,51 (current/size 在 Pages 内)；PointValueController.java:
      82-83、DriverController.java:237-238 (@RequestBody PointValueQuery/DriverQuery)
- 原文：第5步 tip 称『PointTypeEnum 为 STRING / INT / FLOAT / DOUBLE』作为完整取值集
    - 实情：PointTypeEnum 实际还含 LONG 与 BOOLEAN：STRING(0)/INT(3)/LONG(4)/FLOAT(5)/DOUBLE(6)/BOOLEAN(7)。page
      列举的枚举集不完整（漏 LONG、BOOLEAN）
    - 证据：dc3-common/dc3-common-constant/src/main/java/io/github/pnoker/common/enums/PointTypeEnum.java:41-76

**契约问题：**

- 第3步 driverName 描述『形如 dc3-driver-virtual（或 VirtualDriver）』不精确：virtual 驱动注册的 driver.name 是 'Virtual
  Driver'（带空格），'VirtualDriver' 是 driver.code（路由标识），'dc3-driver-virtual' 是模块/服务名——三者分属不同字段，把它们当作同一个
  driverName 的近似写法会误导读者用错字段（来源 dc3-driver/dc3-driver-virtual/src/main/resources/application.yml:
  21-22）。属契约§5『首次出现就给出值的来源』未落实，为 minor。

## operation/index.md  [major]

**事实错误：**

- 原文：成功信号「命令有回执」：『下发读/写命令后，凭返回的命令 ID 查 command_history，executeStatus/executeResult 有终态结果』(
  line 43)
    - 实情：字段名错且表名混淆。点位命令回执 VO 是 PointCommandHistoryVO，状态字段叫 status(PointCommandStatusEnum)、结果字段叫
      responseValue，根本没有 executeStatus/executeResult 这两个字段。executeStatus/executeResult 在 dc3-common-data
      全模块源码中不存在。同时该页黄金路径用的是 point_command/read，对应回执表是 dc3_point_command_history（路由前缀
      /point_command_history），而 command_history（/command_history、CommandHistoryVO、dc3.e.command
      自定义命令命名空间）是另一套独立通道，二者被混为一谈。
    - 证据：dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/entity/vo/PointCommandHistoryVO.java:
      73-85 (requestValue/responseValue/status，无 executeStatus/executeResult); DataConstant.java:46
      POINT_COMMAND_HISTORY_URL_PREFIX="/point_command_history" vs :64 COMMAND_HISTORY_URL_PREFIX="/command_history";
      PointCommandHistoryController.java:76 @GetMapping("/get_by_command_id")
- 原文：成功信号「位号有值」：『POST /point_value/latest 能查到该设备位号的最新值，value 与 valueTime 非空』(line 42)
    - 实情：字段名不存在。PointValueVO 没有 value 或 valueTime 字段；真实字段是 rawValue / calValue / numValue（值）与
      createTime / operateTime（时间）。判定『有值』应看 rawValue/calValue 或 numValue 与 createTime，不能用 value/valueTime。
    - 证据：dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/entity/vo/PointValueVO.java:67-111 (
      rawValue/calValue/numValue/createTime/operateTime，无 value/valueTime)
- 原文：最小示例第 2 步注释与 warning：『用它去 command_history 查回执』(line 94)、『凭 ID 查 command_history』(line 100)
    - 实情：误导：黄金路径下发的是 point_command/read（点位命令），其回执应查 /point_command_history（表
      dc3_point_command_history），而非 command_history。command_history 是自定义命令(CommandCall)
      的独立通道，二者命名空间不同。读者照此会查错接口/表。
    - 证据：DataConstant.java:44/46/64; PointCommandController.java:74 @PostMapping("/read") 返回 R<String> commandId;
      PointCommandHistoryController.java:56 @RequestMapping(POINT_COMMAND_HISTORY_URL_PREFIX)+:76 /get_by_command_id;
      dossier B.5 区分 dc3.e.point_command 与 dc3.e.command 两套命名空间

## operation/device-onboarding.md  [major]

**事实错误：**

- 原文：响应 JSON 示例 records 元素为 { "deviceId":..., "pointId":..., "value": "23.71", "valueTime": "2026-06-22T10:00:
  00Z" }，且正文称 Page<PointValueVO>（含 deviceId / pointId / value / valueTime）
    - 实情：PointValueVO 没有 value 字段也没有 valueTime 字段。真实字段为
      rawValue（原始值）、calValue（工程值，String）、numValue（Double，可空）、createTime（采集时间 LocalDateTime，格式 yyyy-MM-dd
      HH:mm:ss，非 ISO Z）、operateTime，外加 driverId/tenantId/hasLatestValue。按页面写的字段名解析响应会取不到值；valueTime
      这个键根本不存在，时间字段名是 createTime/operateTime。
    - 证据：dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/entity/vo/PointValueVO.java:55-111（字段
      deviceId/pointId/rawValue/calValue/numValue/hasLatestValue/driverId/tenantId/createTime/operateTime，无
      value/valueTime）
- 原文：pointTypeFlag 取 PointTypeEnum（STRING / INT / FLOAT / DOUBLE）
    - 实情：PointTypeEnum 实际有 8 个值：STRING(0)/BYTE(1)/SHORT(2)/INT(3)/LONG(4)/FLOAT(5)/DOUBLE(6)/BOOLEAN(7)。页面只列出
      4 个并以括号枚举形式呈现为该枚举的全部取值，遗漏了 BYTE/SHORT/LONG/BOOLEAN，会误导读者以为只有这 4 种数据类型可选。
    - 证据：dc3-common/dc3-common-constant/src/main/java/io/github/pnoker/common/enums/PointTypeEnum.java:41-76

## operation/data-commands.md  [major]

**事实错误：**

- 原文：轮询结果用 `GET /command_history/get_by_record_id?recordId=...`（页面第 30、106、179 行及轮询 curl）
    - 实情：`/command_history/*` 是【自定义设备级命令】(CommandCallVO, dc3.e.command 命名空间) 的接口，权限码
      command_history:get。位号级命令(本页主题)的轮询端点是 `GET /point_command_history/get_by_command_id?commandId=...`
      ，权限码 point_command_history:get，返回 PointCommandHistoryVO。页面把两套接口张冠李戴——这正是页面自己在第 167-169 行
      info 框警告不要混用的错误。
    -
  证据：dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/controller/PointCommandHistoryController.java:
  68-80 (GetMapping("/get_by_command_id"), @perm.can('point_command_history','get'))；对比
  CommandHistoryController.java:100-113 (get_by_record_id 属 command_history)；DataConstant.java:46,64 (两个独立前缀
  /point_command_history 与 /command_history)
- 原文：轮询返回 `CommandHistoryVO`，关键字段 `executeStatus`、`executeResult`、`executeTime`、`expireTime`（页面第 112 行及第
  114-124 行 JSON）
    - 实情：位号命令轮询返回的是 PointCommandHistoryVO，且不存在 executeStatus/executeResult/executeTime 这三个字段名（全仓库
      grep 无匹配）。真实字段为 status（不是 executeStatus）、responseValue（不是 executeResult）、finishTime（不是
      executeTime）、requestValue、expireTime、type、source。页面 JSON 示例里的 executeStatus/executeResult/executeTime
      均为虚构字段名。
    - 证据：dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/entity/vo/PointCommandHistoryVO.java:
      58,73,76,79,99,103 (commandId/requestValue/responseValue/status/finishTime/expireTime)；grep
      executeStatus|executeResult|executeTime --include=*.java 全仓库 0 匹配
- 原文：每条值含 `value`、`valueTime`；JSON 示例 `{"deviceId":1001,"pointId":2001,"value":"26.5","valueTime":"..."}`（页面第
  38、64、71 行）
    - 实情：PointValueVO 没有 value 字段也没有 valueTime 字段。真实字段为 rawValue、calValue、numValue，以及时间戳
      createTime/operateTime。页面自己在第 78 行 warning 又正确写了 raw_value/cal_value，与 JSON 示例里的 value/valueTime
      自相矛盾。
    - 证据：dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/entity/vo/PointValueVO.java:
      55,61,67,73,79,104,111 (deviceId/pointId/rawValue/calValue/numValue/createTime/operateTime；无 value/valueTime)
- 原文：请求体用顶层 `current`、`size` 分页，并用 `startTime`/`endTime` 查历史区间（页面第 49、57-59 行 curl 及第 64 行正文「多
  startTime/endTime 两个区间字段」）
    - 实情：PointValueQuery 没有顶层 current/size（分页嵌在 page:Pages 内），也没有 startTime/endTime 字段。时间过滤字段是
      createTimeFrom、rangeHours、rangeKey(today/24h/7d/30d)。curl 示例发送的 current/size/startTime/endTime 都不在请求契约里。
    - 证据：dc3-common/dc3-common-repository/src/main/java/io/github/pnoker/common/entity/query/PointValueQuery.java:
      58,102,110,119 (page:Pages, createTimeFrom, rangeHours, rangeKey；无 current/size/startTime/endTime 顶层字段)
- 原文：「终态共七种」（页面第 152 行）
    - 实情：终态实为六种：SUCCESS(2)、FAILED(3)、TIMEOUT(4)、EXPIRED(5)、DEAD(6)、DUPLICATE(7)。PointCommandStatusEnum 共 8
      个值，PENDING(0)/SENT(1) 为过程态，余 6 个为终态。页面正文随后列举的也恰是 6 个，「七种」与自身列举矛盾，应为「六种」。
    - 证据：dc3-common/dc3-common-constant/src/main/java/io/github/pnoker/common/enums/PointCommandStatusEnum.java:
      41-76 (PENDING0/SENT1 过程态 + SUCCESS2/FAILED3/TIMEOUT4/EXPIRED5/DEAD6/DUPLICATE7 共6终态)

**链接问题：**

- 页面第 30 行 mermaid 与第 106 行 curl 把位号命令轮询链接/调用指向 `GET /command_history/get_by_record_id`，实际应为
  `/point_command_history/get_by_command_id`（查询参数也应是 commandId 而非 recordId）——指向了错误的端点目标

## operation/alarms.md  [major]

**事实错误：**

- 原文：实操 curl 请求体: "alarmSourceFlag": 0, "alarmTargetTypeFlag": 1（lines 159-164），并在 tip 称三视图"只需切换
  alarmTargetTypeFlag（2/1/0）"（lines 194-196）
    - 实情：POST /dashboard/alert/page 的请求体 AlertPageQuery 根本没有 alarmSourceFlag/alarmTargetTypeFlag 字段。其字段为
      source(String: driver/device/point)、alarmTypeFlag(Integer)、confirmFlag、rangeHours、rangeKey、current、size。三视图的区分参数是
      source 字符串（driver/device/point），不是 alarmTargetTypeFlag 整数。Controller 实际只取 q.getSource()
      /q.getAlarmTypeFlag()/q.getConfirmFlag()。页面给的请求会被忽略，复刻不出三视图。
    - 证据：dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/entity/query/AlertPageQuery.java:
      50-80 (字段定义);
      dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/controller/DashboardController.java:
      312-322 (绑定 source/alarmTypeFlag/confirmFlag)
- 原文：响应形态示例 records[0] 含字段
  alarmSourceFlag、alarmTargetTypeFlag、alarmTypeFlag、alarmLevelFlag、deviceId、ruleId、ruleStateId、confirmFlag、createTime（lines
  175-186）
    - 实情：真实响应 VO 是 AlertItemVO，字段只有 id、source(String)
      、sourceId、pointId、alarmTypeFlag、confirmFlag、createTime、message。页面虚构了
      alarmSourceFlag/alarmTargetTypeFlag/alarmLevelFlag/deviceId/ruleId/ruleStateId 全部不存在于响应中，且漏掉了真实存在的
      source/sourceId/message。
    - 证据：dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/entity/vo/dashboard/AlertItemVO.java:
      51-76; 装配见 DashboardServiceImpl.java:159-168
- 原文：响应示例顶层 "code": 200（line 169）
    - 实情：R 包装类的 code 是 String，成功默认值为 "R200"（SuccessCode.OK.getCode()），不是整数 200。
    - 证据：dc3-common/dc3-common-public/src/main/java/io/github/pnoker/common/entity/R.java:57-58; SuccessCode.java:39
      OK(200, "R200", "Success")
- 原文：dc3_event_history 的 event_level_flag（0=normal/1=warning/2=severe/3=urgent）（line 143）
    - 实情：实际枚举 EventLevelEnum 取值为 0=LOW(low)、1=MEDIUM(medium)、2=HIGH(high)、3=CRITICAL(critical)，与页面写的
      normal/warning/severe/urgent 不符。
    - 证据：dc3-common/dc3-common-constant/src/main/java/io/github/pnoker/common/enums/EventLevelEnum.java:41-56

**契约问题：**

- 实操（查询与确认）小节整段示例不是真实路径合约：请求体字段、响应 JSON 字段、顶层 code 全部与源码不符（见 factErrors
  2/3/4），违反契约 §3（响应给真实 JSON 形态，字段来自 facts-pack/源码）与 §7（禁止占位符/编造示例）。tip 容器进一步把不存在的
  alarmTargetTypeFlag 当作可切换参数误导读者。
- 触发到送达 mermaid 与正文 line 117 称 "通知经 RabbitMQ 异步落 dc3_notify_history 再投递渠道"、Ins2 标注 "
  dc3_notify_history INSERT（异步 via RabbitMQ）"，与实现顺序相反：RuleNotificationServiceImpl 先在事务内同步
  persistPendingHistory() 落 pending 历史，再由 NotifyTaskSender 经 RabbitMQ 发布 NotifyTaskDTO 投递、回写 status。落库不是异步经
  RabbitMQ 的，异步的是投递与状态回写。措辞需纠正以免误导（实现见 RuleNotificationServiceImpl.java:
  137-152、NotifyTaskSender.java:47-59）。

## operation/agentic.md  [major]

**事实错误：**

- 原文：工具表「代表方法」列：TenantTool getTenant() / UserTool getUser() / DeviceTool getDevice()/listDevices() /
  DriverTool getDriver()/listDrivers() / ProfileTool getProfile()/listProfiles() / PointTool getPoint()/listPoints() /
  PointValueTool getLatestValue()/getHistory() / CommandTool getCommand()/listCommands() / EventTool getEvent()
  /listEvents()
    - 实情：这些方法名几乎全部不存在。真实方法：TenantTool.getCurrentTenantInfo()；UserTool.getCurrentUserProfile()
      ；DeviceTool.lookupDeviceById()/lookupDevicesByIds()/searchDevices()/listDevicesByDriverId()
      /listDevicesByProfileId()/getDeviceLatestPointValues()/getDeviceStatusesByIds()；DriverTool.lookupDriverById()
      /searchDrivers()/getDriverDeviceStatusSummary()；ProfileTool.lookupProfileById()/searchProfiles()
      /listProfilesByDeviceId()；PointTool.lookupPointById()/searchPoints()/listPointsByDeviceId()
      ；PointValueTool.getLatestPointValue()/getPointValueHistory()/readPointValue()/writePointValue()
      ；CommandTool.lookupCommandById()/searchCommands()/listCommandsByDeviceId()/listCommandsByProfileId()
      ；EventTool.lookupEventById()/searchEvents()/listEventsByDeviceId()。仅 SystemTool.getSystemHealth() 与页面一致。
    -
  证据：dc3-common/dc3-common-agentic/src/main/java/io/github/pnoker/common/agentic/tools/{TenantTool,UserTool,DeviceTool,DriverTool,ProfileTool,PointTool,PointValueTool,CommandTool,EventTool}.java（各
  @Tool 方法签名）
- 原文：它们都遵循平台的 CRUD 动词约定：取单条用 getXxx、取集合用 listXxx。
    - 实情：实际工具方法主要用 lookup*（单条/批量按 ID）与 search*（分页查询）动词，并非统一 getXxx/listXxx，与该断言相反；例如查单条设备是
      lookupDeviceById() 而非 getDevice()，分页查询是 searchDevices() 而非 listDevices()。
    -
  证据：dc3-common/dc3-common-agentic/src/main/java/io/github/pnoker/common/agentic/tools/DeviceTool.java（lookupDeviceById/searchDevices/listDevicesByDriverId）
- 原文：sequenceDiagram 中 Chat->>Tool: 调用 PointValueTool.getLatestValue()
    - 实情：方法名错误，真实方法为 PointValueTool.getLatestPointValue()（@Tool 标注 "Get latest point value"）。
    - 证据：dc3-common/dc3-common-agentic/src/main/java/io/github/pnoker/common/agentic/tools/PointValueTool.java:65
- 原文：智能中心对高风险（HIGH）动作走两阶段确认……服务返回 CONFIRM_REQUIRED 和一个 confirmId，把这次调用挂起为「待确认」……携带
  confirmId 再发一次确认请求（§86 mermaid 返回 CONFIRM_REQUIRED + confirmId、§170-171）
    - 实情：Agentic 内的确认机制不是 CONFIRM_REQUIRED/confirmId，也不是按 riskLevel=HIGH 的通用风险策略门控。真实实现：唯一的写工具
      writePointValue 调用 actionService.createWritePointValueAction(...) 生成一个 Action（actionId，UUID），状态用
      AgenticActionStatusEnum.PENDING，过期为 now+10 分钟；用户经 POST /action/confirm 或 POST /action/reject（参数
      action_id）来确认/拒绝。CONFIRM_REQUIRED/confirmId/HIGH 风险门控/confirm-ttl=PT5M 属于另一子系统 MCP Gateway 的
      dc3_mcp_tool_confirmation（见 ../automation/mcp），页面把它误植到 Agentic 聊天链路。
    -
  证据：dc3-common/dc3-common-agentic/src/main/java/io/github/pnoker/common/agentic/service/impl/ActionServiceImpl.java:
  63-80,96,126；controller/ActionController.java:77,104-105,129-130；tools/PointValueTool.java:140-167
- 原文：每个工具/接口上的 x-dc3-ai 注解（riskLevel / destructive / idempotent /
  openWorld）目前是在源码里手工标注的……新增工具时需要同步维护这组元数据，否则它的风险等级会缺省。
    - 实情：Agentic 的 10 个工具方法上并没有 x-dc3-ai / riskLevel 注解。工具方法仅带 @AgenticToolMetadata(domain, title)
      ，该注解只有 domain() 和 title() 两个字段，没有任何风险级别字段。x-dc3-ai 扩展只出现在 Controller/REST 端点（如
      ChatController）的 @Operation 上，用于 OpenAPI/MCP 目录，并非「每个工具」上。说工具的「风险等级会缺省」无源码依据。
    -
  证据：dc3-common/dc3-common-agentic/src/main/java/io/github/pnoker/common/agentic/annotation/AgenticToolMetadata.java:
  33-37（仅 domain()/title()）；tools/ 目录下无任何 x-dc3-ai/riskLevel（grep 为空）

**契约问题：**

- §99 与 §89 mermaid 称智能中心写命令「落到数据中心的 POST /point_command/write」「下发 point_command/write」。实际 Agentic
  写命令经 PointCommandFacade（gRPC 实现 PointCommandGrpcFacade / 本地 PointCommandLocalFacade）下发，并非调用 HTTP POST
  /point_command/write 端点；该 HTTP 端点是数据中心 PointCommandController 面向 Web/CLI 的另一调用面。把
  Agentic→数据中心的事实锚点写成 HTTP 路径属轻度误导（终点命令平面相同，但传输面错）。建议：§4 诚实标注，明确 Agentic 走
  facade（gRPC）。
- 违反契约 §4/§7「把未实现/受开关或不存在的写成已支持」：两阶段确认章节把 MCP Gateway 的 CONFIRM_REQUIRED/confirmId/HIGH
  风险门控当成 Agentic 既有行为陈述，未区分两个子系统，读者据此调用 Agentic 接口会找不到 confirmId/CONFIRM_REQUIRED，应改述为
  actionId + POST /action/confirm/reject。

## development/driver-authoring.md  [major]

**事实错误：**

- 原文：整篇教程以 `cp -r dc3-driver/dc3-driver-virtual dc3-driver/dc3-driver-bacnet-ip` 派生“新驱动”，类名
  `BacnetIpDriverApplication` / `BacnetIpDriverCustomServiceImpl`、`code: BacnetIpDriver`、并教读者在 dc3-driver/pom.xml
  注册 `<module>dc3-driver-bacnet-ip</module>`。
    - 实情：dc3-driver-bacnet-ip 已是仓库内已发布的生产驱动模块（与示例同名同 code 同类名）。按教程照做会与既有模块冲突：`cp`
      目标目录已存在、pom 中已有该 module（重复声明）。示例应改用 28 个驱动中确实不存在的协议名，否则误导读者去“创建”一个已存在的驱动。
    - 证据：dc3-driver/pom.xml:181 `<module>dc3-driver-bacnet-ip</module>`；目录 dc3-driver/dc3-driver-bacnet-ip
      存在；dc3-driver/dc3-driver-bacnet-ip/src/main/resources/application.yml:22 `code: BacnetIpDriver`
      ；BacnetIpDriverApplication.java 与 service/impl/BacnetIpDriverCustomServiceImpl.java 均已存在
- 原文：::: danger 块称 `dc3.driver.code` “绑定了管理中心的驱动元数据和 RabbitMQ 路由——改了等于换了一个驱动类型，已接入的设备会全部失联”，并把
  RabbitMQ 路由归因于 code。
    - 实情：RabbitMQ 命令队列与 routing key 由 `dc3.driver.service`（getService()）构建，而非 `code`。`code` 仅作为
      driverCode 注册到管理中心（DriverRegisterServiceImpl.setDriverCode）。页面自身的标识表（行335）也把 RabbitMQ 路由正确归给
      `dc3.driver.service`，danger 块与之矛盾且事实错误。
    - 证据：dc3-common/.../driver/config/DriverTopicConfig.java:92,109 用
      `QUEUE_POINT_COMMAND_PREFIX + driverProperties.getService()` /
      `ROUTING_POINT_COMMAND_PREFIX + driverProperties.getService()`；DriverRegisterServiceImpl.java:76
      `setDriverCode(driverProperties.getCode())` 仅元数据注册
- 原文：::: warning enable 字段名：模板用的是 `enable`（非 `enabled`），“与源码 `DriverScheduleServiceImpl` 校验的属性一致”。
    - 实情：源码绑定的字段是 `enabled` 而非 `enable`：DriverScheduleServiceImpl 读取 `property.getRead().getEnabled()` /
      `getCustom().getEnabled()` / `deviceHealth.getEnabled()`，对应 DriverProperties 内 `private Boolean enabled`。Spring
      宽松绑定不会把 `enable` 映射到 `enabled`（属于不同属性名）。模板里写 `enable` 实际上不会绑定到 `enabled`
      ，文档却声称二者一致——理由陈述与源码相反。
    - 证据：DriverScheduleServiceImpl.java:79,90,69 `getEnabled()`；DriverProperties.java:266
      `private Boolean enabled = false;`、:211 `private Boolean enabled = true;`；virtual/application.yml 用
      `enable: true`
- 原文：冒烟示例 curl `POST /point_value/latest` 请求体为 `{"deviceId": 1, "pointId": 1, "current": 1, "size": 10}`，把
  current/size 置于顶层。
    - 实情：该端点入参类型为 `PointValueQuery`，分页字段在嵌套对象 `page`（Pages.current / Pages.size）下，顶层没有
      current/size 字段。顶层放 current/size 不会绑定到分页，会被忽略并回落默认值。正确形态应嵌套在 `page` 下。
    - 证据：PointValueController.java:82-83 `@PostMapping("/latest")` 入参 `PointValueQuery`；PointValueQuery.java:58
      `private Pages page;`（只有 deviceId/pointId/page，无顶层 current/size）；Pages.java:48,51 `current`/`size`

**契约问题：**

- §6/§4 诚实标注：attribute-type-flag 表（行183）把属性类型写成“`STRING` / `INT` / `DOUBLE` / `BOOLEAN`”，呈现为完整枚举，但
  AttributeTypeEnum 实有 8 个值（STRING/BYTE/SHORT/INT/LONG/FLOAT/DOUBLE/BOOLEAN，见 AttributeTypeEnum.java:
  41-76）。列出的四个值本身正确，但以斜杠枚举形式给出会让读者误以为只有这四种；建议标注“部分/常用”或补全。

## development/api-documentation.md  [major]

**事实错误：**

- 原文：登录握手中客户端本地计算 md5(md5(password) + salt) 再换 token（出现在导语 line 43 "用盐把密码哈希后换取 token"、时序图
  note line 53 "本地计算 md5(md5(password) + salt)"、curl 注释 line 74）
    - 实情：实际算法是单次 md5(password)，盐不参与密码哈希。dc3-cli/src/utils/crypto.ts 注释明写 "The server expects MD5(
      rawPassword) during token generation"；client.ts:139 与 200 发送 password: md5(password)，salt 作为独立字段单独传。盐仅用于
      token 签名（TokenServiceImpl.java:110 KeyUtil.generateToken(principalId, salt, tenantId)），后端用
      PasswordUtil.verify(rawPassword, hash)（Argon2id/bcrypt）校验，密码路径完全不含 salt。页面的 "双重 md5 + 拼盐" 公式是编造的。
    - 证据：/Users/pnoker/Code/pnoker/IoTDC3/github/dc3-cli/src/utils/crypto.ts:3-9;
      /Users/pnoker/Code/pnoker/IoTDC3/github/dc3-cli/src/core/client.ts:139,200;
      dc3-common/dc3-common-auth/src/main/java/io/github/pnoker/common/auth/service/impl/LocalCredentialServiceImpl.java:
      163-172; dc3-common/dc3-common-public/src/main/java/io/github/pnoker/common/utils/PasswordUtil.java:51-72;
      dc3-common/dc3-common-auth/src/main/java/io/github/pnoker/common/auth/biz/impl/TokenServiceImpl.java:110
- 原文：生产环境（pro profile）通过共享配置 application-web.yml 关闭 Swagger/OpenAPI 暴露（line 180-191，并把禁用 YAML 片段归到
  application-web.yml）
    - 实情：禁用动作不在共享 application-web.yml 里。application-web.yml 只设基线路径（path 等），其 line 74 的 enabled:false
      是 internal-signature-enabled 与 swagger 无关。真正的 springdoc.api-docs.enabled:false / swagger-ui.enabled:false
      分别落在每个中心自己的 application-pro.yml（auth/manager/data/agentic/single 各一份）。application-web.yml
      注释本身就写明 "Production disables these endpoints in each service's application-pro.yml."
    - 证据：dc3-common/dc3-common-web/src/main/resources/application-web.yml:57-63,74;
      dc3-center/dc3-center-auth/src/main/resources/application-pro.yml:30-34（manager/data/agentic/single 同构）

**契约问题：**

- §4/§7 诚实标注：line 142 "riskLevel 一般由动词语义推导——delete→HIGH, add/update→MEDIUM, get/list→LOW" 把人工注解约定写成了代码推导。实际
  riskLevel 取自每个 @Operation 上手写的 x-dc3-ai 注解（OAuthMcpRuntimeServiceImpl.applyQuality→normalizeRisk(
  quality.getRiskLevel())，空/非法才兜底 HIGH），并非由动词自动算出；只有 read_only_hint 是真正按 HTTP
  方法派生（GET→1/POST→0，applyQuality line 154）。dossier B.7 明确标注该元数据 "manually authored, not auto-generated"。"
  一般" 有所对冲，但与紧邻的 read_only_hint 真派生并列，易让读者误以为 riskLevel 也是机器推导。建议改为 "
  按动词语义约定人工标注"。

## development/testing.md  [major]

**事实错误：**

- 原文：覆盖率门禁小节（第 148 行）：「覆盖率较基线回退超过 1% 会阻断改动」
    - 实情：代码库中不存在任何「基线回退/1% delta」门禁。覆盖率检查脚本只对静态最低阈值（line≥0.20、branch≥0.15）做判定，没有任何
      baseline 跟踪、逐次提交 delta 或 1% 回退比较逻辑。check_coverage.py 的判定就是 `counter.ratio >= minimum`
      （仅静态阈值），低于阈值才失败。该「1% 回退」机制属虚构。
    - 证据：dc3-coverage/scripts/check_coverage.py:60-67,79-95（仅 --minimum-line/--minimum-branch，check() 实现为
      counter.ratio >= minimum，无 baseline/regression 参数）；dc3-coverage/pom.xml:41-42,229-234（只传
      minimum-line/minimum-branch）；.github/workflows/test.yml:103-147（coverage job 无回退比较步骤）；全仓 grep 无
      regression/baseline/1% 门禁命中

## development/changelog.md  [minor]

**契约问题：**

- §2/§7 缺图：本页讲解的是一条数据管线（git 提交 → dc3/bin/changelog.py → dc3/doc/CHANGE.md → VitePress @include 内联），正属于
  §2 规定应配 flowchart 的'数据流/管线'类型，且 §7 要求'每页至少一张图（除纯清单/社区页）'，而全页 mermaid 数量为
  0。这是一篇解释性页面而非纯清单页，应补一张 flowchart LR 展示生成链路。
- 措辞不完整（非事实错误）：第 30 行称'仅变更日志'固定 subject 为 docs(release): update generated
  changelog，且'生成器也会识别并跳过这类提交'。实际 dc3/bin/changelog.py:38 的 GENERATED_CHANGELOG_RE = ^(docs|chore)
  \(release\): update generated changelog$ 同时识别 docs(release): 与 chore(release):。页面对 docs(release): 的陈述为真但遗漏了
  chore(release): 也被识别跳过。
- 措辞不完整（非事实错误）：第 39-52 行类别表把 Security 的'来源提交类型'仅标为 security。实际 changelog.py:174-176
  还会把任意提交信息中含 security/vulnerability/cve/auth bypass 关键词的提交提升到 Security 类别。表格作为 type→category
  映射本身无误，但关键词提升这一行为未在正文/表中提及。

## automation/index.md  [major]

**事实错误：**

- 原文：::: info 已实现与尚未实现 callout 内称：工具目录的刷新机制（『仅按计划任务（默认 PT5M）与 dc3_api/dc3_resource
  提交后事件刷新』）属于已实现行为。
    - 实情：代码中不存在该自动刷新。McpToolCatalogChangedEvent 在整个 Java 源码中查无此符号；auth 模块无任何 @Scheduled /
      @EnableScheduling / @TransactionalEventListener；也无 dc3.mcp.tool.refresh-interval 配置（PT5M 默认值只存在于
      superpowers 设计稿与 factspack，未落地）。refreshToolCatalog() 仅由管理端点 POST /mcp/tool/catalog/refresh 手动触发——其
      OpenAPI 描述明确写『call after API registrations change so the catalog stays current』，即需手工调用。把『定时刷新 +
      提交后事件刷新』写成既成事实属事实错误，且违反契约 §4/§7（未实现写成已实现）。
    -
  证据：dc3-common/dc3-common-auth/src/main/java/io/github/pnoker/common/auth/biz/impl/OAuthMcpRuntimeServiceImpl.java:
  497-515 (refreshToolCatalog 仅 @Transactional，无调度/事件)
  ；dc3-common/dc3-common-auth/src/main/java/io/github/pnoker/common/auth/controller/McpManagementController.java:
  273-276 (唯一调用方为手动 POST /mcp/tool/catalog/refresh)；grep 全仓 McpToolCatalogChangedEvent 与 auth 模块
  @Scheduled/@EnableScheduling 均零命中

**契约问题：**

- §4/§7 违规（与上述 factError 同源）：line 103 的『已实现与尚未实现』info 容器把尚未实现的工具目录自动刷新（PT5M 定时 +
  dc3_api/dc3_resource 提交后事件）陈述为已实现的现状，应改为标注为『设计已规划、当前仅支持手动 POST
  /mcp/tool/catalog/refresh 刷新』。

## automation/cli.md  [major]

**事实错误：**

- 原文：退出码表把 `2` 网络错误（网关不可达）、`3` 鉴权错误（需要先登录）列为 dc3 的真实退出码，并给出
  `dc3 device list --ci || case $? in 3) ...;; 2) ...;; esac` 的 CI 分流示例，宣称『dc3 用退出码区分失败的性质』
    - 实情：CLI 源码只实现了退出码 0 与 1。所有错误（含网络不可达、AuthError/403、ApiError）最终都走 index.ts 顶层 catch 的
      `process.exit(1)`；不存在 NetworkError 类，AuthError/ApiError 也不携带 2/3 退出码，printAndExit 仅会退出 0 或显式传入的
      1。页面给出的 `case $? in 3) ... 2) ...` 分支永远不会命中。这是把未实现的契约写成了可用行为（违反契约 §4/§7）。
    - 证据：dc3-cli/src/index.ts:47 (catch → process.exit(1)); dc3-cli/src/utils/format.ts:112-115 (printAndExit 仅
      exitCode 默认0); dc3-cli/src/core/client.ts:266-296 (仅 AuthError/ApiError，无退出码映射、无 NetworkError)；全仓 grep
      `process.exit\|exitCode` 无 2/3 路径

**契约问题：**

- 缺少 dossier 图表清单中为 automation/cli 规定的第三张图 D41『CLI command architecture (classDiagram)』(entry → 14
  modules → client/config/token/credential)。页面只含 D40(鉴权时序图) 与 D42(凭据解析 flowchart)。硬性『每页≥1
  图』已满足，故仅为清单缺图的次要问题（dossier §E D40/D41/D42）。

## automation/mcp.md  [major]

**事实错误：**

- 原文：目录如何刷新：聚合在两种时机刷新：定时（`dc3.mcp.tool.refresh-interval`，默认 `PT5M`）+ 事件驱动（`dc3_api` /
  `dc3_resource` 变更提交后触发 `McpToolCatalogChangedEvent`，`AFTER_COMMIT`）。所以新增接口后，工具目录最迟 5
  分钟内自动更新，无需手工注册。（mcp.md:33-35）
    - 实情：代码中不存在 `dc3.mcp.tool.refresh-interval` 配置项、不存在任何 `@Scheduled`
      刷新（dc3-common-auth/dc3-center-auth 全模块 0 个 @Scheduled）、也不存在 `McpToolCatalogChangedEvent` 或任何
      AFTER_COMMIT/TransactionalEventListener。工具目录唯一的刷新入口是管理员手动调用的 HTTP 端点 `refreshToolCatalog()`
      （McpManagementController.refreshToolCatalog → OAuthMcpRuntimeServiceImpl.refreshToolCatalog）。所谓‘定时 +
      事件驱动自动刷新、5 分钟内自动更新、无需手工注册’整段是虚构的。
    -
  证据：dc3-common/dc3-common-auth/src/main/java/io/github/pnoker/common/auth/controller/McpManagementController.java:
  274-275（手动刷新端点）；dc3-common/dc3-common-auth/src/main/java/io/github/pnoker/common/auth/biz/impl/OAuthMcpRuntimeServiceImpl.java:
  497-516（refreshToolCatalog 实现）；全仓 `grep -rn refresh-interval|McpToolCatalogChangedEvent` 无任何源码命中；
  `grep -rln @Scheduled dc3-common/dc3-common-auth/src/main` 为空
- 原文：工具的风险等级由动词语义推导，不靠人工打标：`delete` → HIGH；`add`/`update` → MEDIUM；`get`/`list` → LOW（mcp.md:
  25-31）
    - 实情：riskLevel 不是从动词推导的，而是每个接口在 `@Extension(name="x-dc3-ai")` 中逐条手工标注的，且
      resource-registrar 在扫描期强制校验该注解必须存在且 riskLevel 合法（缺失即报缺陷）。聚合器/服务只是原样读取注解里的
      riskLevel（quality.getRiskLevel()），仅在注解整体缺失时才回退为保守的 HIGH。证伪反例：`POST /point_command/write` 被手工标为
      HIGH（destructive=true），并非按‘write/update→MEDIUM’的规则。所以‘不靠人工打标’与给出的动词→风险映射均不成立。（注：同一节里‘read_only_hint
      由 GET→1/POST→0 推导’这一句是对的。）
    - 证据：dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/controller/PointCommandController.java:
      93-99（/write riskLevel=HIGH 手工标注）、68-74（/read
      riskLevel=LOW）；dc3-common/dc3-common-auth/src/main/java/io/github/pnoker/common/auth/tool/McpOpenApiAggregator.java:
      138,154（riskLevel 直接读自
      x-dc3-ai）；dc3-common/dc3-common-auth/src/main/java/io/github/pnoker/common/auth/biz/impl/OAuthMcpRuntimeServiceImpl.java:
      154-157,164（注解缺失才默认
      HIGH）；dc3-common/dc3-common-resource-registrar/src/main/java/io/github/pnoker/common/resource/registrar/scan/ApiAnnotationValidator.java:
      85,90（强制校验 x-dc3-ai 与 riskLevel）
- 原文：tool_id 格式 `{service_name}:{HTTP_METHOD}:{api_path}`，示例 `manager:POST:/device/add`、
  `data:POST:/point_command/write`、`data:POST:/point_value/latest`；正文亦用 `data:POST:/point_command/write`（mcp.md:
  17-23, 52）
    - 实情：tool_id 等于 dc3_api.api_code，其 service 段是完整服务名 `dc3-center-<x>`（来自
      spring.application.name=@project.artifactId@，如 dc3-center-manager），不是裸短名 manager/data。真实形如
      `dc3-center-manager:POST:/device/add`、`dc3-center-data:POST:/point_value/latest`。页面所有 tool_id 示例都漏掉了
      `dc3-center-` 前缀。
    - 证据：dc3-common/dc3-common-auth/src/main/java/io/github/pnoker/common/auth/tool/McpOpenApiAggregator.java:
      47（api_code 示例 `dc3-center-manager:POST:/device/add`
      ）、62,176（SERVICE_PREFIX=dc3-center-）；dc3-common/dc3-common-auth/src/main/java/io/github/pnoker/common/auth/biz/impl/ResourceRegistrySyncServiceImpl.java:
      78-79（apiCode=serviceName:METHOD:
      path）；dc3-common/dc3-common-resource-registrar/src/main/java/io/github/pnoker/common/resource/registrar/ResourceRegistrar.java:
      111-117（serviceName 取 spring.application.name）；dc3-center/dc3-center-manager/src/main/resources/application.yml:
      22-23（name=@project.artifactId@，模块 artifactId=dc3-center-manager）

**契约问题：**

- 访问控制小节用 `data:POST:/point_command/write` 作为‘默认可见、只差白名单/scope/权限’的示例工具（mcp.md:44 mermaid 注释、52
  正文），但该工具实为 HIGH 风险（destructive=true），按本页自己的规则（mcp.md:47,54-56‘HIGH 默认隐藏、需 mcp:tools:call:high +
  两阶段确认’）它本应默认不可见。拿一个 HIGH
  工具当低门槛示例与本页其它论述自相矛盾，会误导读者以为写命令是普通可见工具。证据：PointCommandController.java:
  93-99。建议改用真正的 LOW 工具（如 /point_value/latest 或 /point_command/read）做‘默认可见’示例。
- 契约 §4/§7 要求‘不得把未实现写成已支持’。mcp.md:33-35
  把并不存在的‘定时+事件驱动自动刷新’当作已实现机制正面陈述（甚至放进 ::: tip），违反诚实标注；实际只有手动管理端点。

## guide/index.md  [major]

**事实错误：**

- 原文：::: tip 启动顺序 —— 分布式起栈时按 Gateway → Auth → Manager → Data → Agentic → Driver 顺序启动（line 73）
    - 实情：Gateway 启动在最后，不在最前。docker-compose.yml 中 gateway 的 depends_on 为 auth/manager/data/agentic（均
      service_healthy），即四个中心健康后 Gateway 才启动；auth 无依赖最先起。实际健康门控顺序是 Auth → Manager → Data →
      Agentic → Gateway → Driver（与 dossier B.1 一致）。页面把 Gateway 排在首位与真实依赖图相反。
    - 证据：iot-dc3/dc3/docker-compose.yml:83-91 (gateway depends_on auth/manager/data/agentic); :133-136 (
      manager→auth); :158-162 (data→auth+manager); :184-190 (agentic→auth+manager+data); :222-224 (driver→manager);
      dossier B.1 docs/superpowers/specs/2026-06-22-docs-overhaul-dossier.md:31

**契约问题：**

- 契约 §4 诚实标注/反 AI-ish 准确性：页面 line 34-36 用 ::: info 标注可观测性页『本页建设中…正在补全』，line 97
  延伸阅读再注『（建设中）』。但 docs/guide/observability.md 已是 8.4KB 的完整页（含组件职责、端口、Kibana/Grafana 接入、env
  调参等小节，非 stub）。把已完成页面标为『建设中』属于失实/误导的内部状态描述，应移除该 info 容器与『（建设中）』后缀。

## guide/usage.md  [minor]

**契约问题：**

- L67 danger 框: "dev 栈为方便调试会额外发布网关 8000 与各中心端口（8300/8400/8500/8600 及对应 gRPC）" — 措辞暗示四个中心都有对应
  gRPC 发布端口，但 agentic（8600）在 docker-compose-dev.yml 中只发布 HTTP 8600，没有 gRPC 端口（agentic 无 gRPC server，只有
  auth/manager/data 发布 9300/9400/9500，见 docker-compose-dev.yml:191-192 vs
  92-94/123-125/156-158）。属轻微过度概括，非硬错误，建议改为"及 auth/manager/data 对应的 gRPC（9300/9400/9500）"。

## guide/logging.md  [major]

**事实错误：**

- 原文：整个『MDC：让一条链路可被串起来』节 + flowchart 的 MDC 节点 + tip：『在请求入口把 traceId、tenantId、userId 放进 MDC
  后，本线程后续每一条日志都会自动带上这些字段』『traceId/tenantId 已由 MDC 自动注入 JSON 输出』，并断言 traceId
  贯穿网关→数据中心→驱动可按 traceId 一搜聚齐
    - 实情：代码库中完全不存在 MDC 写入——全仓 grep 无任何 MDC.put、无 org.slf4j.MDC import。logback.xml 第56行确有 <mdc/>
      provider，但没有任何过滤器/拦截器/AOP 把 traceId/tenantId/userId 放进 MDC，所以 JSON 输出里 mdc 字段实际为空。仓库里唯一的
      traceId 是 MCP 网关审计用的随机 UUID（McpGatewayController.java:234），与日志 MDC 链路关联无关。本节把『未实现』写成了『已实现』。
    - 证据：全仓搜索：MDC.put 0 处命中；org.slf4j.MDC import 0 处命中；traceId 仅出现在
      dc3-common-gateway/.../mcp/McpGatewayController.java:234（MCP 审计 UUID）与 McpConstant.java:293。logback.xml mdc
      provider 在 dc3-common/dc3-common-log/src/main/resources/logback.xml:56
- 原文：@Logs 注解示例：`@Logs(title = "Add Driver", type = LogsType.OpLog)`
    - 实情：@Logs 注解没有 title 成员（成员是 value()），type 的类型是 LogsTypeEnum 而非 LogsType，且 LogsTypeEnum 只有
      INFO/WARN/DEBUG/ERROR 四个值，根本不存在 OpLog。真实用法形如
      `@Logs(value = "warn-resource", type = LogsTypeEnum.WARN, tag = "resource", save = true)`。示例中 title= 与
      LogsType.OpLog 均为虚构。
    - 证据：Logs.java:47-68 (value/type=LogsTypeEnum/tag/save 四个成员，无 title)；LogsTypeEnum.java:39 (
      `INFO, WARN, DEBUG, ERROR`)；真实用法 LogsAspectTest.java:107，均在
      dc3-common/dc3-common-log/src/.../io/github/pnoker/common/annotation/

**契约问题：**

- §4 诚实标注违规：MDC 自动注入 traceId/tenantId/userId
  是代码库中完全未实现的能力，却被当作已实现正常陈述（甚至作为整页核心卖点之一与专门小节 + 图示节点 + tip
  容器）。按契约应以 ::: info 明示『设计已规划、尚未实现』，或删除。
- §5 事实编造：@Logs 示例的 title= 入参与 LogsType.OpLog 枚举值均非源码真实 API（属臆造的注解成员/枚举值），违反『写前就近核对源码、不照搬不臆测』。
- 误导性示例：页面把 @Logs 注解挂在真实存在的 DriverController.add(Mono<R<String>>) 方法上当作活跃模式呈现，但全仓生产代码
  0 处使用 @Logs（仅 LogsAspectTest 测试里用过），DriverController.add 本身也无 @Logs。读者会以为这是平台审计日志的现行约定。

## guide/troubleshooting.md  [major]

**事实错误：**

- 原文：整节 “pre/pro profile 报 Nacos 错误”：症状=用 pre/pro profile 启动时报 Nacos 相关连接错误；根因=pre/pro
  面向注册中心部署，通常期望 Nacos 可用；本地通常没有起 Nacos。
    - 实情：整个平台没有任何 Nacos：源码中 *.xml/*.yml/*.yaml/*.java（排除 target）对 'nacos' 的搜索零命中，没有 Nacos
      依赖、没有注册中心配置。pre/pro profile 实际用的是静态 gRPC 地址（如 static://${CENTER_AUTH_HOST:dc3-center-auth}:
      9300）指向容器主机名，以及 POSTGRES_HOST 默认 dc3-postgres、RABBITMQ_HOST 默认 dc3-rabbitmq。本地用 pre/pro
      失败的真实根因是这些容器主机名解析不到 + HMAC fail-fast，而非 Nacos。这是凭空捏造的故障类别，会把读者引向不存在的组件。
    - 证据：dc3-center/dc3-center-manager/src/main/resources/application-pre.yml:28-44（datasource
      host=dc3-postgres、rabbitmq host=dc3-rabbitmq、grpc channels address=static://dc3-center-auth:9300）；全仓 grep -rln
      -i 'nacos' --include=*.xml/*.yml/*.yaml/*.java（排除 target）零命中

**契约问题：**

- 违反契约 §5（事实来源与核验，不得编造）与 §F.8（Troubleshooting 解释真实根因）：‘pre/pro profile 报 Nacos
  错误’小节描述了一个源码中根本不存在的组件（Nacos）作为故障根因。应删除或改写为真实根因——pre/pro
  默认连接容器主机名（dc3-postgres / dc3-rabbitmq / dc3-center-*）且 HMAC 在 pre/pro 为空或默认值时 fail-fast。

## community/contributing.md  [major]

**事实错误：**

- 原文：提交前装好 `commit-msg` 校验钩子，本地即拦截不合规格式：```bash\nmake install-hooks\n``` （第95-99行）；并在 warning
  中称"钩子会在 git commit 时校验 subject"（第101-103行）
    - 实情：Makefile 中根本不存在 install-hooks 目标，dc3/bin/ 下也没有任何安装 git hook 的脚本或机制。Makefile 的 .PHONY
      列表（Makefile:21-23）为 help/env/.../changelog/openapi/tag/validate-annotations，无 install-hooks；动态 pattern
      目标只覆盖 <op>-<stack> 形式。执行 `make install-hooks` 会直接报 'No rule to make target'。该命令所声称的"
      本地即拦截不合规格式"的钩子安装不会发生。（注：同样的错误也存在于上游 CONTRIBUTING.md:69、AGENTS.md:
      162/499，属仓库既有问题，但本页仍把一个会失败的命令当作可用命令呈现。）
    - 证据：iot-dc3/Makefile:21-23 (.PHONY 全量列表无 install-hooks); 全仓 grep 'install-hooks' 仅命中文档(
      CONTRIBUTING.md/AGENTS.md)，无任何 Makefile 目标或脚本; dc3/bin/ 仅含
      audit_controller_permissions.py/changelog.py/export_openapi.sh/tag.sh

## community/security.md  [major]

**事实错误：**

- 原文：受支持的版本表只列 `2025.9.x`✅、`2025.6.x`✅、`2025.x.x`✅ 三行，称这些是『当前活跃维护的主线版本』，并用 `2025.9.x`
  作年月方案示例。
    - 实情：项目当前实际版本是 2026.5.22（pom.xml dc3.version），镜像 tag 为 2026.6，最新发布线是 2026.5.x（CHANGE.md 顶部为
      2026.5.17，后续 bump 到 2026.5.18/2026.5.22）。表里把当前活跃主线 2026.5.x（及 2026.4.x）完全漏掉，却把更老的 2025
      线列为受支持——与本页『只为当前活跃维护的主线提供安全补丁』自相矛盾，等于告诉用户当前发行线不受支持。这是一份从旧
      GitHub SECURITY.md 套来、未随 2026 版本方案更新的过期表。
    - 证据：/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/pom.xml:24,154 (version 2026.5.22);
      /Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/.env.example:10 (DC3_IMAGE_TAG=2026.6);
      /Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3/doc/CHANGE.md:3 (### 📌 2026.5.17)

**契约问题：**

- 『其余配置项见环境变量详解』段（line 78）把交叉引用的小节名写成『HTTP Gateway & Service Ports』与『gRPC Inter-service』，但目标页
  quickstart/environment.md 实际小节标题是『网关与服务端口 / HTTP Gateway & 服务端口』(line 160/313) 与『gRPC / facade』(
  line 147/299)——这两个英文名来自 factspack 内部 env-catalog 表头，并非已发布文档的真实章节标题，读者照名定位会找不到。
