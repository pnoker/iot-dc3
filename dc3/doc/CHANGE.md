# ✨ What's Changed

### 📌 2026.5.17

_Generated on 2026-05-09._

#### Summary

- Generated from `dc3.release.20251005.00` to `HEAD`.
- Included 244 commits across 10 categories: Security 3, Features 60, Bug Fixes 73, Performance 1, Refactoring 45,
  Documentation 24, Build 1, CI 1, Chores 34, Other 2.
- Most active scopes: dashboard(17), auth(12), data(8), agentic(6), config(5), env(5).
- Highlights: add security quality checks; security: enforce gateway user and command tenant scope; Revise SECURITY.md
  with updated support and practices; web: add health probes to shared web configuration; config: add AUTH_HMAC_SECRET
  to all deployment configurations.

#### Security

- add security quality checks (`2197454be`)
- **security**: enforce gateway user and command tenant scope (`37f86b18f`)
- Revise SECURITY.md with updated support and practices (`86e4d563e`)

#### Features

- **web**: add health probes to shared web configuration (`363fc14a1`)
- **config**: add AUTH_HMAC_SECRET to all deployment configurations (`edaaab90d`)
- introduce TenantOwned interface for type-safe tenant filtering (`a08f15904`)
- **release**: generate changelog from git commits (`8c7a2b024`)
- **auth**: HMAC-sign X-Auth-Principal header end to end (`085966276`)
- **facade,data**: add selectByIds bulk lookup and use it in dashboard live feed (`1fd58e688`)
- **agentic**: add AI agentic center with Spring AI tool-calling (`239334c4b`)
- **facade**: add PointValue facade with gRPC and local implementations (`8a30c9d13`)
- **api**: expand point_value.proto with HistoryValue, ReadCommand, and WriteCommand RPCs (`f941c5613`)
- **manager-dashboard**: four-column topology Sankey endpoint (`ceb4c1c30`)
- **data-dashboard**: add phase-2 insight endpoints (`24d7f5cf0`)
- **manager-dashboard**: expose driver counts grouped by service_name (`5b5b2c3e2`)
- **auth**: add listRoleByResourceId for the resource-detail role tab (`1ae91d2c5`)
- **auth**: role tree endpoint and listUserByRoleId for the role detail/edit UX (`66dab218f`)
- **auth**: add listResourceByUserId for the user-detail resource tab (`9f9945c6d`)
- **auth**: scope user list to the caller's tenant (`a71bb80b6`)
- **auth**: record creator/operator on user/role/resource/menu writes (`1bd75d709`)
- **dashboard**: alarm diagnostic endpoints (activity / type / storm) (`74f26073e`)
- **dashboard**: resolve driver/device/point names for live feed (`c5061e96b`)
- **menu**: support multilingual menu titles in MenuExt.content (`6269f8563`)
- **api**: propagate rangeKey time-range preset to dashboard endpoints (`d17b2d1db`)
- **dashboard**: add alert trend + top-sources endpoints (`4f46ab667`)
- **data**: add time range filtering for alerts and point values (`8261b2f54`)
- **dashboard**: add today-scoped alarm counts and total data volume (`88fd2a6c9`)
- **alert**: split alert stats by device/driver source + code reorg (`17b78384f`)
- **event**: stop logging heartbeats, derive OFFLINE on TTL expiry (`2d7942897`)
- **dashboard**: tenant-scope health + device summary + undo/bulk confirm (`e820a2544`)
- **dashboard**: paged alert list + confirm endpoint (`db32adebc`)
- **dashboard**: sparkline data for every stat card (`e9b970c9a`)
- **dashboard**: aggregate /system/health endpoint (`8eb7e6a3e`)
- **dashboard**: add latency histogram + hourly activity heatmap (`def6776f8`)
- **event**: persist device/driver events + add alarm API (`277076f35`)
- **dashboard**: tenant-scope alert queries now that event tables carry tenant_id (`5f86d3bb7`)
- **dashboard**: expose alert stats + latest alerts endpoints (`2436275d6`)
- **manager**: expose driver/device breakdown endpoints for dashboard (`ada738c82`)
- **data**: add dashboard aggregate endpoints for home page (`c1a25d823`)
- **auth**: expose MenuController and mirror menus into dc3_resource (`af43ce379`)
- **auth**: assemble resource tree via service + apiGroup grouping nodes (`b1e004bfd`)
- **auth**: add api_group to cluster endpoints by owning controller (`2ae1c56ab`)
- add dc3-center-auth gRPC channel to dc3-center-data configs (`892cc96c7`)
- add API management endpoint and enhance auth query capabilities (`1ce24015b`)
- auto-register center API endpoints to auth resource tables (`0dc9295a8`)
- enhance query conditions to check for tenantId and add Resource, Role, and RoleUser controllers (`225916cd2`)
- update technology stack to Spring Boot 4.0.6 and remove Redis dependency in favor of LocalCacheService (`3d1d7cc7b`)
- add gRPC client configuration and exception handling for ResponseStatusException (`f523ceeaf`)
- add TimestamptzLocalDateTimeTypeHandler for PostgreSQL TIMESTAMPTZ support (`213de8607`)
- add .env file for Docker Compose configuration and update usage instructions (`cde90d463`)
- **gateway**: migrate auth filter chain to Facade and trim dependencies (`9392b4d5f`)
- **data**: migrate data module from @GrpcClient to Facade (`c4a74f10a`)
- **facade**: introduce facade layer for cross-service calls (`7615eb9be`)
- add .env file for Docker Compose configuration and update usage instructions (`625ef7b30`)
- add detailed JavaDoc comments to driver implementations and entry points (`8e7a437cc`)
- add JavaDoc comments and enhance field naming consistency across project (`5013ca096`)
- . (`66361055f`)
- add comment (`f206c4300`)
- . (`5ea9f22d0`)
- . (`719fdd900`)
- . (`c728c075f`)
- . (`03d785603`)
- update pom (`f9e7f89ac`)

#### Bug Fixes

