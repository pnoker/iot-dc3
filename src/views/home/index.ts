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

import { computed, defineComponent, onMounted, reactive } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import type { Component } from 'vue';
import { Bell, List, Management, Promotion, TrendCharts, Warning } from '@element-plus/icons-vue';

import { getDeviceList } from '@/api/device';
import { getPointList } from '@/api/point';
import { getProfileList } from '@/api/profile';
import { getDriverList } from '@/api/driver';
import { alertStats, dailyGrowth, statsTimeseries, statsToday } from '@/api/dashboard';

import StatCard from '@/components/card/stat/StatCard.vue';
import LiveDataFeed from './components/LiveDataFeed.vue';
import AnalyticsTabs from './components/AnalyticsTabs.vue';
import SlaBadge from './components/SlaBadge.vue';
import TopologySankey from './components/TopologySankey.vue';
import TrendChart from './components/TrendChart.vue';
import HomeBanner from './components/HomeBanner.vue';
import AlertList from './components/AlertList.vue';
import LatencyChart from './components/LatencyChart.vue';
import ActivityHeatmap from './components/ActivityHeatmap.vue';

type Tone = 'blue' | 'green' | 'orange' | 'purple' | 'red';

interface CardModel {
  key: string;
  title: string;
  value: number | string;
  subtitle: string;
  icon: Component;
  tone: Tone;
  trend: { direction: 'up' | 'down' | 'flat'; label: string } | null;
  sparkline: number[];
  onClick: () => void;
  onRefresh: () => Promise<void> | void;
}

