/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import type {
  AgenticMessage,
  AgenticModelConfig,
  AgenticProvider,
  AgenticSession,
  AgenticVisualizationSpec
} from '@/config/types';
import type {McpAuditRecord, McpConnectionRecord, McpToolRecord, OAuthClientRecord} from '@/config/types/auth';
import {charts} from '../fetch';

/** Stable timestamps so the demo does not regenerate on every load. */
const CREATED = '2026-07-15T09:30:00';
const UPDATED = '2026-08-01T14:20:00';
const T1 = '2026-08-05T10:12:30';
const T2 = '2026-08-05T10:13:05';
const T3 = '2026-08-06T08:45:00';
const T4 = '2026-08-06T16:30:00';

// ─── Agentic providers ──────────────────────────────────────────────

interface ProviderDef {
  name: string;
  providerType: 'OPENAI_COMPATIBLE' | 'ANTHROPIC';
  baseUrl: string;
  defaultFlag: 'DEFAULT' | 'NOT_DEFAULT';
  enableFlag: 'ENABLE' | 'DISABLE';
  remark: string;
}

const providerDefs: ProviderDef[] = [
  {
    name: 'OpenAI',
    providerType: 'OPENAI_COMPATIBLE',
    baseUrl: 'https://api.openai.com/v1',
    defaultFlag: 'DEFAULT',
    enableFlag: 'ENABLE',
    remark: 'OpenAI 官方 API，OpenAI 兼容协议',
  },
  {
    name: 'DeepSeek',
    providerType: 'OPENAI_COMPATIBLE',
    baseUrl: 'https://api.deepseek.com/v1',
    defaultFlag: 'NOT_DEFAULT',
    enableFlag: 'ENABLE',
    remark: 'DeepSeek，OpenAI 兼容接口',
  },
  {
    name: 'Anthropic Claude',
    providerType: 'ANTHROPIC',
    baseUrl: 'https://api.anthropic.com/v1',
    defaultFlag: 'NOT_DEFAULT',
    enableFlag: 'ENABLE',
    remark: 'Anthropic Claude 原生 API',
  },
  {
    name: '通义千问 Qwen',
    providerType: 'OPENAI_COMPATIBLE',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    defaultFlag: 'NOT_DEFAULT',
    enableFlag: 'DISABLE',
    remark: '阿里云百炼 OpenAI 兼容模式',
  },
];

export const agenticProviders: AgenticProvider[] = providerDefs.map((p, i) => ({
  id: `1912345678901234${567 + i}`,
  name: p.name,
  providerType: p.providerType,
  baseUrl: p.baseUrl,
  defaultFlag: p.defaultFlag,
  enableFlag: p.enableFlag,
  remark: p.remark,
  createTime: CREATED,
  operateTime: UPDATED,
}));

// ─── Agentic model configs ──────────────────────────────────────────

interface ModelConfigDef {
  model: string;
  label: string;
  providerIndex: number;
  stream: boolean;
  toolCall: boolean;
  vision: boolean;
  reasoning: boolean;
  temperature: number;
  maxTokens: number;
  defaultFlag: 'DEFAULT' | 'NOT_DEFAULT';
  enableFlag: 'ENABLE' | 'DISABLE';
  remark: string;
}

const modelConfigDefs: ModelConfigDef[] = [
  {
    model: 'gpt-4o',
    label: 'GPT-4o',
    providerIndex: 0,
    stream: true,
    toolCall: true,
    vision: true,
    reasoning: false,
    temperature: 0.7,
    maxTokens: 4096,
    defaultFlag: 'DEFAULT',
    enableFlag: 'ENABLE',
    remark: 'OpenAI 旗舰多模态模型',
  },
  {
    model: 'deepseek-chat',
    label: 'DeepSeek-V3',
    providerIndex: 1,
    stream: true,
    toolCall: true,
    vision: false,
    reasoning: false,
    temperature: 0.5,
    maxTokens: 8192,
    defaultFlag: 'NOT_DEFAULT',
    enableFlag: 'ENABLE',
    remark: 'DeepSeek 通用对话模型',
  },
  {
    model: 'deepseek-reasoner',
    label: 'DeepSeek-R1',
    providerIndex: 1,
    stream: true,
    toolCall: false,
    vision: false,
    reasoning: true,
    temperature: 0.2,
    maxTokens: 8192,
    defaultFlag: 'NOT_DEFAULT',
    enableFlag: 'ENABLE',
    remark: 'DeepSeek 推理模型，支持思维链',
  },
  {
    model: 'claude-3-5-sonnet',
    label: 'Claude 3.5 Sonnet',
    providerIndex: 2,
    stream: true,
    toolCall: true,
    vision: true,
    reasoning: true,
    temperature: 0.7,
    maxTokens: 8192,
    defaultFlag: 'NOT_DEFAULT',
    enableFlag: 'ENABLE',
    remark: 'Anthropic Claude 3.5 Sonnet',
  },
];

