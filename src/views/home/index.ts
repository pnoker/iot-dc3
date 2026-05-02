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
import { Bell, List, Management, Promotion, TrendCharts } from '@element-plus/icons-vue';

import { getDeviceList } from '@/api/device';
import { getPointList } from '@/api/point';
import { getProfileList } from '@/api/profile';
import { getDriverList } from '@/api/driver';
import { alertStats, statsTimeseries, statsToday } from '@/api/dashboard';

import StatCard from './components/StatCard.vue';
import LiveDataFeed from './components/LiveDataFeed.vue';
import AnalyticsTabs from './components/AnalyticsTabs.vue';
import TrendChart from './components/TrendChart.vue';
import HomeBanner from './components/HomeBanner.vue';
import QuickActions from './components/QuickActions.vue';
import AlertList from './components/AlertList.vue';

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
}

export default defineComponent({
  name: 'Home',
  components: { StatCard, LiveDataFeed, AnalyticsTabs, TrendChart, HomeBanner, QuickActions, AlertList },
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
      alertCount: 0,
      alertUnconfirmed: 0,
    });

    // Each stat endpoint doubles as a liveness probe — if the request succeeds
    // the center service is responding. No dedicated /health endpoint needed.
    const serviceStatus = reactive({ auth: true, data: true, manager: true });

    const emptyPage = { current: 1, size: 1 };

    const loadTotals = () => {
      getDriverList({ page: emptyPage })
        .then((r: any) => {
          state.driverCount = r?.data?.total ?? 0;
          serviceStatus.manager = true;
        })
        .catch(() => {
          serviceStatus.manager = false;
        });
      getDeviceList({ page: emptyPage })
        .then((r: any) => (state.deviceCount = r?.data?.total ?? 0))
        .catch(() => {});
      getPointList({ page: emptyPage })
        .then((r: any) => (state.pointCount = r?.data?.total ?? 0))
        .catch(() => {});
      getProfileList({ page: emptyPage })
        .then((r: any) => (state.profileCount = r?.data?.total ?? 0))
        .catch(() => {});
    };

    const loadToday = async () => {
      try {
        const res: any = await statsToday();
        state.todayCount = res?.data?.today ?? 0;
        state.todayPercentChange = res?.data?.percentChange ?? 0;
        serviceStatus.data = true;
      } catch {
        serviceStatus.data = false;
      }
    };

    const loadSparkline = async () => {
      try {
        const res: any = await statsTimeseries({ granularity: 'hour', rangeHours: 24 });
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
      } catch {
        // handled globally
      }
    };

    onMounted(() => {
      loadTotals();
      loadToday();
      loadSparkline();
      loadAlerts();
    });

    const percentTrend = computed(() => {
      const p = state.todayPercentChange;
      if (p > 0) return { direction: 'up' as const, label: `${p}% ${t('home.vsYesterday')}` };
      if (p < 0) return { direction: 'down' as const, label: `${p}% ${t('home.vsYesterday')}` };
      return { direction: 'flat' as const, label: `0% ${t('home.vsYesterday')}` };
    });

    const cards = computed<CardModel[]>(() => [
      {
        key: 'driver',
        title: t('home.driverCount'),
        value: state.driverCount,
        subtitle: '',
        icon: Promotion,
        tone: 'blue',
        trend: null,
        sparkline: [],
        onClick: () => router.push({ name: 'driver' }),
      },
      {
        key: 'device',
        title: t('home.deviceCount'),
        value: state.deviceCount,
        subtitle: '',
        icon: Management,
        tone: 'purple',
        trend: null,
        sparkline: [],
        onClick: () => router.push({ name: 'device' }),
      },
      {
        key: 'point',
        title: t('home.pointCount'),
        value: state.pointCount,
        subtitle: '',
        icon: List,
        tone: 'orange',
        trend: null,
        sparkline: [],
        onClick: () => router.push({ name: 'profile' }),
      },
      {
        key: 'data',
        title: t('home.todayData'),
        value: state.todayCount,
        subtitle: '',
        icon: TrendCharts,
        tone: 'green',
        trend: percentTrend.value,
        sparkline: state.todaySparkline,
        onClick: () => router.push({ name: 'pointValue' }),
      },
      {
        key: 'alert',
        title: t('home.alerts'),
        value: state.alertCount,
        subtitle: state.alertUnconfirmed > 0 ? t('home.alertUnconfirmed', { n: state.alertUnconfirmed }) : '',
        icon: Bell,
        tone: 'red',
        trend: null,
        sparkline: [],
        onClick: () => {
          // Alerts are shown inline in the AlertList panel below; nothing to navigate yet.
        },
      },
    ]);

    return { cards, serviceStatus };
  },
});
