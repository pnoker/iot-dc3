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

type FlagValue = string | number | null | undefined;

const COMMAND_TYPE_BY_INDEX: Record<string, string> = {
  '0': 'CUSTOM',
  '1': 'CONFIG',
  '2': 'ACTION',
};

const CALL_TYPE_BY_INDEX: Record<string, string> = {
  '0': 'SYNC',
  '1': 'ASYNC',
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

function normalizeFlag(value: FlagValue, indexMap: Record<string, string>): string {
  if (value === null || value === undefined || value === '') {
    return '-';
  }

  const raw = String(value).trim();
  return indexMap[raw] ?? raw.toUpperCase();
}

export function isEnabledFlag(value: FlagValue): boolean {
  return String(value).toUpperCase() === 'ENABLE' || Number(value) === 0;
}

export function commandTypeLabel(value: FlagValue): string {
  return normalizeFlag(value, COMMAND_TYPE_BY_INDEX);
}

export function callTypeLabel(value: FlagValue): string {
  return normalizeFlag(value, CALL_TYPE_BY_INDEX);
}

export function eventTypeLabel(value: FlagValue): string {
  return normalizeFlag(value, EVENT_TYPE_BY_INDEX);
}

export function eventLevelLabel(value: FlagValue): string {
  return normalizeFlag(value, EVENT_LEVEL_BY_INDEX);
}

export function eventLevelTag(value: FlagValue): 'success' | 'warning' | 'danger' | 'info' {
  const level = eventLevelLabel(value);
  if (level === 'CRITICAL') return 'danger';
  if (level === 'HIGH') return 'warning';
  if (level === 'MEDIUM') return 'success';
  return 'info';
}