export const agenticModelConfigs: AgenticModelConfig[] = modelConfigDefs.map((m, i) => ({
  id: `1912345678901235${568 + i}`,
  model: m.model,
  label: m.label,
  providerId: agenticProviders[m.providerIndex]!.id,
  providerName: agenticProviders[m.providerIndex]!.name,
  stream: m.stream,
  toolCall: m.toolCall,
  vision: m.vision,
  reasoning: m.reasoning,
  temperature: m.temperature,
  maxTokens: m.maxTokens,
  defaultFlag: m.defaultFlag,
  enableFlag: m.enableFlag,
  remark: m.remark,
  createTime: CREATED,
  operateTime: UPDATED,
}));

// ─── Agentic sessions ───────────────────────────────────────────────

interface SessionDef {
  conversationId: string;
  title: string;
  model: string;
  reasoningEnabled: boolean;
  temperature: number;
  maxTokens: number;
}

const sessionDefs: SessionDef[] = [
  {
    conversationId: 'b1f2a3d4-5e60-4a7b-8c9d-0a1b2c3d4e5f',
    title: 'Modbus 设备点位查询',
    model: 'gpt-4o',
    reasoningEnabled: false,
    temperature: 0.7,
    maxTokens: 4096,
  },
  {
    conversationId: 'c2e3b4c5-6f70-4b8c-9d0e-1b2c3d4e5f6a',
    title: '电力仪表数据分析',
    model: 'deepseek-reasoner',
    reasoningEnabled: true,
    temperature: 0.2,
    maxTokens: 8192,
  },
  {
    conversationId: 'd3f4c5d6-7080-4c9d-0e1f-2c3d4e5f6a7b',
    title: '驱动配置异常排查',
    model: 'claude-3-5-sonnet',
    reasoningEnabled: true,
    temperature: 0.7,
    maxTokens: 8192,
  },
  {
    conversationId: 'e4a5d6e7-8090-4d0e-1f2a-3d4e5f6a7b8c',
    title: 'OPC-UA 连接诊断',
    model: 'deepseek-chat',
    reasoningEnabled: false,
    temperature: 0.5,
    maxTokens: 8192,
  },
];

export const agenticSessions: AgenticSession[] = sessionDefs.map((s, i) => ({
  conversationId: s.conversationId,
  title: s.title,
  sessionExt: {
    model: s.model,
    reasoningEnabled: s.reasoningEnabled,
    temperature: s.temperature,
    maxTokens: s.maxTokens,
  },
  createTime: i % 2 === 0 ? T1 : T3,
  operateTime: i % 2 === 0 ? T3 : T4,
}));

// ─── Agentic messages ───────────────────────────────────────────────
// 3 turns in session 0, 1 user/assistant pair in session 2.

interface MessageDef {
  sessionIndex: number;
  role: 'user' | 'assistant';
  content: string;
  model?: string;
  messageIndex: number;
  status: number;
  streaming: boolean;
  finishReason?: string;
  reasoning?: string;
  tokens?: { input: number; output: number };
  charts?: AgenticVisualizationSpec[];
  createTime: string;
}

