<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <https://www.gnu.org/licenses/>.
  -->

<!-- Route detail page, on the same contract as every other detail view
     (DeviceDetail/DriverDetail/PointDetail/…): base-card shell, error and
     skeleton states, structured el-descriptions fields. The old drawer +
     raw-JSON presentation was the system's only outlier. -->

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
      <el-tabs v-else v-model="reactiveData.active" v-loading="reactiveData.loading" @tab-click="changeActive">
        <el-tab-pane :label="$t('pointValue.detail.title')" name="detail">
          <detail-card>
            <el-descriptions :column="isMobile ? 1 : 2" border>
              <el-descriptions-item :label="$t('pointValue.tool.pointName')">
                {{ reactiveData.point?.pointName || reactiveData.data.pointName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('pointValue.card.device')">
                {{ reactiveData.device?.deviceName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('pointValue.card.rwType')">
                <el-tag v-if="isReadOnly" effect="plain" type="warning">{{ $t('status.readOnly') }}</el-tag>
                <el-tag v-else-if="isWriteOnly" effect="plain" type="info">{{ $t('status.writeOnly') }}</el-tag>
                <el-tag v-else-if="isReadWrite" effect="plain" type="success">{{ $t('status.readWrite') }}</el-tag>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('pointValue.card.processedValue')">
                {{ reactiveData.data.calValue ?? '--' }} {{ hasRecord ? unit : '' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('pointValue.card.rawValue')">
                {{ reactiveData.data.rawValue ?? '--' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('pointValue.card.delay')">
                {{ typeof reactiveData.data.interval === 'number' ? `${reactiveData.data.interval} ms` : '--' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('pointValue.card.collectTime')">
                {{ timestampLabel(reactiveData.data.createTime) }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('pointValue.card.saveTime')">
                {{ timestampLabel(reactiveData.data.operateTime) }}
              </el-descriptions-item>
            </el-descriptions>
          </detail-card>
        </el-tab-pane>
      </el-tabs>
    </base-card>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, reactive, watch} from 'vue';
import {useRoute} from 'vue-router';
import router from '@/config/router';
import {useBreakpoint} from '@/composables/useBreakpoint';

import baseCard from '@/components/card/base/BaseCard.vue';
import detailCard from '@/components/card/detail/DetailCard.vue';
import {listDeviceByIds} from '@/api/device';
import {getPointValueLatest, listPointByIds, listPointUnit} from '@/api/point';
import {timestampLabel} from '@/utils/dateUtil';
import type {TabsPaneContext} from 'element-plus';

const route = useRoute();
const {isMobile} = useBreakpoint();

const reactiveData = reactive({
  loading: false,
  status: 'idle' as 'idle' | 'loading' | 'success' | 'error',
  active: (route.query.active as string) || 'detail',
  data: {} as Record<string, any>,
  device: null as Record<string, any> | null,
  point: null as Record<string, any> | null,
  unit: '' as string,
});

const rwFlag = computed(() => String(reactiveData.data?.rwFlag || '').toUpperCase());
const isReadOnly = computed(() => ['R', 'READ_ONLY'].includes(rwFlag.value));
const isWriteOnly = computed(() => ['W', 'WRITE_ONLY'].includes(rwFlag.value));
const isReadWrite = computed(() => ['RW', 'READ_WRITE'].includes(rwFlag.value));
const unit = computed(() => reactiveData.unit);
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
    // non-fatal — the descriptions degrade to '-'.
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

const changeActive = (tab: TabsPaneContext) => {
  const query = route.query;
  router.push({query: {...query, active: String(tab.props.name)}}).catch(() => {
    // Navigation duplicated — the tab state is already correct.
  });
};

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
