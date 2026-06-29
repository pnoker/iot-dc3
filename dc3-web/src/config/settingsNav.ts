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
  // group containers
  settingsIdentity: 'nav.settingsIdentity',
  settingsAccess: 'nav.settingsAccess',
  settingsModel: 'nav.settingsModel',
  settingsAlarm: 'nav.settingsAlarm',
  settingsEventCommand: 'nav.settingsEventCommand',
  settingsAudit: 'nav.settingsAudit',
  settingsIntegration: 'nav.settingsIntegration',
  settingsSystem: 'nav.settingsSystem',
  // identity
  settingsUser: 'nav.settingsUser',
  settingsPrincipal: 'nav.settingsPrincipal',
  settingsTenantMembership: 'nav.settingsTenantMembership',
  settingsLocalCredential: 'nav.settingsLocalCredential',
  settingsServiceAccount: 'nav.settingsServiceAccount',
  // access control
  settingsRole: 'nav.settingsRole',
  settingsRolePrincipalBind: 'nav.settingsRolePrincipalBind',
  settingsResource: 'nav.settingsResource',
  settingsApi: 'nav.settingsApi',
  settingsMenu: 'nav.settingsMenu',
  // model
  settingsModelConfig: 'nav.settingsModelConfig',
  settingsModelProvider: 'nav.settingsModelProvider',
  // alarm
  settingsAlarmRule: 'nav.settingsAlarmRule',
  settingsAlarmNotify: 'nav.settingsAlarmNotify',
  settingsAlarmMessage: 'nav.settingsAlarmMessage',
  settingsAlarmChannel: 'nav.settingsAlarmChannel',
  settingsAlarmBind: 'nav.settingsAlarmBind',
  settingsAlarmOverview: 'nav.settingsAlarmOverview',
  settingsAlarmState: 'nav.settingsAlarmState',
  settingsAlarmHistory: 'nav.settingsAlarmHistory',
  settingsDriverAlarm: 'nav.settingsDriverAlarm',
  settingsDeviceAlarm: 'nav.settingsDeviceAlarm',
  settingsPointAlarm: 'nav.settingsPointAlarm',
  // event & command
  settingsEventHistory: 'nav.settingsEventHistory',
  settingsCommandHistory: 'nav.settingsCommandHistory',
  // audit
  settingsIdentityAudit: 'nav.settingsIdentityAudit',
  settingsMcpAudit: 'nav.settingsMcpAudit',
  // integration
  settingsMcpServer: 'nav.settingsMcpServer',
  settingsMcpConnection: 'nav.settingsMcpConnection',
  settingsMcpClient: 'nav.settingsMcpClient',
  settingsMcpTool: 'nav.settingsMcpTool',
  // system
  settingsGroup: 'nav.settingsGroup',
  settingsLabel: 'nav.settingsLabel',
  settingsAbout: 'nav.settingsAbout',
  // detail pages
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
  // group containers
  settingsIdentity: 'User',
  settingsAccess: 'Stamp',
  settingsModel: 'Cpu',
  settingsAlarm: 'AlarmClock',
  settingsEventCommand: 'Operation',
  settingsAudit: 'Files',
  settingsIntegration: 'Share',
  settingsSystem: 'Tools',
  // identity
  settingsUser: 'User',
  settingsPrincipal: 'Avatar',
  settingsTenantMembership: 'OfficeBuilding',
  settingsLocalCredential: 'Lock',
  settingsServiceAccount: 'Ticket',
  // access control
  settingsRole: 'UserFilled',
  settingsRolePrincipalBind: 'Link',
  settingsResource: 'Key',
  settingsApi: 'Link',
  settingsMenu: 'Menu',
  // model
  settingsModelConfig: 'ChatDotRound',
  settingsModelProvider: 'ChatLineSquare',
  // alarm
  settingsAlarmRule: 'SetUp',
  settingsAlarmNotify: 'Bell',
  settingsAlarmMessage: 'Message',
  settingsAlarmChannel: 'Connection',
  settingsAlarmBind: 'Link',
  settingsAlarmOverview: 'DataLine',
  settingsAlarmState: 'Monitor',
  settingsAlarmHistory: 'DocumentChecked',
  settingsDriverAlarm: 'Promotion',
  settingsDeviceAlarm: 'Management',
  settingsPointAlarm: 'TrendCharts',
  // event & command
  settingsEventHistory: 'Document',
  settingsCommandHistory: 'Document',
  // audit
  settingsIdentityAudit: 'DocumentChecked',
  settingsMcpAudit: 'Document',
  // integration
  settingsMcpServer: 'Connection',
  settingsMcpConnection: 'Link',
  settingsMcpClient: 'Ticket',
  settingsMcpTool: 'Tools',
  // system
  settingsGroup: 'Grid',
  settingsLabel: 'CollectionTag',
  settingsAbout: 'InfoFilled',
  // detail pages
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