- **agentic**: migrate ObjectMapper to Jackson 3.x (tools.jackson) (`db780bb6f`)
- **deps**: replace jakarta.validation-api with spring-boot-starter-validation (`d7eec55d1`)
- **config**: use webflux base paths (`16fc57bb4`)
- **agentic**: remove servlet context path (`69d623276`)
- **release**: preserve changelog idempotency across dates (`203a9bc16`)
- **release**: trim changelog commit records (`e5e95e658`)
- **release**: keep changelog generation idempotent (`0a21864f4`)
- **config**: harden configuration properties (`11a95e5ee`)
- **container**: include agentic compose service (`d73c8b79f`)
- **agentic**: scope sessions and tools to tenant user (`1c87edbfc`)
- **auth**: enforce tenant membership in user flows (`3900feb09`)
- **driver,grpc**: scope metadata lookups by ownership (`860f85383`)
- **data**: validate tenant scope for status and point values (`6f155af1c`)
- **manager**: enforce tenant scope on resources (`c76e46046`)
- **facade**: add tenant-scoped lookup helpers (`6b759b6c2`)
- **facade,manager**: harden grpc calls and add bulk lookups (`6dce6009b`)
- **facade**: preserve unset enable flag in grpc queries (`9da685f60`)
- **facade,data**: preserve point value tenant scope (`757ee4ec6`)
- **common**: harden thread pool conversion and stats (`d1b97c715`)
- **exception,auth**: support placeholders and reset password once (`17b1956e9`)
- **rabbitmq**: acknowledge messages after processing (`612eb21f8`)
- **modbus-tcp**: restore abstract keyword on 18 vendored modbus4j classes (`56c11700e`)
- **common-public**: fix bugs and clean up unused dependencies (`c25021ff5`)
- **build**: remove requireEncoding rule incompatible with Maven 3.x (`aedf96e46`)
- **driver**: stop heartbeat alarm storm from ttl < cadence mismatch (`aacc20b7e`)
- **grpc**: implement missing ManagerPointServer.selectById (`0b71286e7`)
- **alert**: correct ALARM event_type_flag from 2 to 1 (`9eaf953c2`)
- **alert**: stat queries count only ALARM events (event_type_flag = 2) (`8cc65c9b6`)
- **alert**: remove event_type_flag filter from alert stat queries (`beae4f812`)
- **alert**: stat aggregates also exclude HEARTBEAT (`2a5230a07`)
- **dashboard**: Recent Alerts show message + drop HEARTBEAT rows (`68828093e`)
- **dashboard**: qualify time_bucket with the public schema (`c3121dc6f`)
- **data**: include public in history DS search_path (`559c78434`)
- **dashboard**: drop tenantId IS NULL branch from trend queries (`463444f36`)
- **dashboard**: inline bucket interval + move manager SQL to XML (`47eb3a09f`)
- **dashboard**: wrap UNION subqueries + cast bucket interval explicitly (`119e5fa84`)
- **dal**: set autoResultMap=true on remaining DOs with JsonExt typeHandler (`7413b48ef`)
- **auth**: set autoResultMap=true so JacksonTypeHandler runs on SELECT (`65e7854e1`)
- **menu**: flatten menuExt.content into top-level tree VO fields (`b192201aa`)
- **gateway**: route /api/v3/auth/menu/** to auth service (`a8a443ced`)
- **tenant**: enforce scoped auth writes (`689754513`)
- **gateway**: fix YAML indentation for data_route in application-gateway.yml (`78da5ccb0`)
- **gateway**: clean up dead code, invalid config, and improve error handling (`cc501a4b0`)
- make tenantId query condition null-safe across all fuzzyQuery methods (`5bf8df67d`)
- **data**: carry driverId + tenantId end-to-end across point value pipeline (`1199cd5fd`)
- change driver RabbitMQ queues to durable (`4faf780e7`)
- add null safety for status enum and convert to code string in event services (`25b224b80`)
- replace @GrpcService with @Service annotation in server classes and update pom.xml for Spring gRPC (`fb935f17a`)
- add WARN level logging for gRPC server and reactor.netty in logback configuration (`3dba6c090`)
- replace gRPC client annotations with @Resource in facade classes (`d5aaa1219`)
- update gRPC client configuration and refactor client annotations (`db5c8229b`)
- remove gRPC server dependency from pom.xml (`44d2a843f`)
- update gRPC client configuration structure in application YAML files (`e0b8dabc7`)
- update gRPC client configuration structure in application YAML files (`f279f6e5f`)
- update gRPC client configuration structure in application YAML files (`23e826a8c`)
- restructure gRPC configuration under Spring application settings in YAML files (`b3667903e`)
- move gRPC server configuration under Spring application settings in application.yml (`36a6caed0`)
- downgrade JDK version from 25 to 21 in Dockerfile and related configurations (`08457f915`)
- downgrade JRE version in Dockerfile from 25 to 21 (`152578849`)
- update Mybatis and Dynamic Datasource dependencies to version 4, remove Redis exporter from docker-compose files (
  `297c9ce6d`)
- remove Redis module and dependency from pom.xml (`7dc5faaac`)
- remove Redis configuration from application profiles (`73fe31c82`)
- remove Redis configuration from application profiles (`6183042ce`)
- remove Redis configuration from application profiles (`d578b1121`)
- remove Redis configuration from application profiles (`92dfc77e9`)
- update Docker images to version 2026.5 across all docker-compose files (`dd4e6b34c`)
- exclude .mvn directory from dockerignore to allow Maven configuration (`44489157e`)
- correct JAVA_OPS syntax and update Maven command in Dockerfile (`3e078cf79`)
- revert proto package to api.common.* to fix build breakage (`5491d15f9`)
- complete dc3-api proto optimization (`04e8d29bb`)
- optimize dc3-api pom element order and proto files (`352fece66`)
- remove invalid .* glob from @ComponentScan basePackages (`079dbbf25`)
- standardize comment language in POM files (`0bd4e2e96`)

#### Performance

- **manager**: batch driver-registration attribute diff into 3 round-trips (`c07d873a3`)

#### Refactoring

- **constant**: extract BaseConstant to break circular dependency on dc3-common-exception (`8db13631c`)
- **config**: use per-service port variables from .env.example (`8ad47b12c`)
- replace @Value with @ConfigurationProperties beans (`b0094f69b`)
- **grpc**: add explicit imports for batch query types (`5a33280b2`)
- **agentic**: centralize service constants (`b011d8843`)
- **log**: standardize core logging conventions (`4cb6c7b8e`)
- **agentic**: tighten chat orchestration boundaries (`0787be15b`)
- **i18n**: standardize user-facing text in english (`033b64e90`)
- **config**: standardize dc3 property prefixes (`4fd6ca33f`)
- **container**: remove redundant podman scripts (`d9c68ea9b`)
- **container**: deduplicate aliyun compose stacks (`303629e71`)
- **web**: introduce BaseController.async() and migrate controllers off try/catch (`e65e94d86`)
- **common**: replace per-builder MapStruct page mapping with PageUtil.copyPage (`d38df7148`)
- **manager-dashboard**: return typed Row DTOs from mappers (`2c7e4495b`)
- **data-dashboard**: return typed Row DTOs from mappers (`a83ee65ab`)
- **manager-dashboard**: hoist topology caps into TopologyLimits (`6ed561efe`)
- **data-dashboard**: hoist clamp caps into DashboardLimits (`7876a8f54`)
- simplify imports and reorganize methods in service and entity classes (`6fcea0979`)
- **mapper**: move dashboard/alert SQL from @Select to XML (`98bbc2231`)
- **menu**: drop redundant flat menu fields now that menuExt returns (`4291b1b72`)
- **auth**: drop tenant from registry resources (`88cc124ab`)
- change @Component to @Configuration for auto-registered classes (`6a6ca4cea`)
- **gateway**: extract shared routes and gRPC config to application-gateway.yml (`c88244b46`)
- restructure facade module to aggregator pom, split local into auth/manager submodules (`e486b922d`)
- **data**: swap PointValueJob buffer outside the write lock (`332eb16c5`)
- **data**: remove dead driver/device run timing feature (`20006ccfa`)
- remove explicit Netty dependency from listening-virtual and add field comments (`35aecaf0c`)
- remove Netty dependency and unused transport classes from dc3-common-api (`a5256c88a`)
- replace Redis service with LocalCacheService for in-process caching (`14a9a5746`)
- **manager**: route TenantApi call through Facade (`831a3df36`)
- optimize dc3-gateway and dc3-common-gateway (`cb5e6d452`)
- optimize root, dc3-common and dc3-api pom.xml (`b1f42ba5c`)
- replace `NioEventLoopGroup` with `MultiThreadIoEventLoopGroup` to utilize `NioIoHandler` factory in UDP and TCP Netty
  servers (`75225a53f`)
- replace deprecated `EnvironmentPostProcessor` imports and update `JsonMapper` references (`381414548`)
- **proto**: 优化proto文件注释和结构 (`342fb4df1`)
- standardize and translate comments across driver services, implementations, and application files for clarity and
  consistency (`80d1ef4d6`)
- standardize and translate comments across Protobuf files and environment script for clarity and consistency (
  `313308b8d`)
- standardize and translate comments in Protobuf files for improved clarity and consistency across modules (`d6d918ec8`)
- standardize and translate comments across driver modules for consistency and clarity (`782572484`)
- translate and standardize comments in configuration files across all driver modules for consistency (`47dd8f37f`)
- standardize and translate comments across Protobuf files, Java services, and configuration classes for consistency and
  clarity (`a6f91d24d`)
- remove unused files, standardize code comments and documentation across modules (`2893a84db`)
- standardize descriptions, comments, and documentation across POM files and Protobuf definitions (`0cb5ab320`)
- update Java docs, README files, and Protobuf comments for consistent language and formatting (`65d48101b`)
- standardize protobuf annotations and document translations (`4bc31532e`)

#### Documentation

- align markdown tables and wrap long lines (`8b6b407a0`)
- align markdown tables and wrap long lines (`811c0b721`)
- **agent**: add shared coding agent guidance (`a1a8cee2d`)
- **env**: document environment variable reference (`086ade9d8`)
- align governance and environment guides (`aa65b330c`)
- **env**: align runtime environment variables (`1c615b194`)
- **env**: complete runtime variable examples (`a88627805`)
- consolidate project documentation under dc3 (`2db218cfb`)
- add driver-authoring guide (`d1a016a8c`)
- **common-public**: simplify verbose class-level Javadocs to concise one-liners (`4a17fcd7f`)
- add module overview, quickstart, and troubleshooting guides (`106fcecda`)
- improve Javadoc in dc3-common-driver metadata, gRPC, event, job, and utility classes (`e4e2a6031`)
- improve Javadoc in dc3-common-driver service interfaces and implementations (`17dc4bbb0`)
- improve Javadoc in dc3-common-driver configuration and entity classes (`c256902da`)
- fix incorrect comments and improve Javadoc in S7 PLC API classes (`a500299b6`)
- refresh CLAUDE.md and pin Claude's commit identity (`ccefc179c`)
- Enhance documentation for various classes and methods (`e2025a09c`)
- 完善MQTT和PostgreSQL相关类的文档注释 (`47472b968`)
- 更新工具类和配置类的文档注释 (`5c0669cc7`)
- 更新代码注释和文档说明 (`190e34d65`)
- **web**: 完善Java类文档注释，增加英文描述和详细说明 (`b00136414`)
- **utils**: 更新工具类文档注释为英文并补充详细说明 (`24c75ec69`)
- **application**: 更新应用类注释以提供更清晰的描述 (`f7779cd45`)
- 更新多个驱动模块的类注释文档 (`8a2d9829c`)

#### Build

- **git**: add commit message validation hook (`35377c628`)

#### CI

- harden github workflows (`d797a253b`)

#### Chores

- **java**: sort imports alphabetically (`b5ea88eb9`)
- **java**: expand wildcard imports to explicit imports (`421804a5f`)
- **githooks**: add AGPL license header to commit-msg hook (`7524905ae`)
- add AI coding assistants compatibility entrypoint (`000426cbf`)
- **java**: expand wildcard imports to explicit imports (`5180ac734`)
- remove CLAUDE.md files (`fe2d2bf8c`)
- **mqtt**: remove hardcoded paho version, inherit from parent dependencyManagement (`eff7d4b3b`)
- reorganize POM dependencies with consistent category comments (`0e916774d`)
- remove duplicate java.version property (inherited from dc3-parent) (`69252d890`)
- remove format and format-check targets from Makefile (`6a223a851`)
- apply spring-javaformat 0.0.47 and fix checkstyle violations (`24edb3e0b`)
- **build**: upgrade spring-javaformat to 0.0.47 and add Checkstyle integration (`868d287e0`)
- **build**: add format and format-check targets to Makefile (`99bec4364`)
- **build**: add enforcer plugin and Spring Java Format validation (`7411c61ed`)
- **build**: tune Maven JVM settings and enable parallel builds (`9aecc3e07`)
- **env**: update .env and .env.example for clarity and usage instructions (`ff96a784f`)
- stop tracking .env file from version control (`fbda40ca6`)
- add .claude/TODO.md to .gitignore (`97bddb3d4`)
- **env**: update .env and .env.example for clarity and usage instructions (`ab26dce87`)
- bump version to 2026.5.18 (`605bcc836`)
- relicense source headers under AGPL 3.0 (`783343ff1`)
- **data-dashboard**: apply IDE auto-format (`51468e532`)
- add blank line between constants and ctor in TimeRangeUtil (`7d0588a7f`)
- collapse imports and tidy TimeRangeUtil javadoc (`eb1c75be9`)
- **dashboard**: log driver summary internals for 0/0 diagnosis (`bddc87958`)
- remove unused Nacos, eKuiper and LoadBalancer dead config (`8eef9e855`)
- clean up import statements in data and resource-registrar modules (`cdbacede6`)
- update version to 2026.4.30 in multiple files (`2b2a65b52`)
- add .claude/settings.local.json to .gitignore (`57c79b9b4`)
- remove dangling Javadoc and clean up driver service comments (`0964062af`)
- update version to 2026.4.19 across all modules and adjust branch settings in codeql.yml (`6a4321923`)
- fix inceptionYear and scm connection in all pom.xml (`339cdd304`)
- bump version to 2026.4.18.3 and optimize pom.xml dependencies (`44393212d`)
- bump version to 2026.4.18.3 and optimize pom.xml dependencies (`e13d75cbd`)

#### Other Changes

- Revert "fix(data): include public in history DS search_path" (`b1e1b0609`)
- . (`bb12374ff`)

<details>
<summary>📝 Historical Version Description, Click to Expand</summary>

### 📌 2025.10.5

_Generated on 2026-08-19._

#### Summary
- Generated from `f40016fb7` to `bf64fd7fd`.
- Included 3 commits across 2 categories: Features 2, Documentation 1.
- Highlights: update docker-ci.yml; update version.

#### Features
- update docker-ci.yml (`bf64fd7fd`)
- update version (`5afd4c904`)

#### Documentation
- 添加日语和越南语README文档并更新语言切换链接 (`40e1bd233`)

### 📌 2025.9.13

_Generated on 2026-08-19._

#### Summary
- Generated from `07a2a15b0` to `f40016fb7`.
- Included 73 commits across 4 categories: Features 10, Refactoring 1, CI 1, Other 61.
- Most active scopes: 数据存储(1), 存储(1).
- Highlights: .; 存储: 添加PostgreSQL存储服务实现; .; .; ..

#### Features
- . (`f8346c180`)
- **存储**: 添加PostgreSQL存储服务实现 (`563610e31`)
- . (`11811cc25`)
- . (`b8b1b444a`)
- . (`6c17edd3d`)
- add example usage to Docker CI workflow output (`5992c3b4c`)
- feat: add release notes generation to Docker CI workflow (`ed263d3d8`)
- . (`44142686e`)
- feat: enhance Docker CI workflow with detailed logging and summaries (`a97ee0914`)
- . (`8ae1f4aa6`)

#### Refactoring
- **数据存储**: 重构点值数据存储逻辑，移除原始时间字段 (`003148399`)

#### CI
- 将Docker CI工作流的触发标签从dc3.develop.*改为dc3.release.* (`200e69976`)

#### Other Changes
- . (`f40016fb7`)
- . (`d03c6d327`)
- . (`140f0dcc9`)
- . (`777185e06`)
- . (`a877fcf82`)
- . (`7323ab746`)
- . (`b244b1132`)
- . (`f004ce521`)
- . (`fd3d01b1a`)
- remove hutool (`0acaf7be0`)
- Create codeql.yml (`3b778e9cd`)
- . (`84f33ac9a`)
- . (`1b79c4310`)
- . (`42205e87e`)
- . (`f723765c8`)
- . (`be04ea7ab`)
- . (`d48f6454e`)
- . (`e86425f1a`)
- . (`49d2834ad`)
- . (`69013d836`)
- . (`0ab5c14da`)
- . (`11d609180`)
- . (`c4478851b`)
- . (`fa817d179`)
- . (`b67b2367e`)
- . (`87ea293d7`)
- . (`28c66ec8f`)
- . (`362095ec0`)
- . (`4e3629df4`)
- Update TITLE.md (`3bf43ccf5`)
- Update TITLE.md (`1a2f832e1`)
- Update TITLE.md (`351815baa`)
- Create TITLE.md (`2fccca3b1`)
- Update docker-ci.yml (`59225c274`)
- Update CHANGE.md (`029c91221`)
- Update CHANGE.md (`61cb5808e`)
- Update USAGE.md (`eea5a7997`)
- Update CHANGE.md (`fb42942b0`)
- Create CHANGE.md (`01219cd50`)
- Update docker-ci.yml (`3707cc0ad`)
- Create USAGE.md (`48d0c410f`)
- Delete LICENSE-ZH.txt (`b28133b00`)
- Create CODE_OF_CONDUCT.md (`2d9e4f242`)
- Update CONTRIBUTING.md (`e0728984a`)
- Create CONTRIBUTING.md (`27b741655`)
- . (`a222ac166`)
- . (`a796ef9d3`)
- . (`df2d32723`)
- . (`be4ea3869`)
- . (`d0f034f00`)
- . (`dfc9d3309`)
- . (`2ceeeaea1`)
- . (`b4904be10`)
- . (`6e17da1f0`)
- Update LICENSE.zh.md (`fc4ce3113`)
- Update and rename LICENSE to LICENSE.zh.md (`52f90db57`)
- Update README.zh.md (`4020103a7`)
- Update README.md (`4bcaee8c1`)
- cn readme (`716997630`)
- cn readme (`82d90e590`)
- cn readme (`ed7f5a848`)

### 📌 2025.9.3

- Dependency upgrades
- Add Japanese and Vietnamese introductions

### 2025.9.2

- Dependency upgrades
- Stability improvements
- Remove Hutool
- Remove Undertow
- Update container image dependencies

### 📌 2025.6.29

_Generated on 2026-08-19._

#### Summary
- Generated from `a08ad84ce` to `07a2a15b0`.
- Included 73 commits across 9 categories: Features 25, Bug Fixes 4, Performance 1, Refactoring 18, Documentation 2, Build 3, CI 10, Chores 6, Other 4.
- Most active scopes: docker(5), Dockerfile(4), config(2), docker-ci(2), dc3-common-auth(1), data(1).
- Highlights: update; 调整; 调整; 调整; ..

#### Features
- update (`0db69725d`)
- 调整 (`7102a84de`)
- 调整 (`2afa57984`)
- 调整 (`4b69f0b2e`)
- . (`d8a010c41`)
- . (`1d170be4e`)
- . (`f1f6e78f3`)
- . (`ac20257dc`)
- . (`ed400b170`)
- . (`e53a2d35a`)
- remove mongo module (`b79504cce`)
- . (`62ed730c2`)
- . (`54c376d0d`)
- . (`334f05547`)
- . (`c61b810a9`)
- . (`5147677da`)
- . (`edbc8936e`)
- . (`a2389c163`)
- . (`b4321b045`)
- 调整时间类型 (`1b0dd81d8`)
- 调整表结构 (`4db89d7d4`)
- 在Dockerfile和docker-compose文件中添加APM相关配置 (`18b0fb079`)
- 调整日志 (`bafbc3e13`)
- 调整类 (`e654ff7e5`)
- **docker**: add redis-exporter service to docker-compose (`8a81ed6a9`)

#### Bug Fixes
- correct file paths and add GC logging configuration (`39cbf3879`)
- 修正测试环境数据库连接URL中的数据库名称 (`1ec1e2d60`)
- 修正Dockerfile中ENTRYPOINT的路径问题 (`05b40b226`)
- **docker**: correct COPY command in Dockerfile (`538a3d4f3`)

#### Performance
- 将JVM最大内存从1024m调整为512m (`fcaa8fab4`)

#### Refactoring
- **dc3-common-auth**: 移除不必要的Redis依赖 (`82e9a8157`)
- replace LocalDateTime.now() with LocalDateTimeUtil.now() for consistency (`9455aa281`)
- update APM service port to 9300 and remove exposed port 8200 (`897072945`)
- **Dockerfile**: reorder JAVA_OPS flags for consistency (`16f1bc40e`)
- **config**: 更新测试环境配置文件以优化模块配置 (`bf0472fab`)
- **config**: 更新测试环境配置文件以支持更多服务 (`3a4af6d2e`)
- 使用环境变量替换硬编码的URI (`41d6a3897`)
- 移除Dockerfile中的${PARAMS}参数 (`41a9f3ba0`)
- **Dockerfile**: 修改多个Dockerfile以使用entrypoint.sh (`2a93dcff8`)
- **Dockerfile**: remove GC logging configuration to simplify container startup (`c5072a5fb`)
- improve code formatting and configuration (`8f94beb9d`)
- **Dockerfile**: reorder commands for better logical flow (`a1a011e4e`)
- **data**: restructure MongoDB configuration and repository service (`aefd0c9dc`)
- **data-storage**: replace InfluxDB with MongoDB for point value storage (`c9d275de8`)
- remove TDEngine-related files and configurations (`131922849`)
- simplify COPY paths in Dockerfiles and fix CI workflow (`63ecedea4`)
- **docker**: simplify Dockerfiles and CI workflow by removing builder stage and optimizing build process (`79d8a4e84`)
- **Dockerfiles**: simplify Dockerfile configurations and CI workflows (`11bcec762`)

#### Documentation
- 添加代码注释并改进文档字符串 (`07a2a15b0`)
- update module descriptions in pom.xml files for clarity (`30fd92df0`)

#### Build
- add logstash-logback dependency and update logging configuration (`27a54faa7`)
- remove local build config and add monitoring dependencies (`87d85791b`)
- remove redundant repository configurations from pom.xml (`e557ea6e3`)

#### CI
- **docker**: update tag trigger from dc3.release.* to dc3.develop.* (`fa9f033b4`)
- add docker-compose files for Elasticsearch setup (`aebebfc6c`)
- add Redis service to docker-compose files (`575759200`)
- update Dockerfile paths in CI workflow for multi-service builds (`c727174c4`)
- fix incorrect variable reference in docker-ci.yml (`b20120e2a`)
- **docker-ci**: update JDK setup and fix version variable references (`4507e2668`)
- remove build cache configuration from docker-ci.yml (`5141bf4bc`)
- **docker-ci**: reorder and update CI workflow steps (`c7fdc2fec`)
- remove outdated Docker CI workflows for various services (`5721dc4d8`)
- **gateway**: add docker-ci-gateway.yml and update Dockerfile (`6684102a4`)

#### Chores
- add podman scripts for aliyun services and update logback config (`bf9a1b233`)
- 将所有Dockerfile和docker-compose文件中的NODE_ENV从dev更改为test (`a00817659`)
- add prometheus endpoint to management configuration (`9a3c58e27`)
- **docker**: standardize string quotes and add new compose files (`4495d279a`)
- update version to 2025.2.5 across multiple files (`9be2f5f0d`)
- update project version to 2025.2.4 across all modules (`24faae2fd`)

#### Other Changes
- . (`b43b2f979`)
- Revert "feat: 调整时间类型" (`721a642e3`)
- . (`7bdb0fbbc`)
- . (`6ee211945`)

### 2025.6.6

- Dependency upgrades for enhanced performance
- Stability improvements and bug fixes
- System reliability enhancements
- Core components optimization

### 2025.6.5

- Updated dependencies to latest versions
- Fixed stability issues
- Performance optimizations
- Security patches applied

### 📌 2025.3.10

_Generated on 2026-08-19._

#### Summary
- Generated from `1b2e52aa4` to `a08ad84ce`.
- Included 13 commits across 2 categories: Features 11, Other 2.
- Highlights: 版本信息; 版本信息; 版本信息; 版本信息; 版本信息.

#### Features
- 版本信息 (`5bc7e9a07`)
- 版本信息 (`25d2614da`)
- 版本信息 (`e55f4c593`)
- 版本信息 (`6549bff8e`)
- 版本信息 (`1a5bb878f`)
- . (`05087a932`)
- . (`2c1baa789`)
- version 2024.3.10 (`42e60f1b7`)
- . (`4486fe9b3`)
- . (`20df53f96`)
- . (`18787291e`)

#### Other Changes
- . (`a08ad84ce`)
- . (`6a29baae4`)

### 📌 2024.12.29

_Generated on 2026-08-19._

#### Summary
- Generated from `a706baa07` to `1b2e52aa4`.
- Included 34 commits across 3 categories: Features 32, Bug Fixes 1, Other 1.
- Highlights: .; .; .; .; ..

#### Features
- . (`1b2e52aa4`)
- . (`756e5e202`)
- . (`9ded01982`)
- . (`91f3c314e`)
- . (`265fcfe0b`)
- adjust database connection parameters and add `&stringtype=unspecified` (`84fdb5fc7`)
- adjust builder and permission encoding logic (`85370fd4d`)
- . (`981c6fe2b`)
- . (`4a668711c`)
- . (`21d54e249`)
- . (`765b9aaa6`)
- . (`e0b6c9042`)
- . (`9eda1ec34`)
- . (`747557e48`)
- . (`992d1a39f`)
- . (`5ad0d6a6c`)
- data postgresql (`8014fa6a0`)
- manager postgresql (`4d62e6d13`)
- auth postgresql (`50160796e`)
- . (`83634a6a2`)
- . (`81786744e`)
- . (`fbd0ce12f`)
- . (`d52fdb0da`)
- update mybatis generator (`355f54b60`)
- update email (`167615e20`)
- . (`fb45e9ab1`)
- . (`672409604`)
- . (`4ac50abab`)
- . (`0d41d0bcb`)
- 代码调整，独立模块，合并模块都能运行 (`1f9bae4a9`)
- update pom version (`fc350befa`)
- 更新springboot版本 (`af68b5da2`)

#### Bug Fixes
- auth postgresql (`562a1faaa`)

#### Other Changes
- . (`3beccb234`)

### 📌 2024.8.31

_Generated on 2026-08-19._

#### Summary
- Generated from `7d8d18750` to `a706baa07`.
- Included 6 commits across 1 categories: Features 6.
- Highlights: .; update; fix limiter loading issue; .; 提取gateway代码到common模块中.

#### Features
- . (`a706baa07`)
- update (`927b6f07c`)
- fix limiter loading issue (`5b1368600`)
- . (`67fae60e7`)
- 提取gateway代码到common模块中 (`948d049e0`)
- 合并common和api代码到主项目 (`3ae07be80`)

### 📌 2024.6.24

_Generated on 2026-08-19._

#### Summary
- Generated from `0b481ff63` to `7d8d18750`.
- Included 117 commits across 3 categories: Features 84, Bug Fixes 14, Other 19.
- Highlights: common 代码优化; 提取manager公共代码到common中; 调整data服务通用代码为common模块; 调整通用模块; 调整auth服务通用代码为common模块.

#### Features
- common 代码优化 (`7d8d18750`)
- 提取manager公共代码到common中 (`8efc1605e`)
- 调整data服务通用代码为common模块 (`4f3d98a3f`)
- 调整通用模块 (`968b7b1b0`)
- 调整auth服务通用代码为common模块 (`2ba670d73`)
- config makefile (`e774e3553`)
- . (`5ec1cc689`)
- . (`d956709a0`)
- 修复依赖问题 (`45b39cd1b`)
- 2024.3.2 (`21dca0a5a`)
- . (`4a4e2ae5f`)
- 调整缓存文件位置 (`e69403753`)
- 调整缓存文件位置 (`003c98e7c`)
- . (`939b5a1ed`)
- . (`f73b8ad11`)
- . (`522b2264e`)
- . (`09283a57d`)
- 批量导入优化 (`ead953ed0`)
- . (`4844ee37c`)
- 代码优化 (`c5a8bc557`)
- 代码优化 (`4dc8226ab`)
- 代码优化 (`897b6f47b`)
- 代码优化 (`1303bec3b`)
- 优化grpc数字默认为0的问题 (`2acb56292`)
- . (`b46d843da`)
- 优化通知机制 (`d8b583f83`)
- 清除设备连接缓存 (`7770d760b`)
- add device & point metadata update event (`8ea7a4493`)
- 优化 (`775496448`)
- 优化驱动注册 (`ec278136e`)
- 优化驱动设备列表 (`6999ede64`)
- . (`9d9ece525`)
- 调整 JAVA_OPS (`f3ff0cbf3`)
- . (`007931f23`)
- 调整依赖不兼容问题 (`5b3c1b2c7`)
- . (`a36ca2e98`)
- . (`0fc938bde`)
- . (`7a685d8eb`)
- 调整驱动配置通知逻辑，去掉模版，配置的单独通知逻辑 (`3ff57d383`)
- 1.切换驱动设备，位号，驱动配置，位号配置同步机制 2.支持按照驱动类型是否一次性加载配置到缓存 3.支持分页加载配置缓存 (`b3ea1fe7f`)
- 代码优化 (`c6ee2c61d`)
- 代码优化 (`490996182`)
- 1.调整驱动数据同步模式 2.调整驱动注册模式 3.驱动本地添加数据缓存机制 (`13761340a`)
- 1.添加驱动本地缓存 2.调整gRpc桩代码格式 (`6282caf90`)
- 1.优化驱动读任务逻辑； 2.优化采集值处理工具类； (`0288346cb`)
- 调整驱动工具类型 (`4e882f048`)
- 1.Fail fast if transport is null, check for null on close 2.Log exceptions when error occurs on send (`c555316d8`)
- 修复一个分页查询 Bug (`6532681a4`)
- 升级 (`d370a66b9`)
- 升级 dc3-parent 到 2024.1.3，请在下个月再进行升级 (`5e75af049`)
- 1.拆分位号数据历史接口；2.调试和优化设备历史曲线 (`a63fa1a03`)
- 兼容性调整 (`7997d382d`)
- springboot3 (`214d82ed9`)
- . (`6d873e7ec`)
- . (`a62550145`)
- . (`f0eafea1b`)
- . (`b16213a47`)
- Modbus TCP 驱动test环境采集频率调整 (`0b2e2c538`)
- 正式切换至JDK17 (`026f44cc8`)
- . (`803da68f5`)
- . (`6dbc33704`)
- . (`b24189360`)
- . (`869a16c4a`)
- . (`f1441bf02`)
- . (`c14739622`)
- . (`623186e19`)
- . (`f80dfeeaf`)
- . (`d2d079021`)
- . (`e42690757`)
- . (`627f140a5`)
- . (`c783e6a26`)
- . (`d8ac2f52b`)
- . (`93c9def27`)
- 自动化脚本 (`eb4b95ae8`)
- . (`2f5fa1165`)
- . (`a0ed7f91b`)
- JDK17兼容调整 (`40f1fb144`)
- . (`f14996ea2`)
- . (`20fd1bb59`)
- 代码规范化 (`4d8764cf1`)
- 代码规范化 (`ca638d311`)
- rabbitmq controller (`ba8f90046`)
- . (`9a2debd49`)
- . (`2d2ef8431`)

#### Bug Fixes
- updateTime to operateTime (`af8ff341f`)
- 调整api gateway默认的filter (`babbee6e7`)
- 修复api网关代理问题 (`7080d8353`)
- 修复接口404错误 (`17cd06868`)
- . (`9a7096af6`)
- 优化通知机制 (`a043302d6`)
- 🪲调整程序内日志和错误打印位英文，防止乱码 (`be953b1d6`)
- . (`fcbb2ef7d`)
- add copyright (`410d1ba34`)
- 移除警告 (`65f07cef8`)
- 修复设备启停导致设备模板丢失，影响驱动采集。 (`3bb05b80f`)
- . (`581080cd7`)
- . (`4d70e2cdb`)
- . (`6ca5ab733`)

#### Other Changes
- Create docker-publish.yml (`70ecf4ebe`)
- feat:修改查询 (`bf51660f8`)
- feat:优化 (`570832293`)
- feta:修改 (`0bdd418a7`)
- feat:修改代码 (`96a042094`)
- feat:优化 (`19483c97c`)
- feat:修改topic接口 (`68d5ccb0d`)
- feat:新增topic接口 (`a3623c1a9`)
- feat:索引越界bug修复 (`872e9b6bb`)
- feat:设备驱动位号看板部分接口调整 (`66a861748`)
- feat:新增dc3-center-ekuiper (`6d42e0971`)
- feat:索引越界bug修复 (`f0197839f`)
- feat:设备驱动位号看板部分接口bug修改 (`87994d19b`)
- feat:多数据源tdengine,influxDB部分方法完成 (`5782b1be8`)
- feat:将rabbitmq的代码独立到common模块 (`f8cead6d3`)
- feat:将连接Promethues的接口API进行修改 (`a49941154`)
- feat:调整调用超时时间和连接超时时间为60s (`efcd0a1ef`)
- feat:调整读取超时时间为60s (`13064219f`)
- Create FUNDING.yml (`ce7fd6a0a`)

### 📌 2024.3.31

_Generated on 2026-08-19._

#### Summary
- Generated from `69327e826` to `0b481ff63`.
- Included 76 commits across 3 categories: Features 53, Bug Fixes 5, Other 18.
- Highlights: 多数据源接口定义; 暂时去掉登录限制，20240327; 多数据源框架; 格式化代码; 优化.

#### Features
- 多数据源接口定义 (`27f190120`)
- 暂时去掉登录限制，20240327 (`36e163d0b`)
- 多数据源框架 (`c7ad54b64`)
- 格式化代码 (`e5f8c3d6a`)
- 优化 (`dd190cd43`)
- . (`0ae50b0bf`)
- . (`a313dcb1e`)
- forward data to mqtt (`14f723f95`)
- 枚举调整 (`74c03613a`)
- 枚举调整 (`b1f99fd90`)
- 枚举调整 (`150a79e71`)
- OpenApi注解 (`5d9c48576`)
- OpenApi 接口例子 (`aaa2ad77c`)
- OpenApi 接口例子 (`cc18d86d6`)
- 调整使能状态枚举类的逻辑，后续涉及的枚举需要按此规律全替换 (`fe5360ca7`)
- . (`ef9db2e89`)
- . (`031746d8f`)
- 调整 (`7a8685b49`)
- 调整 (`8c618bcc3`)
- 调整 (`d4b64d37c`)
- 调整 (`f6f3ce40e`)
- 调整 (`1f299ac50`)
- 添加调度任务，在 DATA 和 MANAGER 服务中 (`7cd591d7f`)
- 测试环境调整为单机模式，移除多数据源的配置 (`039314596`)
- 优化 (`687eaad1a`)
- 优化 (`6138cbeda`)
- 优化 (`412531d34`)
- 调度 (`c8196ade4`)
- 优化 Dockerfile (`85d31d21d`)
- 优化&配置统一的JSON序列化反序列化处理 (`5c92f7c03`)
- 设备历史数据接口适配 (`750f2eff3`)
- 兼容性修复 (`830b2d9e0`)
- 设备、驱动状态解析和查询 (`92698221f`)
- 修复权限模块兼容性问题 (`51185ccb9`)
- 修复RabbitMQ消息序列化的问题 (`6580da1b6`)
- 注册中心日志位置调整 (`2dde35911`)
- 修复BUG，适配前端接口 (`1986430e9`)
- 适配 (`15fc15c09`)
- 代码优化，统一bean转换 (`2215a415e`)
- 调整统一的租户获取方式 (`b67b44feb`)
- . (`585df3883`)
- 调整 (`994b54a66`)
- 调整 (`165d2f228`)
- 。 (`89381ed4b`)
- 更新 (`68764d92b`)
- 更新 (`3c35594d3`)
- 调整 驱动 SDK 到 common 模块中，简化开发依赖配置 (`5fd0bf240`)
- . (`211f5ed57`)
- . (`305bbc684`)
- . (`38c42a726`)
- . (`f4e909af1`)
- . (`48892a2ec`)
- 调整各种O&检测属性的完整性 (`7e96340fa`)

#### Bug Fixes
- . (`0b481ff63`)
- 优化 (`b60b2185c`)
- 优化 (`80d2b1d78`)
- 优化 (`6a1dc9a78`)
- 修复多数据源接口依赖，启动问题 (`6f1078730`)

#### Other Changes
- feat:驱动位号接口调整,备注修改,格式化 (`344e72810`)
- feat:修改接口url和http请求为 okhttpclient,并加入身份信息 (`9a60ebd7b`)
- feat:VO类名称bug (`8b646835f`)
- feat:折线图位号接口补充完成 (`a69ca0e59`)
- feat:格式化代码 (`94d32d5a6`)
- feat:位号图表接口完成 (`686eec7a2`)
- feat:驱动设备在线时长代码优化,位号部分接口调整完成. (`8f0070b7b`)
- feat:新增消息总线的所有接口 (`0123bfee8`)
- feat:grpc调用,新增设备数量接口 (`65898bd14`)
- feat:驱动设备在线统计时长返回参数调整,更改定时任务为每小时执行一次 (`a910c806f`)
- feat:位号看板增加位号数据总量接口 (`e3bbdd17f`)
- feat:驱动部分接口实现 (`a320d824a`)
- feat:位号数据看板接口优化注释 (`b38c421b8`)
- feat:位号数据看板接口优化 (`65d185f6b`)
- feat:位号数据看板接口功能实现 (`571445c34`)
- feat:驱动设备在线时长统计调度任务功能实现 (`00577a175`)
- feat:优化 (`9a5646cef`)
- 位号看板功能实现 (`027e8f5a0`)

### 📌 2023.12.31

_Generated on 2026-08-19._

#### Summary
- Generated from `bcab3e546` to `69327e826`.
- Included 59 commits across 1 categories: Features 59.
- Highlights: 规范化; .; .; .; 调整.

#### Features
- 规范化 (`69327e826`)
- . (`af1e44bf2`)
- . (`d14a625a5`)
- . (`87ebe1221`)
- 调整 (`a6466d52b`)
- 优化 (`165e8a143`)
- 优化 (`0e56df0cf`)
- 优化 (`bf9c17a7c`)
- 适配 (`2e2af4fe0`)
- 调整开发镜像版本 (`3630332ad`)
- 调整表结构 (`3fb69d0ae`)
- 调整表结构 (`d81f18dc6`)
- 优化 (`d3a3562df`)
- 优化 (`edda9e51e`)
- 优化 (`79db74d6d`)
- 优化 (`7c957e6e9`)
- 优化 (`3fbf934bd`)
- 优化 (`756abe78e`)
- 优化 (`578f0cd66`)
- 优化 (`46134113c`)
- 调整 (`ed5a5701f`)
- . (`69a311d79`)
- . (`71dae65eb`)
- . (`23bc573dc`)
- . (`a2d9c7914`)
- . (`36763998e`)
- 优化 (`d6e27e3cd`)
- 优化 (`bec548a9e`)
- . (`7b5d9c050`)
- . (`4e1885e8c`)
- . (`e3b277c57`)
- . (`c290cf8e1`)
- . (`37b9d591d`)
- 优化 (`17e414150`)
- . (`0d3c4d3ef`)
- . (`1c9bb3759`)
- . (`cdab67ee7`)
- . (`fa06dea2e`)
- . (`a4ed4fb80`)
- 调整page，调整service接口 (`c9588b55c`)
- . (`150a57264`)
- . (`546d68c0b`)
- . (`bc117b2fe`)
- . (`88f8cd34e`)
- . (`f2ef4e33a`)
- . (`5ddb628be`)
- . (`4f6fdf891`)
- . (`2a9777ce8`)
- . (`ca12523ee`)
- . (`34611cf92`)
- . (`36f24253f`)
- 优化配置文件 (`aff31bac7`)
- . (`04c9d6d5e`)
- . (`03a41480d`)
- . (`487eb0b84`)
- 调整部分依赖版本 (`6c3796866`)
- . (`68fd4c37b`)
- . (`c10a88854`)
- . (`49a538f97`)

### 📌 2023.9.26

_Generated on 2026-08-19._

#### Summary
- Generated from `0f64a1fd6` to `bcab3e546`.
- Included 16 commits across 2 categories: Features 14, Other 2.
- Highlights: .; .; .; .; ..

#### Features
- . (`bcab3e546`)
- . (`2c7df870a`)
- . (`4b5c78c52`)
- . (`880a58ff1`)
- . (`9a0bc8a88`)
- . (`af1e9aa6a`)
- 调整Header信息，改为容器基类默认实现 (`6dc571ac1`)
-  . (`12d78d4bc`)
- update to 2023.4.5 (`246a3a3e7`)
- MySQL配置适配 (`2a08cd5c2`)
- 配置基础Controller (`704a9da7e`)
- 调整web接口 (`e00170e86`)
- 新增 MQTT 主题前缀配置 (`4a365f396`)
- . (`7143b2e93`)

#### Other Changes
- 1.增加nacos dev命名空间 2.增加rule sql 3.优化rule engine (`1a49fb61c`)
- ruleengine init (`5163478da`)

### 📌 2023.6.29

_Generated on 2026-08-19._

#### Summary
- Generated from `4b78b7a3b` to `0f64a1fd6`.
- Included 84 commits across 4 categories: Features 62, Bug Fixes 6, Documentation 1, Other 15.
- Highlights: 2023.4.4; config spring-boot-configuration-processor; 修复mqtt批量数据接收统计; update to 2023.4.3; update to 2023.4.2.

#### Features
- 2023.4.4 (`129f3dcbf`)
- config spring-boot-configuration-processor (`04d2bb786`)
- 修复mqtt批量数据接收统计 (`557d78b71`)
- update to 2023.4.3 (`a204a13e1`)
- update to 2023.4.2 (`e698a6a43`)
- . (`edbc145fd`)
- . (`2ce9fa588`)
- . (`426f60adb`)
- . (`bff83f0cd`)
- . (`a933a150c`)
- 修复 mqtt 驱动指令下发 (`da070bf77`)
- mqtt write command  fix (`c006f430a`)
- mqtt (`6575c5649`)
- 修复驱动写指令携带一个默认的读操作 (`8df208785`)
- update readme (`f25b0a011`)
- 更新Auth模块的auth-common依赖版本 (`5b417d0a6`)
- ping (`97251c926`)
- demo (`0271f2529`)
- demo (`771ae4373`)
- 调整 RELEASE pom (`a3d2e8e3d`)
- . (`351b71a19`)
- 1.增加Auth-Common模块（下一步：完成切面角色及权限验证逻辑） 2.调整登录接口 (`d57cb1dbf`)
- notify (`69b07a2d9`)
- update driver & common (`087a61197`)
- update driver custom service (`b231dc7ef`)
- driver sdk update (`414a5c4f0`)
- pom update (`9ede0159b`)
- docker-compose.yml update (`a6cadaddd`)
- gateway update (`0fb43558f`)
- . (`9b1094575`)
- DO (`d39f0bfbb`)
- Optimize delete service function (`bd32f89d2`)
- Optimize update service function (`3f0b8cf2a`)
- add CNAME:doc.dc3.site (`f0bf36dce`)
- add CNAME:doc.dc3.site (`768e2fa1e`)
- add CNAME:doc.dc3.site (`3c4167f9c`)
- add CNAME:doc.dc3.site (`e3dd84cb8`)
- add CNAME:doc.dc3.site (`8c595908e`)
- new docs (`b6d75aced`)
- add modbus4j (`fc019a24f`)
- Optimize add service function (`b0b4f6381`)
- 1.调整类名 2.login方法返回token,前端获取后，作为header参数发请求 (`e3bf5cdc7`)
- 调整Sonatype Repository (`3bb64e1c7`)
- auth模块服务类适配Service接口改动 (`9eb6c2d5e`)
- . (`68cafead5`)
- . (`c404cd562`)
- 完善UserManageServiceImpl.login 方法， 增加ResourceService 与 RoleResourceBindService (`3f21fc584`)
- auth提交代码，增加UserManageService (`27cf76929`)
- 天气驱动 (`c4c36dc50`)
- 适配 (`1b410bb64`)
- 适配 (`777d43b2e`)
- 适配 (`545381472`)
- 修改基础镜像版本 (`6f0719c99`)
- driver_info to driver_attribute_config & point_info to point_attribute_config (`72db94baa`)
- device remove multi_flag (`87db53e11`)
- point remove accrue_flag, unit to string (`3a981df7a`)
- driver remove service_port (`a06d038d9`)
- user_ext table to user table (`33aa6c8cf`)
- user table to user_login table (`f1749390b`)
- user table to user_login table (`0cfce6986`)
- user table to user_login table (`cf4926160`)
- update_time to operate_time (`e407e099f`)

#### Bug Fixes
- 格式化 (`d85a723b9`)
- opcua read error (`82a1a13f8`)
- remove ping (`0de69668f`)
- 修复auth模块jar无法下载问题 (`a24bb2070`)
- 修复auth模块jar无法下载问题 (`8fa284934`)
- 修复auth模块jar无法下载问题 (`de1f22c1f`)

#### Documentation
- mysql,redis,mongo,rabbitmq 容器配置 (`bc536a479`)

#### Other Changes
- fix pom (`0f64a1fd6`)
- tdengine数据库支持 (`00c2b4d59`)
- 首页相关接口: 1.数据统计 2.天气设备地图 (`b015dc717`)
- 数据统计 (`3c2c6a0d3`)
- Update pom.xml (`5a78a2fb9`)
- 抽取高德天气驱动配置项 (`ce4dd9341`)
- . (`f3f290caa`)
- 添加mysql和mongo数据库初始化脚本 (`dce37ce37`)
- remove docs (`a9322aedc`)
- debug mqtt演示代码无采集时间 (`7c3f67919`)
- Delete CNAME (`d2dba0d3f`)
- Update CNAME (`91a0ca762`)
- Create CNAME (`bb510a0d7`)
- add mqtt演示代码 (`b7a59b2a6`)
- Optimize update service function (`638133df4`)

### 📌 2023.3.31

_Generated on 2026-08-19._

#### Summary
- Generated from `b18985601` to `4b78b7a3b`.
- Included 63 commits across 3 categories: Features 55, Bug Fixes 4, Other 4.
- Highlights: .; 优化; 优化驱动通知; 调整MQTT类型驱动逻辑; mqtt模块修复.

#### Features
- . (`f6e039108`)
- 优化 (`4cb162309`)
- 优化驱动通知 (`e644da62d`)
- 调整MQTT类型驱动逻辑 (`8393ab2a0`)
- mqtt模块修复 (`ea9ac6eec`)
- 优化 (`d9860fab0`)
- . (`f88bff832`)
- 更新驱动事件状态接口 (`03fc8605b`)
- 调整驱动事件和设备事件 (`19c7ef9b7`)
- 设备指令：读、写 (`9caba2bc6`)
- 驱动注册逻辑更新 (`08aacfc8a`)
- 驱动SDK更新 (`4e3b91804`)
- 新增一个dtu（移讯通）类型的驱动 (`1aced73cd`)
- 1.去掉驱动的status配置；2.优化驱动注册机制 (`380b7911d`)
- . (`b52491335`)
- 调整 Copyright 为致敬每一位开源开发者 (`53769be71`)
- 调整 Copyright 为致敬每一位开源开发者 (`bb7f9928a`)
- . (`28e333c60`)
- 适配WEB和适配调整驱动 (`4c147ceab`)
- 开发代理程序更新，提供8080和8000端口映射到本地 (`bb98a0be9`)
- 调整驱动逻辑支持远程开发 (`49ab4e83b`)
- . (`da22ec153`)
- 调整接口文件 (`34f6f0626`)
- 清理代码&移出多余文件 (`90e20ad9f`)
- 新代理工具 (`22fe61082`)
- nacos 替换 eureka (`0a51a2587`)
- 初始化sql调整 (`063dd00cc`)
- 开发环境端口配置 (`f87fa68a1`)
- 上传三个平台的代理工具 (`1f9519a9e`)
- 更新 (`5c4025464`)
- 更新 (`3a396bd69`)
- 更新 (`baf873b1d`)
- 更新 (`e387a3372`)
- 更新 (`2329dd2c3`)
- . (`4d3bdbd98`)
- . (`55b2ffb64`)
- . (`7e2e1b246`)
- main (`e43a84ef9`)
- 更新 (`5438db447`)
- 更新 (`f928efaeb`)
- 更新 (`30b9fa5eb`)
- 更新 (`7b626c5d9`)
- 更新 (`f34ccef33`)
- 更新 (`ef7cb304f`)
- 更新 (`195c83a17`)
- 更新 (`90632b1bd`)
- 更新 (`508ba32a3`)
- 更新 (`b76a24e7c`)
- 更新 (`d6e07e174`)
- 更新 (`21fb90e10`)
- 代码调整 (`3b5fc7710`)
- update (`4f3155469`)
- V2022.2.3 (`d32d1b688`)
- V2022.2.2 (`6d21f50be`)
- V2022.2.1 (`7465e39f4`)

#### Bug Fixes
- 升级grpc接口问题，驱动注册失败修复 (`2bc597ce2`)
- 1.修复驱动连接异常；2.修复gRpc服务配置错误 (`29e0faa6c`)
- 1.代理端口修复；2.rabbitmq连接修复 (`7e7e75af3`)
- 修复代理工具Mac平台链接异常的问题 (`f07ffae53`)

#### Other Changes
- debug 增加bucket判断,防止not found (`4b78b7a3b`)
- 增加influxData存储策略 (`924c898f2`)
- lwm2m驱动使用说明 (`4d4329a82`)
- lwm2m init (`148aee5b0`)

### 📌 2022.12.28

_Generated on 2026-08-19._

#### Summary
- Generated from `e6bbc1a80` to `b18985601`.
- Included 21 commits across 3 categories: Features 18, Bug Fixes 1, Other 2.
- Highlights: V2022.1.9; V2022.1.8; mysql 初始化脚本更新; dockerfile config; import exception and public.

#### Features
- V2022.1.9 (`b18985601`)
- V2022.1.8 (`1c14a6770`)
- mysql 初始化脚本更新 (`364aa5086`)
- dockerfile config (`1e3eb38af`)
- import exception and public (`d2de8478a`)
- 移出register和monitor (`01b2c3627`)
- 添加注释 (`ffd26e863`)
- . (`be5f409f5`)
- . (`a923720a8`)
- pom (`dba7d061a`)
- doc (`c98eaea7e`)
- . (`296a9fb8e`)
- . (`bbcd17687`)
- 代码调整 (`00e7e2c95`)
- 代码调整 (`9a7239b8e`)
- 代码调整 (`9a447ad47`)
- 代码调整 (`22b602548`)
- 调整开发环境节点IP配置为环境变量控制 (`dcb153038`)

#### Bug Fixes
- 修复驱动&位号信息通知故障 (`2a4d6c165`)

#### Other Changes
- dc3-center-manager: fix logic error in method selectByDeviceId (`4d76be837`)
- dc3-center-data: correct the key format of getKey from Redis (`940cd978d`)

### 📌 2022.8.21

_Generated on 2026-08-19._

#### Summary
- Generated from `074fa9769` to `e6bbc1a80`.
- Included 5 commits across 1 categories: Features 5.
- Highlights: update code; update code; update code; 移除批量导入导出逻辑; 代码优化.

#### Features
- update code (`e6bbc1a80`)
- update code (`ab094518e`)
- update code (`543c3bd8d`)
- 移除批量导入导出逻辑 (`eec75b9d3`)
- 代码优化 (`dedb3509a`)

### 📌 2022.6.22

_Generated on 2026-08-19._

#### Summary
- Generated from `1757606a1` to `074fa9769`.
- Included 18 commits across 3 categories: Features 10, Bug Fixes 1, Other 7.
- Highlights: tag bash; .; 支持配置是否post数据到opentsdb和elasticsearch; 调整 ID 为 String 类型; 调整.

#### Features
- tag bash (`01814dd31`)
- . (`fa17c4e55`)
- 支持配置是否post数据到opentsdb和elasticsearch (`969894dad`)
- 调整 ID 为 String 类型 (`39959de1b`)
- 调整 (`4deb4ef3e`)
- mqtt 驱动添加批量接收消息逻辑 (`eda31352a`)
- 代码优化 (`cbec9701a`)
- 代码优化 (`2b2be5b4d`)
- 代码优化 (`46956634f`)
- 代码优化 (`d6a7d1fa1`)

#### Bug Fixes
- 调整 Xss 大小 (`074fa9769`)

#### Other Changes
- 修复了使用字符串作为jwt密码导致的生成和解析密码不一致仍旧可以解析成功的问题 (`a4b8e1cd3`)
- ElasticsearchConfig (`8ed1007c0`)
- ElasticsearchConfig (`b0aa12adf`)
- 更新配置文件 (`5a936ec66`)
- 更新配置文件 (`9703c6a0e`)
- 更新配置文件 (`5b5f0fa1e`)
- . (`218d58994`)

### 📌 2022.3.30

_Generated on 2026-08-19._

#### Summary
- Generated from `b42a78237` to `1757606a1`.
- Included 25 commits across 3 categories: Features 6, Bug Fixes 13, Other 6.
- Highlights: 配置日志切面; demo bash & docker-compose yaml; .; rename; 2022.1.0.

#### Features
- 配置日志切面 (`6b45fad2f`)
- demo bash & docker-compose yaml (`e04491fb0`)
- . (`1496f113a`)
- rename (`0c8b2557c`)
- 2022.1.0 (`23f4e5004`)
- 日常更新 (`707a420e7`)

#### Bug Fixes
- 调整服务名称 (`8407f8495`)
- 循环依赖 (`38855213f`)
- 调整方法名称 (`e1fae660b`)
- 循环依赖 (`51fdc7257`)
- 循环依赖 (`8fce0e6fc`)
- docker log config (`44dc66f5d`)
- . (`71c1cc5d7`)
- enable thread io & disable append only file (`5262d987e`)
- registry.cn-beijing.aliyuncs.com/dc3/redis:6.2.6-alpine (`4dec35472`)
- . (`06f9e450b`)
- rdb&aof config (`89b47f322`)
- default redis config (`779211257`)
- redis config (`e9b3da2e5`)

#### Other Changes
- 调整配置文件 (`1757606a1`)
- thread (`79bc48cb2`)
- . (`4baace6d1`)
- . (`d90fed550`)
- 添加 modbus4j error 说明 (`ba7b4f7c3`)
- . (`705dcefe1`)

### 📌 2021.12.28

_Generated on 2026-08-19._

#### Summary
- Generated from `ba8812a92` to `b42a78237`.
- Included 42 commits across 4 categories: Features 24, Bug Fixes 16, Tests 1, Other 1.
- Most active scopes: docker(12), data(1).
- Highlights: iptables.md; iptables.md; emqx cluster readme; docker: rabbitmq cluster port config; docker: rabbitmq cluster mqtt tls.

#### Features
- iptables.md (`b42a78237`)
- iptables.md (`ad39df0de`)
- emqx cluster readme (`c9ef98073`)
- **docker**: rabbitmq cluster port config (`916582ef3`)
- **docker**: rabbitmq cluster mqtt tls (`6a68fb15a`)
- rabbitmq cluster readme (`6a4379016`)
- **docker**: rabbitmq cluster node port (`58d7b7d90`)
- rabbitmq 调整目录结构 (`b86929836`)
- **docker**: rabbitmq etc (`a788cd92b`)
- **docker**: rabbitmq env & advanced config (`bcb992d43`)
- **docker**: rabbitmq port config (`6db51b495`)
- rabbitmq etc rabbitmq.conf (`b50aa3538`)
- **docker**: rabbitmq server add plugins (`36986017a`)
- rabbitmq config (`1131dbdbd`)
- **docker**: rabbitmq server (`1860d1a06`)
- redis集群部署说明 (`8fa901178`)
- 更新说明 (`d68108cf8`)
- mongo 集群部署说明 (`81c6743a7`)
- **docker**: mongo & redis config (`4d3843d8d`)
- set influxdb token (`c1908d786`)
- **docker**: influxdb dockerfile (`9834263f7`)
- **data**: opentsdb (`657f35ff4`)
- **docker**: grafana dockerfile config (`008dfa84a`)
- add opentsdb to docker-compose.yml (`0998bf89b`)

#### Bug Fixes
- docker mongo crlf -> lf (`5221c0a49`)
- driver start (`4fd730882`)
- rabbitmq mqq tls set password (`25f0b2446`)
- rabbitmq mqq tls (`d58433e92`)
- rabbitmq cluster readme (`9d4b7bf61`)
- plcs7 driver read & write (`fd018ef13`)
- redis mset (`dc1c901b5`)
- mvn package bug (`335fdbdd1`)
- **docker**: opentsdb dockerfile (`b338d97fe`)
- spring boot version (`e37fe7079`)
- Tcp Encoder (`e17d25f76`)
- emqx port (`d130547aa`)
- api (`29c1f3bc0`)
- readme (`ed9187813`)
- jar version (`02dd5e5ef`)
- spring boot plugin version (`90493da32`)

#### Tests
- update spring boot version (`eb78fade5`)

#### Other Changes
- RedisUtil (`46cd1cd3d`)

### 📌 2021.9.24

_Generated on 2026-08-19._

#### Summary
- Generated from `1ff4ee428` to `ba8812a92`.
- Included 49 commits across 3 categories: Features 32, Bug Fixes 6, Other 11.
- Most active scopes: api(7), driver(2), edge-gateway(2), develop(1), mqtt driver(1).
- Highlights: add reset readme; qq group link; reset bash; 优化; 调整 & 添加 tsdb 支持（demo）.

#### Features
- add reset readme (`ba8812a92`)
- qq group link (`d8bc63d96`)
- reset bash (`57335c320`)
- 优化 (`f29e81c85`)
- 调整 & 添加 tsdb 支持（demo） (`525b1b00a`)
- 优化MongoDB数据存储方式 (`fde02cdd4`)
- set gateway timeout (`4c1d25671`)
- add git tag script (`e4e375945`)
- add maven settings.xml (`176e36a2a`)
- update spring boot (`278e7446c`)
- dev (`31959fdfb`)
- opc ua driver (`dcb54d730`)
- request mapping (`dad56dcdf`)
- driver mqtt & edge gateway (`cd417b513`)
- driver sdk (`c66d1d0f1`)
- profiles (`89d704df4`)
- base common (`80576510f`)
- manager service (`f0ef15a36`)
- data service (`65bbea7c8`)
- manager api (`7c4967561`)
- pointValue Client (`9a5ccc9b9`)
- **driver**: . (`9244d685e`)
- **edge-gateway**: add a new type driver: edge-gateway (`81b6c5060`)
- **driver**: add driver type (`2fda5dd23`)
- **api**: Dictionary (`4f32f5193`)
- **api**: . (`ec797cc36`)
- **api**: attribute value @NotNull (`e5fc981b8`)
- **api**: add response code (`7eccc77ca`)
- **api**: add api (`e95b71163`)
- **api**: add selectByAttributeIdAndDeviceIdAndPointId (`e4c69e8c8`)
- **api**: add selectByAttributeIdAndDeviceId (`cc4e2db01`)
- **mqtt driver**: TLS (`4c94713f1`)

#### Bug Fixes
- pom (`1f2a77ffc`)
- remove start (`c2e60ece0`)
- **edge-gateway**: api type (`e769635aa`)
- **develop**: dev (`2c92c7495`)
- repackage bug (`7a352975d`)
- line (`59642a335`)

#### Other Changes
- . (`0bec1d890`)
- . (`0eb6da143`)
- . (`cf9cc62ff`)
- . (`97cae05c3`)
- . (`2d92d17e2`)
- . (`3a479fb9f`)
- . (`514ccb230`)
- . (`78d5d5878`)
- . (`bf493c6af`)
- . (`a12b140d2`)
- . (`b9869961c`)

### 📌 2021.6.30

_Generated on 2026-08-19._

#### Summary
- Generated from `551911de2` to `1ff4ee428`.
- Included 90 commits across 5 categories: Features 1, Bug Fixes 6, Build 1, Chores 1, Other 81.
- Most active scopes: dc3-driver(2), maven(1), dc3-rtmp(1), dc3-gateway(1), dc3-center(1), dc3-manager(1).
- Highlights: dc3-driver: 驱动、位号属性调整; dc3-driver: 修改gc日志不打印; dc3-rtmp: 修复gc日志不打印; dc3-gateway: 修复gc日志不打印; dc3-manager: 修复gc日志不打印.

#### Features
- **dc3-driver**: 驱动、位号属性调整 (`34ec04716`)

#### Bug Fixes
- **dc3-driver**: 修改gc日志不打印 (`0e60f7ac3`)
- **dc3-rtmp**: 修复gc日志不打印 (`d50325bc8`)
- **dc3-gateway**: 修复gc日志不打印 (`e021954d8`)
- **dc3-manager**: 修复gc日志不打印 (`261ccefe7`)
- **dc3-data**: 修复gc日志不打印 (`9e0f05d21`)
- **dc3-auth**: 修复gc日志不打印 (`a317c0617`)

#### Build
- **maven**: pom.xml 添加 dc3 gitee maven 仓库配置 (`9283cb72a`)

#### Chores
- **dc3-center**: 调整日志位置 (`54a07b640`)

#### Other Changes
- . (`1ff4ee428`)
- . (`e6941c183`)
- Wiki & Demo (`d0e318cd0`)
- . (`31446371c`)
- . (`b4e4cb54f`)
- . (`6afd2cafa`)
- . (`b7c83c548`)
- 去掉 dc3-config，取而代之的是 dc3-profiles，这样更合理一些，同时更便于管理 (`50cdad8e3`)
- 适配 spring-boot 2.4.x 版本配置文件，并修复 1.3.2.SR 版本 (`df4812969`)
- Driver SDK 适配 spring-boot 2.4.x 版本配置文件 (`9ee6f6c6e`)
- rollback (`5b486d574`)
- . (`d91b08fc8`)
- 调整 event rabbitmq 配置 (`41bacd396`)
- dc3-auth 适配 (`b6c39e629`)
- . (`782497e1d`)
- dc3-monitor 适配 spring-boot 2.4.x (`5750695ca`)
- 调整 dc3-gateway 模块 适配 spring-boot 2.4.x 版本 (`d26340f10`)
- 更合理的管理通用的配置文件 (`e4931f16b`)
- 新增 dc3-profiles 模块，用于适配 spring boot 2.4.x 版本新的配置逻辑 (`b46e73048`)
- 调整配置文件和日志配置 (`37c930959`)
- 镜像启动 添加GC日志和优化JRE (`8cbb66a78`)
- 优化 dc3-monitor 启动占用较高内存 (`422771ac7`)
- 优化 dc3-register 依赖，减少启动内存消耗 (`9a4d9b6ef`)
- 大版本更新1.3.2.SR升级 (`5ef4c7f01`)
- 适配 Driver SDK (`4170882a2`)
- 修复 Long 数据类型返回到前端序列号问题 (`e9ca5a57e`)
- 调整 DictionaryApi 设备接口 (`879361583`)
- 调整 驱动注册 逻辑 (`2c67705b7`)
- 调整 Dictionary 逻辑 (`c40d2e578`)
- logback.xml config (`e308d9b42`)
- 适配，因为去掉了 deviceNameMap (`22f75a5da`)
- 去掉 deviceNameMap ，因为设备名称会存在重复问题 (`e8b74db5d`)
- 优化 DriverMetadata 逻辑 (`a337e7f8f`)
- 调整 DriverMetadataService 逻辑 (`27ec628e2`)
- 调整 DriverSDk 逻辑 (`5df8e2d61`)
- 调整 DriverMetadataReceiver 逻辑 (`e9c816b0d`)
- . (`1df7cc61d`)
- 调整 DriverMetadata 逻辑 (`9fcad4ce6`)
- 调整 DriverContext 逻辑 (`6f70044c6`)
- 调整 PointService 逻辑 (`fefe4f9ae`)
- 调整 ProfileService 和 ProfileBindService 逻辑 (`1a49ecff6`)
- 调整 DeviceService 逻辑 (`615d479be`)
- [🔨新增接口] 状态接口新增单独查询设备和驱动的状态接口 (`33e1d800a`)
- 调整 GroupService 逻辑，待调整该逻辑 (`dfac64174`)
- 调整 DriverService (`1a6d60943`)
- 调整 DriverInfo 由之前关联 Profile 修改到 关联 Device (`0058badbf`)
- 调整 NotifyServiceImpl 模版和位号修改需要通知到多个关联的驱动服务 (`013fa6206`)
- 调整 Profile 和 Device 数据库表结构，调整 Profile 为独立，调整 Device 关联 Driver (`e79253e11`)
- 调整TopicRabbitConfig的配置 (`9ede6415b`)
- 调整驱动配置：由绑定固定的模版（因为模版现在抽象出来了）改为绑定指定的设备 (`195ecea8f`)
- . (`6299caee9`)
- [新增] 设备与模版映射关联表和相应逻辑 (`3c056ee99`)
- 新增若干常量 (`966989105`)
- 调整租户与用户关联表结构 (`a7349b086`)
- [fix🐛] 租户与用户关联 (`53a2c1e3b`)
- . (`cd9f33076`)
- 更新 GitHub Maven 仓库 Token (`ad1194194`)
- 更新 GitHub Maven 仓库 Token (`fd3dedc63`)
- Driver、Profile、Point、Device 添加 enable 字段 (`3871a6f77`)
- Driver、Profile、Point、Device 添加 enable 字段 (`c83625a81`)
- 调整 dc3-api-rtmp (`aa9986657`)
- 调整 dc3-api-manager (`164d0cb09`)
- 调整 dc3-api-auth (`551f3bf70`)
- 调整 dc3-auth (`4fad71155`)
- 调整 dc3-base (`050c32115`)
- 调整 dc3-gateway (`b1db86392`)
- 调整 sdk 配置文件 (`329ccd94f`)
- 调整API接口说明 (`b391acbb4`)
- 调整数据库结构 (`16076a575`)
- 调整 dc3-auth 接口说明 (`e73bed100`)
- 调整 auth 接口说明 (`c96e9919a`)
- [!新功能👍]多租户实现 (`5e214d53e`)
- 更新spring cloud版本到2020.0.2，并适配若干配置项 (`1789a0bf5`)
- jmeter config (`65d9eca08`)
- update (`61a1a10f9`)
- mqtt 相关配置 (`d332b7dc8`)
- 调整： 1.DriverService 新增： 1.DataCustomService 2.消息队列TTL设置，更新后需要重新构建&启动rabbitmq容器 (`d63de6a95`)
- 调整： 1.调整ID类型 2.添加tenant租户字段 (`d29241000`)
- 调整： 1.用户校验 2.设备事件 (`a61447542`)
- 修改主键ID生成策略：雪花算法 (`dd473dbd7`)
- gateway redis (`4532bab16`)

### 📌 2021.3.26

_Generated on 2026-08-19._

#### Summary
- Generated from `9e037b6bf` to `551911de2`.
- Included 10 commits across 1 categories: Other 10.

#### Other Changes
- . (`551911de2`)
- 优化 (`84f22e3b6`)
- 优化 (`a34a30b17`)
- 优化 (`5a42c9c74`)
- 优化 (`491dfee98`)
- 优化 (`010ff06dc`)
- 调整驱动注册机制 & 驱动元数据同步机制 (`120ecf8c6`)
- 调整注册和配置同步机制 (`1f2527f0b`)
- V1.2.1，新年快乐！ (`14d6aa2e3`)
- Update Dockerfile & Jvm (`947611a6d`)

### 📌 2020.12.25

_Generated on 2026-08-19._

#### Summary
- Generated from `c4f00c9d7` to `9e037b6bf`.
- Included 38 commits across 1 categories: Other 38.

#### Other Changes
- 升级1.2.0 (`9e037b6bf`)
- 新增脚本文件 (`674181d65`)
- 添加丰富的环境变量配置 (`3935210be`)
- 优化token机制 (`f05b2560f`)
- . (`7cfe3febc`)
- . (`c252b8861`)
- . (`7b0943da3`)
- device status default:offline (`759bdc27a`)
- 更新说明文档 (`7d6bcc5ff`)
- device status default:offline (`96a31410e`)
- . (`be1f153f8`)
- add event api (`455de387a`)
- 使用 deviceEvent 替代 deviceStatus (`fcb0b8517`)
- . (`f60a887b7`)
- mqtt 模块使用文档 (`527a66e7d`)
- 补充:modbus tcp说明文档 (`4773ef087`)
- 添加modbus tcp驱动模块说明文档 (`31eba770e`)
- remove event module (`786d0acc2`)
- . (`c65fb9173`)
- point value update (`46a1f3abc`)
- rabbitmq config (`fec0a5f8b`)
- 数据流速动态监控，批量、定时入库机制 (`86b094a86`)
- 调整 (`baa4a6711`)
- mongo init script (`0fd0e47f0`)
- add event (`7951db77e`)
- init event module (`79da540b0`)
- . (`c27e33501`)
- . (`b30fd5076`)
- . (`0a14f947a`)
- . (`4e6be09ad`)
- . (`4c28aab49`)
- . (`3a7ffd529`)
- doc (`e1fec71cc`)
- . (`5228fb920`)
- . (`3fceb7c79`)
- 优化demo启动顺序 (`7f839b7c2`)
- 优化 (`a70f480ef`)
- 数据纠正接口 (`76b4a08d7`)

### 📌 2020.9.28

_Generated on 2026-08-19._

#### Summary
- Generated from `b254ec8e8` to `c4f00c9d7`.
- Included 94 commits across 2 categories: Security 1, Other 93.
- Highlights: 调整： 1.redis pool 2.openfeign log 3.spring boot version 4.mysql version 5.hystrix pool & timeout 6.mongo 7.spring security.

#### Security
- 调整： 1.redis pool 2.openfeign log 3.spring boot version 4.mysql version 5.hystrix pool & timeout 6.mongo 7.spring security (`49292114e`)

#### Other Changes
- v1.1.0 (`c4f00c9d7`)
- v1.1.0 (`925e9282c`)
- . (`c5b7e6bf2`)
- 调整driver-sdk (`43b090dfa`)
- 添加OpcUa驱动模块使用说明 (`6e0d73b23`)
- 添加 request mapping 列表请求接口，访问 / 即可获取当前服务的全部接口信息 (`d8a752810`)
- 更新文档说明 (`ec5f75c6b`)
- ### dc3-register 调整 (`ca9260724`)
- 更新文档 (`18f5f911f`)
- 调整 (`64f710b3a`)
- . (`77a6d5444`)
- . (`03d1e74eb`)
- . (`20ad0f863`)
- . (`ce04d1cc0`)
- 调整rabbitmq中mqtt插件，添加dc3 vhost配置 (`1b89558a1`)
- . (`a0bfe5f20`)
- . (`d314f100a`)
- . (`d604f047f`)
- Create CNAME (`d26e1b9f6`)
- . (`0a750a1c3`)
- Create CNAME (`776c317c3`)
- . (`5e65e34d9`)
- . (`9c068ff8d`)
- . (`e86281e42`)
- . (`42df547e8`)
- . (`2ccb0ecb3`)
- 新增无授权Exception UnAuthorizedException.java (`01bc7aa7f`)
- 新增颠倒byte[]方法 (`532c5b710`)
- 批量导入新增multi设置 (`d3cca5c1c`)
- multi (`dc8cc1bbd`)
- 优化 (`8035d3399`)
- 取消 udp read timeout (`bc66237a7`)
- 允许**/*.json文件打包到resources (`de0692a7b`)
- 优化 (`2d8c0a5d4`)
- . (`51b809da5`)
- . (`0b94feb29`)
- . (`5328c6278`)
- . (`a25cfc6d6`)
- sdk中通过设备查询日志打印null (`7385f5e13`)
- . (`313f2226c`)
- . (`2410f2659`)
- 188b tcp & udp (`7e45756ce`)
- dev.sh (`a30c22acb`)
- dev.sh (`3f12df1fc`)
- 代码优化 (`6b6256f02`)
- 添加bytesToInt大小端转换 (`ab98d27ae`)
- . (`445ff37b6`)
- merge (`b812455cc`)
- . (`1357d3a62`)
- 优化批量导入 共享模版功能 (`a5cd35469`)
- #bugfix 修复批量导入是出现中文乱码的问题 (`9788deb61`)
- . (`f2fcafbf8`)
- 更新说明 (`46f712ee3`)
- water device 188b driver (`75ad8323f`)
- . (`9453b3085`)
- 位号数据 存储类型支持：单点 & 结构化 存储 (`9c03e8d7e`)
- 优化 manager & data & driver sdk 消息组件配置 (`11b586e26`)
- #bugfix 修复批量导入，不更新位号问题 (`5806ee276`)
- rabbitmq 添加 virtual-host=dc3 配置 (`fcbb26994`)
- mysql 设备添加multi（数据存储类型）字段 (`78e56c02b`)
- install-env.sh 安装基础环境脚本文件 (`ac89a1ad8`)
- . (`b10ca77f3`)
- #bugfix gateway authentic (`ce5a2ff46`)
- 1.调整rabbitmq配置，添加virtual-host:dc3 2.调整redis线程池 (`9245b3050`)
- auth update (`10909b87a`)
- add maven setting.xml (`b6335b933`)
- . (`99a75c71d`)
- . (`7113702d0`)
- . (`6158a19e3`)
- #bugfix 修复Mqtt和Virtual Listening驱动模块的profile driverInfo 为空异常 (`d22241554`)
- update spring boot version (`2ca733362`)
- Set theme jekyll-theme-cayman (`1253875bf`)
- . (`a3e8e7e46`)
- version (`75eeaf5c2`)
- maven version (`c3437fec6`)
- maven version (`4b7c609cf`)
- 管理品台 -》 管理平台 (`97e685df9`)
- 调整modbus-tcp (`2617b439b`)
- 测试&调整合并 (`7f9e12efb`)
- 解决模版删除报错问题 (`db1142788`)
- https://gitee.com/pnoker/iot-dc3/issues/I1MQUG (`6efc74a8c`)
- 调整自定义线程池maximum-pool-size=512 (`3b4cbe140`)
- 优化并发读取，调整Opc Ua异步读 (`531d8554e`)
- aop (`f62828bf0`)
- dc3.version=1.0.0 (`b438e9856`)
- dc3.version (`a52f63d56`)
- dc3.version (`a5bc1475b`)
- 优化批量上传格式 (`fa50be2e2`)
- 批量上传调整 (`57818e57a`)
- 驱动模板支撑修改名称 (`430aa2007`)
- . (`f56981908`)
- . (`ac51e2b0b`)
- . (`ef6210560`)

