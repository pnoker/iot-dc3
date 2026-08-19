# ✨ What's Changed

### 📌 2026.8.19

_Generated on 2026-08-19._

#### Summary
- Generated from `956de3dd3` to `59fc5be9b`.
- Included 9 commits across 6 categories: Bug Fixes 1, Refactoring 1, Documentation 4, Build 1, CI 1, Chores 1.
- Most active scopes: deploy(2), usage(1), web(1), docs(1), repository(1), javadoc(1).
- Highlights: deploy: pin web image tag correctly in k8s and helm; common: enforce shared contract boundaries.

#### Bug Fixes
- **deploy**: pin web image tag correctly in k8s and helm (`2a10b14d9`)

#### Refactoring
- **common**: enforce shared contract boundaries (`59fc5be9b`)

#### Documentation
- translate dc3/doc readme to english (`0fdad7caf`)
- **deploy**: deduplicate deployment guides in usage and deployment (`f8eabd9b4`)
- **usage**: add swarm, kubernetes and helm deployment guides (`d804b2e73`)
- **repository**: align documentation with executable sources (`b61d6350a`)

#### Build
- **web**: align package metadata with build image (`003686240`)

#### CI
- **docs**: enforce documentation consistency (`2426d1b47`)

#### Chores
- **javadoc**: document non-override public methods (`51f0efd2d`)


### 📌 2026.8.18

_Generated on 2026-08-18._

#### Summary
- Generated from `78c4e6d53` to `956de3dd3`.
- Included 10 commits across 7 categories: Breaking Changes 1, Features 2, Bug Fixes 2, Refactoring 1, Documentation 1, Tests 1, Chores 2.
- Most active scopes: deploy(3), driver(2), brand(1), database(1), logging(1).
- Highlights: driver: enforce lease-fenced durable telemetry; deploy: add k8s and helm manifests and make targets; deploy: add compose scale and swarm deployment configs; driver: isolate hardware resources and validate protocol I/O; database: repair and validate PostgreSQL initialization.

#### Breaking Changes
- **driver**: enforce lease-fenced durable telemetry (`956de3dd3`)

#### Features
- **deploy**: add k8s and helm manifests and make targets (`6e9c38d80`)
- **deploy**: add compose scale and swarm deployment configs (`c1e30e574`)

#### Bug Fixes
- **driver**: isolate hardware resources and validate protocol I/O (`caf34ba85`)
- **database**: repair and validate PostgreSQL initialization (`fd159b7a3`)

#### Refactoring
- **logging**: standardize application logging (`aec6ebfc7`)

#### Documentation
- **deploy**: add deployment guide (`64058f773`)

#### Tests
- enforce truthful coverage and behavioral quality (`dddf2e869`)

#### Chores
- **brand**: refine repository artwork (`21017f015`)
- refresh dependencies and project documentation (`8f6520dd7`)


### 📌 2026.8.17

_Generated on 2026-08-17._

#### Summary
- Generated from `a7a1650be` to `78c4e6d53`.
- Included 13 commits across 5 categories: Breaking Changes 1, Features 8, Bug Fixes 1, Build 2, Other Changes 1.
- Most active scopes: driver(9), dc3-web(1), deps(1).
- Highlights: emit identifiers as strings on the HTTP contract; driver: add dnp3 master driver over native stack; driver: add iec61850 driver for substation automation; driver: add knx driver for knx building automation; driver: add lorawan driver for chirpstack mqtt uplink.

#### Breaking Changes
- emit identifiers as strings on the HTTP contract (`c87f31be9`)

#### Features
- **driver**: add dnp3 master driver over native stack (`25541646c`)
- **driver**: add iec61850 driver for substation automation (`5aa6457aa`)
- **driver**: add knx driver for knx building automation (`dea95bf34`)
- **driver**: add lorawan driver for chirpstack mqtt uplink (`a137eeb0d`)
- **driver**: add kafka driver for kafka streaming data source (`314dae66a`)
- **driver**: add redis driver for redis key-value data source (`9d00105c4`)
- **driver**: add mbus driver for EN13757 meter bus (`9a2a09488`)
- **driver**: add dlt645 driver for DL/T645-2007 electricity meters (`78c4e6d53`)

#### Bug Fixes
- **dc3-web**: override nanoid to 3.3.18 to resolve high severity advisory (`044eafa2f`)

