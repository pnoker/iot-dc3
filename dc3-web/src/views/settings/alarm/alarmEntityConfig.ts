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

import {
  addMessage,
  addNotify,
  addNotifyChannel,
  addNotifyChannelBind,
  addRule,
  deleteMessage,
  deleteNotify,
  deleteNotifyChannel,
  deleteNotifyChannelBind,
  deleteRule,
  listMessage,
  listNotify,
  listNotifyChannel,
  listNotifyChannelBind,
  listNotifyHistory,
  listRule,
  listRuleState,
  updateMessage,
  updateNotify,
  updateNotifyChannel,
  updateNotifyChannelBind,
  updateRule,
} from '@/api/alarm';
import {listDevice} from '@/api/device';
import {listDriver} from '@/api/driver';
import {listPoint} from '@/api/point';
import type {PageQuery} from '@/config/types';

export type AlarmTabKey = 'rule' | 'notify' | 'message' | 'channel' | 'bind' | 'state' | 'history';
export type AlarmFieldKind = 'input' | 'number' | 'select' | 'remoteSelect' | 'enableFlag' | 'textarea' | 'json';
export type AlarmColumnKind = 'text' | 'tag' | 'code' | 'time' | 'json';

export interface AlarmOption {
  label: string;
  value: string;
}

export interface AlarmFieldConfig {
  prop: string;
  label: string;
  kind?: AlarmFieldKind;
  options?: AlarmOption[];
  // remoteSelect: async option loader. Receives the live form so dependent fields
  // (e.g. entityId reading alarmTargetTypeFlag) can branch on other selections.
  loadOptions?: (form: Record<string, any>) => Promise<AlarmOption[]>;
  placeholder?: string;
  required?: boolean;
  span?: number;
  rows?: number;
  precision?: number;
}

export interface AlarmColumnConfig {
  prop: string;
  label: string;
  kind?: AlarmColumnKind;
  width?: number | string;
  minWidth?: number | string;
  fixed?: boolean | 'left' | 'right';
  overflow?: boolean;
}

export interface AlarmEntityConfig {
  key: AlarmTabKey;
  label: string;
  editable: boolean;
  searchProp?: string;
  searchLabel: string;
  searchPlaceholder: string;
  filterProp?: string;
  filterLabel?: string;
  filterPlaceholder?: string;
  filterOptions: AlarmOption[];
  columns: AlarmColumnConfig[];
  fields: AlarmFieldConfig[];
  defaultForm: () => Record<string, unknown>;
  list: (query: PageQuery) => Promise<R>;
  add?: (payload: Record<string, unknown>) => Promise<R>;
  update?: (payload: Record<string, unknown>) => Promise<R>;
  remove?: (id: string) => Promise<R>;
}

export const ALARM_DETAIL_ROUTE_MAP: Record<AlarmTabKey, string> = {
  rule: 'settingsAlarmRuleDetail',
  notify: 'settingsAlarmNotifyDetail',
  message: 'settingsAlarmMessageDetail',
  channel: 'settingsAlarmChannelDetail',
  bind: 'settingsAlarmBindDetail',
  state: 'settingsAlarmStateDetail',
  history: 'settingsAlarmHistoryDetail',
};

type Translate = (key: string) => string;

const structuredExt = (type: string, content: Record<string, unknown>, remark = '') => ({
  type,
  version: 1,
  remark,
  content,
});

const defaultRuleExt = () =>
  structuredExt('alarm-rule', {
    condition: {field: 'numValue', operator: '>', threshold: 80, unit: ''},
    window: {mode: 'LAST', minSamples: 1},
    recovery: {enabled: true, operator: '<=', threshold: 75, duration: 'PT5M'},
    severity: 'P2',
    eventType: 'ALARM',
    labels: [],
  });

const defaultNotifyExt = () =>
  structuredExt('alarm-notify-policy', {
    dedup: {enabled: true, key: '${tenantId}:${ruleCode}:${entityId}'},
    rateLimit: {intervalMs: 300000, maxCount: 1},
    repeat: {enabled: false},
    recovery: {enabled: true, sendRecoveryMessage: true, autoConfirmOnRecovery: false},
  });

