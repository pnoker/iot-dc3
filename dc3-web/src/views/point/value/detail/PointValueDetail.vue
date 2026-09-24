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

<!-- Route detail page, on the same contract as every other detail view
     (DeviceDetail/DriverDetail/ProfileDetail/…): base-card shell, error and
     skeleton states, then the point dashboard — a single read-only board
     with the identity banner, stat strip and the six data modules. The old
     tabbed detail/descriptions presentation was absorbed by the banner. -->

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
        <el-button :loading="reactiveData.loading" link type="danger" @click="reload">
          {{ $t('common.retry') }}
        </el-button>
      </el-alert>
      <el-empty
        v-if="reactiveData.status === 'error' && !hasRecord"
        :description="$t('common.loadFailed')"
      />
      <el-skeleton v-else-if="!hasRecord && reactiveData.loading" :rows="6" animated/>
      <el-empty
        v-else-if="!hasRecord"
        :description="$t('pointValue.card.noLatestValue')"
      />
      <point-dashboard
        v-else
        :device-id="String(route.query.deviceId || '')"
        :device-name="deviceName"
        :latest="reactiveData.data"
        :point-id="String(route.query.pointId || '')"
        :point-name="pointName"
        :unit="reactiveData.unit"
      ></point-dashboard>
    </base-card>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, reactive, watch} from 'vue';
import {useRoute} from 'vue-router';

import baseCard from '@/components/card/base/BaseCard.vue';
import pointDashboard from './dashboard/PointDashboard.vue';
import {listDeviceByIds} from '@/api/device';
import {getPointValueLatest, listPointByIds, listPointUnit} from '@/api/point';

const route = useRoute();

const reactiveData = reactive({
  loading: false,
  status: 'idle' as 'idle' | 'loading' | 'success' | 'error',
  data: {} as Record<string, any>,
  device: null as Record<string, any> | null,
  point: null as Record<string, any> | null,
  unit: '' as string,
});

const deviceName = computed(() => String(reactiveData.device?.deviceName || ''));
const pointName = computed(() => String(reactiveData.point?.pointName || reactiveData.data.pointName || ''));
// Time-series latest-value records carry no record id — presence is "the
// endpoint returned an item", not "item.id is truthy".
const hasRecord = computed(() => !!reactiveData.data.deviceId || reactiveData.data.calValue !== undefined);

const load = async () => {
  const deviceId = String(route.query.deviceId || '');
  const pointId = String(route.query.pointId || '');
  if (!deviceId && !pointId) return;

  reactiveData.loading = true;
  reactiveData.status = 'loading';
  try {
    // Lookup helpers enrich the record with names/units; failures here are
    // non-fatal — the banner degrades to '-'.
    const [latest, devices, points, units] = await Promise.all([
      getPointValueLatest({deviceId, pointId, offset: 0, limit: 1}),
      deviceId ? listDeviceByIds([deviceId]).catch(() => []) : Promise.resolve([]),
      pointId ? listPointByIds([pointId]).catch(() => []) : Promise.resolve([]),
      pointId ? listPointUnit([pointId]).catch(() => []) : Promise.resolve([]),
    ]);
    reactiveData.data = latest?.items?.[0] ?? {};
    // The byIds endpoints return id→record maps (see PointValue.vue's
    // lookup tables), not arrays.
    const deviceMap = (devices || {}) as Record<string, any>;
    const pointMap = (points || {}) as Record<string, any>;
    const unitMap = (units || {}) as Record<string, any>;
    reactiveData.device = deviceMap[deviceId] ?? null;
    reactiveData.point = pointMap[pointId] ?? null;
    reactiveData.unit = String(unitMap[pointId] ?? '').trim();
    reactiveData.status = 'success';
  } catch {
    reactiveData.status = 'error';
  } finally {
    reactiveData.loading = false;
  }
};

const reload = () => void load();

onMounted(() => void load());
watch(() => [route.query.deviceId, route.query.pointId], () => void load());
</script>

<style lang="scss" scoped>
.detail-page-alert {
  margin-bottom: var(--dc3-gutter);

  :deep(.el-alert__content) {
    display: flex;
  }
}
</style>
