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

#### Features
- update docker-ci.yml (`bf64fd7fd`)
- update version (`5afd4c904`)

#### Documentation
- add Japanese and Vietnamese READMEs with language links (`40e1bd233`)

### 📌 2025.9.13

_Generated on 2026-08-19._

#### Summary

- Generated from `07a2a15b0` to `f40016fb7`.
- Included 73 commits across 6 categories: Features 9, Refactoring 3, Documentation 8, Build 5, CI 7, Other Changes 41.
- Most active scopes: data-storage(1), storage(1).
- Highlights: update RedisPointValueDO.java; storage: add PostgreSQL storage service; add example usage to Docker CI
  workflow output; feat: add release notes generation to Docker CI workflow; feat: enhance Docker CI workflow with
  detailed logging and summaries.

#### Features
- update RedisPointValueDO.java (`f8346c180`)
- **storage**: add PostgreSQL storage service (`563610e31`)
- add new classes PointValueBoolManager, PointValueDoubleManager, PointValueFloatManager and 21 more plus updates (31 files) (`11811cc25`)
- add new class PointQueryBO plus updates (17 files) (`b8b1b444a`)
- update code across modules (28 files) (`6c17edd3d`)
- add example usage to Docker CI workflow output (`5992c3b4c`)
- feat: add release notes generation to Docker CI workflow (`ed263d3d8`)
- feat: enhance Docker CI workflow with detailed logging and summaries (`a97ee0914`)
- update code across modules (905 files) (`8ae1f4aa6`)

#### Refactoring
- move/rename code in dc3 (3 of 4 files) (`28c66ec8f`)
- move/rename code in CI (3 of 6 files) (`4e3629df4`)
- **data-storage**: refactor point value storage and remove raw time field (`003148399`)

#### Documentation
- update documentation (2 files) (`f40016fb7`)
- update documentation (2 files) (`d03c6d327`)
- update documentation (2 files) (`777185e06`)
- add documentation and supporting files (4 files) (`42205e87e`)
- update USAGE.md (`49d2834ad`)
- update USAGE.md (`69013d836`)
- update documentation (3 files) (`11d609180`)
- update TITLE.md (`c4478851b`)

#### Build
- bump dc3.common.api.version 2025.9.1 to 2025.9.2, dc3.api.auth.version 2025.9.1 to 2025.9.2, dc3.api.data.version 2025.9.1 to 2025.9.2 across 36 module POMs (`b244b1132`)
- update build and configuration files (23 files) (`f004ce521`)
- bump dc3.common.api.version 2025.6.5 to 2025.6.6, dc3.api.auth.version 2025.6.5 to 2025.6.6, dc3.api.data.version 2025.6.5 to 2025.6.6 across 37 module POMs (`a796ef9d3`)
- update build and configuration files (99 files) (`d0f034f00`)
- bump dc3.common.api.version 2025.6.4 to 2025.6.5, dc3.api.auth.version 2025.6.4 to 2025.6.5, dc3.api.data.version 2025.6.4 to 2025.6.5 across 37 module POMs (`44142686e`)

#### CI
- update CI workflow docker-ci.yml (`1b79c4310`)
- update CI workflow docker-ci.yml (`f723765c8`)
- update CI workflow docker-ci.yml (`be04ea7ab`)
- update CI workflow docker-ci.yml (`0ab5c14da`)
- update CI workflow docker-ci.yml (`87ea293d7`)
- update CI workflow docker-ci.yml (`362095ec0`)
- trigger Docker CI on dc3.release.* tags instead of dc3.develop.* (`200e69976`)

#### Other Changes
- update README.md; update README.zh.md; add dev.env.sh (`140f0dcc9`)
- update code across modules (5 files) (`a877fcf82`)
- update multiple modules (30 files) (`7323ab746`)
- update RedisPointValueDO.java (`fd3d01b1a`)
- remove hutool (`0acaf7be0`)
- Create codeql.yml (`3b778e9cd`)
- update docker-ci.yml; update CHANGE.zh.md; update TITLE.zh.md (`84f33ac9a`)
- update multiple modules (10 files) (`d48f6454e`)
- update multiple modules (4 files) (`e86425f1a`)
- update docker-ci.yml; update TITLE.md (`fa817d179`)
- update docker-ci.yml; update TITLE.md (`b67b2367e`)
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
- update OkHttpConfig.java (`a222ac166`)
- update multiple modules (48 files) (`df2d32723`)
- update LICENSE-AGPL.txt; update README.md; update README.zh.md (`be4ea3869`)
- update code across modules (1161 files) (`dfc9d3309`)
- move LICENSE.zh.txt to LICENSE-ZH.txt; add LICENSE.txt (`2ceeeaea1`)
- add LICENSE-AGPL.txt (`b4904be10`)
- update COPYRIGHT; move LICENSE.zh.md to LICENSE.zh.txt; update logo-blue.zh.png (`6e17da1f0`)
- Update LICENSE.zh.md (`fc4ce3113`)
- Update and rename LICENSE to LICENSE.zh.md (`52f90db57`)
- Update README.zh.md (`4020103a7`)
- Update README.md (`4bcaee8c1`)
- add Chinese README (`716997630`)
- add Chinese README (`82d90e590`)
- add Chinese README (`ed7f5a848`)

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
- Included 73 commits across 9 categories: Features 16, Bug Fixes 4, Performance 1, Refactoring 19, Documentation 2, Build 5, CI 12, Chores 11, Other Changes 3.
- Most active scopes: docker(5), Dockerfile(4), config(2), docker-ci(2), dc3-common-auth(1), data(1).
- Highlights: update PointValueDO.java; update logback.xml; remove DeviceEventController.java; remove
  DriverEventController.java; add APM settings to Dockerfile and compose files; docker: add redis-exporter service to
  docker-compose; correct file paths and add GC logging configuration.

#### Features
- update (`0db69725d`)
- update PointValueDO.java; update logback.xml (`7102a84de`)
- update dc3-common-data code (7 files) (`2afa57984`)
- update code across modules (922 files) (`4b69f0b2e`)
- update multiple modules (14 files) (`f1f6e78f3`)
- add code to dc3-common-data (10 files) (`ac20257dc`)
- remove DeviceEventController.java; remove DriverEventController.java (`ed400b170`)
- remove mongo module (`b79504cce`)
- add new classes PointValueBoolManager, PointValueByteManager, PointValueDecimalManager and 31 more plus updates (55 files) (`62ed730c2`)
- update dev.env (`b4321b045`)
- adjust time types (`1b0dd81d8`)
- adjust table schema (`4db89d7d4`)
- add APM settings to Dockerfile and compose files (`18b0fb079`)
- adjust logging (`bafbc3e13`)
- update dc3-common-gateway code (6 files) (`e654ff7e5`)
- **docker**: add redis-exporter service to docker-compose (`8a81ed6a9`)

#### Bug Fixes
- correct file paths and add GC logging configuration (`39cbf3879`)
- fix test database name in JDBC URL (`1ec1e2d60`)
- fix ENTRYPOINT path in Dockerfile (`05b40b226`)
- **docker**: correct COPY command in Dockerfile (`538a3d4f3`)

#### Performance
- reduce JVM max heap from 1024m to 512m (`fcaa8fab4`)

#### Refactoring
- move/rename code in dc3-common-data-postgres (5 of 5 files) (`1d170be4e`)
- **dc3-common-auth**: remove unnecessary Redis dependency (`82e9a8157`)
- replace LocalDateTime.now() with LocalDateTimeUtil.now() for consistency (`9455aa281`)
- update APM service port to 9300 and remove exposed port 8200 (`897072945`)
- **Dockerfile**: reorder JAVA_OPS flags for consistency (`16f1bc40e`)
- **config**: tune module settings in test configuration (`bf0472fab`)
- **config**: extend test configuration to more services (`3a4af6d2e`)
- replace hardcoded URIs with environment variables (`41d6a3897`)
- drop ${PARAMS} from Dockerfile (`41a9f3ba0`)
- **Dockerfile**: switch Dockerfiles to entrypoint.sh (`2a93dcff8`)
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
- add code comments and improve docstrings (`07a2a15b0`)
- update module descriptions in pom.xml files for clarity (`30fd92df0`)

#### Build
- update dc3-common-manager POM (`54c376d0d`)
- update build and configuration files (8 files) (`a2389c163`)
- add logstash-logback dependency and update logging configuration (`27a54faa7`)
- remove local build config and add monitoring dependencies (`87d85791b`)
- remove redundant repository configurations from pom.xml (`e557ea6e3`)

#### CI
- update CI workflow docker-ci.yml (`334f05547`)
- update CI workflow docker-ci.yml (`c61b810a9`)
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
- remove code in dc3-common-data-postgres (66 files) (`d8a010c41`)
- remove code in dc3-common-data (7 files) (`e53a2d35a`)
- remove code in dc3-common-auth (5 files) (`5147677da`)
- remove code in dc3-common-auth (25 files) (`edbc8936e`)
- add podman scripts for aliyun services and update logback config (`bf9a1b233`)
- set NODE_ENV=test across Dockerfiles and compose files (`a00817659`)
- add prometheus endpoint to management configuration (`9a3c58e27`)
- **docker**: standardize string quotes and add new compose files (`4495d279a`)
- update version to 2025.2.5 across multiple files (`9be2f5f0d`)
- update project version to 2025.2.4 across all modules (`24faae2fd`)
- update container configuration (4 files) (`6ee211945`)

#### Other Changes
- update dev.env (`b43b2f979`)
- Revert "feat: adjust time types" (`721a642e3`)
- update code across modules (933 files) (`7bdb0fbbc`)

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
- Included 13 commits across 2 categories: Features 10, Chores 3.
- Highlights: update PointValueVO.java; update PointValue.java; update PointValueBO.java.

#### Features
- update version metadata (`5bc7e9a07`)
- update version metadata (`25d2614da`)
- update version metadata (`e55f4c593`)
- update version metadata (`6549bff8e`)
- update version metadata (`1a5bb878f`)
- update dc3-common-manager code (26 files) (`2c1baa789`)
- version 2024.3.10 (`42e60f1b7`)
- update code across modules (10 files) (`4486fe9b3`)
- update PointValueVO.java; update PointValue.java; update PointValueBO.java (`20df53f96`)
- update code across modules (18 files) (`18787291e`)

#### Chores
- update docker-compose-grafana.yml (`a08ad84ce`)
- update docker-compose-grafana.yml (`6a29baae4`)
- update container configuration (2 files) (`05087a932`)

### 📌 2024.12.29

_Generated on 2026-08-19._

#### Summary

- Generated from `a706baa07` to `1b2e52aa4`.
- Included 34 commits across 6 categories: Features 26, Bug Fixes 1, Refactoring 1, Build 3, Chores 2, Other Changes 1.
- Highlights: update DeviceMapper.xml; update PointMapper.xml; update ProfileMapper.xml; adjust database connection
  parameters and add `&stringtype=unspecified`; adjust builder and permission encoding logic; update
  DataInitRunner.java; update ManagerInitRunner.java; update DataTopicConfig.java; update DriverTopicConfig.java.

