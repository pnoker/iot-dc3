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
      router.push({name: 'driverDetail', query: {id: idStr, active: 'detail'}}).catch(() => {
      });
      break;
    case 'device':
      router.push({name: 'deviceDetail', query: {id: idStr, active: 'detail'}}).catch(() => {
      });
      break;
    case 'profile':
      router.push({name: 'profileDetail', query: {id: idStr, active: 'detail'}}).catch(() => {
      });
      break;
    case 'point':
      router.push({name: 'pointValue', query: {pointId: idStr}}).catch(() => {
      });
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
  router.push({name, query: {sourceId: String(sourceId)}}).catch(() => {
  });
};
