# dc3-cli 全方位测试报告

**测试对象**: `iot-dc3` 仓库 `dc3-cli/`（Node >= 22, commander 12.1.0, tsup 构建）
**测试代码版本 (git rev)**: `338357430bef28baa67685d796d57848410e5358`（2026-10-06 08:33 +08:00 提交；验证阶段所用 dist 构建于 2026-10-06 08:45 本地，与 src 逐项核对一致）
**报告日期**: 2026-10-06

---

## 1. 执行摘要

- 12 个小队（5 黑盒 + 3 白盒 + fuzz / perf / AI 契约 / 多级指令）合计约 **1,370 次 CLI 调用**、40+ 个 mock/stub 网关进程（全部按 PID 清理、netstat 复核），产出 **56 条缺陷，已全部完成对抗验证判定：55 条 CONFIRMED**（critical 5 / high 10 / medium 17 / low 23）**+ 1 条 PARTIAL**（F044，缺陷归属上移至后端平台层），**0 条被反驳（REFUTED = 0）、0 条遗留未验证（UNVERIFIABLE = 0）**——对全部确认项的多角度反驳尝试（工作树已修？dist 陈旧？文档化行为？测试已覆盖？）均告失败。
- **后续补充对抗验证轮已覆盖初始验证配额耗尽后遗留的全部 31 条积压发现（F026-F056）**：30 条 CONFIRMED（medium 7 / low 23）+ 1 条 PARTIAL，判定条目与验证证据已并入第 3 节补充小节，第 4 节判定分布同步更新。
- 最严重缺陷 **F001**：CLI 入口从未调用 `program.enablePositionalOptions()`，commander 根选项跨整个 argv 匹配——README 记载的标准写法 `dc3 device update 42 --version 3` 被根 `--version` 布尔旗标拦截，**全部 16 个 update/delete 子命令打印 "0.1.0"、exit 0、零 HTTP 请求**（用户/agent 以为写成功，实际什么都没发生）；仅 `--version=3` 等号形式可用。
- 安全双杀：**F002** "加密"凭据库把明文 entries 映射（identifier→password）直接写进 `credentials.enc`，且 AES-256-GCM 密钥可由公开机器信息 + 硬编码 salt 离线重推导（静落盘加密机密性为零）；**F012** KeychainStore 在 win32/darwin/linux 三平台把 identifier/password 未转义内插进 shell 命令串，已实测任意命令执行。
- Windows 默认体验灾难（**F005**）：默认 keychain store 的可用性探测跑 `powershell Get-Help Get-StoredCredential`（~7-10.5s、stock Windows 恒 false、无超时），登录报 "Login successful" 但密码**未保存到任何位置**，静默续期永远不可能成功，每条 renewal 窗口命令多付 ~7s（12 倍延迟税）。
- 交互与输出契约破坏：**F004** 提示符在 stdin EOF 时 exit 0 静默空转（对脚本/CI 是假成功），open-but-silent stdin 永久挂起；**F009** tools 家族成功路径输出两份拼接 JSON 文档且 exit 1；**F013** 交互式密码输入后 `process.stdout` 被永久损坏，后续输出全部静默丢弃。
- 状态持久化层系统性缺陷：**F011/F024** 非原子就地写 + 无锁 + 静默 parse 重置——并发登录静默丢凭据（6 并行仅存 5/7 条）、令牌湮灭与"登出后会话复活"均已端到端复现；**F006** 一次越界 `config set settings.retry_count 99` 即可砖掉 config.json，下一次写入**永久抹掉全部 profiles**。
- 覆盖率基线 **42.36% stmts / 33.8% branch**；`index.ts / auth.ts / chat.ts / tool.ts / dashboard.ts / topic.ts / attachment.ts / utils/prompt.ts` 为 **0%**，凭据/令牌层 4.87%-12%；`test:coverage` 因缺 `@vitest/coverage-v8` 无法运行；99 个现有单测全绿但结构性测不到多数确认缺陷（如 manager-write 测试用裸 `new Command()` 未注册 `.version()`，F001 的碰撞永远打不中）。
- 大面积行为**经验证健全**（120+ 项）：8 组 CRUD wire 契约、退出码分类 0/1/2/3（非 tools 命令）、token 续期与 401 恢复、multipart 导入（sha256 字节级比对）、OAuth/MCP 门禁、格式优先级、管道 JSON 输出合法性等。修复路线图（第 10 节）遵循 AGENTS.md 第一性原则：一律根治最低破坏层，并为每条缺陷配防复发守护测试（负向验证）。

---

## 2. 方法论矩阵

| # | 小队 | 范围与方法 | 验证方式 | 规模 |
|---|------|-----------|----------|------|
| 1 | bb:config-auth | 配置 / 认证生命周期黑盒 | 隔离 HOME（USERPROFILE/HOME 覆盖）+ 4 个 mock 网关实例 + 1 次 live 只读会话；逐条断言退出码 / 输出 / 磁盘状态 | 62 次调用 / 15 批；39 项健全 / 9 条发现 |
| 2 | bb:manager-crud | 8 组 manager 资源 CRUD wire 契约 | mock.log 逐请求断言；forced-status / rich-entity 变体 mock（共 6 实例）；全程 timeout 15 零挂起 | ~100 次调用；1 次 live 只读 |
| 3 | bb:runtime-reads | 数据面只读（point / command / event / topic / dashboard / alert / analytics / format） | mock.log 86 行逐条比对 + 3 次 live 抽查 + 管道 JSON 校验 | ~80 次调用 / 8 批；9/9 piped JSON VALID |
| 4 | bb:agentic | session / action / provider / model / attachment / chat / tools | 9 个模式化 mock（500 / object 包络 / SSE hold / SSE / RPC error）跨 8 次重启 + 10 次 live 只读 | ~45 次调用 |
| 5 | bb:device-import | device import multipart / 轮询 / 幂等 / 重试 | 请求体 sha256 字节级比对；11 个模式 mock（op-mode / submit-status / fail-login） | ~25 次调用（含 18 次 import） |
| 6 | wb:credentials-security | 凭据存储白盒 | 源码审计 + 临时 vitest 探针（7 断言，跑后删除）+ 离线解密复现 + 2 mock；EOF / 挂起场景 | ~45 次调用 |
| 7 | wb:concurrency-state | 并发 / 状态文件完整性 | 2 个 TTL mock（7200s / 45s）；并行风暴（8×6 续期、10× 并行登录、42-key 放大 6×6）；原语级 torn-read 探针 | ~140 次调用 |
| 8 | wb:core-paths | 核心路径单元级（client / format / mcp / prompt） | 2 个临时 vitest 文件共 45 单测（跑后删除）+ 8 次黑盒差分（2 mock） | 45 单测 + 8 黑盒 |
| 9 | fuzz:adversarial | 全部 21 命令组 / 24 子命令对抗参数 | ~120 种参数形态矩阵；验证 mock 记录 112 次请求 | ~190 次调用 / 10 批 / 160 矩阵行 |
| 10 | perf:latency | 启动 / 负载 / keychain 税 / args 体积 | 交错配对对照（keychain vs encrypted、1p vs 50p、args 空 vs 30k 等）；7 个 TTL/LIST 变体 mock | ~45 批 / ~230 次调用 |
| 11 | ai:contract | AI / agent 集成契约 | 读扫描 69 + help 扫描 272 + 退出码分类 / 选项优先级 / prompt / profile 探针；1 mock + 3 status stub | ~380 次调用 |
| 12 | multi:nested | 多级指令矩阵（嵌套命令树 / help / 边缘形态） | device / config / auth 树 + help 形态差分 + 2 个 commander 最小复现 | ~65 次调用；20 项健全 / 7 条发现 |

**统一对抗验证阶段**：每条候选缺陷在当前 dist + src 上重放原始 repro，并从四个角度尝试反驳（工作树是否已修 / dist 是否陈旧 / 是否文档化预期行为 / 现有测试是否覆盖），全部反驳失败才判 CONFIRMED。前 25 条完成后验证配额耗尽，余下 31 条保留原始执行证据但未做对抗复核。

---

## 3. 确认缺陷（56 条，按 correctedSeverity 排序）

判定分布：**CONFIRMED 55 + PARTIAL 1**。初始验证轮 25 条（critical 5 / high 10 / medium 10），其中 5 条存在验证者对原始报告的微小修正，在对应条目"验证差异"中注明（修正均不削弱结论，多数反而强化）；后续补充验证轮覆盖剩余 31 条积压项（F026-F056）：**30 条 CONFIRMED**（medium 7 / low 23）**+ 1 条 PARTIAL**（F044，low），以"补充验证轮"小节追加于既有 25 条之后，同样按 correctedSeverity 排序，条目格式紧凑（根因/复现要点/影响面/根治修复/守护测试）。

### Critical（5 条）

#### F001 · critical · 根程序选项跨全 argv 解析：16 个 update/delete 子命令静默 no-op（exit 0、零 HTTP 请求）
- **根因（最低破坏层）**：`src/index.ts:44-49` 在 commander program 上注册根级选项（`-V/--version`、`--profile`、`--format`）却从不调用 `program.enablePositionalOptions()`。commander 12.1.0 的 `parseOptions` 对 program 选项默认位置不敏感（`lib/command.js:1733-1755`），根布尔 version 选项捕获 argv 中任意位置的 `--version <value>` 空格形式，其监听器 `writeOut + _exit(0)` 在子命令分发前终止进程；`--long=value` 快速路径仅匹配带值选项，故等号形式幸存。同一机制使 `--format/--profile` 变成全 argv last-occurrence-wins，叶子级 `.option('--format')` 恒为死代码。
- **复现要点**：`dc3 device update 42 --version 3 --name newname` → stdout 恰为 `0.1.0`，exit 0，mock.log 请求计数不变；8 实体 × {update, delete} 共 16 个子命令全部如此；`device list -V`、`device update --version 7 42` 同样被拦；对照 `--version=3` 正常执行 GET+POST。
- **影响面**：README:65-137 与 `--help` 展示的**文档化标准用法**整体失效——用户与 agent 相信写操作成功而实际零请求；`--format` 优先级承诺（README:244-247 `command --format > global`）仅在常规顺序下巧合成立。
- **根治修复**：`src/index.ts` 调用 `program.enablePositionalOptions()`，使根选项仅在首个子命令名之前被识别（与 README:235 契约一致）；随后复查 `utils/format.ts detectFormat` 是否真正实现文档优先级，并决定子命令后出现全局选项的报错语义（unknown-option 严格优于静默劫持）。
- **防复发守护测试**：按真实入口形态构建 program（`new Command()` + `.version('0.1.0')` + 根选项 + preAction），对 8 实体 × {update, delete} 断言 `parseAsync` 执行预期请求且从不输出 "0.1.0"；负向断言 `device list -V` 与裸 `--version` 仍正常出版本号；`--format table`（前）+ `--format json`（叶）解析为 json、非法叶 `--format` exit 1。
- **验证差异**：无（完全复现）。

#### F002 · critical · EncryptedFileStore 把明文 entries 映射写入 credentials.enc，且 AES-256-GCM 密钥可离线重推导——静态加密机密性为零
- **根因（最低破坏层）**：`src/core/credential-encrypted.ts` 序列化层缺陷——`EncryptedData.entries`（:41）被 `writeEntries`（:88）原样写入密文旁，而 `readEntries`（:60-75）只解密 `enc.data`、从不读 `enc.entries`（明文副本纯泄漏、无任何消费者）；更深层 `deriveKey`（:44-47）仅用公开机器标识（hostname-username-arch）+ 编译进 dist 的硬编码 salt 做 scrypt，无任何秘密参与密钥推导。
- **复现要点**：`auth login --store encrypted` 后 `credentials.enc` 含 `"entries":{"admin@default":"<明文密码>"}`（grep 命中 1）；独立 node 脚本用同样公开材料离线解密 `data` 字段成功。
- **影响面**：任何能读该文件（或推断三个公开值）的人即得密码；违反根 AGENTS.md 第 36 行"secrets 不出现在序列化中"；Windows 上 0600 权限承诺亦无效（见 F026，未验证）。
- **根治修复**：删除 `entries` 字段及明文写入（读取不用它，无迁移风险；可一次性清洗遗留文件）；改为首次生成随机 32 字节密钥存 `~/.dc3/credentials.key`（0600；Windows 用 DPAPI 或明示残余风险），使单凭文件无法解密；OS keychain 仍为文档化首选。
- **防复发守护测试**：`savePassword` 后读原始文件断言 `!raw.includes('pw')` 且 `parsed.entries === undefined`（负向验证）；roundtrip `getPassword` 仍返回密码；删除/移走 key 文件后解密失败。
- **验证差异**：无。

#### F003 · critical · attachment upload 发送 JSON 序列化的 Buffer 并静默丢弃 X-Filename / Content-Type 头——上传对所有文件 100% 损坏
- **根因（最低破坏层）**：`Dc3Client.post(path, body?)`（`src/core/client.ts:135-137`）无 headers 参数，`attachment.ts:48` 的第三个参数被静默丢弃（TS2554 过参错误之所以能出货，因 tsup/esbuild 构建不做类型检查）；`request()` 随即 `JSON.stringify` Buffer 产出 `{"type":"Buffer","data":[...]}` + `application/json`。更深一层：服务端契约要求 multipart/form-data `@RequestPart("file") FilePart`（filename 取自 part disposition），CLI 却发 raw octet-stream + 服务端从不读取的 X-Filename 头——仅修丢头问题上传依然是坏的。
- **复现要点**：上传 256 字节 0x00-0xff，mock 收到 940 字节、body 头部为 `{"type":"Buffer","data":[0,1,2,3`、content-type `application/json`、无 x-filename（ASCII 与中文文件名同样）；CLI 侧 exit 0。`tsc --noEmit` 当前置恰一处错误即此行。
- **影响面**：attachment upload 功能完全不可用；暴露"构建管线无类型检查"这一流程级缺口。
- **根治修复**：`attachment.ts` 改用 FormData + 既有 `postForm`（client.ts:139-145 转发 headers、FormData 分支删除 Content-Type 让 fetch 生成 boundary），对齐 `dc3-web/src/api/agentic.ts:196-204`；**流程根治：`tsc --noEmit` 接入 build/prepublish/CI**。
- **防复发守护测试**：本地捕获服务器断言 (a) content-type 以 `multipart/form-data` 开头，(b) 存在名为 `file` 的 part 且字节等于 fixture（含非 ASCII 文件名的 disposition）；CI 增加 `tsc --noEmit` exit 0 负向门禁。
- **验证差异**：无。

#### F004 · critical · 交互提示未做 TTY 门禁且 stdin EOF 时永不 settle：`auth login`（及 config reset、oauth 提示）exit 0 静默空转（对脚本是假成功）；open-but-silent stdin 无限挂起
- **根因（最低破坏层）**：`src/utils/prompt.ts:25-33/40-54`——readline 的 Promise 只在 `rl.question` 回调内 resolve，从不监听接口 `close` 事件；EOF 时回调永不触发，Promise 无结算路径，事件循环排空后 Node 以未触碰的默认 exitCode 0 退出。调用层（auth.ts:58-60/107-108、config.ts:252-254）把"缺旗标"当"提示"，无 `stdin.isTTY` 门禁。
- **复现要点**：`auth login < /dev/null` → 打印 "Tenant: " 后 exit 0、无 tokens.json、0 请求；`-t default -u admin < /dev/null` → 完全静默 exit 0（passwordPrompt 静音）；`sleep 10 | timeout 6 auth login` → exit 124（挂死）；`config reset < /dev/null` → 打印确认问题后 exit 0 未执行。
- **影响面**：CI / agent 链式 `dc3 auth login && ...` 视假成功为真；违反 errors.ts 刻意维护的 1/2/3 退出码契约；生态惯例（gh/docker/npm 非 TTY 报错，inquirer/clack close 即 reject）证实这是缺陷类而非设计。
- **根治修复**：prompt.ts 给每个提示终止契约——`rl.on('close', () => reject(new PromptAbortedError(...)))`（与 question 回调幂等）、finally 恢复 stdout.write、SIGINT 同路径；调用层在 `!stdin.isTTY` 且必需凭据旗标缺失时抛用法错误（指明 `--tenant/--username/--password` 或 `DC3_PASSWORD` 或 TTY 运行）。
- **防复发守护测试**：(a) spawn `auth login` stdin 关闭 → 非零退出 + stderr 指明缺失交互输入 + 无 tokens.json/config 写入；(b) config reset 同型（文件不变）；(c) open-silent stdin 短超时内自行非零退出（防挂起负向测试）；(d) 单元：Readable `push(null)` 中途 `question` Promise 被 reject 且 stdout.write 已恢复。
- **验证差异**：无。

