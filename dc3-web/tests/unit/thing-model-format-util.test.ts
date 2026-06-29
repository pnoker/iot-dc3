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

import {describe, expect, it} from 'vitest';

import {
  ALARM_TYPE_OPTIONS,
  alarmLevelLabel,
  alarmLevelTag,
  alarmTypeLabel,
  alarmTypeTag,
  callTypeLabel,
  callTypeValue,
  commandTimeoutLabel,
  commandTypeLabel,
  commandTypeValue,
  enableFlagValue,
  eventLevelLabel,
  eventLevelTag,
  eventLevelValue,
  eventTypeLabel,
  eventTypeValue,
  isEnabledFlag,
  normalizeCommandTimeoutSeconds,
  paramDirectionValue,
  pointTypeValue,
} from '@/utils/thingModelFormatUtil';

describe('thingModelFormatUtil', () => {
  it('normalizes enable flags from legacy indexes and textual values', () => {
    expect(isEnabledFlag(true)).toBe(true);
    expect(isEnabledFlag('enabled')).toBe(true);
    expect(isEnabledFlag('0')).toBe(true);
    expect(isEnabledFlag(false)).toBe(false);
    expect(isEnabledFlag('1')).toBe(false);
    expect(isEnabledFlag('')).toBe(false);

    expect(enableFlagValue('0')).toBe('ENABLE');
    expect(enableFlagValue('1')).toBe('DISABLE');
    expect(enableFlagValue('', 'DISABLE')).toBe('DISABLE');
    expect(enableFlagValue('custom')).toBe('CUSTOM');
  });

  it('maps command and parameter enum indexes to form values', () => {
    expect(commandTypeLabel(null)).toBe('-');
    expect(commandTypeLabel('0')).toBe('CUSTOM');
    expect(commandTypeValue('1')).toBe('CONFIG');
    expect(commandTypeValue('', 'ACTION')).toBe('ACTION');

    expect(callTypeLabel('1')).toBe('ASYNC');
    expect(callTypeValue(undefined, 'ASYNC')).toBe('ASYNC');
    expect(paramDirectionValue('1')).toBe('OUTPUT');
    expect(paramDirectionValue('', 'INPUT')).toBe('INPUT');
    expect(pointTypeValue('6')).toBe('DOUBLE');
  });

  it('maps event enum indexes and derives level tag types', () => {
    expect(eventTypeLabel('2')).toBe('FAULT');
    expect(eventTypeValue('', 'LIFECYCLE')).toBe('LIFECYCLE');

    expect(eventLevelLabel('0')).toBe('LOW');
    expect(eventLevelValue('3')).toBe('CRITICAL');
    expect(eventLevelValue('', 'MEDIUM')).toBe('MEDIUM');
    expect(eventLevelTag('3')).toBe('danger');
    expect(eventLevelTag('2')).toBe('warning');
    expect(eventLevelTag('1')).toBe('success');
    expect(eventLevelTag('0')).toBe('info');
  });

  it('exposes alarm type options and integer-keyed label/tag helpers', () => {
    expect(ALARM_TYPE_OPTIONS).toEqual([
      {value: 0, label: 'RULE'},
      {value: 1, label: 'OFFLINE'},
      {value: 2, label: 'FAULT'},
      {value: 3, label: 'STATE_FLIP'},
      {value: 4, label: 'REPORT'},
    ]);

    expect(alarmTypeLabel(0)).toBe('RULE');
    expect(alarmTypeLabel(4)).toBe('REPORT');
    expect(alarmTypeLabel(99)).toBe('99');
    expect(alarmTypeLabel(null)).toBe('-');
    expect(alarmTypeLabel(undefined)).toBe('-');

    expect(alarmTypeTag(0)).toBe('info');
    expect(alarmTypeTag(1)).toBe('danger');
    expect(alarmTypeTag(2)).toBe('danger');
    expect(alarmTypeTag(3)).toBe('warning');
    expect(alarmTypeTag(4)).toBe('info');
    expect(alarmTypeTag(99)).toBe('info');
    expect(alarmTypeTag(null)).toBe('info');
  });

  it('maps alarm levels to P0..P3 priority labels and severity-ordered tags', () => {
    expect(alarmLevelLabel(0)).toBe('P0');
    expect(alarmLevelLabel(3)).toBe('P3');
    expect(alarmLevelLabel(undefined)).toBe('—');
    expect(alarmLevelLabel(99)).toBe('—');

    // P0 is highest priority → most severe (danger); P3 → recovered (success).
    expect(alarmLevelTag(0)).toBe('danger');
    expect(alarmLevelTag(1)).toBe('warning');
    expect(alarmLevelTag(2)).toBe('info');
    expect(alarmLevelTag(3)).toBe('success');
    expect(alarmLevelTag(undefined)).toBe('info');
  });

  it('normalizes legacy millisecond command timeouts to seconds', () => {
    expect(normalizeCommandTimeoutSeconds(null)).toBeUndefined();
    expect(normalizeCommandTimeoutSeconds('')).toBeUndefined();
    expect(normalizeCommandTimeoutSeconds('not-a-number')).toBeUndefined();
    expect(normalizeCommandTimeoutSeconds(0)).toBeUndefined();
    expect(normalizeCommandTimeoutSeconds(30)).toBe(30);
    expect(normalizeCommandTimeoutSeconds('2000')).toBe(2);
    expect(normalizeCommandTimeoutSeconds(1500)).toBe(1500);

    expect(commandTimeoutLabel(undefined)).toBe('-');
    expect(commandTimeoutLabel(1000)).toBe('1');
  });
});
