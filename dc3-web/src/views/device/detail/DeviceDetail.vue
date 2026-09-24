<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <https://www.gnu.org/licenses/>.
  -->

<template>
  <div>
    <base-card>
      <el-alert
        v-if="reactiveData.status === 'error'"
        :closable="false"
        :title="$t('common.loadFailed')"
        class="detail-page-alert"
        show-icon
        type="error"
      >
        <el-button :loading="reactiveData.loading" link type="danger" @click="device">
          {{ $t('common.retry') }}
        </el-button>
      </el-alert>
      <el-empty
        v-if="reactiveData.status === 'error' && !reactiveData.data.id"
        :description="$t('common.loadFailed')"
      />
      <el-skeleton v-else-if="reactiveData.status === 'loading' && !reactiveData.data.id" :rows="6" animated />
      <el-tabs
        v-else-if="reactiveData.status !== 'error' && reactiveData.data.id"
        v-model="reactiveData.active"
        v-loading="reactiveData.loading"
        @tab-click="changeActive"
      >
        <el-tab-pane :label="$t('device.detail.dashboard')" name="dashboard">
          <div class="device-dashboard">
            <!-- Device 名片: tone-tile hero (name + attachment + status chip)
                 with the machine code and live meta demoted to a quiet footer. -->
            <div class="device-dashboard__banner">
              <div class="device-dashboard__hero">
                <span class="device-dashboard__tile" aria-hidden="true">
                  <el-icon :size="26"><Management/></el-icon>
                </span>
                <div class="device-dashboard__title">
                  <span class="device-dashboard__name">{{ reactiveData.data.deviceName || '-' }}</span>
                  <span class="device-dashboard__attach">
                    {{ reactiveData.driver.driverName || '-' }} · {{ reactiveData.profile.profileName || '-' }}
                  </span>
                </div>
                <span class="device-dashboard__status-chip" :class="`is-${statusCode}`">
                  <span class="device-dashboard__status-dot"></span>
                  {{ statusLabel }}
                </span>
              </div>
              <div class="device-dashboard__footer">
                <button
                  v-if="reactiveData.data.deviceCode"
                  class="device-dashboard__code"
                  :title="reactiveData.data.deviceCode"
                  type="button"
                  @click="copyCode"
                >
                  {{ reactiveData.data.deviceCode }}
                </button>
                <span class="device-dashboard__live-meta">
                  {{ $t('device.detail.heartbeatTime') }} {{ heartbeatLabel }}
                  <span aria-hidden="true">·</span>
                  {{ $t('device.detail.timeoutConfig') }} {{ timeoutLabel }}
                </span>
              </div>
            </div>

            <!-- Stat cards: point count, data quality, missing points, last update. -->
            <div class="device-dashboard__stats">
              <stat-card
                :icon="ListIcon"
                :loading="coverageLoading"
                :title="$t('device.detail.totalPoints')"
                :value="totalPointsLabel"
                tone="blue"
              />
              <stat-card
                :error="qualityError"
                :icon="OdometerIcon"
                :loading="qualityLoading"
                :on-refresh="loadQuality"
                :subtitle="$t('device.detail.qualitySubtitle')"
                :title="$t('device.detail.dataQuality')"
                :value="qualityRatioLabel"
                tone="green"
              />
              <stat-card
                :error="coverageError"
                :icon="WarningIcon"
                :loading="coverageLoading"
                :on-refresh="loadCoverage"
                :title="$t('device.detail.missingPoints')"
                :value="missingPointCount"
                tone="red"
              />
              <stat-card
                :error="qualityError"
                :icon="ClockIcon"
                :loading="qualityLoading"
                :on-refresh="loadQuality"
                :title="$t('device.detail.latestUpdate')"
                :value="latestUpdateLabel"
                tone="purple"
              />
            </div>

            <!-- Trend + weekly activity heatmap. -->
            <el-row :gutter="8" class="device-dashboard__row">
              <el-col :lg="12" :md="24" :sm="24" :xl="12" :xs="24" class="device-dashboard__col">
                <trend-chart :device-id="reactiveData.id"/>
              </el-col>
              <el-col :lg="12" :md="24" :sm="24" :xl="12" :xs="24" class="device-dashboard__col">
                <activity-heatmap :device-id="reactiveData.id"/>
              </el-col>
            </el-row>

            <!-- Latency histogram + quality ring + availability timeline. -->
            <el-row :gutter="8" class="device-dashboard__row">
              <el-col :lg="12" :md="24" :sm="24" :xl="12" :xs="24" class="device-dashboard__col">
                <latency-chart :device-id="reactiveData.id"/>
              </el-col>
              <el-col :lg="6" :md="24" :sm="24" :xl="6" :xs="24" class="device-dashboard__col">
                <dashboard-card
                  :error="qualityError"
                  :error-text="$t('common.loadFailed')"
                  :footer-meta="$t('device.detail.qualityFooter')"
                  :loading="qualityLoading"
                  :retry-text="$t('common.retry')"
                  :title="$t('device.detail.dataQuality')"
                  body-mode="plain"
                  @refresh="loadQuality"
                >
                  <div class="device-dashboard__ring">
                    <quality-ring :quality="qualityData"/>
                  </div>
                </dashboard-card>
              </el-col>
              <el-col :lg="6" :md="24" :sm="24" :xl="6" :xs="24" class="device-dashboard__col">
                <dashboard-card
                  :error="availabilityError"
                  :error-text="$t('common.loadFailed')"
                  :footer-meta="$t('device.detail.availabilityFooter')"
                  :loading="availabilityLoading"
                  :retry-text="$t('common.retry')"
                  :title="$t('device.detail.availabilityTitle')"
                  body-mode="plain"
                  @refresh="loadAvailability"
                >
                  <availability-timeline
                    :missing-items="coverageData.items"
                    :missing-points="coverageData.missingPoints"
                    :silent-sources="silentData"
                    :total-points="coverageData.totalPoints"
                  />
                </dashboard-card>
              </el-col>
            </el-row>

            <!-- Live data feed + device-scoped alert list. -->
            <el-row :gutter="8" class="device-dashboard__row">
              <el-col :lg="12" :md="24" :sm="24" :xl="12" :xs="24" class="device-dashboard__col">
                <live-data-feed :device-id="reactiveData.id" :size="20"/>
              </el-col>
              <el-col :lg="12" :md="24" :sm="24" :xl="12" :xs="24" class="device-dashboard__col">
                <alert-list :device-id="reactiveData.id" :size="10"/>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>
        <el-tab-pane :label="$t('device.detail.relatedPoints')" lazy name="point">
          <point ref="pointViewRef" :device-id="reactiveData.id" :embedded="'device'"></point>
        </el-tab-pane>
        <el-tab-pane :label="$t('device.detail.relatedCommands')" lazy name="command">
          <command-list
            v-if="profileId"
            ref="commandViewRef"
            :embedded="'device'"
            :profile-id="profileId"
          ></command-list>
          <el-empty v-else :description="$t('common.description')"/>
        </el-tab-pane>
        <el-tab-pane :label="$t('device.detail.relatedEvents')" lazy name="event">
          <event-list v-if="profileId" ref="eventViewRef" :embedded="'device'" :profile-id="profileId"></event-list>
          <el-empty v-else :description="$t('common.description')"/>
        </el-tab-pane>
        <el-tab-pane :label="$t('device.detail.deviceData')" lazy name="pointValue">
          <point-value ref="pointValueViewRef" :device-id="reactiveData.id" :embedded="'device'"></point-value>
        </el-tab-pane>
      </el-tabs>
    </base-card>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, onMounted, reactive, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {Clock as ClockIcon, List as ListIcon, Management, Odometer as OdometerIcon, Warning as WarningIcon} from '@element-plus/icons-vue';