export default defineComponent({
  name: 'Home',
  components: {
    StatCard,
    LiveDataFeed,
    AnalyticsTabs,
    SlaBadge,
    TopologySankey,
    TrendChart,
    HomeBanner,
    AlertList,
    LatencyChart,
    ActivityHeatmap,
  },
  setup() {
    const { t } = useI18n();
    const router = useRouter();

    const state = reactive({
      driverCount: 0,
      deviceCount: 0,
      pointCount: 0,
      profileCount: 0,
      todayCount: 0,
      todayPercentChange: 0,
      todaySparkline: [] as number[],
      totalCount: 0,
      alertCount: 0,
      alertUnconfirmed: 0,
      deviceAlertCount: 0,
      driverAlertCount: 0,
      deviceUnconfirmed: 0,
      driverUnconfirmed: 0,
      todayDeviceAlarms: 0,
      todayDriverAlarms: 0,
      todayDeviceUnconfirmed: 0,
      todayDriverUnconfirmed: 0,
      driverSparkline: [] as number[],
      deviceSparkline: [] as number[],
      pointSparkline: [] as number[],
      alertSparkline: [] as number[],
    });

    // HomeBanner polls /dashboard/system/health on its own; index.ts no longer
    // derives service status from stat-request success.
    const emptyPage = { current: 1, size: 1 };

    const loadTotals = () =>
      Promise.all([
        getDriverList({ page: emptyPage })
          .then((r: any) => (state.driverCount = r?.data?.total ?? 0))
          .catch(() => {}),
        getDeviceList({ page: emptyPage })
          .then((r: any) => (state.deviceCount = r?.data?.total ?? 0))
          .catch(() => {}),
        getPointList({ page: emptyPage })
          .then((r: any) => (state.pointCount = r?.data?.total ?? 0))
          .catch(() => {}),
        getProfileList({ page: emptyPage })
          .then((r: any) => (state.profileCount = r?.data?.total ?? 0))
          .catch(() => {}),
      ]);

    const loadToday = async () => {
      try {
        const res: any = await statsToday();
        state.todayCount = res?.data?.today ?? 0;
        state.todayPercentChange = res?.data?.percentChange ?? 0;
        state.totalCount = res?.data?.total ?? 0;
      } catch {
        // handled globally
      }
    };

    const loadSparkline = async () => {
      try {
        const res: any = await statsTimeseries({ granularity: 'hour', rangeKey: '24h' });
        const buckets: Array<{ count: number }> = res?.data ?? [];
        state.todaySparkline = buckets.map((b) => Number(b.count) || 0);
      } catch {
        // handled globally
      }
    };

    const loadAlerts = async () => {
      try {
        const res: any = await alertStats();
        state.alertCount = res?.data?.total ?? 0;
        state.alertUnconfirmed = res?.data?.unconfirmed ?? 0;
        state.deviceAlertCount = res?.data?.deviceAlerts ?? 0;
        state.driverAlertCount = res?.data?.driverAlerts ?? 0;
        state.deviceUnconfirmed = res?.data?.deviceUnconfirmed ?? 0;
        state.driverUnconfirmed = res?.data?.driverUnconfirmed ?? 0;
        state.todayDeviceAlarms = res?.data?.todayDeviceAlarms ?? 0;
        state.todayDriverAlarms = res?.data?.todayDriverAlarms ?? 0;
        state.todayDeviceUnconfirmed = res?.data?.todayDeviceUnconfirmed ?? 0;
        state.todayDriverUnconfirmed = res?.data?.todayDriverUnconfirmed ?? 0;
        state.alertSparkline = Array.isArray(res?.data?.sparkline24h) ? res.data.sparkline24h.map(Number) : [];
      } catch {
        // handled globally
      }
    };

    const loadGrowth = async () => {
      try {
        const res: any = await dailyGrowth(7);
        state.driverSparkline = (res?.data?.driver ?? []).map(Number);
        state.deviceSparkline = (res?.data?.device ?? []).map(Number);
        state.pointSparkline = (res?.data?.point ?? []).map(Number);
      } catch {
        // handled globally
      }
    };

    onMounted(() => {
      loadTotals();
      loadToday();
      loadSparkline();
      loadAlerts();
      loadGrowth();
    });

    const percentTrend = computed(() => {
      const p = state.todayPercentChange;
      if (p > 0) return { direction: 'up' as const, label: `${p}% ${t('home.vsYesterday')}` };
      if (p < 0) return { direction: 'down' as const, label: `${p}% ${t('home.vsYesterday')}` };
      return { direction: 'flat' as const, label: `0% ${t('home.vsYesterday')}` };
    });

    // Each stat card gets its own refresh: counters + its sparkline are
    // re-fetched so the user doesn't have to pick between "latest count" and
    // "latest trend". loadTotals / loadGrowth both cover all four entities,
    // so we reuse them for driver/device/point and combine for data/alert.
    const refreshDriver = async () => {
      await Promise.all([loadTotals(), loadGrowth()]);
    };
    const refreshDevice = refreshDriver;
    const refreshPoint = refreshDriver;
    const refreshData = async () => {
      await Promise.all([loadToday(), loadSparkline()]);
    };
    const refreshAlert = loadAlerts;

    const sparkTrend = (spark: number[]): { direction: 'up' | 'down' | 'flat'; label: string } | null => {
      if (spark.length < 2) return null;
      const prev = spark[spark.length - 2] ?? 0;
      const curr = spark[spark.length - 1] ?? 0;
      const diff = curr - prev;
      if (diff > 0) return { direction: 'up', label: `+${diff}` };
      if (diff < 0) return { direction: 'down', label: `${diff}` };
      return { direction: 'flat', label: '0' };
    };

    // Every card gets a subtitle tied to its primary metric so the number
    // never stands alone — matches the event-overview layout rhythm:
    //   entity cards → how many alarms this fleet has accumulated
    //   point card → how many profiles the points are organised under
    //   data card → cumulative total (already defined via todayTotal)
    //   alarm cards → today's unconfirmed count (always shown, even 0, so
    //                 card heights stay equal across the grid)
    const cards = computed<CardModel[]>(() => [
      {
        key: 'driver',
        title: t('home.driverCount'),
        value: state.driverCount,
        subtitle: t('home.entityAlarms', { n: state.driverAlertCount }),
        icon: Promotion,
        tone: 'blue',
        trend: sparkTrend(state.driverSparkline),
        sparkline: state.driverSparkline,
        onClick: () => router.push({ name: 'driver' }),
        onRefresh: refreshDriver,
      },
      {
        key: 'device',
        title: t('home.deviceCount'),
        value: state.deviceCount,
        subtitle: t('home.entityAlarms', { n: state.deviceAlertCount }),
        icon: Management,
        tone: 'purple',
        trend: sparkTrend(state.deviceSparkline),
        sparkline: state.deviceSparkline,
        onClick: () => router.push({ name: 'device' }),
        onRefresh: refreshDevice,
      },
      {
        key: 'point',
        title: t('home.pointCount'),
        value: state.pointCount,
        subtitle: t('home.pointsAcrossProfiles', { n: state.profileCount }),
        icon: List,
        tone: 'orange',
        trend: sparkTrend(state.pointSparkline),
        sparkline: state.pointSparkline,
        onClick: () => router.push({ name: 'profile' }),
        onRefresh: refreshPoint,
      },
      {
        key: 'data',
        title: t('home.todayData'),
        value: state.todayCount,
        subtitle: state.totalCount > 0 ? t('home.todayTotal', { n: state.totalCount }) : '',
        icon: TrendCharts,
        tone: 'green',
        trend: percentTrend.value,
        sparkline: state.todaySparkline,
        onClick: () => router.push({ name: 'pointValue' }),
        onRefresh: refreshData,
      },
      {
        key: 'alert',
        title: t('home.driverAlarms'),
        value: state.todayDriverAlarms,
        subtitle: t('home.alertUnconfirmed', { n: state.todayDriverUnconfirmed }),
        icon: Bell,
        tone: 'red',
        trend: sparkTrend(state.alertSparkline),
        sparkline: state.alertSparkline,
        onClick: () => router.push({ name: 'settingsDriverEvent' }),
        onRefresh: refreshAlert,
      },
      {
        key: 'deviceAlert',
        title: t('home.deviceAlarms'),
        value: state.todayDeviceAlarms,
        subtitle: t('home.alertUnconfirmed', { n: state.todayDeviceUnconfirmed }),
        icon: Warning,
        tone: 'orange',
        trend: sparkTrend(state.alertSparkline),
        sparkline: state.alertSparkline,
        onClick: () => router.push({ name: 'settingsDeviceEvent' }),
        onRefresh: refreshAlert,
      },
    ]);

    return { cards };
  },
});