#### Features
- update DeviceMapper.xml; update PointMapper.xml; update ProfileMapper.xml (`1b2e52aa4`)
- update code across modules (4 files) (`756e5e202`)
- update code across modules (9 files) (`9ded01982`)
- add new class CodeUtil plus updates (884 files) (`91f3c314e`)
- add new class UserHeaderUtil plus updates (26 files) (`265fcfe0b`)
- adjust database connection parameters and add `&stringtype=unspecified` (`84fdb5fc7`)
- adjust builder and permission encoding logic (`85370fd4d`)
- update DataInitRunner.java; update ManagerInitRunner.java (`981c6fe2b`)
- update code across modules (9 files) (`4a668711c`)
- update code across modules (25 files) (`765b9aaa6`)
- update DataTopicConfig.java; update DriverTopicConfig.java (`e0b6c9042`)
- update multiple modules (7 files) (`992d1a39f`)
- update application-dev.yml (`5ad0d6a6c`)
- data postgresql (`8014fa6a0`)
- manager postgresql (`4d62e6d13`)
- auth postgresql (`50160796e`)
- update TDEngineRepositoryMapper.xml (`83634a6a2`)
- update code across modules (4 files) (`81786744e`)
- update MybatisUtil.java (`fbd0ce12f`)
- update mybatis generator (`355f54b60`)
- update email (`167615e20`)
- update MybatisGenerator.java; update MybatisUtil.java; update docker-compose-dev.yml (`fb45e9ab1`)
- update multiple modules (4 files) (`672409604`)
- make standalone and merged modules both runnable (`1f9bae4a9`)
- update pom version (`fc350befa`)
- upgrade Spring Boot version (`af68b5da2`)

#### Bug Fixes
- auth postgresql (`562a1faaa`)

#### Refactoring
- relocate classes and update package references (58 files) (`747557e48`)

#### Build
- bump dc3.common.api.version 2024.3.7 to 2024.3.9, dc3.api.auth.version 2024.3.7 to 2024.3.9, dc3.api.data.version 2024.3.7 to 2024.3.9 across 42 module POMs (`21d54e249`)
- update build and configuration files (15 files) (`9eda1ec34`)
- bump dc3.common.api.version 2024.3.3 to 2024.3.7, dc3.api.auth.version 2024.3.3 to 2024.3.7, dc3.api.data.version 2024.3.3 to 2024.3.7 across 42 module POMs (`d52fdb0da`)

#### Chores
- update docker-compose-dev.yml (`4ac50abab`)
- update container configuration (15 files) (`0d41d0bcb`)

#### Other Changes
- update multiple modules (28 files) (`3beccb234`)

### 📌 2024.8.31

_Generated on 2026-08-19._

#### Summary

- Generated from `7d8d18750` to `a706baa07`.
- Included 6 commits across 2 categories: Features 5, Documentation 1.
- Highlights: fix limiter loading issue; extract gateway code into common module; merge common and api code into main
  project.

#### Features
- update code across modules (128 files) (`a706baa07`)
- update (`927b6f07c`)
- fix limiter loading issue (`5b1368600`)
- extract gateway code into common module (`948d049e0`)
- merge common and api code into main project (`3ae07be80`)

#### Documentation
- update README.md (`67fae60e7`)

### 📌 2024.6.24

_Generated on 2026-08-19._

#### Summary

- Generated from `0b481ff63` to `7d8d18750`.
- Included 117 commits across 7 categories: Features 53, Bug Fixes 13, Refactoring 3, Documentation 1, Build 8, Chores 20, Other Changes 19.
- Highlights: extract manager common code into common module; extract data service common code into common module;
  extract auth service common code into common module; move cache file location; move cache file location.

#### Features
- common module cleanup (`7d8d18750`)
- extract manager common code into common module (`8efc1605e`)
- extract data service common code into common module (`4f3d98a3f`)
- adjust common modules (`968b7b1b0`)
- extract auth service common code into common module (`2ba670d73`)
- config makefile (`e774e3553`)
- update code across modules (4 files) (`5ec1cc689`)
- fix dependency issues (`45b39cd1b`)
- 2024.3.2 (`21dca0a5a`)
- move cache file location (`e69403753`)
- move cache file location (`003c98e7c`)
- improve batch import (`ead953ed0`)
- update code across modules (45 files) (`1303bec3b`)
- fix gRPC numbers defaulting to 0 (`2acb56292`)
- update multiple modules (99 files) (`b46d843da`)
- improve notification mechanism (`d8b583f83`)
- clear device connection cache (`7770d760b`)
- add device & point metadata update event (`8ea7a4493`)
- improve driver registration (`ec278136e`)
- improve driver device list (`6999ede64`)
- update NettyServerHandler.java (`9d9ece525`)
- adjust JAVA_OPTS (`f3ff0cbf3`)
- resolve dependency incompatibilities (`5b3c1b2c7`)
- update code across modules (21 files) (`a36ca2e98`)
- notify driver configs individually instead of via profile template (`3ff57d383`)
- rework metadata sync for driver/device/point configs; support bulk and paged config cache loading by driver type (`b3ea1fe7f`)
- rework driver data sync and registration; add local data cache (`13761340a`)
- add driver local cache; format gRPC stubs (`6282caf90`)
- improve driver read-task logic and collection value utilities (`0288346cb`)
- adjust driver utility types (`4e882f048`)
- 1.Fail fast if transport is null, check for null on close 2.Log exceptions when error occurs on send (`c555316d8`)
- fix a pagination query bug (`6532681a4`)
- upgrade dependencies (`d370a66b9`)
- upgrade dc3-parent to 2024.1.3 (`5e75af049`)
- split point history endpoints; debug and improve device history curves (`a63fa1a03`)
- update code across modules (337 files) (`7997d382d`)
- springboot3 (`214d82ed9`)
- update .dockerignore; update Dockerfile.dc3 (`6d873e7ec`)
- update code across modules (102 files) (`b16213a47`)
- tune Modbus TCP test collection cadence (`0b2e2c538`)
- switch to JDK 17 (`026f44cc8`)
- update update.sh; update docker-compose-test.yml; add Dockerfile.mqtt (`b24189360`)
- update .dockerignore (`f1441bf02`)
- update update.sh; update docker-compose-test.yml (`c14739622`)
- update application-pre.yml; update application-pro.yml; update application-test.yml (`d2d079021`)
- automation scripts (`eb4b95ae8`)
- JDK 17 compatibility adjustments (`40f1fb144`)
- update dc3-center-data code (4 files) (`20fd1bb59`)
- standardize code style (`4d8764cf1`)
- standardize code style (`ca638d311`)
- rabbitmq controller (`ba8f90046`)
- update dc3-center-data code (11 files) (`9a2debd49`)
- update dc3-center-data code (8 files) (`2d2ef8431`)

#### Bug Fixes
- updateTime to operateTime (`af8ff341f`)
- adjust default API gateway filters (`babbee6e7`)
- fix API gateway proxy issue (`7080d8353`)
- fix interface 404 errors (`17cd06868`)
- update code across modules (12 files) (`9a7096af6`)
- improve notification mechanism (`a043302d6`)
- print logs and errors in English to avoid mojibake (`be953b1d6`)
- update GrpcDriverAttributeBuilder.java; update GrpcDriverAttributeConfigBuilder.java; update GrpcPointAttributeBuilder.java (`fcbb2ef7d`)
- add copyright (`410d1ba34`)
- remove warnings (`65f07cef8`)
- fix device profile loss on enable/disable breaking collection (`3bb05b80f`)
- update DeviceStatusHistoryMapper.xml; update DriverStatusHistoryMapper.xml (`581080cd7`)
- update pom.xml; update DeviceEventReceiver.java; update DriverEventReceiver.java (`6ca5ab733`)

#### Refactoring
- move/rename code in dc3-center-manager (3 of 3 files) (`c5a8bc557`)
- move/rename code in dc3-center-manager (6 of 9 files) (`4dc8226ab`)
- move/rename code in modules (8 of 24 files) (`897b6f47b`)

#### Documentation
- update README.md (`09283a57d`)

#### Build
- update build and configuration files (21 files) (`d956709a0`)
- update pom.xml POM (`522b2264e`)
- update pom.xml POM (`7a685d8eb`)
- update build and configuration files (5 files) (`f80dfeeaf`)
- update build and configuration files (5 files) (`627f140a5`)
- update build and configuration files (24 files) (`d8ac2f52b`)
- update build and configuration files (17 files) (`2f5fa1165`)
- update pom.xml POM (`4d70e2cdb`)

#### Chores
- update tag.sh script (`4a4e2ae5f`)
- remove code in dc3-center-ekuiper (52 files) (`939b5a1ed`)
- update tag.sh script (`f73b8ad11`)
- update container configuration (2 files) (`4844ee37c`)
- remove code in dc3-center-manager (124 files) (`775496448`)
- update update.sh script (`007931f23`)
- update update.sh script (`0fc938bde`)
- remove code in modules (26 files) (`c6ee2c61d`)
- remove code in dc3-center-manager (5 files) (`490996182`)
- update docker-compose-web.yml (`a62550145`)
- update docker-compose-web.yml (`f0eafea1b`)
- update container configuration (2 files) (`803da68f5`)
- update docker-compose-test.yml (`6dbc33704`)
- update container configuration (2 files) (`869a16c4a`)
- update docker-compose-influx.yml (`623186e19`)
- update container configuration (2 files) (`e42690757`)
- update container configuration (2 files) (`c783e6a26`)
- update update.sh script (`93c9def27`)
- update docker-compose-option.yml (`a0ed7f91b`)
- update container configuration (4 files) (`f14996ea2`)

#### Other Changes
- Create docker-publish.yml (`70ecf4ebe`)
- rework queries (`bf51660f8`)
- update TopicServiceImpl.java (`570832293`)
- update TopicController.java (`0bdd418a7`)
- update TopicController.java; update TopicQuery.java (`96a042094`)
- update TopicQuery.java; update TopicService.java (`19483c97c`)
- update topic interfaces (`68d5ccb0d`)
- add topic interfaces (`a3623c1a9`)
- fix index out-of-bounds bug (`872e9b6bb`)
- adjust device/driver/point dashboard interfaces (`66a861748`)
- add dc3-center-ekuiper (`6d42e0971`)
- fix index out-of-bounds bug (`f0197839f`)
- fix device/driver/point dashboard interface bugs (`87994d19b`)
- implement TDengine and InfluxDB datasource methods (`5782b1be8`)
- extract RabbitMQ code into common module (`f8cead6d3`)
- rework Prometheus API integration (`a49941154`)
- set call and connect timeouts to 60s (`efcd0a1ef`)
- set read timeout to 60s (`13064219f`)
- Create FUNDING.yml (`ce7fd6a0a`)

### 📌 2024.3.31

_Generated on 2026-08-19._

#### Summary

- Generated from `69327e826` to `0b481ff63`.
- Included 76 commits across 7 categories: Features 46, Bug Fixes 5, Refactoring 3, Documentation 1, Build 2, Chores 2, Other Changes 17.
- Highlights: define multi-datasource interfaces; temporarily disable login restriction; multi-datasource framework;
  update application-test.yml; update docker-compose.yml; update pom.xml; add OpenAPI interface examples.

#### Features
- define multi-datasource interfaces (`27f190120`)
- temporarily disable login restriction (`36e163d0b`)
- multi-datasource framework (`c7ad54b64`)
- format code (`e5f8c3d6a`)
- update dc3-center-manager code (4 files) (`dd190cd43`)
- update application-test.yml; update docker-compose.yml; update pom.xml (`a313dcb1e`)
- forward data to mqtt (`14f723f95`)
- adjust enums (`74c03613a`)
- adjust enums (`b1f99fd90`)
- adjust enums (`150a79e71`)
- add OpenAPI annotations (`5d9c48576`)
- add OpenAPI interface examples (`aaa2ad77c`)
- add OpenAPI interface examples (`cc18d86d6`)
- rework enable-status enum as pattern for remaining enums (`fe5360ca7`)
- update code across modules (14 files) (`ef9db2e89`)
- update TokenController.java; update AuthenticGatewayFilter.java (`031746d8f`)
- update code across modules (176 files) (`7a8685b49`)
- update dc3-center-auth code (10 files) (`8c618bcc3`)
- update MybatisGenerator.java; update MybatisGenerator.java; update MybatisGenerator.java (`d4b64d37c`)
- update multiple modules (15 files) (`f6f3ce40e`)
- add new class EveryMinuteJob plus updates (18 files) (`1f299ac50`)
- add scheduled jobs to data and manager services (`7cd591d7f`)
- switch test env to single node; drop multi-datasource config (`039314596`)
- update code across modules (56 files) (`687eaad1a`)
- scheduling adjustments (`c8196ade4`)
- improve Dockerfile (`85d31d21d`)
- unify JSON serialization configuration (`5c92f7c03`)
- adapt device history data interfaces (`750f2eff3`)
- compatibility fixes (`830b2d9e0`)
- parse and query device/driver status (`92698221f`)
- fix permission module compatibility (`51185ccb9`)
- fix RabbitMQ message serialization (`6580da1b6`)
- move register-center logs (`2dde35911`)
- fix bugs and adapt frontend interfaces (`1986430e9`)
- update code across modules (13 files) (`15fc15c09`)
- unify bean conversion (`2215a415e`)
- unify tenant resolution (`b67b44feb`)
- update code across modules (15 files) (`585df3883`)
- update code across modules (26 files) (`68764d92b`)
- update code across modules (23 files) (`3c35594d3`)
- move driver SDK into common to simplify dependencies (`5fd0bf240`)
- update code across modules (144 files) (`211f5ed57`)
- add new class OpenAPIConfig plus updates (4 files) (`38c42a726`)
- update code across modules (29 files) (`f4e909af1`)
- update code across modules (130 files) (`48892a2ec`)
- improve detection attribute completeness (`7e96340fa`)

