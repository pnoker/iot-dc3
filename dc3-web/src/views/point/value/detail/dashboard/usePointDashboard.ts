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

/**
 * Data-loading composable for the point dashboard (位号看板). One round trip
 * per concern, each with its own loader so a failing section (e.g. alerts)
 * degrades alone instead of blanking the whole board:
 *   - dashboard  — GET /point_value/dashboard (window-scoped, range-hours driven)
 *   - alertProfile — GET /dashboard/alert/point_profile (30-day profile, point-scoped)
 *   - recentAlerts — POST /dashboard/alert/page (source=point, latest 5)
 *   - peers      — listPointByDeviceId + getPointValueLatest (device peers)
 */

import {ref} from 'vue';

import {alertPage, getPointAlertProfile} from '@/api/dashboard/alert';
import {getPointValueLatest, getPointValueDashboard, listPointByDeviceId} from '@/api/point';
import type {AlertEventRow, PointAlertProfile, PointValueDashboard} from '@/config/types/dashboard';
import type {PointRecord} from '@/config/types/manager';
import {useAsyncLoader} from '@/utils/asyncLoaderUtil';

/**
 * One neighbour point card row: the point metadata plus its latest value,
 * joined on pointId.
 */
export interface PeerSnapshotItem {
  pointId: string;
  pointName: string;
  unit: string;
  value: string | null;
  createTime: string | null;
}

/**
 * The composable handle: four independent loader states plus their payloads.
 */
export interface PointDashboardState {
  dashboard: {
    loading: ReturnType<typeof useAsyncLoader>['loading'];
    error: ReturnType<typeof useAsyncLoader>['error'];
    status: ReturnType<typeof useAsyncLoader>['status'];
    data: Readonly<ReturnType<typeof ref<PointValueDashboard | null>>>;
  };
  alertProfile: {
    loading: ReturnType<typeof useAsyncLoader>['loading'];
    error: ReturnType<typeof useAsyncLoader>['error'];
    status: ReturnType<typeof useAsyncLoader>['status'];
    data: Readonly<ReturnType<typeof ref<PointAlertProfile | null>>>;
  };
  recentAlerts: {
    loading: ReturnType<typeof useAsyncLoader>['loading'];
    error: ReturnType<typeof useAsyncLoader>['error'];
    status: ReturnType<typeof useAsyncLoader>['status'];
    rows: Readonly<ReturnType<typeof ref<AlertEventRow[]>>>;
  };
  peers: {
    loading: ReturnType<typeof useAsyncLoader>['loading'];
    error: ReturnType<typeof useAsyncLoader>['error'];
    status: ReturnType<typeof useAsyncLoader>['status'];
    items: Readonly<ReturnType<typeof ref<PeerSnapshotItem[]>>>;
  };
}

/**
 * Point dashboard data orchestration. Loaders are exposed separately so the
 * host can re-run the window-scoped dashboard on range change while the
 * 30-day alert profile and peer snapshot only reload on id change.
 * @returns the composable handle
 */
export const usePointDashboard = () => {
  const dashboard = useAsyncLoader();
  const dashboardData = ref<PointValueDashboard | null>(null);
  const alertProfile = useAsyncLoader();
  const alertProfileData = ref<PointAlertProfile | null>(null);
  const recentAlerts = useAsyncLoader();
  const recentAlertRows = ref<AlertEventRow[]>([]);
  const peers = useAsyncLoader();
  const peerItems = ref<PeerSnapshotItem[]>([]);

  /**
   * Load the window-scoped dashboard payload.
   * @param deviceId - device id whose point is being queried
   * @param pointId - point id whose dashboard is being queried
   * @param rangeHours - lookback window in hours from 1 through 168
   * @returns the resolved dashboard payload
   */
  const loadDashboard = (deviceId: string, pointId: string, rangeHours: number) =>
    dashboard.run(() => getPointValueDashboard(deviceId, pointId, rangeHours), {
      apply: (res) => {
        dashboardData.value = res ?? null;
      },
    });

  /**
   * Load the 30-day alarm profile of the point.
   * @param pointId - point id whose alarm profile is being queried
   * @returns the resolved alarm profile payload
   */
  const loadAlertProfile = (pointId: string) =>
    alertProfile.run(() => getPointAlertProfile(pointId), {
      apply: (res) => {
        alertProfileData.value = res ?? null;
      },
    });

  /**
   * Load the latest 5 point-scoped alerts.
   * @param pointId - point id whose recent alerts are being queried
   * @returns the resolved alert page
   */
  const loadRecentAlerts = (pointId: string) =>
    recentAlerts.run(
      () =>
        alertPage({
          source: 'point',
          sourceId: pointId,
          rangeKey: '30d',
          offset: 0,
          limit: 5,
        }),
      {
        apply: (res) => {
          recentAlertRows.value = (Array.isArray(res?.items) ? res.items : []) as AlertEventRow[];
        },
      }
    );

  /**
   * Load the neighbour snapshot: every other point of the device joined
   * with its latest value on pointId.
   * @param deviceId - device id whose points are listed
   * @param currentPointId - point id to exclude from the snapshot
   * @returns the resolved peer snapshot items
   */
  const loadPeers = (deviceId: string, currentPointId: string) =>
    peers.run(
      async () => {
        if (!deviceId) return [] as PeerSnapshotItem[];
        const [points, latest] = await Promise.all([
          listPointByDeviceId(deviceId).catch(() => [] as PointRecord[]),
          getPointValueLatest({deviceId, offset: 0, limit: 50}).catch(() => null),
        ]);
        const latestById = new Map<string, Record<string, unknown>>();
        for (const row of Array.isArray(latest?.items) ? latest.items : []) {
          const pid = String(row.pointId ?? '');
          if (pid) latestById.set(pid, row);
        }
        return (Array.isArray(points) ? points : [])
          .filter((point) => String(point.id ?? '') !== currentPointId)
          .map((point) => {
            const pid = String(point.id ?? '');
            const row = latestById.get(pid);
            return {
              pointId: pid,
              pointName: String(point.pointName ?? pid),
              unit: String(point.unit ?? '').trim(),
              value: row != null && row.calValue !== undefined && row.calValue !== null ? String(row.calValue) : null,
              createTime: row?.createTime != null ? String(row.createTime) : null,
            };
          });
      },
      {
        apply: (items) => {
          peerItems.value = items ?? [];
        },
      }
    );

  return {
    dashboard: {
      loading: dashboard.loading,
      error: dashboard.error,
      status: dashboard.status,
      data: dashboardData,
    },
    alertProfile: {
      loading: alertProfile.loading,
      error: alertProfile.error,
      status: alertProfile.status,
      data: alertProfileData,
    },
    recentAlerts: {
      loading: recentAlerts.loading,
      error: recentAlerts.error,
      status: recentAlerts.status,
      rows: recentAlertRows,
    },
    peers: {
      loading: peers.loading,
      error: peers.error,
      status: peers.status,
      items: peerItems,
    },
    loadDashboard,
    loadAlertProfile,
    loadRecentAlerts,
    loadPeers,
  };
};
