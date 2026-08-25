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

import type {AgenticVisualizationSpec} from '@/config/types';
import {currentMockLocale} from './locale';
import type {MockDb} from './db';

const CHAT_URL = '/api/v3/agentic/chat/completions';

const encoder = new TextEncoder();
const sse = (obj: unknown): Uint8Array => encoder.encode(`data: ${JSON.stringify(obj)}\n\n`);
const DONE = encoder.encode('data: [DONE]\n\n');

const round = (n: number, d = 1) => Number(n.toFixed(d));
const hourly = (i: number) => `${String(i).padStart(2, '0')}:00`;

/** Realistic IoT demo charts — shared by the seed history and live mock replies. */
export const charts = {
  tempTrend: (): AgenticVisualizationSpec => ({
    id: 'temp-trend',
    type: 'line',
    title: '3 号风机轴承温度（过去 24h）',
    description: '告警阈值 80℃',
    dataset: Array.from({length: 24}, (_, i) => ({
      time: hourly(i),
      temp: round(71 + 9 * Math.sin((i - 14) / 3.8) + (i === 14 ? 5 : 0) + (i % 4) * 0.4),
    })),
    encode: {x: 'time', y: 'temp'},
    scale: {y: 'linear'},
    meta: {unit: '℃', yLabel: '温度 (℃)'},
  }),
  humidity: (): AgenticVisualizationSpec => ({
    id: 'humidity',
    type: 'area',
    title: '车间环境湿度（过去 24h）',
    dataset: Array.from({length: 24}, (_, i) => ({
      time: hourly(i),
      humidity: round(48 + 12 * Math.sin(i / 4) + (i % 3))
    })),
    encode: {x: 'time', y: 'humidity'},
    scale: {y: 'linear'},
    meta: {unit: '%RH', yLabel: '湿度 (%RH)'},
  }),
  powerTrend: (): AgenticVisualizationSpec => ({
    id: 'power-trend',
    type: 'line',
    title: '有功功率趋势（过去 24h）',
    dataset: Array.from({length: 24}, (_, i) => ({
      time: hourly(i),
      power: round(1.6 + 0.9 * Math.sin((i - 8) / 3) + (i > 8 && i < 18 ? 0.5 : 0), 2)
    })),
    encode: {x: 'time', y: 'power'},
    scale: {y: 'linear'},
    meta: {unit: 'kW', yLabel: '功率 (kW)'},
  }),
  deviceStatus: (): AgenticVisualizationSpec => ({
    id: 'device-status',
    type: 'donut',
    title: '设备在线状态分布',
    dataset: [
      {status: '在线', count: 12},
      {status: '离线', count: 3},
      {status: '告警', count: 1},
    ],
    encode: {y: 'count', color: 'status'},
  }),
  pointValues: (): AgenticVisualizationSpec => ({
    id: 'point-values',
    type: 'column',
    title: '关键位号实时值',
    dataset: [
      {point: '温度', value: 24.5},
      {point: '湿度', value: 58.2},
      {point: '电压', value: 221.3},
      {point: '电流', value: 9.8},
      {point: '功率', value: 2.1},
    ],
    encode: {x: 'point', y: 'value'},
  }),
  energyWeek: (): AgenticVisualizationSpec => ({
    id: 'energy-week',
    type: 'bar',
    title: '近 7 日能耗',
    dataset: [
      {day: '周一', kwh: 52.4},
      {day: '周二', kwh: 48.1},
      {day: '周三', kwh: 55.8},
      {day: '周四', kwh: 61.2},
      {day: '周五', kwh: 58.7},
      {day: '周六', kwh: 33.5},
      {day: '周日', kwh: 30.2},
    ],
    encode: {x: 'day', y: 'kwh'},
    meta: {unit: 'kWh', yLabel: '能耗 (kWh)'},
  }),
  alarmSummary: (): AgenticVisualizationSpec => ({
    id: 'alarm-summary',
    type: 'stat',
    title: '告警概览（近 24h）',
    dataset: [{总告警: 17, P0紧急: 1, P1重要: 4, P2次要: 12, 已恢复: 9}],
    encode: {},
  }),
  driverLoad: (): AgenticVisualizationSpec => ({
    id: 'driver-load',
    type: 'scatter',
    title: '驱动负载分布（CPU vs 内存）',
    dataset: [
      {driver: 'Modbus-TCP', cpu: 18, mem: 42},
      {driver: 'OPC-UA', cpu: 35, mem: 58},
      {driver: 'MQTT', cpu: 12, mem: 31},
      {driver: 'BACnet', cpu: 22, mem: 39},
      {driver: 'S7 PLC', cpu: 41, mem: 64},
      {driver: 'CANopen', cpu: 8, mem: 24},
      {driver: 'IEC104', cpu: 28, mem: 47},
      {driver: 'OPC-DA', cpu: 33, mem: 52},
    ],
    encode: {x: 'cpu', y: 'mem', color: 'driver'},
    meta: {xLabel: 'CPU (%)', yLabel: '内存 (%)'},
  }),
};