#### Bug Fixes
- update DeviceDO.java (`0b481ff63`)
- update dc3-center-data code (23 files) (`b60b2185c`)
- update PointValueServiceImpl.java (`80d2b1d78`)
- update dc3-center-data code (8 files) (`6a1dc9a78`)
- fix multi-datasource dependencies and startup (`6f1078730`)

#### Refactoring
- move/rename code in dc3-center-manager (4 of 5 files) (`9a5646cef`)
- move/rename code in modules (33 of 59 files) (`994b54a66`)
- move/rename code in dc3-center-manager (9 of 16 files) (`165d2f228`)

#### Documentation
- update README.md (`89381ed4b`)

#### Build
- update build and configuration files (6 files) (`0ae50b0bf`)
- update build and configuration files (30 files) (`412531d34`)

#### Chores
- update container configuration (15 files) (`6138cbeda`)
- remove code in dc3 (56 files) (`305bbc684`)

#### Other Changes
- adjust driver point interfaces and notes (`344e72810`)
- switch interface calls to OkHttpClient with auth headers (`9a60ebd7b`)
- fix VO class name bug (`8b646835f`)
- complete line-chart point endpoints (`a69ca0e59`)
- format code (`94d32d5a6`)
- complete point chart endpoints (`686eec7a2`)
- optimize online-duration code; finish point endpoints (`8f0070b7b`)
- add message bus interfaces (`0123bfee8`)
- add device count endpoint over gRPC (`65898bd14`)
- adjust online-duration stats; run job hourly (`a910c806f`)
- add point data volume endpoint to dashboard (`e3bbdd17f`)
- implement driver interfaces (`a320d824a`)
- improve point dashboard endpoint comments (`b38c421b8`)
- improve point dashboard endpoints (`65d185f6b`)
- implement point dashboard endpoints (`571445c34`)
- implement online-duration statistics job (`00577a175`)
- implement point dashboard (`027e8f5a0`)

### 📌 2023.12.31

_Generated on 2026-08-19._

#### Summary

- Generated from `bcab3e546` to `69327e826`.
- Included 59 commits across 3 categories: Features 47, Build 6, Chores 6.
- Highlights: bump development image version; update MybatisGenerator.java; update iot-dc3-rule.sql; update
  DriverStatusServiceImpl.java; update NettyServerHandler.java; update MqttReceiveServiceImpl.java; adjust pagination
  and service interfaces; update MybatisGenerator.java; update MybatisGenerator.java; update MybatisGenerator.java.

#### Features
- standardization pass (`69327e826`)
- add new classes TenantExt, AlarmMessageExt, AlarmNotifyExt and 7 more plus updates (85 files) (`af1e44bf2`)
- update code across modules (17 files) (`d14a625a5`)
- add new classes DriverEvent, DeviceEvent plus updates (7 files) (`87ebe1221`)
- update code across modules (108 files) (`a6466d52b`)
- add new class DriverTokenDO plus updates (19 files) (`0e56df0cf`)
- add new classes LabelService, AlarmMessageProfileService, AlarmNotifyProfileService and 33 more plus updates (86 files) (`bf9c17a7c`)
- update multiple modules (52 files) (`2e2af4fe0`)
- bump development image version (`3630332ad`)
- adjust table schema (`3fb69d0ae`)
- adjust table schema (`d81f18dc6`)
- add new classes ApiService, LabelService, MenuService and 38 more plus updates (185 files) (`d3a3562df`)
- update MybatisGenerator.java; update iot-dc3-rule.sql (`edda9e51e`)
- add new classes LabelBindDO, AlarmNotifyProfileDO, AlarmRuleDO and 3 more plus updates (31 files) (`79db74d6d`)
- add new classes UserPasswordDO, LabelBindDO, RoleResourceBindDO and 11 more plus updates (90 files) (`7c957e6e9`)
- add new classes UserVO, UserDO plus updates (21 files) (`3fbf934bd`)
- add new class DictionaryBuilder plus updates (98 files) (`756abe78e`)
- add new classes DeviceQuery, DriverQuery plus updates (53 files) (`578f0cd66`)
- update code across modules (156 files) (`ed5a5701f`)
- update code across modules (28 files) (`69a311d79`)
- update DriverStatusServiceImpl.java; update NettyServerHandler.java; update MqttReceiveServiceImpl.java (`23bc573dc`)
- update code across modules (75 files) (`a2d9c7914`)
- update dc3-center-manager code (12 files) (`36763998e`)
- update code across modules (55 files) (`d6e27e3cd`)
- add new classes EsPointValue, MgPointValue, InfluxPoint and 7 more plus updates (180 files) (`bec548a9e`)
- update dc3-center-manager code (44 files) (`7b5d9c050`)
- add new classes ApiExtBO, ApiExtVO, UserPasswordBO and 23 more plus updates (91 files) (`4e1885e8c`)
- add new classes ProfileBindBO, ProfileBindVO, PointAttributeConfigBO and 7 more plus updates (47 files) (`e3b277c57`)
- add new classes DriverAttributeConfigBO, DeviceBO, DriverAttributeBO and 5 more plus updates (42 files) (`c290cf8e1`)
- add code to dc3-center-manager (9 files) (`37b9d591d`)
- add new classes LabelBindBO, LabelBO, GroupBO and 12 more plus updates (203 files) (`17e414150`)
- update dc3-center-manager code (18 files) (`0d3c4d3ef`)
- add COPYRIGHT (`cdab67ee7`)
- update code across modules (4 files) (`fa06dea2e`)
- adjust pagination and service interfaces (`c9588b55c`)
- add new classes LabelBindVO, LabelVO, GroupVO plus updates (25 files) (`bc117b2fe`)
- update MybatisGenerator.java; update MybatisGenerator.java; update MybatisGenerator.java (`88f8cd34e`)
- update code across modules (18 files) (`f2ef4e33a`)
- add new class LabelDO plus updates (7 files) (`5ddb628be`)
- update dc3-center-manager code (10 files) (`4f6fdf891`)
- add new class GroupDO plus updates (8 files) (`2a9777ce8`)
- update MybatisGenerator.java (`ca12523ee`)
- add new classes LabelBind, Label, Group plus updates (7 files) (`34611cf92`)
- streamline configuration files (`aff31bac7`)
- update MybatisGenerator.java; add MybatisGenerator.java; add MybatisGenerator.java (`03a41480d`)
- update pom.xml; add MybatisGenerator.java (`487eb0b84`)
- adjust dependency versions (`6c3796866`)

#### Build
- update build and configuration files (5 files) (`1c9bb3759`)
- update build and configuration files (6 files) (`36f24253f`)
- update build and configuration files (6 files) (`04c9d6d5e`)
- update pom.xml POM (`68fd4c37b`)
- add dc3-common-register, dc3-api-auth dependencies to pom.xml POM (`c10a88854`)
- update dc3-center-auth POM (`49a538f97`)

#### Chores
- remove code in dc3-center-manager (133 files) (`165e8a143`)
- remove code in dc3-center-manager (73 files) (`46134113c`)
- remove code in dc3-center-manager (17 files) (`71dae65eb`)
- update docker-compose-dev.yml (`a4ed4fb80`)
- update docker-compose-dev.yml (`150a57264`)
- update docker-compose-dev.yml (`546d68c0b`)

### 📌 2023.9.26

_Generated on 2026-08-19._

#### Summary

- Generated from `0f64a1fd6` to `bcab3e546`.
- Included 16 commits across 3 categories: Features 13, Build 1, Other Changes 2.
- Highlights: update application-dev.yml; move header handling into base container; remove
  Californium3.client.properties; remove leshan-client-demo.jar; adapt MySQL configuration; add MQTT topic prefix
  configuration.

#### Features
- update application-dev.yml (`bcab3e546`)
- update code across modules (654 files) (`4b5c78c52`)
- update dc3-center-auth code (7 files) (`880a58ff1`)
- update multiple modules (6 files) (`9a0bc8a88`)
- update code across modules (32 files) (`af1e9aa6a`)
- move header handling into base container (`6dc571ac1`)
- remove Californium3.client.properties; remove leshan-client-demo.jar (`12d78d4bc`)
- update to 2023.4.5 (`246a3a3e7`)
- adapt MySQL configuration (`2a08cd5c2`)
- add base controller (`704a9da7e`)
- adjust web interfaces (`e00170e86`)
- add MQTT topic prefix configuration (`4a365f396`)
- update docker-compose-dev.yml; update pom.xml (`7143b2e93`)

#### Build
- add dc3-common-public dependencies to root POM (`2c7df870a`)

#### Other Changes
- add Nacos dev namespace and rule SQL; improve rule engine (`1a49fb61c`)
- ruleengine init (`5163478da`)

### 📌 2023.6.29

_Generated on 2026-08-19._

#### Summary

- Generated from `4b78b7a3b` to `0f64a1fd6`.
- Included 84 commits across 7 categories: Features 52, Bug Fixes 6, Refactoring 1, Documentation 5, Build 1, Chores 4, Other Changes 15.
- Highlights: config spring-boot-configuration-processor; fix MQTT batch receive statistics; fix MQTT driver command
  dispatch; stop write commands carrying a default read; bump auth-common dependency in auth module.

#### Features
- 2023.4.4 (`129f3dcbf`)
- config spring-boot-configuration-processor (`04d2bb786`)
- fix MQTT batch receive statistics (`557d78b71`)
- update to 2023.4.3 (`a204a13e1`)
- update to 2023.4.2 (`e698a6a43`)
- update code across modules (4 files) (`edbc145fd`)
- fix MQTT driver command dispatch (`da070bf77`)
- mqtt write command  fix (`c006f430a`)
- add new class MqttSendService plus updates (8 files) (`6575c5649`)
- stop write commands carrying a default read (`8df208785`)
- update readme (`f25b0a011`)
- bump auth-common dependency in auth module (`5b417d0a6`)
- add IndexController.java; add IndexController.java; add IndexController.java (`97251c926`)
- adjust release POM (`a3d2e8e3d`)
- add auth-common module; adjust login endpoint (`d57cb1dbf`)
- notify (`69b07a2d9`)
- update driver & common (`087a61197`)
- update driver custom service (`b231dc7ef`)
- driver sdk update (`414a5c4f0`)
- pom update (`9ede0159b`)
- docker-compose.yml update (`a6cadaddd`)
- gateway update (`0fb43558f`)
- update DriverService.java; update DriverServiceImpl.java (`9b1094575`)
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
- rename classes; return token from login for header auth (`e3bf5cdc7`)
- adjust Sonatype repository (`3bb64e1c7`)
- adapt auth services to Service interface changes (`9eb6c2d5e`)
- update RoleResourceBindServiceImpl.java; update DriverAttributeServiceImpl.java; update PointAttributeConfigServiceImpl.java (`68cafead5`)
- update code across modules (57 files) (`c404cd562`)
- implement user login; add resource and role-bind services (`3f21fc584`)
- add UserManageService (`27cf76929`)
- weather driver (`c4c36dc50`)
- update dc3-center-manager code (5 files) (`1b410bb64`)
- bump base image version (`6f0719c99`)
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
- format code (`d85a723b9`)
- opcua read error (`82a1a13f8`)
- remove ping (`0de69668f`)
- fix auth module jar download failure (`a24bb2070`)
- fix auth module jar download failure (`8fa284934`)
- fix auth module jar download failure (`de1f22c1f`)