#### F005 · critical · Windows 上默认 keychain store 是慢速静默 no-op：isAvailable() 探测 ~7-10.5s 恒 false，登录报成功但密码未存任何位置，静默续期永不可能，探测无超时
- **根因（最低破坏层）**：`credential-keychain.ts:43-44` 探测工具选错且无界——起完整 PowerShell 跑 `Get-Help Get-StoredCredential`（第三方 PSGallery 模块，stock Windows 永不安装，~8.5-10.5s 后 exit 1）；与兄弟方法不同未传 timeout。上层叠加：`credential-store.ts:105-107` 把"不可用"当静默跳过（无警告、无文档承诺的 fallback 链），`auth.ts:72-90` 无条件打印 "Login successful"。
- **复现要点**：stock Windows 默认 store 登录 → ok:true（7.2-11.3s）后 `~/.dc3` 仅 config.json+tokens.json，密码 grep 全盘为零、`cmdkey /list` 无条目；稳态 `device list` p50 ~7.1s（mock.log 恰一次 device/list，续期死在探测）；过期 token 23.7s 后 exit 3（proactive + 401 回退两次探测串行）；对照 `--store encrypted` 登录 343-1753ms、`config get` 803-924ms、`auth status` 769ms。
- **影响面**：默认平台 × 默认 store = 密码从不持久化、静默续期死亡、每条 renewal 窗口命令 ~12 倍延迟、过期场景 15s CI 包装下表现为 exit 124。
- **根治修复**：win32 改快速有界能力探测（`powershell -NoProfile -Command "if (Get-Command New-StoredCredential -ErrorAction SilentlyContinue) { exit 0 }; exit 1"` + `timeout: 2000`；darwin/linux 探测同样加超时）；`savePasswordToStore` 返回 `persisted:boolean`，未持久化时显著警告（JSON 增加 `password_saved:false`）；实现或删除 credential-store.ts:20-22 的虚假 fallback 链注释；可选深层修复：改用 inbox Windows Credential Manager（cmdkey/CredRead）让 stock Windows 真正可用。
- **防复发守护测试**：(a) exec stub 断言 win32 isAvailable 携带 ≤2000ms timeout 且 stub exit 1 时返回 false；(b) stub isAvailable=false 时 `auth login` 输出/JSON 含"未保存"警告；(c) 延迟回归：stub exec sleep 10s 断言 resolvePassword 在 ~2s 内返回/拒绝；端到端断言 store 不可用时 renewal 窗口内 device list <1s。
- **验证差异**：无。

### High（10 条）

#### F006 · high · `config set` 接受越界 settings，下次加载砖掉 config.json，随后任何写入永久摧毁全部 profiles
- **根因**：配置写路径——`ConfigManager.setSetting`（config-manager.ts:301-306）赋值即持久化，无 `AppSettingsSchema` 校验（max 边界只在读取时 enforced）；`readConfig`（:114-123）把一切 parse 失败（含 CLI 自己写出的越界值）归类为"文件损坏"→ 默认值 + 下次写入覆盖；命令层 `parseSettingInteger` 只有非负下界且边界不从 schema 派生（单一事实源被破坏）。净不变量被打破：**CLI 持久化了自己读不回的状态**。
- **复现要点**：`config set settings.retry_count 99` → ok:true 落盘；下一次 `config list` → corrupt 警告 + profiles:{}；再任意写命令 → config.json 重写为引导默认值（真实 gateway/tenant/store 永久消失）；`.bak` 保存的是**损坏版**而非 last-good。`retry_count 4`（超 max 3 仅 1）与 `renewal_threshold_hours 99` 同样触发。
- **影响面**：一个合法用户手误即可摧毁整个 CLI 本地状态；讽刺的是 `retry_count` 在 config-manager 之外零消费者（能毁配置的死设置）。
- **根治修复**：setSetting 写盘前过 `AppSettingsSchema.shape[key].parse` 并以 Zod 信息作为 exit 1 业务错误；命令层边界从 schema 派生；readConfig 纵深防御——区分真损坏与字段级越界，对后者按字段回退默认但**保留 profiles**，且不覆盖本身可解析的既有 `.bak`。
- **守护测试**：负向——`retry_count 99`/`4` exit 1 且磁盘文件逐字节不变；手写越界 config + 真实 profile 后任意写命令，profile 必须存活；roundtrip 不变量——每次成功 set 后新进程 `config list` 无 corrupt 警告。
- **验证差异**：砖化后 `device list` 在验证轮表现为 exit 2（网络错误，默认 gateway localhost:8000）而非原报告的 exit 1 "Profile not found"（其运行发生在重建裸 default profile 之前）——同一砖化，症状表层差异。

#### F007 · high · 全新安装引导端到端断裂：gateway-first `config set` 抛原始 ZodError 倾倒；建议的 `dc3 config init` 不存在；无法创建 profile（README Multi-Profile 流程不可达）
- **根因**：`ProfileConfigSchema`（config-manager.ts:25-30）中 username 是唯一无默认的必填字段，`setProfile`（:255）对每次部分写校验**整个合并后 profile**——首写必炸除非恰好带 username。叠加：getActiveProfile 的补救串（:222）指向从未注册的 `config init`；顶层 handler 原样转发 ZodError.message（裸 issues 数组泄到 stderr，违反 `{ok:false,message}` 契约）；完全没有 profile 创建面。
- **复现要点**：全新 HOME `config set gateway http://...`（README:15 Quick Start 第 1 步）→ exit 1 + stderr 裸 zod 数组；`device list` → "Run: dc3 config init" → `error: unknown command 'init'`；`config profile use prod` → not found 且无创建命令；`config set gateway not-a-url` / 空 tenant 同样裸 ZodError。绕过方案：**仅** username-first 可行。
- **影响面**：每个 manager 命令的文档化首跑路径断裂且无可用错误信息；README Multi-Profile（:255-266）需手改 config.json。
- **根治修复**：setProfile 校验**传入 partial**（逐 key 过 `ProfileConfigSchema.shape[key].parse`，失败走 `printAndExit({ok:false,...},1)`；username 必填性由消费端 getActiveProfile 把守）；注册真实 init / `config profile create <name>`；handleFatalError 检测 ZodError 折叠为单行人读信息。
- **守护测试**：全新 tmp HOME gateway-first exit 0 并以 schema 默认值落盘；非法值 exit 1 且 stderr 不含 `"code":"invalid_string"`；**快照测试遍历 program.commands 断言 src 中每个 `Run: dc3 ...` 串都指向已注册命令**（消灭整类悬空补救信息）；README Multi-Profile 序列端到端测试。
- **验证差异**：原报告称 tenant-first 可作绕过——实际 tenant-first **同样失败**（唯一无默认字段是 username），强化而非削弱排序陷阱结论。

#### F008 · high · `auth login --no-save` 是 no-op：commander v12 否定旗标暴露为 `save:false` 而代码读 `options.noSave`（恒 undefined）——密码照样持久化且 credential_store 被静默降级（永不为 'prompt'）
- **根因**：`.option('--no-save')` 语义下 commander 置 `options.save=false`、从不设 `options.noSave`；`auth.ts:68`（store 选择：`'prompt'` 分支不可达 → 静默覆盖为 `--store` 默认 keychain）与 `:72`（保存守卫：`!undefined` 恒真 → `savePasswordToStore` 必然执行）两处误读。仓库正确范例就在 `device.ts:227`（`opts.wait === false`）。
- **复现要点**：种子 profile `credential_store:"encrypted"` 后 `auth login --no-save` → config 翻转为 `"keychain"`（永非 'prompt'）；变体 `--no-save --store encrypted` → `credentials.enc` **被创建**且解密出 `admin@tenantA` 条目（明文密码在显式 opt-out 后仍持久化）；最小 commander 复现 `parse(['x','--no-save'])` → `{"save":false}, o.noSave === undefined`。
- **影响面**：文档承诺 "Do not save password" 被违反——在 keychain 探测成功的机器（macOS / 装模块的 Windows）上明文密码必然入库；stock Windows 本测试箱仅因探测恒 false 而"自我中和"。
- **根治修复**：action 内一次派生 `const noSave = options.save === false;`，两处使用（store 选择与保存守卫）。
- **守护测试**：扩展 login 测试（mock seam 已存在）——`--no-save` 时 `savePasswordToStore` 未被调用且持久化 profile `credential_store === 'prompt'`；负向对照：无旗标时被调用、`--store` 生效、预置 store 在 `--no-save` 时保留；可选端到端断言无 credentials.enc。
- **验证差异**：无。

#### F009 · high · `tools list` / `tools call` 成功时在正确输出后追加 `{"ok":false,"message":"silent exit"}` 并 exit 1——SilentExit 哨兵被命令自身 try/catch 吞掉并误报为错误
- **根因**：`printAndExit`（format.ts:145-153）以抛 `SilentExit` 终止命令；`tool.ts:41-58/67-88` 的 catch 把**一切**被捕获值当失败（无 `instanceof SilentExit` 透传），用 `printAndExit({ok:false,...},1)` 重报——覆写 exitCode 0→1、拼接第二个 JSON 文档、再抛的 SilentExit 被顶层 index.ts:84-85 静默吞掉。控制流契约（"print 后 unwind 穿过所有命令代码"）被 catch 边界违反。
- **复现要点**：OAuth 登录后 `tools list --format json` → 正确 rows 数组**后跟**第二份错误文档，exit 1（mock.log 确认 /mcp 200）；`tools call device.list --args {"limit":1}` 同；内部校验路径 `--args not-json` 双文档。对照：catch 内打印的失败路径（AuthError 3 / NetworkError 2）单次打印且保码——仅 try 块内发起的打印会双打印。
- **影响面**：整个 tools 家族的成功路径破坏 `&&` 链、`| jq` 管道（两份拼接文档）与 agent 退出码解读；同形隐患还存在于 alert/analytics/attachment/command/session 五个文件。
- **根治修复**：集中式共享 action wrapper（AuthError→3 / NetworkError→2 / 其他→1，**永远重抛 SilentExit**），tool.ts 等六文件统一改造；或最小化：每个 catch 首句 `if (err instanceof SilentExit) throw err;`，或改为 try 内计算、try 外打印（auth.ts 已用此形态并验证 exit 0 单文档）。
- **守护测试**：spawn 构建产物对 mock /mcp——`tools list` exit 0 且 stdout 恰一份可 parse 的 JSON；`tools call` 同；`--args not-json` exit 1 且恰 `{ok:false,message:'--args is not valid JSON'}`；跨命令家族的"单 JSON 文档 + 预期退出码"契约测试。
- **验证差异**：无。

#### F010 · high · `waitForOperation` 无 deadline / 最大轮询数 / AbortSignal：永不终态的 operation 把 CLI 永久挂起（固定 500ms 节拍、无退避）
- **根因**：`device.ts:43-47` 循环唯一终止条件是服务端终态；把"未终态"与"永远等"混为一谈。`core/http.ts` 的 30s AbortSignal.timeout 只约束单次 fetch 不约束循环；唯一钳制是 `Math.max(100, pollInterval)`。
- **复现要点**：MOCK_OP_MODE=RUNNING（恒 200 RUNNING）+ `timeout 15 device import` → exit 124、~15.6s、零输出；mock.log 29 次 GET 轮询、间隔 min/avg/max 518/523/534ms（零退避，外推 ~7127 次/小时）。对照：`--no-wait` 立即 exit 0；3 次后 SUCCEEDED 的 mock 1.9s 完成。
- **影响面**：网关/worker 卡在 PENDING/RUNNING 即挂死进程，脚本与 CI 无限停摆，Ctrl-C 是唯一出路。
- **根治修复**：在 waitForOperation 内部根治——由 operation 自带 `expiresAt` 推导总 deadline（`--wait-timeout <seconds>` 缺省 600、0 显式退出，有 expiresAt 时钳到其+grace）；指数退避（pollInterval 起步、封顶 10s）；SIGINT 接 AbortSignal（退出前打印最后状态与 statusUri）；超时以结构化非零错误收场（`{ok:false, operationId, lastStatus, elapsedMs, statusUri}`）。
- **守护测试**：mock fetch 恒 RUNNING + fake timers 推过 deadline → 命令以非零超时错误终止（而非永 pending Promise）；断言请求时间戳间隔递增（退避）——无界循环回归即失败。
- **验证差异**：无。

#### F011 · high · 非原子就地写 + 无锁 + 静默 parse 重置：并发命令可湮灭 tokens.json 并静默丢弃凭据条目
- **根因（最低破坏层）**：持久化层三个写入者（token-manager.ts:72-76、credential-encrypted.ts:90、config-manager.ts:128）全部 `writeFile` 目标文件就地截断写（无 temp+rename、无进程间互斥）；load 路径（token-manager.ts:61-70、credential-encrypted.ts:60-75）把"文件缺失（首跑）"与"读到写一半/损坏"混同并静默代之以空状态。两种失效模式：lost-update 读-改-写（端到端复现）；torn-read-then-silent-reset（读空 → `{}` → 下次 save 只剩一个 profile，永久湮灭其余、无警告无 .bak）。
- **复现要点**：(b) 端到端复现且**比报告更糟**——6 个并行 `auth login`（同 home 不同用户）全部 exit 0 ok:true、mock 记录全部 7 次 generate，credentials.enc 仅存 **5/7** 条（agent2/agent4 静默丢失）；(a) 42-key 放大未复刻 keys=1 瞬态，但机制链完全证实——风暴中 live 捕获一次撕裂态（170 次调用中 1 次 PARSE_FAIL）、原语探针 7238/14657（49%）并发写下读到空文件、临时 vitest（现 src）证明种子 43 key + 写 '' → load 静默重置 → 下次 save 只剩 ['a'] 且零输出。
- **影响面**：多 profile 令牌/凭据静默湮灭；`fs.writeFile` 的 open→write 间隙在本平台可观测。
- **根治修复**：(1) 三写入者统一 `${path}.tmp-${pid}-${rand}` + fsync + `fs.rename` 原子替换；(2) load 仅 ENOENT 返回 `{}`，parse 失败短退避重读后**响亮失败**并保 .bak（如 config-manager 已有做法），绝不从静默重置态持久化；(3) load+save 全程加进程间锁（O_EXCL lockfile 带陈旧恢复）——原子 rename 单独**不能**修 (b) 的丢更新。
- **守护测试**：(i) 种子 N profile + 覆写 ''（撕裂态）→ saveState 后仍 N+1 key 或响亮失败；savePassword 同；(ii) 并发回归：N 个并行调用共享 tmp home 结束后恰 N 条（静默丢失即失败）；(iii) 探针式断言写入者被 kill 在写中也不留不可解析目标文件。
- **验证差异**：keys=1 一次性事故为低概率交错（已证链路的非确定性末端），机制级证据完备。

