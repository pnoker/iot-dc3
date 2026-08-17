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
  AgenticAction,
  AgenticAttachment,
  AgenticMessage,
  AgenticMessageContent,
  AgenticMessageContext,
  AgenticSession,
  AgenticTraceEvent,
  AgenticVisualizationSpec,
} from '@/config/types';

type Locale = 'zh' | 'en';
type Localized<T> = Record<Locale, T>;

const text = <T>(zh: T, en: T): Localized<T> => ({zh, en});
const now = new Date();
const at = (daysAgo: number, hour: number, minute: number): string => {
  const value = new Date(now);
  value.setDate(value.getDate() - daysAgo);
  value.setHours(hour, minute, 0, 0);
  return value.toISOString();
};

const chart = (
  id: string,
  type: AgenticVisualizationSpec['type'],
  title: Localized<string>,
  dataset: AgenticVisualizationSpec['dataset'] | Localized<AgenticVisualizationSpec['dataset']>,
  encode: AgenticVisualizationSpec['encode'],
  meta?: Record<string, unknown>,
): Localized<AgenticVisualizationSpec> => {
  const datasets = Array.isArray(dataset) ? text(dataset, dataset) : dataset;
  return {
    zh: {id, type, title: title.zh, dataset: datasets.zh, encode, meta},
    en: {id, type, title: title.en, dataset: datasets.en, encode, meta},
  };
};

const visualizations = {
  fleet: chart('fleet-health', 'donut', text('设备健康分布', 'Device health distribution'), text([
    {status: '健康', count: 38}, {status: '预警', count: 5}, {status: '离线', count: 3}, {status: '维护中', count: 2},
  ], [
    {status: 'Healthy', count: 38}, {status: 'Warning', count: 5}, {status: 'Offline', count: 3}, {status: 'Maintenance', count: 2},
  ]), {y: 'count', color: 'status'}),
  availability: chart('site-availability', 'bar', text('产线可用率（最近 7 日）', 'Line availability (last 7 days)'), text([
    {line: 'A 线', availability: 99.2}, {line: 'B 线', availability: 97.8}, {line: 'C 线', availability: 94.6}, {line: '公辅', availability: 99.7},
  ], [
    {line: 'Line A', availability: 99.2}, {line: 'Line B', availability: 97.8}, {line: 'Line C', availability: 94.6}, {line: 'Utility', availability: 99.7},
  ]), {x: 'line', y: 'availability'}, {unit: '%'}),
  alarmTrend: chart('alarm-storm-trend', 'area', text('告警风暴时间线', 'Alarm storm timeline'), Array.from({length: 12}, (_, i) => ({
    minute: `${String(9 + Math.floor(i / 6)).padStart(2, '0')}:${String((i % 6) * 10).padStart(2, '0')}`,
    alarms: [2, 4, 7, 19, 43, 68, 51, 31, 14, 8, 5, 3][i],
  })), {x: 'minute', y: 'alarms'}),
  alarmSources: chart('alarm-sources', 'column', text('关联告警来源 Top 5', 'Top 5 correlated alarm sources'), [
    {source: 'FAN-03', count: 47}, {source: 'PLC-C07', count: 28}, {source: 'VFD-03', count: 19}, {source: 'TEMP-03A', count: 14}, {source: 'LINE-C', count: 9},
  ], {x: 'source', y: 'count'}),
  energy: chart('energy-baseline', 'line', text('实际能耗与基线', 'Actual energy vs baseline'), Array.from({length: 24}, (_, hour) => ({
    hour: `${String(hour).padStart(2, '0')}:00`, baseline: Number((22 + 7 * Math.sin((hour - 8) / 4)).toFixed(1)), actual: Number((24 + 9 * Math.sin((hour - 7) / 4) + (hour > 13 && hour < 18 ? 6 : 0)).toFixed(1)),
  })), {x: 'hour', y: 'actual', color: 'series'}, {unit: 'kWh'}),
  energyTop: chart('energy-top', 'bar', text('设备能耗排行', 'Energy consumption by asset'), [
    {asset: 'Compressor-02', kwh: 86.4}, {asset: 'Injection-A', kwh: 72.1}, {asset: 'Fan-03', kwh: 58.7}, {asset: 'Chiller-01', kwh: 45.2}, {asset: 'Pump-07', kwh: 31.8},
  ], {x: 'asset', y: 'kwh'}, {unit: 'kWh'}),
  opcLatency: chart('opc-latency', 'scatter', text('OPC-UA 握手延迟与失败', 'OPC-UA handshake latency and failures'), text(
    Array.from({length: 20}, (_, i) => ({attempt: i + 1, latency: 650 + ((i * 347) % 5100), result: i === 6 || i === 14 || i === 15 ? '失败' : '成功'})),
    Array.from({length: 20}, (_, i) => ({attempt: i + 1, latency: 650 + ((i * 347) % 5100), result: i === 6 || i === 14 || i === 15 ? 'Failed' : 'Success'})),
  ), {x: 'attempt', y: 'latency', color: 'result'}, {unit: 'ms'}),
  coldStorage: chart('cold-storage-temperature', 'line', text('冷库温度与开门事件', 'Cold-room temperature and door events'), Array.from({length: 24}, (_, hour) => ({
    hour: `${String(hour).padStart(2, '0')}:00`, temperature: Number((-18 + Math.sin(hour / 2) * 1.1 + (hour === 10 || hour === 16 ? 4.5 : 0)).toFixed(1)),
  })), {x: 'hour', y: 'temperature'}, {unit: '°C'}),
  vibration: chart('bearing-vibration', 'heatmap', text('电机轴承振动频谱热力图', 'Motor bearing vibration spectrum'), Array.from({length: 28}, (_, i) => ({
    time: `${String(8 + Math.floor(i / 7)).padStart(2, '0')}:00`, band: `${(i % 7 + 1) * 100}Hz`, amplitude: Number((1.2 + ((i * 17) % 31) / 10).toFixed(1)),
  })), {x: 'time', y: 'band', color: 'amplitude'}, {unit: 'mm/s'}),
  rollout: chart('rollout-impact', 'line', text('发布前后通信错误率', 'Communication error rate before and after rollout'), Array.from({length: 18}, (_, i) => ({
    minute: i * 5, errorRate: Number((i < 8 ? 0.3 + (i % 3) * 0.1 : 2.4 - (i - 8) * 0.18).toFixed(2)),
  })), {x: 'minute', y: 'errorRate'}, {unit: '%'}),
  warningRanking: chart('warning-risk-ranking', 'bar', text('预警设备风险评分', 'Warning asset risk scores'), [
    {asset: 'FAN-03', score: 92}, {asset: 'PLC-C07', score: 81}, {asset: 'PUMP-12', score: 63}, {asset: 'METER-09', score: 48}, {asset: 'SENSOR-18', score: 36},
  ], {x: 'asset', y: 'score'}, {unit: 'score'}),
  fanTemperature: chart('fan-03-temperature', 'line', text('FAN-03 轴承温度与阈值', 'FAN-03 bearing temperature and threshold'), Array.from({length: 24}, (_, hour) => ({
    hour: `${String(hour).padStart(2, '0')}:00`, temperature: Number((69 + hour * 0.42 + Math.sin(hour / 2.2) * 2.8 + (hour > 15 ? 3.2 : 0)).toFixed(1)), threshold: 80,
  })), {x: 'hour', y: 'temperature', color: 'series'}, {unit: '°C'}),
  communicationErrors: chart('plc-c07-errors', 'area', text('PLC-C07 通信错误率（最近 60 分钟）', 'PLC-C07 communication error rate (last 60 min)'), Array.from({length: 13}, (_, i) => ({
    minute: `${i * 5}m`, errorRate: Number((0.18 + ((i * 7) % 5) * 0.08 + (i > 6 && i < 10 ? 1.45 : 0)).toFixed(2)),
  })), {x: 'minute', y: 'errorRate'}, {unit: '%'}),
  linkQuality: chart('link-quality-heatmap', 'heatmap', text('通信链路质量热力图', 'Communication link quality heatmap'), Array.from({length: 24}, (_, i) => ({
    time: `${String(8 + Math.floor(i / 6)).padStart(2, '0')}:${String((i % 6) * 10).padStart(2, '0')}`,
    link: ['PLC-C07→SW-07', 'SW-07→GW-02', 'GW-02→DC3'][i % 3], quality: 99 - ((i * 13) % 17),
  })), {x: 'time', y: 'link', color: 'quality'}, {unit: '%'}),
  shiftReadiness: chart('shift-readiness', 'stat', text('本班处置计划', 'Current shift action plan'), text([{
    紧急处置: 1, 计划工单: 2, 观察项: 3, 已排除离线: 3, 预计恢复: '60 分钟', 交接完整度: '100%',
  }], [{
    urgentActions: 1, plannedWorkOrders: 2, watchItems: 3, excludedOffline: 3, estimatedRecovery: '60 min', handoverCompleteness: '100%',
  }]), {}),
  commandPreview: chart('command-preview', 'stat', text('写指令变更预览', 'Write command change preview'), text([{
    目标设备: 'Cold-Storage-02', 当前值: '-18°C', 目标值: '-20°C', 影响点位: 3, 风险: '中等', 回滚: '已就绪',
  }], [{
    target: 'Cold-Storage-02', current: '-18°C', requested: '-20°C', affectedPoints: 3, risk: 'Medium', rollback: 'Ready',
  }]), {}),
};

