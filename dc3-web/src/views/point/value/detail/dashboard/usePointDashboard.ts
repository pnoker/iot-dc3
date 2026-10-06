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
 * Data-loading composable for the point dashboard. One round trip
 * per concern, each with its own loader so a failing section (e.g. alerts)
 * degrades alone instead of blanking the whole board:
 *   - dashboard  — GET /point_value/dashboard (window-scoped, range-hours driven)
 *   - alertProfile — GET /dashboard/alert/point_profile (30-day profile, point-scoped)
 *   - recentAlerts — POST /dashboard/alert/page (source=point, latest 5)
 */

import {ref} from 'vue';

import {listAlertPage, getPointAlertProfile} from '@/api/dashboard/alert';
import {getPointValueDashboard} from '@/api/point';
import type {AlertEventRow, PointAlertProfile, PointValueDashboard} from '@/config/types/dashboard';
import {useAsyncLoader} from '@/utils/asyncLoaderUtil';

/**
 * The composable handle: three independent loader states plus their payloads.
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

  /**
   * Load the window-scoped dashboard payload. When `sections` is provided
   * (a manual single-card refresh), only those keys of the payload are
   * applied — keys consumed by other cards keep their old object references
   * so their chart watchers don't fire and the charts don't re-render.
   * @param deviceId - device id whose point is being queried
   * @param pointId - point id whose dashboard is being queried
   * @param rangeHours - lookback window in hours from 1 through 168
   * @param sections - payload keys to apply; null/undefined applies everything
   * @returns the resolved dashboard payload
   */
  const loadDashboard = (
    deviceId: string,
    pointId: string,
    rangeHours: number,
    sections: readonly string[] | null = null
  ) =>
    dashboard.run(() => getPointValueDashboard(deviceId, pointId, rangeHours), {
      apply: (res) => {
        if (sections && dashboardData.value && res) {
          const toUpdate = new Set(sections);
          const merged = {...res} as Record<string, unknown>;
          const old = dashboardData.value as unknown as Record<string, unknown>;
          for (const key of Object.keys(old)) {
            if (!toUpdate.has(key)) merged[key] = old[key];
          }
          dashboardData.value = merged as unknown as PointValueDashboard;
        } else {
          dashboardData.value = res ?? null;
        }
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
        listAlertPage({
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
    loadDashboard,
    loadAlertProfile,
    loadRecentAlerts,
  };
};