#### F012 · high · Shell/PowerShell 注入：KeychainStore 把 identifier 与 password 未转义内插进 exec 命令串（win32/darwin/linux 全平台）
- **根因（最低破坏层）**：`credential-keychain.ts` 使用 `node:child_process exec`（必然过 shell：win32 cmd.exe→powershell，POSIX /bin/sh）并在所有路径字符串内插不可信输入（savePassword win32:101-103 / linux:93-94 / darwin:87-89；getPassword darwin:57/linux:64；deletePassword darwin:112/linux:116）。win32 单引号未翻倍（`'` 闭合 PS 字面量开启语句注入）、POSIX 双引号内 `$()`/反引号展开、`"` 逃逸；仅 win32 Target 名被净化。linux 分支额外用 `echo "${password}"` 管道（进程表可见）。
- **复现要点**：单元级 7/7 断言（对抗 fixture 的精确命令串）；**端到端实弹**：复刻 :97-106 构造、良性 payload `x')) ; whoami > '<marker>' ; #` 经 node exec → cmd.exe → powershell 链**真实执行** whoami 并落 marker 文件（UTF-16 输出）；POSIX 语义验证 `echo "p$(echo X)x\`echo Y\`y"` → `pXxYy`。可达性：auth.ts:73 以原始 `--username/--tenant/--password` 直喂。
- **影响面**：credential_store=keychain 时任意命令执行（攻击者影响的凭据或用户自伤输入）。
- **根治修复**：全平台改 `execFile`（argv 数组、无 shell），秘密走 stdin 绝不走 argv/echo——linux `secret-tool store ... ` + child.stdin；darwin `security ...`（或 `security -i` 命令走 stdin）；win32 `powershell -NoProfile -EncodedCommand`（PS 单引号翻倍 + 密码走 stdin，EncodedCommand 顺带消灭 cmd.exe 引号层）。
- **守护测试**：mock child_process，对抗 fixture 断言 (1) execFile 收到 argv 数组（payload 为离散元素）或解码后脚本无未翻倍单引号，(2) 构造串中无裸 `$()`/反引号/引号外 `'`，(3) linux 上秘密绝不出现在 echo/argv 位置。
- **验证差异**：原报告演示 payload `' ; Whoami > ...; (` 引号不平衡会导致 PS 解析错误不执行；同型平衡 payload 已实测执行——结论不变。

#### F013 · high · passwordPrompt 永久损坏 process.stdout：交互式密码输入后的全部命令输出被静默丢弃（mock 显示登录成功但 stdout 0 字节、exit 0）
- **根因（最低破坏层）**：`prompt.ts:44-49` 给进程级 stdout 单例安装自有属性 `stdout._write = stdout.write.bind(stdout)`。Node Writable 内部经动态属性查找分发 `_write(chunk, enc, state.onwrite)`，自有属性拦截后重入 `writeOrBuffer` 只缓冲、`onwrite` 永不回调——`state.writing` 锁死、`writableLength` 永不排空，此后每次 `stdout.write` 返回 true 而字节永不达 fd。恢复路径只重指 `stdout.write` 为绑定克隆（丢失恒等性）且从不删除 `_write`。次要：静音在 `rl.question` 写提示前安装，"Password: " 问题文本本身也被吞。
- **复现要点**：管道密码差分——`printf 'dc3dc3dc3\n' | auth login ... --store env` → exit 0、stdout 0 字节，mock.log 记录完整 salt+generate 200、tokens.json 已落盘（登录成功仅输出被吞）；对照 `-p dc3dc3dc3` → 214 字节 JSON。机制探针：resolve 后 `stdout.write` 恒等性改变、`_write` 自有属性存在、后续 write true 但文件 0 字节、`writableLength` 卡 54。
- **影响面**：所有交互式密码/secret 提示（auth login、oauth client secret）对管道/agent 消费者静默成功。
- **根治修复**：彻底不碰 process.stdout——问句写 stderr（管道 stdout 保持干净），readline 用私有黑洞 `new Writable({ write(_c,_e,cb){cb();} })` 作 output；若必须保留变异方案，捕获原始函数恒等（非 `.bind`）、恢复原引用、删除 `_write`、安装静音前先发问句。
- **守护测试**：子进程 vitest——stdin 管道密码 + stdout 重定向文件，断言 (1) 结果输出（登录 JSON）>0 字节，(2) 子进程经 stderr 报告 `hasOwnProperty(stdout,'_write')===false` 且 `writableLength===0`，(3) 问句出现在 stderr 而非 stdout。
- **验证差异**：无。

#### F014 · high · `dc3 help <group> <sub>` 忽略子命令路径：exit 0 打印**组级**帮助——commander 的 help 命令只转发第一个路径段，agent 自然的发现约定拿到错误层级帮助且无错误
- **根因**：commander 12.1.0 `_parseCommand`（command.js:1463）只把 `operands[1]` 交给 `_dispatchHelpCommand`（:1278-1293），其余段在查找前被静默丢弃；CLI 根程序注册裸 `new Command()` 从不覆写 helpCommand。
- **复现要点**：`dc3 help device list` 首行 `Usage: dc3 device [options] [command]`（组帮助）vs `dc3 device list --help` 首行 `Usage: dc3 device list [options]` + 完整选项表，diff 不同；`dc3 help config profile use` 同样退到 config 组帮助而 `dc3 config help profile` 正确；扫描：**全部 20 个**含子命令的组皆然；加重项：`dc3 help device typo-not-a-sub` 也 exit 0 给组帮助（丢弃的段从不校验）。
- **影响面**：脚本化帮助发现的用户/AI agent 以成功码拿到更浅的错误帮助。
- **根治修复**：`src/index.ts` 移除默认 help 命令，注册路径感知版本——`help [commands...]` 逐段解析（匹配 name/alias），未知段 `cmd.error(...)` exit 1，末端 `cmd.help()`；同时消灭"打错子命令名也 exit 0"的加重项。升级 commander 前先核对上游发行说明。
- **守护测试**：对每组每子命令断言 `dc3 help g s` 首行 === `dc3 g s --help` 首行且 exit 0；负向：`dc3 help device no-such-sub` 非零退出。
- **验证差异**：原报告"21 组"轻微超计（chat 无子命令，实为 20 组）；"115/272"扫描计数未重推导，但缺陷类普适成立。

#### F015 · high · 双分歧错误通道：`{ok:false}` JSON 走 STDOUT（exit 1/3）vs `Error:`/`error:` 文本走 STDERR（exit 1/2/3）——仅解析 stdout 的 agent 无法统一探测失败
- **根因**：不存在单一失败发射 chokepoint——`printAndExit`（format.ts:145）把任意 payload（含错误形态对象）写 STDOUT + 任意退出码，命令 action 临时起意调用；`handleFatalError`（errors.ts:64-77）把逃逸异常以 `Error: <msg>` 文本写 STDERR + 分类码；commander 另发 `error:` 用法错误到 stderr。退出码分类学（0/1/2/3）有设计，**失败时的流/payload 契约从未被设计**——同一失败类走哪个通道取决于哪个命令碰巧用了哪条路径。
- **复现要点**：STDOUT 通道（stderr 全空）：`config set nosuch.key x`、`alert bulk-confirm --args notjson`、`provider check`、`tools list`（无 OAuth，exit 3——唯一 exit 3 走 stdout 的案例）；STDERR 通道（stdout 全空）：`device count` 缺必需项、死网关 exit 2、401 exit 3、强制 404/500 exit 1。同一 "Profile default not found" 经 tools list 走 stdout JSON、经 device list 走 stderr 文本。
- **影响面**：README:269-277 把 `--format json` stdout 解析定位为 agent 集成路径，却未定义错误流契约——stdout-JSON-only agent 在每个 stderr 通道失败上得到空串/解析错误。
- **根治修复**：统一 chokepoint——每次失败在 STDOUT 发机器可读 envelope `{ok:false, error:{kind:'auth|network|api|usage|validation', code, message}}` + STDERR 保留人读行，维持 0/1/2/3；实现：handleFatalError 扩展为唯一失败出口、命令级 `printAndExit({ok:false})` 全部改为抛类型化错误（新增 ValidationError/UsageError→1）、commander 用法错误经 configureOutput/exitOverride 接入同一 envelope。
- **守护测试**：spawn 级失败矩阵（未知 key、坏 --args、缺必需项、死网关、401、404、500、MCP 无 OAuth）逐项断言：退出码符合分类表 AND stdout 可 parse 且 `ok===false` AND `error.kind` 正确；负向：无失败路径 stdout 为空/不可解析、无成功路径发 ok:false。
- **验证差异**：无。

### Medium（10 条）

#### F016 · medium · `dc3 config reset` 留下活令牌与已存密码（tokens.json、credentials.enc 存活）；事后 `auth status` 仍 authenticated:true；租户继承角已实证
- **根因**：本地认证状态分居三 store（config.json / tokens.json / 凭据库），reset 只认第一个——`ConfigManager.reset()`（:311-314）仅重写 config.json，reset action 未调任何 token/凭据清理；加重层：client.ts request() 纯按 tokens.json（profile 名索引）组 X-Auth 头，不与活动 profile 的 tenant/username 交叉校验。
- **复现要点**：确认 reset 后 config.json 为默认（profiles:{}）但 tokens.json（348B）与 credentials.enc（196B）原样存活；`auth status` → `{"authenticated":true,"tenant":"tenantA",...,"remaining":"1h 59m"}`；离线解密存活文件得回明文密码；**租户继承**：reset 后以 bob@tenantB 重建 default profile（从未登录）跑 `device list` 成功且 mock 记录 `{"tenant":"tenantA","login":"admin"}`——静默沿用他人活会话。
- **影响面**：确认语 "This will delete all profiles and config" 误导用户以为机器已清空；下一个配置 default profile 者静默继承旧租户令牌。
- **根治修复**：reset action 遍历 `tokenManager.getAllStates()` 逐条 `clearState(profile)` + `deletePasswordFromStore(username@tenant)`（空时两库自行 unlink）；长期：把状态库注入 `ConfigManager.reset()` 形成单一 chokepoint，杜绝部分 reset。
- **守护测试**：种子有效态后 reset → tokens.json/credentials.enc 不存在且 `auth status` authenticated:false；负向租户隔离：reset 后以不同租户重建 default，对本地 mock 发请求，断言捕获头**不含**旧 X-Auth-Tenant/X-Auth-Login。

#### F017 · medium · dashboard/alert 读平面查询串裸模板内插（无 encodeURIComponent）：`&` 注入额外网关参数、`#` 静默截断值、`=` 渗入结构
- **根因**：命令层查询构造——13+ 调用点（dashboard.ts L48-50/64-66/78/104/127-129；alert.ts L67/109/122-124/136-138/146-149/236）模板字面量内插用户旗标；client 层无共享查询构建器，编码约定应用不一致（alert confirm/unconfirm 与 --query 分支有编码，其余没有）；且无枚举/范围校验。
- **复现要点**：`--granularity 'hour&evil=1'` → 线上 `?granularity=hour&evil=1&range_hours=24`（evil=1 成为独立网关参数）；`'24#section'` → fragment 静默丢弃；`'a=b'` → `granularity=a=b`；`top --limit abc --range-hours -5`、`top-n -3` 等全部直传 exit 0；对照 `confirm --source 'dev&x'` → `dev%26x`（仓库约定存在，这些站点漏用）。
- **影响面**：查询参数注入 / 静默截断 → 服务端收到与用户所求不同的查询（错误分析结果、无错误）。
- **根治修复**：`dc3Client.get(path, params?)` 或 `utils/query.ts buildQuery` 基于 URLSearchParams，全部列出站点改走；配套 `choices()` 枚举（granularity/dimension/mode）与 `parseNonNegativeInteger`（days/limit/range-hours/top-n/baseline-days/silent-minutes）。
- **守护测试**：对抗 fixture 断言请求 URL 含 `hour%26evil%3D1` / `24%23section` / `100%23frag`；负向：任何发出的 URL 不含独立 `evil=`/`q=injected` 参数；保留 confirm 编码对照断言。

#### F018 · medium · 显式空串选项值在 device/driver/profile/event/command 的 add+update（含 point --name）被静默丢弃——无法清空 name/remark；与 group/label 行为不一致
- **根因**：命令层选项→payload 映射用 JS 真值守卫（`opts.name ? {...} : {}`）混淆"选项缺席"与"值为空串"；`--name ''` 解析为 `''`（falsy）被省略，update 的 `{...current, ...changes}` 保留服务端旧值，add 直接缺字段。仓库正确惯用式（`!== undefined`）就在 group.ts:80 / label.ts:84-86。
- **复现要点**：rich-entity mock 下 `device update 77 --version=9 --name ''` → POST body 保留 `deviceName:"svc-name"`，exit 0 零警告；`device add --description ''` → remark 键整体缺失；对照 `group update 1 --name ''` → 发送 `"groupName":""`；`driver update --name ''` 同样受累。
- **影响面**：用户无法经 CLI 清空名称/备注且无任何反馈；同类不一致遍布 6 个命令文件。
- **根治修复**：device.ts:129/132/110、driver.ts:94、profile.ts:116、event.ts:114、command.ts:104、point.ts:183 的字符串选项守卫改 `!== undefined` 存在性检查——空名是否合法交由网关 4xx 回答（正确反馈环），而非客户端静默丢弃。
- **守护测试**：扩展 manager-write 表驱动循环——`[entity,'update','id A&B','--version','3','--name','']` 断言 `JSON.parse(body)[nameField] === ""`（严格相等，非 toMatchObject 真值）；device 专属对 `--description ''` 在 update 与 add body 均断言 `remark:""`。
- **验证差异**：原报告把 point 列入"会发送"阵营——实际 point.ts:183 的 --name 同为真值守卫（行为验证：`--unit ''` 发送而 pointName 被丢）；一致使用 `!== undefined` 的只有 group 与 label。

#### F019 · medium · `alert --query` 帮助宣称 "(repeatable)" 但重复旗标静默 last-wins；畸形 kv（无 `=`）与空键 kv 被静默丢弃 exit 0
- **根因**：`--query` 注册为普通单值 string 选项（无收集器 parse 函数），commander last-write-wins 使 `appendQuery` 的 `Array.isArray` 分支（alert.ts:151-155）恒为死代码、帮助文本 "(repeatable)" 为假；`eq > 0` 守卫静默丢弃畸形输入而非 fail-fast。
- **复现要点**：`--query a=1 --query b=2` → 服务端只见 `?b=2`（a=1 静默蒸发）exit 0；`--query noequal` / `--query =value` → 空 query exit 0 ok:true；对照 `--query a=1` → `?a=1`。
- **影响面**：用户指定的过滤器静默消失，服务端跑出与所求不同的分析且无错误。
- **根治修复**：按 commander 文档注册收集器 `.option('--query <k=v>', '...', (v, prev=[]) => prev.concat(v), [])` 使 opts.query 恒为 string[]（现数组分支复活）；`kv.indexOf('=') <= 0` 时 `printAndExit({ok:false,message:'--query expects <key>=<value>, got: ...'},1)` fail-fast。
- **守护测试**：`['alert','storm-sources','--query','a=1','--query','b=2']` 产出 URL 同时含 a=1 与 b=2；`noequal`/`=value` 非零退出且消息点名坏值；负向：任何良形重复组合都不会产出缺过滤器的 URL。