### 📌 2020.6.25

_Generated on 2026-08-19._

#### Summary
- Generated from `3fe4eb946` to `b254ec8e8`.
- Included 83 commits across 1 categories: Other 83.

#### Other Changes
- update timer (`b254ec8e8`)
- 批量导入数据功能 (`0879ed475`)
- 升级spring boot 和 spring cloud 的依赖 (`4413233f7`)
- . (`0c28b3c1a`)
- . (`9253564ee`)
- 优化日志输出 (`c8314dd0d`)
- threadPoolExecutor (`dc0c9ed6c`)
- sh (`bcd2b5fb8`)
- driver & device status (`e8a1eded2`)
- 日志调整 (`200f7cb3a`)
- 调整驱动配置的变更通知逻辑 (`d4d51a30d`)
- 调整测点&测点配置的变更通知逻辑 (`3c0cf0d3a`)
- 调整模板的变更通知逻辑 (`197e92732`)
- 调整设备的变更通知逻辑 (`23c058c53`)
- 优化通知机制 (`f33ecf1a1`)
- 修改 SDK 接收 profile/device/point 数据修改逻辑为 Mq ，从而使得可以在内网也能接收数据变更的通知 (`0af3ddee3`)
- manager add rabbitmq support (`112e9e032`)
- change sdk & data rabbitmq config (`7b6ec87b5`)
- driver sdk 优化 (`5c45471f2`)
- . (`dcc570d6f`)
- 去设备Code和Status，改由auth和data模块统一管理 (`9665b7feb`)
- add jenkins (`621f78fcd`)
- driver sdk bug fix (`3ccf1b56f`)
- data apo config (`b041c5a18`)
- gateway update (`1182552e5`)
- user update (`dafbc94ad`)
- update调整缓存产生的bug (`10ca998d7`)
- PointAttributeClient 调整 (`24931fc0f`)
- PointInfoClient 调整 (`4601cdbb7`)
- PointClient 调整 (`def573d8c`)
- ProfileClient 调整 (`8a64d5a70`)
- GroupClient 调整 (`711f3ac8a`)
- DriverInfoClient 调整 (`3f94d935f`)
- DriverClient 调整 (`0e2b17aff`)
- DriverAttributeClient 调整 (`c01a030e2`)
- DictionaryClient 调整 (`6684f17a8`)
- DeviceApi 调整 (`501d460dd`)
- RtmpClient 调整 (`7f9106674`)
- TokenApi 调整 (`0dbf509e6`)
- UserApi 调整 (`25f303ac6`)
- BlackIpApi 调整 (`5f2ade2cc`)
- IP黑名单去掉修改 (`42629b002`)
- 提供统一的线程池，用于自定义线程调用 (`eaab79ea4`)
- 设备去掉CODE，改由Auth模块统一管理 (`8745ec25d`)
- dc3-auth.png (`01771ca1b`)
- gateway ip 限制 & 黑名单功能 (`5382fcd21`)
- token接口错误限制 (`7b0d46416`)
- 添加salt接口和注销token接口 (`b918ea45f`)
- docker images (`477aa3e07`)
- . (`2ce1238bd`)
- 完善modbus tcp 协议驱动功能 (`db3935bf3`)
- modbus 读功能 (`9371a3529`)
- flink docker (`c73cc7a1a`)
- README.md (`02d6fd3bd`)
- mqtt driver (`2f8d9a508`)
- bugfix #I1HXOR https://gitee.com/pnoker/iot-dc3/issues/I1HXOR (`57ec3b295`)
- spring cloud 版本升级 (`6dadb6361`)
- . (`f3519fde2`)
- mqtt init (`3861d9b19`)
- README.md (`a6b0ee3db`)
- opcua README.md (`8c1fb1074`)
- opcua read & write (`295b4d542`)
- opcda README.md && http接口测试 (`2d6c5a2dd`)
- opcda check (`33a3691db`)
- opcda check (`a535a5c1e`)
- 提交 http client api 接口说明 ./dc3/api (`2bca10b0c`)
- opcua (`7c01e50d9`)
- 钉钉群 (`0e9115c67`)
- chart (`94320aee5`)
- chart (`25480bde1`)
- copyright (`5e3f6de9c`)
- package change (`acb54a553`)
- open sources dc3 web ui (`200bf1b81`)
- open sources dc3 web ui (`8848daa8d`)
- wiki (`69d029e77`)
- remove opc ua module (`62fa820e8`)
- gitee (`30a689670`)
- gitee (`61549f3d0`)
- gitee (`8e8ffd39a`)
- . (`c6f3ddd1d`)
- . (`d0c9e9710`)
- Opc Da  & 写操作 (`652464956`)
- Opc Da  & 写操作 (`57513c940`)

