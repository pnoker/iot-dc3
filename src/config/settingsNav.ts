/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export interface SettingsNavNode {
  name: string;
  titleKey: string;
  icon?: string;
  children?: SettingsNavNode[];
}

export interface SettingsBreadcrumbParent {
  path: string;
  titleKey: string;
  code: string;
}

export const SETTINGS_TITLE_KEYS: Record<string, string> = {
  settingsUser: 'nav.settingsUser',
  settingsRole: 'nav.settingsRole',
  settingsResource: 'nav.settingsResource',
  settingsApi: 'nav.settingsApi',
  settingsMenu: 'nav.settingsMenu',
  settingsGroup: 'nav.settingsGroup',
  settingsLabel: 'nav.settingsLabel',
  settingsAlarm: 'nav.settingsAlarm',
  settingsAlarmRule: 'nav.settingsAlarmRule',
  settingsAlarmNotify: 'nav.settingsAlarmNotify',
  settingsAlarmMessage: 'nav.settingsAlarmMessage',
  settingsAlarmChannel: 'nav.settingsAlarmChannel',
  settingsAlarmBind: 'nav.settingsAlarmBind',
  settingsAlarmState: 'nav.settingsAlarmState',
  settingsAlarmHistory: 'nav.settingsAlarmHistory',
  settingsDeviceAlarm: 'nav.settingsDeviceAlarm',
  settingsDriverAlarm: 'nav.settingsDriverAlarm',
  settingsModel: 'nav.settingsModel',
  settingsModelConfig: 'nav.settingsModelConfig',
  settingsModelProvider: 'nav.settingsModelProvider',
  settingsEvent: 'nav.settingsEvent',
  settingsAlarmOverview: 'nav.settingsAlarmOverview',
  settingsPointAlarm: 'nav.settingsPointAlarm',
  settingsCommand: 'nav.settingsCommand',
  settingsCommandHistory: 'nav.settingsCommandHistory',
  settingsEventHistory: 'nav.settingsEventHistory',
  settingsAbout: 'nav.settingsAbout',
  settingsUserDetail: 'nav.settingsUserDetail',
  settingsRoleDetail: 'nav.settingsRoleDetail',
  settingsResourceDetail: 'nav.settingsResourceDetail',
  settingsApiDetail: 'nav.settingsApiDetail',
  settingsMenuDetail: 'nav.settingsMenuDetail',
  settingsGroupDetail: 'nav.settingsGroupDetail',
  settingsLabelDetail: 'nav.settingsLabelDetail',
  settingsAlarmRuleDetail: 'nav.settingsAlarmRuleDetail',
  settingsAlarmNotifyDetail: 'nav.settingsAlarmNotifyDetail',
  settingsAlarmMessageDetail: 'nav.settingsAlarmMessageDetail',
  settingsAlarmChannelDetail: 'nav.settingsAlarmChannelDetail',
  settingsAlarmBindDetail: 'nav.settingsAlarmBindDetail',
  settingsAlarmStateDetail: 'nav.settingsAlarmStateDetail',
  settingsAlarmHistoryDetail: 'nav.settingsAlarmHistoryDetail',
  settingsModelConfigDetail: 'nav.settingsModelConfigDetail',
  settingsModelProviderDetail: 'nav.settingsModelProviderDetail',
};