#### F020 · medium · 表格渲染器搅乱列表数据：包络响应（OffsetPage / {ok,total,data}）渲染成单行巨型 JSON（数组分支不可达）、表头只取首行键、嵌套对象打印 [object Object]、标量数组空白；yaml 同样塌缩
- **根因**：输出格式层的隐式契约（flat record 或均质扁平 record 的裸数组）与命令层实际喂入的网关包络不匹配（Dc3Client 不做展示层解包）——`formatTable` 的 isRecord 分支（format.ts:61）吃掉一切包络，`flattenObject`（:37）把行数组 JSON.stringify 成单格；数组分支表头 `Object.keys(data[0])`（:48）丢后续行键、`String()` 单元格（:50）产生 [object Object]；yaml 分支 `${k}: ${JSON.stringify(v)}`（:100）同样单行塌缩。TTY 默认 format 是 table。
- **复现要点**：OffsetPage mock 下 `alert list --format table` → 恰 5 行，`items │ [{...},{...},{...}]` 整个数组一行；`device list --limit 10000 --format table | wc -l` → 5；对照裸数组（alert latest）渲染正常列表；yaml 同塌缩；`--format json` 正确（缺陷纯在 table/yaml 展示层）。
- **影响面**：人类面对的每个列表都是一行不可读 JSON（100k 条时 ~10MB 单行）；异质行键静默丢失（单元级验证 SECRET-MARKER 消失）。
- **根治修复**：一次修在 format.ts 使 ~20 个命令文件受益且 JSON 输出字节不动（agent 依赖）：(a) 先探测已知列表包络（items / data）取行渲染，分页元信息作 footer（`-- 3 of 42 (offset 4, limit 3), hasNext: true`）；(b) 表头取全行键并集（首现顺序）；(c) cellify() 处理嵌套/ null / undefined；(d) yaml 分支同探测并按行输出真列表。
- **守护测试**：envelope-in-table 不含 `'[{"id":'` 且各记录字段分行可见；异质行显示 extra；嵌套单元格含 `"a":1` 而非 [object Object]；`formatOutput([1,2],'table')` 显示值；yaml 包络每行一个条目；JSON 格式快照锁死 agent 契约不漂移。

#### F021 · medium · 多个表面绕过共享数字/枚举校验器：`action pending --offset/--limit` 裸 parseInt（offset=NaN 与截断整数上线；live 网关 400）、`point history --limit` 转发原始字符串（limit=-5）、dashboard/alert 数字旗标无校验、`provider check --level` 接受任意值
- **根因**：各命令文件的 commander 选项声明层——仓库已有共享校验器（parseNonNegativeInteger/parsePositiveInteger、parseProviderType）但五组表面（session.ts:143-144、point.ts:86、dashboard 多处、alert 多处、provider.ts:131）把数字/枚举旗标声明为裸 `'<n>'` string 或无 InvalidArgumentError 的 parseInt，垃圾值直接序列化进查询串/请求体，把校验推迟给服务端。
- **复现要点**：`action pending --offset abc --limit 5` → 线上 `offset=NaN&limit=5` exit 0 打印 ok:true；`--offset -5.9 --limit 1e9` → `offset=-5&limit=1`（parseInt 截断，limit 静默差 9 个数量级）；`point history --limit=-5` 直传；`dashboard top --limit abc --range-hours -5`、`device-stats --top-n -3`、`provider check --level GARBAGE_LEVEL`（body 直传）全部 exit 0；live 网关只读验证 `--offset abc` → `Error: HTTP 400: Type mismatch.`；对照 `point list --limit -1` → commander exit 1 清晰报错。
- **影响面**：垃圾与截断值上线，后端报晦涩错误或静默错误分页；CLI 打印结果 exit 0 仿佛有效。
- **根治修复**：选项声明时挂共享 parser——offset 用 parseNonNegativeInteger，limit/top-n/days/range-hours/baseline-days/silent-minutes 用 parsePositiveInteger，`--level` 加 L1|L2|BOTH choices（镜像 parseProviderType）。
- **守护测试**：(a) 表驱动——各表面跑垃圾参数断言 commander InvalidArgumentError/exit 1 在任何 HTTP 前（spy dc3Client 断言零调用）；(b) 自省扫描——遍历 program.commands 断言每个占位符 `<n>`/数字默认值的选项都挂了 parser（修复前恰这五处失败、修复后通过的负向门禁）。

#### F022 · medium · MCP 客户端发送 `Accept: application/json, text/event-stream` 却无法解码 SSE 帧的 JSON-RPC 响应——浮出原始 SyntaxError 而非诊断
- **根因**：`mcp.ts:42` 的 Accept 头按 MCP streamable-HTTP 传输邀请 SSE（规范允许 POST 以 SSE 帧应答），但响应处理从不检查 Content-Type、无条件 `res.json()`（:61）——SSE 帧 200 抛裸 V8 SyntaxError，经 tool.ts 通用 catch 原样打印为用户诊断。
- **复现要点**：mock /mcp 以 `Content-Type: text/event-stream` + `event: message\ndata: {jsonrpc result}` 应答 → `tools list` 输出 `{"ok":false,"message":"Unexpected token 'e', \"event: mes\"... is not valid JSON"}` exit 1（字节级一致）；`tools call` 同；正对照同 mock 改 application/json 正常渲染——缺陷孤立于 SSE 帧。
- **影响面**：对规范合规的 SSE 服务器 tools 家族静默不可用。
- **根治修复**：rpc() 在 res.ok 后按 content-type 分支——json 保持 `res.json()`；`text/event-stream` 取文本、按空行拆帧、取 data: 行 JSON.parse、按请求 id 匹配（兜底首条）；其他类型给显式诊断 `Unsupported /mcp response content-type: X`。
- **守护测试**：stub fetch 返回 SSE Response 断言 `listTools()` 解析为 `{tools:[]}`；伴生断言 application/json 分支仍正常（双向负向守护）。

#### F023 · medium · `chat --stream` 收到 [DONE] 不终止：break 只退出内层行循环，进程生命周期绑定服务端关连接（SSE keep-alive 网关下无限挂起）；多发两个尾部换行
- **根因**：chat.ts:103-127 的 SSE 消费循环唯一退出条件是传输层（reader done / 拒绝）；应用层终止符 `[DONE]`（:112-115）的无标签 break 作用域仅是内层 for——外层 while(true) 继续无限 await；叠加流式 fetch 刻意无超时（:72-85）。化妆级兄弟：`process.stdout.write('\n')` 在 [DONE] 分支与循环后各发一次。
- **复现要点**：mock 写完 delta + `data: [DONE]` 后 hold 8000ms——内容 +229ms 已完整，第二个换行 +8242ms（服务端关闭后 ~230ms）才出现，墙钟 8735ms；hold 60s 变体被 timeout 12 杀（exit 124，stdout 早已完整）；正常 happy path 0.577s exit 0。
- **影响面**：心跳/会话复用型网关（MCP 风格 streamable HTTP 的标准行为）下 CLI 永久挂起。
- **根治修复**：按协议哨兵终止——[DONE] 置 finished 标志、外层条件 `while (!finished)`、循环后 `await reader.cancel()`（undici 立即释放底层 socket，不等服务端关闭）；可选 AbortController 双保险；保留无超时设计（事件驱动而非定时器）；尾部换合并为一次。
- **守护测试**：本地 server 写完 [DONE] 后永不 res.end——动作须在 ~2s 测试超时内 resolve（负向：2s 时仍 pending 即失败）+ stdout 精确等于 `Hello world\n`（单换行锁定化妆修复）。
- **验证差异**：无。

#### F024 · medium · 并发 logout + 命令令会话复活："Logged out successfully" 同时写入全新有效令牌、请求持续成功；密码被删后复活会话到期转为静默硬认证错误
- **根因**：TokenManager 以纯读-改-写文件操作持久化每 profile 状态（saveState/clearState last-writer-wins，无跨进程互斥、无代数/epoch 校验）；`Dc3Client.logout`（client.ts:346-360）cancel→clearState→deletePasswordFromStore 与 request()/renewToken 的续期路径零协同——clearState 之后完成的续期持久化一枚 **cancel 之后铸造**的令牌，复活用户刚结束的会话；logout 的密码删除又剥夺该复活会话的续期能力。
- **复现要点**：并行 `auth logout` + `device list`（TTL 45s mock）→ **9/9 复活**：logout 打印 ok:true exit 0、list 拿数据 exit 0、tokens.json 存新铸令牌（每轮 fresh iat）；mock 序列 salt→generate→**cancel** 200 | salt→generate→list 200——存活令牌铸造于 cancel 之后（即使网关执行吊销也会接受这个全新会话）。二阶效应证实：连续批次中第 1 轮删密码后，后续轮静默续期失败（零 salt/generate）→ 401 exit 3 而 tokens.json 仍显示在册。
- **影响面**：登出意图被并发彻底击穿；之后转为无预警硬认证失败（本地"已登录"态与实际相反）。
- **根治修复**：序列化并版本化状态迁移——(1) tokens.json 全部变更包进排他 lockfile（带陈旧恢复）；(2) 每 profile 持久化 logout epoch：logout 以**单次写**完成 epoch 提升+清条目（先于或原子于 cancel），renewToken 铸造前捕获 epoch、generate 后复核，不匹配则丢弃新令牌并服务端 POST cancel；(3) logout 顺序定为 epoch-bump/clear → cancel → deletePassword。
- **守护测试**：renewToken 的 generate 与 saveState 间注入延迟，期间跑完 logout——断言 tokens.json 静止时无该 profile 条目、mock 记录了对登出后铸造令牌的 cancel、后续 `device list` 报 "Not logged in"（exit 3）而非返回数据；负向：无竞争的普通 logout 恰 cancel一枚并清态。
- **验证差异**：原报告"5/5 轮每轮新令牌"需密码跨轮可解析（其凭据 setup 存活或轮间重登）；连续批次中密码竞争使后续轮提前进入硬认证阶段——恰为报告自述的次级竞争按描述表现。

#### F025 · medium · 网关不可达时 `auth logout` exit 2，令牌与已存密码双双留在磁盘（网络失败无本地清理）
- **根因**：命令层（auth.ts:136-149）把有保证的本地清理（deletePasswordFromStore；client 内 clearState）严格排在易失败的远端 POST 之后且无 finally；client 层错误传播是文档化的预期行为，最低破坏层在命令 action——远端 cancel 抛出时本地状态处置既无保证也无告知。
- **复现要点**：登录后把 gateway 指到 `http://127.0.0.1:9`，`auth logout` → stderr `Error: Network request failed (.../token/cancel): fetch failed` exit 2、stdout 空（--format json 下也无 JSON），随后 tokens.json（活令牌）与 credentials.enc 均在盘上；对照 happy path：exit 0、mock 收到 cancel、两文件全清。
- **影响面**：用户明确要求销毁的令牌仍可被 `dc3 auth token` / 任何 tokens.json 读者使用，且保留是静默的（密码保留或属 fail-safe，令牌保留不是）。
- **根治修复**：logout action 使本地处置确定性且可见——cancel 包 try/catch，失败时明确警告"服务端未能吊销 + 本地保留哪些产物供重试"，和/或加 `--force` 跳过远端直接清本地；或 try/cancel/finally 清理 + 明示服务端令牌存活到过期（语义须拍板，静默保留即 bug）。
- **守护测试**：种子状态 + 网关指向不可达端口，调 logout action，断言 (a) 网络错误仍浮出 exit 2，(b) 所选语义的后置条件成立（文件已删，或警告点名保留物）——卫生保证不可静默回归。

### Medium — 补充验证轮新增（7 条）

#### F026 · medium · Windows 上 tokens.json/credentials.enc 承诺的 0600 无效：raw mode 为 0666 且无任何 ACL 加固
- **根因（最低破坏层）**：跨平台文件权限语义——Node `writeFile {mode:0o600}` 在 Windows 上无法表达按主体限制（libuv 仅映射 DOS read-only 属性；0600 写位置位故与 0666 不可区分），CLI 又无 win32 ACL 回退；credential-encrypted.ts:29-32 的安全注释记载了平台无法提供的"主防线（file permissions 0600）"，而 AES 密钥本身由可猜测的机器标识（hostname+username+arch）派生。
- **复现要点**：源码核对（token-manager.ts:72-76 / credential-encrypted.ts:90 写 mode 0o600）；隔离 home 双租户 `login --store encrypted` 后 node `fs.statSync` raw mode = 666（tokens.json / credentials.enc / config.json 三者同），Git Bash `stat -c %a` = 644（group/world 读位在场）；icacls 显示全部 ACE 自 profile 目录继承 '(I)'——CLI 未添加任何显式限制 ACE。
- **影响面**：注释承诺的 "primary defense" 在 Windows 上不存在，凭据/令牌文件对同机其他主体可读。
- **根治修复**：win32 写入后经 child_process 施加显式限制 ACL（`icacls <file> /inheritance:r /grant "${user}:F"`，仅当前用户），POSIX 保留 mode 0o600；同步修正安全注释停止在 Windows 上过度承诺。
- **守护测试**：登录 `--store encrypted` 后解析 icacls 输出断言状态文件无继承/world ACE（win32）、mode 0600（POSIX）——钉死注释所承诺的负向行为。

#### F027 · medium · 损坏状态文件被静默吞掉且下次写摧毁数据（credentials.enc / config.json / tokens.json 三种模式全复现）
- **根因（最低破坏层）**：三个状态文件无统一损坏协议——readEntries()（credential-encrypted.ts:72-74）与 TokenManager.load()（token-manager.ts:61-70）把 ENOENT 与损坏/不可解密内容混同（blanket catch→空态），每条 save 路径无条件从内存态整文件重写；config.json 有部分缓解（警告 + 损坏字节 .bak + 写警告）但 backupCorruptFile（config-manager.ts:92-101）复制的是**当前损坏字节**并覆盖既有 .bak——从不持有 last-good、无轮转；tokens.json / credentials.enc 完全无缓解。
- **复现要点**：credentials.enc 覆写 GARBAGE 后 `device list --format json` exit 0、stderr 0 字节；re-login 后文件仅剩新条目（其余 profile 密码被空 map 回写静默销毁）；续期子项代码级证实（client.ts:166-169 password null→silent return false）。config.json 截断 120 字节→警告 + `Profile "default" not found` exit 1，随后 `config set username admin` 触发第二次警告并以 in-memory defaults 重写——seeded 'prod' profile 永久消失。tokens.json 截断→`auth status` {"authenticated":false} exit 0 零警告无 .bak；re-login 后仅剩新 profile。
- **影响面**：一次损坏事件放大为跨 profile 数据销毁，全程零诊断。
- **根治修复**：三个 store 共享 quarantine-and-warn helper——对已存在文件 parse/decrypt 失败时 (1) stderr 警告点名文件与原因，(2) 任何后续写之前把损坏字节轮转到时间戳/<n> 后缀隔离副本，(3) credentials.enc 在未成功读取或显式恢复前拒绝覆写剩余条目（保留损坏密文使未损条目可恢复）。
- **守护测试**：逐文件回归——损坏后断言 stderr 警告、隔离副本存在、后续 login 保留其他 profile 条目（防湮灭负向守护）。

#### F028 · medium · README:91 记载 `point history [--count 100]` 而实现是 `--limit`（+`--cursor`）：`--count` 报 unknown option
- **根因（最低破坏层）**：README/命令契约漂移——旗标实现为 --limit（point.ts:86-88，另加 --cursor 分页）但 README 速查行从未更新；无 doc-vs-CLI 契约测试能抓住。
- **复现要点**：README 原文执行 `point history 456789 --device-id dev-1 --count 100` → exit 1、stderr `error: unknown option '--count'`；`--limit 50 --cursor X` → exit 0 且 mock 记录 GET /api/v3/data/point_value/history；`point history --help` 无 --count。
- **影响面**：把 README 当命令契约的 agent 照抄即失败。
- **根治修复**：README L91 改为 `dc3 point history <id> --device-id <did> [--limit 100] [--cursor <cursor>]`。
- **守护测试**：smoke test 解析 README 每条围栏命令行，断言其至少通过 commander 选项校验（exit 非 unknown-option/unknown-command）——覆盖整类 README 漂移（同守护亦覆盖其他 README-drift 发现）。

