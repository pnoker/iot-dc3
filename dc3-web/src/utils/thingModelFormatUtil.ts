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

type FlagValue = string | number | boolean | null | undefined;
type TimeoutValue = string | number | null | undefined;

const LEGACY_TIMEOUT_MILLISECONDS_THRESHOLD = 1000;
const MILLISECONDS_PER_SECOND = 1000;

const COMMAND_TYPE_BY_INDEX: Record<string, string> = {
  '0': 'CUSTOM',
  '1': 'CONFIG',
  '2': 'ACTION',
};

const CALL_TYPE_BY_INDEX: Record<string, string> = {
  '0': 'SYNC',
  '1': 'ASYNC',
};

const PARAM_DIRECTION_BY_INDEX: Record<string, string> = {
  '0': 'INPUT',
  '1': 'OUTPUT',
};

const POINT_TYPE_BY_INDEX: Record<string, string> = {
  '0': 'STRING',
  '1': 'BYTE',
  '2': 'SHORT',
  '3': 'INT',
  '4': 'LONG',
  '5': 'FLOAT',
  '6': 'DOUBLE',
  '7': 'BOOLEAN',
};

const EVENT_TYPE_BY_INDEX: Record<string, string> = {
  '0': 'INFO',
  '1': 'ALERT',
  '2': 'FAULT',
  '3': 'LIFECYCLE',
};

const EVENT_LEVEL_BY_INDEX: Record<string, string> = {
  '0': 'LOW',
  '1': 'MEDIUM',
  '2': 'HIGH',
  '3': 'CRITICAL',
};

const ENABLE_FLAG_BY_INDEX: Record<string, string> = {
  '0': 'ENABLE',
  '1': 'DISABLE',
};

function normalizeFlag(value: FlagValue, indexMap: Record<string, string>): string {
  if (value === null || value === undefined || value === '') {
    return '-';
  }

  const raw = String(value).trim();
  return indexMap[raw] ?? raw.toUpperCase();
}

function normalizeFormFlag(value: FlagValue, indexMap: Record<string, string>, fallback: string): string {
  const normalized = normalizeFlag(value, indexMap);
  return normalized === '-' ? fallback : normalized;
}

/** Whether a backend enable flag (ENABLE/0/true forms) means enabled. */
export function isEnabledFlag(value: FlagValue): boolean {
  if (value === true) return true;
  if (value === false || value === null || value === undefined || value === '') return false;

  const normalized = String(value).trim().toUpperCase();
  return normalized === 'ENABLE' || normalized === 'ENABLED' || normalized === 'TRUE' || normalized === '0';
}

/** Normalize an enable flag to the ENABLE/DISABLE wire token. */
export function enableFlagValue(value: FlagValue, fallback = 'ENABLE'): string {
  return normalizeFormFlag(value, ENABLE_FLAG_BY_INDEX, fallback);
}

/** Display token for a command type flag, or dash when absent. */
export function commandTypeLabel(value: FlagValue): string {
  return normalizeFlag(value, COMMAND_TYPE_BY_INDEX);
}

/** Normalize a command type flag to its wire token. */
export function commandTypeValue(value: FlagValue, fallback = 'CUSTOM'): string {
  return normalizeFormFlag(value, COMMAND_TYPE_BY_INDEX, fallback);
}

/** Display token for a call type flag, or dash when absent. */
export function callTypeLabel(value: FlagValue): string {
  return normalizeFlag(value, CALL_TYPE_BY_INDEX);
}

/** Normalize a call type flag to its wire token. */
export function callTypeValue(value: FlagValue, fallback = 'SYNC'): string {
  return normalizeFormFlag(value, CALL_TYPE_BY_INDEX, fallback);
}

/** Normalize a param direction flag to its wire token. */
export function paramDirectionValue(value: FlagValue, fallback = 'INPUT'): string {
  return normalizeFormFlag(value, PARAM_DIRECTION_BY_INDEX, fallback);
}

/** Normalize a point type flag to its wire token. */
export function pointTypeValue(value: FlagValue, fallback = 'STRING'): string {
  return normalizeFormFlag(value, POINT_TYPE_BY_INDEX, fallback);
}

/** Display token for an event type flag, or dash when absent. */
export function eventTypeLabel(value: FlagValue): string {
  return normalizeFlag(value, EVENT_TYPE_BY_INDEX);
}