interface Scenario {
  match: RegExp;
  events: { type: string; title: string; detail?: string; phase?: string; status?: string; name?: string }[];
  text: string;
  textEn: string;
  charts: () => AgenticVisualizationSpec[];
}

const scenarios: Scenario[] = [
  {
    match: /温度|temp|发热|过热/i,
    events: [
      {type: 'tool', title: '查询位号历史值', name: 'point_value_list', phase: 'start', status: 'running'},
      {type: 'reasoning', title: '识别温度异常', detail: '14:00 达到 85.4℃，超过 80℃ 阈值并持续 2 小时'},
      {type: 'tool', title: '查询位号历史值', phase: 'result', status: 'success'},
    ],
    text:
      '3 号风机轴承温度在过去 24 小时持续攀升，**14:00 达到峰值 85.4℃**，超过 80℃ 告警阈值约 2 小时。结合状态分布，当前有 1 台设备处于告警。建议检查润滑与散热，必要时降低负载。',
    textEn:
      'Fan 3 bearing temperature climbed over the last 24 hours and **peaked at 85.4°C at 14:00**, staying above the 80°C threshold for about two hours. One asset is currently in alarm. Check lubrication and cooling, and reduce load if necessary.',
    charts: () => [charts.tempTrend(), charts.deviceStatus(), charts.alarmSummary()],
  },
  {
    match: /能耗|用电|电量|energy|功率/i,
    events: [
      {type: 'tool', title: '聚合设备能耗', name: 'point_value_stats', phase: 'start', status: 'running'},
      {type: 'reasoning', title: '能耗趋势分析', detail: '工作日能耗显著高于周末，周四峰值 61.2 kWh'},
      {type: 'tool', title: '聚合设备能耗', phase: 'result', status: 'success'},
    ],
    text:
      '近 7 日总能耗 **340.9 kWh**，工作日均值 55.2 kWh，周末降至 31.9 kWh。周四最高（61.2 kWh）。功率曲线显示白天（8-18 时）负载明显抬升，符合生产节律。可考虑在谷电时段调度高耗能任务。',
    textEn:
      'Total energy over the last seven days was **340.9 kWh**. Weekdays averaged 55.2 kWh and weekends 31.9 kWh, with Thursday highest at 61.2 kWh. The daytime load increase matches production hours; schedule flexible loads during off-peak tariffs.',
    charts: () => [charts.energyWeek(), charts.powerTrend()],
  },
  {
    match: /驱动|driver|掉线|负载/i,
    events: [
      {type: 'tool', title: '查询驱动运行状态', name: 'driver_status_list', phase: 'start', status: 'running'},
      {type: 'reasoning', title: '驱动健康评估', detail: 'S7 PLC 驱动 CPU 41%、内存 64%，负载偏高'},
      {type: 'tool', title: '查询驱动运行状态', phase: 'result', status: 'success'},
    ],
    text:
      '8 个采集驱动中，**S7 PLC** 负载最高（CPU 41% / 内存 64%），**OPC-UA** 次之。CANopen 最低。OPC-UA 近期存在周期性掉线（与定时任务重合），建议检查证书有效期与握手超时。',
    textEn:
      'Among eight collection drivers, **S7 PLC** has the highest load (41% CPU / 64% memory), followed by **OPC-UA**. CANopen is lowest. OPC-UA disconnects align with a scheduled task; check certificate validity and handshake timeout.',
    charts: () => [charts.driverLoad(), charts.deviceStatus()],
  },
  {
    match: /位号|point|点位|实时/i,
    events: [
      {type: 'tool', title: '查询位号实时值', name: 'point_value_latest', phase: 'start', status: 'running'},
      {type: 'tool', title: '查询位号实时值', phase: 'result', status: 'success'},
    ],
    text: '当前关键位号实时值：温度 24.5℃、湿度 58.2%RH、电压 221.3V、电流 9.8A、功率 2.1kW，均在正常区间。环境湿度过去 24h 在 48-60% 波动。',
    textEn: 'Current key values are 24.5°C, 58.2% RH, 221.3 V, 9.8 A, and 2.1 kW. All are within normal ranges; ambient humidity varied between 48% and 60% over the last 24 hours.',
    charts: () => [charts.pointValues(), charts.humidity()],
  },
];

