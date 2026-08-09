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
  MessageRecord,
  NotifyChannelBindRecord,
  NotifyChannelRecord,
  NotifyHistoryRecord,
  NotifyRecord,
  RuleRecord,
  RuleStateRecord,
} from '@/config/types/alarm';

/** Stable timestamps so the demo does not regenerate on every load. */
const CREATED = '2026-07-15T09:30:00';
const UPDATED = '2026-08-01T14:20:00';
const RECENT = '2026-08-05T10:15:00';

/** Wraps a payload as a versioned StructuredExt, mirroring alarmEntityConfig defaults. */
const ext = (type: string, content: Record<string, unknown>, version = 1) => ({
  type,
  version,
  content,
});

interface ChannelDef {
  name: string;
  code: string;
  type: string;
  credential: string;
  content: Record<string, unknown>;
}

const channelDefs: ChannelDef[] = [
  {
    name: '飞书告警机器人',
    code: 'feishu-alarm-bot',
    type: 'FEISHU_BOT',
    credential: 'feishu-bot-alarm',
    content: {
      signEnabled: true,
      cardVersion: 'interactive-card-v1',
      atAllAllowed: true,
      testMessageEnabled: true,
      options: {locale: 'zh-CN'}
    },
  },
  {
    name: '钉钉运维群机器人',
    code: 'dingtalk-ops',
    type: 'DINGTALK_BOT',
    credential: 'dingtalk-ops-bot',
    content: {
      signEnabled: true,
      cardVersion: 'markdown-v1',
      atAllAllowed: true,
      testMessageEnabled: true,
      options: {locale: 'zh-CN'}
    },
  },
  {
    name: '告警邮件通道',
    code: 'alarm-email',
    type: 'EMAIL',
    credential: 'smtp-alarm',
    content: {
      signEnabled: false,
      cardVersion: '',
      atAllAllowed: false,
      testMessageEnabled: true,
      options: {locale: 'zh-CN', from: 'alarm@dc3.site'}
    },
  },
  {
    name: '通用 Webhook',
    code: 'generic-webhook',
    type: 'WEBHOOK',
    credential: 'webhook-pagerduty',
    content: {
      signEnabled: true,
      cardVersion: '',
      atAllAllowed: false,
      testMessageEnabled: true,
      options: {method: 'POST', timeoutMs: 5000}
    },
  },
];

interface NotifyDef {
  name: string;
  code: string;
  autoConfirm: 'AUTO' | 'MANUAL';
  interval: number;
  content: Record<string, unknown>;
}

const notifyDefs: NotifyDef[] = [
  {
    name: '标准告警通知',
    code: 'notify-standard',
    autoConfirm: 'AUTO',
    interval: 300000,
    content: {
      dedup: {enabled: true, key: '${tenantId}:${ruleCode}:${entityId}'},
      rateLimit: {intervalMs: 300000, maxCount: 1},
      repeat: {enabled: false},
      recovery: {enabled: true, sendRecoveryMessage: true, autoConfirmOnRecovery: true},
    },
  },
  {
    name: '紧急告警通知',
    code: 'notify-urgent',
    autoConfirm: 'MANUAL',
    interval: 60000,
    content: {
      dedup: {enabled: true, key: '${tenantId}:${ruleCode}:${entityId}'},
      rateLimit: {intervalMs: 60000, maxCount: 3},
      repeat: {enabled: true, intervalMs: 300000},
      recovery: {enabled: true, sendRecoveryMessage: true, autoConfirmOnRecovery: false},
    },
  },
  {
    name: '低优先级通知',
    code: 'notify-low',
    autoConfirm: 'AUTO',
    interval: 900000,
    content: {
      dedup: {enabled: true, key: '${tenantId}:${ruleCode}:${entityId}'},
      rateLimit: {intervalMs: 900000, maxCount: 1},
      repeat: {enabled: false},
      recovery: {enabled: false, sendRecoveryMessage: false, autoConfirmOnRecovery: false},
    },
  },
  {
    name: '静默恢复通知',
    code: 'notify-silent',
    autoConfirm: 'AUTO',
    interval: 1800000,
    content: {
      dedup: {enabled: true, key: '${tenantId}:${ruleCode}:${entityId}'},
      rateLimit: {intervalMs: 1800000, maxCount: 1},
      repeat: {enabled: false},
      recovery: {enabled: true, sendRecoveryMessage: true, autoConfirmOnRecovery: true},
    },
  },
];

