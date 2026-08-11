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

import {devices, drivers, points, profiles} from './seed/entities';
import {
  identityAudits,
  localCredentials,
  principals,
  rolePrincipalBinds,
  roleResourceBinds,
  roles,
  serviceAccounts,
  tenantMemberships,
  users,
} from './seed/auth';
import {
  apis,
  attributeConfigs,
  attributes,
  commandParams,
  commands,
  eventParams,
  events,
  groups,
  labels,
  resources,
} from './seed/manager';
import {
  alarmChannelBinds,
  alarmChannels,
  alarmHistories,
  alarmMessages,
  alarmNotifies,
  alarmRules,
  alarmRuleStates,
} from './seed/data';
import {
  agenticModelConfigs,
  agenticProviders,
  mcpAudits,
  mcpClients,
  mcpConnections,
  mcpTools,
} from './seed/agentic';
import {
  agenticActions,
  agenticAttachments,
  agenticMessages,
  agenticSessions,
} from './seed/agentic-conversations';

/**
 * Mutable in-memory store. add/update/delete handlers mutate these arrays so a
 * demo session reflects user actions without a backend. Initialized from the
 * static seed; copies so mutation never leaks back into the seed modules.
 */
export interface MockDb {
  drivers: Record<string, unknown>[];
  devices: Record<string, unknown>[];
  profiles: Record<string, unknown>[];
  points: Record<string, unknown>[];
  users: Record<string, unknown>[];
  roles: Record<string, unknown>[];
  rolePrincipalBinds: Record<string, unknown>[];
  roleResourceBinds: Record<string, unknown>[];
  principals: Record<string, unknown>[];
  serviceAccounts: Record<string, unknown>[];
  tenantMemberships: Record<string, unknown>[];
  identityAudits: Record<string, unknown>[];
  localCredentials: Record<string, unknown>[];
  groups: Record<string, unknown>[];
  labels: Record<string, unknown>[];
  apis: Record<string, unknown>[];
  resources: Record<string, unknown>[];
  commands: Record<string, unknown>[];
  commandParams: Record<string, unknown>[];
  events: Record<string, unknown>[];
  eventParams: Record<string, unknown>[];
  attributes: Record<string, unknown>[];
  attributeConfigs: Record<string, unknown>[];
  alarmRules: Record<string, unknown>[];
  alarmNotifies: Record<string, unknown>[];
  alarmMessages: Record<string, unknown>[];
  alarmChannels: Record<string, unknown>[];
  alarmChannelBinds: Record<string, unknown>[];
  alarmRuleStates: Record<string, unknown>[];
  alarmHistories: Record<string, unknown>[];
  agenticProviders: Record<string, unknown>[];
  agenticModelConfigs: Record<string, unknown>[];
  agenticSessions: Record<string, unknown>[];
  agenticMessages: Record<string, unknown>[];
  agenticActions: Record<string, unknown>[];
  agenticAttachments: Record<string, unknown>[];
  mcpClients: Record<string, unknown>[];
  mcpConnections: Record<string, unknown>[];
  mcpTools: Record<string, unknown>[];
  mcpAudits: Record<string, unknown>[];
}

const copy = <T>(rows: T[]): T[] => rows.map((r) => ({...r}));

export const db: MockDb = {
  drivers: copy(drivers),
  devices: copy(devices),
  profiles: copy(profiles),
  points: copy(points),
  users: copy(users),
  roles: copy(roles),
  rolePrincipalBinds: copy(rolePrincipalBinds),
  roleResourceBinds: copy(roleResourceBinds),
  principals: copy(principals),
  serviceAccounts: copy(serviceAccounts),
  tenantMemberships: copy(tenantMemberships),
  identityAudits: copy(identityAudits),
  localCredentials: copy(localCredentials),
  groups: copy(groups),
  labels: copy(labels),
  apis: copy(apis),
  resources: copy(resources),
  commands: copy(commands),
  commandParams: copy(commandParams),
  events: copy(events),
  eventParams: copy(eventParams),
  attributes: copy(attributes),
  attributeConfigs: copy(attributeConfigs),
  alarmRules: copy(alarmRules),
  alarmNotifies: copy(alarmNotifies),
  alarmMessages: copy(alarmMessages),
  alarmChannels: copy(alarmChannels),
  alarmChannelBinds: copy(alarmChannelBinds),
  alarmRuleStates: copy(alarmRuleStates),
  alarmHistories: copy(alarmHistories),
  agenticProviders: copy(agenticProviders),
  agenticModelConfigs: copy(agenticModelConfigs),
  agenticSessions: copy(agenticSessions),
  agenticMessages: copy(agenticMessages),
  agenticActions: copy(agenticActions),
  agenticAttachments: copy(agenticAttachments),
  mcpClients: copy(mcpClients),
  mcpConnections: copy(mcpConnections),
  mcpTools: copy(mcpTools),
  mcpAudits: copy(mcpAudits),
};
