# AI 助手（Agentic）体系体检报告

> 审计日期：2026-10-20 · 方法：4 阶段多智能体审计（8 读图 → 7 维度深审 → 每发现 3 镜头对抗验证 → 完备性批评）
> 规模：220 个 agent · 4,538 次工具调用 · 约 1,100 万 tokens · 81 分钟
> 结论口径：**62 条确认**（15 high / 33 medium / 14 low），6 条被对抗验证否决，13 条因 API 限流票数不全（均已获 ≥2 票通过，结论可信但置信略降）

---

## 一、总评（直接回答三个问题）

### 1. 健不健全？——**骨架健全，多处未达生产水位**

分层设计、依赖方向、端口-适配器分工全部符合仓库既有模式且干净。但有 15 个 high 级缺陷集中在**韧性**（幻觉工具名/慢消费者杀死整轮会话）、**安全**（SSRF 探测面、API Key 明文落库、提示注入零防御）、**契约**（OpenAI 兼容承诺与实现背离）三块——这些在单用户开发场景无感，多租户生产场景会集中引爆。

### 2. 全不全面？——**主体全面，但有五块"版图外"区域零审计**

12 工具 + 双运行时 + 会话/附件/动作管理 + 前端全套 + 41 测试的主体覆盖完整。但批评员发现五个**边界组成部分**从未被任何维度触及（见第六节），最典型的是 **MCP 外供通道**（平台把 API 以 MCP 工具暴露给外部 AI 客户端）——这是 AI 助手的"第三条暴露面"，完全零审计。

### 3. 是最佳实践吗？——**局部达标，体系不是**

**已达最佳实践水位的**：租户上下文强制注入（不信任模型参数）、AgenticToolResult 统一信封、写操作 10 分钟人审门、前端 SWR 不闪/XSS 消毒/竞态双令牌/千例对话语料、契约测试纪律。

**低于最佳实践的五个体系级差距**（均有 high 级证据）：
1. 供应商抽象不完整——换 Anthropic 等非 OpenAI 兼容供应商 = 静默砍掉 95% 工具面，且系统提示词仍向模型许诺全部能力
2. 上下文管理无 token 预算——按条数截窗、字符/4 估算用量、analytics 结果 5000 点无截断直接注入
3. Agent 循环终止 fail-hard——超 8 轮直接报错杀整轮，无重复调用检测、无末轮去工具强制收尾
4. 零指标埋点——LLM 延迟/工具成功率/token/队列深度全部不可度量，prometheus 端点空挂
5. 多副本语义未设计——进程内缓存无 TTL、失效仅本节点、限流按节点而非全局

---

## 二、High 级缺陷（15 条，对抗验证通过）

### 韧性组（agent 循环）
| # | 缺陷 | 位置 |
|---|---|---|
| H1 | **幻觉工具名 `Mono.error` 杀死整轮会话**——未知工具应返回 tool-error 让模型自愈（轮数上限已有兜底） | `OpenAiCompatibleAgenticRuntime.java:456` |
| H2 | **慢 SSE 消费者被硬性判死**——`requestedFromDownstream()==0` 即终止生成并落库 FAILED，零缓冲容忍 | `OpenAiCompatibleAgenticRuntime.java:271` |
| H3 | **双通道工具注册造成 Provider 级能力断层**——Spring AI 路径仅 3 类 @Tool（约 10 方法），60+ reactive 工具整体缺席；桥接代码在 L463-466 已有反向先例可对称复用 | `ChatClientConfig.java:106` |

### 安全组
| # | 缺陷 | 位置 |
|---|---|---|
| H4 | **连通性探测成 SSRF 通道**——草稿探测接受任意 baseUrl；providerId+自定义 baseUrl 组合会把存量 API Key 发往攻击者主机。需 https-only + 私网/环回段拒绝（解析后校验防 DNS rebinding）+ 改 baseUrl 必须同时换 key | `AgenticConnectivityCheckServiceImpl.java:160` |
| H5 | **LLM API Key 明文落库、无 TTL 常驻堆缓存**——静态加密完全缺失；建议信封加密（AES-GCM+主密钥）、缓存存脱敏副本 | `R2dbcModelProviderStore.java:46` |
| H6 | **提示注入零防御**——附件元数据无定界符直接拼 system prompt，工具结果回流无消毒。需注入防御段 + 随机化定界符 + 已知注入模式过滤 | `ChatClientConfig.java:53` |