interface SessionDef {
  id: string;
  title: Localized<string>;
  summary: Localized<string>;
  icon: 'monitor' | 'warning' | 'trend' | 'connection' | 'odometer' | 'tools' | 'operation' | 'lightning';
  category: string;
  model: string;
  reasoning: boolean;
  daysAgo: number;
  hour: number;
}

const sessionDefs: SessionDef[] = [
  {id: 'aa000001-0000-4000-8000-000000000001', title: text('全厂设备健康巡检', 'Plant-wide device health review'), summary: text('48 台设备健康度、离线与维护建议', 'Health, offline status, and maintenance advice for 48 assets'), icon: 'monitor', category: 'health', model: 'gpt-4o', reasoning: false, daysAgo: 0, hour: 9},
  {id: 'aa000002-0000-4000-8000-000000000002', title: text('C 线告警风暴根因分析', 'Line C alarm storm root-cause analysis'), summary: text('117 条关联告警压缩为 1 个根因事件', '117 correlated alarms reduced to one root cause'), icon: 'warning', category: 'alarm', model: 'deepseek-reasoner', reasoning: true, daysAgo: 0, hour: 8},
  {id: 'aa000003-0000-4000-8000-000000000003', title: text('空压站能耗异常优化', 'Compressed-air energy anomaly optimization'), summary: text('对比能耗基线并估算节能空间', 'Compare energy baseline and estimate savings'), icon: 'trend', category: 'energy', model: 'deepseek-reasoner', reasoning: true, daysAgo: 1, hour: 16},
  {id: 'aa000004-0000-4000-8000-000000000004', title: text('OPC-UA 证书与掉线诊断', 'OPC-UA certificate and disconnect diagnosis'), summary: text('证书链、握手延迟和 SecureChannel 排查', 'Certificate chain, latency, and SecureChannel diagnosis'), icon: 'connection', category: 'connectivity', model: 'claude-3-5-sonnet', reasoning: true, daysAgo: 1, hour: 11},
  {id: 'aa000005-0000-4000-8000-000000000005', title: text('2 号冷库温控 SLA 复盘', 'Cold Room 2 temperature SLA review'), summary: text('越界时段、开门事件与补偿策略', 'Excursions, door events, and compensation strategy'), icon: 'odometer', category: 'sla', model: 'gpt-4o', reasoning: false, daysAgo: 2, hour: 14},
  {id: 'aa000006-0000-4000-8000-000000000006', title: text('电机 M-104 预测性维护', 'Motor M-104 predictive maintenance'), summary: text('振动频谱异常及轴承剩余寿命评估', 'Vibration anomaly and bearing remaining-life estimate'), icon: 'tools', category: 'maintenance', model: 'claude-3-5-sonnet', reasoning: true, daysAgo: 3, hour: 10},
  {id: 'aa000007-0000-4000-8000-000000000007', title: text('Modbus 驱动发布影响评估', 'Modbus driver rollout impact assessment'), summary: text('灰度发布后错误率与回滚决策', 'Error rate and rollback decision after canary rollout'), icon: 'operation', category: 'change', model: 'deepseek-chat', reasoning: false, daysAgo: 4, hour: 17},
  {id: 'aa000008-0000-4000-8000-000000000008', title: text('冷库设定值安全下发', 'Safe cold-room setpoint command'), summary: text('写指令影响分析、审批与回滚方案', 'Write impact analysis, approval, and rollback plan'), icon: 'lightning', category: 'command', model: 'gpt-4o', reasoning: false, daysAgo: 5, hour: 15},
];

export const agenticSessions: AgenticSession[] = sessionDefs.map((session, index) => ({
  conversationId: session.id,
  title: session.title.zh,
  titleI18n: session.title,
  summaryI18n: session.summary,
  sessionExt: {
    model: session.model,
    reasoningEnabled: session.reasoning,
    temperature: session.reasoning ? 0.2 : 0.6,
    maxTokens: 8192,
    icon: session.icon,
    category: session.category,
  },
  createTime: at(session.daysAgo + 1, session.hour, 5),
  operateTime: index === 0 ? at(0, 10, 24) : at(session.daysAgo, session.hour, 42 - (index % 4) * 4),
}));

interface AssistantOptions {
  reasoning?: Localized<string>;
  tools?: string[];
  traces?: Localized<AgenticTraceEvent[]>;
  contexts?: Localized<AgenticMessageContext[]>;
  charts?: Array<Localized<AgenticVisualizationSpec>>;
  tokens?: AgenticMessageContent['tokens'];
  finishReason?: string;
  recovered?: boolean;
}