interface MessageDef {
  name: string;
  code: string;
  level: 'P0' | 'P1' | 'P2' | 'P3';
  content: Record<string, unknown>;
}

const messageDefs: MessageDef[] = [
  {
    name: '飞书卡片告警模板',
    code: 'msg-feishu-card',
    level: 'P1',
    content: {
      variables: ['severity', 'device', 'point', 'value', 'unit', 'threshold', 'triggerTime'],
      templates: [
        {
          channelType: 'FEISHU_BOT',
          payloadType: 'CARD',
          template: {
            title: '${severity} ${device} 告警',
            summary: '${point} 当前 ${value}${unit}, 阈值 ${threshold}${unit}'
          },
        },
      ],
    },
  },
  {
    name: '钉钉文本告警模板',
    code: 'msg-dingtalk-text',
    level: 'P2',
    content: {
      variables: ['severity', 'device', 'point', 'value', 'unit', 'threshold', 'triggerTime'],
      templates: [
        {
          channelType: 'DINGTALK_BOT',
          payloadType: 'MARKDOWN',
          template: {
            title: '${severity} ${device} 告警',
            text: '${point}=${value}${unit} (阈值 ${threshold}${unit}) @ ${triggerTime}'
          },
        },
      ],
    },
  },
  {
    name: '邮件告警模板',
    code: 'msg-email',
    level: 'P2',
    content: {
      variables: ['severity', 'device', 'point', 'value', 'unit', 'threshold', 'triggerTime'],
      templates: [
        {
          channelType: 'EMAIL',
          payloadType: 'HTML',
          template: {
            subject: '[${severity}] ${device} ${point} 告警',
            body: '<p>设备 ${device} 的 ${point} 当前值 ${value}${unit}, 超过阈值 ${threshold}${unit}。</p>',
          },
        },
      ],
    },
  },
  {
    name: '通用 Webhook 模板',
    code: 'msg-webhook-json',
    level: 'P3',
    content: {
      variables: ['severity', 'device', 'point', 'value', 'unit', 'threshold', 'triggerTime'],
      templates: [
        {
          channelType: 'WEBHOOK',
          payloadType: 'JSON',
          template: {
            event: 'alarm',
            severity: '${severity}',
            device: '${device}',
            point: '${point}',
            value: '${value}',
            threshold: '${threshold}',
            triggeredAt: '${triggerTime}',
          },
        },
      ],
    },
  },
];

interface BindDef {
  notifyIdx: number;
  channelIdx: number;
  content: Record<string, unknown>;
}

// Each bind wires a notify policy to a channel, narrowing which levels it carries.
const bindDefs: BindDef[] = [
  {notifyIdx: 0, channelIdx: 0, content: {levels: ['P1', 'P2'], sendRecovery: true, rateLimitOverrideMs: 300000}},
  {notifyIdx: 0, channelIdx: 3, content: {levels: ['P0', 'P1', 'P2'], sendRecovery: true, rateLimitOverrideMs: 60000}},
  {notifyIdx: 1, channelIdx: 1, content: {levels: ['P0', 'P1'], sendRecovery: true, rateLimitOverrideMs: 60000}},
  {notifyIdx: 2, channelIdx: 2, content: {levels: ['P2', 'P3'], sendRecovery: false, rateLimitOverrideMs: 900000}},
  {notifyIdx: 3, channelIdx: 0, content: {levels: ['P3'], sendRecovery: true, rateLimitOverrideMs: 1800000}},
];

interface RuleDef {
  name: string;
  code: string;
  entity: string;
  notifyIdx: number;
  messageIdx: number;
  condition: Record<string, unknown>;
  severity: 'P0' | 'P1' | 'P2' | 'P3';
  remark: string;
  enabled: boolean;
}