export const SETTINGS_IDENTITY_CHILDREN: SettingsNavNode[] = [
  {name: 'settingsUser', titleKey: 'nav.settingsUser', icon: 'User'},
  {name: 'settingsPrincipal', titleKey: 'nav.settingsPrincipal', icon: 'Avatar'},
  {name: 'settingsTenantMembership', titleKey: 'nav.settingsTenantMembership', icon: 'OfficeBuilding'},
  {name: 'settingsLocalCredential', titleKey: 'nav.settingsLocalCredential', icon: 'Lock'},
  {name: 'settingsServiceAccount', titleKey: 'nav.settingsServiceAccount', icon: 'Ticket'},
];

export const SETTINGS_ACCESS_CHILDREN: SettingsNavNode[] = [
  {name: 'settingsRole', titleKey: 'nav.settingsRole', icon: 'UserFilled'},
  {name: 'settingsRolePrincipalBind', titleKey: 'nav.settingsRolePrincipalBind', icon: 'Link'},
  {name: 'settingsResource', titleKey: 'nav.settingsResource', icon: 'Key'},
  {name: 'settingsApi', titleKey: 'nav.settingsApi', icon: 'Link'},
  {name: 'settingsMenu', titleKey: 'nav.settingsMenu', icon: 'Menu'},
];

export const SETTINGS_MODEL_CHILDREN: SettingsNavNode[] = [
  {name: 'settingsModelConfig', titleKey: 'nav.settingsModelConfig', icon: 'ChatDotRound'},
  {name: 'settingsModelProvider', titleKey: 'nav.settingsModelProvider', icon: 'ChatLineSquare'},
];

// Order reflects two intent groups: configuration first (rule/notify/message/channel/bind),
// then monitoring (overview/state/history + driver/device/point alarm views).
export const SETTINGS_ALARM_CHILDREN: SettingsNavNode[] = [
  {name: 'settingsAlarmRule', titleKey: 'nav.settingsAlarmRule', icon: 'SetUp'},
  {name: 'settingsAlarmNotify', titleKey: 'nav.settingsAlarmNotify', icon: 'Bell'},
  {name: 'settingsAlarmMessage', titleKey: 'nav.settingsAlarmMessage', icon: 'Message'},
  {name: 'settingsAlarmChannel', titleKey: 'nav.settingsAlarmChannel', icon: 'Connection'},
  {name: 'settingsAlarmBind', titleKey: 'nav.settingsAlarmBind', icon: 'Link'},
  {name: 'settingsAlarmOverview', titleKey: 'nav.settingsAlarmOverview', icon: 'DataLine'},
  {name: 'settingsAlarmState', titleKey: 'nav.settingsAlarmState', icon: 'Monitor'},
  {name: 'settingsAlarmHistory', titleKey: 'nav.settingsAlarmHistory', icon: 'DocumentChecked'},
  {name: 'settingsDriverAlarm', titleKey: 'nav.settingsDriverAlarm', icon: 'Promotion'},
  {name: 'settingsDeviceAlarm', titleKey: 'nav.settingsDeviceAlarm', icon: 'Management'},
  {name: 'settingsPointAlarm', titleKey: 'nav.settingsPointAlarm', icon: 'TrendCharts'},
];

export const SETTINGS_EVENT_COMMAND_CHILDREN: SettingsNavNode[] = [
  {name: 'settingsEventHistory', titleKey: 'nav.settingsEventHistory', icon: 'Document'},
  {name: 'settingsCommandHistory', titleKey: 'nav.settingsCommandHistory', icon: 'Document'},
];

export const SETTINGS_AUDIT_CHILDREN: SettingsNavNode[] = [
  {name: 'settingsIdentityAudit', titleKey: 'nav.settingsIdentityAudit', icon: 'DocumentChecked'},
  {name: 'settingsMcpAudit', titleKey: 'nav.settingsMcpAudit', icon: 'Document'},
];