#### F030 · medium · 退出码 1 混杂用法错误、业务校验错误与 HTTP 404/409/500——不解析 stderr 前缀无法区分
- **根因（最低破坏层）**：错误分类学（errors.ts）把三类语义不同的失败折叠进一个码——commander 用法错误从无独立码；ApiError 携带 statusCode 但 handleFatalError（errors.ts:69-75）忽略之（仅 Auth/Network 特判），client.ts buildError（:378-389）只把 401/403 映射为 AuthError，404/409/500 全部落入 exit-1 默认。
- **复现要点**：success→0；缺必需项 / 未知选项 / 坏整数 / 未知命令→全 exit 1（stderr）；stdout-JSON 业务错误（point history 缺 --device-id）→exit 1 且 {ok:false,...} 在 stdout；连接拒绝（port 9）→exit 2；401→exit 3；自有 stub 返回 404/409/500→全部 exit 1（'Not found (404)' / 'HTTP 409' / 'Server error (500)'）。
- **影响面**：agent 无法仅凭退出码区分 usage / business / gateway 5xx，只能靠通道 + stderr 文本前缀猜。
- **根治修复**：扩展分类学——保留 2/3；为用法错误引入独立码（commander exitOverride/configureOutput 映射，如 64）、按 ApiError.statusCode 类别分码（4xx client vs 5xx server，如 4/5）；README 退出码表同步更新。
- **守护测试**：按类参数化回归（每状态一个 stub server + argv fixture）断言文档化退出码，契约无法静默回归。

#### F031 · medium · 未记载命令面 21 个子命令：provider/model/attachment（13 个，非 15）+ session get / alert type-distribution / config profile delete / group update/delete / label add-update-delete（8 个）
- **根因（最低破坏层）**：README 未随 commit 8444e0190（provider/model/attachment 组）与 56fffbbed（group/label 写路径）扩展；工作流无 doc-sync 步骤或 doc 覆盖守护——~21 个工作子命令对把 README 当命令契约的 agent 不可见。
- **复现要点**：源码逐处核对全部在册；README grep：provider/model/attachment 零命令记载、'session get' / 'type-distribution' / 'group update/delete' / 'label add/update/delete' 零命中。运行时（隔离 home + 自有 mock）：provider list→/api/v3/agentic/provider/list、model config-list→/agentic/model/config/list、attachment list、session get→/session/get_by_conversation_id、alert type-distribution→/dashboard/alert/type_distribution、group update/delete 与 label add/update/delete→/manager/{group,label}/{update,delete}（全部 exit 0 且 wire 记录）；`config profile delete nonexistentprof`→exit 1 'Profile not found'（命令存在且功能正常）。
- **影响面**：README 契约面缺 21 个可用子命令；agent 无法发现。
- **根治修复**：补 README 章节（provider 5 / model 6 / attachment 2 子命令 + 8 个散布子命令）；防复发：回归测试程序化注册完整命令树并断言每个注册路径（组+子命令）出现在 README.md——新子命令无文档即 CI 失败。
- **验证差异（含新发现标记 triage）**：三个 agentic 组实为 13 个子命令（5+6+2）非原报告的 15；验证期间另观察到 README 记载的空格形式 `--version <n>`（如 `device delete 99 --version 5`）触发根 .version 处理器——打印 "0.1.0"、exit 0、零请求（src/index.ts:47 根布尔 --version；与 F001 同根因的独立观测，佐证 F001）。

#### F032 · medium · 大 `--args`（1 MB）与长 `--name`（100 KB）不可用：OS argv 限制且无任何 `--args-file`/stdin 替代通道
- **根因（最低破坏层）**：全部 JSON-body 旗标均为 inline requiredOption 字符串（analytics.ts:53 / alert.ts:243 / tool.ts:65），无文件或 stdin 输入通道——超过 OS argv/命令行限制的 payload 不可表示（Windows CreateProcess 命令行上限 32,767 字符，天花板跨 shell、非 Git Bash 假象）。
- **复现要点**：1MB 合法 JSON（1,048,612 字节）作 --args → exit 126 `timeout: failed to run command 'node': Argument list too long`、零请求；100KB --name 同；对照 28,694 字节 --args → exit 0 且 rawBodyBytes=28694 上线（解析成本可忽略，传输/argv 是瓶颈）。
- **影响面**：agent 生成的大 analytics/alert payload 无任何缓解路径。
- **根治修复**：analytics run / alert bulk-confirm / tools call 增加 `--args-file <path>`（'-' 表 stdin）：读文件 JSON.parse，沿用既有 invalid-JSON exit-1 路径；README 记载旗标与 inline 体积上限。
- **守护测试**：>200KB 合法 JSON fixture 经 --args-file（及 '-' 管道 stdin）调用断言 exit 0 且 mock 收到完整 body；负向：--args-file 坏 JSON 仍 exit 1 结构化消息。

#### F033 · medium · 文档化优先级链（keychain→encrypted→env→prompt）未实现：只查配置的单个 store；DC3_PASSWORD 登录时从不读取，CI 无头登录不可能
- **根因（最低破坏层）**：credential-store.ts:20 头注释承诺逐级 fallback，但 selectStore（:57-76）只返回一个 store、resolvePassword（:84-95）只查它；auth login 的输入解析只认 -p/--password 与交互 prompt（'options.password || passwordPrompt(...)'）——env 永不作为输入源。两层断裂：resolvePassword 缺链、login 缺 env 输入源。
- **复现要点**：场景 A（keychain 默认 + DC3_PASSWORD 导出 + 未登录）`device list` → 401 exit 3，mock 仅一次未认证请求——env fallback 从未触发。场景 B（DC3_PASSWORD 导出 + `auth login --store env` + stdin /dev/null）→ exit 0 且 stdout/stderr/wire 全零，readline 密码 prompt 挂在 EOF、事件循环排空 exit 0，随后 auth status {"authenticated":false}——README L51 契约（'Read password from DC3_PASSWORD env'）不可满足。
- **影响面**：文档承诺的 CI/无头登录路径完全失效（与 F004 的 EOF 假成功复合）。
- **根治修复**：resolvePassword 实现文档链——先查配置 store，再逐级 keychain→encrypted→env（env 以 DC3_PASSWORD 已设置为门），返回首个非空密码；auth login 在 prompt 前先查 DC3_PASSWORD；若另选优先级则同步头注释与 README——注释必须与代码一致。
- **守护测试**：(1) DC3_PASSWORD + keychain 不可用 + 未登录 → device list 经 env 自动登录/续期成功；(2) DC3_PASSWORD + `auth login --store env` 闭 stdin 无 -p → 无头 exit 0 且 state 有 token。

### Low — 补充验证轮新增（24 条：CONFIRMED 23 + PARTIAL 1）

#### F029 · low · `create` 兼容别名在全部 8 组零弃用警告——承诺的一个版本生命周期无任何强制或预告
- **根因（最低破坏层）**：弃用生命周期仅存在于文档散文——无运行时标记（commander preAction 无警告、help 无 DEPRECATED 标签、无 release 计数器），承诺的 one-release 期限既不可强制也不被预告。
- **复现要点**：8 组注册 .alias('create')（command.ts:71 / event.ts:84 / driver.ts:62 / device.ts:95 / point.ts:139 / label.ts:58 / profile.ts:93 / group.ts:59）；grep -i 'deprecat' 全 src 零命中；README:28-29 仍承诺 'create remains available as a deprecated compat alias for one release'；执行 `device create ...` / `label create ...` → exit 0、stderr 0 字节；help 把 alias 渲染为一等公民 'add|create'；wire 上 add 与 create 请求逐字节相同。
- **影响面**：alias 被移除前用户/agent 零信号。
- **根治修复**：program 级 preAction hook——经兼容 alias 进入时 stderr 每调用警告一次；help 渲染为 'add (deprecated alias: create)'。
- **守护测试**：`device create` 断言 stderr 弃用警告点名移除；alias 仍路由到与 add 相同 action 的断言。

#### F034 · low · JSON-body 旗标接受非对象 JSON：`command call --params` / `analytics run --args` / `alert bulk-confirm --args` 原样转发数组/null/标量
- **根因（最低破坏层）**：TS 注解 Record<string,unknown> 是运行时未检查的断言——JSON.parse 接受任意 JSON 值，无层级校验对象性即放入 wire 字段（语法查了、形状没查）。
- **复现要点**：`command call --params [1,2,3]` → body paramValues=[1,2,3]；null→null；42→42；"abc"→"abc"（全 exit 0）；analytics run / alert bulk-confirm --args 同型原样上线。后端契约核实：CommandCallVO.java:62 `private Map<String, String> paramValues;`——数组/null/标量服务端反序列化 400，而非走 CLI 自己干净的 exit-1 'Invalid JSON' 路径。
- **影响面**：形状错误的 payload 把本可客户端拦截的错误推给服务端晦涩 400。
- **根治修复**：JSON.parse 后校验 `typeof === 'object' && !== null && !Array.isArray`，失败走既有结构化 exit-1（'--params must be a JSON object'）；对象契约站点（command.ts:132-140、analytics.ts:65-71、alert.ts:249-251、tool.ts:70-74）同样处理。
- **守护测试**：每旗标 [1,2]/null/42/"abc" 断言 exit 1 + 结构化消息 + 零 wire 请求；正对照对象仍 round-trip。

#### F035 · low · parseNonNegativeInteger 经 Number() 过宽：`--limit=` 静默变 0（空分页），' 5'/'0x10'/'5.0'/'1e2' 全部接受
- **根因（最低破坏层）**：utils/manager.ts:27-33 守卫只查 Number() 的**结果**（isSafeInteger/非负）不查词法形式——Number() 全量 JS 强制转换（空白裁剪、hex/指数/小数字面量、空串→0），错误文案 'must be a non-negative safe integer' 暗示的词法严格性代码并未执行。
- **复现要点**：`device list --limit ''` → {"offset":0,"limit":0} exit 0（静默分页为零）；' 5'→5、'0x10'→16、'5.0'→5、'1e2'→100（全 exit 0 上线）；`device update 42 --version=0x10`→wire version=16、`--version=`（空）→version 0 发送；对照 abc/NaN/Infinity/-5/9007199254740993/1e999 正确 exit 1。同一 parser 支撑全部 --offset/--limit/--version。
- **影响面**：手误的空值静默变成合法 0；不同进制/记数法穿透。
- **根治修复**：词法先行——`/^\d+$/` 不匹配即 throw InvalidArgumentError，再 Number(value) 走既有 isSafeInteger/2^53 守卫（拒绝 ''、' 5'、'0x10'、'5.0'、'1e2'，保留 '0'/'5'/'100'）。
- **守护测试**：vitest 表驱动覆盖两个 parser 的精确接受/拒绝矩阵。

#### F036 · low · `settings.color` 接受任意垃圾静默存为 false 仍报成功；settings.* 的 set/get 键不对称
- **根因（最低破坏层）**：config.ts:104-105 color setter `value === 'true'` 强转无布尔校验——任意非 'true' 串静默变 false 而命令 echo 原值成功；键规范化不对称——setter 只 switch 前缀键（settings.*）、getter（:179-187）默认分支裸查 settings 对象不剥离可选前缀（与 auth.tenant/tenant 双向接受恰成精确反转）。
- **复现要点**：`config set settings.color maybe` → {ok:true,'settings.color set to maybe'} 落盘 "color": false；TRUE/ture 同；`set settings.retry_count 3` ok 而 `get settings.retry_count` → 'Unknown config key' exit 1、`get retry_count` → 3 exit 0。附注：`config get` 需先配置好 profile（username+gateway），否则先撞 'No profile configured' 守卫。
- **影响面**：布尔设置静默错误持久化；同一键 setter 与 getter 各认一种拼法。
- **根治修复**：布尔 settings 像 output_format 一样只收 'true'|'false' 否则结构化 exit 1；双侧对称剥离可选 `settings.` 前缀（镜像 tenant/auth.tenant 双接受）。
- **守护测试**：(a) set settings.color maybe → exit 1 且落盘值不变；(b) get settings.retry_count 与 get retry_count 返回同值。

#### F037 · low · 登录后改 config tenant 静默破坏自动续期；兄弟场景验证：过期令牌 + 无匹配存储密码时 logout exit 3 状态卡死
- **根因（最低破坏层）**：续期/登出的密码寻键从可变 profile config（client.ts:66-67/:95 传 profile.tenant/profile.username；renewToken:166 按 `${username}@${tenant}` 解析）派生，而非持久化 token state 自带的 tenant/username（TokenManager 已存储）——任何 login 后 `config set tenant` 静默孤立加密密码，renewal 无警告返回 false、裸 401 浮出。次级：logout() 仅在 cancel 请求成功后清本地态（clearState 不可达），cancel 401（过期 + 密码不可解析）时 profile 永久卡在已登录。
- **复现要点**：A（TTL 1s mock）login tenantA → set tenant tenantB → 过期后 `device list` → 'token expired' exit 3，mock 零续期调用（store 持 admin@tenantA 而寻键 admin@tenantB 落空）；B 反事实（不改 tenant）同过期 → salt+generate 正常、exit 0——隔离 config 漂移为因；C login tenantA → 切换 → 过期 → `auth logout` → 401 exit 3 且 tokens.json 带过期态留盘（cancel 携带过期 token 被 401）；对照恢复 tenantA 后 logout exit 0 且 tokens.json 删除。
- **影响面**：合法的租户切换静默杀死续期；登出意图被服务端 401 卡死。
- **根治修复**：(1) request()/renewToken 优先按 token state 的 tenant/username 寻键（回退 profile 值），profile 身份偏离时发警告点名 mismatch（如 'password stored for admin@tenantA but profile says tenantB; renewal skipped'）；(2) logout 在 cancel 失败时也清本地态（或文档化 --force 逃生门）。
- **守护测试**：tenant 切换 + 过期 → 警告出现且退出语义钉死；过期 + 缺密码 logout → 本地态已清。

#### F038 · low · delete 无条件丢弃 2xx 响应体（200-with-body 打印为空，exit 0）
- **根因（最低破坏层）**：utils/manager.ts:79-87 deleteManagerResource 把 delete 响应定型 void、按设计丢弃已解析 body；client.ts request() 仅对 204 特判（:123-125），200-with-body 被解析后扔掉且无任何代码路径能呈现。
- **复现要点**：mock DELETE 200 + JSON envelope：`device delete 55 --version=7` → exit 0、stdout 0 字节、stderr 0 字节，parsed 200 body 静默丢失（mock.log 确认 200）。
- **影响面**：网关附在 2xx delete 上的任何 payload（警告 envelope、审计信息）丢失，成功仅凭状态码推断；对 204 契约正确，属意外正确。
- **根治修复**：契约显式化——或打印非空 2xx delete body（printAndExit，空/204 保持静默），或保 void 但以测试钉死 'DELETE 200-with-body → exit 0 无输出' 并在 deleteManagerRoot 注释声明抑制是有意的。
- **守护测试**：无论选哪边，回归测试即强制函数。