const defaultScenario: Scenario = {
  match: /.*/,
  events: [
    {type: 'tool', title: '查询设备总览', name: 'dashboard_stats', phase: 'start', status: 'running'},
    {type: 'reasoning', title: '综合态势分析', detail: '汇总设备/告警/能耗指标'},
    {type: 'tool', title: '查询设备总览', phase: 'result', status: 'success'},
  ],
  text:
    '已为你汇总当前平台态势：16 台设备中 12 在线、3 离线、1 告警；近 24h 共 17 条告警（P0×1, P1×4, P2×12），已恢复 9 条。3 号风机温度异常是当前首要风险，详细分析可参见相关会话。',
  textEn:
    'Current platform summary: 12 of 16 assets are online, three are offline, and one is in alarm. There were 17 alarms in the last 24 hours (P0×1, P1×4, P2×12), with nine recovered. Fan 3 temperature is the top current risk.',
  charts: () => [charts.deviceStatus(), charts.tempTrend(), charts.alarmSummary()],
};

const pickScenario = (prompt: string): Scenario =>
  scenarios.find((s) => s.match.test(prompt)) ?? defaultScenario;

const scenarioText = (scenario: Scenario) => currentMockLocale() === 'zh' ? scenario.text : scenario.textEn;

const localizedEvents = (scenario: Scenario) => {
  if (currentMockLocale() === 'zh') return scenario.events;
  return scenario.events.map((event) => ({
    ...event,
    title: event.name ? event.name.replaceAll('_', ' ') : 'Analyze operational context',
    detail: event.detail ? 'Correlate current telemetry, history, and alarm context.' : undefined,
  }));
};

const localizedChart = (spec: AgenticVisualizationSpec): AgenticVisualizationSpec => {
  if (currentMockLocale() === 'zh') return spec;
  const titles: Record<string, string> = {
    'temp-trend': 'Fan 3 bearing temperature (last 24h)',
    humidity: 'Workshop humidity (last 24h)',
    'power-trend': 'Active power trend (last 24h)',
    'device-status': 'Device connectivity distribution',
    'point-values': 'Current key point values',
    'energy-week': 'Energy consumption (last 7 days)',
    'alarm-summary': 'Alarm summary (last 24h)',
    'driver-load': 'Driver load distribution (CPU vs memory)',
  };
  let dataset = spec.dataset;
  if (spec.id === 'device-status') {
    const statuses: Record<string, string> = {'在线': 'Online', '离线': 'Offline', '告警': 'Alarm'};
    dataset = dataset.map((row) => ({...row, status: statuses[String(row.status)] || row.status}));
  } else if (spec.id === 'point-values') {
    const points: Record<string, string> = {
      '温度': 'Temperature',
      '湿度': 'Humidity',
      '电压': 'Voltage',
      '电流': 'Current',
      '功率': 'Power'
    };
    dataset = dataset.map((row) => ({...row, point: points[String(row.point)] || row.point}));
  } else if (spec.id === 'energy-week') {
    const days: Record<string, string> = {
      '周一': 'Mon',
      '周二': 'Tue',
      '周三': 'Wed',
      '周四': 'Thu',
      '周五': 'Fri',
      '周六': 'Sat',
      '周日': 'Sun'
    };
    dataset = dataset.map((row) => ({...row, day: days[String(row.day)] || row.day}));
  } else if (spec.id === 'alarm-summary') {
    dataset = [{total: 17, P0: 1, P1: 4, P2: 12, recovered: 9}];
  }
  return {...spec, title: titles[spec.id || ''] || spec.title, dataset};
};

