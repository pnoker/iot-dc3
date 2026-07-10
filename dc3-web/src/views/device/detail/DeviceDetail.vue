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
      <el-tabs v-model="reactiveData.active" @tab-click="changeActive">
        <el-tab-pane :label="$t('device.detail.deviceInfo')" name="detail">
          <detail-card>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="$t('device.detail.deviceName')"
              >{{ reactiveData.data.deviceName }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('device.detail.driverName')"
              >{{ reactiveData.driver.driverName }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('device.detail.profileName')"
              >{{ reactiveData.profile.profileName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('device.detail.profileCode')"
              >{{ reactiveData.profile.profileCode || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('device.detail.pointCount')">{{ pointLength }}</el-descriptions-item>
              <el-descriptions-item :label="$t('device.detail.commandCount')">{{ commandLength }}</el-descriptions-item>
              <el-descriptions-item :label="$t('device.detail.eventCount')">{{ eventLength }}</el-descriptions-item>
              <el-descriptions-item :label="$t('common.operationTime')"
              >{{ timestamp(reactiveData.data.createTime) }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.createTime')"
              >{{ timestamp(reactiveData.data.createTime) }}
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
import {computed, onMounted, reactive, ref, watch} from 'vue';

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

const route = useRoute();
const pointViewRef: any = ref<InstanceType<typeof point>>();
const commandViewRef = ref<InstanceType<typeof CommandList>>();
const eventViewRef = ref<InstanceType<typeof EventList>>();
const pointValueViewRef: any = ref<InstanceType<typeof pointValue>>();

// 定义响应式数据
const reactiveData = reactive({
  id: route.query.id as string,
  active: (route.query.active as string) || 'detail',
  profileLoading: true,
  pointLoading: true,
  pointValueLoading: true,
  data: {} as any,
  driver: {} as any,
  profile: {} as any,
  profileTable: {} as Record<string, any>,
  pointTable: {} as Record<string, any>,
  deviceTable: {} as Record<string, any>,
  unitTable: {} as Record<string, any>,
  listProfileData: [] as any[],
  listPointData: [] as any[],
  listPointValueData: [] as any[],
  listPointValueHistoryData: {} as Record<string, any>,
  pointValueDetailData: {} as Record<string, any>,
});

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

const device = () => {
  getDeviceById(reactiveData.id)
    .then((res) => {
      reactiveData.data = res.data;
      reactiveData.deviceTable[reactiveData.data.id] = reactiveData.data.deviceName;
      reactiveData.profile = {};

      getDriverById(reactiveData.data.driverId)
        .then((res) => {
          reactiveData.driver = res.data;
        })
        .catch(() => {
          // nothing to do
        });

      if (reactiveData.data.profileId) {
        getProfileById(String(reactiveData.data.profileId))
          .then((res) => {
            reactiveData.profile = res.data || {};
          })
          .catch(() => {
            // nothing to do
          });
      }
    })
    .catch(() => {
      // nothing to do
    })
    .finally(() => {
      reactiveData.profileLoading = false;
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
    const nextId = id as string;
    if (nextId && nextId !== reactiveData.id) {
      reactiveData.id = nextId;
      device();
    }
    reactiveData.active = (active as string) || 'detail';
  }
);

onMounted(() => {
  device();
});
</script>