#### F039 · low · point add/update 以 JSON 字符串发送 baseValue/multiple 而后端 PointVO 声明 BigDecimal（valueDecimal 是数值）
- **根因（最低破坏层）**：point.ts:146-147 字符串默认 '0'/'1'，body 赋值（:158-159）转发原始选项字符串——--base-value/--multiple 无数值解析/校验即上线（对照 --value-decimal 经 parseNonNegativeInteger 以数字发送）。
- **复现要点**：`point add --name pt2 --profile-id 100 --type INT --rw READ_WRITE --value-decimal 5 --base-value 2 --multiple 0.5 --unit C` → wire {"valueDecimal":5,"baseValue":"2","multiple":"0.5"}；默认变体 baseValue:"0"、multiple:"1"；后端 PointVO.java L109/L118 `private BigDecimal baseValue/multiple;`——wire 类型与 VO 不一致，今日能跑纯因 Jackson 默认 string→BigDecimal 强转。
- **影响面**：更严格解析器或非 Java 消费者即断；point update 转发同样字符串字段。
- **根治修复**：--base-value/--multiple 走校验数值解析（验证后再 string→Number 保精度）并以 JSON number 发送对齐 BigDecimal 字段；update 路径同步。
- **守护测试**：断言 wire body typeof baseValue === 'number' && typeof multiple === 'number'，非数值客户端 exit 1。

#### F040 · low · JSON-RPC 请求 id 用 Date.now()——毫秒级分辨率碰撞
- **根因（最低破坏层）**：src/core/mcp.ts:50 `id: Date.now()`——id 生成用墙钟毫秒而非 per-client 唯一计数器，同一毫秒内两个请求按 id 不可区分。
- **复现要点**：临时 vitest（仿 test/mcp.test.ts mocks，vi.useFakeTimers() 冻结 Date.now，跑后删除，2 passed）：顺序两次 rpc 调用产生相同 id；Promise.all 并发在飞请求同样碰撞。当前影响低——McpClient 顺序 await 且忽略响应 id——但回显 id 做关联的服务器或未来并发多路复用即得歧义响应。
- **根治修复**：模块级单调计数器（`let nextId = 1; id: nextId++`）或 crypto.randomUUID()；可选在接受结果前校验响应 id 匹配请求 id。
- **守护测试**：冻结 fake timers 下两次调用 id 必不同，且客户端能把响应匹配回各自请求。

#### F041 · low · YAML 输出键原样发射：含 ': ' 的键产出不可解析 YAML
- **根因（最低破坏层）**：format.ts:99-101 `${k}: ${JSON.stringify(v)}`——手搓序列化只对值 JSON 引号、键原样内插，顶层键含 YAML 语义字符（'colon space'、'#'、前导 '- '）即产出无 YAML 解析器接受的输出。
- **复现要点**：mock 响应顶层键 "conn: status" → `device list --format yaml` exit 0 输出行 `conn: status: "ok"`；js-yaml（repo node_modules）load 捕获输出抛 'bad indentation of a mapping entry'；--format json 对照正常。细化：嵌套用户键（config list 的 profile 名）作为值被 stringify 保持有效——触发需记录**顶层**键含冒号空格，server 驱动记录可携带（metadata/attribute maps）；核心缺陷与机器消费者影响真实、频率低。
- **根治修复**：经正规 dumper 序列化（js-yaml 已在 node_modules），或手搓输出必须保留时对含 YAML 指示符的键套用与值相同的 JSON.stringify 引号规则。
- **守护测试**：单测 formatOutput({'a: b':1},'yaml') round-trip 经 yaml.load；CLI 级 `--format yaml` 读命令输出可被 js-yaml 解析的回归。

#### F042 · low · 空 body 与非 JSON 响应的退化错误面：200 空 body 崩 JSON 解析出晦涩 'Unexpected end of JSON input'；200 text/plain → 裸 SyntaxError；500 空 body → 'Server error (500): ' 细节为空
- **根因（最低破坏层）**：Dc3Client.request() 假定一切非 204 响应为 JSON（client.ts:127 及 :114 的 401-retry 路径同无守卫），buildError（:362-390）假定 body 存在/可解析——空与非 JSON 场景泄漏传输层 SyntaxError、产出无上下文（无状态码/路径）消息。
- **复现要点**：(1) 200 空 body → stderr 'Error: Unexpected end of JSON input' exit 1；(2) 200 text/plain 'plain ok' → 'Unexpected token 'p', "plain ok" is not valid JSON' exit 1；(3) 500 空 body → 'Error: Server error (500): '（冒号后字节级为空）exit 1。
- **影响面**：网关侧最需要诊断的退化场景恰好产出最无信息量的错误。
- **根治修复**：body 一次性读 text——2xx 空返 undefined（视同 204 无内容）；2xx 非空但 JSON.parse 失败抛携带 status/Content-Type/前 ~120 字符的 ApiError；buildError 空文本以 '(empty body)' 占位，错误串永不以悬空冒号结尾。
- **守护测试**：stub server 三场景（200-empty / 200-text/plain / 500-empty）断言消息内容与退出码的集成测试。

#### F043 · low · provider/model update 把非数组 list 响应误诊为 'not found'；读-改-写还把每个额外服务端字段原样回传
- **根因（最低破坏层）**：两层——(a) update 以 `Array.isArray(x) ? x.find(...) : null`（model.ts:131-136 / provider.ts:91-96）从 list 响应解析实体，无 envelope 解包且不区分"响应形状意外"与"id 缺席"；(b) update body 为 {...current, ...changes, id}（model.ts:150-154 / provider.ts:105-109），重序列化 list 里服务端碰巧包含的一切字段而非白名单字段集。
- **复现要点**：envelope 形 list（{ok:true,data:[...],total:1}）→ `model update 5 --label zzz` 输出 'Model config 5 not found' exit 1 且 wire 零 update 请求；bare-array 含 secretExtraField/creatorId/operateTime 时 update POST body 原样回传全部字段。live 只读核实：当前网关 model config-list / provider list 返回 bare array 带审计字段（createTime/creatorId/creatorName/lastCheckLatencyMs/lastCheckStatus/lastCheckTime）——字段回传今日活跃，误诊为潜伏（网关一旦像 device list 那样包 envelope 即触发）。
- **影响面**：审计/潜在敏感字段被静默回写；网关响应形状演进即把真实条目误报为不存在。
- **根治修复**：解包常见 envelope（{data:[...]}）再 find，形状无法识别时发独立（更响亮的）消息；update body 改用与 add 相同的字段白名单，server-only/审计/secret 命名字段永不回传。
- **守护测试**：(i) envelope 形 list 断言 update 继续或形状专属失败消息；(ii) list 条目带额外字段断言该字段不在 POST 的 update body。

#### F044 · low · PARTIAL · 无 'attachment delete' 子命令（仅 upload/list）——但后端同样不暴露 delete 端点，缺口是平台级而非 CLI 级
- **PARTIAL 差异**：CLI 观测在当前 dist 原样复现——`attachment --help` 仅 'upload [options] <file>' 与 'list [options]'；`attachment delete 1` → "error: unknown command 'delete'"。但重读后端 AttachmentController（dc3-common-agentic .../AttachmentController.java:83-120）发现其本身只有 POST /upload 与 GET /list——网关无 delete 路由可供 CLI 包装。"清理工作流不可达"成立，缺陷归属从 CLI 覆盖缺口上移至平台 API 面，CLI 忠实镜像了可用路由。
- **根因（最低破坏层）**：AttachmentController 无 delete 端点——资源面缺乏服务端删除，CLI 命令 1:1 生成自可用路由。
- **复现要点**：见 PARTIAL 差异（CLI 侧 unknown command + 后端路由面核对）。
- **根治修复**：后端先加 delete 路由（`@PostMapping("/delete")` 返回 Mono<Void>，循 session/model delete 模式）+ service 层删除，再在 attachment.ts 镜像 `attachment delete <id>`（del + printAndExit(undefined)）。
- **守护测试**：后端路由命名契约测试（ApiRouteNamingContractTest 模式）+ CLI 测试断言子命令向正确路径发请求。

#### F045 · low · `session delete` 与 `action confirm/reject` 零确认摩擦即执行——与文档化 TTY 确认通道设计相悖
- **根因（最低破坏层）**：破坏性 agentic 操作（session.ts:90-101 delete、:113-136 act()）实现时未加确认门，尽管 session.ts 头注释（22-26 行）自引设计文档的 'CLI TTY confirmation channel' 且 confirm() helper 存在、config reset 已在用（config.ts:251-252 / prompt.ts:61）——属命令 action 的遗漏而非能力缺失。
- **复现要点**：闭 stdin `printf '' | dc3 session delete conv-1` → 729ms exit 0、两流均无 prompt 文本、DELETE /api/v3/agentic/session/delete?conversation_id=conv-1 立即上线；`printf '' | dc3 action confirm action-1` → 1064ms exit 0、POST /action/confirm?action_id=action-1 立即上线（wire log 捕获）。非交互 stdin 本会挂起或拒绝任何 prompt——证明对 agent/脚本零摩擦。
- **影响面**：高风险审批环（agentic 会话销毁 / action 批准）无任何护栏。
- **根治修复**：session delete 与 action confirm/reject 用既有 confirm() 加门并提供显式 --yes（脚本/CI/MCP agent 仍可非交互继续）；确认拒绝时零请求退出。
- **守护测试**：(a) 闭 stdin 无 --yes → 零 HTTP 请求 + 'Cancelled' 退出契约；(b) --yes → 执行请求——钉死负向防门静默回归。

#### F046 · low · device import 的 request JSON part 在 Content-Disposition 携带 filename="blob"（undici 对无文件名 Blob 的默认）
- **根因（最低破坏层）**：CLI 的 FormData 构造（device.ts:203-214 `form.append('request', new Blob([...], {type:'application/json'}))` 无第三 filename 参数）——按 fetch 规范 multipart 序列化（undici 实现），任何 Blob 类型 part 即使调用方意在纯元数据字段也获默认 filename("blob")；以 Blob append JSON 必然在线上泄漏 filename，CLI 从未钉死 README L72 所称 "canonical multipart request" 的 part 形状。
- **复现要点**：扩展 mock（共享 harness 副本加 multipart part dumper）捕获 POST /api/v3/manager/device/import：part 1 头 = 'Content-Disposition: form-data; name="request"; filename="blob"' + 'Content-Type: application/json'（35 字节 {"driverId":"10","profileId":"100"}）；part 2 = name="file"; filename="dev.xlsx"；CLI exit 0（--no-wait）。dist（1554-1566 行，构建晚于 src）与 src 一致。
- **影响面**：canonical wire 契约依赖序列化器默认值——undici 升级或严格后端 @RequestPart 解析即可能破裂。
- **根治修复**：wire 契约显式化——手搓 multipart body 使 request part 为 'Content-Disposition: form-data; name="request"'（无 filename）且保留 Content-Type: application/json；或验证后端 @RequestPart 转换器接受后改 plain string 字段。
- **守护测试**：golden-wire 测试（mock 或 undici FormData 检查）断言 request part 的精确 Content-Disposition（filename 缺席）与 Content-Type——序列化器升级无法静默改变 canonical 契约。

#### F047 · low · 导入文件不存在时抛非结构化 ENOENT（回显解析后的绝对本地路径），而兄弟校验发结构化 JSON
- **根因（最低破坏层）**：import action 自身输入校验——device.ts:197 裸 `const content = await readFile(file);` 无 try/catch，夹在两个结构化 printAndExit 检查（:193-195 扩展名、:198-200 空文件）之间；ENOENT 逃逸到通用 fatal handler（errors.ts:64-65 'Error: <raw message>'）——同类用户输入错误得到两种输出契约，且泄漏解析后绝对路径。
- **复现要点**：`device import <ISO>/nope-missing.xlsx --driver-id 10 --profile-id 100 --format json` → stdout 0 字节、stderr 'Error: ENOENT: no such file or directory, open C:\Users\pnoker\AppData\Local\Temp\...\nope-missing.xlsx'、exit 1。
- **影响面**：退出码一致（1）但错误形状与路径泄漏不一致；stdout-JSON-only agent 拿到空串。
- **根治修复**：readFile 包 try/catch（或 stat 预检），发 printAndExit({ok:false, message:'Import file not found: <用户原样路径>'}, format, 1) 对齐兄弟检查。
- **守护测试**：缺失路径 + --format json 断言 (a) stdout 可 parse 且 ok:false，(b) stderr 空，(c) 消息不含解析后绝对目录，(d) exit 1——整个校验族的形状平价。

#### F048 · low · 裸 `dc3` 把 help 打到 STDERR 且 exit 1（期待 stdout help + exit 0 的 agent 会误读）
- **根因（最低破坏层）**：CLI 入口与非交互调用者的契约——src/index.ts 未注册根 action handler 也未覆写 help 行为，commander "有子命令但无 action" 的默认路径经 helpError 发 help（stderr + exitCode 1）；裸调用的通道/退出契约是框架意外而非钉死的决定。
- **复现要点**：裸 `node dist/index.js` → stdout 0 字节、stderr 1795 字节以 'Usage: dc3 [options] [command]' 开头、exit 1；缓解面核实：`dc3 --help` 与 `dc3 help` → stdout 同 1795 字节、exit 0。
- **影响面**：以裸调用发现命令面的 agent 把"帮助"当"错误"。
- **根治修复**：二选一并钉死——(a) 裸调用等同 help：`program.action(() => { program.outputHelp(); process.exitCode = 0; })`（stdout）；(b) 保持 commander 默认（git 同立场）并在 README AI-agent/退出码节文档化（'bare dc3 → help on stderr, exit 1; 用 dc3 --help 取 stdout+0'）。
- **守护测试**：spawn 无参数断言所选通道 + 退出码；另断言 --help/help 给 stdout + exit 0——commander 升级无法静默移动契约。

#### F049 · low · auth login 成功 JSON 的 expires_at 用 locale 格式（'2026/10/6 12:02:09'）而 auth status 用 ISO-8601——跨机器/locale 不可预测
- **根因（最低破坏层）**：auth 命令呈现格式化——机器可读输出字段无共享日期渲染规则：login（auth.ts:78 及 OAuth 路径 :124-125）在人面向 toLocaleString() 上用了它，status 用 toISOString()（:169）——expires_at 字段无跨命令稳定契约。
- **复现要点**：login stdout 含 "expires_at": "2026/10/6 12:02:09"（message 同文本）；紧接同一 token 的 `auth status --format json` 显示 "expires_at": "2026-10-06T04:02:09.000Z"——同一瞬间、两种格式、其一依赖机器/locale。
- **影响面**：机器消费者无法稳定解析 login 输出的时间字段。
- **根治修复**：一切机器可读 expires_at（及 JSON/YAML 输出中其他时间戳字段）经一个共享 helper 出 ISO-8601；locale 格式仅保留给人读文本/表格。
- **守护测试**：mock 登录后断言 login 输出 expires_at 匹配 ISO 格式且与随后 auth status 解析为同一瞬间。

#### F050 · low · README MCP 示例用 'transport' 键而 Claude Code .mcp.json 要求 'type'
- **根因（最低破坏层）**：README 文档漂移——示例作者把 `claude mcp add --transport <t>` CLI 旗标名当作 .mcp.json schema 键，配置文件 schema 要求 `type`（stdio|sse|http|ws）；无 doc lint 保持 README MCP 片段 schema 合法。
- **复现要点**：Claude Code 2.1.28 实测（隔离 CLAUDE_CONFIG_DIR + 临时项目目录，真实用户配置未动）：(1) README:284-294 原样形状的 .mcp.json 被拒——`claude mcp list` 诊断 '[Warning] ... has a "url" but no "type"; add "type": "http" (or "sse" / "ws")'，服务器不可用；(2) `claude mcp add --transport http dc3 http://localhost:8000/mcp` 持久化 {"type":"http","url":...}——文件 schema 键确为 type。原 finding 的 'probable' 运行时半边升级为实测（当前 Claude Code 至少给出带修复提示的显式 skipped 诊断而非静默失败）。
- **影响面**：README 粘贴即坏。
- **根治修复**：README L289 改 "type": "http"（一词修复）。
- **守护测试**：docs-lint 提取 README 每个 .mcp.json 片段并按 mcpServers schema 校验（有 url 必须有 type；transport 等未知键告警）——MCP 配置示例无法再漂移。