#### Refactoring
- move/rename code in dc3-center-manager (8 of 17 files) (`545381472`)

#### Documentation
- container configuration for MySQL, Redis, MongoDB, RabbitMQ (`bc536a479`)
- update README.md (`2ce9fa588`)
- update README.md (`426f60adb`)
- update README.md (`bff83f0cd`)
- update README.md (`a933a150c`)

#### Build
- update pom.xml POM (`351b71a19`)

#### Chores
- update docker-compose.yml (`0271f2529`)
- update docker-compose.yml (`771ae4373`)
- remove code in docs (307 files) (`d39f0bfbb`)
- update container configuration (14 files) (`777d43b2e`)

#### Other Changes
- fix POM (`0f64a1fd6`)
- add TDengine support (`00c2b4d59`)
- home page endpoints for statistics and weather device map (`b015dc717`)
- data statistics (`3c2c6a0d3`)
- Update pom.xml (`5a78a2fb9`)
- extract Amap weather driver configuration (`ce4dd9341`)
- update .DS_Store code (5 files) (`f3f290caa`)
- add MySQL and MongoDB init scripts (`dce37ce37`)
- remove docs (`a9322aedc`)
- fix missing collection time in MQTT demo (`7c3f67919`)
- remove CNAME record (`d2dba0d3f`)
- update CNAME record (`91a0ca762`)
- add CNAME record (`bb510a0d7`)
- add MQTT demo code (`b7a59b2a6`)
- Optimize update service function (`638133df4`)

### 📌 2023.3.31

_Generated on 2026-08-19._

#### Summary

- Generated from `b18985601` to `4b78b7a3b`.
- Included 63 commits across 5 categories: Features 47, Bug Fixes 4, Build 6, Chores 2, Other Changes 4.
- Highlights: improve driver notifications; adjust MQTT driver logic; update driver event status interface; adjust
  driver and device events; device commands: read and write.

#### Features
- update code across modules (15 files) (`f6e039108`)
- improve driver notifications (`e644da62d`)
- adjust MQTT driver logic (`8393ab2a0`)
- fix MQTT module (`ea9ac6eec`)
- update dc3-center-manager code (32 files) (`d9860fab0`)
- remove dc3-api (`f88bff832`)
- update driver event status interface (`03fc8605b`)
- adjust driver and device events (`19c7ef9b7`)
- device commands: read and write (`9caba2bc6`)
- update driver registration logic (`08aacfc8a`)
- update code across modules (20 files) (`4e3b91804`)
- add DTU (XunTong) driver (`1aced73cd`)
- drop driver status config; improve registration (`380b7911d`)
- update code across modules (53 files) (`b52491335`)
- dedicate copyright to open-source developers (`53769be71`)
- dedicate copyright to open-source developers (`bb7f9928a`)
- update application.yml (`28e333c60`)
- adapt web and drivers (`4c147ceab`)
- update dev proxy with local port mapping (`bb98a0be9`)
- support remote development in driver logic (`49ab4e83b`)
- update code across modules (7 files) (`da22ec153`)
- adjust interface files (`34f6f0626`)
- clean up code and remove extra files (`90e20ad9f`)
- new proxy tool (`22fe61082`)
- replace Eureka with Nacos (`0a51a2587`)
- adjust init SQL (`063dd00cc`)
- configure development ports (`f87fa68a1`)
- upload proxy tools for three platforms (`1f9519a9e`)
- update dc3-api (`e387a3372`)
- update docker-compose.yml; update pom.xml (`2329dd2c3`)
- update docs code (4 files) (`4d3bdbd98`)
- remove dc3-api (`7e2e1b246`)
- add dc3-api (`5438db447`)
- update .gitignore (`f928efaeb`)
- update multiple modules (38 files) (`30b9fa5eb`)
- add new classes user, UserApi plus updates (10 files) (`7b626c5d9`)
- update code across modules (153 files) (`f34ccef33`)
- update code across modules (217 files) (`195c83a17`)
- update iot-dc3.sql (`90632b1bd`)
- update .gitignore (`508ba32a3`)
- add new classes UserBO, BaseBO plus updates (137 files) (`b76a24e7c`)
- update multiple modules (60 files) (`d6e07e174`)
- update multiple modules (72 files) (`3b5fc7710`)
- update (`4f3155469`)
- V2022.2.3 (`d32d1b688`)
- V2022.2.2 (`6d21f50be`)
- V2022.2.1 (`7465e39f4`)

#### Bug Fixes
- fix gRPC upgrade breaking driver registration (`2bc597ce2`)
- fix driver connection errors and gRPC service config (`29e0faa6c`)
- fix proxy ports and RabbitMQ connections (`7e7e75af3`)
- fix proxy tool link error on macOS (`f07ffae53`)

#### Build
- update build and configuration files (15 files) (`4cb162309`)
- update root POM (`5c4025464`)
- update build and configuration files (22 files) (`baf873b1d`)
- update pom.xml POM (`e43a84ef9`)
- update build and configuration files (27 files) (`ef7cb304f`)
- update build and configuration files (36 files) (`21fb90e10`)

#### Chores
- update container configuration (4 files) (`3a396bd69`)
- remove files (166 files) (`55b2ffb64`)

#### Other Changes
- guard bucket check to prevent not-found errors (`4b78b7a3b`)
- add InfluxDB storage policy (`924c898f2`)
- add LwM2M driver usage guide (`4d4329a82`)
- initialize LwM2M driver (`148aee5b0`)

### 📌 2022.12.28

_Generated on 2026-08-19._

#### Summary

- Generated from `e6bbc1a80` to `b18985601`.
- Included 21 commits across 4 categories: Features 17, Bug Fixes 1, Build 1, Other Changes 2.
- Highlights: update MySQL init scripts; import exception and public; remove register and monitor modules; update
  JsonUtil.java; update KeyUtil.java; update UserServiceImpl.java.

#### Features
- V2022.1.9 (`b18985601`)
- V2022.1.8 (`1c14a6770`)
- update MySQL init scripts (`364aa5086`)
- dockerfile config (`1e3eb38af`)
- import exception and public (`d2de8478a`)
- remove register and monitor modules (`01b2c3627`)
- add comments (`ffd26e863`)
- update code across modules (126 files) (`be5f409f5`)
- add new classes DriverServiceConstant, ExceptionConstant, DataServiceConstant and 22 more plus updates (247 files) (`a923720a8`)
- update JsonUtil.java; update KeyUtil.java (`c98eaea7e`)
- add new classes FieldUtil, MqttScheduleServiceImpl, MqttScheduleJob plus updates (47 files) (`296a9fb8e`)
- update UserServiceImpl.java (`bbcd17687`)
- update code across modules (679 files) (`00e7e2c95`)
- update code across modules (50 files) (`9a7239b8e`)
- add new class PointValueTypeEnum plus updates (52 files) (`9a447ad47`)
- update dc3-common-base code (8 files) (`22b602548`)
- externalize dev node IPs via environment variables (`dcb153038`)

#### Bug Fixes
- fix driver and point notification failures (`2a4d6c165`)

#### Build
- add maven-gpg-plugin, maven-javadoc-plugin dependencies to pom.xml POM (`dba7d061a`)

#### Other Changes
- dc3-center-manager: fix logic error in method selectByDeviceId (`4d76be837`)
- dc3-center-data: correct the key format of getKey from Redis (`940cd978d`)

### 📌 2022.8.21

_Generated on 2026-08-19._

#### Summary

- Generated from `074fa9769` to `e6bbc1a80`.
- Included 5 commits across 1 category: Features 5.
- Highlights: remove batch import/export logic.

#### Features
- update code (`e6bbc1a80`)
- update code (`ab094518e`)
- update code (`543c3bd8d`)
- remove batch import/export logic (`eec75b9d3`)
- update code across modules (132 files) (`dedb3509a`)

### 📌 2022.6.22

_Generated on 2026-08-19._

#### Summary

- Generated from `1757606a1` to `074fa9769`.
- Included 18 commits across 4 categories: Features 9, Bug Fixes 1, Build 3, Other Changes 5.
- Highlights: make OpenTSDB/Elasticsearch posting configurable; change ID type to String; add batch receive to MQTT
  driver.

#### Features
- tag bash (`01814dd31`)
- update dc3-center-data code (5 files) (`fa17c4e55`)
- make OpenTSDB/Elasticsearch posting configurable (`969894dad`)
- change ID type to String (`39959de1b`)
- add new classes ValueConstant, DriverCommandFallback, DriverCommandClient and 3 more plus updates (712 files) (`4deb4ef3e`)
- add batch receive to MQTT driver (`eda31352a`)
- update dc3-common-base code (24 files) (`cbec9701a`)
- add new class X509Util plus updates (18 files) (`2b2be5b4d`)
- add new class JsonUtil plus updates (27 files) (`d6a7d1fa1`)

#### Bug Fixes
- adjust XSS size limit (`074fa9769`)

#### Build
- update build and configuration files (44 files) (`46956634f`)
- update build and configuration files (14 files) (`5a936ec66`)
- update build and configuration files (8 files) (`9703c6a0e`)

#### Other Changes
- fix JWT accepting mismatched string secrets (`a4b8e1cd3`)
- ElasticsearchConfig (`8ed1007c0`)
- ElasticsearchConfig (`b0aa12adf`)
- update code across modules (28 files) (`5b5f0fa1e`)
- update multiple modules (23 files) (`218d58994`)

### 📌 2022.3.30

_Generated on 2026-08-19._

#### Summary

- Generated from `b42a78237` to `1757606a1`.
- Included 25 commits across 4 categories: Features 6, Bug Fixes 13, Chores 1, Other Changes 5.
- Highlights: configure logging aspect; demo bash & docker-compose yaml; update GatewayConfig.java; update pom.xml;
  resolve circular dependency; resolve circular dependency.

#### Features
- configure logging aspect (`6b45fad2f`)
- demo bash & docker-compose yaml (`e04491fb0`)
- update GatewayConfig.java; update pom.xml (`1496f113a`)
- rename (`0c8b2557c`)
- 2022.1.0 (`23f4e5004`)
- update code across modules (5 files) (`707a420e7`)

#### Bug Fixes
- rename services (`8407f8495`)
- resolve circular dependency (`38855213f`)
- rename methods (`e1fae660b`)
- resolve circular dependency (`51fdc7257`)
- resolve circular dependency (`8fce0e6fc`)
- docker log config (`44dc66f5d`)
- update multiple modules (6 files) (`71c1cc5d7`)
- enable thread io & disable append only file (`5262d987e`)
- registry.cn-beijing.aliyuncs.com/dc3/redis:6.2.6-alpine (`4dec35472`)
- update dc3 code (10 files) (`06f9e450b`)
- rdb&aof config (`89b47f322`)
- default redis config (`779211257`)
- redis config (`e9b3da2e5`)

#### Chores
- bump hutool.version 5.6.3 to 5.7.20, fastjson.version 1.2.76 to 1.2.79, jna.version 5.8.0 to 5.10.0 in dc3-common-base POM (`d90fed550`)