const defaultMessageExt = () =>
  structuredExt('alarm-message-template', {
    variables: ['severity', 'device', 'point', 'value', 'unit', 'threshold', 'triggerTime'],
    templates: [
      {
        channelType: 'FEISHU_BOT',
        payloadType: 'CARD',
        template: {
          title: '${severity} ${device} alarm',
          summary: '${point} is ${value}${unit}, threshold ${threshold}${unit}.',
        },
      },
    ],
  });

const defaultChannelExt = () =>
  structuredExt('notify-channel', {
    signEnabled: true,
    cardVersion: 'interactive-card-v1',
    atAllAllowed: false,
    testMessageEnabled: true,
    options: {locale: 'zh-CN'},
  });

const defaultBindExt = () =>
  structuredExt('notify-channel-bind', {
    levels: ['P0', 'P1', 'P2'],
    sendRecovery: true,
    rateLimitOverrideMs: 300000,
  });

// remoteSelect loaders: resolve foreign-key fields to selectable {name → id}
// options instead of typing raw ids. value is String(id) so edit-mode echo matches.
const FK_PAGE: PageQuery = {page: {current: 1, size: 1000}};

const loadNotifyOptions = async (): Promise<AlarmOption[]> => {
  const res: any = await listNotify(FK_PAGE);
  return (res?.data?.records || []).map((r: any) => ({
    label: r.notifyName || r.notifyCode || String(r.id),
    value: String(r.id),
  }));
};
const loadMessageOptions = async (): Promise<AlarmOption[]> => {
  const res: any = await listMessage(FK_PAGE);
  return (res?.data?.records || []).map((r: any) => ({
    label: r.messageName || r.messageCode || String(r.id),
    value: String(r.id),
  }));
};
const loadChannelOptions = async (): Promise<AlarmOption[]> => {
  const res: any = await listNotifyChannel(FK_PAGE);
  return (res?.data?.records || []).map((r: any) => ({
    label: r.channelName || r.channelCode || String(r.id),
    value: String(r.id),
  }));
};
const loadEntityOptions = async (form: Record<string, any>): Promise<AlarmOption[]> => {
  const type = form.alarmTargetTypeFlag;
  let res: any;
  let nameKey: string;
  if (type === 'DEVICE') {
    res = await listDevice(FK_PAGE);
    nameKey = 'deviceName';
  } else if (type === 'DRIVER') {
    res = await listDriver(FK_PAGE);
    nameKey = 'driverName';
  } else {
    res = await listPoint(FK_PAGE);
    nameKey = 'pointName';
  }
  return (res?.data?.records || []).map((r: any) => ({label: r[nameKey] || String(r.id), value: String(r.id)}));
};

