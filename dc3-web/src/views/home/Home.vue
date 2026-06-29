<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -      https://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <div class="home">
    <!-- Row 0: greeting + clock + service status -->
    <el-row :gutter="8" class="home__row">
      <el-col :span="24" class="home__col">
        <home-banner />
      </el-col>
    </el-row>

    <!-- SLA strip: hides itself when nothing is wrong. Only shows when
         there are alarms unconfirmed > 24h OR devices gone silent. Each
         chip deep-links into the event-overview tab that owns the signal. -->
    <sla-badge />

    <!-- Row 1: 6 indicator cards, always 6-wide via CSS grid -->
    <div class="home__stats">
      <stat-card
        v-for="c in cards"
        :key="c.key"
        :icon="c.icon"
        :on-refresh="c.onRefresh"
        :sparkline="c.sparkline"
        :subtitle="c.subtitle"
        :title="c.title"
        :tone="c.tone"
        :trend="c.trend"
        :value="c.value"
        @click="c.onClick"
      />
    </div>

    <!-- Row 2: trend chart + the two live widgets packed at 3:1.5:1.5 —
         TrendChart gets half the row for a readable time series, and
         LiveFeed + Alarms each take a quarter so the rows inside stay
         legible (4/24 was cramping timestamps and message text). Narrow
         breakpoints stack everything to full width. Lives right below the
         stat cards so the "what happened just now" trio sits in the
         operator's eye line. -->
    <el-row :gutter="8" class="home__row">
      <el-col :lg="12" :md="24" :sm="24" :xl="12" :xs="24" class="home__col">
        <trend-chart />
      </el-col>
      <el-col :lg="6" :md="24" :sm="24" :xl="6" :xs="24" class="home__col">
        <live-data-feed :size="20" />
      </el-col>
      <el-col :lg="6" :md="24" :sm="24" :xl="6" :xs="24" class="home__col">
        <alert-list :size="10" />
      </el-col>
    </el-row>

    <!-- Row 3: analytics split in half — structural breakdowns left,
         top-N activity right, mirroring the two intent groups operators
         use to explore the fleet. -->
    <el-row :gutter="8" class="home__row">
      <el-col :lg="12" :md="24" :sm="24" :xl="12" :xs="24" class="home__col">
        <analytics-tabs group="structural" />
      </el-col>
      <el-col :lg="12" :md="24" :sm="24" :xl="12" :xs="24" class="home__col">
        <analytics-tabs group="top" />
      </el-col>
    </el-row>

    <!-- Row 4: latency histogram + hourly activity heatmap -->
    <el-row :gutter="8" class="home__row">
      <el-col :lg="12" :md="24" :sm="24" :xl="12" :xs="24" class="home__col">
        <latency-chart />
      </el-col>
      <el-col :lg="12" :md="24" :sm="24" :xl="12" :xs="24" class="home__col">
        <activity-heatmap />
      </el-col>
    </el-row>

    <!-- Row 5 (bottom): tenant-wide topology Sankey (Driver → Device →
         Profile → Point). Full-width because four columns need the
         horizontal budget; height 480 instead of the family 440 so each
         column has enough vertical room for labels. Anchored at the
         bottom as a "zoom-out" summary after the operator has scanned
         the realtime / analytics / latency rows above. -->
    <el-row :gutter="8" class="home__row">
      <el-col :span="24" class="home__col">
        <topology-sankey />
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts" setup>
  import type {Component} from 'vue';
  import {computed, onMounted, reactive} from 'vue';
  import {useI18n} from 'vue-i18n';
  import {useRouter} from 'vue-router';
  import {Bell, List, Management, Promotion, TrendCharts, Warning} from '@element-plus/icons-vue';

  import {listDevice} from '@/api/device';
  import {listPoint} from '@/api/point';
  import {listProfile} from '@/api/profile';
  import {listDriver} from '@/api/driver';
  import {alertStats, dailyGrowth, statsTimeseries, statsToday} from '@/api/dashboard';
  import type {
    AlertStatsSummary,
    DailyGrowthSummary,
    StatsTimeBucket,
    StatsTodaySummary,
  } from '@/config/types/dashboard';

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
    trend: {direction: 'up' | 'down' | 'flat'; label: string} | null;
    sparkline: number[];
    onClick: () => void;
    onRefresh: () => Promise<void> | void;
  }

  interface HomeState {
    driverCount: number;
    deviceCount: number;
    pointCount: number;
    profileCount: number;
    todayCount: number;
    todayPercentChange: number;
    todaySparkline: number[];
    totalCount: number;
    alertCount: number;
    alertUnconfirmed: number;
    deviceAlertCount: number;
    driverAlertCount: number;
    deviceUnconfirmed: number;
    driverUnconfirmed: number;
    todayDeviceAlarms: number;
    todayDriverAlarms: number;
    todayDeviceUnconfirmed: number;
    todayDriverUnconfirmed: number;
    driverSparkline: number[];
    deviceSparkline: number[];
    pointSparkline: number[];
    alertSparkline: number[];
  }

  interface ListPageSummary {
    total?: number;
  }

  type ListPageResponse = R<ListPageSummary>;

  const {t} = useI18n();
  const router = useRouter();

  const state = reactive<HomeState>({
    driverCount: 0,
    deviceCount: 0,
    pointCount: 0,
    profileCount: 0,
    todayCount: 0,
    todayPercentChange: 0,
    todaySparkline: [],
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
    driverSparkline: [],
    deviceSparkline: [],
    pointSparkline: [],
    alertSparkline: [],
  });

  const emptyPage = {current: 1, size: 1};
  const toNumber = (value: number | string | null | undefined) => Number(value) || 0;
  const toNumberArray = (values: Array<number | string | null | undefined>) => values.map((value) => toNumber(value));
  const getSettledTotal = (result: PromiseSettledResult<ListPageResponse>) =>
    result.status === 'fulfilled' ? toNumber(result.value.data.total) : 0;

  const loadTotals = async () => {
    const [driverRes, deviceRes, pointRes, profileRes] = await Promise.allSettled([
      listDriver<ListPageResponse>({page: emptyPage}),
      listDevice<ListPageResponse>({page: emptyPage}),
      listPoint<ListPageResponse>({page: emptyPage}),
      listProfile<ListPageResponse>({page: emptyPage}),
    ]);

    state.driverCount = getSettledTotal(driverRes);
    state.deviceCount = getSettledTotal(deviceRes);
    state.pointCount = getSettledTotal(pointRes);
    state.profileCount = getSettledTotal(profileRes);
  };

  const loadToday = async () => {
    try {
      const res = await statsToday();
      const data: StatsTodaySummary = res.data;
      state.todayCount = toNumber(data.today);
      state.todayPercentChange = toNumber(data.percentChange);
      state.totalCount = toNumber(data.total);
    } catch {
      // handled globally
    }
  };

  const loadSparkline = async () => {
    try {
      const res = await statsTimeseries({granularity: 'hour', rangeKey: '24h'});
      const buckets: StatsTimeBucket[] = res.data;
      state.todaySparkline = buckets.map((bucket) => toNumber(bucket.count));
    } catch {
      // handled globally
    }
  };

  const loadAlerts = async () => {
    try {
      const res = await alertStats();
      const data: AlertStatsSummary = res.data;
      state.alertCount = toNumber(data.total);
      state.alertUnconfirmed = toNumber(data.unconfirmed);
      state.deviceAlertCount = toNumber(data.deviceAlerts);
      state.driverAlertCount = toNumber(data.driverAlerts);
      state.deviceUnconfirmed = toNumber(data.deviceUnconfirmed);
      state.driverUnconfirmed = toNumber(data.driverUnconfirmed);
      state.todayDeviceAlarms = toNumber(data.todayDeviceAlarms);
      state.todayDriverAlarms = toNumber(data.todayDriverAlarms);
      state.todayDeviceUnconfirmed = toNumber(data.todayDeviceUnconfirmed);
      state.todayDriverUnconfirmed = toNumber(data.todayDriverUnconfirmed);
      state.alertSparkline = toNumberArray(data.sparkline24h ?? []);
    } catch {
      // handled globally
    }
  };

  const loadGrowth = async () => {
    try {
      const res = await dailyGrowth(7);
      const data: DailyGrowthSummary = res.data;
      state.driverSparkline = toNumberArray(data.driverDailyCounts ?? []);
      state.deviceSparkline = toNumberArray(data.deviceDailyCounts ?? []);
      state.pointSparkline = toNumberArray(data.pointDailyCounts ?? []);
    } catch {
      // handled globally
    }
  };

  onMounted(() => {
    void loadTotals();
    void loadToday();
    void loadSparkline();
    void loadAlerts();
    void loadGrowth();
  });

  const percentTrend = computed(() => {
    const percent = state.todayPercentChange;
    if (percent > 0) {
      return {direction: 'up' as const, label: `${percent}% ${t('home.vsYesterday')}`};
    }
    if (percent < 0) {
      return {direction: 'down' as const, label: `${percent}% ${t('home.vsYesterday')}`};
    }
    return {direction: 'flat' as const, label: `0% ${t('home.vsYesterday')}`};
  });

  const refreshDriver = async () => {
    await Promise.all([loadTotals(), loadGrowth()]);
  };
  const refreshDevice = refreshDriver;
  const refreshPoint = refreshDriver;
  const refreshData = async () => {
    await Promise.all([loadToday(), loadSparkline()]);
  };
  const refreshAlert = loadAlerts;

  const sparkTrend = (spark: number[]): {direction: 'up' | 'down' | 'flat'; label: string} | null => {
    if (spark.length < 2) {
      return null;
    }

    const previous = spark[spark.length - 2] ?? 0;
    const current = spark[spark.length - 1] ?? 0;
    const diff = current - previous;
    if (diff > 0) {
      return {direction: 'up', label: `+${diff}`};
    }
    if (diff < 0) {
      return {direction: 'down', label: `${diff}`};
    }
    return {direction: 'flat', label: '0'};
  };

  const cards = computed<CardModel[]>(() => [
    {
      key: 'driver',
      title: t('home.driverCount'),
      value: state.driverCount,
      subtitle: t('home.entityAlarms', {n: state.driverAlertCount}),
      icon: Promotion,
      tone: 'blue',
      trend: sparkTrend(state.driverSparkline),
      sparkline: state.driverSparkline,
      onClick: () => router.push({name: 'driver'}),
      onRefresh: refreshDriver,
    },
    {
      key: 'device',
      title: t('home.deviceCount'),
      value: state.deviceCount,
      subtitle: t('home.entityAlarms', {n: state.deviceAlertCount}),
      icon: Management,
      tone: 'purple',
      trend: sparkTrend(state.deviceSparkline),
      sparkline: state.deviceSparkline,
      onClick: () => router.push({name: 'device'}),
      onRefresh: refreshDevice,
    },
    {
      key: 'point',
      title: t('home.pointCount'),
      value: state.pointCount,
      subtitle: t('home.pointsAcrossProfiles', {n: state.profileCount}),
      icon: List,
      tone: 'orange',
      trend: sparkTrend(state.pointSparkline),
      sparkline: state.pointSparkline,
      onClick: () => router.push({name: 'profile'}),
      onRefresh: refreshPoint,
    },
    {
      key: 'data',
      title: t('home.todayData'),
      value: state.todayCount,
      subtitle: state.totalCount > 0 ? t('home.todayTotal', {n: state.totalCount}) : '',
      icon: TrendCharts,
      tone: 'green',
      trend: percentTrend.value,
      sparkline: state.todaySparkline,
      onClick: () => router.push({name: 'pointValue'}),
      onRefresh: refreshData,
    },
    {
      key: 'alert',
      title: t('home.driverAlarms'),
      value: state.todayDriverAlarms,
      subtitle: t('home.alertUnconfirmed', {n: state.todayDriverUnconfirmed}),
      icon: Bell,
      tone: 'red',
      trend: sparkTrend(state.alertSparkline),
      sparkline: state.alertSparkline,
      onClick: () => router.push({name: 'settingsDriverAlarm'}),
      onRefresh: refreshAlert,
    },
    {
      key: 'deviceAlert',
      title: t('home.deviceAlarms'),
      value: state.todayDeviceAlarms,
      subtitle: t('home.alertUnconfirmed', {n: state.todayDeviceUnconfirmed}),
      icon: Warning,
      tone: 'orange',
      trend: sparkTrend(state.alertSparkline),
      sparkline: state.alertSparkline,
      onClick: () => router.push({name: 'settingsDeviceAlarm'}),
      onRefresh: refreshAlert,
    },
  ]);