### 契约组
| # | 缺陷 | 位置 |
|---|---|---|
| H7 | **非流式响应输出 camelCase 字段名**（finishReason/promptTokens）——背离自称的 OpenAI 兼容契约；请求的 max_tokens 被静默忽略 | `ChatCompletionResponseVO.java:93` |
| H8 | **conversationId 运行时必填但 OpenAPI 声明可选**——标准 OpenAI 客户端每次请求必失败；文档与实现互相矛盾 | `AgenticChatRequestPreparer.java:178` |

### 可观测组
| # | 缺陷 | 位置 |
|---|---|---|
| H9 | **agentic 模块零指标**——五组必备指标（信号量水位/轮次 Timer/工具 Counter/token 用量/队列深度）全缺 | `AgenticChatServiceImpl.java:173` |
| H10 | **trace 贯穿断裂**——requestId 从未写入 MDC（日志关联槽恒空），insight RPC 不携带关联 ID，跨服务排障断链 | `RequestIdWebFilter.java:103` |

### 前端组
| # | 缺陷 | 位置 |
|---|---|---|
| H11 | **Composer 回车在 IME 输入中途提交**——中日韩输入法确认候选词的 Enter 被当发送，中文用户无法正常输入 | `AgenticComposer.vue:80` |

### 最佳实践组
| # | 缺陷 | 位置 |
|---|---|---|
| H12 | 同 H3（供应商维度视角） | — |
| H13 | **上下文无 token 预算**（同总评差距 2） | `AgenticProperties.java:64` |
| H14 | **工具 schema 手写内联 JSON 且 7 个 analytics 工具的 series/window 是裸 `{"type":"object"}`**——schema 字面量与代码校验常数平行维护必然漂移；QUERY_LATEST 已证明可填全 | `ReactiveAgenticToolRegistry.java:325` |
| H15 | 同 H1（最佳实践维度视角） | — |

---

## 三、Medium 级主题（33 条，按主题归并）

- **架构**：1180 行注册表 8 组复制粘贴工厂/接口/解析器（一个泛型工厂可删约 500 行）；telescoping 构造器静默丢工具；`tools()` 每轮全量重建且排序被 `Map.copyOf` 丢弃；db 适配器反向注入 service 层执行文件系统副作用（分层倒置）；MessageController 绕过 service 直连 repository 且无分页；ChatClientFactory 四张缓存无 TTL、fallback 客户端永久粘滞
- **正确性**：软删会话墓碑永久毒化 conversationId（唯一约束冲突抛裸异常）；session_ext 并发读-合-写丢失更新（无锁无 CAS）；阻塞文件操作跑在 Netty 事件循环；TIMESTAMPTZ 绑定 UTC-naive LocalDateTime（时区漂移）；附件 TOCTOU 孤儿文件
- **安全**：连通性缓存无淘汰且 key 由客户端材料构成（内存 DoS）；租户缺模型配置时静默回退平台全局 Key 且该路径不限流；权限动词错位（批准高危写操作仅需 `action:list`，上传/聊天复用 list/get 动词）；附件无 MIME 白名单无租户配额
- **契约**：请求 messages 数组声明为完整上下文但只取最后一条 user；三种分页风格并存；AttachmentVO 泄露服务端存储路径；SessionVO 声明永远返回 null 的字段
- **可观测**：provider 真实 usage 被 includeUsage 请求后丢弃（全靠估算且系统性偏低）；限流半成品（无超时无队列上限，未知名模型完全绕过）；SSE 无心跳无事件名、失败恒 HTTP 200 收尾
- **前端**：流式热路径 O(n²)（每 delta 全量 markdown 重解析+DOMPurify）；mock 引擎整段输出不模拟逐 token 流；会话列表硬上限 50 无翻页（更早会话 UI 不可达）；停靠模式下拉与工作台栏数据口径分叉
- **最佳实践**：数值参数 `10.0`/`1e3` 形态抛 NumberFormatException；工具注册表与 provider 格式定义每请求重建

## 四、Low 级（14 条）

5 个注入未用的工具参数（重构残留）；OpenAI 客户端三处复制（运行时每请求新建无连接池复用）；工具异常 message 原样回灌模型（内部拓扑泄露）；provider 路由嵌套不一致；运维日志缺租户归属；fetch 通道 401 处理与 axios 拦截器漂移；失败回合无重试按钮；等——详见附录数据。

---

## 五、做得好的（必须说）