#### Other Changes
- update multiple modules (74 files) (`1757606a1`)
- update ThreadPoolConfig.java; update application-server.yml; update pom.xml (`79bc48cb2`)
- update dc3-common-base code (6 files) (`4baace6d1`)
- document modbus4j errors (`ba7b4f7c3`)
- remove dc3-boot-starter-1.3.2.SR.jar; remove dc3-start-error.md; update pom.xml (`705dcefe1`)

### 📌 2021.12.28

_Generated on 2026-08-19._

#### Summary

- Generated from `ba8812a92` to `b42a78237`.
- Included 42 commits across 4 categories: Features 24, Bug Fixes 16, Tests 1, Other Changes 1.
- Most active scopes: docker(12), data(1).
- Highlights: docker: rabbitmq cluster port config; docker: rabbitmq cluster mqtt tls; docker: rabbitmq cluster node
  port; restructure RabbitMQ module layout; docker: rabbitmq env & advanced config.

#### Features
- iptables.md (`b42a78237`)
- iptables.md (`ad39df0de`)
- emqx cluster readme (`c9ef98073`)
- **docker**: rabbitmq cluster port config (`916582ef3`)
- **docker**: rabbitmq cluster mqtt tls (`6a68fb15a`)
- rabbitmq cluster readme (`6a4379016`)
- **docker**: rabbitmq cluster node port (`58d7b7d90`)
- restructure RabbitMQ module layout (`b86929836`)
- **docker**: rabbitmq etc (`a788cd92b`)
- **docker**: rabbitmq env & advanced config (`bcb992d43`)
- **docker**: rabbitmq port config (`6db51b495`)
- rabbitmq etc rabbitmq.conf (`b50aa3538`)
- **docker**: rabbitmq server add plugins (`36986017a`)
- rabbitmq config (`1131dbdbd`)
- **docker**: rabbitmq server (`1860d1a06`)
- Redis cluster deployment guide (`8fa901178`)
- update project description (`d68108cf8`)
- MongoDB cluster deployment guide (`81c6743a7`)
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
- update MqttConfig.java; update dc3-gateway.http (`29c1f3bc0`)
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
- Included 49 commits across 4 categories: Features 35, Bug Fixes 5, Build 1, Other Changes 8.
- Most active scopes: api(7), driver(2), edge-gateway(2), mqtt driver(1), develop(1).
- Highlights: update application.yml; update docker-compose.yml; driver mqtt & edge gateway; edge-gateway: add a new
  type driver: edge-gateway; api: update DriverInfoApi.java; update PointInfoApi.java; api: attribute value @NotNull.

#### Features
- add reset readme (`ba8812a92`)
- qq group link (`d8bc63d96`)
- reset bash (`57335c320`)
- update application.yml; update docker-compose.yml (`f29e81c85`)
- add TSDB support (demo) (`525b1b00a`)
- improve MongoDB storage (`fde02cdd4`)
- set gateway timeout (`4c1d25671`)
- add new classes MessageHeader, MqttProperties plus updates (18 files) (`3a479fb9f`)
- add new class DevicePoint plus updates (10 files) (`78d5d5878`)
- add new classes AutoService, AutoClient, AutoClientHystrix and 2 more plus updates (13 files) (`bf493c6af`)
- add git tag script (`e4e375945`)
- add maven settings.xml (`176e36a2a`)
- update spring boot (`278e7446c`)
- update multiple modules (5 files) (`31959fdfb`)
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
- **driver**: update application.yml (`9244d685e`)
- **edge-gateway**: add a new type driver: edge-gateway (`81b6c5060`)
- **driver**: add driver type (`2fda5dd23`)
- **api**: Dictionary (`4f32f5193`)
- **api**: update DriverInfoApi.java; update PointInfoApi.java (`ec797cc36`)
- **api**: attribute value @NotNull (`e5fc981b8`)
- **api**: add response code (`7eccc77ca`)
- **api**: add api (`e95b71163`)
- **api**: add selectByAttributeIdAndDeviceIdAndPointId (`e4c69e8c8`)
- **api**: add selectByAttributeIdAndDeviceId (`cc4e2db01`)
- **mqtt driver**: update code across modules (32 files) (`4c94713f1`)

#### Bug Fixes
- remove start (`c2e60ece0`)
- **edge-gateway**: api type (`e769635aa`)
- **develop**: update application-dev.yml; update application.yml (`2c92c7495`)
- repackage bug (`7a352975d`)
- update dc3 code (25 files) (`59642a335`)

#### Build
- add dc3-core, dc3-profiles, dc3-api-data dependencies to root POM (`1f2a77ffc`)

#### Other Changes
- update AutoClientHystrix.java (`0bec1d890`)
- update dc3-base code (7 files) (`0eb6da143`)
- add MqttSendHandler.java; update application.yml (`cf9cc62ff`)
- update code across modules (24 files) (`97cae05c3`)
- update code across modules (9 files) (`2d92d17e2`)
- update application-mysql.yml (`514ccb230`)
- update multiple modules (10 files) (`a12b140d2`)
- update multiple modules (7 files) (`b9869961c`)

### 📌 2021.6.30

_Generated on 2026-08-19._

#### Summary

- Generated from `551911de2` to `1ff4ee428`.
- Included 90 commits across 6 categories: Features 1, Bug Fixes 6, Documentation 1, Build 2, Chores 1, Other Changes 79.
- Most active scopes: dc3-driver(2), maven(1), dc3-rtmp(1), dc3-gateway(1), dc3-manager(1), dc3-data(1).
- Highlights: dc3-driver: adjust driver and point attributes.

#### Features
- **dc3-driver**: adjust driver and point attributes (`34ec04716`)

#### Bug Fixes
- **dc3-driver**: fix GC log output (`0e60f7ac3`)
- **dc3-rtmp**: fix GC log output (`d50325bc8`)
- **dc3-gateway**: fix GC log output (`e021954d8`)
- **dc3-manager**: fix GC log output (`261ccefe7`)
- **dc3-data**: fix GC log output (`9e0f05d21`)
- **dc3-auth**: fix GC log output (`a317c0617`)

#### Documentation
- update documentation (2 files) (`e6941c183`)

#### Build
- **maven**: add dc3 Gitee Maven repository (`9283cb72a`)
- update build and configuration files (5 files) (`d91b08fc8`)

#### Chores
- **dc3-center**: move log files (`54a07b640`)

#### Other Changes
- update code across modules (22 files) (`1ff4ee428`)
- add wiki and demo links (`d0e318cd0`)
- update code across modules (11 files) (`31446371c`)
- update TokenClient.java; update TokenClientHystrix.java (`b4e4cb54f`)
- update multiple modules (10 files) (`6afd2cafa`)
- update multiple modules (9 files) (`b7c83c548`)
- replace dc3-config with dc3-profiles (`50cdad8e3`)
- adapt to Spring Boot 2.4.x config; fix 1.3.2.SR release (`df4812969`)
- adapt Driver SDK to Spring Boot 2.4.x config (`9ee6f6c6e`)
- update TopicRabbitConfig.java; update DriverEventReceiver.java (`5b486d574`)
- adjust event RabbitMQ configuration (`41bacd396`)
- adapt dc3-auth (`b6c39e629`)
- update multiple modules (7 files) (`782497e1d`)
- adapt dc3-monitor to Spring Boot 2.4.x (`5750695ca`)
- adapt dc3-gateway to Spring Boot 2.4.x (`d26340f10`)
- improve shared configuration management (`e4931f16b`)
- add dc3-profiles module for Spring Boot 2.4.x (`b46e73048`)
- adjust configuration and logging (`37c930959`)
- add GC logs and slim JRE for images (`8cbb66a78`)
- reduce dc3-monitor startup memory (`422771ac7`)
- trim dc3-register dependencies to cut memory (`9a4d9b6ef`)
- upgrade to 1.3.2.SR (`5ef4c7f01`)
- adapt Driver SDK (`4170882a2`)
- fix Long serialization to frontend (`e9ca5a57e`)
- adjust DictionaryApi device interfaces (`879361583`)
- adjust driver registration logic (`2c67705b7`)
- adjust dictionary logic (`c40d2e578`)
- logback.xml config (`e308d9b42`)
- adapt to deviceNameMap removal (`22f75a5da`)
- drop deviceNameMap due to duplicate names (`e8b74db5d`)
- improve DriverMetadata logic (`a337e7f8f`)
- adjust DriverMetadataService logic (`27ec628e2`)
- adjust Driver SDK logic (`5df8e2d61`)
- adjust DriverMetadataReceiver logic (`e9c816b0d`)
- update NettyTcpServerHandler.java; update NettyUdpServerHandler.java (`1df7cc61d`)
- adjust DriverMetadata logic (`9fcad4ce6`)
- adjust DriverContext logic (`6f70044c6`)
- adjust PointService logic (`fefe4f9ae`)
- adjust profile services logic (`1a49ecff6`)
- adjust DeviceService logic (`615d479be`)
- add separate device/driver status endpoints (`33e1d800a`)
- adjust GroupService logic (WIP) (`dfac64174`)
- adjust DriverService (`1a6d60943`)
- re-link DriverInfo from profile to device (`0058badbf`)
- notify all related drivers on profile/point changes (`013fa6206`)
- make Profile standalone; link Device to Driver in schema (`e79253e11`)
- adjust topic RabbitMQ configuration (`9ede6415b`)
- bind driver configs to devices instead of fixed profiles (`195ecea8f`)
- update Point.java (`6299caee9`)
- add device-profile mapping table and logic (`3c056ee99`)
- add constants (`966989105`)
- adjust tenant-user table schema (`a7349b086`)
- fix tenant-user association (`53a2c1e3b`)
- update settings.xml (`cd9f33076`)
- rotate GitHub Maven repository token (`ad1194194`)
- rotate GitHub Maven repository token (`fd3dedc63`)
- add enable flag to driver, profile, point, and device (`3871a6f77`)
- add enable flag to driver, profile, point, and device (`c83625a81`)
- adjust dc3-api-rtmp (`aa9986657`)
- adjust dc3-api-manager (`164d0cb09`)
- adjust dc3-api-auth (`551f3bf70`)
- adjust dc3-auth (`4fad71155`)
- adjust dc3-base (`050c32115`)
- adjust dc3-gateway (`b1db86392`)
- adjust SDK configuration (`329ccd94f`)
- update API documentation (`b391acbb4`)
- adjust database schema (`16076a575`)
- update dc3-auth API documentation (`e73bed100`)
- update auth API documentation (`c96e9919a`)
- implement multi-tenancy (`5e214d53e`)
- upgrade Spring Cloud to 2020.0.2 and adapt config (`1789a0bf5`)
- jmeter config (`65d9eca08`)
- update code across modules (533 files) (`61a1a10f9`)
- MQTT configuration (`d332b7dc8`)
- add DataCustomService to DriverService; set queue TTL (`d63de6a95`)
- adjust ID types; add tenant field (`d29241000`)
- user validation and device events (`a61447542`)
- use snowflake primary keys (`dd473dbd7`)
- gateway redis (`4532bab16`)

### 📌 2021.3.26

_Generated on 2026-08-19._

#### Summary

- Generated from `9e037b6bf` to `551911de2`.
- Included 10 commits across 2 categories: Features 3, Other Changes 7.

#### Features
- add new classes DriverSdkService, DriverSdkServiceImpl plus updates (18 files) (`84f22e3b6`)
- add new classes DriverEventDto, StatusClientHystrix, StatusClient and 2 more plus updates (80 files) (`a34a30b17`)
- add new class DuplicateException plus updates (29 files) (`010ff06dc`)

#### Other Changes
- update multiple modules (15 files) (`551911de2`)
- update multiple modules (56 files) (`5a42c9c74`)
- update dc3-sdk code (12 files) (`491dfee98`)
- rework driver registration and metadata sync (`120ecf8c6`)
- adjust registration and config sync (`1f2527f0b`)
- release 1.2.1 (New Year) (`14d6aa2e3`)
- Update Dockerfile & Jvm (`947611a6d`)