import {useRoute} from 'vue-router';
import router from '@/config/router';

import {getDriverById} from '@/api/driver';
import {getProfileById} from '@/api/profile';
import {getDeviceById} from '@/api/device';
import {deviceCoverageGap, deviceQuality, deviceSilentSources, deviceStatusDetail} from '@/api/dashboard/device';

import baseCard from '@/components/card/base/BaseCard.vue';
import detailCard from '@/components/card/detail/DetailCard.vue';
import StatCard from '@/components/card/stat/StatCard.vue';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import QualityRing from '@/components/chart/QualityRing.vue';
import AvailabilityTimeline from '@/components/chart/AvailabilityTimeline.vue';
import point from '@/views/point/Point.vue';
import pointValue from '@/views/point/value/PointValue.vue';
import CommandList from '@/views/settings/command/CommandList.vue';
import EventList from '@/views/settings/event/definition/EventList.vue';
import TrendChart from '@/views/home/components/TrendChart.vue';
import ActivityHeatmap from '@/views/home/components/ActivityHeatmap.vue';
import LatencyChart from '@/views/home/components/LatencyChart.vue';
import LiveDataFeed from '@/views/home/components/LiveDataFeed.vue';
import AlertList from '@/views/home/components/AlertList.vue';
import {timestamp} from '@/utils/dateUtil';
import {copy} from '@/utils/commonUtil';
import {useAsyncLoader} from '@/utils/asyncLoaderUtil';
import type {DeviceRecord, DriverRecord, PointRecord, ProfileRecord} from '@/config/types/manager';
import type {DeviceCoverageGap, DeviceQuality, DeviceSilentSource, DeviceStatusDetail} from '@/config/types/dashboard';
import {useBreakpoint} from '@/composables/useBreakpoint';