const streamChunks = (prompt: string): unknown[] => {
  const s = pickScenario(prompt);
  const chunks: unknown[] = localizedEvents(s).map((e) => ({object: 'agentic.event', ...e}));
  chunks.push({choices: [{delta: {content: scenarioText(s)}}]});
  for (const c of s.charts()) chunks.push({object: 'agentic.visualization', visualization: localizedChart(c)});
  chunks.push({choices: [{delta: {content: currentMockLocale() === 'zh' ? '需要进一步下钻某个设备或时段，告诉我即可。' : 'Tell me if you want to drill into a specific asset or time range.'}}]});
  chunks.push({choices: [{finish_reason: 'stop'}]});
  return chunks;
};

const mockResponse = (prompt: string, stream: boolean): Response => {
  const s = pickScenario(prompt);
  if (stream) {
    const body = new ReadableStream<Uint8Array>({
      start(controller) {
        for (const chunk of streamChunks(prompt)) controller.enqueue(sse(chunk));
        controller.enqueue(DONE);
        controller.close();
      },
    });
    return new Response(body, {
      status: 200,
      headers: {'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache'},
    });
  }
  return new Response(
    JSON.stringify({
      choices: [{
        message: {role: 'assistant', content: scenarioText(s), contentExt: {charts: s.charts().map(localizedChart)}},
        finishReason: 'stop'
      }],
    }),
    {status: 200, headers: {'Content-Type': 'application/json'}},
  );
};

interface MockChatRequest {
  conversationId?: string;
  model?: string;
  messages?: Array<{ role?: string; content?: string }>;
}

const persistMockTurn = (db: MockDb, request: MockChatRequest, prompt: string): void => {
  if (!request.conversationId) return;
  const scenario = pickScenario(prompt);
  const rows = db.agenticMessages.filter((message) => String(message.conversationId) === request.conversationId);
  const nextIndex = rows.reduce((max, message) => Math.max(max, Number(message.messageIndex ?? -1)), -1) + 1;
  const created = new Date().toISOString();
  const suffix = currentMockLocale() === 'zh'
    ? '需要进一步下钻某个设备或时段，告诉我即可。'
    : 'Tell me if you want to drill into a specific asset or time range.';
  db.agenticMessages.push(
    {
      id: `mock-user-${Date.now()}`,
      conversationId: request.conversationId,
      role: 'user',
      content: prompt,
      messageIndex: nextIndex,
      status: 2,
      streaming: false,
      createTime: created
    },
    {
      id: `mock-assistant-${Date.now()}`,
      conversationId: request.conversationId,
      role: 'assistant',
      content: `${scenarioText(scenario)}${suffix}`,
      contentExt: {
        tools: localizedEvents(scenario).filter((event) => event.type === 'tool').map((event) => event.name).filter(Boolean),
        traces: localizedEvents(scenario),
        charts: scenario.charts().map(localizedChart),
        tokens: {input: 1480, output: 680, context: 820},
      },
      model: request.model || 'gpt-4o', messageIndex: nextIndex + 1, status: 2, streaming: false,
      finishReason: 'stop', createTime: created,
    },
  );
  const session = db.agenticSessions.find((item) => String(item.conversationId) === request.conversationId);
  if (session) session.operateTime = created;
};

/**
 * The agentic assistant streams chat over a raw `fetch` (SSE), bypassing the
 * axios mock adapter. In mock builds this installs a fetch interceptor that
 * answers `/api/v3/agentic/chat/completions` with a scripted, chart-bearing,
 * keyword-aware reply so the AI assistant is fully demoable without a backend.
 */
export function installAgenticFetchMock(db: MockDb): void {
  const original = window.fetch.bind(window);
  window.fetch = ((input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url;
    if (url.includes(CHAT_URL)) {
      let stream = true;
      let prompt = '';
      let request: MockChatRequest = {};
      try {
        request = JSON.parse(String(init?.body ?? '{}')) as MockChatRequest & { stream?: boolean };
        stream = (request as MockChatRequest & { stream?: boolean }).stream !== false;
        const msgs = Array.isArray(request.messages) ? request.messages : [];
        prompt = String(msgs[msgs.length - 1]?.content ?? '');
      } catch {
        /* defaults */
      }
      persistMockTurn(db, request, prompt);
      return Promise.resolve(mockResponse(prompt, stream));
    }
    return original(input as RequestInfo, init);
  }) as typeof window.fetch;
}
