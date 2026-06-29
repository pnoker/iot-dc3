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

import type {Router} from 'vue-router';

/**
 * Central "jump to entity detail" utility. Every list-shaped dashboard
 * card used to repeat `router.push({ name: 'driverDetail', query: { id,
 * active: 'detail' }})` inline — each card then also had near-identical
 * handlers for device / profile / point cases. Consolidated here so the
 * URL shape + query convention stays consistent project-wide.
 *
 * <p>Point jumps land on {@code pointValue} (the data page) rather than a
 * dedicated detail route — that's the existing project convention and
 * what operators expect when clicking a point id.</p>
 */

/** All entity kinds that dashboard cards can route to. */
export type JumpKind = 'driver' | 'device' | 'profile' | 'point';

/** Event-source flavour — drives jumps to settingsDeviceAlarm / settingsDriverAlarm. */
export type AlertSourceKind = 'point' | 'device' | 'driver';

/**
 * Jump to the detail / value page for the given entity. No-op on invalid
 * kind. Router errors (e.g. aborted nav during transitions) are swallowed
 * so callers don't have to .catch() themselves.
 */
export const jumpToEntity = (router: Router, kind: JumpKind, id: string | number): void => {
  const idStr = String(id);
  if (!idStr) return;
  switch (kind) {
    case 'driver':
      router.push({name: 'driverDetail', query: {id: idStr, active: 'detail'}}).catch(() => {});
      break;
    case 'device':
      router.push({name: 'deviceDetail', query: {id: idStr, active: 'detail'}}).catch(() => {});
      break;
    case 'profile':
      router.push({name: 'profileDetail', query: {id: idStr, active: 'detail'}}).catch(() => {});
      break;
    case 'point':
      router.push({name: 'pointValue', query: {pointId: idStr}}).catch(() => {});
      break;
  }
};

/**
 * Jump to the per-source event list with the source id pre-filtered.
 */
export const jumpToSourceEvents = (router: Router, source: AlertSourceKind, sourceId: string | number): void => {
  let name: string;
  if (source === 'point') name = 'settingsPointAlarm';
  else if (source === 'driver') name = 'settingsDriverAlarm';
  else name = 'settingsDeviceAlarm';
  router.push({name, query: {sourceId: String(sourceId)}}).catch(() => {});
};