### 📌 2020.3.27

_Generated on 2026-08-19._

#### Summary
- Generated from `e691a42fe` to `3fe4eb946`.
- Included 70 commits across 1 categories: Other 70.

#### Other Changes
- kube (`3fe4eb946`)
- 暴露端口 (`f757c52d1`)
- demo 截图 (`42ad79cae`)
- demo 截图 (`30872eb61`)
- demo (`cdb7e5718`)
- demo一键启动说明 (`7be49a928`)
- SDK启动注册检查 (`b72ab4d32`)
- README.md (`aaf2b1864`)
- README.md (`7160e63b8`)
- Dockerfile (`a91975d83`)
- plc s7 驱动 (`79ba76c78`)
- like查询bug -> 调整为eq (`e85bff52b`)
- github (`eaccb3a3b`)
- SDK优化 (`58a82cfd4`)
- SDK优化 (`794bfea96`)
- SDK优化 (`853b8a87e`)
- bytes转换工具类 (`03f1e3557`)
- 优化驱动注册逻辑&bug fix (`6b653b076`)
- Mongo添加线程池配合 & 支持多副本 (`495beb63d`)
- 优化 (`43fb82f32`)
- . (`cb5d5b0fc`)
- README.md (`a0d573cfb`)
- 初版本诞生了，驱动sdk和数据存储基本功能完成 (`26afae32f`)
- 优化驱动缓存的操作 (`60a87368d`)
- read (`45105edde`)
- 优化设备信息修改通知驱动逻辑 (`d5ae69113`)
- 调整修改设备，模板通知驱动逻辑 & 添加初版数据接收存储 (`c8bdc243b`)
- point value (`2d0149d95`)
- 将调度功能挪到驱动层进行配置 (`fd986650a`)
- 添加设备&模板增删改通知到协议驱动SDK (`30136c1f1`)
- 调整sdk (`65b8cffce`)
- 添加driver配置的spring configuration metadata注释 (`9eb674d49`)
- 优化驱动SDK初始化逻辑 (`6ac4f3c6a`)
- web (`5e841ee7b`)
- web (`7368785ba`)
- dictionary 整合 (`80e35c73f`)
- 去多余接口 & 规范化 (`9cba1c815`)
- 初始化加载驱动信息和位号信息 (`0ca4fb2e7`)
- 注释 (`162851445`)
- ConnectInfo.java -> DriverAttribute.java,ProfileInfo.java -> PointAttribute.java (`cca8e890d`)
- driver info (`9a1884ead`)
- driver info (`2f03ab0c9`)
- interface (`f244381f5`)
- virtual (`f8d6bf610`)
- manager & data & rtmp (`2a5c31364`)
- port & mysql & docker-compose (`f692ebe50`)
- gateway & data (`844a66eaf`)
- base update (`db9204d19`)
- config 合并 & 简化 (`3b9a3143b`)
- message (`39b286439`)
- sdk schedule init (`a20dfb439`)
- README.md (`2027ab02f`)
- add driver (`46643f7de`)
- web (`d0190c692`)
- 位号模糊查询 (`efb750441`)
- 字典数据&数据表长度优化 (`1ef5baf51`)
- 调整 (`34e2bb254`)
- update cache & dic table (`7c2806590`)
- . (`e7e3c1559`)
- 设备 (`e95762d18`)
- 新年快乐 🎨 (`0f37f677a`)
- 设备管理，驱动接口 (`7eed6df57`)
- 设备管理，模板&位号接口 (`a75a1aca2`)
- 设备管理，设备分组接口 (`d990d8342`)
- 设备管理，设备接口 (`5ae894ac9`)
- ingress 调整 (`346d5c06f`)
- ingress (`e2c3e1de8`)
- k8s network namespace (`bd15277d1`)
- k8s (`6847f928c`)
- 分页&其他调整 (`2368de310`)

