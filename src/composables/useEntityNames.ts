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

import { reactive } from 'vue';

import { listDeviceByIds } from '@/api/device';
import { listDriverByIds } from '@/api/driver';
import { getPointByIds } from '@/api/point';
import { listProfileByIds } from '@/api/profile';

/**
 * Cross-component reactive cache for entity id → display name lookups.
 * Replaces the seven copies of `resolveNames` + local `nameMap` that every
 * dashboard card used to maintain. Two concrete wins:
 *
 *   1. **Cache is process-wide.** Opening FlappingSources after SilentSources
 *      already resolved a device id doesn't re-issue the batch request.
 *   2. **Inflight dedup.** Two cards mounting simultaneously won't fire two
 *      identical listDeviceByIds calls for the same missing ids — the second
 *      one sees ids marked inflight and waits for the first to land.
 *
 * Errors are swallowed silently (the axios response interceptor already
 * surfaces toast / login-redirect for 4xx/5xx); the UI falls back to the
 * id string rather than showing a spinner forever.
 */

type BatchResponse = { data?: Record<string, any> };

type EntityKind = 'device' | 'driver' | 'profile' | 'point';

// Module-level reactive caches — shared across every component that
// imports useEntityNames(). `reactive()` at module scope is valid Vue 3
// usage and cheaper than Pinia for this small surface.
const cache: Record<EntityKind, Record<string, string>> = {
  device: reactive<Record<string, string>>({}),
  driver: reactive<Record<string, string>>({}),
  profile: reactive<Record<string, string>>({}),
  point: reactive<Record<string, string>>({}),
};

const inflight: Record<EntityKind, Set<string>> = {
  device: new Set(),
  driver: new Set(),
  profile: new Set(),
  point: new Set(),
};

const fetchers: Record<EntityKind, (ids: string[]) => Promise<BatchResponse>> = {
  device: (ids) => listDeviceByIds(ids) as Promise<BatchResponse>,
  driver: (ids) => listDriverByIds(ids) as Promise<BatchResponse>,
  profile: (ids) => listProfileByIds(ids) as Promise<BatchResponse>,
  point: (ids) => getPointByIds(ids) as Promise<BatchResponse>,
};

// Entity payloads returned by the getXxxByIds endpoints all use a
// "<kind>Name" field — centralised here so adding a new kind is one line.
const nameField: Record<EntityKind, string> = {
  device: 'deviceName',
  driver: 'driverName',
  profile: 'profileName',
  point: 'pointName',
};

async function fetchMissing(kind: EntityKind, rawIds: Array<string | number>): Promise<void> {
  if (!rawIds || rawIds.length === 0) return;
  const missing: string[] = [];
  for (const raw of rawIds) {
    const id = String(raw);
    if (!id) continue;
    if (cache[kind][id] !== undefined) continue;
    if (inflight[kind].has(id)) continue;
    missing.push(id);
  }
  if (missing.length === 0) return;
  missing.forEach((id) => inflight[kind].add(id));
  try {
    const res = await fetchers[kind](missing);
    const data = res?.data ?? {};
    for (const id of missing) {
      const row = data[id];
      // Cache whatever the backend returned — even empty → fallback to id.
      // Storing `id` as the value means future lookups hit cache (no re-fetch
      // of deleted entities every time a historical row references them).
      cache[kind][id] = row?.[nameField[kind]] ?? id;
    }
  } catch {
    // errors handled by global axios interceptor
  } finally {
    missing.forEach((id) => inflight[kind].delete(id));
  }
}

export type AlertSourceKind = 'device' | 'driver';

export const useEntityNames = () => {
  const resolveDevices = (ids: Array<string | number>) => fetchMissing('device', ids);
  const resolveDrivers = (ids: Array<string | number>) => fetchMissing('driver', ids);
  const resolveProfiles = (ids: Array<string | number>) => fetchMissing('profile', ids);
  const resolvePoints = (ids: Array<string | number>) => fetchMissing('point', ids);

  const deviceName = (id: string | number | undefined | null): string =>
    id == null ? '' : (cache.device[String(id)] ?? String(id));
  const driverName = (id: string | number | undefined | null): string =>
    id == null ? '' : (cache.driver[String(id)] ?? String(id));
  const profileName = (id: string | number | undefined | null): string =>
    id == null ? '' : (cache.profile[String(id)] ?? String(id));
  const pointName = (id: string | number | undefined | null): string =>
    id == null ? '' : (cache.point[String(id)] ?? String(id));

  /** Resolve a mixed batch of (source, id) rows in one call. */
  const resolveBySource = async (
    rows: Array<{ source: AlertSourceKind; sourceId: string | number }>
  ): Promise<void> => {
    const devIds: Array<string | number> = [];
    const drvIds: Array<string | number> = [];
    for (const r of rows) {
      (r.source === 'device' ? devIds : drvIds).push(r.sourceId);
    }
    await Promise.all([resolveDevices(devIds), resolveDrivers(drvIds)]);
  };

  const nameBySource = (source: AlertSourceKind, id: string | number): string =>
    source === 'device' ? deviceName(id) : driverName(id);

  return {
    // single-kind
    resolveDevices,
    resolveDrivers,
    resolveProfiles,
    resolvePoints,
    deviceName,
    driverName,
    profileName,
    pointName,
    // source-aware
    resolveBySource,
    nameBySource,
  };
};
