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
  <div>
    <base-card>
      <el-tabs v-model="reactiveData.active" @tab-click="changeActive">
        <el-tab-pane :label="$t('device.detail.deviceInfo')" name="detail">
          <detail-card>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="$t('device.detail.deviceName')">{{
                reactiveData.data.deviceName
              }}</el-descriptions-item>
              <el-descriptions-item :label="$t('device.detail.profileCount')">{{ profileLength }}</el-descriptions-item>
              <el-descriptions-item :label="$t('device.detail.pointCount')">{{ pointLength }}</el-descriptions-item>
              <el-descriptions-item :label="$t('device.detail.driverName')">{{
                reactiveData.driver.driverName
              }}</el-descriptions-item>
              <el-descriptions-item :label="$t('common.operationTime')">{{
                timestamp(reactiveData.data.createTime)
              }}</el-descriptions-item>
              <el-descriptions-item :label="$t('common.createTime')">{{
                timestamp(reactiveData.data.createTime)
              }}</el-descriptions-item>
            </el-descriptions>
          </detail-card>
        </el-tab-pane>
        <el-tab-pane :label="$t('device.detail.relatedProfiles')" name="profile">
          <profile ref="profileViewRef" :device-id="reactiveData.id" :embedded="'device'"></profile>
        </el-tab-pane>
        <el-tab-pane :label="$t('device.detail.relatedPoints')" name="point">
          <point ref="pointViewRef" :device-id="reactiveData.id" :embedded="'device'"></point>
        </el-tab-pane>
        <el-tab-pane :label="$t('device.detail.deviceData')" name="pointValue">
          <point-value ref="pointValueViewRef" :device-id="reactiveData.id" :embedded="'device'"></point-value>
        </el-tab-pane>
      </el-tabs>
    </base-card>
  </div>
</template>

<script lang="ts" setup>
  import { computed, onMounted, reactive, ref } from 'vue';

  import { useRoute } from 'vue-router';
  import router from '@/config/router';

  import { getDriverById } from '@/api/driver';
  import { getProfileByDeviceId, getProfileByIds } from '@/api/profile';
  import { getDeviceById } from '@/api/device';

  import baseCard from '@/components/card/base/BaseCard.vue';
  import detailCard from '@/components/card/detail/DetailCard.vue';
  import profile from '@/views/profile/Profile.vue';
  import point from '@/views/point/Point.vue';
  import pointValue from '@/views/point/value/PointValue.vue';
  import { timestamp } from '@/utils/dateUtil';

  const route = useRoute();
  const profileViewRef: any = ref<InstanceType<typeof profile>>();
  const pointViewRef: any = ref<InstanceType<typeof point>>();
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

  const profileLength = computed(() => {
    return profileViewRef.value?.reactiveData?.page?.total || 0;
  });

  const pointLength = computed(() => {
    return pointViewRef.value?.reactiveData?.page?.total || 0;
  });

  const device = () => {
    getDeviceById(reactiveData.id)
      .then((res) => {
        reactiveData.data = res.data;
        reactiveData.deviceTable[reactiveData.data.id] = reactiveData.data.deviceName;

        getDriverById(reactiveData.data.driverId)
          .then((res) => {
            reactiveData.driver = res.data;
          })
          .catch(() => {
            // nothing to do
          });
      })
      .catch(() => {
        // nothing to do
      });
  };

  const profiles = () => {
    getProfileByDeviceId(reactiveData.id)
      .then((res) => {
        reactiveData.listProfileData = res.data;

        // profile
        const profileIds = Array.from(new Set(reactiveData.listProfileData.map((pointValue) => pointValue.id)));
        getProfileByIds(profileIds)
          .then((res) => {
            reactiveData.profileTable = res.data;
          })
          .catch(() => {
            // nothing to do
          });
      })
      .catch(() => {
        // nothing to do
      })
      .finally(() => {
        reactiveData.profileLoading = false;
      });
  };

  const changeActive = (tab: any) => {
    const query = route.query;
    router.push({ query: { ...query, active: String(tab.props.name) } }).catch(() => {
      // nothing to do
    });

    switch (tab.props.name) {
      case 'profile':
        break;
      case 'point':
        pointViewRef.value?.refresh();
        break;
      case 'pointValue':
        pointValueViewRef.value?.refresh();
        break;
      default:
        break;
    }
  };

  onMounted(() => {
    device();
    profiles();
  });
</script>

<style lang="scss" scoped>
  @use '@/styles/things-card.scss';
</style>