const route = useRoute();
const {t} = useI18n();
const {isMobile} = useBreakpoint();
const pointViewRef = ref<InstanceType<typeof point>>();
const commandViewRef = ref<InstanceType<typeof CommandList>>();
const eventViewRef = ref<InstanceType<typeof EventList>>();
const pointValueViewRef = ref<InstanceType<typeof pointValue>>();

const reactiveData = reactive({
  id: String(route.query.id ?? ''),
  active: (route.query.active as string) || 'dashboard',
  loading: true,
  status: 'idle' as 'idle' | 'loading' | 'success' | 'error',
  data: {} as Partial<DeviceRecord>,
  driver: {} as Partial<DriverRecord>,
  profile: {} as Partial<ProfileRecord>,
  driverLoading: false,
  profileLoading: false,
  driverLoadError: false,
  profileLoadError: false,
  profileTable: {} as Record<string, any>,
  pointTable: {} as Record<string, any>,
  deviceTable: {} as Record<string, any>,
  unitTable: {} as Record<string, any>,
  listProfileData: [] as ProfileRecord[],
  listPointData: [] as PointRecord[],
  listPointValueData: [] as Record<string, unknown>[],
  listPointValueHistoryData: {} as Record<string, any>,
  pointValueDetailData: {} as Record<string, any>,
});

let requestId = 0;
let driverRequestId = 0;
let profileRequestId = 0;

// ---- Device dashboard data (lazy-loaded when the dashboard tab activates) ----
const statusDetail = ref<Partial<DeviceStatusDetail>>({});
const qualityData = ref<DeviceQuality | null>(null);
const coverageData = ref<DeviceCoverageGap>({totalPoints: 0, missingPoints: 0, items: []});
const silentData = ref<DeviceSilentSource[]>([]);

const statusLoader = useAsyncLoader();
const qualityLoader = useAsyncLoader();
const coverageLoader = useAsyncLoader();
const silentLoader = useAsyncLoader();

const loadStatus = async () => {
  const deviceId = String(reactiveData.id || '');
  if (!deviceId) return;
  await statusLoader.run(() => deviceStatusDetail(deviceId), {
    apply: (res) => {
      if (deviceId !== String(reactiveData.id || '')) return;
      statusDetail.value = res || {};
    },
  });
};

const loadQuality = async () => {
  const deviceId = String(reactiveData.id || '');
  if (!deviceId) return;
  await qualityLoader.run(() => deviceQuality(deviceId), {
    apply: (res) => {
      if (deviceId !== String(reactiveData.id || '')) return;
      qualityData.value = res || null;
    },
  });
};

const loadCoverage = async () => {
  const deviceId = String(reactiveData.id || '');
  if (!deviceId) return;
  await coverageLoader.run(() => deviceCoverageGap(deviceId), {
    apply: (res) => {
      if (deviceId !== String(reactiveData.id || '')) return;
      coverageData.value = res || {totalPoints: 0, missingPoints: 0, items: []};
    },
  });
};

const loadSilent = async () => {
  const deviceId = String(reactiveData.id || '');
  if (!deviceId) return;
  await silentLoader.run(() => deviceSilentSources(deviceId, {limit: 10}), {
    apply: (res) => {
      if (deviceId !== String(reactiveData.id || '')) return;
      silentData.value = Array.isArray(res) ? res : [];
    },
  });
};

const loadAvailability = () => {
  void loadCoverage();
  void loadSilent();
};