1. **租户隔离是全审计最强项**：每个工具从 ToolContext 强制提取 tenantId（不信任模型参数）、InsightDispatcher 二次校验、DB 层 WHERE 全带 tenant_id、写点值 10 分钟人审门全程租户校验
2. **统一信封设计**：AgenticToolResult{success,code,message,data,visualizations} 六态 code 枚举，已注册工具的错误被 tracing 层降级为 tool-result 不炸循环（讽刺的是未知工具名反而炸——H1 正是没对齐这个自己的好模式）
3. **前端工程质量高于社区平均**：SWR 缓存播种+尾部对齐合并（无重载闪烁）、幽灵占位、reduced-motion 双处生效、多宽度适配、DOMPurify 全量消毒、竞态双令牌、1000+ 用例对话语料
4. **仓库纪律**：禁 @PathVariable 全模块合规（grep 验证）、RFC 9457 problem+json 统一错误、密钥 VO write-only + LogSanitizer 达标、消息索引 FOR UPDATE 正确串行化、action 审批 CAS 竞态安全

---

## 六、完备性缺口（批评员裁定，二轮审计候选）

1. **MCP 外供通道**（`dc3-common-auth` 的 McpManagementController/ReactiveOAuthMcpRuntimeServiceImpl + `openapi-agentic.json` 冻结快照 + dc3-cli mcp.ts）——第三条暴露面，且快照与 7 个 controller 实际契约的漂移**无任何守护**
2. **ActiveAgenticProfileConfig** 经 spring.factories 以 HIGHEST_PRECEDENCE 无条件激活 agentic profile 到任何宿主（含 center-single）——未审计
3. **native/AOT 形态**：手写 JSON schema + ObjectMapper + MethodToolCallbackProvider 反射在 native 管线（真实发布形态）下行为从未验证
4. **权限种子闭环**：全部 initdb/migrations grep 不到任何 agentic 资源码——@perm.can 引用的资源从何而来、默认谁能批准写动作，未定性（verb 错位是笔误还是有意无法判断）
5. **数据保留与隐私**：dc3_message（全量对话+工具轨迹）与 dc3_attachment 无保留期策略、无租户清理任务、无合规视角
6. **多副本部署**：SSE 网关粘性、缓存跨节点失效、信号量按节点——多处仅"注释自认退化"
7. **Insight TSDB 租户隔离未下钻**（ReactiveTsdbStore 是否强制 tenant 分量）——租户隔离是硬规则，此处必须二轮实证
8. 文档面（iot-dc3-docs 仓库）、可达性/i18n、mock 契约守护、工具层测试盲区（CommandTool/EventTool 恰是最弱校验处且零专测）

## 七、修复路线建议

- **P0（发布前必须）**：H4 SSRF、H5 密钥加密、H1+H2 循环韧性（都是小改动大收益）、H11 IME（一行守卫）
- **P1（生产水位）**：H3 双通道桥接、H13 token 预算、H7/H8 契约对齐、H9+H10 指标+trace、权限动词、附件白名单+配额、限流补全
- **P2（工程债）**：注册表泛型化重构（-500 行）、分页统一、缓存 TTL、前端 O(n²) 批量合并、MessageController 分页
- **二轮审计**：第六节 8 项（MCP 通道优先）

---

## 八、查漏补缺二轮结果（2026-10-21/22）

62 条逐条销账（52 fixed / 9 partial / 2 missed）+ 回归扫描 + 同步面检查（50-agent workflow），全部 partial/missed 已补齐：key 外带向量、https-only、NAT64/6to4、usage 端到端、工具指标、重复调用检测、工具格式缓存、token 预算、附件配额、fetch 401 收敛、快照重同步 + 漂移守护测试、mock 对齐、历史附件数字 id bug。**pinnedDns 的 OkHttp 接线经 javap 实证不可行**（SDK builder 无定制口）——缝隙 API 保留待 SDK 演进，此为唯一技术性未闭环项。L61 结构化输出维持豁免。

## 附：方法论与可信度说明

- 7 维度审查员各自独立读码（非仅依赖读图摘要），每条 finding 带 file:line+代码证据+可落地建议
- 每条发现经 3 镜头对抗验证（证据核对/影响评估/主动反驳），≥2/3 票保留；13 条因 API 限流仅 2 票（全部为 2 正票，无反对票）
- 6 条被否决项含一条高价值仲裁：审计员称"信号量许可取消时永久泄漏为最严重缺陷"，验证员否决，人工复核确认**否决正确**（`Flux.usingWhen` 三参重载在 cancel 路径同样执行 release；仅剩 acquire 阻塞中途被取消的极窄边缘）
- 完整结构化数据（62 条全文+8 张子系统地图+7 份维度总结+批评报告）：会话任务输出 `wegnwa0xu.output`，journal 见 `subagents/workflows/wf_3344a3b1-c53/journal.jsonl`