const messageDefs: MessageDef[] = [
  // Session 0 — Modbus 点位查询
  {
    sessionIndex: 0,
    role: 'user',
    content: '帮我列出 DEV-001 设备下的所有点位，并标注读写权限。',
    messageIndex: 0,
    status: 2,
    streaming: false,
    createTime: T1,
  },
  {
    sessionIndex: 0,
    role: 'assistant',
    model: 'gpt-4o',
    messageIndex: 1,
    status: 2,
    streaming: false,
    finishReason: 'stop',
    tokens: {input: 1180, output: 726},
    charts: [charts.pointValues()],
    createTime: T2,
    content:
      'DEV-001 挂载 5 个采集点位，当前实时值如上图。温度/湿度/电压/电流为只读量，功率为派生只读量。需要调整某点位的单位或读写属性，告诉我具体位号即可。',
  },
  {
    sessionIndex: 0,
    role: 'user',
    content: '看下最近 24 小时的温度和湿度趋势。',
    messageIndex: 2,
    status: 2,
    streaming: false,
    createTime: T3,
  },
  {
    sessionIndex: 0,
    role: 'assistant',
    model: 'gpt-4o',
    messageIndex: 3,
    status: 2,
    streaming: false,
    finishReason: 'stop',
    tokens: {input: 1340, output: 612},
    charts: [charts.tempTrend(), charts.humidity()],
    createTime: T3,
    content:
      '过去 24h 温度在 71-85℃ 波动，14:00 达到峰值 85.4℃（接近 80℃ 告警阈值）；湿度稳定在 48-60%RH。整体环境正常，建议关注午后温度峰值。',
  },
  // Session 1 — 电力仪表数据分析
  {
    sessionIndex: 1,
    role: 'user',
    content: '统计本周能耗，并分析功率趋势。',
    messageIndex: 0,
    status: 2,
    streaming: false,
    createTime: T2,
  },
  {
    sessionIndex: 1,
    role: 'assistant',
    model: 'deepseek-reasoner',
    messageIndex: 1,
    status: 2,
    streaming: false,
    finishReason: 'stop',
    reasoning:
      '聚合近 7 日 point_value 历史，按日求和得能耗；功率取 24h 均值曲线，识别生产时段（8-18 时）负载抬升。',
    tokens: {input: 2100, output: 980},
    charts: [charts.energyWeek(), charts.powerTrend()],
    createTime: T2,
    content:
      '近 7 日总能耗 **340.9 kWh**，工作日均值 55.2 kWh，周末降至 31.9 kWh，周四最高（61.2 kWh）。功率曲线在 8-18 时明显抬升，符合生产节律。建议在谷电时段调度高耗能任务以降低用电成本。',
  },
  {
    sessionIndex: 1,
    role: 'user',
    content: '哪个设备能耗最高？',
    messageIndex: 2,
    status: 2,
    streaming: false,
    createTime: T3,
  },
  {
    sessionIndex: 1,
    role: 'assistant',
    model: 'deepseek-reasoner',
    messageIndex: 3,
    status: 2,
    streaming: false,
    finishReason: 'stop',
    tokens: {input: 1820, output: 540},
    createTime: T3,
    content:
      '能耗 Top3：① 3 号风机（68.4 kWh，占 20%）② 注塑机 A（54.2 kWh）③ 空压机（41.7 kWh）。3 号风机近期温度偏高，可能与持续高负载相关，建议结合温度告警一并排查。',
  },
  // Session 2 — 驱动配置异常排查
  {
    sessionIndex: 2,
    role: 'user',
    content: 'OPC-UA 驱动频繁掉线，帮我排查，顺便看下各驱动负载。',
    messageIndex: 0,
    status: 2,
    streaming: false,
    createTime: T4,
  },
  {
    sessionIndex: 2,
    role: 'assistant',
    model: 'claude-3-5-sonnet',
    messageIndex: 1,
    status: 2,
    streaming: false,
    finishReason: 'stop',
    tokens: {input: 1320, output: 580},
    charts: [charts.driverLoad(), charts.deviceStatus()],
    createTime: T4,
    content:
      '日志显示 OPC-UA 掉线集中在每小时第 12 分附近，伴随 SecureChannel 超时。各驱动负载如上：S7 PLC（CPU 41%/内存 64%）、OPC-UA（35%/58%）偏高。建议：1) 检查服务端证书有效期；2) 排查第 12 分定时任务导致的网络抖动；3) 把握手超时从 5s 调到 10s 后观察一轮。',
  },
  // Session 3 — 设备总览
  {
    sessionIndex: 3,
    role: 'user',
    content: '当前平台整体态势如何？',
    messageIndex: 0,
    status: 2,
    streaming: false,
    createTime: T4,
  },
  {
    sessionIndex: 3,
    role: 'assistant',
    model: 'deepseek-chat',
    messageIndex: 1,
    status: 2,
    streaming: false,
    finishReason: 'stop',
    tokens: {input: 1560, output: 720},
    charts: [charts.deviceStatus(), charts.tempTrend(), charts.alarmSummary()],
    createTime: T4,
    content:
      '当前态势：16 台设备中 12 在线、3 离线、1 告警（3 号风机温度异常）。近 24h 共 17 条告警（P0×1 / P1×4 / P2×12），已恢复 9 条。首要风险是 3 号风机温度，建议优先处理散热与负载。',
  },
];

