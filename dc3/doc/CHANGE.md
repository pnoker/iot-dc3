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

### 📌 2025.9.3

- Dependency upgrades
- Add Japanese and Vietnamese introductions

### 2025.9.2

- Dependency upgrades
- Stability improvements
- Remove Hutool
- Remove Undertow
- Update container image dependencies

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

</details>