</script>

<style lang="scss" scoped>
  .home {
    padding: 0 4px;

    .home__row {
      margin-bottom: 8px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    // el-row already carries the 8px vertical rhythm between rows. On wide
    // screens the cols sit side-by-side so they don't need a bottom margin
    // of their own — adding one stacked with .home__row margin-bottom,
    // blowing the gap out to 16px. Only the narrow-screen breakpoint where
    // cols collapse into a single column actually needs the extra spacer.
    .home__col {
      margin-bottom: 0;

      @media (max-width: 1024px) {
        margin-bottom: 8px;

        &:last-child {
          margin-bottom: 0;
        }
      }
    }

    // SlaBadge is conditionally rendered between the banner row and the
    // stat grid. Neither neighbour sets a top margin, so by default the
    // badge sits flush against the stats. Restore the page's 8px rhythm
    // so it reads as its own strip, not as a banner appendage.
    .sla-badge {
      margin-bottom: 8px;
    }

    // Stat indicators: always fit the strip on one line, regardless of how
    // many cards the cards computed property ends up with. Below 1280px
    // (tablet / mobile) fall back to 2 cols so cards don't squeeze below
    // their minimum usable width.
    .home__stats {
      display: grid;
      grid-template-columns: repeat(6, minmax(0, 1fr));
      gap: 8px;
      margin-bottom: 8px;

      @media (max-width: 1280px) {
        grid-template-columns: repeat(3, minmax(0, 1fr));
      }

      @media (max-width: 640px) {
        grid-template-columns: 1fr;
      }
    }
  }
</style>