// entityId references point ids 5001+ from entities.ts; notify/message resolve by index.
const ruleDefs: RuleDef[] = [
  {
    name: '高温告警',
    code: 'rule-high-temp',
    entity: '5001',
    notifyIdx: 0,
    messageIdx: 0,
    condition: {field: 'numValue', operator: '>', threshold: 80, unit: '℃'},
    severity: 'P1',
    remark: '温度超过 80℃ 触发',
    enabled: true,
  },
  {
    name: '湿度越限告警',
    code: 'rule-humidity',
    entity: '5002',
    notifyIdx: 2,
    messageIdx: 2,
    condition: {field: 'numValue', operator: '>', threshold: 90, unit: '%RH'},
    severity: 'P2',
    remark: '湿度高于 90%RH',
    enabled: true,
  },
  {
    name: '电压异常告警',
    code: 'rule-voltage',
    entity: '5011',
    notifyIdx: 1,
    messageIdx: 1,
    condition: {field: 'numValue', operator: '>', threshold: 240, unit: 'V'},
    severity: 'P0',
    remark: '电压超过 240V 视为紧急',
    enabled: true,
  },
  {
    name: '网关 CPU 过载',
    code: 'rule-cpu-overload',
    entity: '5031',
    notifyIdx: 0,
    messageIdx: 3,
    condition: {field: 'numValue', operator: '>', threshold: 85, unit: '%'},
    severity: 'P2',
    remark: '边缘网关 CPU 持续偏高',
    enabled: true,
  },
  {
    name: '空调回风温度高',
    code: 'rule-hvac-return-temp',
    entity: '5052',
    notifyIdx: 3,
    messageIdx: 0,
    condition: {field: 'numValue', operator: '>', threshold: 26, unit: '℃'},
    severity: 'P3',
    remark: '回风温度偏高, 仅静默通知',
    enabled: false,
  },
];

interface StateDef {
  ruleIdx: number;
  entity: string;
  state: 'FIRING' | 'PENDING' | 'RECOVERED';
  fingerprint: string;
  triggerCount: number;
  first: string;
  last: string;
  recover: string;
  notify: string;
  alarmId: string;
  content: Record<string, unknown>;
}

const stateDefs: StateDef[] = [
  {
    ruleIdx: 0,
    entity: '5001',
    state: 'FIRING',
    fingerprint: 'fp-9f2a3c7e1b8d4f60',
    triggerCount: 3,
    first: '2026-08-05T08:00:00',
    last: RECENT,
    recover: '',
    notify: '2026-08-05T10:16:00',
    alarmId: 'ALM-20260805-001',
    content: {evaluatedValue: 83.5, sampleCount: 3, lastEvalTime: RECENT},
  },
  {
    ruleIdx: 1,
    entity: '5002',
    state: 'RECOVERED',
    fingerprint: 'fp-4c1d8a6f2e0b9c73',
    triggerCount: 1,
    first: '2026-08-04T22:10:00',
    last: '2026-08-04T22:10:00',
    recover: '2026-08-05T06:30:00',
    notify: '2026-08-04T22:11:00',
    alarmId: 'ALM-20260804-014',
    content: {evaluatedValue: 78.4, sampleCount: 1, lastEvalTime: '2026-08-05T06:30:00'},
  },
  {
    ruleIdx: 2,
    entity: '5011',
    state: 'PENDING',
    fingerprint: 'fp-0a7b3e5d9c218f64',
    triggerCount: 0,
    first: '',
    last: '',
    recover: '',
    notify: '',
    alarmId: '',
    content: {evaluatedValue: 0, sampleCount: 0, lastEvalTime: ''},
  },
  {
    ruleIdx: 3,
    entity: '5031',
    state: 'FIRING',
    fingerprint: 'fp-b58e2d7104a96f3c',
    triggerCount: 5,
    first: '2026-08-05T07:45:00',
    last: '2026-08-05T10:14:00',
    recover: '',
    notify: '2026-08-05T10:15:00',
    alarmId: 'ALM-20260805-002',
    content: {evaluatedValue: 92.1, sampleCount: 5, lastEvalTime: '2026-08-05T10:14:00'},
  },
  {
    ruleIdx: 4,
    entity: '5052',
    state: 'RECOVERED',
    fingerprint: 'fp-d31f6a90c8e45b27',
    triggerCount: 2,
    first: '2026-08-03T14:00:00',
    last: '2026-08-03T18:20:00',
    recover: '2026-08-03T20:05:00',
    notify: '2026-08-03T18:21:00',
    alarmId: 'ALM-20260803-007',
    content: {evaluatedValue: 25.2, sampleCount: 2, lastEvalTime: '2026-08-03T20:05:00'},
  },
];

