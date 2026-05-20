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
  settingsModel: 'nav.settingsModel',
  settingsAgentic: 'nav.settingsAgentic',
  settingsAgenticProvider: 'nav.settingsAgenticProvider',
  // settingsEvent is both the Event group and the Overview route. The route
  // title is the leaf label; the group title is supplied by eventParent below.
  settingsEvent: 'nav.settingsEventOverview',
  settingsEventOverview: 'nav.settingsEventOverview',
  settingsDeviceEvent: 'nav.settingsDeviceEvent',
  settingsDriverEvent: 'nav.settingsDriverEvent',
  settingsPointEvent: 'nav.settingsPointEvent',
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
  settingsAgenticDetail: 'nav.settingsAgenticDetail',
  settingsAgenticProviderDetail: 'nav.settingsAgenticProviderDetail',
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
  settingsModel: 'Cpu',
  settingsAgentic: 'ChatDotRound',
  settingsAgenticProvider: 'ChatLineSquare',
  settingsEvent: 'Bell',
  settingsEventOverview: 'DataLine',
  settingsDeviceEvent: 'Management',
  settingsDriverEvent: 'Promotion',
  settingsPointEvent: 'TrendCharts',
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
  settingsAgenticDetail: 'ChatDotRound',
  settingsAgenticProviderDetail: 'ChatLineSquare',
};

export const SETTINGS_MODEL_CHILDREN: SettingsNavNode[] = [
  { name: 'settingsAgentic', titleKey: 'nav.settingsAgentic', icon: 'ChatDotRound' },
  { name: 'settingsAgenticProvider', titleKey: 'nav.settingsAgenticProvider', icon: 'ChatLineSquare' },
];

export const SETTINGS_ALARM_CHILDREN: SettingsNavNode[] = [
  { name: 'settingsAlarmRule', titleKey: 'nav.settingsAlarmRule', icon: 'SetUp' },
  { name: 'settingsAlarmNotify', titleKey: 'nav.settingsAlarmNotify', icon: 'Bell' },
  { name: 'settingsAlarmMessage', titleKey: 'nav.settingsAlarmMessage', icon: 'Message' },
  { name: 'settingsAlarmChannel', titleKey: 'nav.settingsAlarmChannel', icon: 'Connection' },
  { name: 'settingsAlarmBind', titleKey: 'nav.settingsAlarmBind', icon: 'Link' },
  { name: 'settingsAlarmState', titleKey: 'nav.settingsAlarmState', icon: 'Monitor' },
  { name: 'settingsAlarmHistory', titleKey: 'nav.settingsAlarmHistory', icon: 'DocumentChecked' },
];

export const SETTINGS_EVENT_CHILDREN: SettingsNavNode[] = [
  { name: 'settingsEventOverview', titleKey: 'nav.settingsEventOverview', icon: 'DataLine' },
  { name: 'settingsDriverEvent', titleKey: 'nav.settingsDriverEvent', icon: 'Promotion' },
  { name: 'settingsDeviceEvent', titleKey: 'nav.settingsDeviceEvent', icon: 'Management' },
  { name: 'settingsPointEvent', titleKey: 'nav.settingsPointEvent', icon: 'TrendCharts' },
];

export const SETTINGS_FALLBACK_SIDEBAR: SettingsNavNode[] = [
  { name: 'settingsUser', titleKey: 'nav.settingsUser', icon: 'User' },
  { name: 'settingsRole', titleKey: 'nav.settingsRole', icon: 'UserFilled' },
  { name: 'settingsResource', titleKey: 'nav.settingsResource', icon: 'Key' },
  { name: 'settingsApi', titleKey: 'nav.settingsApi', icon: 'Link' },
  { name: 'settingsMenu', titleKey: 'nav.settingsMenu', icon: 'Menu' },
  { name: 'settingsGroup', titleKey: 'nav.settingsGroup', icon: 'Grid' },
  { name: 'settingsLabel', titleKey: 'nav.settingsLabel', icon: 'CollectionTag' },
  { name: 'settingsAlarm', titleKey: 'nav.settingsAlarm', icon: 'AlarmClock', children: SETTINGS_ALARM_CHILDREN },
  { name: 'settingsModel', titleKey: 'nav.settingsModel', icon: 'Cpu', children: SETTINGS_MODEL_CHILDREN },
  { name: 'settingsEvent', titleKey: 'nav.settingsEvent', icon: 'Bell', children: SETTINGS_EVENT_CHILDREN },
];