/** Normalize an event type flag to its wire token. */
export function eventTypeValue(value: FlagValue, fallback = 'INFO'): string {
  return normalizeFormFlag(value, EVENT_TYPE_BY_INDEX, fallback);
}

/** Display token for an event level flag, or dash when absent. */
export function eventLevelLabel(value: FlagValue): string {
  return normalizeFlag(value, EVENT_LEVEL_BY_INDEX);
}

/** Normalize an event level flag to its wire token. */
export function eventLevelValue(value: FlagValue, fallback = 'LOW'): string {
  return normalizeFormFlag(value, EVENT_LEVEL_BY_INDEX, fallback);
}

/** Element Plus tag type for an event level. */
export function eventLevelTag(value: FlagValue): 'success' | 'warning' | 'danger' | 'info' {
  const level = eventLevelLabel(value);
  if (level === 'CRITICAL') return 'danger';
  if (level === 'HIGH') return 'warning';
  if (level === 'MEDIUM') return 'success';
  return 'info';
}

/** Normalize a command timeout to seconds, migrating legacy millisecond values. */
export function normalizeCommandTimeoutSeconds(value: TimeoutValue): number | undefined {
  if (value === null || value === undefined || value === '') {
    return undefined;
  }

  const timeout = Number(value);
  if (!Number.isFinite(timeout) || timeout <= 0) {
    return undefined;
  }

  if (timeout >= LEGACY_TIMEOUT_MILLISECONDS_THRESHOLD && timeout % MILLISECONDS_PER_SECOND === 0) {
    return timeout / MILLISECONDS_PER_SECOND;
  }

  return timeout;
}

/** Human label for a command timeout, or dash when unset. */
export function commandTimeoutLabel(value: TimeoutValue): string {
  return normalizeCommandTimeoutSeconds(value)?.toString() ?? '-';
}

// Alarm helpers — wire format is integer (0..4 for type, 0..3 for level),
// distinct from the string-coded EventTypeFlag/EventLevelFlag enums above.
// Backend enum is `AlarmTypeEnum`; level uses P0..P3 priority labels.

const ALARM_TYPE_LABEL_BY_INDEX: Record<number, string> = {
  0: 'RULE',
  1: 'OFFLINE',
  2: 'FAULT',
  3: 'STATE_FLIP',
  4: 'REPORT',
};

const ALARM_TYPE_TAG_BY_INDEX: Record<number, 'info' | 'warning' | 'danger'> = {
  0: 'info',
  1: 'danger',
  2: 'danger',
  3: 'warning',
  4: 'info',
};

const ALARM_LEVEL_LABEL_BY_INDEX: Record<number, string> = {
  0: 'P0',
  1: 'P1',
  2: 'P2',
  3: 'P3',
};

const ALARM_LEVEL_TAG_BY_INDEX: Record<number, 'info' | 'success' | 'warning' | 'danger'> = {
  0: 'danger',
  1: 'warning',
  2: 'info',
  3: 'success',
};

/** Select options for alarm types. */
export const ALARM_TYPE_OPTIONS: ReadonlyArray<{ value: number; label: string }> = [
  {value: 0, label: 'RULE'},
  {value: 1, label: 'OFFLINE'},
  {value: 2, label: 'FAULT'},
  {value: 3, label: 'STATE_FLIP'},
  {value: 4, label: 'REPORT'},
];

/** Display token for an alarm type index. */
export function alarmTypeLabel(flag: number | null | undefined): string {
  if (flag === null || flag === undefined) return '-';
  return ALARM_TYPE_LABEL_BY_INDEX[flag] ?? String(flag);
}

/** Element Plus tag type for an alarm type index. */
export function alarmTypeTag(flag: number | null | undefined): 'info' | 'warning' | 'danger' {
  if (flag === null || flag === undefined) return 'info';
  return ALARM_TYPE_TAG_BY_INDEX[flag] ?? 'info';
}

/** Priority label (P0..P3) for an alarm level index. */
export function alarmLevelLabel(flag: number | null | undefined): string {
  if (flag === null || flag === undefined) return '—';
  return ALARM_LEVEL_LABEL_BY_INDEX[flag] ?? '—';
}

/** Element Plus tag type for an alarm level index. */
export function alarmLevelTag(flag: number | null | undefined): 'info' | 'success' | 'warning' | 'danger' {
  if (flag === null || flag === undefined) return 'info';
  return ALARM_LEVEL_TAG_BY_INDEX[flag] ?? 'info';
}

