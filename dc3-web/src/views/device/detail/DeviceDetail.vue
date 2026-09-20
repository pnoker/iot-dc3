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
        <el-tab-pane :label="$t('device.detail.deviceInfo')" name="detail">
          <detail-card>
            <el-descriptions :column="isMobile ? 1 : 2" border>
              <el-descriptions-item :label="$t('device.detail.deviceName')"
              >{{ reactiveData.data.deviceName }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('device.detail.driverName')"
              >
                <span v-if="reactiveData.driverLoadError" class="detail-inline-error">
                  {{ $t('common.loadFailed') }}
                  <el-button :loading="reactiveData.driverLoading" link size="small" type="danger" @click="retryDriver">
                    {{ $t('common.retry') }}
                  </el-button>
                </span>
                <span v-else>{{ reactiveData.driver.driverName || '-' }}</span>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('device.detail.profileName')"
              >
                <span v-if="reactiveData.profileLoadError" class="detail-inline-error">
                  {{ $t('common.loadFailed') }}
                  <el-button :loading="reactiveData.profileLoading" link size="small" type="danger" @click="retryProfile">
                    {{ $t('common.retry') }}
                  </el-button>
                </span>
                <span v-else>{{ reactiveData.profile.profileName || '-' }}</span>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('device.detail.profileCode')"
              >{{ reactiveData.profile.profileCode || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('device.detail.pointCount')">{{ pointLength }}</el-descriptions-item>
              <el-descriptions-item :label="$t('device.detail.commandCount')">{{ commandLength }}</el-descriptions-item>
              <el-descriptions-item :label="$t('device.detail.eventCount')">{{ eventLength }}</el-descriptions-item>
              <el-descriptions-item :label="$t('common.operationTime')"
              >{{ timestamp(reactiveData.data.operateTime || '') }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.createTime')"
              >{{ timestamp(reactiveData.data.createTime || '') }}
              </el-descriptions-item>
            </el-descriptions>
          </detail-card>
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

import {useRoute} from 'vue-router';
import router from '@/config/router';

import {getDriverById} from '@/api/driver';
import {getProfileById} from '@/api/profile';
import {getDeviceById} from '@/api/device';

import baseCard from '@/components/card/base/BaseCard.vue';
import detailCard from '@/components/card/detail/DetailCard.vue';
import point from '@/views/point/Point.vue';
import pointValue from '@/views/point/value/PointValue.vue';
import CommandList from '@/views/settings/command/CommandList.vue';
import EventList from '@/views/settings/event/definition/EventList.vue';
import {timestamp} from '@/utils/dateUtil';
import type {DeviceRecord, DriverRecord, PointRecord, ProfileRecord} from '@/config/types/manager';
import {useBreakpoint} from '@/composables/useBreakpoint';

const route = useRoute();
const {isMobile} = useBreakpoint();
const pointViewRef = ref<InstanceType<typeof point>>();
const commandViewRef = ref<InstanceType<typeof CommandList>>();
const eventViewRef = ref<InstanceType<typeof EventList>>();
const pointValueViewRef = ref<InstanceType<typeof pointValue>>();

const reactiveData = reactive({
  id: String(route.query.id ?? ''),
  active: (route.query.active as string) || 'detail',
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

const profileId = computed(() => String(reactiveData.data.profileId || ''));

const pointLength = computed(() => {
  return pointViewRef.value?.reactiveData?.page?.total || 0;
});

const commandLength = computed(() => {
  return commandViewRef.value?.reactiveData?.page?.total || 0;
});

const eventLength = computed(() => {
  return eventViewRef.value?.reactiveData?.page?.total || 0;
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
      device();
    }
    reactiveData.active = (active as string) || 'detail';
  }
);

onMounted(() => {
  device();
});

onBeforeUnmount(() => {
  requestId += 1;
  driverRequestId += 1;
  profileRequestId += 1;
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
</style>