export const agenticMessages: AgenticMessage[] = messageDefs.map((m, i) => ({
  id: `1912345678912345${100 + i}`,
  conversationId: agenticSessions[m.sessionIndex]?.conversationId ?? '',
  role: m.role,
  content: m.content,
  contentExt:
    m.tokens || m.reasoning || (m.charts && m.charts.length)
      ? {
        ...(m.tokens ? {tokens: m.tokens} : {}),
        ...(m.reasoning ? {reasoningContent: m.reasoning} : {}),
        ...(m.charts && m.charts.length ? {charts: m.charts} : {}),
      }
      : undefined,
  model: m.model,
  messageIndex: m.messageIndex,
  status: m.status,
  streaming: m.streaming,
  finishReason: m.finishReason,
  createTime: m.createTime,
}));

// ─── MCP OAuth clients ──────────────────────────────────────────────

interface ClientDef {
  clientId: string;
  clientName: string;
  clientType: 'CONFIDENTIAL' | 'PUBLIC';
  ownerPrincipalId: string;
  serviceAccountPrincipalId: string;
  tenantId: string;
  authorizationGrantTypes: string;
  redirectUris: string;
  scopes: string;
  enableFlag: 'ENABLE' | 'DISABLE';
}

const clientDefs: ClientDef[] = [
  {
    clientId: 'dc3_8f14e45fceea167a5a36dedd4bea2540',
    clientName: 'dc3-web',
    clientType: 'CONFIDENTIAL',
    ownerPrincipalId: '1',
    serviceAccountPrincipalId: '1001',
    tenantId: '1',
    authorizationGrantTypes: 'authorization_code,client_credentials,refresh_token',
    redirectUris: 'http://localhost:5173/auth/mcp/callback',
    scopes: 'mcp:tool:list mcp:tool:call mcp:connection:manage',
    enableFlag: 'ENABLE',
  },
  {
    clientId: 'dc3_2c624232cdd221771b4ea5508d82b4c7',
    clientName: 'dc3-agentic',
    clientType: 'CONFIDENTIAL',
    ownerPrincipalId: '1',
    serviceAccountPrincipalId: '1002',
    tenantId: '1',
    authorizationGrantTypes: 'client_credentials',
    redirectUris: '',
    scopes: 'mcp:tool:list mcp:tool:call',
    enableFlag: 'ENABLE',
  },
  {
    clientId: 'dc3_3d373b1a2e6d41f29a7b9c8e5d4f3a2b',
    clientName: 'dc3-cli',
    clientType: 'PUBLIC',
    ownerPrincipalId: '1',
    serviceAccountPrincipalId: '',
    tenantId: '1',
    authorizationGrantTypes: 'authorization_code,refresh_token',
    redirectUris: 'http://localhost:8765/callback',
    scopes: 'mcp:tool:list',
    enableFlag: 'ENABLE',
  },
  {
    clientId: 'dc3_9e107d9d372bb6826bd81d3542a419d6',
    clientName: 'external-etl',
    clientType: 'CONFIDENTIAL',
    ownerPrincipalId: '1',
    serviceAccountPrincipalId: '1003',
    tenantId: '1',
    authorizationGrantTypes: 'client_credentials',
    redirectUris: '',
    scopes: 'mcp:tool:list',
    enableFlag: 'DISABLE',
  },
];