#### F051 · low · README 记载的 'auth login --oauth' 被 --help 隐藏而兄弟选项描述引用它
- **根因（最低破坏层）**：help 面/文档契约漂移——选项经 commander .hideHelp()（auth.ts:41-46）刻意隐藏（代码注释 auth.ts:93-95：等网关侧 RS58 verification 启用），但 README.md:223 公开要求 agent 使用该旗标且三个兄弟选项描述（--client-id/--client-secret/--scope 的 '(with --oauth)'）引用它——无自洽发现路径（照 README 走的 agent 无法经 --help 确认旗标存在）。
- **复现要点**：`node dist/index.js auth login --help` exit 0，Options 块无 --oauth 条目——仅 3 处 'oauth' 命中在兄弟选项描述文本内。
- **影响面**：文档要求的登录形态在帮助面上不存在。
- **根治修复**：对齐两面——去掉 .hideHelp()（旗标功能可用且 README 记载，倾向此项），或保持隐藏并在 README:223 注明刻意不列出。
- **守护测试**：`auth login --help` 断言 '--oauth' 出现在 Options 块（今日失败）。

#### F052 · low · `--format ''`（空串）被静默当作未设置（exit 0 默认格式）而非像其他非法值一样拒绝
- **根因（最低破坏层）**：detectFormat 的 "显式提供" 谓词把 '' 当未提供（format.ts:118 `explicit !== undefined && explicit !== ''`），与其自身 fail-fast 注释矛盾——`--format=` 是显式请求却被静默强转默认格式；context.ts:53 全局 --format 的镜像守卫同病。
- **复现要点**：`device list --format ''`（已登录隔离 home + mock）→ exit 0、默认 JSON 输出、stderr 空、POST /manager/device/list 200；对照同树 `--format xml` → exit 1 "error: unknown format 'xml' (expected json, table, or yaml)"。
- **影响面**：显式但空的格式请求静默降级——与全部其他不支持值的拒绝语义不一致。
- **根治修复**：src/utils/format.ts detectFormat 与 src/core/context.ts applyGlobalOptions 的守卫改 `explicit !== undefined`——未知格式拒绝覆盖空串。
- **守护测试**：`--format ''` 断言 exit 1 'unknown format'（今日 exit 0 失败）。

#### F053 · low · argv 卫生：多余位置参数静默丢弃（`device get 1 2` 只作用 id 1）、空串 id 原样上线、空/空白 `--profile` 静默回退默认 profile
- **根因（最低破坏层）**：三个独立缺口——(a) commander 程序默认 allowExcessArguments=true，多余 positional 无诊断蒸发；(b) 资源 id positional 无非空校验直送 wire（device.ts:86-88 encodeURIComponent 转发，get/update/delete 均然）；(c) context.ts:47 `opts.profile && opts.profile.trim()` 把 ''/空白静默忽略回退默认 profile，与不存在名路径（exit 1）不一致。
- **复现要点**：(1) `device get 1 2 --format json` → exit 0、恰一次 GET ?id=1（多余段静默丢弃）；(2) `device list frobnicate` → exit 0 正常列表；(3) `device get ''` → exit 0、GET .../get_by_id?id=（空值原样上线）；(4) `--profile ''` / `--profile '   '` device list → exit 0 静默由 default profile 服务；对照 `--profile nonexistent` → exit 1 'Profile not found'、`device 2` → exit 1 unknown command。
- **影响面**：agent 的拼写/转义错误被静默吞掉，请求作用于错误（或空）目标。
- **根治修复**：三层各修——(a) program `.allowExcessArguments(false)`（或公共参数计数断言 helper）使多余 positional 产生 commander 干脆错误；(b) 共享 manager get/update/delete 包装做非空 id 校验、任何请求前 exit 1；(c) applyGlobalOptions 对已提供但空/空白的 --profile exit 1 而非回退默认。
- **守护测试**：`device get 1 2` 非零退出或显式诊断；`device get ''` exit 1 且零 wire（断言 mock log 空）；`--profile '  '` exit 1——今日全部失败。

#### F054 · low · `config set gateway` 接受 javascript:/ftp:// URL、base path 与 ?query 后缀（zod .url() 只查可解析性），失败推迟到请求时
- **根因（最低破坏层）**：ProfileConfigSchema.gateway（config-manager.ts:26 `z.string().url()`）只验证 URL 可解析性——任意 scheme 通过、无 http/https 细化、无 pathname/query 组建拒绝；normalizeGateway（http.ts:31-33）仅剥尾斜杠。误配置在 `config set` 时被急切接受，首次请求才以混乱迟到失败（exit 2 'fetch failed' 或 404）浮出。
- **复现要点**：`config set gateway 'javascript:alert(1)'` / `'ftp://127.0.0.1:1'` / `'http://127.0.0.1:52478/base'` / `'http://127.0.0.1:52478/?x=1'` → 全部 {ok:true} exit 0 原样落盘；使用时分别 exit 2（fetch failed，javascript:/ftp:）与 exit 1（404，base path）；对照 'not-a-url' 在 set 时被 zod 正确拒绝且不落盘。
- **影响面**：坏配置的报错时机与形态都与根因脱钩，用户排查困难。
- **根治修复**：schema 收紧——`z.string().url().regex(/^https?:\/\//u, 'gateway must use http or https').refine(u => { const p = new URL(u); return p.pathname === '/' && p.search === ''; }, 'gateway must be a bare origin (no path/query)')`（剥尾斜杠后 refine）。
- **守护测试**：javascript:alert(1)（及 ftp:/带路径形式）exit 1 结构化校验错误；http://host:8000 仍成功。

#### F055 · low · 登录/续期把明文密码放 POST body 走任意配置的 gateway URL（http 静默接受，默认 gateway 即 http://localhost:8000）
- **根因（最低破坏层）**：传输加固缺口叠加 by-design 协议——登录/续期契约要求密码在请求体（client.ts:312-321 忠实实现 salt+password 协议，CLI 侧缺陷在缺传输加固而非 body 形状），但 CLI 对所配 URL 无 scheme 守卫：非 localhost 明文 http 网关零警告、默认 gateway 为 http://localhost:8000（config-manager.ts:26 及 dist 均然，src 全文 grep 无 http/https 检查或 insecure-transport 警告）——远程部署静默明文发送凭据。
- **复现要点**：对 127.0.0.1 mock 登录，mock.log：{"method":"POST","path":"/api/v3/auth/token/generate","body":{...,"password":"Plain#Pass9"},"status":200}——密码线上明文，且登录全程零 stderr 警告；预配置前登录尝试确实命中 http://localhost:8000。
- **影响面**：远程 http 部署下凭据裸奔。
- **根治修复**：client 登录/续期路径在 resolved gateway 为 http:// 且非 localhost/127.0.0.1 时 stderr 一次性警告（'password will be sent in cleartext over http'）；profile 默认改 https:// URL（或首配要求显式 scheme）。完全修复在平台侧（challenge/response 或 TLS-only），CLI 侧警告/默认值即根因对齐的本地修复。
- **守护测试**：指向 http:// LAN 地址断言警告出现、localhost 无警告。

#### F056 · low · `dc3 help <nonexistent>` exit 1 向 stderr 倾倒整页 37 行 help 且无任何错误信息（对照 `dc3 <nonexistent>` 的干脆单行）
- **根因（最低破坏层）**：commander 隐式 help-command fallback——对未知 `help <target>`，commander 12（command.js:1278-1294）把目标作为子命令带 --help 重派发，终结于 help({error:true})（stderr 全量 help dump）而不产生诊断；dc3-cli 未注册自定义 help 命令或 unknown-command hook 恢复诊断。
- **复现要点**：`node dist/index.js help nonexistent` → exit 1、stdout 0 行、stderr 37 行直接以 'Usage: dc3 [options] [command]' 开头、grep 'unknown command' 零命中；对照 `node dist/index.js nonexistent` → 单行 stderr "error: unknown command 'nonexistent'"。
- **影响面**：help 路径的打字错误换来 37 行帮助墙且无错误线索。
- **根治修复**：注册显式 help 命令（`program.command('help [command...]')` 逐段走 .commands 解析目标；未解析时 stderr `error: unknown command '<name>'` exit 1，否则 target.help()）替换隐式 fallback。
- **守护测试**：`help nonexistent` 断言 stderr 为含 'unknown command' 的单行诊断且无 'Usage:' dump；`help device` 仍打印 device help——诊断用例今日失败。

---

## 4. 已反驳与未验证（透明度清单）

**REFUTED：0 条。** 对全部 CONFIRMED 项的反驳尝试（工作树已修 / dist 陈旧 / 文档化行为 / 测试覆盖 / 平台特定 / 生态惯例）均告失败，细节在各条 verifyEvidence。

**UNVERIFIABLE：0 条（原 31 条积压已清零）。** 原始统一对抗验证轮完成前 25 条后配额耗尽（"verify cap reached"），F026-F056 共 31 条曾标记 UNVERIFIABLE（均由发现小队以已执行过的 repro 报告，confidence 多为 confirmed）。**后续补充对抗验证轮已对全部 31 条完成独立复核**（当前 dist + src 重放原始 repro + 四角度反驳尝试）：**30 条 CONFIRMED**（F026-F033 medium 7 条、F029/F034-F043/F045-F056 low 23 条）、**1 条 PARTIAL**（F044——CLI 观测原样复现，但后端 AttachmentController 本身无 delete 端点，缺陷归属上移至平台 API 面）。全部条目与验证证据已并入第 3 节"补充验证轮"小节。至此 56 条发现全部有最终判定：**CONFIRMED 55 + PARTIAL 1 + REFUTED 0 + UNVERIFIABLE 0**。


---

## 5. 性能数据（从小队 stats 与验证证据提取）

| # | 场景 | 实测数值 | 来源 |
|---|------|----------|------|
| 1 | keychain 可用性探测（standalone `powershell Get-Help Get-StoredCredential`） | exit 1，耗时 8528 / 9369 / 10058ms（另轮 p50 7246ms，max 8062ms）；对照 `powershell exit 0` 766ms / p50 261ms | F005 / perf |
| 2 | `auth login` 默认 keychain store | 7201 / 7212 / 7879ms（验证轮 11279ms），exit 0 ok:true 但密码零持久化 | F005 |
| 3 | `auth login --store encrypted`（对照） | 343 / 351 / 447ms（验证轮 1753ms） | F005 |
| 4 | 稳态 `device list`（keychain 默认，renewal 窗口内） | p50 7101ms ×3（验证轮 10912 / 12744 / 10277ms）；mock.log 恰 1 次 device/list、零续期 HTTP | F005 |
| 5 | 不走续期路径的对照命令 | `config get` 803-924ms；`auth status` 769ms（keychain 税约 12 倍） | F005 |
| 6 | 过期 token + keychain 默认 | 23722ms 后 exit 3（proactive + 401 回退两次探测串行）；15s 包装下 exit 124 @21.4s | F005 |
| 7 | device import 卡死轮询（恒 RUNNING） | 固定 ~500ms 节拍（间隔 518/523/534ms，零退避）；15s 内 28-29 次轮询；外推 ~7127 次/小时；单请求 30s 超时不管循环 | F010 |
| 8 | `chat --stream`（[DONE] 后服务端 hold 8s） | 内容 +229ms 完整；进程 8735ms 才退（服务端 8015ms 关闭）；hold 60s 变体被 timeout 12 杀 @12576ms exit 124；正常 happy path 0.577s | F023 |
| 9 | `auth login` stdin EOF 假成功 | 0.79s，exit 0，零请求 | F004 |
| 10 | `--args` 体积极限（OS argv） | 1MB（1,032,127B）与 100KB `--name` 均被 OS 拒绝（Argument list too long）；Windows 命令行上限 32767 字符；~29.5KB 内可行（28,771B JSON 正常往返）；空 vs 30k args：318-1047ms 噪声主导；1MB JSON.parse 仅 25ms（瓶颈是传输不是解析） | F032（perf，未对抗复核） |
| 11 | 并发 torn-read 原语探针 | 7238 / 14657 次读（49%）在并发就地写下观察到空文件；CLI 风暴 170 次调用中 live 捕获 1 次撕裂态 | F011 |
| 12 | 并行登录丢更新 | 6 并行 login 全 exit 0，credentials.enc 仅存 5/7 条（2 条静默丢失） | F011 |
| 13 | help 扫描（ai:contract） | 272 条帮助路径：157 干净 / 115 缺陷（全部为 `help <g> <s>` 形态）；读扫描 69 条：65 OK / 4 可解释非 OK | ai:contract |
| 14 | fuzz 矩阵 | ~120 种参数形态、~190 次调用、160 矩阵行；验证 mock 记录 112 次请求 | fuzz |

---

## 6. 覆盖率与测试缺口

**基线（侦察阶段实测）**：statements **42.36%** / branches **33.8%** / functions 45.8% / lines 43.16%；现有 **99 个单测全绿**；`npm run test:coverage` 因缺 `@vitest/coverage-v8` **无法运行**（脚本损坏）。

**0% 覆盖文件**（全部为本次确认缺陷的宿主）：
`src/index.ts`（F001/F014/F056 宿主）、`src/commands/auth.ts`（F004/F008/F025/F051）、`src/commands/chat.ts`（F023）、`src/commands/tool.ts`（F009）、`src/commands/dashboard.ts`（F017/F021）、`src/commands/topic.ts`、`src/commands/attachment.ts`（F003/F044）、`src/utils/prompt.ts`（F004/F013）。

**近 0%**：`credential-keychain.ts` 4.87%（F005/F012）、`credential-encrypted.ts` 8.82%（F002/F027）、`token-manager.ts` 10%（F011/F024）、`credential-store.ts` 12%（F033）。

**结构性测试缺口与缺陷的对应**：
- F001 之所以能出货：`test/manager-write.test.ts` 的 buildProgram() 用裸 `new Command()` + exitOverride、**不注册 `.version()`**，根/叶 `--version` 碰撞在测试缝上不可达（149-200 行传 `'--version','3'` 而真实入口有 `.version('0.1.0'）`）。
- F003 之所以能出货：attachment 上传无任何 wire 格式测试（contract.test.ts 仅断言命令名存在），且构建管线（tsup/esbuild）无类型检查——`tsc --noEmit` 今天跑恰好只报这一处 TS2554。
- F004/F013（prompt.ts 0%）、F005/F012（keychain 4.87%）、F002（encrypted 8.82%）、F011/F024（token-manager 10%）全部落在 0-12% 的凭据/交互层。
- 建议缺口闭环：修复 `test:coverage`（安装 @vitest/coverage-v8）并入 CI 门禁；为 0% 文件补 **spawn 级集成测试**（真实入口形态构建 program、真实 dist 产物），按第 3 节各"守护测试"落地。

---

## 7. AI 友好契约符合度