#### Build
- **deps**: bump dompurify from 3.4.12 to 3.4.13 in /dc3-web (#206) (`33ee1a728`)
- **driver**: register eight new drivers in pom dockerfile and compose (`def5c3437`)

#### Other Changes
- refactor!(web): parse identifiers as strings and drop json-bigint (`b02d8f81f`)


### 📌 2026.8.14

_Generated on 2026-08-14._

#### Summary
- Generated from `6c9149f9b` to `a7a1650be`.
- Included 3 commits across 2 categories: Features 1, Documentation 2.
- Most active scopes: slogan(2), brand(1).
- Highlights: brand: add multilingual banners with dc3 protocol mesh to readmes.

#### Features
- **brand**: add multilingual banners with dc3 protocol mesh to readmes (`dabd5d971`)

#### Documentation
- **slogan**: converge remaining industrial-ai-iot wording to canonical slogan (`9596334fc`)
- **slogan**: unify positioning across readme languages and web meta (`a7a1650be`)


### 📌 2026.8.12

_Generated on 2026-08-12._

#### Summary
- Generated from `8201d75de` to `6c9149f9b`.
- Included 11 commits across 5 categories: Features 2, Bug Fixes 2, Documentation 2, CI 1, Chores 4.
- Most active scopes: web(5), java(1), sql(1), auth(1), readme(1), agents(1).
- Highlights: web: align mock menu and user seed with SQL init script; web: enrich the localized Industrial AI mock experience; auth: bypass tenant interceptor in permission lookup; web: prefill the mock login password.

#### Features
- **web**: align mock menu and user seed with SQL init script (`b0aa03316`)
- **web**: enrich the localized Industrial AI mock experience (`6c9149f9b`)

#### Bug Fixes
- **auth**: bypass tenant interceptor in permission lookup (`f024b1469`)
- **web**: prefill the mock login password (`75a5571e2`)

#### Documentation
- **readme**: refresh module documentation (`6b37cb5cc`)
- **agents**: consolidate repository guidance (`9099ad093`)

#### CI
- **release**: verify releases before publishing artifacts (`a58064cc5`)

#### Chores
- **web**: reformat menu seed and index meta tags (`437d7bec4`)
- **java**: indent permission lookup body under runIgnore lambda (`7d95dffd3`)
- **sql**: drop duplicated license header in init scripts (`661392914`)
- **web**: resize header logo and add brand title (`e17c2e878`)


### 📌 2026.8.9

_Generated on 2026-08-09._

#### Summary
- Generated from `73ff48bcb` to `8201d75de`.
- Included 10 commits across 5 categories: Features 2, Bug Fixes 4, Refactoring 1, Documentation 1, Chores 2.
- Most active scopes: web(3), test(1), web,auth(1), auth(1), gateway(1), postgres(1).
- Highlights: web: realistic mock demo + login-page entry and header icon fixes (#201); web: enable AI assistant with charted chat + enrich dashboard and entity data (#203); test: adapt TokenController test to httpOnly cookie auth; auth: runIgnore tenant check in pre-login token paths; gateway: use Object config for argument-less Authentic filter.

#### Features
- **web**: realistic mock demo + login-page entry and header icon fixes (#201) (`84110719e`)
- **web**: enable AI assistant with charted chat + enrich dashboard and entity data (#203) (`11fbf1d4b`)

#### Bug Fixes
- **test**: adapt TokenController test to httpOnly cookie auth (`a558490b9`)
- **auth**: runIgnore tenant check in pre-login token paths (`ef1d80df2`)
- **gateway**: use Object config for argument-less Authentic filter (`4713832b7`)
- **postgres**: register TenantLineHandler via auto-configuration (`d30b36df0`)

#### Refactoring
- **web,auth**: httpOnly cookie auth + CSP + dedup + type debt (`a73ba1267`)

#### Documentation
- rewrap project guides and README files (`50c6de943`)

#### Chores
- **web**: reformat mock seed, views and components (`2da8b2aa2`)
- **java**: reformat auth/agentic/data sources and tests (`8201d75de`)


### 📌 2026.8.6

_Generated on 2026-08-06._

#### Summary
- Generated from `8a333a279` to `73ff48bcb`.
- Included 13 commits across 7 categories: Security 1, Features 1, Bug Fixes 3, Refactoring 4, Build 1, CI 1, Chores 2.
- Most active scopes: api(5), docs(2), web(2), deps(1), security(1), agentic(1).
- Highlights: security: patch postcss/brace-expansion CVEs + link SECURITY.md (#168); web: static mock demo deployable to demo.dc3.site (#199); docs: restore 5-pillar nav and fix version switcher position (#167); api: dc3-api contract overhaul (auth JWT, history, presence, decision enum) (#193); agentic: run markdown sanitize test under jsdom (#176).

#### Security
- **security**: patch postcss/brace-expansion CVEs + link SECURITY.md (#168) (`22263fc48`)

#### Features
- **web**: static mock demo deployable to demo.dc3.site (#199) (`7802eeb99`)

#### Bug Fixes
- **docs**: restore 5-pillar nav and fix version switcher position (#167) (`4b8a18b10`)
- **api**: dc3-api contract overhaul (auth JWT, history, presence, decision enum) (#193) (`7c643a8e8`)
- **agentic**: run markdown sanitize test under jsdom (#176) (`e2527af78`)

#### Refactoring
- **api**: type OAuth grant_type as proto enum (#198) (`98d8eb80b`)
- **api**: type MCP audit status as proto enum (#197) (`3f7848735`)
- **api**: type MCP principal_type as proto enum (#196) (`4a69185d9`)
- **api**: type MCP risk_level as proto enum (#195) (`3a0396260`)

#### Build
- **deps**: bump pnpm/action-setup to v6.0.10 (#171) (`7c76f19c1`)

#### CI
- **web**: widen deploy-pages tolerance for Pages queue stalls (`c8e65ceaf`)

#### Chores
- **docs**: move docs site to standalone pnoker/iot-dc3-docs repo (#177) (`d06abff59`)
- reformat codebase (backend java/xml, docs, web) (#175) (`73ff48bcb`)


### 📌 2026.8.3

_Generated on 2026-08-03._

#### Summary
- Generated from `c02fc3b3f` to `8a333a279`.
- Included 22 commits across 5 categories: Security 2, Refactoring 4, Build 14, CI 1, Chores 1.
- Most active scopes: deps(10), quality(4), deps-dev(4), security(2), backend(1), ci(1).
- Highlights: security: harden log-injection + CAN command injection (#158); security: sanitize agentic assistant HTML with DOMPurify (XSS) (#157); quality: fix unused-param/confusing-sig/local-temp/js-comparison (12) (#163); quality: fix 27 medium-risk CodeQL alerts (null/exception/boundary/exposure) (#162); quality: migrate 23 deprecated API calls (#161).

#### Security
- **security**: harden log-injection + CAN command injection (#158) (`fa1cd49ba`)
- **security**: sanitize agentic assistant HTML with DOMPurify (XSS) (#157) (`67526972a`)

#### Refactoring
- **quality**: fix unused-param/confusing-sig/local-temp/js-comparison (12) (#163) (`00c67406b`)
- **quality**: fix 27 medium-risk CodeQL alerts (null/exception/boundary/exposure) (#162) (`4e2c5bc77`)
- **quality**: migrate 23 deprecated API calls (#161) (`8531e3b87`)
- **quality**: fix 21 low-risk CodeQL quality alerts (#160) (`852f0682d`)

#### Build
- **deps-dev**: bump eslint-plugin-vue in /dc3-web (#140) (`7e116b906`)
- **deps**: bump californium 4.0.0-M3 → 4.0.0-M6 + adapt CoapExchange import (#156) (`4dd477163`)
- **deps**: bump actions/setup-node from 6.5.0 to 7.0.0 (#153) (`f817ce4e2`)
- **deps**: bump actions/setup-java from 4.8.0 to 5.7.0 (#152) (`7cf76107c`)
- **deps**: bump mybatis-plus 3.5.16 → 3.5.17 + adapt IService/ServiceImpl imports (#154) (`5e0003f01`)
- **deps**: bump com.squareup.okhttp3:okhttp-bom from 5.3.2 to 5.4.0 (#142) (`231a44e3c`)
- **deps**: bump actions/upload-pages-artifact from 3.0.1 to 5.0.0 (#135) (`ba2c52228`)
- **deps**: bump docker/metadata-action from 5.10.0 to 6.2.0 (#130) (`344e1b107`)
- **deps-dev**: bump sass from 1.101.0 to 1.102.0 in /dc3-web (#139) (`152d6e9bf`)
- **deps-dev**: bump @playwright/test from 1.61.1 to 1.62.0 in /dc3-web (#138) (`69c43f320`)
- **deps-dev**: bump globals from 17.7.0 to 17.8.0 in /dc3-web (#137) (`cce10d17e`)
- **deps**: bump tools.jackson.core:jackson-databind (#136) (`e18dbef8d`)
- **deps**: bump softprops/action-gh-release (#134) (`9b61f5720`)
- **deps**: bump ossf/scorecard-action (#131) (`02b8bf95a`)

#### CI
- **backend**: run backend CI on every PR (fix required-checks BLOCKED) (#155) (`8ec3d3804`)

#### Chores
- **ci**: route dependabot PRs to develop (#151) (`8a333a279`)


### 📌 2026.7.27

_Generated on 2026-07-27._

#### Summary
- Generated from `db14f0f08` to `c02fc3b3f`.
- Included 11 commits across 4 categories: Features 6, Refactoring 3, Build 1, Tests 1.
- Most active scopes: driver(5), data(4), docs(2).
- Highlights: docs: add documentation versioning system; driver: configure driver buffer and mount container volume; driver: wire send-failure buffering and quartz republish; driver: add point value local buffer DAO and service; driver: introduce sqlite-jdbc dependency and buffer config properties.

#### Features
- **docs**: add documentation versioning system (`2a328a252`)
- **driver**: configure driver buffer and mount container volume (`7bf615b3b`)
- **driver**: wire send-failure buffering and quartz republish (`9f574d909`)
- **driver**: add point value local buffer DAO and service (`58d47179a`)
- **driver**: introduce sqlite-jdbc dependency and buffer config properties (`f019e2066`)
- **data**: add PointValueIngestBuffer with bounded queue and backpressure (`f8ddcf587`)

#### Refactoring
- **data**: remove PointValueJob, hand ingestion to buffer (`a1876727d`)
- **data**: route point value ingestion through bounded buffer (`6293bbdd1`)
- **data**: extend PointBatchProperties for bounded ingest buffer (`c02fc3b3f`)

#### Build
- **docs**: add makefile targets and pin node version (`e3d335b56`)

#### Tests
- **driver**: add point value buffer unit tests (`18eac7871`)


### 📌 2026.7.23

_Generated on 2026-07-23._

#### Summary
- Generated from `f2554b10f` to `db14f0f08`.
- Included 11 commits across 7 categories: Features 2, Bug Fixes 2, Performance 1, Documentation 1, Build 2, Chores 2, Other Changes 1.
- Most active scopes: docs(5), home(1), observability(1), gateway(1), infra(1), deps(1).
- Highlights: docs: locale-aware SEO with per-section og images and JSON-LD dates; observability: integrate opentelemetry tracing into request-id mechanism; docs: unify navbar background and fix sidebar scrollbar overlap; docs: bind topology aria-label correctly and meet AA contrast.

#### Features
- **docs**: locale-aware SEO with per-section og images and JSON-LD dates (`dd1ec9764`)
- **observability**: integrate opentelemetry tracing into request-id mechanism (`41c43b359`)

#### Bug Fixes
- **docs**: unify navbar background and fix sidebar scrollbar overlap (`bd3405a30`)
- **docs**: bind topology aria-label correctly and meet AA contrast (`242f03102`)

#### Performance
- **docs**: drop AdSense and preconnect analytics (`7aeece378`)

#### Documentation
- **home**: surface the AI pillar and add keyword home titles (`eb1f09593`)

#### Build
- **infra**: add jaeger tracing backend to optional docker-compose stack (`bed0877e0`)
- **deps**: add opentelemetry bom and tracing dependencies (`db14f0f08`)

#### Chores
- **docs**: drop unused deps and stale diagrams-src html (`d1f533bbc`)
- **gateway**: align mcp controller javadoc param descriptions (`91ccd25a5`)

#### Other Changes
- i18n(docs): translate frontend test-debugging FAQ to Chinese (`799765d88`)


### 📌 2026.7.19

_Generated on 2026-07-19._

#### Summary
- Generated from `c702b2a9a` to `f2554b10f`.
- Included 16 commits across 4 categories: Features 1, Bug Fixes 1, Documentation 1, Build 13.
- Most active scopes: deps(10), web(2), deps-dev(2), seo(1).
- Highlights: configure docs AdSense auto ads; web: restore frontend testing guardrails doc and fix test path.

#### Features
- configure docs AdSense auto ads (`f2554b10f`)

#### Bug Fixes
- **web**: restore frontend testing guardrails doc and fix test path (`829e82fe2`)

#### Documentation
- **seo**: move meta into dynamic transformHead and add llms.txt (`ef70a279c`)

#### Build
- **web**: bump frontend dependencies (`8b7c65715`)
- **deps**: bump pnoker/dc3-nginx from 2025.2 to 2025.9 in /dc3-web (#127) (`1dbb75739`)
- **deps**: bump pnoker/dc3-node from 2025.2 to 2025.9 in /dc3-web (#126) (`7e2d03009`)
- **deps-dev**: bump vite from 8.1.0 to 8.1.5 in /dc3-web (#124) (`2d064e336`)
- **deps-dev**: bump vue-tsc from 3.3.5 to 3.3.7 in /dc3-web (#122) (`1d2c51098`)
- **deps**: bump actions/checkout from 4.3.1 to 7.0.0 (#103) (`ac39d9d60`)
- **deps**: bump docker/login-action from 3.7.0 to 4.4.0 (#106) (`c13feb0b7`)
- **deps**: bump docker/setup-qemu-action from 3.7.0 to 4.2.0 (#105) (`583bd4993`)
- **deps**: bump com.infiniteautomation:bacnet4j from 6.0.1 to 6.1.0 (#110) (`ca359f6fc`)
- **deps**: bump org.snmp4j:snmp4j from 3.7.4 to 3.12.2 (#109) (`af02d9002`)
- **deps**: bump swagger-core.version from 2.2.47 to 2.2.52 (#108) (`f5d893dcb`)
- **deps**: bump docker/setup-buildx-action from 3.12.0 to 4.2.0 (#104) (`96d8e61e0`)
- **deps**: bump pnpm/action-setup (#102) (`f64327d4e`)


### 📌 2026.7.17

_Generated on 2026-07-17._

#### Summary
- Generated from `7596fd2e1` to `c702b2a9a`.
- Included 2 commits across 1 categories: Documentation 2.
- Most active scopes: site(1), seo(1).

#### Documentation
- **site**: consolidate top-level nav pillars and rehome technology-stack (`261af5721`)
- **seo**: add sitemap, robots.txt and social sharing meta (`c702b2a9a`)


### 📌 2026.7.16

_Generated on 2026-07-16._

#### Summary
- Generated from `2662eb2db` to `7596fd2e1`.
- Included 11 commits across 6 categories: Security 1, Bug Fixes 1, Refactoring 2, Documentation 5, CI 1, Chores 1.
- Most active scopes: security(1), opc-da(1), bo(1), config(1), driver(1).
- Highlights: security: redact identifiers and model names in logs; guard numeric parsing against NumberFormatException; bo: annotate inherited Lombok getters with @Override and add @ToString; config: drop redundant YAML blocks covered by shared profile bases.

#### Security
- **security**: redact identifiers and model names in logs (`741ad7cc4`)

#### Bug Fixes
- guard numeric parsing against NumberFormatException (`3f083e7ee`)

#### Refactoring
- **bo**: annotate inherited Lombok getters with @Override and add @ToString (`9bd4dd303`)
- **config**: drop redundant YAML blocks covered by shared profile bases (`fe28d4180`)

#### Documentation
- add Baidu Tongji analytics to the vitepress site (`d3b636f49`)
- add Google Analytics (GA4) to the vitepress site (`ff4a54d45`)
- **driver**: document dc3-common-driver public APIs, fields and constants (`d4bd33eef`)
- update pnpm-lock.yaml after removing mermaid dependencies (`d5ffa8795`)
- convert all 107 mermaid diagrams to self-contained SVG components (`7596fd2e1`)

#### CI
- pin GitHub Actions and CodeQL workflows to immutable commit hashes (`ad89fde9d`)

#### Chores
- **opc-da**: add missing @Override to openscada adapter methods (`0ff5f7a14`)


### 📌 2026.7.15

_Generated on 2026-07-15._

#### Summary
- Generated from `8d08826e2` to `2662eb2db`.
- Included 13 commits across 1 categories: Documentation 13.
- Most active scopes: auth(7), agentic(4), manager(2).

#### Documentation
- **manager**: document driver device grpc server helpers (`c8d1a0bf0`)
- **manager**: document driver point grpc server helpers (`e3633a971`)
- **auth**: document tenant membership, token, denylist and user helpers (`78168fc14`)
- **auth**: document resource sync builders and grouping helpers (`4e8dd8d04`)
- **auth**: document role and resource tree helpers (`da11d8874`)
- **auth**: document mcp runtime grpc server conversion helpers (`3693d81c2`)
- **agentic**: document action service impl helpers (`7675e39ba`)
- **agentic**: document model provider service impl helpers (`b31a4f6a9`)
- **agentic**: document attachment service impl helpers (`e3b356b82`)
- **auth**: document local credential service impl helpers (`c97b9eb3b`)
- **agentic**: document model config service impl private helpers (`61e1e6a87`)
- **auth**: document role principal bind service impl helpers (`92af0af23`)
- **auth**: document service account service impl helpers (`2662eb2db`)


### 📌 2026.7.14

_Generated on 2026-07-14._

#### Summary
- Generated from `e84d2c3ca` to `8d08826e2`.
- Included 29 commits across 7 categories: Security 1, Bug Fixes 5, Refactoring 4, Documentation 15, Build 1, CI 1, Chores 2.
- Most active scopes: manager(5), data(4), gateway(3), auth(2), driver,public(1), facade(1).
- Highlights: security: harden AES/RSA ciphers and cacerts import in KeyUtil/KeyStoreUtil; driver-coap: bind CoAP server to the configured serverHost; driver-fins: correct 32-bit read length and FLOAT write encoding; manager: add @Transactional to DriverServiceImpl delete/update; api: set size (not pages) when defaulting a null GrpcPage.

#### Security
- **security**: harden AES/RSA ciphers and cacerts import in KeyUtil/KeyStoreUtil (`8968d15e5`)

#### Bug Fixes
- **driver-coap**: bind CoAP server to the configured serverHost (`3a031073c`)
- **driver-fins**: correct 32-bit read length and FLOAT write encoding (`b01b2802d`)
- **manager**: add @Transactional to DriverServiceImpl delete/update (`83bcee395`)
- **api**: set size (not pages) when defaulting a null GrpcPage (`24cfe977f`)
- **gateway**: isolate MCP audit failures from the tool-call result (`03f1f7958`)

#### Refactoring
- **manager**: rename getPointByDeviceId to getCountByDeviceId (`3102df76a`)
- **center-single**: drop redundant @MapperScan (`d39456027`)
- **nginx**: move SSL certs into nginx/certs directory (`6cd05d2c6`)
- **docker**: remove duplicated driver target stages (`a32ac4272`)

#### Documentation
- **driver,public**: document sdk job lock converter and util helpers (`d5914ffe6`)
- **facade**: document guardOrThrow across grpc facades (`78ca24d4d`)
- **dal**: document group and label entity-type validators (`e61e829f1`)
- **data**: document notify and rule state service impl helpers (`10dd261e2`)
- **data**: document scanner, state-flip and notify adapter helpers (`239b2bc00`)
- **data**: document alarm engine private methods (`3a2de55f4`)
- **data**: document notify policy engine private methods (`e2f8c9a69`)
- **manager**: document remaining service impl private helpers (`109b1b071`)
- **manager**: document event and command param service methods (`68e53d55f`)
- **manager**: document dashboard helpers and command service methods (`97bdaaba1`)
- **auth**: document oauth runtime and resource sync private methods (`41a5444fa`)
- **auth**: document controller private helpers (`7082301e8`)
- **gateway**: document authentic filter helpers (`e39d5b50a`)
- **gateway**: document mcp gateway controller dispatch and client methods (`f244fa050`)
- **i18n**: make issue and PR templates bilingual (`46d788de7`)

#### Build
- **maven**: tidy .mvn config and use Central for wrapper download (`783e9463f`)

#### CI
- optimize GitHub Actions and dependabot coverage (`8d08826e2`)

#### Chores
- **web,docs**: reindent Vue template interpolation blocks (`956b710a6`)
- **mapper**: reindent SQL in MyBatis mapper XML (`5d0ce7353`)


### 📌 2026.7.13

_Generated on 2026-07-13._

#### Summary
- Generated from `4ea339ba1` to `e84d2c3ca`.
- Included 4 commits across 2 categories: Documentation 2, Chores 2.
- Most active scopes: i18n(1).

#### Documentation
- fix outdated cross-repo references in dc3-web/AGENTS.md (`cfeb86dc9`)
- **i18n**: add Korean, Spanish, Russian README and architecture diagrams (`e84d2c3ca`)

#### Chores
- consolidate dc3-web config and legal files into root (`99811cdea`)
- align Javadoc @param tags and docs table formatting (`d52b10666`)


### 📌 2026.7.10

_Generated on 2026-07-10._

#### Summary
- Generated from `77a20466b` to `4ea339ba1`.
- Included 22 commits across 3 categories: Security 1, Documentation 18, Chores 3.
- Most active scopes: readme(5), manager(2), agentic(2), data(2), web(2), deps(1).
- Highlights: deps: override vulnerable npm transitive deps.

#### Security
- **deps**: override vulnerable npm transitive deps (`76c8aafb9`)

#### Documentation
- **readme**: align quickstart docs guidance (`1ea6fb123`)
- **readme**: move reference architecture details to docs (`3a6f99151`)
- **readme**: add documentation site link (`bdd947004`)
- **readme**: streamline readme and docs navigation (`358cfd2f6`)
- **readme**: convert svg to png for gitee rendering and soften ai wording (`2b1ae4f53`)
- move superpowers out of iot-dc3 to parent workspace (`7ce64ac28`)
- **common**: document gateway, mqtt and registrar contract methods (`f4e7bef80`)
- **drivers**: document plcs7 and melsec protocol helpers (`398026fc9`)
- **driver**: document grpc list clients and sender status helper (`ebe69c58f`)
- **facade**: document status health contract and grpc support helpers (`9406b338c`)
- **public**: document keystore import, hmac config and path helpers (`2f5150533`)
- **manager**: document controller validation helpers (`ebbf885b1`)
- **manager**: document dashboard stats and command/event list contracts (`6fe25c657`)
- **auth**: document audit log and credential service contract methods (`6b36c4854`)
- **agentic**: document tools helpers, factory methods and entity fields (`ad74df0d9`)
- **agentic**: document service contracts and chat orchestration methods (`4871f0ecc`)
- **data**: document dashboard alert and value mapper queries (`6d06098fd`)
- **data**: document event command history service contracts and helpers (`81819d290`)

#### Chores
- **web**: reformat frontend with IDEA code style (`d47d125af`)
- align license headers to AGPL across maven wrapper, docs and web assets (`d228f4e25`)
- **web**: remove prettier, use IDE formatter for code style (`4ea339ba1`)


### 📌 2026.7.9

_Generated on 2026-07-09._

#### Summary
- Generated from `0d8390db5` to `77a20466b`.
- Included 23 commits across 6 categories: Features 3, Bug Fixes 1, Refactoring 2, Documentation 10, Tests 2, Chores 5.
- Most active scopes: manager(2), auth(2), data(2), facade(2), driver(2), common(2).
- Highlights: observability: add Grafana dashboard and PostgreSQL init scripts; dc3-web: add unified logger and apply prettier formatting; common: support request id propagation across HTTP, gRPC and RabbitMQ; driver: validate null payload before logging in rabbit receivers; common: remove unused @Logs annotation and aspect.

#### Features
- **observability**: add Grafana dashboard and PostgreSQL init scripts (`c35708859`)
- **dc3-web**: add unified logger and apply prettier formatting (`afdac8930`)
- **common**: support request id propagation across HTTP, gRPC and RabbitMQ (`f9c21b5a7`)

#### Bug Fixes
- **driver**: validate null payload before logging in rabbit receivers (`22370e9fe`)

#### Refactoring
- **common**: remove unused @Logs annotation and aspect (`1636d64c5`)
- **api**: reorganize proto files from center to common package (`77a20466b`)

#### Documentation
- **center**: remove misplaced javadoc between annotations (`0ecc7ecf2`)
- **drivers**: align since tags with version across protocol drivers (`0fd8d5b6b`)
- **driver**: fix read write service since tags and register builder return (`8d6b7635c`)
- **facade**: align since tags with version on 2026 apis (`9341ae510`)
- **model**: fix base builder param types and document mcp tool dto (`03e6ddb0b`)
- **agentic**: document agent runtime loop and model config fields (`91d933e62`)
- **data**: fix broken javadoc and document rabbit receivers (`dc0a1d478`)
- **manager**: fix service interface javadoc and placeholder comments (`83e22de7e`)
- **auth**: fix token service javadoc and service impl placeholders (`e428ee6f4`)
- update documentation, architecture diagrams and superpowers analysis (`992026a26`)

#### Tests
- **data**: pin alarm silence windows and trigger_count sql (`e9bb99954`)
- **facade**: cover tenant context lifecycle and page count (`bc04e8eab`)

#### Chores
- **manager**: drop unused devicebo import in devicemanager (`434034bc0`)
- **auth**: align oauth mcp javadoc param columns (`5e2ea4625`)
- apply indentation and formatting fixes (`838749b94`)
- adjust log levels and clean up imports in receivers (`9030d70cc`)
- rename unused catch parameters to 'ignored' (`f26316403`)


### 📌 2026.7.3

_Generated on 2026-07-03._

#### Summary
- Generated from `fc6c53e00` to `0d8390db5`.
- Included 1 commits across 1 categories: Bug Fixes 1.
- Most active scopes: tenant(1).
- Highlights: tenant: bind tenant context on gRPC server paths.

#### Bug Fixes
- **tenant**: bind tenant context on gRPC server paths (`0d8390db5`)


### 📌 2026.7.2

_Generated on 2026-07-02._

#### Summary
- Generated from `4dbf7d1ff` to `fc6c53e00`.
- Included 17 commits across 5 categories: Features 6, Bug Fixes 1, Refactoring 5, Documentation 4, Chores 1.
- Most active scopes: tenant(13), superpowers(4).
- Highlights: tenant: wrap gateway MCP public endpoint in runIgnore; tenant: wrap OAuth public endpoints in runIgnore; tenant: wrap request-time tenant-free paths in runIgnore; tenant: register TenantLineInnerInterceptor + agentic BO implements TenantOwned; tenant: add TenantLineHandlerImpl with fail-closed + whitelist.

#### Features
- **tenant**: wrap gateway MCP public endpoint in runIgnore (`a8ded884f`)
- **tenant**: wrap OAuth public endpoints in runIgnore (`5c6477d21`)
- **tenant**: wrap request-time tenant-free paths in runIgnore (`6b7b23791`)
- **tenant**: register TenantLineInnerInterceptor + agentic BO implements TenantOwned (`9c50831f5`)
- **tenant**: add TenantLineHandlerImpl with fail-closed + whitelist (`478eeae54`)
- **tenant**: add TenantNotScopedException + runIgnoreAction + 500 mapping (`ef7a4057c`)

#### Bug Fixes
- **tenant**: bind tenant context in DriverRegisterServiceImpl gRPC paths (`5bcd128db`)

#### Refactoring
- **tenant**: remove redundant hand filter in manager core (covered by interceptor) (`507c7fdfb`)
- **tenant**: remove redundant hand filter in manager attribute/param/config (covered by interceptor) (`c2010af0c`)
- **tenant**: remove redundant hand filter in data (covered by interceptor) (`8e059f699`)
- **tenant**: remove redundant hand filter in agentic (covered by interceptor) (`196e5cc6f`)
- **tenant**: remove redundant hand filter in dal (covered by interceptor) (`44f3cde84`)

#### Documentation
- **superpowers**: add tenant cleanup redundant filter spec and plan (`90d0b77ac`)
- **superpowers**: mark D-1 fixed in identification defect analysis (`2eb2056a6`)
- **superpowers**: add d1 tenant-line interceptor fix plan (`94c9a3c6a`)
- **superpowers**: add d1 tenant-line interceptor fix design (`fc6c53e00`)

#### Chores
- **tenant**: polish runIgnore indentation + javadoc + whitelist test coverage (`0157e9d93`)


### 📌 2026.7.1

_Generated on 2026-07-01._

#### Summary
- Generated from `2984dd3a2` to `4dbf7d1ff`.
- Included 16 commits across 2 categories: Bug Fixes 1, Documentation 15.
- Most active scopes: superpowers(11), foundations(2), driver(1).
- Highlights: driver: resolve 4 real defects in OPC-UA and MQTT drivers (#112).

#### Bug Fixes
- **driver**: resolve 4 real defects in OPC-UA and MQTT drivers (#112) (`91d871723`)

#### Documentation
- **superpowers**: fix file location and accuracy per final review (`21b5b2bfb`)
- **superpowers**: add identification-layer slice retrospective (`4c669172b`)
- **superpowers**: add identification identity & tenant isolation defect analysis (`79bd038a7`)
- **foundations**: add verified academic citations to identification page (`576b60c08`)
- **superpowers**: add identification knowledge entries with verified citations (`9f736483e`)
- **superpowers**: add identification-layer slice plan (`37cdbf5a5`)
- **superpowers**: add identification-layer slice design (`148703348`)
- enrich sensing page with verified academic citations + calibration defect analysis (#113) (`48cace5fa`)
- **superpowers**: add sensing-layer slice retrospective (`5a562f703`)
- **superpowers**: add sensing calibration defect analysis (`c04f50fe0`)
- **foundations**: add verified academic citations to sensing page (`c02b2c456`)
- **superpowers**: add sensing knowledge entries with verified citations (`19eac8308`)
- enrich protocol-layer pages with verified academic citations + defect analysis (#111) (`2ec81eb0e`)
- **superpowers**: add sensing-layer slice design and plan (`ed9d767ff`)
- **superpowers**: mark D1-D4 real defects as fixed in protocol-defects analysis (`4dbf7d1ff`)


### 📌 2026.6.30

_Generated on 2026-06-30._

#### Summary
- Generated from `819ad72f1` to `2984dd3a2`.
- Included 10 commits across 1 categories: Documentation 10.
- Most active scopes: superpowers(7), foundations(2), drivers(1).

#### Documentation
- **superpowers**: unify citation style (inline) and reference format (GB/T 7714) (`31fe615d7`)
- **superpowers**: add protocol-layer slice retrospective (`edf6ff210`)
- **superpowers**: add protocol-layer defect analysis (reuse + incremental) (`65cf4d367`)
- **drivers**: add protocol-adaptation-layer rationale with verified citations (`75f22828b`)
- **foundations**: add verified academic citations to iot-protocols page (`e8235ae8a`)
- **foundations**: add verified academic citations to fieldbus page (`9fbe9588f`)
- **superpowers**: mark iot-protocols entries verified by independent pdftotext check (`dc0768774`)
- **superpowers**: add iot-protocols knowledge entries with verified citations (`efbf1d3e1`)
- **superpowers**: add fieldbus knowledge entries with verified citations (`14ccc947e`)
- **superpowers**: add protocol-layer slice design and plan (`2984dd3a2`)


### 📌 2026.6.29

_Generated on 2026-06-29._

#### Summary
- Generated from `e133f5a02` to `819ad72f1`.
- Included 14 commits across 5 categories: Features 1, Bug Fixes 5, Refactoring 4, Documentation 2, Chores 2.
- Most active scopes: docs(7), dc3-web(2).
- Highlights: docs: add fullscreen zoom to architecture diagrams; docs: replace Vue template interpolation with static text in panorama SVGs; docs: align Architecture.vue stroke, dash, and fill styles with architecture SVG; docs: scale down panorama stroke-widths to match architecture visual thickness; docs: align panorama stroke-dasharray with architecture SVG style.

#### Features
- **docs**: add fullscreen zoom to architecture diagrams (`a78da8fc6`)

#### Bug Fixes
- **docs**: replace Vue template interpolation with static text in panorama SVGs (`d9de19a2d`)
- **docs**: align Architecture.vue stroke, dash, and fill styles with architecture SVG (`437c8b36f`)
- **docs**: scale down panorama stroke-widths to match architecture visual thickness (`f38519cba`)
- **docs**: align panorama stroke-dasharray with architecture SVG style (`af953dfe0`)
- **docs**: remove dead link to frontend-testing-guardrails (`819ad72f1`)

#### Refactoring
- remove root package.json, set hooksPath directly from dc3-web (`e739363df`)
- move lint-staged governance to dc3-web, keep husky infra at root (`dc570b553`)
- **dc3-web**: remove Tauri desktop shell (`b3d3962c8`)
- **docs**: rename svg architecture diagram components (`ef66a38a3`)

#### Documentation
- polish README table formatting and line breaks (`501f0a9a7`)
- update git hooks documentation from githooks to husky (`aa34de64a`)

#### Chores
- **dc3-web**: apply updated prettier bracket spacing and endOfLine (`a855c6f38`)
- sync develop with main (panorama SVGs and dead link fix) (`85fc616c2`)


### 📌 2026.6.28

_Generated on 2026-06-28._

#### Summary
- Generated from `93b111e3f` to `e133f5a02`.
- Included 42 commits across 8 categories: Features 3, Bug Fixes 5, Refactoring 3, Documentation 6, Build 13, CI 2, Tests 1, Chores 9.
- Most active scopes: deps(12), docs(9), web(4), docker(2).
- Highlights: docs: add ja/vi panorama SVGs and harmonize color scheme with architecture SVGs; docs: add image zoom via medium-zoom and enhance code block styling; docs: add social links, reorder nav, FAQ, frontend guide, and IDEA setup; docs: normalize panorama stroke widths to match architecture SVG style; docs: add xmlns attribute to panorama SVGs for GitHub/Gitee rendering.

#### Features
- **docs**: add ja/vi panorama SVGs and harmonize color scheme with architecture SVGs (`f2f5b744c`)
- **docs**: add image zoom via medium-zoom and enhance code block styling (`9abd1c67b`)
- **docs**: add social links, reorder nav, FAQ, frontend guide, and IDEA setup (`0efdf6303`)

#### Bug Fixes
- **docs**: normalize panorama stroke widths to match architecture SVG style (`90a272d88`)
- **docs**: add xmlns attribute to panorama SVGs for GitHub/Gitee rendering (`d7c39d2eb`)
- **docs**: remove .html extensions from README links to match VitePress cleanUrls (`3439b3d1e`)
- remove Chinese text from production code (`f29f01ccd`)
- **docs**: correct broken markdown bold syntax in zh/en index pages (`ef01d7de3`)

#### Refactoring
- **docs**: unify hero and nav logo to single logo.svg (`750219b52`)
- migrate images from dc3/images to docs/public/images (`c808d4549`)
- **docs**: extract all i18n strings to locales/{lang}.json (`49f018cb1`)

#### Documentation
- 新增四层参考架构组件，优化架构图亮色主题与 README 架构概览 (`3c725da38`)
- add open-source license explanation pages in zh and en (`b9ff83bc1`)
- replace code of conduct include with full inline content (`a1d249bd1`)
- add TODO remediation plan covering 19 documentation improvements (`fb043ec8f`)
- update all README variants with latest logo, archive notices, and localized AI hints (`812e24403`)
- 完善工作流与贡献指南 (`e133f5a02`)

#### Build
- **deps**: bump actions/dependency-review-action from 4 to 5 (`0aa5c9e62`)
- **deps**: bump actions/upload-artifact from 4 to 7 (`2d1b78b74`)
- **deps**: bump softprops/action-gh-release from 2 to 3 (`b0c8e9236`)
- **deps**: bump jakarta.xml.bind-api from 4.0.2 to 4.0.5 (`5f6ac1732`)
- **deps**: bump actions/deploy-pages from 4 to 5 (`e8b7730cd`)
- **deps**: bump time from 0.3.37 to 0.3.51 in /dc3-web/src-tauri (`46f2ec088`)
- **deps**: bump tokio from 1.42.0 to 1.46.1 in /dc3-web/src-tauri (`5c158e5e4`)
- **deps**: bump bytes from 1.9.0 to 1.12.0 in /dc3-web/src-tauri (`4f1d91226`)
- **deps**: bump actions/setup-node from 4 to 6 (`5d4760320`)
- **deps**: bump crossbeam-channel from 0.5.13 to 0.5.15 (`72b75065e`)
- **deps**: bump oshi-core from 7.1.0 to 7.3.2 (`70e193c59`)
- **deps**: bump mermaid from 11.15.0 to 11.16.0 in /docs (`ef0cfbd45`)
- add Maven Wrapper and expand docker-compose with all driver services (`9d69edd0c`)

#### CI
- **docker**: add 20 missing drivers to docker-ci services list (#101) (`cfb2183bc`)
- **docker**: add 20 missing drivers to docker-ci services list (#100) (`4454d84e8`)

#### Tests
- **web**: update component test assertions for MiniAreaChart and ThingsCardActions (`7bfc9c4a8`)

#### Chores
- consolidate CI, docs, and issue templates (#98) (`e01c8deda`)
- consolidate CI, docs, and issue templates (#99) (`72a5fceac`)
- **web**: update project metadata, dependencies, and static assets (`ee80e72cf`)
- **web**: apply consistent formatting to TypeScript type definitions (`4104c5162`)
- **web**: apply consistent formatting to Vue components (`7b16c7332`)
- apply consistent formatting to docs, config, and root files (`f301e41c6`)
- remove unused public assets and orphan documentation (`e5b7d676d`)
- remove old dc3/images directory after migration to docs/public/images (`ef0185e7e`)
- merge iot-dc3-web frontend into monorepo (`2198d7fff`)


### 📌 2026.6.26

_Generated on 2026-06-26._

#### Summary
- Generated from `e61b62039` to `93b111e3f`.
- Included 21 commits across 5 categories: Bug Fixes 3, Documentation 11, CI 2, Tests 1, Chores 4.
- Most active scopes: site(5), agentic(2), ci(2), git(2).
- Highlights: agentic: align victools jsonschema to 5.0.0 and update tool API tests; agentic: add okhttp client deps and migrate tool API to spring-ai 2.0.0; ci: rename PR template to canonical uppercase and apply unified template.

#### Bug Fixes
- **agentic**: align victools jsonschema to 5.0.0 and update tool API tests (`6ecd91b1b`)
- **agentic**: add okhttp client deps and migrate tool API to spring-ai 2.0.0 (`a15795019`)
- **ci**: rename PR template to canonical uppercase and apply unified template (`8b731cf94`)

#### Documentation
- document branch model and release flow in CONTRIBUTING (`620be9905`)
- unify PR template, add issue templates (`d69c22af1`)
- add git workflow design spec and implementation plan (`17eceb7ad`)
- document branch model and semver release flow; align changelog tag pattern (`e89f4cc1d`)
- add PR and issue templates (`2ba4f73b7`)
- comprehensive documentation overhaul across English and Chinese sites (`1c11831cb`)
- **site**: apply verification fixes to remaining driver and foundation pages (`5f1ab36ad`)
- **site**: fix factual issues flagged by independent source verification (`c291c6f0a`)
- **site**: translate .vitepress code comments to English and calm hero animations (`4b190c1d4`)
- **site**: link data-plane, auth-rbac and ai hubs to their foundations chapters (`dbe4d6bc8`)
- **site**: deepen all 28 driver pages and add foundations back-links (`93b111e3f`)

#### CI
- trim master branch, rewire docker tag to v* (`8a4cd8817`)
- drop release branch, rewire triggers to develop/main and v* tags (`dd3ee73fb`)

#### Tests
- **ci**: fix timezone-fragile test and scope coverage check to aggregate job (`b8d4169e1`)

#### Chores
- **git**: add semver tag script and make tag target (`9914e0a1f`)
- **git**: rewrite tag.sh to semver on main with dry-run (`a32354c60`)
- fix table alignment and whitespace in README files (`d18bf4d03`)
- fix formatting in Java source files and tests (`a6c63a9c3`)


### 📌 2026.6.25

_Generated on 2026-06-25._

#### Summary
- Generated from `a6846212f` to `e61b62039`.
- Included 10 commits across 4 categories: Bug Fixes 1, Documentation 7, CI 1, Chores 1.
- Most active scopes: site(6), views(1), theme(1).
- Highlights: views: resolve 76 vue-tsc el-table row type errors.

#### Bug Fixes
- **views**: resolve 76 vue-tsc el-table row type errors (`2e387cd05`)

#### Documentation
- **site**: add IoT foundations pillar — four-layer knowledge woven with DC3 (`0a33fb47d`)
- **site**: fix language switcher, restyle mermaid, add bespoke flow diagrams (`147368a7e`)
- **site**: refine top nav into five pillars, add glossary and driver matrix (`cfd5a2a85`)
- **site**: add IA-refresh spec and P0 plan (`1701413a4`)
- **site**: fix browser title; add hero wave background and converging particle field (`d06037ee1`)
- **site**: live hero logo, unified architecture component, grouped sidebar (`f6990019b`)
- fix tenant-isolation fiction + data-plane schema/compression + MCP endpoint (`e61b62039`)

#### CI
- defer pnpm version to packageManager, fixing action-setup conflict (`99da7e0c2`)

#### Chores
- **theme**: unify primary color to brand blue #1296db (`fd50965bf`)


### 📌 2026.6.24

_Generated on 2026-06-24._

#### Summary
- Generated from `43d917574` to `a6846212f`.
- Included 17 commits across 6 categories: Security 1, Bug Fixes 2, Documentation 8, Build 2, CI 2, Chores 2.
- Most active scopes: docs(3), deps(2), site(2), assets(1), iot-dc3(1), drivers(1).
- Highlights: manage docs npm deps via dependabot; scope codeql security-events:write to job level; auth,gateway: mark OAuth2.1/MCP protocol endpoints @PublicEndpoint to pass controller permission audit; docs: transparent home navbar so hero title isn't occluded; restore doc-page header divider to match sidebar.

#### Security
- manage docs npm deps via dependabot; scope codeql security-events:write to job level (`0f0eac8c2`)

#### Bug Fixes
- **auth,gateway**: mark OAuth2.1/MCP protocol endpoints @PublicEndpoint to pass controller permission audit (`8b507b313`)
- **docs**: transparent home navbar so hero title isn't occluded; restore doc-page header divider to match sidebar (`85d7f300d`)

#### Documentation
- overhaul site with bilingual i18n, mermaid, and expanded guides (`8c0f1b00a`)
- point claude guidance to root agents file (`9d7ad679f`)
- **iot-dc3**: refresh multilingual README with sharper AI-native positioning and design principles (`9aa6a425d`)
- **drivers**: add bilingual reference pages for all 28 drivers + catalog (`c470de529`)
- **concepts**: add 9 bilingual core-concept reference pages (`77273d2b2`)
- **home**: add recolored logo hero image (azure gradient + soft glow), floating instead of bordered card (`11d2d0176`)
- **site**: switch base to / for docs.dc3.site custom domain (fix icon/lang-redirect paths) (`64035dd72`)
- **site**: logo-blue retheme, grouped sidebar, frosted header, flat cards; relocate vitepress project under docs/ (`a6846212f`)

#### Build
- **deps**: upgrade spring-ai to 2.0.0 ga and drop milestone repository (`a620afb08`)
- **deps**: bump frontend dependencies and pnpm to 11.8.0 (`774ca580d`)

#### CI
- **docs**: point pnpm/action-setup at docs/package.json for packageManager (`181c947fe`)
- **docs**: build on Node 22 and use packageManager pnpm (fix node:sqlite / pnpm Node>=22.13 requirement) (`2030eab43`)

#### Chores
- remove obsolete dc3-stack-test skill (`c564ba014`)
- **assets**: switch logo and favicon from png/ico to svg (`663bfb8de`)


### 📌 2026.6.22

_Generated on 2026-06-22._

#### Summary
- Generated from `f21ec89b1` to `43d917574`.
- Included 25 commits across 8 categories: Security 1, Features 2, Bug Fixes 5, Refactoring 4, Documentation 9, Build 1, Tests 2, Chores 1.
- Most active scopes: driver(7), common(4), auth(3), readme(1), gateway(1), api(1).
- Highlights: driver: bind jdbc write value as a parameter to prevent sql injection; settings: add MCP connection/client/tool management pages; seed: add MCP connection/client/tool management menus; driver: fail fast in dlms/iec104 stubs instead of fabricating read/write success; driver: set coap driver type to DRIVER_CLIENT.

#### Security
- **driver**: bind jdbc write value as a parameter to prevent sql injection (`9aa5a6bdb`)

#### Features
- **settings**: add MCP connection/client/tool management pages (`3a88a643e`)
- **seed**: add MCP connection/client/tool management menus (`705286622`)

#### Bug Fixes
- **driver**: fail fast in dlms/iec104 stubs instead of fabricating read/write success (`ec52b1a7f`)
- **driver**: set coap driver type to DRIVER_CLIENT (`a3032fbc6`)
- **driver**: stop mqtt validate() requiring point attributes as driver config (`ad7973e08`)
- **common**: correctly convert PEM certificates and keys in X509Util (`c2c9a690a`)
- **auth**: manage client_credentials MCP connections by creator (`43d917574`)

#### Refactoring
- **manager**: move @EnableAsync to ManagerInitRunner auto-config (`bf8f8fefe`)
- **data**: drop unused TaskScheduler bean and @EnableScheduling (`d5227d0e8`)
- **auth**: accept domain BO on oauth write paths (`141297ddb`)
- **auth**: use domain enums for oauth/mcp VO fields (`e7ff516ab`)

#### Documentation
- correct Node/pnpm versions and stale monorepo paths (`a0dea62bc`)
- **readme**: add READMEs for previously undocumented modules (`98f0ac312`)
- **driver**: fix attribute classification and flag WIP mqtt driver (`7320b9918`)
- **common**: fix thread and mqtt configuration examples (`49892923b`)
- **gateway**: correct routing table and service addressing (`a8ba610df`)
- **api**: correct gRPC RPC names, import paths, and missing services (`abd826bf6`)
- **common**: fill or drop empty comment placeholders and add missing auth javadoc (`f74d16b38`)
- **common**: tighten javadoc and drop redundant comments across modules (`5df7998d4`)
- refine x-dc3-ai standard and annotation-rollout specs (`91643afed`)

#### Build
- align Dockerfile pnpm to packageManager 11.3.0 (`51a162a1e`)

#### Tests
- **driver**: add postgresql testcontainers integration test for jdbc io (`46f8243c0`)
- **driver**: add minimal unit tests for 13 previously untested drivers (`452ed896f`)

#### Chores
- align .env.example image tag default to 2026.6 (`d5ac154ae`)


### 📌 2026.6.19

_Generated on 2026-06-19._

#### Summary
- Generated from `a14b01007` to `f21ec89b1`.
- Included 41 commits across 7 categories: Features 14, Bug Fixes 5, Refactoring 7, Documentation 4, Build 1, Tests 4, Chores 6.
- Most active scopes: auth(21), settings(3), registrar(3), agentic(2), data(2), manager(2).
- Highlights: settings: familiarize pages with EntityListPage and show entity names; auth: add principal list_by_ids endpoint for name resolution; auth: annotate all endpoints with x-dc3-ai and add ratchet annotation gate; agentic: annotate all endpoints with x-dc3-ai and add ratchet annotation gate; data: annotate all endpoints with x-dc3-ai and add ratchet annotation gate.

#### Features
- **settings**: familiarize pages with EntityListPage and show entity names (`4a945ce9c`)
- **auth**: add principal list_by_ids endpoint for name resolution (`9e7fd1ca5`)
- **auth**: annotate all endpoints with x-dc3-ai and add ratchet annotation gate (`74feecdaf`)
- **agentic**: annotate all endpoints with x-dc3-ai and add ratchet annotation gate (`d828b57a0`)
- **data**: annotate all endpoints with x-dc3-ai and add ratchet annotation gate (`44fa6ac4d`)
- **alarm**: rename event views to alarm, add alarm routes and i18n, extend login and settings nav (`a129c90fa`)
- **manager**: annotate all endpoints with x-dc3-ai and add ratchet annotation gate (`04313eb9f`)
- **registrar**: add controller annotation gate and make target (`12d611d31`)
- **registrar**: validate request body and param descriptions in annotation validator (`ed589109b`)
- **manager**: annotate list-drivers with x-dc3-ai as path-b canary (`fb84a539e`)
- **registrar**: add build-time x-dc3-ai annotation validator (`9d93fd3b9`)
- **auth**: source mcp tool quality from openapi json at catalog refresh (`ef2e192cd`)
- **auth**: parse x-dc3-ai and operation text into ToolQuality (`069829f60`)
- **auth**: add ToolQuality holder for openapi-sourced tool metadata (`d8d1367de`)

#### Bug Fixes
- **settings**: register EnableFlagSegmented in Principal page (`2e84c1ce4`)
- **auth**: make audit log queries postgres-compatible (`af0f23ad1`)
- **auth**: read oauth token/revoke form via ServerWebExchange, not @RequestBody (`3738ba9e9`)
- **auth**: re-catalog mcp tools on quality-only edits in refresh change-detection (`b83ccc831`)
- **auth**: persist enable_flag on mcp tool catalog update so hidden toggle applies (`2623ace88`)

#### Refactoring
- **seed**: restructure settings menu into eight groups (`44547e267`)
- **auth**: reclassify oauth client registration dto as vo (`c2b4e4285`)
- **auth**: move mcp/oauth request payloads to vo/query with mapstruct builders (`56562d485`)
- **auth**: remove dead inputSchemasByApiCode superseded by toolQualityByApiCode (`e7fc8cf2c`)
- **auth**: reorganize MCP tool quality mapping and annotation validation (`7e103e1db`)
- **auth**: remove legacy scanner-sourced x-dc3-ai path end-to-end (`b80909565`)
- **auth**: drop api_ext ai reads and heuristics from tool candidate sql (`c6de71bee`)

#### Documentation
- reflow settings design and entity-list-engine plan (`7da125f28`)
- **auth**: clarify toolChanged covers quality columns not identity columns (`27f477abf`)
- **mcp**: add SP2 x-dc3-ai annotation rollout spec (`d66ce3e8e`)
- **mcp**: add x-dc3-ai standard and parse-pipeline spec and plan (`af7f99c52`)

#### Build
- **deploy**: add postgres/rabbitmq dependency assets and regenerate openapi snapshots (`5131b3756`)

#### Tests
- **settings**: add page smoke and structural guardrails (`c7dc85d9e`)
- **auth**: guard api_code format consistency across json and db sides (`69ada6aab`)
- **auth**: tighten x-dc3-ai proof to annotation conversion only (`1ec51b3af`)
- **auth**: prove swagger-core emits x-dc3-ai extension (`8658d6503`)

#### Chores
- apply eslint import and attribute ordering (`b6f3f983b`)
- **auth**: regenerate openapi snapshots with x-dc3-ai for all 334 endpoints (`5cead0fad`)
- **model**: remove dead CmdParameterDTO (`1dc0e6b21`)
- **data**: align javadoc params and license headers (`3c812a2ee`)
- **auth**: fix import order and @Operation summary spacing (`1355e6a58`)
- **agentic**: normalize import order and constructor indentation (`f21ec89b1`)


### 📌 2026.6.18

_Generated on 2026-06-18._

#### Summary
- Generated from `fe48fd3ff` to `a14b01007`.
- Included 46 commits across 7 categories: Features 8, Bug Fixes 3, Refactoring 18, Documentation 11, Build 2, Tests 3, Chores 1.
- Most active scopes: settings(16), auth(5), common(3), agentic(3), data(3), manager(2).
- Highlights: common: add RequirePasswordChangeFlagEnum for local credentials; mcp: authorize high-risk tool calls and harden auth lifecycle; common: add thread-bound TenantContextHolder for tenant-line integration; settings: add form/payload hooks; migrate menu tree page; settings: row-aware relations + link column; migrate resource tree page.

#### Features
- **common**: add RequirePasswordChangeFlagEnum for local credentials (`2d7f97070`)
- **mcp**: authorize high-risk tool calls and harden auth lifecycle (`a94b2464a`)
- **common**: add thread-bound TenantContextHolder for tenant-line integration (`c084fa4bd`)
- **settings**: add form/payload hooks; migrate menu tree page (`acb835f3d`)
- **settings**: row-aware relations + link column; migrate resource tree page (`32c73d907`)
- **settings**: make treeSelect form-aware, restore group parent filtering (`b8d22cffa`)
- **settings**: add EntityListPage component and composable test (`43f5d1334`)
- **settings**: add useEntityListPage composable for list engine (`2302e9839`)

#### Bug Fixes
- **settings**: strip empty user phone/email on submit, restore maxlength (`b5674c316`)
- **settings**: normalize group root parent to null, restore root label (`293ba32fa`)
- **settings**: correct entity list config reactivity, payload test, dedup columns (`ce3381f72`)

#### Refactoring
- **common**: split ResponseEnum into SuccessCode/ErrorCode and align body code with HTTP status (`d2c897846`)
- **constant**: move WindowModeEnum to dc3-common-constant (`8f1865262`)
- **repository**: replace WindowAggregateRequest with WindowAggregateQuery (`694ebad0b`)
- **agentic**: rename Response classes to VO (`da601b12a`)
- **auth**: bind OAuth runtime settings via @ConfigurationProperties (`a230ffd9f`)
- **data**: rename AlertBulkConfirmRequest to AlertBulkConfirmVO (`d6171c9d0`)
- **agentic**: replace request DTOs with VO/DTO/BO entities (`bd15ff248`)
- **api**: send snake_case query params for audit endpoints (`06dadbd95`)
- **auth**: use snake_case request params on the MCP audit endpoint (`a35f01c4c`)
- **manager**: drop IService inheritance from TopicService (`7a4e74b4f`)
- **data**: accept BO instead of VO for command and event write paths (`654f3173f`)
- **auth**: return BO/VO instead of DO from audit-log and tenant-membership services (`7a694ce4b`)
- **auth**: remove unused RequirePermission annotation and tidy up (`123a5a5bb`)
- **settings**: migrate role page to config-driven entity list engine (`0b96691f2`)
- **settings**: migrate user page to config-driven entity list engine (`98bd46e22`)
- **settings**: migrate read-only api page to config-driven entity list engine (`36210916a`)
- **settings**: migrate group page to config-driven entity list engine (`1551ca6ea`)
- **settings**: migrate label page to config-driven entity list engine (`228bb2910`)

#### Documentation
- **gateway**: add openapi operations and javadoc to mcp gateway (`2f646bf3e`)
- **agentic**: normalize controller javadoc (`f217460c3`)
- **data**: normalize controller javadoc and openapi parameters (`bbd054d0c`)
- **auth**: normalize controller javadoc and add oauth openapi operations (`cc925a8f3`)
- **manager**: normalize controller javadoc and openapi parameters (`cc7ec6bb6`)
- **dc3-common**: normalize @Schema/@Parameter across API layer (`8995dd5ab`)
- align Make command references with new compose shortcuts (`cba2d5478`)
- **agents**: document the read-only projection VO exception (`582f103d4`)
- **openapi**: enrich operation and schema docs for MCP tool exposure (`c6768b5fa`)
- mark identity and mcp design proposals as landed (`cc29d5390`)
- **settings**: add entity-list-engine plan and record rollout progress (`032cca4e1`)

#### Build
- add compose shortcut targets and drop commit-msg lint hook (`35d985e2f`)
- **settings**: add EntityListConfig types for list engine (`a14b01007`)

#### Tests
- **api**: register auth API wrappers in the contract matrix (`de0cda084`)
- align route contract and manager metadata tests with backend behaviour (`3ecc78e07`)
- **settings**: align ToolCard stub with real filters-slot contract (`3a8e04a6e`)

#### Chores
- **build**: trim Makefile targets and phony list (`88284e4a8`)


### 📌 2026.6.15

_Generated on 2026-06-15._

#### Summary
- Generated from `d139a05d9` to `fe48fd3ff`.
- Included 12 commits across 3 categories: Features 9, Bug Fixes 2, Documentation 1.
- Most active scopes: mcp(4), settings(2), auth(2), gateway(1), driver(1).
- Highlights: mcp: add MCP tool call audit view page; mcp: add read API for the tool call audit log; settings: add identity management pages; mcp: service account dropdown, read-only tenant and connection info dialog; mcp: derive tool schemas from openapi and harden client registration.

#### Features
- **mcp**: add MCP tool call audit view page (`c5625d3e9`)
- **mcp**: add read API for the tool call audit log (`dbf53b022`)
- **settings**: add identity management pages (`ea4d8c7c9`)
- **mcp**: service account dropdown, read-only tenant and connection info dialog (`9bdc17b1c`)
- **mcp**: derive tool schemas from openapi and harden client registration (`0f242a053`)
- **auth**: add principal and tenant membership management APIs (`ff8d86bd5`)
- update pom version (`0feba780e`)
- **settings**: add identity/authorization change audit view page (`1b714e8ec`)
- **auth**: add identity/authorization change audit log (`195f9540b`)

#### Bug Fixes
- **gateway**: default MCP WebClient builder and route all auth paths via wildcard (`398faca2e`)
- **driver**: migrate point value publish confirm to ConfirmCallback (`fe48fd3ff`)

#### Documentation
- add scratch note on MCP/service-account API base paths (`ab7b309d5`)


### 📌 2026.6.12

_Generated on 2026-06-12._

#### Summary
- Generated from `b67438590` to `d139a05d9`.
- Included 15 commits across 4 categories: Features 5, Refactoring 7, Documentation 2, Tests 1.
- Most active scopes: mcp(8), auth(3), driver(1), design(1).
- Highlights: mcp: add settings management page; auth: align frontend with principal identity; mcp: add oauth-backed gateway runtime; auth: introduce principal identity model; add yuce docs.

#### Features
- **mcp**: add settings management page (`e63338b9e`)
- **auth**: align frontend with principal identity (`7ffbc540d`)
- **mcp**: add oauth-backed gateway runtime (`fbccee8f7`)
- **auth**: introduce principal identity model (`35dc46e95`)
- add yuce docs (`2d255c18b`)

#### Refactoring
- **mcp**: centralize gateway backend properties (`650c192ea`)
- **mcp**: route runtime calls through facade (`8ea0c0faf`)
- **driver**: centralize http driver json constants (`0f1245f00`)
- **mcp**: type auth gateway contracts (`d2a954731`)
- **mcp**: reuse frontend option constants (`1f44b80cf`)
- **mcp**: reuse shared protocol constants (`f831cc8c1`)
- **mcp**: centralize protocol constants and digest helpers (`2eabf25b8`)

#### Documentation
- **auth**: document principal and mcp architecture (`7dacc4705`)
- **design**: add Gateway MCP server design proposal (`f75003b03`)

#### Tests
- harden frontend e2e coverage (`d139a05d9`)


### 📌 2026.6.11

_Generated on 2026-06-11._

#### Summary
- Generated from `9bb22ce67` to `b67438590`.
- Included 11 commits across 5 categories: Security 1, Features 1, Bug Fixes 3, Documentation 1, Chores 5.
- Most active scopes: compose(2), auth(2), rabbitmq(1), make(1).
- Highlights: add .m2/ to gitignore, DC3_SECURITY_KEY, and minor formatting fixes; rabbitmq: expose tls configuration; support reactive wildcard permissions; auth: add resource-by-code lookup to prevent sync drift; auth: deduplicate resource_code in sync batch to prevent unique constraint violation.

#### Security
- add .m2/ to gitignore, DC3_SECURITY_KEY, and minor formatting fixes (`cd6dca688`)

#### Features
- **rabbitmq**: expose tls configuration (`ec68f0cb7`)

#### Bug Fixes
- support reactive wildcard permissions (`1811d4f8f`)
- **auth**: add resource-by-code lookup to prevent sync drift (`41e4408ac`)
- **auth**: deduplicate resource_code in sync batch to prevent unique constraint violation (`b4a2f3733`)

#### Documentation
- update documentation and README (`b67438590`)

#### Chores
- **compose**: pass rabbitmq initialization env (`56d85f7ef`)
- **make**: simplify web compose defaults (`26bc874f1`)
- **compose**: streamline environment defaults (`7a2cc9b8f`)
- complete Swagger import reordering and add OpenAPI @Schema annotations (`5809dfa83`)
- reorganize OpenAPI annotations and imports project-wide (`8fe8f3645`)


### 📌 2026.6.10

_Generated on 2026-06-10._

#### Summary
- Generated from `51a384720` to `9bb22ce67`.
- Included 20 commits across 5 categories: Security 3, Features 1, Bug Fixes 6, Refactoring 2, Documentation 8.
- Most active scopes: auth(7), security(2), common(2), api(2), openapi(1), manager(1).
- Highlights: security: audit controller authorization; security: require hmac secret in protected profiles; auth: refine OpenAPI security exposure for public endpoints; auth: enforce system-admin authorization for resource, menu, and API write operations; auth: align permission resource registration.

#### Security
- **security**: audit controller authorization (`c3b793467`)
- **security**: require hmac secret in protected profiles (`0c1a5de7d`)
- **auth**: refine OpenAPI security exposure for public endpoints (`29ac1cd35`)

#### Features
- **auth**: enforce system-admin authorization for resource, menu, and API write operations (`d6902c0b5`)

#### Bug Fixes
- **auth**: align permission resource registration (`b0740743f`)
- **auth**: enforce cross-service permissions (`fe766350b`)
- **auth**: adapt navigation to scoped menus (`531afa43c`)
- **api**: use record ids for history lookups (`9946070ea`)
- **auth**: scope menus and permission registration (`bf4a5d52f`)
- **api**: align history and session contracts (`654badf78`)

#### Refactoring
- **common**: centralize controller helper logic (`1929e1718`)
- **constant**: normalize enum codes and remarks (`9bb22ce67`)

#### Documentation
- **openapi**: normalize controller annotations (`f26d5c0b0`)
- **common**: refine shared metadata (`085e455f0`)
- **manager**: refine OpenAPI metadata (`ccaf35a7a`)
- **facade**: refine schema metadata (`0ed9cc678`)
- **data**: refine OpenAPI metadata (`42f2563dc`)
- **dal**: refine OpenAPI metadata (`5776bda63`)
- **auth**: refine OpenAPI metadata (`dc74db1e2`)
- **agentic**: refine OpenAPI metadata (`e1e45fb8e`)


### 📌 2026.6.9

_Generated on 2026-06-09._

#### Summary
- Generated from `859f52305` to `51a384720`.
- Included 9 commits across 5 categories: Security 1, Refactoring 4, Documentation 2, Build 1, Tests 1.
- Most active scopes: openapi(2), command(1), data(1), types(1), model(1).
- Highlights: openapi: wire up springdoc OpenAPI docs and fix security chain loading; command: type command history source as enum name; data: unify command history status/type/source to enum index; types: align enum-backed fields with backend changes; model: standardize domain enums and object boundaries.

#### Security
- **openapi**: wire up springdoc OpenAPI docs and fix security chain loading (`51a384720`)

#### Refactoring
- **command**: type command history source as enum name (`b61cc0f5a`)
- **data**: unify command history status/type/source to enum index (`b3ee76f89`)
- **types**: align enum-backed fields with backend changes (`0d55b4d86`)
- **model**: standardize domain enums and object boundaries (`72515d9f3`)

#### Documentation
- restructure documentation and archive engineering notes (`69e5fbe0a`)
- **openapi**: add API documentation guide, export tooling, and conventions (`8a2a87f96`)

#### Build
- align docs build scripts and CI pnpm version (`00433f67a`)

#### Tests
- align tests with enum rename and fix pre-existing failures (`1b00e7434`)


### 📌 2026.6.8

_Generated on 2026-06-08._

#### Summary
- Generated from `d3ae818e2` to `859f52305`.
- Included 12 commits across 4 categories: Security 1, Features 6, Bug Fixes 2, Chores 3.
- Most active scopes: doc(4), auth(2), build(1), pom(1), driver(1), web(1).
- Highlights: auth: replace homegrown WebFilter with Spring Security Reactive; driver: add DriverValidator SPI for protocol-level sandbox validation; doc: add @Schema annotations to dc3-common-dal entity classes; doc: add @Tag, @Operation, and @Parameter to all REST controllers; doc: add @Schema annotations to all DTO classes.

#### Security
- **auth**: replace homegrown WebFilter with Spring Security Reactive (`859f52305`)

#### Features
- **driver**: add DriverValidator SPI for protocol-level sandbox validation (`be88efc37`)
- **doc**: add @Schema annotations to dc3-common-dal entity classes (`e232f761c`)
- **doc**: add @Tag, @Operation, and @Parameter to all REST controllers (`39ab5630b`)
- **doc**: add @Schema annotations to all DTO classes (`7d42809b1`)
- **doc**: add springdoc-openapi infrastructure (`a07d3f29e`)
- **auth**: add @PreAuthorize to all REST controllers (54 controllers, 303 endpoints) (`bd6d236b5`)

#### Bug Fixes
- resolve pre-existing test failures and route naming violations (`778e9add6`)
- **build**: default MVN_SETTINGS to .mvn/settings.xml and repair test compilation (`b256be709`)

#### Chores
- **pom**: bump dependency versions (`6884022dc`)
- **web**: replace wildcard import with explicit imports in ServiceMcpToolsController (`9ca6a0cd3`)
- **deps**: upgrade Spring AI to 2.0.0-M8 and migrate tool API (`f58e8dd52`)


### 📌 2026.6.6

_Generated on 2026-06-06._

#### Summary
- Generated from `f5743a89f` to `d3ae818e2`.
- Included 20 commits across 5 categories: Security 2, Features 1, Bug Fixes 8, Refactoring 8, Documentation 1.
- Most active scopes: utils(2), types(2), views(2), pom(1), enum(1), api(1).
- Highlights: update tests for security, naming, and validation fixes; security: prevent auth data loss and permission bypass; update pom version; pom: clean up dependency management and plugin configuration; utils: correct utility class design violations.

#### Security
- update tests for security, naming, and validation fixes (`3a4eaf4b8`)
- **security**: prevent auth data loss and permission bypass (`2dfd083f6`)

#### Features
- update pom version (`5730f7c13`)

#### Bug Fixes
- **pom**: clean up dependency management and plugin configuration (`b5ef7a850`)
- **utils**: correct utility class design violations (`9d1cd10c4`)
- **enum**: change ResponseEnum index type from Byte to Integer (`405a4104c`)
- add missing final to static fields, prevent runtime mutation (`23b90a9c0`)
- **views**: dead code removal, loading states, and error handling (`3acfb6d19`)
- **utils**: modernize validation utils and fix entity name race condition (`21ef19824`)
- **agentic**: null safety in session title sync and add store reset (`9f667ef47`)
- **auth**: login/logout error handling and store cleanup (`9e45d05eb`)

#### Refactoring
- **types**: flatten AgenticModelConfig inheritance (`4e71e9c3f`)
- **views**: update callers for renamed API functions and types (`94fe6fd97`)
- **types**: enforce Form/Record extends pattern and fix naming (`30561dc7a`)
- **api**: rename get→list for collection endpoints and fix naming (`2f24fe48b`)
- fix variable naming inconsistencies (`639687a44`)
- rename proto RPCs and facade methods to follow conventions (`a7b19611a`)
- correct CRUD and boolean method naming violations (`87f2263e7`)
- rename classes and enums to follow naming conventions (`d3ae818e2`)

#### Documentation
- update project documentation (`44b2b3a76`)


### 📌 2026.6.5

_Generated on 2026-06-05._

#### Summary
- Generated from `26f6e91a6` to `f5743a89f`.
- Included 24 commits across 7 categories: Security 4, Features 3, Bug Fixes 6, Performance 1, Refactoring 6, Documentation 3, Tests 1.
- Most active scopes: driver(4), auth(3), data(2), manager(2), router(1), pages(1).
- Highlights: login: remove hardcoded default password from login form; auth: enforce tenant isolation in auth management controllers; auth: implement endpoint-level RBAC permission enforcement; auth: remove hardcoded default keys, secrets, and password fallbacks; data: add VO and Builder classes for history entities.

#### Security
- **login**: remove hardcoded default password from login form (`31507e552`)
- **auth**: enforce tenant isolation in auth management controllers (`293845a52`)
- **auth**: implement endpoint-level RBAC permission enforcement (`aaf90f203`)
- **auth**: remove hardcoded default keys, secrets, and password fallbacks (`f5743a89f`)

#### Features
- **data**: add VO and Builder classes for history entities (`570d13e2b`)
- **router**: add client-side permission guard based on menu tree (`ed83ce097`)
- **pages**: add 30-second auto-refresh to alarm, event and command pages (`d56e7a60e`)

#### Bug Fixes
- **matrix**: add discard confirmation and unsaved changes guard (`93f8c8cb1`)
- **axios**: add global error handling and network error notifications (`1662332e1`)
- **rabbitmq**: enable persistent message delivery and dead letter queues (`176407c7f`)
- **driver**: prevent write-echo double-scaling and add connection resilience (`7654401f3`)
- **manager**: correct operateTime set target on entity DOs (`be94deb7e`)
- **manager**: close cross-tenant data leaks in list-by-relation queries (`e9d9dce2b`)

#### Performance
- **data**: implement batch latest point value query to eliminate N+1 (`97b792dae`)

#### Refactoring
- fix low-severity code convention violations (`038d0c092`)
- fix medium-severity code convention violations (`099d37c9b`)
- add rollbackFor to @Transactional and configure TaskScheduler (`f095948a7`)
- **driver**: replace log string concatenation with parameterized placeholders (`80c45456b`)
- fix high-severity code convention violations (`ebb04bb5c`)
- **driver**: clean up unused imports and dead code in protocol drivers (`cdea07e3d`)

#### Documentation
- add project guidance files for AI coding tools (`42e28ca9b`)
- **readme**: add product screenshots and update Chinese README (`7f73b06cf`)
- update project documentation and guidance files (`55605d66a`)

#### Tests
- **driver**: add test helpers and update driver configuration formatting (`233e279db`)


### 📌 2026.6.2

_Generated on 2026-06-02._

#### Summary
- Generated from `e4902522b` to `26f6e91a6`.
- Included 7 commits across 5 categories: Features 2, Bug Fixes 1, Refactoring 2, Tests 1, Chores 1.
- Most active scopes: dialog(1), driver(1).
- Highlights: polish device-edit matrix tables with pagination and unified styling; driver: add 17 protocol drivers to extend IoT connectivity coverage; repair settings sidebar fallback and route references after menu rename; rename navigation keys to match backend menu code changes; realign navigation with Alarm/Event/Command menu restructure.

#### Features
- polish device-edit matrix tables with pagination and unified styling (`81dd6a7ca`)
- **driver**: add 17 protocol drivers to extend IoT connectivity coverage (`26f6e91a6`)

#### Bug Fixes
- repair settings sidebar fallback and route references after menu rename (`f7c266e05`)

#### Refactoring
- rename navigation keys to match backend menu code changes (`7b96b37e3`)
- realign navigation with Alarm/Event/Command menu restructure (`f8754caed`)

#### Tests
- update tests for route rename and add Node 22 storage polyfill (`b2c30f6c4`)

#### Chores
- **dialog**: extract shared dialog min-height to things-dialog.scss and remove inline overrides (`e85a8f26e`)


### 📌 2026.5.28

_Generated on 2026-05-28._

#### Summary
- Generated from `918b3d5e8` to `e4902522b`.
- Included 10 commits across 3 categories: Features 2, Bug Fixes 3, Refactoring 5.
- Most active scopes: form(1), segmented(1), composable(1), i18n(1), dialog(1), settings(1).
- Highlights: segmented: add MatrixStatusSegmented and EnableFlagSegmented includeAll; i18n: localize clipboard/notification utils and add shared common locale keys; add missing form validation rules and centralize regex patterns; dialog: add spacing between form content and footer buttons; settings: fix breadcrumb position and independent sidebar/content scroll.

#### Features
- **segmented**: add MatrixStatusSegmented and EnableFlagSegmented includeAll (`d9c14389d`)
- **i18n**: localize clipboard/notification utils and add shared common locale keys (`d55e80fbd`)

#### Bug Fixes
- add missing form validation rules and centralize regex patterns (`ebf157b43`)
- **dialog**: add spacing between form content and footer buttons (`91472a9b2`)
- **settings**: fix breadcrumb position and independent sidebar/content scroll (`4da753710`)

#### Refactoring
- **form**: centralize validators and tighten input contracts (`da5223a13`)
- **composable**: support server-paginated lists in usePagedList (`93f0727e8`)
- extract InfoCard and MatrixToolbar shared components (`14e1c88e2`)
- standardize API return types from String to Long/Integer (`058a72c1a`)
- **profile,device**: replace step wizard with tab layout in edit pages (`e4902522b`)


### 📌 2026.5.26

_Generated on 2026-05-26._

#### Summary
- Generated from `dedb03fe7` to `918b3d5e8`.
- Included 18 commits across 6 categories: Security 1, Features 5, Bug Fixes 4, Refactoring 1, Tests 4, Chores 3.
- Most active scopes: driver(3), driver-sl651(1), manager(1), data(1), views(1), component(1).
- Highlights: address code review — security, thread safety, performance, and null safety; driver-sl651: forward telemetry as point values via reflective SL651 API; data: support command/event lookup by code and tenant-scoped reads; device-edit: refresh thing model definitions from selected profile; driver: add Mitsubishi Melsec MC and SL651 hydrological telemetry drivers.

#### Security
- address code review — security, thread safety, performance, and null safety (`27bcf1383`)

#### Features
- **driver-sl651**: forward telemetry as point values via reflective SL651 API (`c87f25d0a`)
- **data**: support command/event lookup by code and tenant-scoped reads (`b6e724765`)
- **device-edit**: refresh thing model definitions from selected profile (`52522b02a`)
- **driver**: add Mitsubishi Melsec MC and SL651 hydrological telemetry drivers (`29f159407`)
- update pom version to 2026.5.22 (`918b3d5e8`)

#### Bug Fixes
- **driver**: invalidate failing connectors and tighten driver null-safety (`60cd16d8b`)
- **driver**: harden command runtime, dedup, and per-device locking (`1ecafdaeb`)
- **manager**: preserve immutable entity codes and harden duplicate checks (`5ccc6140e`)
- prevent double-submit race and add inline param name validation (`435a1a38b`)

#### Refactoring
- enforce list* prefix and consolidate alarm formatters (`026e980c6`)

#### Tests
- add comprehensive e2e specs for all routes and button functions (`76bd96cf0`)
- **views**: cover every list page and the device-edit regression spot (`759d17acc`)
- **component**: cover the twenty previously untested components (`80d5d3c0f`)
- **infra**: contract registry, naming guardrail, and missing util coverage (`292aed3ac`)

#### Chores
- format code (`1520cc655`)
- format code (`79193b256`)
- format code (`9dc43265a`)


### 📌 2026.5.25

_Generated on 2026-05-25._

#### Summary
- Generated from `f30c4a2f7` to `dedb03fe7`.
- Included 4 commits across 1 categories: Features 4.
- Highlights: format    &  style; add alarm/command/event types, i18n entries, shared styles, and thingModel format utilities; add command and event attribute matrix editors in device edit; add command/event attribute configuration system.

#### Features
- format    &  style (`d5b6ea6f7`)
- add alarm/command/event types, i18n entries, shared styles, and thingModel format utilities (`ebb229e40`)
- add command and event attribute matrix editors in device edit (`7b4f53050`)
- add command/event attribute configuration system (`dedb03fe7`)


### 📌 2026.5.24

_Generated on 2026-05-24._

#### Summary
- Generated from `cf2ffb019` to `f30c4a2f7`.
- Included 18 commits across 5 categories: Features 5, Bug Fixes 4, Refactoring 4, Documentation 2, Chores 3.
- Highlights: add point attribute matrix editor and refine card grid layout; restructure command/event management with card layout and inline detail; add layered architecture diagram and reposition project narrative; extend ModelConfig and ModelProvider BO/DO/VO with additional fields; add command and event management views.

#### Features
- add point attribute matrix editor and refine card grid layout (`1fa8e6f80`)
- restructure command/event management with card layout and inline detail (`b338c2c27`)
- add layered architecture diagram and reposition project narrative (`fd3af6463`)
- extend ModelConfig and ModelProvider BO/DO/VO with additional fields (`0db7bcd6c`)
- add command and event management views (`f30c4a2f7`)

#### Bug Fixes
- update Login view layout adjustments (`553bd42ff`)
- add missing tenantId ignore in MapStruct mapper builders (`348267b14`)
- resolve test compilation issues (`4b9e3ad49`)
- update Login view layout adjustments (`2ab419dea`)

#### Refactoring
- normalize import order and template attribute formatting (`ee4f2459f`)
- extract domain magic strings to constants (`95a0f363e`)
- extract driver protocol strings to config and constants (`0defdb5b6`)
- align data history models and constants (`3b378e854`)

#### Documentation
- fix stale doc paths and version info in AI agent instructions (`c40c3844b`)
- remove AI commit identity section from AGENTS.md (`8df45eac0`)

#### Chores
- format MyBatis XML headers, SQL indentation and docs tables (`b5ab016ca`)
- format DTO builders, E2E teardown and Javadoc alignment (`a88cb03aa`)
- remove unused imports (`4aed1dcee`)


### 📌 2026.5.23

_Generated on 2026-05-23._

#### Summary
- Generated from `0583fbee0` to `cf2ffb019`.
- Included 8 commits across 3 categories: Features 4, Refactoring 1, Documentation 3.
- Highlights: add event alarm trigger, E2E contract tests, and docs; add driver SDK custom command execution and event reporting; add custom command call API, event report API with RabbitMQ and gRPC; add Command and Event model with full CRUD stack and facade layer; convert Device↔Profile from M:N to 1:1 via device.profile_id.

#### Features
- add event alarm trigger, E2E contract tests, and docs (`f0d0c7913`)
- add driver SDK custom command execution and event reporting (`5b24bc775`)
- add custom command call API, event report API with RabbitMQ and gRPC (`150ba3324`)
- add Command and Event model with full CRUD stack and facade layer (`aa5c4ddf8`)

#### Refactoring
- convert Device↔Profile from M:N to 1:1 via device.profile_id (`dd4f575ba`)

#### Documentation
- mark Phase 1 device-profile single-ownership tasks as done (`e8b2eb908`)
- mark Phase 1 (Device↔Profile 1:1) as done (`84a33daef`)
- update thing-model design with Phase 1 implementation plan (`cf2ffb019`)


### 📌 2026.5.22

_Generated on 2026-05-22._

#### Summary
- Generated from `850da6186` to `0583fbee0`.
- Included 14 commits across 5 categories: Features 7, Bug Fixes 1, Refactoring 3, Documentation 1, Chores 2.
- Most active scopes: data(4), command(2), api(1).
- Highlights: complete point command P0-P2 rebuild — DTO, lock, validation, API; unify DeviceStatusEnum and DriverStatusEnum into EntityStatusEnum; refine device and driver health state handling; add device health timeout state handling; command: add DLX, result receipt, dedup cache, and validation.

#### Features
- complete point command P0-P2 rebuild — DTO, lock, validation, API (`c556c6a0b`)
- unify DeviceStatusEnum and DriverStatusEnum into EntityStatusEnum (`47760888b`)
- refine device and driver health state handling (`436b3b01b`)
- add device health timeout state handling (`0471bd904`)
- **command**: add DLX, result receipt, dedup cache, and validation (`6b4cefc74`)
- **data**: add point command persistence model and status enums (`10b09d265`)
- **data**: implement RabbitMQ TTL+DLX timeout for driver and device state (`877214929`)

#### Bug Fixes
- **data**: harden entity state design and add test coverage (`0583fbee0`)

#### Refactoring
- **api**: update point command API paths to /point_command (`665885444`)
- **command**: rename PointValueCommand/DeviceCommand to PointCommand (`2617565e7`)
- **data**: align entity state table with lease-based timeout design doc (`439330f3f`)

#### Documentation
- update point-command.md status to reflect P0-P3 implementation (`c6b70c166`)

#### Chores
- clean up RabbitConstant, fix imports and update docs formatting (`2e75061c7`)
- apply consistent code formatting across all modules and docs (`9007cbc22`)


### 📌 2026.5.21

_Generated on 2026-05-21._

#### Summary
- Generated from `4367f0360` to `850da6186`.
- Included 51 commits across 4 categories: Features 17, Bug Fixes 4, Refactoring 13, Documentation 17.
- Most active scopes: data(14), alarm(5), dal(3), auth(3), manager(3), driver(2).
- Highlights: data: add persistent state lease table for driver/device status; alarm: batch rule pipeline with processBatch dispatch; rule alarm optimization; alarm: hybrid window aggregator and evaluator; repository: time-windowed point-value aggregation.

#### Features
- **data**: add persistent state lease table for driver/device status (`04d0d5342`)
- **alarm**: batch rule pipeline with processBatch dispatch (`e85dd9ba7`)
- rule alarm optimization (`a0b392c3a`)
- **alarm**: hybrid window aggregator and evaluator (`c7d927bec`)
- **repository**: time-windowed point-value aggregation (`3b01a16ad`)
- **alarm**: in-memory window sample buffer (`c5fcc0b4f`)
- **alarm**: parse ISO-8601 window duration and accept all modes at save (`cd94180d2`)
- **data**: cache notify policy and message templates (`fa6243f01`)
- **data**: batch alarm rule trigger entrypoint (`308df030c`)
- **data**: cache active rules in RuleRegistry (`8404ad362`)
- **data**: async notify dispatch via RabbitMQ worker (`2ea09124f`)
- **data**: introduce notify task pending state in history writes (`f2f14558b`)
- **data**: persist alarm_level_flag and unify severity source (`c39fa95fd`)
- **dal**: allow filtering groups by position in list query (`524a9cb1d`)
- **driver,manager**: broadcast driver metadata events and let drivers refresh on demand (`e38c7d432`)
- add point alarm source to event dashboard and list views (`c2e3baab7`)
- merge dc3_device_event and dc3_driver_event into dc3_entity_alarm (`850da6186`)

#### Bug Fixes
- **data**: reject non-LAST window modes at evaluation and save time (`e2c86f8d7`)
- **data**: require firing state before evaluating recovery (`9531861c4`)
- **data**: backfill tenantId on device/driver alarm path (`69afed88a`)
- **driver**: harden metadata cache against lost updates and orphan ids (`2a6619b9a`)

#### Refactoring
- replace manual constructors with @RequiredArgsConstructor (`db14585cb`)
- **alarm**: rename eventId to alarmId; surface alarm level (`0fc4e4386`)
- **data**: rename eventId to alarmId across rule/notify domain (`80aac4b4b`)
- **constant,data,driver**: consolidate scattered ad-hoc constants (`9fd003403`)
- **drivers,tests**: switch to constructor injection via @RequiredArgsConstructor (`91eef8cbc`)
- **agentic,web,gateway**: switch to constructor injection via @RequiredArgsConstructor (`15ecdadea`)
- **auth**: switch to constructor injection via @RequiredArgsConstructor (`092c63e84`)
- **manager**: switch to constructor injection via @RequiredArgsConstructor (`44d6d845a`)
- **data**: switch to constructor injection via @RequiredArgsConstructor (`6f6a15e52`)
- **facade**: switch to constructor injection via @RequiredArgsConstructor (`d27572f42`)
- **driver**: switch to constructor injection via @RequiredArgsConstructor (`8f8e2b1c9`)
- **mqtt**: switch to constructor injection via @RequiredArgsConstructor (`5b5deb63c`)
- **dal**: switch to constructor injection via @RequiredArgsConstructor (`62d8a2d40`)

#### Documentation
- **gateway,api,quartz,repository**: improve class-level Javadoc (`60cebd179`)
- **exception**: add missing class-level Javadoc (`604786b49`)
- **dal**: improve class-level Javadoc descriptions (`945f731a2`)
- **model,constant**: improve class-level Javadoc descriptions (`b4ef02688`)
- **agentic**: improve class-level Javadoc descriptions (`1c2252fcd`)
- **data**: fix stale and improve DO class Javadoc descriptions (`94ed95314`)
- **auth**: improve insufficient class-level Javadoc descriptions (`ff6a817c8`)
- **manager**: improve insufficient class-level Javadoc descriptions (`71fb3a87b`)
- **todo**: update javadoc-cleanup plan with completion status (`cc75f8ad0`)
- **model**: fix remaining empty Javadoc on Ext classes (`cc4843585`)
- **driver,web,rabbitmq,repository,dal**: fix class-level Javadoc (`760269d3d`)
- **model,constant**: fix class-level Javadoc (`3e7b779f2`)
- **agentic**: add missing class-level Javadoc (`0079106c6`)
- **data**: fix class-level Javadoc across the data module (`a1cd8ad3a`)
- **manager**: fix class-level Javadoc across the manager module (`576d86dd8`)
- **auth**: fix class-level Javadoc across the auth module (`7fd38e0c2`)
- **data,manager**: fix stale and broken Javadoc references (`3b134a085`)


### 📌 2026.5.20

_Generated on 2026-05-20._

#### Summary
- Generated from `14faabedc` to `4367f0360`.
- Included 3 commits across 1 categories: Refactoring 3.
- Highlights: rename alarm record view to history; rename notify record to history; remove Redis dependency, update docs and workspace config.

#### Refactoring
- rename alarm record view to history (`54285f9b6`)
- rename notify record to history (`c69b453af`)
- remove Redis dependency, update docs and workspace config (`4367f0360`)


### 📌 2026.5.19

_Generated on 2026-05-19._

#### Summary
- Generated from `6ba82c711` to `14faabedc`.
- Included 16 commits across 6 categories: Security 2, Features 2, Bug Fixes 4, Refactoring 3, Tests 2, Chores 3.
- Most active scopes: auth(3), point-value(1), runtime(1), data(1), agentic(1).
- Highlights: auth: replace MD5 password hashing with bcrypt; auth: enable HMAC gateway-to-service signing by default; agentic: replace thinking pulse dot with animated icon and shimmer text; update package versions; auth: send MD5 password directly instead of double-hashing with salt.

#### Security
- **auth**: replace MD5 password hashing with bcrypt (`e88280e3f`)
- **auth**: enable HMAC gateway-to-service signing by default (`cb423056c`)

#### Features
- **agentic**: replace thinking pulse dot with animated icon and shimmer text (`01edf3252`)
- update package versions (`cb6f0c3b0`)

#### Bug Fixes
- **auth**: send MD5 password directly instead of double-hashing with salt (`860672e54`)
- **point-value**: show missing latest values (`20996e857`)
- **data**: expose missing latest point values (`0cf3627dd`)
- align settings ui hierarchy and spacing (`5f293ea6e`)

#### Refactoring
- eliminate `any` types in utils and settings, remove unused deps and duplicate scripts (`f389db5d2`)
- **runtime**: encapsulate mutable static state (`89282a71c`)
- improve settings page consistency (`0d25294fb`)

#### Tests
- harden test infrastructure to A-grade with fixtures, stubs, guardrails, and e2e auth coverage (`365cf71c0`)
- improve backend test coverage governance (`5a3ab1440`)

#### Chores
- add dc3 stack test skill (`b957702c8`)
- update .gitignore for pnpm lock file (`dce69688c`)
- apply consistent formatting across GitHub workflows, docs and common modules (`14faabedc`)


### 📌 2026.5.18

_Generated on 2026-05-18._

#### Summary
- Generated from `7847bffbd` to `6ba82c711`.
- Included 42 commits across 6 categories: Features 3, Bug Fixes 7, Refactoring 21, Documentation 6, Tests 3, Chores 2.
- Most active scopes: common(9), api(5), site(3), manager(2), test(2), component(1).
- Highlights: update maven config; update package version; add detail pages for group, label, alarm, and agentic settings; common: final cleanup of residual select* identifiers; common: clean up residual select* names missed by main rename.

#### Features
- update maven config (`d0e42e2a3`)
- update package version (`0b5b3689a`)
- add detail pages for group, label, alarm, and agentic settings (`b31f54024`)

#### Bug Fixes
- **common**: final cleanup of residual select* identifiers (`3dd628492`)
- **common**: clean up residual select* names missed by main rename (`a3323cfc6`)
- **api**: point getDeviceCountByDriverId at /get_count_by_driver_id (`18d106577`)
- **manager**: rename DeviceController.listByDriverId to getCountByDriverId (`92a7685ae`)
- **test**: catch up tests with BaseService rename (`fdea6dac1`)
- trigger alarm rules and structure runtime payloads (`543f0e302`)
- prevent flex overflow in card and settings layouts (`2040db769`)

#### Refactoring
- **api**: rename API functions to match get/list cardinality convention (`aac73ee77`)
- **api**: rename HTTP paths to match backend get/list unification (`dc283bd29`)
- rename Controller methods and HTTP paths to match Service rename (`1d543a49d`)
- **data**: rename DeviceStatusService and DriverStatusService selectByPage to getStatusByPage (`33e96b0ca`)
- **common**: rename Service multi-record selectBy* to listBy* and selectTree to listTree (`7eff99a40`)
- **common**: rename Service single-record selectBy* to getBy* (`3394475f2`)
- **common**: rename BaseService.selectById to getById (`ab607cb4c`)
- **common**: rename Facade list/get methods (api+grpc+local+callers) (`be127cef8`)
- **common**: align gRPC server impls and stub callers with renamed RPCs (`d66946818`)
- **api**: rename gRPC SelectBy* RPCs to GetBy*/ListBy* (`476799e51`)
- **home**: sync DailyGrowthSummary to backend GrowthVO rename (`a9d92549d`)
- **manager**: clarify GrowthVO sparkline field names (`3972c7f97`)
- **common**: normalize enum naming and document suffix convention (`1919ac68b`)
- replace ambiguous boolean method outcomes with clear semantics (`548159a6d`)
- rename static final fields to UPPER_SNAKE_CASE (`3fa286f42`)
- **test**: update mocks for renamed BaseService verbs (`4a2904b52`)
- align callers with renamed BaseService verbs (`f5dfbbe45`)
- **common**: unify BaseService CRUD verbs to add/delete/list (`87705049a`)
- **i18n**: normalize alarm and model navigation labels (`957ea8c88`)
- **settings**: streamline list views and alarm detail routing (`18c7124ce`)
- replace el-switch enable flags with EnableFlagSegmented (`6ba82c711`)

#### Documentation
- **site**: import legacy operation manual from dc3-docs (`48f5d2f0e`)
- **claude**: document API get/list verb convention in CLAUDE.md (`722551e55`)
- **agents**: document CRUD verb convention for list/get unification (`79a67de2d`)
- **site**: switch documentation framework from MkDocs Material to VitePress (`9e9c1dc2e`)
- **release**: align TITLE.md with main README description (`317b7ac7b`)
- **site**: scaffold MkDocs Material documentation site with GitHub Pages CI (`bca6d7aed`)

#### Tests
- **component**: align alarm-notify mocks with renamed alarm api functions (`3f9b61e93`)
- **api**: regenerate api-contracts snapshots after path rename (`11588a80e`)
- **e2e**: extend coverage to alarm, agentic, and detail routes (`6321f0ebd`)

#### Chores
- **gitignore**: expand ignore patterns for build, IDE, OS and secrets (`2fed75bcf`)
- bump version to 2026.5.18 across all modules (`7f5da1a0f`)


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

### 📌 2026.5.16

_Generated on 2026-05-16._

#### Summary
- Generated from `74e16bfb3` to `7847bffbd`.
- Included 42 commits across 8 categories: Features 9, Bug Fixes 5, Refactoring 14, Documentation 2, Build 7, CI 1, Tests 1, Chores 3.
- Most active scopes: agentic(19), data(3), driver(1), model(1), constant(1), docker(1).
- Highlights: agentic: trace structured tool results; do someting test and code review; driver: add CoAP driver module scaffold; data: add alarm rule engine and notification pipeline; data: add alarm CRUD entities, DAL, services and controllers.

#### Features
- **agentic**: trace structured tool results (`4094d1b5a`)
- do someting test and code review (`019daf62c`)
- **driver**: add CoAP driver module scaffold (`ba187405b`)
- **data**: add alarm rule engine and notification pipeline (`2b3e15a8c`)
- **data**: add alarm CRUD entities, DAL, services and controllers (`2d1e86525`)
- **model**: add alarm notification and rule state extensions (`55b424509`)
- **constant**: add alarm enums and constants (`a5675080b`)
- **docker**: add web frontend and consolidate compose stacks (`b9b2a4773`)
- **agentic**: add structured visualization support (`1177a027b`)

#### Bug Fixes
- **agentic**: align tool calling runtime behavior (`99d86138b`)
- **agentic**: persist structured runtime failures (`73b651e03`)
- **agentic**: describe point writes as pending actions (`eb2ac61e5`)
- improve dashboard and agentic backend APIs (`e5a908870`)
- **agentic**: preserve reasoning through tool loop (`7847bffbd`)

#### Refactoring
- **agentic**: centralize runtime conventions (`65cb86fa5`)
- **agentic**: make tool loop explicit (`a01948093`)
- **agentic**: encapsulate run trace state (`bac7e8e43`)
- **agentic**: use explicit tool context (`d97de0c06`)
- **agentic**: centralize tool invocation tracing (`9e6fa0ec2`)
- **agentic**: isolate runtime response mapping (`5dd29e295`)
- **agentic**: centralize run event construction (`149246d03`)
- **agentic**: unify runtime event model (`11d57cb37`)
- **agentic**: introduce runtime boundary (`7bf1d6d8c`)
- **agentic**: enforce backend tool safety boundaries (`dd18de649`)
- **agentic**: remove direct query chat path (`6fd791979`)
- **agentic**: simplify assistant tool orchestration (`e2c2da1b1`)
- **agentic**: split chat orchestration and structure tool results (`20bb57df1`)
- **data**: redesign alarm rule/notification/message domain models (`a4e24a9c7`)

#### Documentation
- update documentation for compose and Makefile changes (`80ec2beab`)
- update READMEs and project documentation (`994ccba99`)

#### Build
- refactor Dockerfile, Makefile and compose files (`83ee84773`)
- refine Dockerfile comments and dockerignore scope (`a9c190879`)
- add container Maven settings without mirror (`946f5b7ef`)
- update root pom and banner version to 2026.5.17 (`737caa178`)
- update Makefile and Maven settings (`addbeea20`)
- consolidate per-service Dockerfiles into unified multi-target build (`105c63603`)
- bump version to 2026.5.17 (`996518186`)

#### CI
- move Maven build into Docker BuildKit builder stage (`1dd659999`)

#### Tests
- add alarm component unit tests (`33cfe9c7a`)

#### Chores
- fix import ordering, javavadoc alignment and ctor placement (`b9b459862`)
- correct @since tag to project inception date (`7c853ac8b`)
- format dc3-common and dc3 module code (`4f5aab074`)


### 📌 2026.5.15

_Generated on 2026-05-15._

#### Summary
- Generated from `aed2db0ff` to `74e16bfb3`.
- Included 28 commits across 6 categories: Features 3, Bug Fixes 7, Refactoring 11, Build 1, Tests 4, Chores 2.
- Most active scopes: driver(14), common(4), test(2), agentic(2), public(1), rabbitmq(1).
- Highlights: agentic: persist session config; driver: allow opting out of automatic driver profile activation; driver: move numValue projection from data service to driver side; test: align data module tests with refactored production code; driver: preserve protocol exception causes in modbus/opc drivers.

#### Features
- **agentic**: persist session config (`9ca4d8a5f`)
- **driver**: allow opting out of automatic driver profile activation (`77d301a67`)
- **driver**: move numValue projection from data service to driver side (`712506b32`)

#### Bug Fixes
- **test**: align data module tests with refactored production code (`175c4d055`)
- **driver**: preserve protocol exception causes in modbus/opc drivers (`a0e774cca`)
- **driver**: bound device command retries to a single redelivery (`63af37fde`)
- **driver**: preserve write exceptions and echo a PointValue on success (`8b76402d4`)
- **driver**: align metadata cache policy with event-driven invalidation (`41c336af0`)
- **driver**: close four production-grade bugs in the driver SDK (`6b7051156`)
- **test**: add missing assertj import in agentic controller tests (`292eec1b2`)

#### Refactoring
- **common**: improve postgres and repository modules (`9437934ca`)
- **public**: improve shared auto configuration (`c7db32f21`)
- **common**: standardize constants and quartz config (`8268dcea8`)
- **rabbitmq**: tighten config, add high-throughput factory, opt-out profile (`9bda79c0e`)
- **common**: preserve exception causes (`db683533b`)
- **common-mqtt**: harden MQTT auto configuration (`9301680b9`)
- **driver**: split DriverCustomService into capability-scoped SPIs (`eecb28897`)
- **driver**: relocate SDK classes under common.driver subtree (`e6b175bad`)
- **driver**: drop unused driver-command receiver and CONFIG branch (`1d8df644e`)
- **common**: consolidate EnvironmentPostProcessor registration (`dc8f3de06`)
- **driver**: rename RValue/WValue and add CalculatedPointValue (`cb457c37c`)

#### Build
- **agentic**: upgrade Spring AI from 2.0.0-M5 to 2.0.0-M6 (`67f1f7c2c`)

#### Tests
- **driver**: align scheduler-init test with current rethrow behaviour (`b3758149f`)
- **driver**: align read/write service tests with renamed value types (`cf48d10b2`)
- **data**: add PostgresRepositoryServiceImpl numValue pass-through tests (`f60ff6c9e`)
- **driver**: add RValue and PointValue numValue projection tests (`5dd2191ab`)

#### Chores
- fix minor formatting in dc3 module (`56cfc2231`)
- format dc3-common, dc3-driver and dc3-e2e test code (`74e16bfb3`)


### 📌 2026.5.14

_Generated on 2026-05-14._

#### Summary
- Generated from `aa0da3572` to `aed2db0ff`.
- Included 13 commits across 5 categories: Features 2, Bug Fixes 1, Refactoring 1, Tests 8, Chores 1.
- Most active scopes: agentic(6), e2e(3), data(2), facade(1).
- Highlights: agentic: expand platform tool capabilities; agentic: replay chat memory from dc3_message and require client conversationId; update auth controller login flow, fix test compilation and assertions; data: collapse history into a single point-value table.

#### Features
- **agentic**: expand platform tool capabilities (`ca0393ae7`)
- **agentic**: replay chat memory from dc3_message and require client conversationId (`ddda8c08f`)

#### Bug Fixes
- update auth controller login flow, fix test compilation and assertions (`aed2db0ff`)

#### Refactoring
- **data**: collapse history into a single point-value table (`d40dd656e`)

#### Tests
- **agentic**: wire memory advisor in chat client factory test (`309e3476b`)
- **e2e**: stabilize infrastructure harness (`9123b480f`)
- **e2e**: cover infrastructure contracts with rest-assured and direct clients (`69090d131`)
- **e2e**: bootstrap dockerized end-to-end harness (`c49dd630e`)
- **facade**: cover local facade implementations across auth, manager and data (`9161243a5`)
- **agentic**: cover controllers, properties and chat client factory eviction (`e61e31178`)
- **agentic**: cover session, message, model and action service implementations (`20187511d`)
- **agentic**: cover request context, conversation id, token estimator and skill module (`c22af65ee`)

#### Chores
- **data**: rename numericValue to numValue to match raw/cal prefix style (`48d7839d0`)


### 📌 2026.5.13

_Generated on 2026-05-13._

#### Summary
- Generated from `8f33f5394` to `aa0da3572`.
- Included 39 commits across 7 categories: Security 1, Bug Fixes 1, Refactoring 2, Documentation 1, CI 1, Tests 32, Chores 1.
- Most active scopes: driver(8), infra(5), data(5), manager(5), auth(3), public(3).
- Highlights: auth: cover security-critical service impls with mockito; test-infra: propagate test bom into dc3-common dependency management; standardize API routes and uploads; simplify PointController, extract ChatClientFactory and remove deprecated service methods.

#### Security
- **auth**: cover security-critical service impls with mockito (`afaa899c1`)

#### Bug Fixes
- **test-infra**: propagate test bom into dc3-common dependency management (`ade857936`)

#### Refactoring
- standardize API routes and uploads (`1e415cd44`)
- simplify PointController, extract ChatClientFactory and remove deprecated service methods (`aa0da3572`)

#### Documentation
- **test**: add testing guide and update validation checklist (`10e083602`)

#### CI
- add unit, integration and e2e workflows with makefile targets (`efd26c645`)

#### Tests
- **driver**: cover plcs7 driver custom service and point variable mapping (`9ce05b838`)
- **driver**: cover opc-ua and opc-da driver custom services (`108cb3957`)
- **driver**: cover mqtt driver custom service and receive pipeline (`b0eeb86ce`)
- **driver**: cover modbus tcp driver custom service with mocked master (`f5fc53cd8`)
- **driver**: cover virtual and listening-virtual driver custom services (`22bd52681`)
- **infra**: cover resource registrar lifecycle and api endpoint scanner (`1391c9cbb`)
- **infra**: cover quartz, mqtt and thread pool wiring (`2f1253cde`)
- **rabbitmq**: cover ack util and rabbit config wiring (`5aade4808`)
- **driver**: cover rabbit receivers and metadata event publisher (`9c3b87e4d`)
- **driver**: cover driver sdk service impls (`bbab1dc79`)
- **data**: cover point value grpc server and rate-throttling job (`1a3b6ec09`)
- **data**: cover rabbit receivers with manual ack/nack contracts (`1593b8dca`)
- **data**: cover local cache service and point value cache facade (`fc64b9611`)
- **data**: cover driver/device status and event biz services (`f13654a62`)
- **data**: cover point value, command and schedule biz services (`b6afeb020`)
- **manager**: cover metadata event listener and hourly job (`400bfc38a`)
- **manager**: cover driver controller and grpc envelope shaping (`e659f584e`)
- **manager**: cover dictionary and driver-register biz services (`bef940fac`)
- **manager**: cover attribute and profile-bind service impls (`7b419bd53`)
- **manager**: cover driver, device, point and profile service impls (`5fc6716ed`)
- **gateway**: cover authentic filter and global exception handler (`956be2793`)
- **auth**: cover token controller and grpc envelope shaping (`ef836178d`)
- **auth**: cover biz layer with focus on token edge paths (`fb99b84aa`)
- **common**: cover indexed domain enums via shared contract (`10ff9da9b`)
- **driver**: cover binary codec helpers used by protocol drivers (`425dbd768`)
- add common exception coverage (`d82bea91a`)
- **public**: cover optional wrappers and response entities (`2480594aa`)
- **public**: cover time, regex, page, tree and lambda utilities (`8768971ee`)
- **public**: cover arithmetic, codec, decode, json and key utilities (`1f30422a0`)
- **infra**: add coverage and end-to-end aggregator modules (`c47e5f0a3`)
- **infra**: wire test bom and plugins in root pom (`a3e95968b`)
- **infra**: introduce dc3-common-test module with shared fixtures (`36ffe2bb6`)

#### Chores
- **test**: remove playwright leftover and harden gitignore (`c19720090`)


### 📌 2026.5.12

_Generated on 2026-05-12._

#### Summary
- Generated from `66a364df3` to `8f33f5394`.
- Included 4 commits across 3 categories: Bug Fixes 2, Refactoring 1, Chores 1.
- Most active scopes: backend(1), agentic(1), builder(1).
- Highlights: backend: support group and label settings; builder: suppress MapStruct unmapped target property warnings; agentic: use enum types in requests and fix default model resolution.

#### Bug Fixes
- **backend**: support group and label settings (`37294b177`)
- **builder**: suppress MapStruct unmapped target property warnings (`8f33f5394`)

#### Refactoring
- **agentic**: use enum types in requests and fix default model resolution (`6fcc11da1`)

#### Chores
- reorder imports and fix doc table alignment (`d99e6cb2c`)


### 📌 2026.5.11

_Generated on 2026-05-11._

#### Summary
- Generated from `ca45788d5` to `66a364df3`.
- Included 15 commits across 5 categories: Security 1, Features 4, Refactoring 5, Documentation 3, Build 2.
- Most active scopes: agentic(7), manager(2), domain(2), auth(1), agent(1), deploy(1).
- Highlights: auth: exclude credentials from toString; manager: filter entities by group and label; manager: add group and label binding APIs; agentic: configure fallback provider and profile activation; agentic: add ModelProvider module with CRUD operations.

#### Security
- **auth**: exclude credentials from toString (`8f0f171c3`)

#### Features
- **manager**: filter entities by group and label (`f859750c8`)
- **manager**: add group and label binding APIs (`97c7094a7`)
- **agentic**: configure fallback provider and profile activation (`285ed9273`)
- **agentic**: add ModelProvider module with CRUD operations (`66a364df3`)

#### Refactoring
- **domain**: align group and label taxonomy enums (`ebf8bbefe`)
- **domain**: use enums for business flags (`0d8ac8d08`)
- **agentic**: standardize domain mapping (`2d60ebe13`)
- **agentic**: bind ModelConfig to provider via providerId foreign key (`53f6272e8`)
- **agentic**: replace ChatClient singleton with per-provider factory (`0f3a08d90`)

#### Documentation
- **agent**: document domain modeling conventions (`88a114f8a`)
- **agentic**: document provider configuration (`989443e99`)
- update AGENTS.md with project architecture and workflow instructions (`6d97de316`)

#### Build
- **deploy**: add agentic memory and tool-calling env variables to docker-compose (`d65fc715f`)
- **agentic**: add spring-ai-starter-model-anthropic dependency (`292784066`)


### 📌 2026.5.10

_Generated on 2026-05-10._

#### Summary
- Generated from `868bf6c2b` to `ca45788d5`.
- Included 8 commits across 4 categories: Features 2, Bug Fixes 3, Refactoring 2, Chores 1.
- Most active scopes: auth(2), config(1), agentic(1), manager(1), driver(1), web(1).
- Highlights: agentic: add message, attachment, action, model config and skill submodules; auth: auto-manage tenant binding on user create and delete; manager: guard profile bind removal and initialize empty device list; driver: tolerate incomplete attribute config and relax name pattern; auth: use @NotNull instead of @NotBlank for Long parent ID fields.

#### Features
- **agentic**: add message, attachment, action, model config and skill submodules (`d721b01c5`)
- **auth**: auto-manage tenant binding on user create and delete (`46b8c54af`)

#### Bug Fixes
- **manager**: guard profile bind removal and initialize empty device list (`480054d9a`)
- **driver**: tolerate incomplete attribute config and relax name pattern (`c5f1d8bd9`)
- **auth**: use @NotNull instead of @NotBlank for Long parent ID fields (`f08d31101`)

#### Refactoring
- **config**: update default AI model to deepseek-v4-flash and agentic env variables (`9d7e238eb`)
- **web**: remove context-path WebFilter from WebFilterConfig (`7e04ffd13`)

#### Chores
- **java**: sort imports and remove unused imports across modules (`ca45788d5`)

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