export const createAlarmEntityConfigs = (t: Translate) => {
  const enableOptions: AlarmOption[] = [
    {label: t('common.enable'), value: 'ENABLE'},
    {label: t('common.disable'), value: 'DISABLE'},
  ];
  const autoConfirmOptions: AlarmOption[] = [
    {label: t('settings.alarm.auto'), value: 'AUTO'},
    {label: t('settings.alarm.manual'), value: 'MANUAL'},
  ];
  const targetOptions: AlarmOption[] = [
    {label: t('settings.alarm.point'), value: 'POINT'},
    {label: t('settings.alarm.device'), value: 'DEVICE'},
    {label: t('settings.alarm.driver'), value: 'DRIVER'},
  ];
  const channelTypeOptions: AlarmOption[] = [
    {label: 'Feishu Bot', value: 'FEISHU_BOT'},
    {label: 'Webhook', value: 'WEBHOOK'},
    {label: 'Email', value: 'EMAIL'},
  ];
  const messageLevelOptions: AlarmOption[] = [
    {label: 'P0', value: 'P0'},
    {label: 'P1', value: 'P1'},
    {label: 'P2', value: 'P2'},
    {label: 'P3', value: 'P3'},
  ];
  const ruleStateOptions: AlarmOption[] = [
    {label: t('settings.alarm.normal'), value: 'NORMAL'},
    {label: t('settings.alarm.firing'), value: 'FIRING'},
    {label: t('settings.alarm.recovered'), value: 'RECOVERED'},
  ];
  const historyStatusOptions: AlarmOption[] = [
    {label: t('settings.alarm.pending'), value: 'PENDING'},
    {label: t('settings.alarm.success'), value: 'SUCCESS'},
    {label: t('settings.alarm.failed'), value: 'FAILED'},
    {label: t('settings.alarm.retrying'), value: 'RETRYING'},
    {label: t('settings.alarm.skipped'), value: 'SKIPPED'},
  ];

  const commonColumns = (): AlarmColumnConfig[] => [
    {prop: 'remark', label: t('common.remark'), minWidth: 150},
    {prop: 'createTime', label: t('common.createTime'), kind: 'time', width: 165},
  ];

  const commonFields = (): AlarmFieldConfig[] => [
    {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'enableFlag', span: 12},
    {prop: 'remark', label: t('common.remark'), kind: 'textarea', span: 24, rows: 3},
  ];

  const configs: AlarmEntityConfig[] = [
    {
      key: 'rule',
      label: t('settings.alarm.rules'),
      editable: true,
      searchProp: 'ruleName',
      searchLabel: t('settings.alarm.ruleName'),
      searchPlaceholder: t('settings.alarm.searchRule'),
      filterProp: 'enableFlag',
      filterLabel: t('common.enableFlag'),
      filterPlaceholder: t('common.enableFlag'),
      filterOptions: enableOptions,
      columns: [
        {prop: 'ruleName', label: t('settings.alarm.ruleName'), minWidth: 180},
        {prop: 'ruleCode', label: t('settings.alarm.ruleCode'), kind: 'code', minWidth: 180},
        {prop: 'alarmTargetTypeFlag', label: t('settings.alarm.targetType'), kind: 'tag', width: 110},
        {prop: 'entityId', label: t('settings.alarm.entityId'), kind: 'code', minWidth: 130},
        {prop: 'notifyId', label: t('settings.alarm.notifyId'), kind: 'code', minWidth: 130},
        {prop: 'messageId', label: t('settings.alarm.messageId'), kind: 'code', minWidth: 130},
        {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'tag', width: 90},
        ...commonColumns(),
      ],
      fields: [
        {prop: 'ruleName', label: t('settings.alarm.ruleName'), required: true},
        {prop: 'ruleCode', label: t('settings.alarm.ruleCode')},
        {
          prop: 'alarmTargetTypeFlag',
          label: t('settings.alarm.targetType'),
          kind: 'select',
          options: targetOptions,
          required: true,
        },
        {
          prop: 'entityId',
          label: t('settings.alarm.entityId'),
          kind: 'remoteSelect',
          loadOptions: loadEntityOptions,
          required: true,
        },
        {
          prop: 'notifyId',
          label: t('settings.alarm.notifyId'),
          kind: 'remoteSelect',
          loadOptions: loadNotifyOptions,
          required: true,
        },
        {
          prop: 'messageId',
          label: t('settings.alarm.messageId'),
          kind: 'remoteSelect',
          loadOptions: loadMessageOptions,
          required: true,
        },
        {prop: 'ruleExt', label: t('settings.alarm.ruleExt'), kind: 'json', span: 24, rows: 10, required: true},
        ...commonFields(),
      ],
      defaultForm: () => ({
        ruleName: '',
        ruleCode: '',
        alarmTargetTypeFlag: 'POINT',
        entityId: '',
        notifyId: '',
        messageId: '',
        ruleExt: defaultRuleExt(),
        enableFlag: 'ENABLE',
        remark: '',
      }),
      list: listRule,
      add: addRule,
      update: updateRule,
      remove: deleteRule,
    },
    {
      key: 'notify',
      label: t('settings.alarm.notifies'),
      editable: true,
      searchProp: 'notifyName',
      searchLabel: t('settings.alarm.notifyName'),
      searchPlaceholder: t('settings.alarm.searchNotify'),
      filterProp: 'enableFlag',
      filterLabel: t('common.enableFlag'),
      filterPlaceholder: t('common.enableFlag'),
      filterOptions: enableOptions,
      columns: [
        {prop: 'notifyName', label: t('settings.alarm.notifyName'), minWidth: 180},
        {prop: 'notifyCode', label: t('settings.alarm.notifyCode'), kind: 'code', minWidth: 180},
        {prop: 'autoConfirmFlag', label: t('settings.alarm.autoConfirm'), kind: 'tag', width: 120},
        {prop: 'notifyInterval', label: t('settings.alarm.notifyInterval'), width: 130},
        {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'tag', width: 90},
        ...commonColumns(),
      ],
      fields: [
        {prop: 'notifyName', label: t('settings.alarm.notifyName'), required: true},
        {prop: 'notifyCode', label: t('settings.alarm.notifyCode')},
        {
          prop: 'autoConfirmFlag',
          label: t('settings.alarm.autoConfirm'),
          kind: 'select',
          options: autoConfirmOptions,
          required: true,
        },
        {prop: 'notifyInterval', label: t('settings.alarm.notifyInterval'), kind: 'number', required: true},
        {prop: 'notifyExt', label: t('settings.alarm.notifyExt'), kind: 'json', span: 24, rows: 10, required: true},
        ...commonFields(),
      ],
      defaultForm: () => ({
        notifyName: '',
        notifyCode: '',
        autoConfirmFlag: 'MANUAL',
        notifyInterval: 300000,
        notifyExt: defaultNotifyExt(),
        enableFlag: 'ENABLE',
        remark: '',
      }),
      list: listNotify,
      add: addNotify,
      update: updateNotify,
      remove: deleteNotify,
    },
    {
      key: 'message',
      label: t('settings.alarm.messages'),
      editable: true,
      searchProp: 'messageName',
      searchLabel: t('settings.alarm.messageName'),
      searchPlaceholder: t('settings.alarm.searchMessage'),
      filterProp: 'enableFlag',
      filterLabel: t('common.enableFlag'),
      filterPlaceholder: t('common.enableFlag'),
      filterOptions: enableOptions,
      columns: [
        {prop: 'messageName', label: t('settings.alarm.messageName'), minWidth: 190},
        {prop: 'messageCode', label: t('settings.alarm.messageCode'), kind: 'code', minWidth: 190},
        {prop: 'messageLevel', label: t('settings.alarm.messageLevel'), kind: 'tag', width: 110},
        {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'tag', width: 90},
        ...commonColumns(),
      ],
      fields: [
        {prop: 'messageName', label: t('settings.alarm.messageName'), required: true},
        {prop: 'messageCode', label: t('settings.alarm.messageCode')},
        {
          prop: 'messageLevel',
          label: t('settings.alarm.messageLevel'),
          kind: 'select',
          options: messageLevelOptions,
          required: true,
        },
        {prop: 'messageExt', label: t('settings.alarm.messageExt'), kind: 'json', span: 24, rows: 10, required: true},
        ...commonFields(),
      ],
      defaultForm: () => ({
        messageName: '',
        messageCode: '',
        messageLevel: 'P2',
        messageExt: defaultMessageExt(),
        enableFlag: 'ENABLE',
        remark: '',
      }),
      list: listMessage,
      add: addMessage,
      update: updateMessage,
      remove: deleteMessage,
    },
    {
      key: 'channel',
      label: t('settings.alarm.channels'),
      editable: true,
      searchProp: 'channelName',
      searchLabel: t('settings.alarm.channelName'),
      searchPlaceholder: t('settings.alarm.searchChannel'),
      filterProp: 'channelTypeFlag',
      filterLabel: t('settings.alarm.channelType'),
      filterPlaceholder: t('settings.alarm.channelType'),
      filterOptions: channelTypeOptions,
      columns: [
        {prop: 'channelName', label: t('settings.alarm.channelName'), minWidth: 180},
        {prop: 'channelCode', label: t('settings.alarm.channelCode'), kind: 'code', minWidth: 180},
        {prop: 'channelTypeFlag', label: t('settings.alarm.channelType'), kind: 'tag', width: 130},
        {prop: 'credentialRef', label: t('settings.alarm.credentialRef'), kind: 'code', minWidth: 190},
        {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'tag', width: 90},
        ...commonColumns(),
      ],
      fields: [
        {prop: 'channelName', label: t('settings.alarm.channelName'), required: true},
        {prop: 'channelCode', label: t('settings.alarm.channelCode')},
        {
          prop: 'channelTypeFlag',
          label: t('settings.alarm.channelType'),
          kind: 'select',
          options: channelTypeOptions,
          required: true,
        },
        {prop: 'credentialRef', label: t('settings.alarm.credentialRef'), required: true},
        {prop: 'channelExt', label: t('settings.alarm.channelExt'), kind: 'json', span: 24, rows: 8, required: true},
        ...commonFields(),
      ],
      defaultForm: () => ({
        channelName: '',
        channelCode: '',
        channelTypeFlag: 'FEISHU_BOT',
        credentialRef: 'secret:feishu:',
        channelExt: defaultChannelExt(),
        enableFlag: 'ENABLE',
        remark: '',
      }),
      list: listNotifyChannel,
      add: addNotifyChannel,
      update: updateNotifyChannel,
      remove: deleteNotifyChannel,
    },
    {
      key: 'bind',
      label: t('settings.alarm.bindings'),
      editable: true,
      searchProp: 'notifyId',
      searchLabel: t('settings.alarm.notifyId'),
      searchPlaceholder: t('settings.alarm.searchBinding'),
      filterProp: 'enableFlag',
      filterLabel: t('common.enableFlag'),
      filterPlaceholder: t('common.enableFlag'),
      filterOptions: enableOptions,
      columns: [
        {prop: 'notifyId', label: t('settings.alarm.notifyId'), kind: 'code', minWidth: 140},
        {prop: 'channelId', label: t('settings.alarm.channelId'), kind: 'code', minWidth: 140},
        {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'tag', width: 90},
        ...commonColumns(),
      ],
      fields: [
        {
          prop: 'notifyId',
          label: t('settings.alarm.notifyId'),
          kind: 'remoteSelect',
          loadOptions: loadNotifyOptions,
          required: true,
        },
        {
          prop: 'channelId',
          label: t('settings.alarm.channelId'),
          kind: 'remoteSelect',
          loadOptions: loadChannelOptions,
          required: true,
        },
        {prop: 'bindExt', label: t('settings.alarm.bindExt'), kind: 'json', span: 24, rows: 8, required: true},
        ...commonFields(),
      ],
      defaultForm: () => ({
        notifyId: '',
        channelId: '',
        bindExt: defaultBindExt(),
        enableFlag: 'ENABLE',
        remark: '',
      }),
      list: listNotifyChannelBind,
      add: addNotifyChannelBind,
      update: updateNotifyChannelBind,
      remove: deleteNotifyChannelBind,
    },
    {
      key: 'state',
      label: t('settings.alarm.states'),
      editable: false,
      searchProp: 'ruleId',
      searchLabel: t('settings.alarm.ruleId'),
      searchPlaceholder: t('settings.alarm.searchState'),
      filterProp: 'entityStateFlag',
      filterLabel: t('settings.alarm.state'),
      filterPlaceholder: t('settings.alarm.state'),
      filterOptions: ruleStateOptions,
      columns: [
        {prop: 'ruleId', label: t('settings.alarm.ruleId'), kind: 'code', minWidth: 130},
        {prop: 'alarmTargetTypeFlag', label: t('settings.alarm.targetType'), kind: 'tag', width: 110},
        {prop: 'entityId', label: t('settings.alarm.entityId'), kind: 'code', minWidth: 130},
        {prop: 'entityStateFlag', label: t('settings.alarm.state'), kind: 'tag', width: 110},
        {prop: 'triggerCount', label: t('settings.alarm.triggerCount'), width: 110},
        {prop: 'lastTriggerTime', label: t('settings.alarm.lastTriggerTime'), kind: 'time', width: 165},
        ...commonColumns(),
      ],
      fields: [],
      defaultForm: () => ({}),
      list: listRuleState,
    },
    {
      key: 'history',
      label: t('settings.alarm.histories'),
      editable: false,
      searchProp: 'target',
      searchLabel: t('settings.alarm.target'),
      searchPlaceholder: t('settings.alarm.searchHistory'),
      filterProp: 'statusFlag',
      filterLabel: t('settings.alarm.status'),
      filterPlaceholder: t('settings.alarm.status'),
      filterOptions: historyStatusOptions,
      columns: [
        {prop: 'ruleId', label: t('settings.alarm.ruleId'), kind: 'code', minWidth: 130},
        {prop: 'channelId', label: t('settings.alarm.channelId'), kind: 'code', minWidth: 130},
        {prop: 'channelTypeFlag', label: t('settings.alarm.channelType'), kind: 'tag', width: 130},
        {prop: 'target', label: t('settings.alarm.target'), minWidth: 160},
        {prop: 'statusFlag', label: t('settings.alarm.status'), kind: 'tag', width: 110},
        {prop: 'retryCount', label: t('settings.alarm.retryCount'), width: 100},
        {prop: 'errorMessage', label: t('settings.alarm.errorMessage'), minWidth: 180},
        ...commonColumns(),
      ],
      fields: [],
      defaultForm: () => ({}),
      list: listNotifyHistory,
    },
  ];

  return {
    configs,
  };
};