**成立的承诺（已验证）**：
- `--format json` stdout 恒为合法 JSON：9/9 管道默认扫描 + 15 用例扫描全部 VALID（成功路径）。
- 退出码分类 0/1/2/3 在非 tools 命令上全程保持（~190 次调用无一越界；仅外部 timeout 124 与 OS argv 126 属环境）。
- 格式优先级 `command --format > global --format > settings > TTY 默认` 在常规顺序下成立并被验证。
- 未知选项 / 未知子命令 / 缺必需参数：单行干净错误、exit 1、零网络调用。
- `auth token --header` 提供精确 X-Auth-Tenant/X-Auth-Login/X-Auth-Token JSON 脚本面；登录输出仅 token_prefix（20 字符，不解码入 payload，无 claims/签名泄漏）。
- OAuth 门禁正确：classic ticket 无法访问 /mcp 且**零网络流量**；JSON-RPC wire 形状（jsonrpc 2.0 / id / method / params.arguments）正确；Bearer 与 X-Auth-* 的通道纪律严格分界。
- `analytics list` 恰好 9 个文档化 op，全部路由正确；unknown op 有指引；坏 `--args` exit 1。
- 两种显式帮助形式（`--help`、`dc3 help`）stdout + exit 0。

**被违反的承诺（按影响排序）**：
- **F001 (CONFIRMED)**：README:65-137 与 `--help` 展示的 `--version <n>` 空格形式（写操作的标准用法）静默 no-op——对 agent 最致命。
- **F004 (CONFIRMED)**：非交互 stdin 下 exit 0 假成功，与 errors.ts 刻意维护的 1/2/3 契约直接冲突。
- **F009 (CONFIRMED)**：tools 家族成功 → exit 1 + 双 JSON 文档；`| jq`、`&&`、退出码解读全坏。
- **F014 (CONFIRMED)**：`help <g> <s>` 这个 agent 自然发现约定返回错误层级帮助且 exit 0。
- **F015 (CONFIRMED)**：失败通道二义（stdout JSON vs stderr text）——README:269-277 把 stdout JSON 定为集成路径却未定义错误流契约。
- F028 / F050 / F051 / F032 / F048 / F049 / F052 / F030（均 UNVERIFIABLE，发现小队已执行 repro）：README 示例本身不可用（`--count` 不存在、MCP `transport` 应为 `type`）、`--oauth` 隐藏于 help、大 `--args` 无替代通道、裸 `dc3` help 走 stderr、expires_at 双格式、`--format ''` 静默、退出码 1 语义过载。

---

## 8. 多级指令矩阵结论

multi:nested 小队（~65 次调用，11 个 Bash 批次 + 2 个 commander 最小复现；20 项行为验证健全、7 条发现，进入最终判定的归属项为 F001 / F008 / F014 / F029 / F053 / F056）：

- **嵌套分发本身健全**：二/三级子命令解析、alias 展开、必需参数校验、`dc3 config help profile` 正确给出 profile 组帮助、叶位空参数（`dc3 device 2`）正确报错、未知子命令单行报错。
- **陷阱集中在四类根因**：(1) 根 program 选项位置不敏感——`--version` 空格形式/叶后 `-V`/`--format`/`--profile` 全 argv last-wins（F001）；(2) help 派发只转发第一段路径（F014），未知 help 目标倾倒整页帮助且无错误（F056）；(3) commander 否定旗标语义误读（F008 `--no-save`）；(4) argv 卫生——多余位置参数静默丢弃、空串 id 原样上线、空白 `--profile` 静默回退默认（F053）；外加 alias 无弃用信号（F029）。
- **对 agent 的操作性结论**（修复前）：帮助发现只信 `<g> <s> --help`；写操作一律用 `--version=N` 等号形式；多级命令中全局选项严格前置；不要依赖 `help <g> <s>` 与 `--no-save`。

---

## 9. 文档漂移清单

| 位置 | 漂移内容 | 缺陷 | 判定 |
|------|----------|------|------|
| README.md:65-137 | `update <id> --version <n>` 空格形式为文档化标准用法，实际被根选项拦截 no-op | F001 | CONFIRMED |
| README.md:235-247 | 全局选项"置于子命令前"+ `command --format > global` 优先级——实际是 argv 最后出现者优先，仅常规顺序下巧合成立 | F001 | CONFIRMED |
| auth.ts:40 | "--no-save: Do not save password"——实际仍保存且静默降级 store | F008 | CONFIRMED |
| config-manager.ts:222 / config.ts:142 | "Run: dc3 config init" 指向不存在的命令；config get 建议 gateway-first 而该路径首跑必失败 | F007 | CONFIRMED |
| README.md:255-266 | Multi-Profile 工作流以 `config profile use prod` 开篇，但无任何创建 profile 的命令 | F007 | CONFIRMED |
| config reset 确认语 | "delete all profiles and config"，但 tokens.json / credentials.enc 存活 | F016 | CONFIRMED |
| alert.ts:171 | `--query` 帮助 "(repeatable)"，实为 last-wins | F019 | CONFIRMED |
| credential-store.ts:20-22 | 注释承诺 keychain→encrypted→env→prompt 优先级链，resolvePassword 只查单一 store | F033 | UNVERIFIABLE |
| README.md:91 | `point history [--count 100]`——实现为 `--limit/--cursor` | F028 | UNVERIFIABLE |
| README.md:28-29/64 | create 为"一个版本的弃用兼容别名"——零警告、无生命周期强制 | F029 | UNVERIFIABLE |
| README.md:214-231 | provider/model/attachment 15 个子命令 + session get / alert type-distribution / config profile delete / group update/delete / label 全家族未记载 | F031 | UNVERIFIABLE |
| README.md:289 | MCP 示例用 `transport` 键，Claude Code .mcp.json 需 `type` | F050 | UNVERIFIABLE (probable) |
| auth.ts:41-46 | `--oauth` 被 hideHelp 但 README:223 要求 agent 使用它 | F051 | UNVERIFIABLE |
| README.md:307-314 | 退出码表 1 = "业务错误(无效输入)"——404/409/500 与用法错误共用 1 | F030 | UNVERIFIABLE |

---

## 10. 修复优先级路线图（遵循 AGENTS.md 第一性原则：根治最低破坏层 + 防复发守护测试，反对症状修补）

### P0 — 立即（静默数据丢失 / 安全 / 假成功）
1. **F001**：`src/index.ts` 调用 `program.enablePositionalOptions()`；复查 detectFormat 优先级实现。守护：真实入口形态的 program 对 8 实体 × update/delete 全断言 + `-V` 负向。
2. **F002**：删除 `EncryptedData.entries` 与明文写入；随机密钥文件（0600 / Windows DPAPI）替代公开材料派生；遗留文件一次性清洗。守护：磁盘原文不含密码、无 key 文件不可解密。
3. **F003**：attachment 改 FormData + `postForm` 对齐服务端 `@RequestPart("file")` 契约；**流程级根治：`tsc --noEmit` 进 build/CI**。守护：捕获服务器断言 multipart 字节相等 + 非 ASCII 文件名 + tsc 门禁。
4. **F004**：prompt 终止契约（close/SIGINT reject、finally 恢复 stdout）+ 调用层 `!stdin.isTTY` 抛用法错误。守护：spawn 级非零退出 / 无状态写入 / 防挂起负向测试。
5. **F005**：快速有界探测（Get-Command + -NoProfile + 2s timeout，三平台）；`persisted:boolean` + 未保存显著警告（JSON `password_saved:false`）；实现或删除虚假 fallback 链注释。守护：exec stub 断言 timeout + <1s 延迟回归。

### P1 — high（状态完整性 / 注入 / 契约统一）
6. **F006**：setSetting 写前过 `AppSettingsSchema.shape[key].parse`；命令层边界从 schema 派生（单一事实源）；readConfig 区分损坏与越界并 salvage profiles。
7. **F011**：三个写入者统一 temp+fsync+rename 原子替换；load 区分 ENOENT 与 parse 失败（重试 / 响亮失败 / .bak）；load+save 全程进程间锁。
8. **F012**：全平台 `execFile`（argv 数组）+ 秘密走 stdin；win32 `-EncodedCommand` + PS 单引号翻倍。
9. **F008**：`const noSave = options.save === false` 一处派生两处使用（对齐 device.ts:227 惯用式）。
10. **F013**：passwordPrompt 改 stderr 问句 + 私有黑洞 Writable，彻底不碰 process.stdout。
11. **F009**：SilentExit 透传（共享 action wrapper），顺带覆盖 alert/analytics/attachment/command/session 同形隐患。
12. **F010**：waitForOperation 加 deadline（--wait-timeout / expiresAt 推导）+ 指数退避 + SIGINT AbortSignal + 结构化超时错误。
13. **F007**：setProfile 逐 key 校验 partial；注册真实 `config init` / `profile create`；handleFatalError 折叠 ZodError；"Run: dc3 ..." 全量快照测试。
14. **F014**：help 命令改全路径感知版（未知段报错）。
15. **F015**：单一失败 chokepoint（stdout 机器 envelope + stderr 人读行 + 既有 0/1/2/3），commander 用法错误接入。

### P2 — medium / 低危 / 流程
16. **F016** reset 全状态清除 + 租户隔离负向测试；**F024** 登出 epoch + 锁（与 F011 同层，一并修）。
17. **F017** URLSearchParams 查询构建 + 枚举/整数校验；**F021** 数字/枚举旗标统一挂共享 parser + 选项自省扫描门禁。
18. **F018** 空串存在性检查（6 个命令文件）；**F019** `--query` 收集器 + fail-fast。
19. **F020** 表格/yaml 包络感知渲染（JSON 输出字节级不动）；**F022** MCP SSE 帧 parsing 分支；**F023** [DONE] 终止 + reader.cancel() + 单换行；**F025** logout 本地清理 try/finally + 明示保留物。
20. 未验证高价值项随同层修复顺带覆盖：**F026/F027**（与 F002/F011 同层）、**F030**（与 F015 一并）、**F033**（与 F005 的链注释一并）。
21. 文档漂移批量修正：F028 / F031 / F050 / F051（README 与 help 同步）。
22. 流程项：安装 `@vitest/coverage-v8` 修复 `test:coverage` 并入 CI 门禁；为 0% 文件补 spawn 级集成测试（见第 6 节）。

---

## 11. 经验证健全的行为（verifiedSound 汇总，120+ 项）

- **认证与令牌**：登录 happy path 各 store 语义正确（encrypted 落盘 / env 不落盘 / prompt 不存储）；失败路径分类正确（401→3 带详情、网关 Down→2、坏 OAuth secret→3 且旧态不动）；logout happy path 完整清理双文件、重复 logout "Already logged out" 零网络；token 过期矩阵（有密码→完全静默 proactive 续期；无密码→恰一次 401 exit 3；401→续期→重试恰一次）；oauth ticket 不触发续期、干净 exit 3；租户隔离（profile 间不串 token、租户不匹配 403→3 无重试风暴）；token_prefix 仅 20 字符不泄 payload；tokens.json 不含密码。
- **配置管理**：username→gateway→tenant 引导顺序可行；profile use/delete（活动 profile 拒删 + 明确信息）；`--profile` 校验与写入定向；reset 确认/取消语义；无效值 `{ok:false}` exit 1；`settings.output_format` 端到端生效；格式优先级。
- **CRUD wire 契约（8 组全验）**：list POST `{offset,limit}` + 条件过滤仅按需；`get_by_id` encodeURIComponent；add 驼峰字段 + 文档化默认值；update（等号形式）读-合-写保留服务端字段且 id/version 覆盖；delete `?id=&version=`；乐观锁校验（缺失/abc/负数/溢出全拒）；204 空输出 exit 0；problem+json 错误分类（404/409/500 → 1，detail 提取）。
- **数据面读取**：point read/write（value 恒字符串且匹配后端 String 契约）；history cursor 编码正确；command call params 校验；event/topic/dashboard/alert/analytics 全部 22+8+9 子命令路径与查询正确；alert confirm/unconfirm 编码正确；bulk-confirm JSON 校验。
- **agentic 平面**：session/provider/model wire 形状与分页；provider update 失败遏制（list 500 时不部分 POST）；model 温度/max-tokens 客户端校验；chat 非流 happy + 流式 happy（0.577s）+ 401→3；tools 门禁与 JSON-RPC envelope；OAuth client_credentials 全链（Basic auth、scope 解析、Bearer 纪律）。
- **device import**：multipart 字节级正确（100/100 bytes、sha256 一致、恰好 2 part、幂等键不在 body）；UUIDv4 幂等键唯一且 401 重试复用同键、重发 multipart 字节一致；--wait/--no-wait/终态（FAILED/CANCELLED/EXPIRED → exit 1）/间隔下限 Math.max(100,·)；401 中流静默续期重发成功；4xx/5xx 单次失败无重试风暴；unicode 文件名 UTF-8 完整。
- **并发（正常窗口内）**：单 profile 6 并行续期 48 次零撕裂；并行 `config set` 不同 key 无丢失；`--profile` 覆盖从不持久化；全新引导流程无 mkdir 竞态。
- **核心单元级**：Dc3Client 401-retry-once（新令牌重试、失败浮出原 401、有界 2 次调用）；proactive 窗口判定精确（网络错误吞掉不产生风暴）；FormData 双次序列化不损；204 双路径；McpClient wire/错误 envelope；http 超时默认 30s 且 SSE 路径安全传 null；format.ts yaml 值 round-trip。
- **输出 / 退出契约**：~190 次调用退出码恒在 {0,1,2,3}；无未捕获栈/crash dump（单行 Error 渲染）；JSON 15 用例全 VALID；未知选项/子命令干净拒绝；CRUD list 面数字守卫完整；重复旗标 last-wins 一致；`--limit=5` 等号形式解析等价（隔离出 F001 纯为旗标碰撞）。
- **安全面（已验证部分）**：密码不出现在任何 CLI stdout/stderr/URL（仅 mock 请求 log，wire 契约使然）；manager 面恒 X-Auth-*、/mcp 与 oauth 恒 Bearer；keychain win32 Target 名净化（唯一存在转义之处）；登录失败只浮出服务端 detail。
- **harness 卫生**：真实 `~/.dc3` 的 config.json/tokens.json mtime 全程前后一致；各队 mock 全部按 PID 清理（netstat 复核无监听残留）；dc3-cli git 工作树 clean；live 网关只读访问全部经由私有快照副本。

---

## 附录：环境与 harness 说明

测试环境：Windows 11 Pro（10.0.26200），Git Bash（POSIX sh），Node v24.19.0，commander 12.1.0（仓库依赖），被测产物为 `dc3-cli/dist/index.js`（构建于 2026-10-06 08:45 本地 = 00:45Z，验证阶段逐项核对与 src 一致；git `338357430bef28baa67685d796d57848410e5358`，2026-10-06 08:33 +08:00 提交）。隔离方法：`mktemp -d` + `cygpath -m` 生成临时目录并以 USERPROFILE/HOME 环境变量覆盖实现隔离 home（真实 `C:/Users/pnoker/.dc3` 全程未触碰，各小队以 mtime 前后比对确认）。网络面：共享 `mock-gateway.mjs` harness（支持 MOCK_TOKEN_TTL_SEC / MOCK_OP_MODE / MOCK_RICH_ENTITY / MOCK_FAIL_LOGIN / MOCK_SSE_HOLD_MS 等模式开关，多小队按需扩展私有副本并记录 mock.log）；live 网关 `http://127.0.0.1:8080` 仅经私有 live-snapshot 副本做只读验证。所有命令置于 `timeout 15` 之下；各小队结束后按 PID taskkill + netstat 复核清理全部 mock/stub 进程，`git status --porcelain` 保持 clean。注意事项：测试期间工作树存在并发修改（chat.ts / provider.ts），全部验证以"当时 dist 与 src 一致"为前提；本报告所有时间均为本地时间（UTC+8）；第 4 节 31 条 UNVERIFIACBLE 项的数据可信度以发现小队的执行证据为准，未经统一对抗复核，落地修复前建议先按其 repro 复现确认。