const loadDashboard = () => {
  void loadStatus();
  void loadQuality();
  void loadAvailability();
};

const qualityLoading = computed(() => qualityLoader.loading.value);
const qualityError = computed(() => !!qualityLoader.error.value);
const coverageLoading = computed(() => coverageLoader.loading.value);
const coverageError = computed(() => !!coverageLoader.error.value);
const availabilityLoading = computed(() => coverageLoader.loading.value || silentLoader.loading.value);
const availabilityError = computed(() => !!coverageLoader.error.value || !!silentLoader.error.value);

const profileId = computed(() => String(reactiveData.data.profileId || ''));

const pointLength = computed(() => {
  return pointViewRef.value?.reactiveData?.page?.total || 0;
});

// The dashboard's point count prefers the declared-point total from the
// coverage endpoint (the point tab is lazy, so pointLength is 0 until the
// user opens it); fall back to the lazy tab once it has loaded.
const totalPointsLabel = computed(() => coverageData.value?.totalPoints || pointLength.value);

const commandLength = computed(() => {
  return commandViewRef.value?.reactiveData?.page?.total || 0;
});

const eventLength = computed(() => {
  return eventViewRef.value?.reactiveData?.page?.total || 0;
});

const statusCode = computed(() => String(statusDetail.value.status || '').toLowerCase());

const copyCode = () => copy(String(reactiveData.data.deviceCode || ''), t('device.detail.copyCode'));

const statusLabel = computed(() => {
  switch (statusCode.value) {
    case 'online':
      return t('status.online');
    case 'offline':
      return t('status.offline');
    case 'maintain':
      return t('status.maintain');
    case 'fault':
      return t('status.fault');
    default:
      return t('status.unknown');
  }
});

const statusDotColor = computed(() => {
  switch (statusCode.value) {
    case 'online':
      return 'var(--el-color-success)';
    case 'maintain':
      return 'var(--el-color-warning)';
    case 'fault':
      return 'var(--el-color-danger)';
    case 'offline':
      return 'var(--el-color-info)';
    default:
      return 'var(--el-color-info)';
  }
});

const heartbeatLabel = computed(() => {
  const heartbeat = statusDetail.value.lastHeartbeatTime;
  return heartbeat ? timestamp(String(heartbeat)) : '-';
});

const timeoutLabel = computed(() => {
  const seconds = statusDetail.value.timeoutSeconds;
  return seconds != null ? `${seconds}s` : '-';
});

const qualityRatioLabel = computed(() => {
  const ratio = qualityData.value?.numericRatio;
  if (ratio == null || !Number.isFinite(ratio)) return '—';
  return `${Number.isInteger(ratio) ? ratio : ratio.toFixed(1)}%`;
});

const missingPointCount = computed(() => coverageData.value?.missingPoints ?? 0);

const latestUpdateLabel = computed(() => {
  const latest = qualityData.value?.latestSeen;
  return latest ? timestamp(String(latest)) : '—';
});

const loadDriver = (deviceRequestId: number, deviceId: string, driverId: string) => {
  if (!driverId) return;
  const relationRequestId = ++driverRequestId;
  reactiveData.driverLoading = true;
  reactiveData.driverLoadError = false;
  void getDriverById(driverId)
    .then((res) => {
      if (
        deviceRequestId !== requestId ||
        relationRequestId !== driverRequestId ||
        deviceId !== String(reactiveData.id || '')
      ) return;
      reactiveData.driver = res || {};
    })
    .catch(() => {
      if (
        deviceRequestId === requestId &&
        relationRequestId === driverRequestId &&
        deviceId === String(reactiveData.id || '')
      ) {
        reactiveData.driverLoadError = true;
      }
    })
    .finally(() => {
      if (deviceRequestId === requestId && relationRequestId === driverRequestId) reactiveData.driverLoading = false;
    });
};

const loadProfile = (deviceRequestId: number, deviceId: string, profileIdValue: string) => {
  if (!profileIdValue) return;
  const relationRequestId = ++profileRequestId;
  reactiveData.profileLoading = true;
  reactiveData.profileLoadError = false;
  void getProfileById(profileIdValue)
    .then((res) => {
      if (
        deviceRequestId !== requestId ||
        relationRequestId !== profileRequestId ||
        deviceId !== String(reactiveData.id || '')
      ) return;
      reactiveData.profile = res || {};
    })
    .catch(() => {
      if (
        deviceRequestId === requestId &&
        relationRequestId === profileRequestId &&
        deviceId === String(reactiveData.id || '')
      ) {
        reactiveData.profileLoadError = true;
      }
    })
    .finally(() => {
      if (deviceRequestId === requestId && relationRequestId === profileRequestId) reactiveData.profileLoading = false;
    });
};