interface ConversationTurn {
  user: Localized<string>;
  assistant: Localized<string>;
  options: AssistantOptions;
}

const trace = (name: string, title: Localized<string>, detail: Localized<string>): Localized<AgenticTraceEvent[]> => ({
  zh: [
    {type: 'tool', name, title: title.zh, phase: 'start', status: 'running'},
    {type: 'reasoning', title: '分析上下文', detail: detail.zh, status: 'success'},
    {type: 'tool', name, title: title.zh, detail: '查询成功，返回数据已校验', phase: 'result', status: 'success'},
  ],
  en: [
    {type: 'tool', name, title: title.en, phase: 'start', status: 'running'},
    {type: 'reasoning', title: 'Analyze context', detail: detail.en, status: 'success'},
    {type: 'tool', name, title: title.en, detail: 'Query completed and results validated', phase: 'result', status: 'success'},
  ],
});

const ctx = (zh: string, en: string, type: AgenticMessageContext['type'] = 'tool'): Localized<AgenticMessageContext[]> => ({
  zh: [{type, content: zh}],
  en: [{type, content: en}],
});

const conversations: ConversationTurn[][] = [
  [
    {user: text('做一次全厂设备健康巡检，优先告诉我现在需要处理什么。', 'Run a plant-wide device health review and prioritize what needs attention now.'), assistant: text('已检查 48 台设备和 11 个驱动：**38 台健康、5 台预警、3 台离线、2 台维护中**。当前优先级最高的是 C 线风机 FAN-03 轴承温度上升，其次是 PLC-C07 通信抖动。离线的 3 台设备均位于计划停机区域，无新增生产影响。', 'I checked 48 assets and 11 drivers: **38 healthy, 5 warning, 3 offline, and 2 under maintenance**. FAN-03 bearing temperature is the highest priority, followed by PLC-C07 communication jitter. All three offline assets are in a planned shutdown area, with no new production impact.'), options: {tools: ['device_status_list', 'driver_status_list', 'alarm_active_list'], traces: trace('device_status_list', text('查询全厂设备状态', 'Query plant device status'), text('交叉检查在线状态、活跃告警和维护窗口', 'Cross-check online state, active alarms, and maintenance windows')), contexts: ctx('范围：华东一厂；时间窗：最近 30 分钟；排除：计划维护设备', 'Scope: East China Plant 1; window: last 30 minutes; excluded: planned maintenance'), charts: [visualizations.fleet, visualizations.availability], tokens: {input: 2240, output: 968, context: 1180, system: 320}}},
    {user: text('把 FAN-03 和 PLC-C07 的处理步骤排成值班清单。', 'Turn the FAN-03 and PLC-C07 actions into an on-call checklist.'), assistant: text('值班清单已按风险排序：\n\n1. **FAN-03（立即）**：确认润滑油位与散热风道，负载降至 75%，15 分钟后复测。\n2. **PLC-C07（30 分钟内）**：检查交换机 7 号端口丢包，核对 Modbus 超时与重试配置。\n3. **复核（1 小时后）**：温度应低于 78°C，通信错误率应低于 0.5%；否则升级为 P1。', 'On-call checklist, ordered by risk:\n\n1. **FAN-03 (now):** verify lubrication and cooling airflow, reduce load to 75%, and recheck in 15 minutes.\n2. **PLC-C07 (within 30 min):** inspect packet loss on switch port 7 and verify Modbus timeout/retry settings.\n3. **Review (after 1 hour):** temperature should be below 78°C and communication errors below 0.5%; otherwise escalate to P1.'), options: {tools: ['maintenance_window_get', 'runbook_search'], traces: trace('runbook_search', text('检索设备处置手册', 'Search asset runbooks'), text('将设备风险映射到可执行检查项和升级阈值', 'Map risks to executable checks and escalation thresholds')), contexts: ctx('知识库：设备运维手册 v4.2 / 值班升级策略 2026-Q3', 'Knowledge base: Operations Runbook v4.2 / On-call Escalation Policy 2026-Q3', 'retrieval'), tokens: {input: 1870, output: 734, memory: 210}}},
    {user: text('5 台预警设备都展开一下，按风险和生产影响排序。', 'Expand all five warning assets and rank them by risk and production impact.'), assistant: text('综合告警等级、趋势斜率、设备关键度和冗余能力后，风险排序如下：\n\n1. **FAN-03 · 92 分 · P1**：轴承温度 4 小时上升 8.7°C，无同位备用风机。\n2. **PLC-C07 · 81 分 · P1**：通信错误率峰值 2.1%，影响 C 线 14 个采集点。\n3. **PUMP-12 · 63 分 · P2**：出口压力波动，但并联泵可接管。\n4. **METER-09 · 48 分 · P2**：数据延迟 76 秒，不影响控制闭环。\n5. **SENSOR-18 · 36 分 · P3**：电池剩余 18%，预计还能运行 12 天。\n\n建议本班处理前两项，PUMP-12 加入观察，另外两项进入计划维护。', 'After combining alarm severity, trend slope, asset criticality, and redundancy, the ranking is:\n\n1. **FAN-03 · 92 · P1:** bearing temperature rose 8.7°C in four hours with no standby fan.\n2. **PLC-C07 · 81 · P1:** error rate peaked at 2.1%, affecting 14 Line C points.\n3. **PUMP-12 · 63 · P2:** discharge pressure fluctuates, but a parallel pump can take over.\n4. **METER-09 · 48 · P2:** data is 76 seconds late and does not affect the control loop.\n5. **SENSOR-18 · 36 · P3:** battery is at 18%, with about 12 days remaining.\n\nHandle the first two this shift, watch PUMP-12, and move the other two to planned maintenance.'), options: {reasoning: text('风险评分由严重度 30%、趋势 25%、关键度 25%、冗余与可恢复性 20% 加权，避免仅按告警数量排序。', 'Risk score weights severity 30%, trend 25%, criticality 25%, and redundancy/recoverability 20%, avoiding a simple alarm-count ranking.'), tools: ['asset_criticality_get', 'alarm_trend_score', 'redundancy_check', 'risk_rank'], traces: trace('risk_rank', text('计算设备风险优先级', 'Calculate asset risk priority'), text('对 5 台预警设备执行多维风险评分并校验生产拓扑', 'Score five warning assets across multiple dimensions and validate against production topology')), contexts: ctx('评分策略：OPS-RISK-v3；生产窗口：本班 08:00–20:00；数据新鲜度：小于 45 秒', 'Scoring policy: OPS-RISK-v3; production window: 08:00–20:00; data freshness: under 45 seconds', 'system'), charts: [visualizations.warningRanking, visualizations.fleet], tokens: {input: 4180, output: 1360, context: 2420, system: 340, memory: 280}}},
    {user: text('先下钻 FAN-03，给我完整证据链，不要只给结论。', 'Drill into FAN-03 first and give me the full evidence chain, not just the conclusion.'), assistant: text('FAN-03 的证据链已闭合：\n\n- **现象**：驱动端轴承温度从 70.6°C 升至 82.9°C，16:00 后斜率明显加快。\n- **关联信号**：电机电流均值上升 11%，300–400 Hz 振动能量增加 37%，但环境温度仅变化 1.2°C。\n- **排除项**：温度传感器与备用探头偏差仅 0.3°C；风道压差正常；不存在批量测点漂移。\n- **最可能原因**：润滑状态恶化叠加持续高负载，置信度 **0.87**。\n- **安全边界**：当前低于 86°C 联锁值，但按现有斜率约 95 分钟后可能触发停机。\n\n建议立即降载至 75%，现场检查油位和轴承异响，并在 15 分钟后重新评估趋势。', 'The FAN-03 evidence chain is complete:\n\n- **Symptom:** drive-end bearing temperature rose from 70.6°C to 82.9°C, accelerating after 16:00.\n- **Correlated signals:** mean motor current increased 11% and 300–400 Hz vibration energy rose 37%, while ambient temperature moved only 1.2°C.\n- **Ruled out:** the primary and backup probes differ by only 0.3°C, duct differential pressure is normal, and there is no fleet-wide sensor drift.\n- **Most likely cause:** degrading lubrication combined with sustained high load, confidence **0.87**.\n- **Safety boundary:** still below the 86°C interlock, but the current slope could trigger a stop in about 95 minutes.\n\nReduce load to 75% now, inspect oil level and bearing noise, and reassess the trend after 15 minutes.'), options: {reasoning: text('将温度趋势与电流、振动、环境温度和备用探头做时序对齐；排除环境与传感器偏差后，润滑退化解释力最高。', 'Time-align temperature with current, vibration, ambient temperature, and the backup probe. After excluding environmental and sensor bias, lubrication degradation has the strongest explanatory power.'), tools: ['point_value_multiseries', 'sensor_peer_compare', 'vibration_feature_get', 'failure_mode_rank'], traces: trace('failure_mode_rank', text('构建设备故障证据链', 'Build asset fault evidence chain'), text('交叉验证 6 类信号、排除 3 个替代假设并计算原因置信度', 'Cross-validate six signal groups, rule out three alternatives, and calculate cause confidence')), contexts: {zh: [{type: 'tool', content: '数据窗口：最近 24 小时；采样粒度：1 分钟；有效点 1,440 / 1,440'}, {type: 'retrieval', content: '知识库：FAN-03 FMEA v2.7；相似工单 WO-2026-0418、WO-2026-0522'}], en: [{type: 'tool', content: 'Window: last 24 hours; resolution: 1 minute; valid samples: 1,440 / 1,440'}, {type: 'retrieval', content: 'Knowledge base: FAN-03 FMEA v2.7; similar work orders WO-2026-0418 and WO-2026-0522'}]}, charts: [visualizations.fanTemperature, visualizations.vibration, visualizations.alarmSources], tokens: {input: 6820, output: 2140, text: 1440, context: 3760, memory: 610}}},
    {user: text('PLC-C07 为什么抖动？把查询失败和兜底过程也保留下来。', 'Why is PLC-C07 unstable? Keep the failed query and fallback process in the record.'), assistant: text('实时拓扑工具第一次查询超时，系统没有据此直接判断，而是切换到缓存拓扑和交换机历史指标完成兜底。结果显示：\n\n- SW-07 的 7 号端口在 35–50 分钟前出现 **1.8%–2.3% 丢包**，与 PLC-C07 重试峰值一致。\n- GW-02 到 DC3 链路质量稳定在 98% 以上，平台侧和网关侧可以排除。\n- 端口错误集中在 CRC，且只影响一条物理链路，更符合接头松动或屏蔽接地问题。\n\n当前采集已恢复，但问题具有复发性。建议 30 分钟内检查端口与网线，同时把 Modbus 重试间隔从 200 ms 临时调至 350 ms；未经确认不会修改配置。', 'The live topology tool timed out on the first query. Instead of drawing a conclusion from the failure, the system fell back to cached topology and historical switch metrics. The evidence shows:\n\n- Switch port 7 on SW-07 had **1.8%–2.3% packet loss** 35–50 minutes ago, aligned with the PLC-C07 retry peak.\n- The GW-02 to DC3 link stayed above 98% quality, ruling out the platform and gateway segments.\n- Errors are CRC-heavy and isolated to one physical link, consistent with a loose connector or shielding/grounding issue.\n\nCollection has recovered but recurrence is likely. Inspect the port and cable within 30 minutes and temporarily raise the Modbus retry interval from 200 ms to 350 ms; no configuration will change without approval.'), options: {reasoning: text('工具失败被记录为证据缺口，随后使用 5 分钟前缓存拓扑和交换机 24 小时历史补齐；多段链路对比后故障域收敛到 SW-07 端口 7。', 'Record the tool failure as an evidence gap, then use five-minute cached topology and 24-hour switch history. Segment comparison narrows the fault domain to SW-07 port 7.'), tools: ['topology_live_get', 'topology_cache_get', 'switch_port_metrics', 'driver_retry_stats'], traces: {zh: [{type: 'tool', name: 'topology_live_get', title: '读取实时通信拓扑', phase: 'error', status: 'failed', code: 'MCP_TIMEOUT', detail: '3 秒内未返回，已中止本次调用'}, {type: 'reasoning', title: '选择只读兜底数据源', detail: '改用 5 分钟前缓存拓扑与交换机历史指标，不执行任何写操作', status: 'success'}, {type: 'tool', name: 'topology_cache_get', title: '读取缓存拓扑', phase: 'result', status: 'success', detail: '返回 18 个节点与 21 条链路'}, {type: 'tool', name: 'switch_port_metrics', title: '查询交换机端口指标', phase: 'result', status: 'success', detail: '定位 SW-07 端口 7 CRC 与丢包异常'}], en: [{type: 'tool', name: 'topology_live_get', title: 'Read live communication topology', phase: 'error', status: 'failed', code: 'MCP_TIMEOUT', detail: 'No response within three seconds; call aborted'}, {type: 'reasoning', title: 'Select read-only fallback sources', detail: 'Use five-minute cached topology and historical switch metrics; perform no writes', status: 'success'}, {type: 'tool', name: 'topology_cache_get', title: 'Read cached topology', phase: 'result', status: 'success', detail: 'Returned 18 nodes and 21 links'}, {type: 'tool', name: 'switch_port_metrics', title: 'Query switch port metrics', phase: 'result', status: 'success', detail: 'Located CRC and packet-loss anomalies on SW-07 port 7'}]}, contexts: ctx('兜底数据：拓扑缓存版本 2026.08.12-0915；交换机 SNMP 指标完整度 99.96%', 'Fallback data: topology cache 2026.08.12-0915; switch SNMP metric completeness 99.96%', 'memory'), charts: [visualizations.communicationErrors, visualizations.linkQuality], tokens: {input: 5470, output: 1860, context: 2940, memory: 380}, recovered: true}},
    {user: text('整理成本班交接摘要，并生成工单草案让我确认。', 'Prepare the shift handover summary and draft the work orders for my approval.'), assistant: text('### 本班健康巡检结论\n\n48 台设备与 11 个驱动已完成巡检，当前无立即停线风险。已将 5 台预警设备收敛为 **1 项紧急处置、2 张计划工单、3 项持续观察**。\n\n**立即处置 · FAN-03**\n- 负载降至 75%，检查润滑油位与轴承异响。\n- 15 分钟后复测；温度应低于 78°C，升温斜率应小于 0.15°C/min。\n- 若温度达到 86°C 或振动超过 7.1 mm/s，按联锁预案停机。\n\n**工单草案**\n1. `WO-DRAFT-1042`：检查 FAN-03 润滑与驱动端轴承，优先级 P1，预计 45 分钟。\n2. `WO-DRAFT-1043`：检查 SW-07 端口 7、屏蔽接地与 PLC-C07 网线，优先级 P1，预计 30 分钟。\n\n**观察与交接**\n- PUMP-12 压力波动每 10 分钟复核；METER-09 延迟与 SENSOR-18 电量转入计划维护。\n- 3 台离线设备均属于计划停机，已核对维护窗口。\n- 所有证据、工具失败记录、兜底来源和验收阈值已附入交接包。\n\n两张工单目前只是草案，**确认后才会正式创建并通知值班人员**。', '### Shift health-review conclusion\n\nThe review of 48 assets and 11 drivers is complete, with no immediate line-stop risk. Five warnings are now reduced to **one urgent action, two planned work orders, and three watch items**.\n\n**Immediate action · FAN-03**\n- Reduce load to 75%; inspect lubrication level and bearing noise.\n- Recheck after 15 minutes; temperature should be below 78°C and slope below 0.15°C/min.\n- Stop under the interlock plan if temperature reaches 86°C or vibration exceeds 7.1 mm/s.\n\n**Draft work orders**\n1. `WO-DRAFT-1042`: inspect FAN-03 lubrication and drive-end bearing, P1, estimated 45 minutes.\n2. `WO-DRAFT-1043`: inspect SW-07 port 7, shield grounding, and the PLC-C07 cable, P1, estimated 30 minutes.\n\n**Watch and handover**\n- Review PUMP-12 pressure every ten minutes; move METER-09 latency and SENSOR-18 battery to planned maintenance.\n- All three offline assets are covered by confirmed maintenance windows.\n- The handover package includes evidence, failed tool records, fallback sources, and acceptance thresholds.\n\nBoth work orders remain drafts and **will only be created and assigned after approval**.'), options: {reasoning: text('将诊断结论转换为带责任对象、优先级、工时、验收阈值和升级条件的交接结构；所有写操作保持待确认。', 'Convert diagnosis into a handover structure with owners, priorities, estimates, acceptance thresholds, and escalation conditions; keep every write operation pending approval.'), tools: ['handover_package_build', 'work_order_draft', 'assignee_availability_check', 'approval_policy_get'], traces: trace('handover_package_build', text('生成交接包与工单草案', 'Build handover package and work-order drafts'), text('汇总证据链、处置步骤、验收标准、人员可用性和审批策略', 'Combine evidence, actions, acceptance criteria, staff availability, and approval policy')), contexts: {zh: [{type: 'retrieval', content: '模板：SHIFT-HANDOVER-v5；工单策略：P1 双人复核；通知组：C 线值班组'}, {type: 'system', content: '安全约束：仅创建草案；不得自动下发设备指令或修改驱动配置'}], en: [{type: 'retrieval', content: 'Template: SHIFT-HANDOVER-v5; work-order policy: P1 four-eyes review; notification group: Line C on-call'}, {type: 'system', content: 'Safety constraint: create drafts only; do not dispatch device commands or modify driver configuration'}]}, charts: [visualizations.shiftReadiness, visualizations.warningRanking, visualizations.availability], tokens: {input: 7340, output: 2480, context: 4020, system: 460, memory: 720}}},
  ],
  [
    {user: text('C 线 9 点后突然刷了很多告警，帮我降噪并定位根因。', 'Line C started flooding alarms after 09:00. Reduce the noise and find the root cause.'), assistant: text('09:20–10:10 共出现 **117 条告警**。按设备、时间和因果关系聚类后，可压缩为 1 个根因事件：**FAN-03 变频器过流导致 PLC-C07 通信重试，继而触发 4 级联动告警**。其余 93 条属于重复或派生告警，建议暂时抑制 30 分钟。', 'There were **117 alarms** from 09:20–10:10. Clustering by asset, time, and causality reduces them to one root event: **FAN-03 inverter overcurrent caused PLC-C07 retries, triggering four cascaded alarms**. The other 93 are duplicates or derived alarms and can be suppressed for 30 minutes.'), options: {reasoning: text('先按 90 秒时间窗建立事件簇，再利用设备拓扑验证上游故障；FAN-03 过流最早出现，且能解释后续通信与温度告警。', 'Build 90-second event clusters, then validate upstream faults against topology. FAN-03 overcurrent occurred first and explains the subsequent communication and temperature alarms.'), tools: ['alarm_cluster', 'topology_path', 'alarm_rule_evaluate'], traces: trace('alarm_cluster', text('聚类关联告警', 'Cluster correlated alarms'), text('117 条原始告警归并为 24 个事件，其中 1 个根因事件', '117 raw alarms reduced to 24 events with one root event')), contexts: ctx('规则：ALM-C-OVERCURRENT-07；拓扑：VFD-03 → FAN-03 → PLC-C07', 'Rule: ALM-C-OVERCURRENT-07; topology: VFD-03 → FAN-03 → PLC-C07'), charts: [visualizations.alarmTrend, visualizations.alarmSources], tokens: {input: 4860, output: 1410, context: 2790, memory: 420}}},
    {user: text('先不要改规则，给我一份人工确认点。', 'Do not change the rule yet. Give me manual verification points.'), assistant: text('建议人工确认 4 点：VFD-03 故障码是否为 OC2；FAN-03 电机电流是否持续超过 46A；交换机端口是否在同一时段出现丢包；复位变频器后级联告警是否在 90 秒内消退。全部满足后再启用临时抑制，避免掩盖真实故障。', 'Verify four items manually: whether VFD-03 reports OC2; whether FAN-03 current stays above 46A; whether switch packet loss occurs in the same window; and whether cascaded alarms clear within 90 seconds after the inverter reset. Only then enable temporary suppression to avoid hiding a real fault.'), options: {tools: ['alarm_evidence_bundle'], traces: trace('alarm_evidence_bundle', text('生成告警证据包', 'Build alarm evidence bundle'), text('保留原始时间线、设备快照和规则命中记录', 'Preserve the raw timeline, asset snapshots, and rule matches')), tokens: {input: 1530, output: 620}}},
  ],
  [
    {user: text('空压站昨天能耗为什么超出基线 18%？', 'Why did the compressor station exceed its energy baseline by 18% yesterday?'), assistant: text('偏差主要来自 **Compressor-02 在 14:00–18:00 长时间卸载运行**：产气量下降但电机仍维持 72% 功率，额外消耗约 31.6 kWh。第二因素是主管网压力设定从 0.72 提高到 0.78 MPa，贡献约 11.2 kWh。', 'The main deviation came from **Compressor-02 running unloaded from 14:00–18:00**: air output dropped while the motor remained at 72% power, adding about 31.6 kWh. A secondary factor was the header pressure setpoint increasing from 0.72 to 0.78 MPa, adding about 11.2 kWh.'), options: {reasoning: text('对齐产气量、功率、加载状态和压力设定，使用同温度生产日作为基线，排除环境温度影响。', 'Align flow, power, loading state, and pressure setpoint; use production days with comparable ambient temperature as the baseline.'), tools: ['point_value_aggregate', 'energy_baseline_compare'], traces: trace('energy_baseline_compare', text('对比能耗基线', 'Compare energy baseline'), text('实际 284.7 kWh，基线 241.3 kWh，偏差 18.0%', 'Actual 284.7 kWh vs baseline 241.3 kWh, an 18.0% deviation')), contexts: ctx('基线模型：weekday-production-v3；置信度：0.91', 'Baseline model: weekday-production-v3; confidence: 0.91', 'memory'), charts: [visualizations.energy, visualizations.energyTop], tokens: {input: 3520, output: 1160, context: 1980}}},
    {user: text('给一个不影响产量的优化方案。', 'Suggest an optimization plan that will not affect output.'), assistant: text('建议先做无停机优化：把主管压力恢复到 0.72 MPa；将 Compressor-02 卸载超过 8 分钟时自动休眠；由 Compressor-01 承担基载、02 号机调峰。预计日节电 **36–42 kWh（12%–14%）**，并设置压力低于 0.68 MPa 自动回退。', 'Start with no-downtime optimization: restore header pressure to 0.72 MPa; sleep Compressor-02 after more than 8 minutes unloaded; use Compressor-01 for base load and 02 for peaks. Expected savings are **36–42 kWh/day (12%–14%)**, with automatic rollback if pressure drops below 0.68 MPa.'), options: {tools: ['simulation_run', 'savings_estimate'], traces: trace('simulation_run', text('运行供气策略仿真', 'Run air-supply strategy simulation'), text('验证压力下限、产气需求和故障回退边界', 'Validate pressure floor, air demand, and failure rollback boundaries')), tokens: {input: 2490, output: 910}}},
  ],
  [
    {user: text('OPC-UA 驱动每小时掉线一次，检查证书和握手过程。', 'The OPC-UA driver disconnects hourly. Check certificates and the handshake flow.'), assistant: text('第一次读取服务端证书链时返回 **Bad_SecurityChecksFailed**，随后通过备用端点完成采集。根因是中间证书将在 6 天后到期，且 10:00 自动轮换任务造成约 5.6 秒握手延迟，超过当前 5 秒超时。建议先更新证书链，再把握手超时临时调至 8 秒。', 'The first certificate-chain read returned **Bad_SecurityChecksFailed**, then collection succeeded through the fallback endpoint. The intermediate certificate expires in six days, and the 10:00 rotation job pushes handshake latency to about 5.6 seconds, beyond the current 5-second timeout. Update the chain first, then temporarily raise the handshake timeout to 8 seconds.'), options: {reasoning: text('对比成功与失败握手发现失败集中在证书轮换窗口，网络 RTT 正常，因此排除常规链路抖动。', 'Successful and failed handshakes differ only around the certificate rotation window; network RTT is normal, ruling out ordinary link jitter.'), tools: ['opcua_certificate_get', 'driver_log_search', 'network_latency_probe'], traces: {zh: [{type: 'tool', name: 'opcua_certificate_get', title: '读取 OPC-UA 证书链', phase: 'error', status: 'failed', code: 'Bad_SecurityChecksFailed', detail: '主端点证书校验失败'}, {type: 'tool', name: 'driver_log_search', title: '检索驱动握手日志', phase: 'result', status: 'success', detail: '备用端点返回 20 条握手记录'}, {type: 'reasoning', title: '关联轮换任务', detail: '失败时间与每小时证书轮换窗口完全重合', status: 'success'}], en: [{type: 'tool', name: 'opcua_certificate_get', title: 'Read OPC-UA certificate chain', phase: 'error', status: 'failed', code: 'Bad_SecurityChecksFailed', detail: 'Certificate validation failed on the primary endpoint'}, {type: 'tool', name: 'driver_log_search', title: 'Search driver handshake logs', phase: 'result', status: 'success', detail: 'Fallback endpoint returned 20 handshake records'}, {type: 'reasoning', title: 'Correlate rotation job', detail: 'Failures align exactly with the hourly certificate rotation window', status: 'success'}]}, contexts: ctx('附件：opcua-handshake-20260811.log（2.4 MB），已解析 1,286 行', 'Attachment: opcua-handshake-20260811.log (2.4 MB), 1,286 lines parsed', 'attachment'), charts: [visualizations.opcLatency], tokens: {input: 6240, output: 1720, text: 1286, context: 3320}}},
    {user: text('生成变更建议，但先不要执行。', 'Prepare a change proposal, but do not execute it.'), assistant: text('已生成两阶段方案：先替换中间证书并验证双端信任，再将握手超时从 5 秒改为 8 秒。观察 2 小时后若失败率仍高于 0.5%，自动恢复旧证书和超时。方案仅保存为草稿，未执行任何变更。', 'Prepared a two-stage plan: replace the intermediate certificate and validate mutual trust, then change the handshake timeout from 5 to 8 seconds. If failures remain above 0.5% after two hours, restore the old certificate and timeout automatically. The plan is saved as a draft; no changes were executed.'), options: {tools: ['change_plan_create'], traces: trace('change_plan_create', text('生成可回滚变更计划', 'Create rollback-capable change plan'), text('包含前置检查、分阶段执行、观测指标与自动回滚', 'Includes prechecks, phased execution, observability, and automatic rollback')), tokens: {input: 2010, output: 770}}},
  ],
  [
    {user: text('复盘 2 号冷库昨天的温控 SLA，为什么越界？', 'Review yesterday\'s temperature SLA for Cold Room 2. Why did it breach?'), assistant: text('SLA 要求 -22°C 至 -15°C，昨天合规率 **98.6%**，累计越界 20 分钟。两段峰值分别发生在 10:00 和 16:00，均与装卸开门事件重合；压缩机和蒸发器运行正常，不是制冷故障。', 'The SLA range is -22°C to -15°C. Yesterday\'s compliance was **98.6%**, with 20 minutes out of range. Peaks at 10:00 and 16:00 align with loading-door events; the compressor and evaporator were normal, so this was not a refrigeration fault.'), options: {tools: ['sla_calculate', 'door_event_list', 'point_value_list'], traces: trace('sla_calculate', text('计算温控 SLA', 'Calculate temperature SLA'), text('将分钟级温度与门禁事件、压缩机状态对齐', 'Align minute-level temperature with door events and compressor state')), contexts: ctx('SLA：COLD-CHAIN-L2；允许范围：-22°C ~ -15°C；最大连续越界：15 分钟', 'SLA: COLD-CHAIN-L2; allowed range: -22°C to -15°C; maximum continuous excursion: 15 minutes'), charts: [visualizations.coldStorage], tokens: {input: 3180, output: 980, context: 1770}}},
    {user: text('下次装卸如何避免？', 'How can we prevent this during the next loading operation?'), assistant: text('建议装卸前 20 分钟预冷至 -20°C；单次开门控制在 6 分钟内；门开超过 4 分钟时自动提升风机至 85%；关门后恢复标准策略。按昨日工况回放，预计可把越界从 20 分钟降到 4 分钟。', 'Pre-cool to -20°C for 20 minutes before loading; keep each door opening under six minutes; raise fan speed to 85% after four minutes open; restore the standard policy after closing. Replaying yesterday\'s conditions cuts the predicted excursion from 20 to four minutes.'), options: {tools: ['cold_chain_replay'], traces: trace('cold_chain_replay', text('回放冷链控制策略', 'Replay cold-chain control strategy'), text('使用昨日环境与装卸事件模拟预冷和风机补偿', 'Simulate pre-cooling and fan compensation using yesterday\'s environment and loading events')), tokens: {input: 1750, output: 690}}},
  ],
  [
    {user: text('分析电机 M-104 最近一周振动，是否需要停机？', 'Analyze Motor M-104 vibration over the last week. Does it need to stop?'), assistant: text('振动 RMS 从 2.8 升至 **5.1 mm/s**，主要能量集中在 300 Hz–400 Hz，与驱动端轴承外圈缺陷特征一致。当前未达到 7.1 mm/s 停机阈值，但趋势加速，估算剩余安全运行窗口 **9–14 天**。建议 7 天内安排 2 小时维护窗口。', 'Vibration RMS rose from 2.8 to **5.1 mm/s**, concentrated at 300–400 Hz, matching a drive-end bearing outer-race defect. It has not reached the 7.1 mm/s shutdown threshold, but the trend is accelerating; estimated safe operating window is **9–14 days**. Schedule a two-hour maintenance window within seven days.'), options: {reasoning: text('先去除转速变化造成的频率漂移，再比较 1X、2X 和轴承故障频带；高频包络增长更符合轴承外圈缺陷。', 'Remove frequency drift from speed changes, then compare 1X, 2X, and bearing fault bands; high-frequency envelope growth best matches an outer-race defect.'), tools: ['vibration_spectrum', 'maintenance_history_get', 'remaining_life_estimate'], traces: trace('vibration_spectrum', text('分析振动频谱', 'Analyze vibration spectrum'), text('识别 300–400 Hz 持续增强的故障频带', 'Identify a persistently growing fault band at 300–400 Hz')), contexts: ctx('模型：bearing-rul-v2.1；历史工单：2026-05-18 更换联轴器', 'Model: bearing-rul-v2.1; work order history: coupling replaced on 2026-05-18', 'memory'), charts: [visualizations.vibration], tokens: {input: 5120, output: 1380, context: 2640, memory: 390}}},
    {user: text('把依据和备件一起列出来。', 'List the evidence and required spare parts.'), assistant: text('依据：振动 RMS 连续 6 天上升、外圈故障频带增强 72%、温度同比上升 6.4°C。备件：6309-2RS 轴承 2 件、耐高温润滑脂 1 支、轴封 1 套；库存均可满足。建议同时准备激光对中仪，避免更换后联轴器偏心。', 'Evidence: vibration RMS increased for six consecutive days, the outer-race fault band rose 72%, and temperature increased 6.4°C year over year. Parts: two 6309-2RS bearings, one high-temperature grease cartridge, and one shaft-seal kit; all are in stock. Also prepare a laser alignment tool to avoid coupling eccentricity after replacement.'), options: {tools: ['spare_inventory_check', 'maintenance_evidence_export'], traces: trace('spare_inventory_check', text('核对备件库存', 'Check spare-parts inventory'), text('3 类备件库存充足，最近库位已确认', 'All three part types are available and nearest stock locations confirmed')), tokens: {input: 2090, output: 810}}},
  ],
  [
    {user: text('评估昨晚 Modbus 驱动灰度发布的影响。', 'Assess the impact of last night\'s Modbus driver canary rollout.'), assistant: text('v3.3.1 灰度覆盖 20% 设备后，通信错误率从 0.4% 升至 **2.4%**，集中在固件低于 2.8 的 7 台仪表。自动降级到兼容帧格式后已回落到 0.6%，数据缺口均通过补采恢复。建议暂停扩大灰度，不必全量回滚。', 'After v3.3.1 reached 20% of assets, communication errors rose from 0.4% to **2.4%**, concentrated on seven meters with firmware below 2.8. Automatic fallback to the compatible frame format reduced errors to 0.6%, and backfill recovered all data gaps. Pause rollout expansion; a full rollback is unnecessary.'), options: {tools: ['deployment_compare', 'driver_error_rate', 'data_gap_verify'], traces: trace('deployment_compare', text('对比发布前后指标', 'Compare pre/post rollout metrics'), text('按版本、设备固件和帧格式切分错误率', 'Segment error rates by version, device firmware, and frame format')), contexts: ctx('发布批次：CANARY-20260807-01；目标：20%；自动回退：已触发', 'Deployment: CANARY-20260807-01; target: 20%; automatic fallback: triggered'), charts: [visualizations.rollout], tokens: {input: 3650, output: 1050, context: 2050}}},
    {user: text('下一步怎么灰度更安全？', 'How should we continue the rollout safely?'), assistant: text('先把固件 <2.8 的设备排除并单独建兼容组；下一批从 20% 扩到 35%，观察 60 分钟。门禁条件：错误率 <0.8%、P95 延迟 <220 ms、数据完整率 >99.95%。任一指标连续 5 分钟越界即自动回退。', 'Exclude firmware <2.8 and place those devices in a compatibility group. Expand the next wave from 20% to 35% and observe for 60 minutes. Gates: error rate <0.8%, P95 latency <220 ms, and completeness >99.95%. Automatically roll back if any metric breaches for five consecutive minutes.'), options: {tools: ['rollout_plan_validate'], traces: trace('rollout_plan_validate', text('校验灰度门禁', 'Validate rollout gates'), text('检查目标分组、观测窗口、成功标准和回滚条件', 'Check target cohort, observation window, success criteria, and rollback conditions')), tokens: {input: 1920, output: 740}}},
  ],
  [
    {user: text('把 2 号冷库设定温度从 -18 调到 -20，执行前先做安全检查。', 'Change Cold Room 2 setpoint from -18 to -20, but run safety checks before execution.'), assistant: text('安全检查完成：指令将影响设定温度、压缩机目标和除霜补偿 3 个点位；当前无除霜任务，压缩机负载 63%，供电正常。主要风险是短时能耗增加约 9%。已生成分步写入和自动回滚方案，**需要人工确认后才会下发**。', 'Safety checks passed: the command affects the setpoint, compressor target, and defrost compensation. No defrost task is active, compressor load is 63%, and power is normal. The main risk is a temporary energy increase of about 9%. A staged write and automatic rollback plan is ready and **will only run after manual confirmation**.'), options: {tools: ['command_preflight', 'device_interlock_check', 'rollback_plan_create'], traces: trace('command_preflight', text('执行写指令预检', 'Run write-command preflight'), text('校验权限、设备互锁、影响点位和回滚值', 'Validate permissions, interlocks, affected points, and rollback values')), contexts: ctx('目标设备：Cold-Storage-02；审批策略：双人复核；有效期：30 分钟', 'Target: Cold-Storage-02; approval policy: four-eyes; expires in 30 minutes', 'system'), charts: [visualizations.commandPreview], tokens: {input: 2890, output: 930, context: 1510}}},
    {user: text('如果 10 分钟内温度下降过快怎么办？', 'What if the temperature drops too quickly within ten minutes?'), assistant: text('回滚条件已加入：10 分钟内降温超过 3°C、吸气压力低于 0.12 MPa，或压缩机电流超过 42A，任一条件满足即恢复 -18°C，并记录完整审计事件。当前仍处于待确认状态。', 'Rollback conditions added: restore -18°C if temperature falls more than 3°C in ten minutes, suction pressure drops below 0.12 MPa, or compressor current exceeds 42A. The full event will be audited. The action remains pending confirmation.'), options: {tools: ['rollback_condition_update'], traces: trace('rollback_condition_update', text('更新自动回滚条件', 'Update automatic rollback conditions'), text('新增温降速率、吸气压力和电流三项保护', 'Add protections for cooling rate, suction pressure, and current')), tokens: {input: 1680, output: 610}}},
  ],
];