export const mcpClients: OAuthClientRecord[] = clientDefs.map((c, i) => ({
  id: `190112345670001${10 + i}`,
  clientId: c.clientId,
  clientName: c.clientName,
  clientType: c.clientType,
  ownerPrincipalId: c.ownerPrincipalId,
  serviceAccountPrincipalId: c.serviceAccountPrincipalId,
  tenantId: c.tenantId,
  authorizationGrantTypes: c.authorizationGrantTypes,
  redirectUris: c.redirectUris,
  scopes: c.scopes,
  enableFlag: c.enableFlag,
}));

// ─── MCP connections ────────────────────────────────────────────────

interface ConnectionDef {
  connectionName: string;
  clientIndex: number;
  principalId: string;
  principalType: 'USER' | 'SERVICE_ACCOUNT';
  tenantId: string;
  grantType: 'authorization_code' | 'client_credentials';
  enableFlag: 'ENABLE' | 'DISABLE';
  lastUsedTime: string;
  revokeTime?: string;
}

const connectionDefs: ConnectionDef[] = [
  {
    connectionName: 'Web 控制台管理员',
    clientIndex: 0,
    principalId: '1',
    principalType: 'USER',
    tenantId: '1',
    grantType: 'authorization_code',
    enableFlag: 'ENABLE',
    lastUsedTime: T4,
  },
  {
    connectionName: 'Agentic 服务账户',
    clientIndex: 1,
    principalId: '1002',
    principalType: 'SERVICE_ACCOUNT',
    tenantId: '1',
    grantType: 'client_credentials',
    enableFlag: 'ENABLE',
    lastUsedTime: T3,
  },
  {
    connectionName: 'CLI 开发者',
    clientIndex: 2,
    principalId: '2',
    principalType: 'USER',
    tenantId: '1',
    grantType: 'authorization_code',
    enableFlag: 'DISABLE',
    lastUsedTime: T2,
    revokeTime: T2,
  },
  {
    connectionName: 'ETL 集成服务账户',
    clientIndex: 3,
    principalId: '1003',
    principalType: 'SERVICE_ACCOUNT',
    tenantId: '1',
    grantType: 'client_credentials',
    enableFlag: 'ENABLE',
    lastUsedTime: T1,
  },
];

export const mcpConnections: McpConnectionRecord[] = connectionDefs.map((c, i) => ({
  id: `190112345670001${30 + i}`,
  connectionName: c.connectionName,
  clientId: mcpClients[c.clientIndex]!.clientId,
  principalId: c.principalId,
  principalType: c.principalType,
  tenantId: c.tenantId,
  grantType: c.grantType,
  enableFlag: c.enableFlag,
  lastUsedTime: c.lastUsedTime,
  revokeTime: c.revokeTime,
}));

// ─── MCP tool catalog ───────────────────────────────────────────────
// All tools are generated from the manager service OpenAPI; apiCode follows
// {@code dc3-center-manager:<METHOD>:<path>} and toolId mirrors apiCode.
// command_send is the high-risk destructive tool that requires confirmation.

interface ToolDef {
  toolName: string;
  toolTitle: string;
  apiPath: string;
  httpMethod: 'GET' | 'POST' | 'PUT' | 'DELETE';
  permissionCode: string;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  readOnlyHint: number;
  destructiveHint: number;
  idempotentHint: number;
  openWorldHint: number;
  enableFlag: 'ENABLE' | 'DISABLE';
  remark: string;
  schemaHash: string;
}

const MANAGER = 'dc3-center-manager';