const retryDriver = () => {
  const driverId = String(reactiveData.data.driverId || '');
  if (driverId && reactiveData.data.id) loadDriver(requestId, String(reactiveData.data.id), driverId);
};

const retryProfile = () => {
  const profileIdValue = String(reactiveData.data.profileId || '');
  if (profileIdValue && reactiveData.data.id) loadProfile(requestId, String(reactiveData.data.id), profileIdValue);
};

const device = () => {
  const currentRequestId = ++requestId;
  const deviceId = String(reactiveData.id || '');
  reactiveData.loading = true;
  reactiveData.status = 'loading';
  reactiveData.data = {};
  reactiveData.driver = {};
  reactiveData.profile = {};
  driverRequestId += 1;
  profileRequestId += 1;
  reactiveData.driverLoading = false;
  reactiveData.profileLoading = false;
  reactiveData.driverLoadError = false;
  reactiveData.profileLoadError = false;
  if (!deviceId) {
    reactiveData.loading = false;
    reactiveData.status = 'error';
    return Promise.resolve();
  }
  return getDeviceById(deviceId)
    .then((res) => {
      if (currentRequestId !== requestId || deviceId !== String(reactiveData.id || '')) return;
      reactiveData.data = res || {};
      if (!res?.id) {
        reactiveData.status = 'error';
        return;
      }
      reactiveData.deviceTable = {[res.id]: res.deviceName};
      reactiveData.status = 'success';

      const driverId = String(reactiveData.data.driverId || '');
      loadDriver(currentRequestId, deviceId, driverId);
      loadProfile(currentRequestId, deviceId, String(reactiveData.data.profileId || ''));
    })
    .catch(() => {
      if (currentRequestId === requestId) reactiveData.status = 'error';
    })
    .finally(() => {
      if (currentRequestId === requestId) reactiveData.loading = false;
    });
};

const changeActive = (tab: any) => {
  reactiveData.active = String(tab.props.name);
  const query = route.query;
  router.push({query: {...query, active: String(tab.props.name)}}).catch(() => {
    // nothing to do
  });

  switch (tab.props.name) {
    case 'dashboard':
      loadDashboard();
      break;
    case 'point':
      pointViewRef.value?.refresh();
      break;
    case 'command':
      commandViewRef.value?.refresh();
      break;
    case 'event':
      eventViewRef.value?.refresh();
      break;
    case 'pointValue':
      pointValueViewRef.value?.refresh();
      break;
    default:
      break;
  }
};

watch(
  () => [route.query.id, route.query.active],
  ([id, active]) => {
    const nextId = String(id ?? '');
    if (nextId !== reactiveData.id) {
      reactiveData.id = nextId;
      reactiveData.data = {};
      reactiveData.driver = {};
      reactiveData.profile = {};
      reactiveData.driverLoadError = false;
      reactiveData.profileLoadError = false;
      reactiveData.profileTable = {};
      reactiveData.pointTable = {};
      reactiveData.deviceTable = {};
      reactiveData.unitTable = {};
      reactiveData.listProfileData = [];
      reactiveData.listPointData = [];
      reactiveData.listPointValueData = [];
      reactiveData.listPointValueHistoryData = {};
      reactiveData.pointValueDetailData = {};
      statusDetail.value = {};
      qualityData.value = null;
      coverageData.value = {totalPoints: 0, missingPoints: 0, items: []};
      silentData.value = [];
      device();
      if (((active as string) || 'dashboard') === 'dashboard') loadDashboard();
    }
    reactiveData.active = (active as string) || 'dashboard';
  }
);

onMounted(() => {
  device();
  if (reactiveData.active === 'dashboard') loadDashboard();
});

onBeforeUnmount(() => {
  requestId += 1;
  driverRequestId += 1;
  profileRequestId += 1;
  statusLoader.invalidate();
  qualityLoader.invalidate();
  coverageLoader.invalidate();
  silentLoader.invalidate();
});
</script>