interface HistoryDef {
  ruleIdx: number;
  notifyIdx: number;
  messageIdx: number;
  channelIdx: number;
  alarmId: string;
  target: string;
  status: 'SUCCESS' | 'FAILED';
  error: string;
  retry: number;
  time: string;
  request: Record<string, unknown>;
  response: Record<string, unknown>;
}

const historyDefs: HistoryDef[] = [
  {
    ruleIdx: 0,
    notifyIdx: 0,
    messageIdx: 0,
    channelIdx: 0,
    alarmId: 'ALM-20260805-001',
    target: 'https://open.feishu.cn/open-apis/bot/v2/hook/****',
    status: 'SUCCESS',
    error: '',
    retry: 0,
    time: '2026-08-05T10:16:00',
    request: {
      channelType: 'FEISHU_BOT',
      target: 'https://open.feishu.cn/open-apis/bot/v2/hook/****',
      payloadType: 'CARD'
    },
    response: {httpStatus: 200, code: 0, bizMessage: 'ok', remoteMessageId: 'msg-feishu-90213'},
  },
  {
    ruleIdx: 3,
    notifyIdx: 0,
    messageIdx: 3,
    channelIdx: 3,
    alarmId: 'ALM-20260805-002',
    target: 'https://hooks.pagerduty.com/****',
    status: 'SUCCESS',
    error: '',
    retry: 0,
    time: '2026-08-05T10:15:30',
    request: {channelType: 'WEBHOOK', target: 'https://hooks.pagerduty.com/****', payloadType: 'JSON'},
    response: {httpStatus: 202, code: 0, bizMessage: 'accepted', remoteMessageId: 'pd-55aef01'},
  },
  {
    ruleIdx: 2,
    notifyIdx: 1,
    messageIdx: 1,
    channelIdx: 1,
    alarmId: 'ALM-20260805-003',
    target: 'https://oapi.dingtalk.com/robot/send?access_token=****',
    status: 'FAILED',
    error: 'connect timeout: dingtalk endpoint unreachable after 5000ms',
    retry: 2,
    time: '2026-08-05T09:30:00',
    request: {
      channelType: 'DINGTALK_BOT',
      target: 'https://oapi.dingtalk.com/robot/send?access_token=****',
      payloadType: 'MARKDOWN'
    },
    response: {httpStatus: 0, code: -1, bizMessage: 'connect timeout'},
  },
  {
    ruleIdx: 0,
    notifyIdx: 0,
    messageIdx: 0,
    channelIdx: 0,
    alarmId: 'ALM-20260805-001',
    target: 'https://open.feishu.cn/open-apis/bot/v2/hook/****',
    status: 'SUCCESS',
    error: '',
    retry: 1,
    time: '2026-08-05T09:10:00',
    request: {
      channelType: 'FEISHU_BOT',
      target: 'https://open.feishu.cn/open-apis/bot/v2/hook/****',
      payloadType: 'CARD'
    },
    response: {httpStatus: 200, code: 0, bizMessage: 'ok', remoteMessageId: 'msg-feishu-90188'},
  },
  {
    ruleIdx: 1,
    notifyIdx: 2,
    messageIdx: 2,
    channelIdx: 2,
    alarmId: 'ALM-20260804-014',
    target: 'ops@dc3.site',
    status: 'FAILED',
    error: 'SMTP relay rejected recipient: mailbox full',
    retry: 3,
    time: '2026-08-04T22:11:00',
    request: {channelType: 'EMAIL', target: 'ops@dc3.site', payloadType: 'HTML'},
    response: {delivered: false, smtpResponse: '550 5.2.2 Mailbox full'},
  },
];