### 📌 2019.12.29

_Generated on 2026-08-19._

#### Summary
- Generated from `075c02d09` to `e691a42fe`.
- Included 92 commits across 1 categories: Other 92.

#### Other Changes
- 调整 (`e691a42fe`)
- remove dbs module (`4fe97cd77`)
- rtmp (`4be62d4a4`)
- gateway (`a4f92d1b4`)
- token (`d47768d6c`)
- user (`7983151d3`)
- hystrix (`81899f6b9`)
- okhttp3 (`6885fbd37`)
- 调整docker-compose位置 (`d086de46e`)
- node (`c780c5ec7`)
- gateway 路由配置 token和register (`adacfe502`)
- 调整权限接口 (`ce476518c`)
- 换位置 (`b6edfeb8d`)
- token list (`4861e3b3f`)
- 调整token dbs逻辑功能 (`892a65cce`)
- auth实现类调整 (`703ffdc92`)
- 调整@注解 (`5d9f6f888`)
- 实体类和密钥工具类调整 (`3cbb4ecf7`)
- 配置RedisTemplate&RedisUtils (`f00685770`)
- 调整token&user表结构 (`30246063c`)
- 调整逻辑 (`79ac98c36`)
- +网关统一权限管理 (`a0269b226`)
- 完成第一版dc3-auth功能逻辑 (`639f07a62`)
- 提取Redis缓存配置到common-core (`dda82cedb`)
- . (`086894e59`)
- . (`bd926a2b2`)
- . (`810d90db8`)
- . (`86a0f720f`)
- . (`b5d320816`)
- . (`363fdd66d`)
- change (`ce9df1ebf`)
- 优化接口 (`e45d97056`)
- 优化接口 (`d777a68e5`)
- postman (`7d96648a3`)
- 简化配置和更新缓存 (`fd2ced388`)
- dbs mongodb->mongo (`1ca73acc4`)
- . (`289b7d678`)
- mariadb -> mysql:8.0 java - > java:1.8.0_221 update docker (`d03c9e50c`)
- 重新构造 dc3-base docker image (`9364136c8`)
- Create maven.yml (`3c23d4789`)
- 调整 (`14ce7c01b`)
- . (`58a011a89`)
- 去掉cmdtool，使用hutool工具类 (`e4dcbe902`)
- 更新rtmp,新面孔 (`9d8e65eee`)
- 日常调整 (`a96e40fcc`)
- 日常调整 (`5242d356f`)
- . (`72ebe7dd2`)
- . (`8e3e303b6`)
- . (`1ed243f67`)
- . (`98ac334db`)
- . (`6223ec384`)
- token (`a2e75c38a`)
- all post (`8343acf47`)
- base update (`c558b59e5`)
- log & register (`9da074fc0`)
- no author (`f1c5dcf2a`)
- . (`6ce7b9e5a`)
- docker java ops (`d32146365`)
- . (`65ce9d897`)
- . (`9731d3d54`)
- rtmp http client (`73b959ba0`)
- rtmp http client (`988badeac`)
- rtmp http client (`e574dc54e`)
- page (`6c37db3b4`)
- . (`993a5a433`)
- update (`295feb0ab`)
- 更新 (`6dc994a24`)
- . (`dfb9834bf`)
- . (`147a5fc7c`)
- . (`7ce49f62e`)
- . (`62c507f5a`)
- . (`e58c8c3e1`)
- . (`f6d570b59`)
- . (`428db7445`)
- 调整 (`af8e6aa5b`)
- 调整 (`aa94c99e7`)
- . (`f0ba567ae`)
- auth (`456d809e4`)
- 调整配置 (`e3b995cb4`)
- CI (`c4955bafd`)
- 配置监控中心反向代理 (`80785a73c`)
- . (`1181772b4`)
- . (`7562838d1`)
- Bump netty-all from 4.1.36.Final to 4.1.42.Final (`e4eba92b2`)
- 权限配置 (`04b643e81`)
- . (`1470c19cd`)
- 添加代码 (`4c0a685a8`)
- . (`c64d7c9e4`)
- 端口调整 (`9dab0ce5c`)
- 设备驱动&配置属性 (`79df5bbe6`)
- 提交 (`cb2a29ea0`)
- TAG:删除版本控制 (`81b80e1d4`)