export const SETTINGS_FALLBACK_ICON: Record<string, string> = {
  settings: 'Setting',
  settingsUser: 'User',
  settingsRole: 'UserFilled',
  settingsResource: 'Key',
  settingsApi: 'Link',
  settingsMenu: 'Menu',
  settingsGroup: 'Grid',
  settingsLabel: 'CollectionTag',
  settingsAlarm: 'AlarmClock',
  settingsAlarmRule: 'SetUp',
  settingsAlarmNotify: 'Bell',
  settingsAlarmMessage: 'Message',
  settingsAlarmChannel: 'Connection',
  settingsAlarmBind: 'Link',
  settingsAlarmState: 'Monitor',
  settingsAlarmHistory: 'DocumentChecked',
  settingsDeviceAlarm: 'Management',
  settingsDriverAlarm: 'Promotion',
  settingsModel: 'Cpu',
  settingsModelConfig: 'ChatDotRound',
  settingsModelProvider: 'ChatLineSquare',
  settingsEvent: 'Bell',
  settingsAlarmOverview: 'DataLine',
  settingsPointAlarm: 'TrendCharts',
  settingsCommand: 'Operation',
  settingsCommandHistory: 'Document',
  settingsEventHistory: 'Document',
  settingsAbout: 'InfoFilled',
  settingsUserDetail: 'User',
  settingsRoleDetail: 'UserFilled',
  settingsResourceDetail: 'Key',
  settingsApiDetail: 'Link',
  settingsMenuDetail: 'Menu',
  settingsGroupDetail: 'Grid',
  settingsLabelDetail: 'CollectionTag',
  settingsAlarmRuleDetail: 'SetUp',
  settingsAlarmNotifyDetail: 'Bell',
  settingsAlarmMessageDetail: 'Message',
  settingsAlarmChannelDetail: 'Connection',
  settingsAlarmBindDetail: 'Link',
  settingsAlarmStateDetail: 'Monitor',
  settingsAlarmHistoryDetail: 'DocumentChecked',
  settingsModelConfigDetail: 'ChatDotRound',
  settingsModelProviderDetail: 'ChatLineSquare',
};

export const SETTINGS_MODEL_CHILDREN: SettingsNavNode[] = [
  { name: 'settingsModelConfig', titleKey: 'nav.settingsModelConfig', icon: 'ChatDotRound' },
  { name: 'settingsModelProvider', titleKey: 'nav.settingsModelProvider', icon: 'ChatLineSquare' },
];

export const SETTINGS_ALARM_CHILDREN: SettingsNavNode[] = [
  { name: 'settingsAlarmOverview', titleKey: 'nav.settingsAlarmOverview', icon: 'DataLine' },
  { name: 'settingsAlarmRule', titleKey: 'nav.settingsAlarmRule', icon: 'SetUp' },
  { name: 'settingsAlarmNotify', titleKey: 'nav.settingsAlarmNotify', icon: 'Bell' },
  { name: 'settingsAlarmMessage', titleKey: 'nav.settingsAlarmMessage', icon: 'Message' },
  { name: 'settingsAlarmChannel', titleKey: 'nav.settingsAlarmChannel', icon: 'Connection' },
  { name: 'settingsAlarmBind', titleKey: 'nav.settingsAlarmBind', icon: 'Link' },
  { name: 'settingsAlarmState', titleKey: 'nav.settingsAlarmState', icon: 'Monitor' },
  { name: 'settingsAlarmHistory', titleKey: 'nav.settingsAlarmHistory', icon: 'DocumentChecked' },
  { name: 'settingsDriverAlarm', titleKey: 'nav.settingsDriverAlarm', icon: 'Promotion' },
  { name: 'settingsDeviceAlarm', titleKey: 'nav.settingsDeviceAlarm', icon: 'Management' },
  { name: 'settingsPointAlarm', titleKey: 'nav.settingsPointAlarm', icon: 'TrendCharts' },
];

export const SETTINGS_EVENT_CHILDREN: SettingsNavNode[] = [
  { name: 'settingsEventHistory', titleKey: 'nav.settingsEventHistory', icon: 'Document' },
];

export const SETTINGS_COMMAND_CHILDREN: SettingsNavNode[] = [
  { name: 'settingsCommandHistory', titleKey: 'nav.settingsCommandHistory', icon: 'Document' },
];