### 📌 2020.12.25

_Generated on 2026-08-19._

#### Summary

- Generated from `c4f00c9d7` to `9e037b6bf`.
- Included 38 commits across 5 categories: Features 1, Refactoring 2, Documentation 5, Chores 1, Other Changes 29.
- Highlights: move/rename code in dc3 (10 of 17 files); move/rename code in docs (25 of 61 files).

#### Features
- add new class ScheduleServiceImpl plus updates (19 files) (`baa4a6711`)

#### Refactoring
- move/rename code in dc3 (10 of 17 files) (`c27e33501`)
- move/rename code in docs (25 of 61 files) (`e1fec71cc`)

#### Documentation
- update documentation (5 files) (`c252b8861`)
- add centos-install-kvm-b.md (`7b0943da3`)
- update home.md (`3a7ffd529`)
- update quick-start.md (`5228fb920`)
- update quick-start.md (`3fceb7c79`)

#### Chores
- update docker-compose-demo.yml (`4e6be09ad`)

#### Other Changes
- upgrade to 1.2.0 (`9e037b6bf`)
- add scripts (`674181d65`)
- add environment variable configuration (`3935210be`)
- improve token mechanism (`f05b2560f`)
- update multiple modules (17 files) (`7cfe3febc`)
- device status default:offline (`759bdc27a`)
- update multiple modules (60 files) (`7d6bcc5ff`)
- device status default:offline (`96a31410e`)
- update code across modules (20 files) (`be1f153f8`)
- add event api (`455de387a`)
- replace deviceStatus with deviceEvent (`fcb0b8517`)
- update README.md; update about.md; add gvp.jpg (`f60a887b7`)
- MQTT module usage guide (`527a66e7d`)
- expand Modbus TCP guide (`4773ef087`)
- add Modbus TCP driver guide (`31eba770e`)
- remove event module (`786d0acc2`)
- update DeviceEvent.java; update PointValue.java; update CustomDriverServiceImpl.java (`c65fb9173`)
- point value update (`46a1f3abc`)
- rabbitmq config (`fec0a5f8b`)
- dynamic throughput monitoring; batch and scheduled persistence (`86b094a86`)
- mongo init script (`0fd0e47f0`)
- add event support (`7951db77e`)
- init event module (`79da540b0`)
- update multiple modules (23 files) (`b30fd5076`)
- update multiple modules (7 files) (`0a14f947a`)
- update dc3-rtmp code (5 files) (`4c28aab49`)
- improve demo startup order (`7f839b7c2`)
- update multiple modules (7 files) (`a70f480ef`)
- data correction endpoint (`76b4a08d7`)

### 📌 2020.9.28

_Generated on 2026-08-19._

#### Summary

- Generated from `b254ec8e8` to `c4f00c9d7`.
- Included 94 commits across 5 categories: Features 1, Refactoring 3, Build 1, Chores 7, Other Changes 82.
- Highlights: move/rename code in modules (12 of 26 files); move/rename code in docs (10 of 27 files); move/rename
  code in modules (4 of 9 files).

#### Features
- add code across modules (57 files) (`2ccb0ecb3`)

#### Refactoring
- move/rename code in modules (12 of 26 files) (`ec5f75c6b`)
- move/rename code in docs (10 of 27 files) (`d314f100a`)
- move/rename code in modules (4 of 9 files) (`2410f2659`)

#### Build
- update build and configuration files (22 files) (`8035d3399`)

#### Chores
- add aspectj.png (`77a6d5444`)
- update logo.png (`ce04d1cc0`)
- update Dockerfile (`5328c6278`)
- update container configuration (3 files) (`a25cfc6d6`)
- update images and design documents (9 files) (`6158a19e3`)
- update images and design documents (2 files) (`f56981908`)
- update device-model.png (`ac51e2b0b`)

#### Other Changes
- release 1.1.0 (`c4f00c9d7`)
- release 1.1.0 (`925e9282c`)
- update CustomDriverServiceImpl.java (`c5b7e6bf2`)
- adjust driver-sdk (`43b090dfa`)
- add OPC UA driver guide (`6e0d73b23`)
- expose request mapping list at root path (`d8a752810`)
- adjust dc3-register (`ca9260724`)
- update README.md; update banner.txt; update banner.txt (`18f5f911f`)
- update dc3 code (8 files) (`64f710b3a`)
- update multiple modules (9 files) (`03d1e74eb`)
- update index.html (`20ad0f863`)
- add dc3 vhost to RabbitMQ MQTT plugin (`1b89558a1`)
- update environment.md; update index.html (`a0bfe5f20`)
- remove _config.yml (`d604f047f`)
- add CNAME record (`d26e1b9f6`)
- update code across modules (4 files) (`0a750a1c3`)
- add CNAME record (`776c317c3`)
- remove CNAME; update index.html (`5e65e34d9`)
- update docs code (45 files) (`9c068ff8d`)
- update index.html (`e86281e42`)
- update multiple modules (9 files) (`42df547e8`)
- add UnAuthorizedException (`01bc7aa7f`)
- add byte-array reverse utility (`532c5b710`)
- add multi setting to batch import (`d3cca5c1c`)
- update code across modules (4 files) (`dc8cc1bbd`)
- remove UDP read timeout (`bc66237a7`)
- package JSON files into resources (`de0692a7b`)
- update code across modules (11 files) (`2d8c0a5d4`)
- update code across modules (20 files) (`51b809da5`)
- update BatchServiceImpl.java; update DriverContext.java (`0b94feb29`)
- fix null log in SDK device queries (`7385f5e13`)
- update code across modules (5 files) (`313f2226c`)
- 188b tcp & udp (`7e45756ce`)
- add dev script (`a30c22acb`)
- add dev script (`3f12df1fc`)
- update DriverUtils.java; update NettyServerHandler.java; update GatewayApplication.java (`6b6256f02`)
- add bytes-to-int endianness conversion (`ab98d27ae`)
- update CustomDriverServiceImpl.java; update NettyServer.java; update NettyServerHandler.java (`445ff37b6`)
- merge modules (`b812455cc`)
- update NettyServerHandler.java; update batch-import-188b-template.json (`1357d3a62`)
- improve batch import with shared profiles (`a5cd35469`)
- fix mojibake in batch import (`9788deb61`)
- update batch-import-188b-template.json (`f2fcafbf8`)
- update project description (`46f712ee3`)
- water device 188b driver (`75ad8323f`)
- update dc3-driver-modbus-tcp.http (`9453b3085`)
- support single-point and structured point storage (`9c03e8d7e`)
- improve messaging config across manager, data, and driver SDK (`11b586e26`)
- fix batch import not updating points (`5806ee276`)
- set RabbitMQ virtual-host to dc3 (`fcbb26994`)
- add multi storage-type field to device table (`78e56c02b`)
- add install-env.sh bootstrap script (`ac89a1ad8`)
- update Transcode.java; update TranscodeRunner.java (`b10ca77f3`)
- #bugfix gateway authentic (`ce5a2ff46`)
- set RabbitMQ vhost dc3; tune Redis thread pool (`9245b3050`)
- add READE.md; update TokenServiceImpl.java; update UserServiceImpl.java (`10909b87a`)
- add maven setting.xml (`b6335b933`)
- tune Redis pool, OpenFeign logging, Spring Boot/MySQL versions, Hystrix, MongoDB, Spring Security (`49292114e`)
- add settings.xml (`99a75c71d`)
- update README.md; update docker-compose-demo.yml (`7113702d0`)
- fix empty profile driverInfo in MQTT and Virtual drivers (`d22241554`)
- update spring boot version (`2ca733362`)
- Set theme jekyll-theme-cayman (`1253875bf`)
- update PointValueReceiver.java (`a3e8e7e46`)
- bump version (`75eeaf5c2`)
- maven version (`c3437fec6`)
- maven version (`4b7c609cf`)
- fix management platform naming (`97e685df9`)
- adjust modbus-tcp (`2617b439b`)
- test and adjust merge (`7f9e12efb`)
- fix profile deletion error (`db1142788`)
- https://gitee.com/pnoker/iot-dc3/issues/I1MQUG (`6efc74a8c`)
- set custom pool max size to 512 (`3b4cbe140`)
- improve concurrent OPC UA async reads (`531d8554e`)
- update Dockerfile; update docker-compose.yml; add aspectjweaver-1.9.5.jar (`f62828bf0`)
- dc3.version=1.0.0 (`b438e9856`)
- update dc3.version property (`a52f63d56`)
- update dc3.version property (`a5bc1475b`)
- improve batch upload format (`fa50be2e2`)
- adjust batch upload (`57818e57a`)
- support profile renames in drivers (`430aa2007`)
- update multiple modules (5 files) (`ef6210560`)

### 📌 2020.6.25

_Generated on 2026-08-19._

#### Summary

- Generated from `3fe4eb946` to `b254ec8e8`.
- Included 83 commits across 4 categories: Documentation 1, Build 1, Chores 2, Other Changes 79.

#### Documentation
- update README.md (`2ce1238bd`)

#### Build
- update build and configuration files (7 files) (`b254ec8e8`)

#### Chores
- update demo.sh script (`bcd2b5fb8`)
- update wechart.png (`c6f3ddd1d`)

#### Other Changes
- batch data import (`0879ed475`)
- upgrade Spring Boot and Spring Cloud (`4413233f7`)
- update banner.txt; update banner.txt (`0c28b3c1a`)
- update banner.txt (`9253564ee`)
- improve log output (`c8314dd0d`)
- threadPoolExecutor (`dc0c9ed6c`)
- driver & device status (`e8a1eded2`)
- adjust logging (`200f7cb3a`)
- adjust driver config change notifications (`d4d51a30d`)
- adjust point config change notifications (`3c0cf0d3a`)
- adjust profile change notifications (`197e92732`)
- adjust device change notifications (`23c058c53`)
- improve notification mechanism (`f33ecf1a1`)
- receive metadata changes via MQ for intranet support (`0af3ddee3`)
- manager add rabbitmq support (`112e9e032`)
- change sdk & data rabbitmq config (`7b6ec87b5`)
- driver SDK improvements (`5c45471f2`)
- update dc3-manager code (6 files) (`dcc570d6f`)
- move device code/status to auth and data modules (`9665b7feb`)
- add Jenkins config (`621f78fcd`)
- driver sdk bug fix (`3ccf1b56f`)
- data apo config (`b041c5a18`)
- gateway update (`1182552e5`)
- update code across modules (11 files) (`dafbc94ad`)
- fix cache update bug (`10ca998d7`)
- adjust PointAttributeClient (`24931fc0f`)
- adjust PointInfoClient (`4601cdbb7`)
- adjust PointClient (`def573d8c`)
- adjust ProfileClient (`8a64d5a70`)
- adjust GroupClient (`711f3ac8a`)
- adjust DriverInfoClient (`3f94d935f`)
- adjust DriverClient (`0e2b17aff`)
- adjust DriverAttributeClient (`c01a030e2`)
- adjust DictionaryClient (`6684f17a8`)
- adjust DeviceApi (`501d460dd`)
- adjust RtmpClient (`7f9106674`)
- adjust TokenApi (`0dbf509e6`)
- adjust UserApi (`25f303ac6`)
- adjust BlackIpApi (`5f2ade2cc`)
- remove IP blacklist edits (`42629b002`)
- provide shared thread pool for custom tasks (`eaab79ea4`)
- move device code to auth module (`8745ec25d`)
- update dc3-auth diagram (`01771ca1b`)
- gateway IP restrictions and blacklist (`5382fcd21`)
- rate-limit token endpoint errors (`7b0d46416`)
- add salt and token logout endpoints (`b918ea45f`)
- docker images (`477aa3e07`)
- improve Modbus TCP driver (`db3935bf3`)
- Modbus read support (`9371a3529`)
- add Flink container (`c73cc7a1a`)
- update README (`02d6fd3bd`)
- update MQTT driver (`2f8d9a508`)
- bugfix #I1HXOR https://gitee.com/pnoker/iot-dc3/issues/I1HXOR (`57ec3b295`)
- upgrade Spring Cloud (`6dadb6361`)
- update multiple modules (4 files) (`f3519fde2`)
- initialize MQTT support (`3861d9b19`)
- update README (`a6b0ee3db`)
- opcua README.md (`8c1fb1074`)
- opcua read & write (`295b4d542`)
- OPC DA README and HTTP interface tests (`2d6c5a2dd`)
- check OPC DA module (`33a3691db`)
- check OPC DA module (`a535a5c1e`)
- document HTTP client APIs (`2bca10b0c`)
- update OPC UA (`7c01e50d9`)
- add DingTalk community group link (`0e9115c67`)
- update charts (`94320aee5`)
- update charts (`25480bde1`)
- update copyright (`5e3f6de9c`)
- package change (`acb54a553`)
- open sources dc3 web ui (`200bf1b81`)
- open sources dc3 web ui (`8848daa8d`)
- add wiki (`69d029e77`)
- remove opc ua module (`62fa820e8`)
- add Gitee mirror (`30a689670`)
- add Gitee mirror (`61549f3d0`)
- add Gitee mirror (`8e8ffd39a`)
- update multiple modules (4 files) (`d0c9e9710`)
- add OPC DA write support (`652464956`)
- add OPC DA write support (`57513c940`)