### 📌 2019.9.29

_Generated on 2026-08-19._

#### Summary
- Generated from `dc857123f` to `075c02d09`.
- Included 86 commits across 1 categories: Other 86.

#### Other Changes
- . (`075c02d09`)
- 网关适配 & 添加网关主页 (`382c4b1f1`)
- 规范增删改查接口 (`ff12c0871`)
- 服务接口基类 (`eaa982675`)
- RTMP添加任务 (`6b68c6857`)
- RTMP添加任务 (`2245125b2`)
- . (`12f4db48e`)
- 去ZipKin (`5451671be`)
- api (`1cc7d5bca`)
- 调整cmd (`faf76c9da`)
- 去ZipKin (`6aa440a14`)
- 队列安全问题，打算去掉自建队列，改使用RabbitMQ实现数据传递 (`f465049f5`)
- .... (`90b02b340`)
- Merge branch 'dev' (`4cdccd895`)
- 调整collect位置 (`ec356e46b`)
- zipkin (`c9f7df7f3`)
- swagger2测试 (`db4c5cd8e`)
- swagger2测试 (`1cc202f28`)
- 调整包名，用于适配FeignApi能被扫描加载 (`9c5b7568d`)
- page (`dde405103`)
- rtmp重连bug (`92d27f358`)
- 调整Response为泛型 (`b14ae7216`)
- 调整Response为泛型 (`1398c408c`)
- 调整接口实现继承逻辑 (`ce97b6712`)
- 升级FastJson & Wia Gateway (`86d5037bd`)
- 调整表结构 (`a3f12674a`)
- unified format (`b3d9b9709`)
- . (`be1522234`)
- . (`f5bf8230c`)
- . (`6c0453aa2`)
- . (`e8f47dfa4`)
- commit (`4f39710bc`)
- 提交 (`29c35594e`)
- complete cache config (`3ef9972b4`)
- commit redis cache config (`cac61610a`)
- 提交MongoDB配置 (`3ff02f8b9`)
- complete wia datagram reveice (`3a050b3a7`)
- update (`0bcc74d42`)
- 先提交一部分，回家再继续弄 (`b049a2f76`)
- the client side adds "message key" logic (`5b1d9df50`)
- adjust code style (`bb228f322`)
- gateway (`be91aa686`)
- adjust mock client (`261292854`)
- mock client (`81d353920`)
- 修复 sqlite 死亡提示 (`5a2f09b02`)
- 修复 sqlite 死亡提示 (`6682b0536`)
- . (`3863214b5`)
- 设置关闭微服务之间的api版本控制逻辑 (`4679d609e`)
- . (`89f3fd5f7`)
- . (`75800221d`)
- . (`0c7c0c043`)
- . (`3a232fa35`)
- . (`09e90a630`)
- . (`3e81b9054`)
- . (`08898861d`)
- Update .travis.yml (`01f1d65ae`)
- Update .travis.yml (`f9b656168`)
- Update .travis.yml (`d2d5455f1`)
- Delete CODE_OF_CONDUCT.md (`84164cf7f`)
- . (`3fb9b04fa`)
- . (`bc032d5a7`)
- . (`5abb179a4`)
- . (`1ad69a51c`)
- . (`773a4eb62`)
- . (`55f584386`)
- api接口添加版本号 (`251582e6e`)
- . (`62d664459`)
- . (`134345ef1`)
- 去掉web端源码，后期需要迁移至单独工程中 (`ed1f20525`)
- 日常调整 (`4931200de`)
- api接口安全校验 (`e1ed1e77a`)
- Create CODE_OF_CONDUCT.md (`845b98f84`)
- . (`aa4b18dcb`)
- nginx-rtmp打包到基础镜像中去 (`fae89dce2`)
- ffmpeg打包到基础镜像中去 (`0db2277a1`)
- 调整docker版本 (`50aac8a2a`)
- iot dc3 docker base images (`cc295bdc2`)
- . (`dd7dee9d2`)
- 修改工程打包内容，调整dockerfile (`a6fa50873`)
- . (`1a6c24fe1`)
- update readme (`10cf40fde`)
- 配置docker网络，调整架构图 (`e4ed03107`)
- 提交注册中心Host设置 (`8b736c9f4`)
- 更新readme (`d2b845c7c`)
- docker config‘ (`c5ed740ff`)
- 调整docker设置 (`afdad55ab`)