export const SETTINGS_ACTIVE_ALIAS: Record<string, string> = {
  settingsModel: 'settingsAgentic',
  settingsAgenticDetail: 'settingsAgentic',
  settingsAgenticProviderDetail: 'settingsAgenticProvider',
  settingsAlarm: 'settingsAlarmRule',
  settingsAlarmRuleDetail: 'settingsAlarmRule',
  settingsAlarmNotifyDetail: 'settingsAlarmNotify',
  settingsAlarmMessageDetail: 'settingsAlarmMessage',
  settingsAlarmChannelDetail: 'settingsAlarmChannel',
  settingsAlarmBindDetail: 'settingsAlarmBind',
  settingsAlarmStateDetail: 'settingsAlarmState',
  settingsAlarmHistoryDetail: 'settingsAlarmHistory',
  settingsEvent: 'settingsEventOverview',
  settingsGroupDetail: 'settingsGroup',
  settingsLabelDetail: 'settingsLabel',
};

export const SETTINGS_ROUTE_ALIAS: Record<string, string> = {
  settingsEventOverview: 'settingsEvent',
  settingsAlarm: 'settingsAlarmRule',
  settingsModel: 'settingsAgentic',
};

export const SETTINGS_GROUP_OPENERS: Record<string, string> = {
  settingsAlarmRule: 'settingsAlarm',
  settingsAlarmNotify: 'settingsAlarm',
  settingsAlarmMessage: 'settingsAlarm',
  settingsAlarmChannel: 'settingsAlarm',
  settingsAlarmBind: 'settingsAlarm',
  settingsAlarmState: 'settingsAlarm',
  settingsAlarmHistory: 'settingsAlarm',
  settingsAgentic: 'settingsModel',
  settingsAgenticProvider: 'settingsModel',
  settingsEventOverview: 'settingsEvent',
  settingsDeviceEvent: 'settingsEvent',
  settingsDriverEvent: 'settingsEvent',
  settingsPointEvent: 'settingsEvent',
};

export const getSettingsRouteName = (name: string): string => SETTINGS_ROUTE_ALIAS[name] || name;

export const getSettingsActiveName = (name: string): string => SETTINGS_ACTIVE_ALIAS[name] || name;

export const getSettingsDefaultOpeneds = (activeName: string): string[] => {
  const opener = SETTINGS_GROUP_OPENERS[activeName];
  return opener ? [opener] : [];
};

export const getSettingsTitleKey = (name: string): string | undefined => SETTINGS_TITLE_KEYS[name];

export const getSettingsLeafIconCode = (name: string): string =>
  name === 'settingsEvent' ? 'settingsEventOverview' : name;

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
  settingsAgentic: [modelParent],
  settingsAgenticProvider: [modelParent],
  settingsAgenticDetail: [
    modelParent,
    { path: '/settings/agentic', titleKey: 'nav.settingsAgentic', code: 'settingsAgentic' },
  ],
  settingsAgenticProviderDetail: [
    modelParent,
    { path: '/settings/agentic/provider', titleKey: 'nav.settingsAgenticProvider', code: 'settingsAgenticProvider' },
  ],
  settingsEvent: [eventParent],
  settingsDeviceEvent: [eventParent],
  settingsDriverEvent: [eventParent],
  settingsPointEvent: [eventParent],
};