export const SETTINGS_INTEGRATION_CHILDREN: SettingsNavNode[] = [
  {name: 'settingsMcpServer', titleKey: 'nav.settingsMcpServer', icon: 'Connection'},
  {name: 'settingsMcpConnection', titleKey: 'nav.settingsMcpConnection', icon: 'Link'},
  {name: 'settingsMcpClient', titleKey: 'nav.settingsMcpClient', icon: 'Ticket'},
  {name: 'settingsMcpTool', titleKey: 'nav.settingsMcpTool', icon: 'Tools'},
];

export const SETTINGS_SYSTEM_CHILDREN: SettingsNavNode[] = [
  {name: 'settingsGroup', titleKey: 'nav.settingsGroup', icon: 'Grid'},
  {name: 'settingsLabel', titleKey: 'nav.settingsLabel', icon: 'CollectionTag'},
  {name: 'settingsAbout', titleKey: 'nav.settingsAbout', icon: 'InfoFilled'},
];

export const SETTINGS_FALLBACK_SIDEBAR: SettingsNavNode[] = [
  {name: 'settingsIdentity', titleKey: 'nav.settingsIdentity', icon: 'User', children: SETTINGS_IDENTITY_CHILDREN},
  {name: 'settingsAccess', titleKey: 'nav.settingsAccess', icon: 'Stamp', children: SETTINGS_ACCESS_CHILDREN},
  {name: 'settingsModel', titleKey: 'nav.settingsModel', icon: 'Cpu', children: SETTINGS_MODEL_CHILDREN},
  {name: 'settingsAlarm', titleKey: 'nav.settingsAlarm', icon: 'AlarmClock', children: SETTINGS_ALARM_CHILDREN},
  {
    name: 'settingsEventCommand',
    titleKey: 'nav.settingsEventCommand',
    icon: 'Operation',
    children: SETTINGS_EVENT_COMMAND_CHILDREN,
  },
  {name: 'settingsAudit', titleKey: 'nav.settingsAudit', icon: 'Files', children: SETTINGS_AUDIT_CHILDREN},
  {
    name: 'settingsIntegration',
    titleKey: 'nav.settingsIntegration',
    icon: 'Share',
    children: SETTINGS_INTEGRATION_CHILDREN,
  },
  {name: 'settingsSystem', titleKey: 'nav.settingsSystem', icon: 'Tools', children: SETTINGS_SYSTEM_CHILDREN},
];

export const SETTINGS_ACTIVE_ALIAS: Record<string, string> = {
  // group container → its first child (used when a route resolves to a container name)
  settingsIdentity: 'settingsUser',
  settingsAccess: 'settingsRole',
  settingsModel: 'settingsModelConfig',
  settingsAlarm: 'settingsAlarmRule',
  settingsEventCommand: 'settingsEventHistory',
  settingsAudit: 'settingsIdentityAudit',
  settingsIntegration: 'settingsMcpServer',
  settingsSystem: 'settingsGroup',
  // legacy route containers kept for redirect compatibility
  settingsCommand: 'settingsCommandHistory',
  settingsEvent: 'settingsEventHistory',
  // detail pages highlight their list sibling
  settingsModelConfigDetail: 'settingsModelConfig',
  settingsModelProviderDetail: 'settingsModelProvider',
  settingsAlarmRuleDetail: 'settingsAlarmRule',
  settingsAlarmNotifyDetail: 'settingsAlarmNotify',
  settingsAlarmMessageDetail: 'settingsAlarmMessage',
  settingsAlarmChannelDetail: 'settingsAlarmChannel',
  settingsAlarmBindDetail: 'settingsAlarmBind',
  settingsAlarmStateDetail: 'settingsAlarmState',
  settingsAlarmHistoryDetail: 'settingsAlarmHistory',
  settingsUserDetail: 'settingsUser',
  settingsRoleDetail: 'settingsRole',
  settingsResourceDetail: 'settingsResource',
  settingsApiDetail: 'settingsApi',
  settingsMenuDetail: 'settingsMenu',
  settingsGroupDetail: 'settingsGroup',
  settingsLabelDetail: 'settingsLabel',
};

export const SETTINGS_ROUTE_ALIAS: Record<string, string> = {
  settingsIdentity: 'settingsUser',
  settingsAccess: 'settingsRole',
  settingsModel: 'settingsModelConfig',
  settingsAlarm: 'settingsAlarmRule',
  settingsEventCommand: 'settingsEventHistory',
  settingsAudit: 'settingsIdentityAudit',
  settingsIntegration: 'settingsMcpServer',
  settingsSystem: 'settingsGroup',
  settingsCommand: 'settingsCommandHistory',
  settingsEvent: 'settingsEventHistory',
};