### 📌 2019.6.30

_Generated on 2026-08-19._

#### Summary
- Generated from `2203ccff5` to `dc857123f`.
- Included 83 commits across 1 categories: Other 83.

#### Other Changes
- 添加docker配置 (`dc857123f`)
- 优化pom配置 (`2e2c59906`)
- 明文添加加密 (`722a955bc`)
- pom (`41f495dc5`)
- 设置数据库服务 (`49b9949e4`)
- 调整注册中心和管理中心 (`baba7bba9`)
- 调整 (`7bcbafeae`)
- 版本调整和设备组数采程序线程和队列的建立 (`87a8deb56`)
- 资源服务器 (`a0f715e0d`)
- 提交Wia数据相关类 (`333db7000`)
- 消息组件模块调整 (`365c9a6a3`)
- 添加消息组件模块配置 (`c106a6afe`)
- 结构调整 (`b1dd422eb`)
- 提交plcs7功能 (`313d338b6`)
- vue upgrade (`51a88873c`)
- readme (`45a6b1430`)
- 调整 (`d3a56e164`)
- 调整 (`4325b7563`)
- 调整 (`64d34a4df`)
- client端 (`544194dfa`)
- 完善rtsp-rtmp服务功能 (`88a02274e`)
- 调整api包层级关系，解决feignclient同component的冲突问题 (`74e8d6999`)
- 添加rtmp数据操作逻辑 (`0abe995d2`)
- 删除webapp生产配置文件 (`b59773704`)
- rtmp添加新字段 (`f8c24ee3f`)
- 调整配置文件和依赖 (`bc60358c8`)
- 添加说明 (`cf3b5d1c0`)
- 日常提交 (`e66ec2e2f`)
- 设备端管理和数据采集调整 (`07ded56aa`)
- 调整配置文件和依赖 (`0bc207a07`)
- readme (`2e840fcaf`)
- badge (`bf3a98640`)
- badge (`f58e4101b`)
- 添加Apache License-2.0开源协议 (`87897c347`)
- Rtsp转Rtmp监听线程调整 (`4983dcdfd`)
- Rtsp转Rtmp监听线程调整 (`2f832ea01`)
- GUID工具类 (`b1a35252f`)
- 设备管理 (`739aa9074`)
- web服务的监控提交&调整Redis连接 (`87b914ecf`)
- 调整pom配置 (`cbc6f85bd`)
- Spring Cloud Admin 监控信息配置 (`8c0bdd8ed`)
- 添加服务状态监控 Spring Cloud Admin 模块 (`a18ca57aa`)
- 添加描述信息 (`79eff289d`)
- 添加Actuator监控 (`874eb7bc2`)
- 提交spring cloud gateway配置 (`28872c6d9`)
- 熔断 (`1959ed4a9`)
- 提交负载和熔断修改 (`f86448ba1`)
- 提交rtsp->rtmp例子 (`0052818b1`)
- 提交build脚本 (`72882fbb4`)
- postman 用于api接口测试 (`072b59c3c`)
- 设备端管理服务提交 (`408f314a0`)
- 更新springboot版本到Greenwich,并调整兼容性问题 (`f8e7b8926`)
- 添加加密算法AES\RSA (`c166257dc`)
- 添加测试 (`c5be763b6`)
- 添加对称加密工具 (`e9cff2838`)
- 添加西门子S7 PLC支持 (`24e8935c1`)
- 初始化数据 (`744796ea7`)
- 创建数据库脚本 (`fdedb6568`)
- 初始化Opc\Opc Ua\Plc S7数据处理服务 (`90e88c1a9`)
- 添加测试 (`08b996343`)
- 升级spring-boot版本到2.0最新版本 添加mybatis plus支持 调整依赖版本，进行统一管理 (`9ffe3adc4`)
- 配置网关和rtmp服务 (`eb4ffdf93`)
- socket处理 Netty (`be2729633`)
- socket处理 Netty (`65c31efcc`)
- 提交命令启动器 (`3f1903566`)
- 结构调整&添加说明教程 (`e85254dc4`)
- 调整说明 (`9d510ea4a`)
- 更新 (`6aef368b8`)
- 日常调整 (`bc222535c`)
- . (`4ad1ed2ed`)
- add public (`855780b38`)
- . (`56f57424f`)
- 更新 (`9abf6b332`)
- 更新 (`566c165f2`)
- 配置express (`b981349f6`)
- 配置express (`259cad8c3`)
- 添加vue打包配置 (`581f0ebe4`)
- express (`a999cf0e2`)
- data-helper 初始化 (`5c62267e1`)
- data-helper (`b0d44d077`)
- 调整配置 (`71be7110c`)
- 添加网关服务和配置 (`e3a0b0f9d`)
- 调整用户信息和版权信息 (`6fc4b23bb`)