const toolDefs: ToolDef[] = [
  {
    toolName: 'driver_list',
    toolTitle: 'List drivers',
    apiPath: '/driver/list',
    httpMethod: 'POST',
    permissionCode: `${MANAGER}:List drivers`,
    riskLevel: 'LOW',
    readOnlyHint: 1,
    destructiveHint: 0,
    idempotentHint: 1,
    openWorldHint: 0,
    enableFlag: 'ENABLE',
    remark: '查询驱动列表，只读操作',
    schemaHash: 'a3f1c9e6b2d4870f5e1a9c3b7d8e0f2a',
  },
  {
    toolName: 'device_list',
    toolTitle: 'List devices',
    apiPath: '/device/list',
    httpMethod: 'POST',
    permissionCode: `${MANAGER}:List devices`,
    riskLevel: 'LOW',
    readOnlyHint: 1,
    destructiveHint: 0,
    idempotentHint: 1,
    openWorldHint: 0,
    enableFlag: 'ENABLE',
    remark: '查询设备列表，只读操作',
    schemaHash: 'b4e2d0f7c3e5981a6f2b0d4c8e9f1a3b',
  },
  {
    toolName: 'device_add',
    toolTitle: 'Add device',
    apiPath: '/device/add',
    httpMethod: 'POST',
    permissionCode: `${MANAGER}:Add device`,
    riskLevel: 'MEDIUM',
    readOnlyHint: 0,
    destructiveHint: 0,
    idempotentHint: 0,
    openWorldHint: 0,
    enableFlag: 'ENABLE',
    remark: '新增设备，需写权限',
    schemaHash: 'c5f3e108d4f6a92b7a3c1e5d9f0a2b4c',
  },
  {
    toolName: 'command_send',
    toolTitle: 'Send command to device',
    apiPath: '/command/add',
    httpMethod: 'POST',
    permissionCode: `${MANAGER}:Add command`,
    riskLevel: 'HIGH',
    readOnlyHint: 0,
    destructiveHint: 1,
    idempotentHint: 0,
    openWorldHint: 1,
    enableFlag: 'ENABLE',
    remark: '下发指令到设备，高危操作，需人工确认',
    schemaHash: 'd6a4f219e5a7ba3c8b4d2f6e0a1b3c5d',
  },
  {
    toolName: 'device_delete',
    toolTitle: 'Delete device',
    apiPath: '/device/delete',
    httpMethod: 'POST',
    permissionCode: `${MANAGER}:Delete device`,
    riskLevel: 'HIGH',
    readOnlyHint: 0,
    destructiveHint: 1,
    idempotentHint: 1,
    openWorldHint: 0,
    enableFlag: 'DISABLE',
    remark: '删除设备，高危且不可逆，默认隐藏',
    schemaHash: 'e7b5a320f6b8cb4d9c5e3a7f1b2c4d6e',
  },
];

export const mcpTools: McpToolRecord[] = toolDefs.map((t, i) => ({
  id: `190112345670001${50 + i}`,
  toolId: `${MANAGER}:${t.httpMethod}:${t.apiPath}`,
  toolName: t.toolName,
  toolTitle: t.toolTitle,
  toolCategory: MANAGER,
  serviceName: MANAGER,
  apiCode: `${MANAGER}:${t.httpMethod}:${t.apiPath}`,
  permissionCode: t.permissionCode,
  httpMethod: t.httpMethod,
  apiPath: t.apiPath,
  schemaHash: t.schemaHash,
  riskLevel: t.riskLevel,
  readOnlyHint: t.readOnlyHint,
  destructiveHint: t.destructiveHint,
  idempotentHint: t.idempotentHint,
  openWorldHint: t.openWorldHint,
  enableFlag: t.enableFlag,
  remark: t.remark,
}));

// ─── MCP audit log ──────────────────────────────────────────────────
// toolId in audits mirrors the catalog toolId (= apiCode). Includes a denied
// and a pending confirmation entry for the high-risk command_send tool.

interface AuditDef {
  traceId: string;
  principalId: string;
  principalType: 'USER' | 'SERVICE_ACCOUNT';
  clientIndex: number;
  connectionIndex: number;
  toolIndex: number;
  confirmId?: string;
  idempotencyKey?: string;
  argumentDigest: string;
  status: 'SUCCESS' | 'DENIED' | 'PENDING';
  errorCode?: string;
  durationMs: number;
  clientName: string;
  clientVersion: string;
  remoteIp: string;
  createTime: string;
}