### 📌 2020.3.27

_Generated on 2026-08-19._

#### Summary

- Generated from `e691a42fe` to `3fe4eb946`.
- Included 70 commits across 4 categories: Features 3, Refactoring 2, Build 2, Other Changes 63.
- Highlights: move/rename code in dc3-manager (42 of 65 files).

#### Features
- add new classes TopicInput, TopicOutput, Sender and 2 more plus updates (29 files) (`f244381f5`)
- add new class MessageApplication plus updates (37 files) (`39b286439`)
- add new classes chunk-177c3c86.e4d14e13, chunk-vendors.450584b1, PointInfoClientHystrix and 8 more plus updates (88 files) (`d0190c692`)

#### Refactoring
- relocate classes and update package references (22 files) (`7368785ba`)
- move/rename code in dc3-manager (42 of 65 files) (`34e2bb254`)

#### Build
- update build and configuration files (44 files) (`e7e3c1559`)
- update build and configuration files (48 files) (`6847f928c`)

#### Other Changes
- update multiple modules (6 files) (`3fe4eb946`)
- expose ports (`f757c52d1`)
- add demo screenshots (`42ad79cae`)
- add demo screenshots (`30872eb61`)
- update demo (`cdb7e5718`)
- demo one-click startup guide (`7be49a928`)
- SDK startup registration check (`b72ab4d32`)
- update README (`aaf2b1864`)
- update README (`7160e63b8`)
- update Dockerfiles (`a91975d83`)
- PLC S7 driver (`79ba76c78`)
- change LIKE query to EQ (`e85bff52b`)
- GitHub configuration updates (`eaccb3a3b`)
- driver SDK improvements (`58a82cfd4`)
- driver SDK improvements (`794bfea96`)
- driver SDK improvements (`853b8a87e`)
- byte conversion utilities (`03f1e3557`)
- improve driver registration; fix bugs (`6b653b076`)
- add MongoDB pool tuning and multi-replica support (`495beb63d`)
- update code across modules (42 files) (`43fb82f32`)
- update README.md; update iot-dc3-architecture.pptx; update iot-dc3-architecture2.jpg (`cb5d5b0fc`)
- update README (`a0d573cfb`)
- initial release with driver SDK and data storage (`26afae32f`)
- improve driver cache operations (`60a87368d`)
- read support (`45105edde`)
- improve device-change driver notifications (`d5ae69113`)
- adjust metadata notifications; add initial data ingestion (`c8bdc243b`)
- point value updates (`2d0149d95`)
- move scheduling into driver layer (`fd986650a`)
- notify driver SDK of metadata changes (`30136c1f1`)
- adjust SDK (`65b8cffce`)
- add Spring config metadata for driver properties (`9eb674d49`)
- improve driver SDK initialization (`6ac4f3c6a`)
- update dc3 code (5 files) (`5e841ee7b`)
- consolidate dictionary (`80e35c73f`)
- remove redundant interfaces; standardize (`9cba1c815`)
- load driver and point info at startup (`0ca4fb2e7`)
- add comments (`162851445`)
- ConnectInfo.java -> DriverAttribute.java,ProfileInfo.java -> PointAttribute.java (`cca8e890d`)
- update driver info (`9a1884ead`)
- update driver info (`2f03ab0c9`)
- virtual driver updates (`f8d6bf610`)
- manager & data & rtmp (`2a5c31364`)
- port & mysql & docker-compose (`f692ebe50`)
- gateway & data (`844a66eaf`)
- update base image (`db9204d19`)
- merge and simplify config (`3b9a3143b`)
- sdk schedule init (`a20dfb439`)
- update README (`2027ab02f`)
- add driver (`46643f7de`)
- point fuzzy search (`efb750441`)
- optimize dictionary data and column sizes (`1ef5baf51`)
- update cache & dic table (`7c2806590`)
- device management (`e95762d18`)
- New Year update (`0f37f677a`)
- device management driver interfaces (`7eed6df57`)
- device management profile/point interfaces (`a75a1aca2`)
- device management group interfaces (`d990d8342`)
- device management device interfaces (`5ae894ac9`)
- adjust ingress (`346d5c06f`)
- update ingress (`e2c3e1de8`)
- k8s network namespace (`bd15277d1`)
- pagination and misc adjustments (`2368de310`)

### 📌 2019.12.29

_Generated on 2026-08-19._

#### Summary

- Generated from `075c02d09` to `e691a42fe`.
- Included 92 commits across 5 categories: Features 12, Refactoring 7, Build 1, Chores 4, Other Changes 68.
- Highlights: move/rename code in modules (34 of 97 files); move/rename code in modules (39 of 72 files); move/rename
  code in dc3 (8 of 8 files); move/rename code in dc3-api-dbs (8 of 18 files); move/rename code in dc3-data (16 of 19
  files).

#### Features
- add new classes TokenAuthService, TokenAuthApi, TokenAuthFeignClientHystrix and 9 more plus updates (56 files) (`79ac98c36`)
- add dc3-auth module (`810d90db8`)
- add new class TokenDto plus updates (29 files) (`86a0f720f`)
- add new classes DeviceManagerFeignApiHystrix, DeviceManagerDbsFeignApi, CmdTools and 4 more plus updates (39 files) (`a96e40fcc`)
- add new classes PointProfileMapper, PointPropertyMapper, DeviceDriverMapper and 11 more plus updates (84 files) (`6dc994a24`)
- add code across modules (5 files) (`dfb9834bf`)
- add new classes Dc3Auth2Exception, Dc3AuthenticationSuccessEventHandler, Dc3AuthenticationFailureEvenHandler and 11 more plus updates (31 files) (`147a5fc7c`)
- add new classes DeviceDto, DeviceManagerFeignApiHystrix, DeviceManagerDbsFeignApi plus updates (9 files) (`62c507f5a`)
- add code across modules (34 files) (`f6d570b59`)
- add new classes RequestGlobalFilter, PasswordDecoderFilter plus updates (27 files) (`456d809e4`)
- add new class MonitorController plus updates (9 files) (`7562838d1`)
- add new classes AuthApplication, Schedule, Unit and 4 more plus updates (172 files) (`cb2a29ea0`)

#### Refactoring
- move/rename code in modules (34 of 97 files) (`bd926a2b2`)
- relocate classes and update package references (56 files) (`ce9df1ebf`)
- move/rename code in modules (39 of 72 files) (`14ce7c01b`)
- move/rename code in dc3 (8 of 8 files) (`6223ec384`)
- move/rename code in dc3-api-dbs (8 of 18 files) (`7ce49f62e`)
- move/rename code in dc3-data (16 of 19 files) (`e58c8c3e1`)
- move/rename code in dc3-common-core (31 of 76 files) (`c64d7c9e4`)

#### Build
- update build and configuration files (5 files) (`f0ba567ae`)

#### Chores
- update container configuration (2 files) (`289b7d678`)
- remove code in dc3-oauth (11 files) (`993a5a433`)
- remove code in dc3-group (43 files) (`428db7445`)
- remove code in dc3-common-security (107 files) (`aa94c99e7`)

#### Other Changes
- update code across modules (28 files) (`e691a42fe`)
- remove dbs module (`4fe97cd77`)
- RTMP updates (`4be62d4a4`)
- update dc3-gateway code (7 files) (`a4f92d1b4`)
- update token handling (`d47768d6c`)
- user module updates (`7983151d3`)
- Hystrix updates (`81899f6b9`)
- OkHttp3 updates (`6885fbd37`)
- move docker-compose files (`d086de46e`)
- Node.js updates (`c780c5ec7`)
- gateway routes for token and register (`adacfe502`)
- adjust permission interfaces (`ce476518c`)
- relocate files (`b6edfeb8d`)
- add token list (`4861e3b3f`)
- adjust token database logic (`892a65cce`)
- adjust auth implementations (`703ffdc92`)
- adjust annotations (`5d9f6f888`)
- adjust entities and key utilities (`3cbb4ecf7`)
- configure RedisTemplate and RedisUtils (`f00685770`)
- adjust token and user table schema (`30246063c`)
- unified gateway permission management (`a0269b226`)
- complete first dc3-auth implementation (`639f07a62`)
- extract Redis config into common-core (`dda82cedb`)
- update code across modules (18 files) (`086894e59`)
- add pom.xml; add AuthFeignClient.java; add AuthFeignApiHystrix.java (`b5d320816`)
- update code across modules (16 files) (`363fdd66d`)
- improve interfaces (`e45d97056`)
- improve interfaces (`d777a68e5`)
- add Postman collection (`7d96648a3`)
- simplify config and refresh cache (`fd2ced388`)
- dbs mongodb->mongo (`1ca73acc4`)
- mariadb -> mysql:8.0 java - > java:1.8.0_221 update docker (`d03c9e50c`)
- rebuild dc3-base docker image (`9364136c8`)
- Create maven.yml (`3c23d4789`)
- update dc3-api-dbs code (6 files) (`58a011a89`)
- replace cmdtool with Hutool (`e4dcbe902`)
- refresh RTMP module (`9d8e65eee`)
- update code across modules (62 files) (`5242d356f`)
- update multiple modules (36 files) (`72ebe7dd2`)
- update code across modules (11 files) (`8e3e303b6`)
- update multiple modules (6 files) (`1ed243f67`)
- update dc3 code (5 files) (`98ac334db`)
- update token handling (`a2e75c38a`)
- post all data (`8343acf47`)
- update base image (`c558b59e5`)
- log & register (`9da074fc0`)
- remove author fields (`f1c5dcf2a`)
- update multiple modules (35 files) (`6ce7b9e5a`)
- docker java ops (`d32146365`)
- update code across modules (9 files) (`65ce9d897`)
- update dc3-oauth code (4 files) (`9731d3d54`)
- rtmp http client (`73b959ba0`)
- rtmp http client (`988badeac`)
- rtmp http client (`e574dc54e`)
- add Dc3Page.java (`6c37db3b4`)
- update code across modules (12 files) (`295feb0ab`)
- update multiple modules (4 files) (`af8e6aa5b`)
- move WebSecurityConfigurer.java to WebSecurityConfig.java (`e3b995cb4`)
- update CI (`c4955bafd`)
- configure monitor reverse proxy (`80785a73c`)
- update MonitorController.java; add index.html (`1181772b4`)
- Bump netty-all from 4.1.36.Final to 4.1.42.Final (`e4eba92b2`)
- permission configuration (`04b643e81`)
- update code across modules (195 files) (`1470c19cd`)
- add code (`4c0a685a8`)
- adjust ports (`9dab0ce5c`)
- device drivers and config attributes (`79df5bbe6`)
- drop version-control tags (`81b80e1d4`)