export const alarmChannels: NotifyChannelRecord[] = channelDefs.map((d, i) => ({
  id: String(5120 + i),
  channelName: d.name,
  channelCode: d.code,
  channelTypeFlag: d.type,
  credentialRef: d.credential,
  channelExt: ext('notify-channel', d.content),
  enableFlag: 'ENABLE',
}));

export const alarmNotifies: NotifyRecord[] = notifyDefs.map((d, i) => ({
  id: String(3072 + i),
  notifyName: d.name,
  notifyCode: d.code,
  autoConfirmFlag: d.autoConfirm,
  notifyInterval: d.interval,
  notifyExt: ext('alarm-notify-policy', d.content),
  enableFlag: 'ENABLE',
  remark: `通知策略 ${d.code}`,
}));

export const alarmMessages: MessageRecord[] = messageDefs.map((d, i) => ({
  id: String(4096 + i),
  messageName: d.name,
  messageCode: d.code,
  messageLevel: d.level,
  messageExt: ext('alarm-message-template', d.content),
  enableFlag: 'ENABLE',
}));

export const alarmChannelBinds: NotifyChannelBindRecord[] = bindDefs.map((d, i) => ({
  id: String(6144 + i),
  notifyId: alarmNotifies[d.notifyIdx]!.id,
  channelId: alarmChannels[d.channelIdx]!.id,
  bindExt: ext('notify-channel-bind', d.content),
  enableFlag: i === 4 ? 'DISABLE' : 'ENABLE',
}));

export const alarmRules: RuleRecord[] = ruleDefs.map((d, i) => ({
  id: String(1024 + i),
  ruleName: d.name,
  ruleCode: d.code,
  alarmTargetTypeFlag: 'POINT',
  entityId: d.entity,
  notifyId: alarmNotifies[d.notifyIdx]!.id,
  messageId: alarmMessages[d.messageIdx]!.id,
  ruleExt: ext('alarm-rule', {
    condition: d.condition,
    window: {mode: 'LAST', minSamples: 1},
    severity: d.severity,
    eventType: 'ALARM',
  }),
  enableFlag: d.enabled ? 'ENABLE' : 'DISABLE',
  remark: d.remark,
  creatorName: 'admin',
  createTime: CREATED,
  operatorName: 'system',
  operateTime: UPDATED,
}));

export const alarmRuleStates: RuleStateRecord[] = stateDefs.map((d, i) => ({
  id: String(7168 + i),
  ruleId: alarmRules[d.ruleIdx]!.id,
  alarmTargetTypeFlag: 'POINT',
  entityId: d.entity,
  fingerprint: d.fingerprint,
  entityStateFlag: d.state,
  firstTriggerTime: d.first,
  lastTriggerTime: d.last,
  lastRecoverTime: d.recover,
  lastNotifyTime: d.notify,
  triggerCount: d.triggerCount,
  alarmId: d.alarmId,
  entityStateExt: ext('rule-state-snapshot', d.content),
}));

export const alarmHistories: NotifyHistoryRecord[] = historyDefs.map((d, i) => ({
  id: String(8192 + i),
  ruleId: alarmRules[d.ruleIdx]!.id,
  notifyId: alarmNotifies[d.notifyIdx]!.id,
  messageId: alarmMessages[d.messageIdx]!.id,
  channelId: alarmChannels[d.channelIdx]!.id,
  alarmId: d.alarmId,
  channelTypeFlag: alarmChannels[d.channelIdx]!.channelTypeFlag,
  target: d.target,
  statusFlag: d.status,
  requestExt: ext('notify-request', d.request),
  responseExt: ext('notify-response', d.response),
  errorMessage: d.error,
  retryCount: d.retry,
  createTime: d.time,
}));