export const agenticMessages: AgenticMessage[] = conversations.flatMap((turns, sessionIndex) => {
  const session = sessionDefs[sessionIndex]!;
  return turns.flatMap((turn, turnIndex) => {
    const baseTime = at(session.daysAgo, session.hour, 10 + turnIndex * 14);
    const assistantExt = (locale: Locale): AgenticMessageContent => ({
      tools: turn.options.tools,
      traces: turn.options.traces?.[locale],
      contexts: turn.options.contexts?.[locale],
      charts: turn.options.charts?.map((item) => item[locale]),
      tokens: turn.options.tokens,
      reasoningContent: turn.options.reasoning?.[locale],
      recovered: turn.options.recovered,
    });
    return [
      {
        id: `agentic-${sessionIndex + 1}-${turnIndex + 1}-u`, conversationId: session.id, role: 'user',
        content: turn.user.zh, contentI18n: turn.user, messageIndex: turnIndex * 2, status: 2, streaming: false, createTime: baseTime,
      },
      {
        id: `agentic-${sessionIndex + 1}-${turnIndex + 1}-a`, conversationId: session.id, role: 'assistant', model: session.model,
        content: turn.assistant.zh, contentI18n: turn.assistant, contentExt: assistantExt('zh'),
        contentExtI18n: text(assistantExt('zh'), assistantExt('en')), messageIndex: turnIndex * 2 + 1,
        status: 2, streaming: false, finishReason: turn.options.finishReason ?? 'stop', createTime: baseTime,
      },
    ] as AgenticMessage[];
  });
});