export const SETTINGS_FALLBACK_SIDEBAR: SettingsNavNode[] = [
  { name: 'settingsUser', titleKey: 'nav.settingsUser', icon: 'User' },
  { name: 'settingsRole', titleKey: 'nav.settingsRole', icon: 'UserFilled' },
  { name: 'settingsModel', titleKey: 'nav.settingsModel', icon: 'Cpu', children: SETTINGS_MODEL_CHILDREN },
  { name: 'settingsCommand', titleKey: 'nav.settingsCommand', icon: 'Operation', children: SETTINGS_COMMAND_CHILDREN },
  { name: 'settingsEvent', titleKey: 'nav.settingsEvent', icon: 'Bell', children: SETTINGS_EVENT_CHILDREN },
  { name: 'settingsAlarm', titleKey: 'nav.settingsAlarm', icon: 'AlarmClock', children: SETTINGS_ALARM_CHILDREN },
  { name: 'settingsApi', titleKey: 'nav.settingsApi', icon: 'Link' },
  { name: 'settingsResource', titleKey: 'nav.settingsResource', icon: 'Key' },
  { name: 'settingsMenu', titleKey: 'nav.settingsMenu', icon: 'Menu' },
  { name: 'settingsGroup', titleKey: 'nav.settingsGroup', icon: 'Grid' },
  { name: 'settingsLabel', titleKey: 'nav.settingsLabel', icon: 'CollectionTag' },
];

export const SETTINGS_ACTIVE_ALIAS: Record<string, string> = {
  settingsModel: 'settingsModelConfig',
  settingsModelConfigDetail: 'settingsModelConfig',
  settingsModelProviderDetail: 'settingsModelProvider',
  settingsAlarm: 'settingsAlarmRule',
  settingsAlarmRuleDetail: 'settingsAlarmRule',
  settingsAlarmNotifyDetail: 'settingsAlarmNotify',
  settingsAlarmMessageDetail: 'settingsAlarmMessage',
  settingsAlarmChannelDetail: 'settingsAlarmChannel',
  settingsAlarmBindDetail: 'settingsAlarmBind',
  settingsAlarmStateDetail: 'settingsAlarmState',
  settingsAlarmHistoryDetail: 'settingsAlarmHistory',
  settingsCommand: 'settingsCommandHistory',
  settingsGroupDetail: 'settingsGroup',
  settingsLabelDetail: 'settingsLabel',
};

export const SETTINGS_ROUTE_ALIAS: Record<string, string> = {
  settingsAlarm: 'settingsAlarmRule',
  settingsModel: 'settingsModelConfig',
  settingsCommand: 'settingsCommandHistory',
};

export const SETTINGS_GROUP_OPENERS: Record<string, string> = {
  settingsAlarmRule: 'settingsAlarm',
  settingsAlarmNotify: 'settingsAlarm',
  settingsAlarmMessage: 'settingsAlarm',
  settingsAlarmChannel: 'settingsAlarm',
  settingsAlarmBind: 'settingsAlarm',
  settingsAlarmState: 'settingsAlarm',
  settingsAlarmHistory: 'settingsAlarm',
  settingsDeviceAlarm: 'settingsAlarm',
  settingsDriverAlarm: 'settingsAlarm',
  settingsAlarmOverview: 'settingsAlarm',
  settingsPointAlarm: 'settingsAlarm',
  settingsModelConfig: 'settingsModel',
  settingsModelProvider: 'settingsModel',
  settingsCommandHistory: 'settingsCommand',
  settingsEventHistory: 'settingsEvent',
};

export const getSettingsRouteName = (name: string): string => SETTINGS_ROUTE_ALIAS[name] || name;

export const getSettingsActiveName = (name: string): string => SETTINGS_ACTIVE_ALIAS[name] || name;

export const getSettingsDefaultOpeneds = (activeName: string): string[] => {
  const opener = SETTINGS_GROUP_OPENERS[activeName];
  return opener ? [opener] : [];
};

export const getSettingsTitleKey = (name: string): string | undefined => SETTINGS_TITLE_KEYS[name];

export const getSettingsLeafIconCode = (name: string): string => name;

const commandParent: SettingsBreadcrumbParent = {
  path: '/settings/command/history',
  titleKey: 'nav.settingsCommand',
  code: 'settingsCommand',
};