### 📌 2018.10.30

_Generated on 2026-08-19._

#### Summary
- Generated from `0878f2ad8` to `2203ccff5`.
- Included 1 commits across 1 categories: Other 1.

#### Other Changes
- update jackson-databind (`2203ccff5`)

### 📌 2018.9.25

_Generated on 2026-08-19._

#### Summary
- Generated from `repository start` to `0878f2ad8`.
- Included 18 commits across 2 categories: Security 1, Other 17.
- Highlights: update dependencies defined in these manifest files have known security vulnerabilities webpage/package-lock.json.

#### Security
- update dependencies defined in these manifest files have known security vulnerabilities webpage/package-lock.json (`98701931e`)

#### Other Changes
- pic (`0878f2ad8`)
- icon update (`a1af4976b`)
- 调整位置 (`f5126b0b0`)
- interface (`b19feff1b`)
- interface (`034d23090`)
- architecture (`57d9a6961`)
- architecture (`6cbd4e358`)
- architecture (`65e9414f8`)
- architecture (`a8aab4f2e`)
- architecture (`038dcfc28`)
- update (`c9cbcb312`)
- init step (`1d5ca36d5`)
- view (`54a983f35`)
- view (`cf8df1113`)
- english (`489be6570`)
- build shell (`99638cba6`)
- init iot-dc3 (`3b392b9a2`)

</details>