### 📌 2019.9.29

_Generated on 2026-08-19._

#### Summary

- Generated from `dc857123f` to `075c02d09`.
- Included 86 commits across 5 categories: Features 5, Documentation 5, Build 2, Chores 2, Other Changes 72.

#### Features
- add new classes OpcService, OpcServiceImpl plus updates (15 files) (`be1522234`)
- add new class Global plus updates (13 files) (`6c0453aa2`)
- add new class RtmpServiceImpl plus updates (18 files) (`29c35594e`)
- add new classes DatagramUtils, WiaDeviceService, WiaVariableService and 5 more plus updates (29 files) (`b049a2f76`)
- add new class DeviceVirtualApplication plus updates (16 files) (`09e90a630`)

#### Documentation
- update README.md (`89f3fd5f7`)
- update README.md (`75800221d`)
- update README.md (`0c7c0c043`)
- update README.md (`3a232fa35`)
- update README.md (`3e81b9054`)

#### Build
- update build and configuration files (46 files) (`4931200de`)
- update build and configuration files (17 files) (`aa4b18dcb`)

#### Chores
- update docker-compose.yml (`dd7dee9d2`)
- remove code in web UI (8 files) (`1a6c24fe1`)

#### Other Changes
- update code across modules (10 files) (`075c02d09`)
- adapt gateway; add gateway home page (`382c4b1f1`)
- standardize CRUD interfaces (`ff12c0871`)
- service interface base classes (`eaa982675`)
- add RTMP tasks (`6b68c6857`)
- add RTMP tasks (`2245125b2`)
- update iot-dc3.sql; update docker-compose.yml (`12f4db48e`)
- remove Zipkin (`5451671be`)
- update IndexController.java (`1cc7d5bca`)
- adjust commands (`faf76c9da`)
- remove Zipkin (`6aa440a14`)
- replace in-house queue with RabbitMQ for safety (`f465049f5`)
- update code across modules (5 files) (`90b02b340`)
- Merge branch 'dev' (`4cdccd895`)
- move collect module (`ec356e46b`)
- Zipkin updates (`c9f7df7f3`)
- test Swagger2 setup (`db4c5cd8e`)
- test Swagger2 setup (`1cc202f28`)
- adjust package names for Feign client scanning (`9c5b7568d`)
- update code across modules (8 files) (`dde405103`)
- fix RTMP reconnect bug (`92d27f358`)
- make Response generic (`b14ae7216`)
- make Response generic (`1398c408c`)
- adjust interface inheritance (`ce97b6712`)
- upgrade FastJson and Wia gateway (`86d5037bd`)
- adjust table schema (`a3f12674a`)
- unified format (`b3d9b9709`)
- update OpcController.java (`f5bf8230c`)
- update Global.java; update OutputHandle.java; update RtmpServiceImpl.java (`e8f47dfa4`)
- update dc3-rtmp code (6 files) (`4f39710bc`)
- complete cache config (`3ef9972b4`)
- commit redis cache config (`cac61610a`)
- add MongoDB configuration (`3ff02f8b9`)
- complete wia datagram reveice (`3a050b3a7`)
- update dc3-group code (10 files) (`0bcc74d42`)
- the client side adds "message key" logic (`5b1d9df50`)
- adjust code style (`bb228f322`)
- update code across modules (9 files) (`be91aa686`)
- adjust mock client (`261292854`)
- add mock client (`81d353920`)
- fix SQLite error prompt (`5a2f09b02`)
- fix SQLite error prompt (`6682b0536`)
- update README.md; update iot-dc3-architecture.pptx; update iot-dc3-logo.png (`3863214b5`)
- disable inter-service API version control (`4679d609e`)
- update .travis.yml; update pom.xml (`08898861d`)
- Update .travis.yml (`01f1d65ae`)
- Update .travis.yml (`f9b656168`)
- Update .travis.yml (`d2d5455f1`)
- Delete CODE_OF_CONDUCT.md (`84164cf7f`)
- update .travis.yml; update pom.xml (`3fb9b04fa`)
- update .travis.yml; update README.md (`bc032d5a7`)
- add .travis.yml (`5abb179a4`)
- remove .travis.yml (`1ad69a51c`)
- add .travis.yml; update README.md; update pom.xml (`773a4eb62`)
- update code across modules (7 files) (`55f584386`)
- add API versioning (`251582e6e`)
- update .gitignore (`62d664459`)
- move spring.factories to spring.factories (`134345ef1`)
- remove web source pending split to its own repo (`ed1f20525`)
- API security validation (`e1ed1e77a`)
- Create CODE_OF_CONDUCT.md (`845b98f84`)
- pack nginx-rtmp into base image (`fae89dce2`)
- pack ffmpeg into base image (`0db2277a1`)
- adjust Docker versions (`50aac8a2a`)
- iot dc3 docker base images (`cc295bdc2`)
- adjust packaging and Dockerfiles (`a6fa50873`)
- update readme (`10cf40fde`)
- configure Docker network; update diagrams (`e4ed03107`)
- set register-center host (`8b736c9f4`)
- update README (`d2b845c7c`)
- docker config‘ (`c5ed740ff`)
- adjust Docker settings (`afdad55ab`)

### 📌 2019.6.30

_Generated on 2026-08-19._

#### Summary

- Generated from `2203ccff5` to `dc857123f`.
- Included 83 commits across 5 categories: Features 3, Documentation 3, Build 1, Chores 1, Other Changes 75.

#### Features
- add new class ErrorCode plus updates (21 files) (`e66ec2e2f`)
- add new classes RtspApplication, WrapMapper, Wrapper plus updates (26 files) (`bc222535c`)
- add new class DataBase plus updates (6 files) (`4ad1ed2ed`)

#### Documentation
- update README.md (`d3a56e164`)
- update README.md (`4325b7563`)
- update README.md (`64d34a4df`)

#### Build
- add spring-cloud-starter-sleuth, spring-cloud-sleuth-zipkin, spring-cloud-starter-netflix-hystrix dependencies to pom.xml POM (`41f495dc5`)

#### Chores
- bump spring-cloud.version Greenwich.RELEASE to Greenwich.SR1 in pom.xml POM (`7bcbafeae`)

#### Other Changes
- add Docker configuration (`dc857123f`)
- optimize POM configuration (`2e2c59906`)
- add credential encryption (`722a955bc`)
- set up database services (`49b9949e4`)
- adjust register and manager centers (`baba7bba9`)
- version adjustments; collection threads and queues (`87a8deb56`)
- resource server (`a0f715e0d`)
- add Wia data classes (`333db7000`)
- adjust messaging module (`365c9a6a3`)
- add messaging module configuration (`c106a6afe`)
- update code across modules (223 files) (`b1dd422eb`)
- add PLC S7 support (`313d338b6`)
- upgrade Vue (`51a88873c`)
- update README (`45a6b1430`)
- client side (`544194dfa`)
- improve RTSP-RTMP service (`88a02274e`)
- fix Feign client component conflicts via package layout (`74e8d6999`)
- add RTMP data operations (`0abe995d2`)
- remove webapp production config (`b59773704`)
- add RTMP fields (`f8c24ee3f`)
- adjust configuration and dependencies (`bc60358c8`)
- add notes (`cf3b5d1c0`)
- adjust device management and collection (`07ded56aa`)
- adjust configuration and dependencies (`0bc207a07`)
- update README (`2e840fcaf`)
- update badges (`bf3a98640`)
- update badges (`f58e4101b`)
- add Apache License-2.0 (`87897c347`)
- adjust RTSP-to-RTMP listener thread (`4983dcdfd`)
- adjust RTSP-to-RTMP listener thread (`2f832ea01`)
- GUID utility (`b1a35252f`)
- device management (`739aa9074`)
- add web monitoring; adjust Redis connections (`87b914ecf`)
- adjust POM configuration (`cbc6f85bd`)
- configure Spring Cloud Admin monitoring (`8c0bdd8ed`)
- add Spring Cloud Admin module (`a18ca57aa`)
- add descriptions (`79eff289d`)
- add Actuator monitoring (`874eb7bc2`)
- add Spring Cloud Gateway configuration (`28872c6d9`)
- circuit breaking (`1959ed4a9`)
- load balancing and circuit breaking changes (`f86448ba1`)
- add RTSP-to-RTMP example (`0052818b1`)
- add build scripts (`72882fbb4`)
- add Postman API tests (`072b59c3c`)
- add device management service (`408f314a0`)
- upgrade Spring stack to Greenwich release train with compatibility fixes (`f8e7b8926`)
- add AES/RSA encryption (`c166257dc`)
- add tests (`c5be763b6`)
- add symmetric encryption utilities (`e9cff2838`)
- add Siemens S7 PLC support (`24e8935c1`)
- initial data (`744796ea7`)
- create database scripts (`fdedb6568`)
- initial OPC, OPC UA, and PLC S7 data services (`90e88c1a9`)
- add tests (`08b996343`)
- upgrade Spring Boot 2.0; add MyBatis-Plus; manage dependency versions (`9ffe3adc4`)
- configure gateway and RTMP services (`eb4ffdf93`)
- handle sockets with Netty (`be2729633`)
- handle sockets with Netty (`65c31efcc`)
- add command launcher (`3f1903566`)
- restructuring and tutorial (`e85254dc4`)
- adjust notes (`9d510ea4a`)
- remove WrapMapper.java; remove Wrapper.java; add CommandBuilder.java (`6aef368b8`)
- add public endpoints (`855780b38`)
- update web UI code (4 files) (`56f57424f`)
- update web UI code (10 files) (`9abf6b332`)
- update web UI code (9 files) (`566c165f2`)
- set up Express (`b981349f6`)
- set up Express (`259cad8c3`)
- add Vue build configuration (`581f0ebe4`)
- Express updates (`a999cf0e2`)
- data-helper initialization (`5c62267e1`)
- update data-helper (`b0d44d077`)
- update multiple modules (9 files) (`71be7110c`)
- add gateway service and configuration (`e3a0b0f9d`)
- adjust user and copyright info (`6fc4b23bb`)

### 📌 2018.10.30

_Generated on 2026-08-19._

#### Summary

- Generated from `0878f2ad8` to `2203ccff5`.
- Included 1 commit across 1 category: Other Changes 1.

#### Other Changes
- update jackson-databind (`2203ccff5`)

### 📌 2018.9.25

_Generated on 2026-08-19._

#### Summary

- Generated from `repository start` to `0878f2ad8`.
- Included 18 commits across 5 categories: Features 1, Refactoring 1, Documentation 1, Chores 1, Other Changes 14.

#### Features
- add new classes User, WrapMapper plus updates (20 files) (`b19feff1b`)

#### Refactoring
- relocate classes and update package references (13 files) (`54a983f35`)

#### Documentation
- update README.md (`cf8df1113`)

#### Chores
- remove code in web UI (87 files) (`034d23090`)

#### Other Changes
- update screenshots (`0878f2ad8`)
- update icons (`a1af4976b`)
- relocate files (`f5126b0b0`)
- update architecture diagrams (`57d9a6961`)
- update architecture diagrams (`6cbd4e358`)
- update architecture diagrams (`65e9414f8`)
- update architecture diagrams (`a8aab4f2e`)
- update architecture diagrams (`038dcfc28`)
- update multiple modules (48 files) (`c9cbcb312`)
- initial setup steps (`1d5ca36d5`)
- patch known vulnerable dependencies in webpage/package-lock.json (`98701931e`)
- translate README to English (`489be6570`)
- add build shell scripts (`99638cba6`)
- initial project scaffold (`3b392b9a2`)

</details>