const auditDefs: AuditDef[] = [
  {
    traceId: 'f1a2b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
    principalId: '1',
    principalType: 'USER',
    clientIndex: 0,
    connectionIndex: 0,
    toolIndex: 0,
    argumentDigest: '9b1d4f7e2a8c5601',
    status: 'SUCCESS',
    durationMs: 118,
    clientName: 'dc3-web',
    clientVersion: '1.0.4',
    remoteIp: '10.0.12.31',
    createTime: T1,
  },
  {
    traceId: 'a2b3c4d5-e6f7-4a8b-9c0d-1e2f3a4b5c6d',
    principalId: '1002',
    principalType: 'SERVICE_ACCOUNT',
    clientIndex: 1,
    connectionIndex: 1,
    toolIndex: 2,
    confirmId: '7c1e9a30-4b2d-4e8f-9a1c-3d5e7f8a9b0c',
    idempotencyKey: '3f2a1b8c-5d6e-4f7a-8b9c-0d1e2f3a4b5c',
    argumentDigest: '4e8a1c6f0b3d7925',
    status: 'SUCCESS',
    durationMs: 342,
    clientName: 'dc3-agentic',
    clientVersion: '2026.6.1',
    remoteIp: '10.0.12.47',
    createTime: T2,
  },
  {
    traceId: 'b3c4d5e6-f708-4b9c-0d1e-2f3a4b5c6d7e',
    principalId: '1002',
    principalType: 'SERVICE_ACCOUNT',
    clientIndex: 1,
    connectionIndex: 1,
    toolIndex: 3,
    argumentDigest: '1d7b9e3a5c0f4862',
    status: 'DENIED',
    errorCode: 'HIGH_RISK_CONFIRMATION_REQUIRED',
    durationMs: 46,
    clientName: 'dc3-agentic',
    clientVersion: '2026.6.1',
    remoteIp: '10.0.12.47',
    createTime: T3,
  },
  {
    traceId: 'c4d5e6f7-0819-4c0d-1e2f-3a4b5c6d7e8f',
    principalId: '1',
    principalType: 'USER',
    clientIndex: 0,
    connectionIndex: 0,
    toolIndex: 3,
    confirmId: '8d2f0b41-5c3e-4f9a-8b2d-4e6f8a9c0d1e',
    argumentDigest: '6a2f8d4b0e7c3519',
    status: 'PENDING',
    durationMs: 0,
    clientName: 'dc3-web',
    clientVersion: '1.0.4',
    remoteIp: '10.0.12.31',
    createTime: T3,
  },
  {
    traceId: 'd5e6f708-191a-4d1e-2f3a-4b5c6d7e8f9a',
    principalId: '2',
    principalType: 'USER',
    clientIndex: 2,
    connectionIndex: 2,
    toolIndex: 1,
    argumentDigest: '0c5a3d8b6e1f9274',
    status: 'SUCCESS',
    durationMs: 96,
    clientName: 'dc3-cli',
    clientVersion: '0.4.2',
    remoteIp: '192.168.10.8',
    createTime: T4,
  },
];

export const mcpAudits: McpAuditRecord[] = auditDefs.map((a, i) => {
  const tool = mcpTools[a.toolIndex]!;
  const client = mcpClients[a.clientIndex]!;
  const connection = mcpConnections[a.connectionIndex]!;
  return {
    id: `190112345670001${70 + i}`,
    traceId: a.traceId,
    tenantId: '1',
    principalId: a.principalId,
    principalType: a.principalType,
    clientId: client.clientId,
    connectionId: connection.id,
    toolId: tool.toolId,
    toolName: tool.toolName,
    permissionCode: tool.permissionCode,
    riskLevel: tool.riskLevel,
    confirmId: a.confirmId,
    idempotencyKey: a.idempotencyKey,
    argumentDigest: a.argumentDigest,
    status: a.status,
    errorCode: a.errorCode,
    durationMs: a.durationMs,
    clientName: a.clientName,
    clientVersion: a.clientVersion,
    remoteIp: a.remoteIp,
    createTime: a.createTime,
  };
});