<style lang="scss" scoped>
.detail-page-alert {
  margin-bottom: var(--dc3-gutter);

  :deep(.el-alert__content) {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--dc3-space-2);
  }
}

.detail-inline-error {
  color: var(--el-color-danger);
}

// Device dashboard tab — mirrors the Home page's el-row/gutter rhythm so
// the reused chart/feed cards sit on the same 8px (12px mobile) grid.
.device-dashboard {
  // Device 名片: the device-card header anatomy (tone tile + name + attach)
  // scaled up, with the status as a soft chip and the machine code + live
  // meta demoted to a quiet footer.
  &__banner {
    padding: var(--dc3-space-4);
    border: 1px solid var(--dc3-border-base);
    border-radius: var(--dc3-radius-lg);
    background: var(--dc3-bg-elevated);
    margin-bottom: var(--dc3-gutter);
  }

  &__hero {
    display: flex;
    align-items: center;
    gap: var(--dc3-space-3);
  }

  &__tile {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 48px;
    height: 48px;
    border: 1px solid color-mix(in srgb, var(--dc3-color-purple) 20%, transparent);
    border-radius: var(--dc3-radius-lg);
    background: var(--dc3-color-purple-soft);
    color: var(--dc3-color-purple);
  }

  &__title {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  &__name {
    overflow: hidden;
    font-size: 16px;
    font-weight: 650;
    color: var(--dc3-text-primary);
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__attach {
    overflow: hidden;
    font-size: 12px;
    color: var(--dc3-text-muted);
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  // Soft tone chip — the same language as the point-value status chips.
  &__status-chip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;
    height: 24px;
    padding: 0 10px;
    margin-left: auto;
    border: 1px solid var(--dc3-border-base);
    border-radius: var(--dc3-radius-full);
    background: var(--el-fill-color-light);
    color: var(--dc3-text-secondary);
    font-size: 12px;
    font-weight: 600;

    &.is-online {
      border-color: color-mix(in srgb, var(--el-color-success) 30%, transparent);
      background: var(--el-color-success-light-9);
      color: var(--el-color-success);
    }

    &.is-maintain {
      border-color: color-mix(in srgb, var(--el-color-warning) 30%, transparent);
      background: var(--el-color-warning-light-9);
      color: var(--el-color-warning);
    }

    &.is-fault {
      border-color: color-mix(in srgb, var(--el-color-danger) 30%, transparent);
      background: var(--el-color-danger-light-9);
      color: var(--el-color-danger);
    }

    &.is-offline {
      border-color: var(--dc3-border-base);
      background: var(--el-fill-color-light);
      color: var(--dc3-text-secondary);
    }
  }

  &__status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: currentColor;
    flex-shrink: 0;
  }

  &__footer {
    display: flex;
    align-items: center;
    gap: var(--dc3-space-3);
    margin-top: var(--dc3-space-3);
    padding-top: var(--dc3-space-3);
    border-top: 1px solid var(--dc3-border-base);
  }

  // Machine code: copyable, monospace, ellipsized — a UUID is for copying,
  // not reading.
  &__code {
    overflow: hidden;
    max-width: 100%;
    padding: 0;
    border: 0;
    background: transparent;
    color: var(--dc3-text-muted);
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
    font-size: 12px;
    cursor: pointer;
    text-overflow: ellipsis;
    white-space: nowrap;

    &:hover {
      color: var(--dc3-text-brand);
      text-decoration: underline;
      text-underline-offset: 3px;
    }
  }

  &__live-meta {
    display: inline-flex;
    align-items: center;
    gap: var(--dc3-space-2);
    flex-shrink: 0;
    margin-left: auto;
    font-size: 12px;
    color: var(--dc3-text-secondary);
  }

  // Stat-card strip: 4 across on desktop, 2 on tablet, 1 on mobile.
  &__stats {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: var(--dc3-space-2);
    margin-bottom: var(--dc3-gutter);

    @media (max-width: $breakpoint-md-max) {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    @media (max-width: $breakpoint-xs-max) {
      grid-template-columns: 1fr;
    }
  }

  &__row {
    margin-bottom: var(--dc3-gutter);
    row-gap: var(--dc3-gutter);

    &:last-child {
      margin-bottom: 0;
    }
  }

  &__col {
    margin-bottom: 0;
  }

  // Center the quality ring vertically inside its (taller) dashboard card.
  &__ring {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