export const SETTINGS_GROUP_OPENERS: Record<string, string> = {
  // identity
  settingsUser: 'settingsIdentity',
  settingsPrincipal: 'settingsIdentity',
  settingsTenantMembership: 'settingsIdentity',
  settingsLocalCredential: 'settingsIdentity',
  settingsServiceAccount: 'settingsIdentity',
  // access control
  settingsRole: 'settingsAccess',
  settingsRolePrincipalBind: 'settingsAccess',
  settingsResource: 'settingsAccess',
  settingsApi: 'settingsAccess',
  settingsMenu: 'settingsAccess',
  // model
  settingsModelConfig: 'settingsModel',
  settingsModelProvider: 'settingsModel',
  // alarm
  settingsAlarmRule: 'settingsAlarm',
  settingsAlarmNotify: 'settingsAlarm',
  settingsAlarmMessage: 'settingsAlarm',
  settingsAlarmChannel: 'settingsAlarm',
  settingsAlarmBind: 'settingsAlarm',
  settingsAlarmOverview: 'settingsAlarm',
  settingsAlarmState: 'settingsAlarm',
  settingsAlarmHistory: 'settingsAlarm',
  settingsDriverAlarm: 'settingsAlarm',
  settingsDeviceAlarm: 'settingsAlarm',
  settingsPointAlarm: 'settingsAlarm',
  // event & command
  settingsEventHistory: 'settingsEventCommand',
  settingsCommandHistory: 'settingsEventCommand',
  // audit
  settingsIdentityAudit: 'settingsAudit',
  settingsMcpAudit: 'settingsAudit',
  // integration
  settingsMcpServer: 'settingsIntegration',
  settingsMcpConnection: 'settingsIntegration',
  settingsMcpClient: 'settingsIntegration',
  settingsMcpTool: 'settingsIntegration',
  // system
  settingsGroup: 'settingsSystem',
  settingsLabel: 'settingsSystem',
  settingsAbout: 'settingsSystem',
};

export const getSettingsRouteName = (name: string): string => SETTINGS_ROUTE_ALIAS[name] || name;

export const getSettingsActiveName = (name: string): string => SETTINGS_ACTIVE_ALIAS[name] || name;

export const getSettingsDefaultOpeneds = (activeName: string): string[] => {
  const opener = SETTINGS_GROUP_OPENERS[activeName];
  return opener ? [opener] : [];
};

export const getSettingsTitleKey = (name: string): string | undefined => SETTINGS_TITLE_KEYS[name];

export const getSettingsLeafIconCode = (name: string): string => name;

const identityParent: SettingsBreadcrumbParent = {
  path: '/settings/identity',
  titleKey: 'nav.settingsIdentity',
  code: 'settingsIdentity',
};

const accessParent: SettingsBreadcrumbParent = {
  path: '/settings/access',
  titleKey: 'nav.settingsAccess',
  code: 'settingsAccess',
};

const modelParent: SettingsBreadcrumbParent = {
  path: '/settings/model',
  titleKey: 'nav.settingsModel',
  code: 'settingsModel',
};

const alarmParent: SettingsBreadcrumbParent = {
  path: '/settings/alarm',
  titleKey: 'nav.settingsAlarm',
  code: 'settingsAlarm',
};

const eventCommandParent: SettingsBreadcrumbParent = {
  path: '/settings/event-command',
  titleKey: 'nav.settingsEventCommand',
  code: 'settingsEventCommand',
};

const auditParent: SettingsBreadcrumbParent = {
  path: '/settings/audit',
  titleKey: 'nav.settingsAudit',
  code: 'settingsAudit',
};

const integrationParent: SettingsBreadcrumbParent = {
  path: '/settings/integration',
  titleKey: 'nav.settingsIntegration',
  code: 'settingsIntegration',
};

const systemParent: SettingsBreadcrumbParent = {
  path: '/settings/system',
  titleKey: 'nav.settingsSystem',
  code: 'settingsSystem',
};

