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

export function isEnabledFlag(value: FlagValue): boolean {
  if (value === true) return true;
  if (value === false || value === null || value === undefined || value === '') return false;

  const normalized = String(value).trim().toUpperCase();
  return normalized === 'ENABLE' || normalized === 'ENABLED' || normalized === 'TRUE' || normalized === '0';
}

export function enableFlagValue(value: FlagValue, fallback = 'ENABLE'): string {
  return normalizeFormFlag(value, ENABLE_FLAG_BY_INDEX, fallback);
}

export function commandTypeLabel(value: FlagValue): string {
  return normalizeFlag(value, COMMAND_TYPE_BY_INDEX);
}

export function commandTypeValue(value: FlagValue, fallback = 'CUSTOM'): string {
  return normalizeFormFlag(value, COMMAND_TYPE_BY_INDEX, fallback);
}

export function callTypeLabel(value: FlagValue): string {
  return normalizeFlag(value, CALL_TYPE_BY_INDEX);
}

export function callTypeValue(value: FlagValue, fallback = 'SYNC'): string {
  return normalizeFormFlag(value, CALL_TYPE_BY_INDEX, fallback);
}

export function paramDirectionValue(value: FlagValue, fallback = 'INPUT'): string {
  return normalizeFormFlag(value, PARAM_DIRECTION_BY_INDEX, fallback);
}

export function pointTypeValue(value: FlagValue, fallback = 'STRING'): string {
  return normalizeFormFlag(value, POINT_TYPE_BY_INDEX, fallback);
}

export function eventTypeLabel(value: FlagValue): string {
  return normalizeFlag(value, EVENT_TYPE_BY_INDEX);
}

export function eventTypeValue(value: FlagValue, fallback = 'INFO'): string {
  return normalizeFormFlag(value, EVENT_TYPE_BY_INDEX, fallback);
}

export function eventLevelLabel(value: FlagValue): string {
  return normalizeFlag(value, EVENT_LEVEL_BY_INDEX);
}

export function eventLevelValue(value: FlagValue, fallback = 'LOW'): string {
  return normalizeFormFlag(value, EVENT_LEVEL_BY_INDEX, fallback);
}

export function eventLevelTag(value: FlagValue): 'success' | 'warning' | 'danger' | 'info' {
  const level = eventLevelLabel(value);
  if (level === 'CRITICAL') return 'danger';
  if (level === 'HIGH') return 'warning';
  if (level === 'MEDIUM') return 'success';
  return 'info';
}

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

export function commandTimeoutLabel(value: TimeoutValue): string {
  return normalizeCommandTimeoutSeconds(value)?.toString() ?? '-';
}