const alarmParent: SettingsBreadcrumbParent = {
  path: '/settings/alarm',
  titleKey: 'nav.settingsAlarm',
  code: 'settingsAlarm',
};

const modelParent: SettingsBreadcrumbParent = {
  path: '/settings/model',
  titleKey: 'nav.settingsModel',
  code: 'settingsModel',
};

const eventParent: SettingsBreadcrumbParent = {
  path: '/settings/event',
  titleKey: 'nav.settingsEvent',
  code: 'settingsEvent',
};

export const SETTINGS_BREADCRUMB_PARENTS: Record<string, SettingsBreadcrumbParent[]> = {
  settingsUserDetail: [{ path: '/settings/user', titleKey: 'nav.settingsUser', code: 'settingsUser' }],
  settingsRoleDetail: [{ path: '/settings/role', titleKey: 'nav.settingsRole', code: 'settingsRole' }],
  settingsResourceDetail: [{ path: '/settings/resource', titleKey: 'nav.settingsResource', code: 'settingsResource' }],
  settingsApiDetail: [{ path: '/settings/api', titleKey: 'nav.settingsApi', code: 'settingsApi' }],
  settingsMenuDetail: [{ path: '/settings/menu', titleKey: 'nav.settingsMenu', code: 'settingsMenu' }],
  settingsGroupDetail: [{ path: '/settings/group', titleKey: 'nav.settingsGroup', code: 'settingsGroup' }],
  settingsLabelDetail: [{ path: '/settings/label', titleKey: 'nav.settingsLabel', code: 'settingsLabel' }],
  settingsAlarmRule: [alarmParent],
  settingsAlarmNotify: [alarmParent],
  settingsAlarmMessage: [alarmParent],
  settingsAlarmChannel: [alarmParent],
  settingsAlarmBind: [alarmParent],
  settingsAlarmState: [alarmParent],
  settingsAlarmHistory: [alarmParent],
  settingsDeviceAlarm: [alarmParent],
  settingsDriverAlarm: [alarmParent],
  settingsAlarmRuleDetail: [
    alarmParent,
    { path: '/settings/alarm/rule', titleKey: 'nav.settingsAlarmRule', code: 'settingsAlarmRule' },
  ],
  settingsAlarmNotifyDetail: [
    alarmParent,
    { path: '/settings/alarm/notify', titleKey: 'nav.settingsAlarmNotify', code: 'settingsAlarmNotify' },
  ],
  settingsAlarmMessageDetail: [
    alarmParent,
    { path: '/settings/alarm/message', titleKey: 'nav.settingsAlarmMessage', code: 'settingsAlarmMessage' },
  ],
  settingsAlarmChannelDetail: [
    alarmParent,
    { path: '/settings/alarm/channel', titleKey: 'nav.settingsAlarmChannel', code: 'settingsAlarmChannel' },
  ],
  settingsAlarmBindDetail: [
    alarmParent,
    { path: '/settings/alarm/bind', titleKey: 'nav.settingsAlarmBind', code: 'settingsAlarmBind' },
  ],
  settingsAlarmStateDetail: [
    alarmParent,
    { path: '/settings/alarm/state', titleKey: 'nav.settingsAlarmState', code: 'settingsAlarmState' },
  ],
  settingsAlarmHistoryDetail: [
    alarmParent,
    { path: '/settings/alarm/history', titleKey: 'nav.settingsAlarmHistory', code: 'settingsAlarmHistory' },
  ],
  settingsModelConfig: [modelParent],
  settingsModelProvider: [modelParent],
  settingsModelConfigDetail: [
    modelParent,
    { path: '/settings/model/config', titleKey: 'nav.settingsModelConfig', code: 'settingsModelConfig' },
  ],
  settingsModelProviderDetail: [
    modelParent,
    { path: '/settings/model/provider', titleKey: 'nav.settingsModelProvider', code: 'settingsModelProvider' },
  ],
  settingsAlarmOverview: [alarmParent],
  settingsPointAlarm: [alarmParent],
  settingsCommandHistory: [commandParent],
  settingsEventHistory: [eventParent],
};