export const SETTINGS_BREADCRUMB_PARENTS: Record<string, SettingsBreadcrumbParent[]> = {
  // identity leaves
  settingsUser: [identityParent],
  settingsPrincipal: [identityParent],
  settingsTenantMembership: [identityParent],
  settingsLocalCredential: [identityParent],
  settingsServiceAccount: [identityParent],
  // access control leaves
  settingsRole: [accessParent],
  settingsRolePrincipalBind: [accessParent],
  settingsResource: [accessParent],
  settingsApi: [accessParent],
  settingsMenu: [accessParent],
  // model leaves
  settingsModelConfig: [modelParent],
  settingsModelProvider: [modelParent],
  // alarm leaves
  settingsAlarmRule: [alarmParent],
  settingsAlarmNotify: [alarmParent],
  settingsAlarmMessage: [alarmParent],
  settingsAlarmChannel: [alarmParent],
  settingsAlarmBind: [alarmParent],
  settingsAlarmOverview: [alarmParent],
  settingsAlarmState: [alarmParent],
  settingsAlarmHistory: [alarmParent],
  settingsDriverAlarm: [alarmParent],
  settingsDeviceAlarm: [alarmParent],
  settingsPointAlarm: [alarmParent],
  // event & command leaves
  settingsEventHistory: [eventCommandParent],
  settingsCommandHistory: [eventCommandParent],
  // audit leaves
  settingsIdentityAudit: [auditParent],
  settingsMcpAudit: [auditParent],
  // integration leaves
  settingsMcpServer: [integrationParent],
  settingsMcpConnection: [integrationParent],
  settingsMcpClient: [integrationParent],
  settingsMcpTool: [integrationParent],
  // system leaves
  settingsGroup: [systemParent],
  settingsLabel: [systemParent],
  settingsAbout: [systemParent],
  // detail pages: group → list → detail
  settingsUserDetail: [identityParent, {path: '/settings/user', titleKey: 'nav.settingsUser', code: 'settingsUser'}],
  settingsRoleDetail: [accessParent, {path: '/settings/role', titleKey: 'nav.settingsRole', code: 'settingsRole'}],
  settingsResourceDetail: [
    accessParent,
    {path: '/settings/resource', titleKey: 'nav.settingsResource', code: 'settingsResource'},
  ],
  settingsApiDetail: [accessParent, {path: '/settings/api', titleKey: 'nav.settingsApi', code: 'settingsApi'}],
  settingsMenuDetail: [accessParent, {path: '/settings/menu', titleKey: 'nav.settingsMenu', code: 'settingsMenu'}],
  settingsGroupDetail: [systemParent, {path: '/settings/group', titleKey: 'nav.settingsGroup', code: 'settingsGroup'}],
  settingsLabelDetail: [systemParent, {path: '/settings/label', titleKey: 'nav.settingsLabel', code: 'settingsLabel'}],
  settingsAlarmRuleDetail: [
    alarmParent,
    {path: '/settings/alarm/rule', titleKey: 'nav.settingsAlarmRule', code: 'settingsAlarmRule'},
  ],
  settingsAlarmNotifyDetail: [
    alarmParent,
    {path: '/settings/alarm/notify', titleKey: 'nav.settingsAlarmNotify', code: 'settingsAlarmNotify'},
  ],
  settingsAlarmMessageDetail: [
    alarmParent,
    {path: '/settings/alarm/message', titleKey: 'nav.settingsAlarmMessage', code: 'settingsAlarmMessage'},
  ],
  settingsAlarmChannelDetail: [
    alarmParent,
    {path: '/settings/alarm/channel', titleKey: 'nav.settingsAlarmChannel', code: 'settingsAlarmChannel'},
  ],
  settingsAlarmBindDetail: [
    alarmParent,
    {path: '/settings/alarm/bind', titleKey: 'nav.settingsAlarmBind', code: 'settingsAlarmBind'},
  ],
  settingsAlarmStateDetail: [
    alarmParent,
    {path: '/settings/alarm/state', titleKey: 'nav.settingsAlarmState', code: 'settingsAlarmState'},
  ],
  settingsAlarmHistoryDetail: [
    alarmParent,
    {path: '/settings/alarm/history', titleKey: 'nav.settingsAlarmHistory', code: 'settingsAlarmHistory'},
  ],
  settingsModelConfigDetail: [
    modelParent,
    {path: '/settings/model/config', titleKey: 'nav.settingsModelConfig', code: 'settingsModelConfig'},
  ],
  settingsModelProviderDetail: [
    modelParent,
    {path: '/settings/model/provider', titleKey: 'nav.settingsModelProvider', code: 'settingsModelProvider'},
  ],
};