type LocalizedAction = AgenticAction & {
  titleI18n: Localized<string>;
  descriptionI18n: Localized<string>;
};

export const agenticActions: LocalizedAction[] = [
  {
    actionId: 'action-health-work-orders', conversationId: sessionDefs[0]!.id, actionType: 'WORK_ORDER_CREATE',
    title: '创建 2 张 P1 巡检工单', description: '正式创建 FAN-03 轴承检查和 PLC-C07 通信链路检查工单，并通知 C 线值班组。',
    titleI18n: text('创建 2 张 P1 巡检工单', 'Create two P1 inspection work orders'),
    descriptionI18n: text('正式创建 FAN-03 轴承检查和 PLC-C07 通信链路检查工单，并通知 C 线值班组。', 'Create the FAN-03 bearing and PLC-C07 communication inspection work orders and notify the Line C on-call group.'),
    payload: {
      drafts: [
        {code: 'WO-DRAFT-1042', asset: 'FAN-03', priority: 'P1', estimateMinutes: 45},
        {code: 'WO-DRAFT-1043', asset: 'PLC-C07', priority: 'P1', estimateMinutes: 30},
      ],
      notifyGroup: 'line-c-on-call', approvalPolicy: 'FOUR_EYES',
    },
    status: 0, expireTime: at(-0, 23, 59), remark: 'Drafts only until approved',
  },
  {
    actionId: 'action-cold-storage-setpoint', conversationId: sessionDefs[7]!.id, actionType: 'POINT_WRITE',
    title: '下发 2 号冷库设定值', description: '分两步将设定温度从 -18°C 调整为 -20°C，启用 3 项自动回滚保护。',
    titleI18n: text('下发 2 号冷库设定值', 'Apply Cold Room 2 setpoint'),
    descriptionI18n: text('分两步将设定温度从 -18°C 调整为 -20°C，启用 3 项自动回滚保护。', 'Change the setpoint from -18°C to -20°C in two stages with three automatic rollback protections.'),
    payload: {deviceCode: 'Cold-Storage-02', pointCode: 'set_temperature', from: -18, to: -20, rollback: true},
    status: 0, expireTime: at(-0, 23, 59), remark: 'Four-eyes approval required',
  },
  {
    actionId: 'action-opcua-timeout', conversationId: sessionDefs[3]!.id, actionType: 'DRIVER_CONFIG_UPDATE',
    title: '应用 OPC-UA 超时临时配置', description: '将握手超时从 5 秒调整为 8 秒，2 小时后自动复核失败率。',
    titleI18n: text('应用 OPC-UA 超时临时配置', 'Apply temporary OPC-UA timeout'),
    descriptionI18n: text('将握手超时从 5 秒调整为 8 秒，2 小时后自动复核失败率。', 'Raise handshake timeout from 5 to 8 seconds and automatically review the failure rate after two hours.'),
    payload: {driverCode: 'OPC-UA', property: 'handshakeTimeout', from: 5000, to: 8000}, status: 0, expireTime: at(-0, 23, 59),
  },
];

export const agenticAttachments: AgenticAttachment[] = [
  {id: '91001', conversationId: sessionDefs[3]!.id, fileName: 'opcua-handshake-20260811.log', contentType: 'text/plain', size: 2516582, createTime: at(1, 10, 58)},
  {id: '91002', conversationId: sessionDefs[5]!.id, fileName: 'M-104-vibration-spectrum.csv', contentType: 'text/csv', size: 486210, createTime: at(3, 9, 54)},
  {id: '91003', conversationId: sessionDefs[6]!.id, fileName: 'canary-rollout-metrics.json', contentType: 'application/json', size: 128044, createTime: at(4, 16, 50)},
];
