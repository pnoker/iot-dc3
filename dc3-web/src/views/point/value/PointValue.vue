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
    <point-value-tool
      :embedded="embedded"
      :page="reactiveData.page"
      :cursor-mode="embedded !== 'device'"
      :cursor-previous="embedded !== 'device' && reactiveData.page.current > 1"
      :cursor-next="embedded !== 'device' && reactiveData.page.hasNext"
      @refresh="refresh"
      @reset="reset"
      @search="search"
      @size-change="sizeChange"
      @current-change="currentChange"
      @cursor-previous="cursorPrevious"
      @cursor-next="cursorNext"
    ></point-value-tool>

    <el-alert
      v-if="reactiveData.status === 'error'"
      :closable="false"
      :title="$t('common.loadFailed')"
      class="entity-page-error"
      show-icon
      type="error"
    >
      <el-button :loading="reactiveData.loading" link type="danger" @click="refresh">
        {{ $t('common.retry') }}
      </el-button>
    </el-alert>

    <el-alert
      v-if="reactiveData.deviceLookupError"
      :closable="false"
      :title="$t('common.optionLoadFailed')"
      class="entity-page-error"
      show-icon
      type="error"
    >
      <el-button :loading="reactiveData.deviceLookupLoading" link type="danger" @click="retryDeviceLookup">
        {{ $t('common.retry') }}
      </el-button>
    </el-alert>
    <el-alert
      v-if="reactiveData.pointLookupError"
      :closable="false"
      :title="$t('common.optionLoadFailed')"
      class="entity-page-error"
      show-icon
      type="error"
    >
      <el-button :loading="reactiveData.pointLookupLoading" link type="danger" @click="retryPointLookup">
        {{ $t('common.retry') }}
      </el-button>
    </el-alert>
    <el-alert
      v-if="reactiveData.unitLookupError"
      :closable="false"
      :title="$t('common.optionLoadFailed')"
      class="entity-page-error"
      show-icon
      type="error"
    >
      <el-button :loading="reactiveData.unitLookupLoading" link type="danger" @click="retryUnitLookup">
        {{ $t('common.retry') }}
      </el-button>
    </el-alert>

    <!-- Fluid grid wall — see .entity-card-wall in global.scss. -->
    <div class="entity-card-wall">
      <template v-if="reactiveData.loading">
        <skeleton-card v-for="data in 12" :key="data" :footer="true" :loading="true"></skeleton-card>
      </template>
      <template v-else-if="hasData">
        <el-empty
          :description="embedded == 'device' ? $t('pointValue.empty.noDevice') : $t('pointValue.empty.noData')"
        ></el-empty>
      </template>
      <template v-else-if="reactiveData.status === 'error' && reactiveData.listData.length === 0">
        <el-empty :description="$t('common.loadFailed')"></el-empty>
      </template>
      <template v-else>
        <point-value-card
          v-for="data in reactiveData.listData"
          :key="data.id"
          :data="data"
          :device="reactiveData.deviceTable[data.deviceId]"
          :embedded="embedded"
          :point="reactiveData.pointTable[data.pointId]"
          :unit="reactiveData.unitTable[data.pointId]"
          @detail-thing="openDetail"
          @write-thing="openWrite"
        ></point-value-card>
      </template>
    </div>

    <point-value-edit-form ref="editRef" @update-thing="writeValue"/>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, onMounted, reactive, ref, watch} from 'vue';
import {getPointValueLatest, listPointByIds, listPointUnit, listPointValue, writePointValue} from '@/api/point';
import {listDeviceByIds} from '@/api/device';
import router from '@/config/router';

import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
import pointValueTool from './tool/PointValueTool.vue';
import pointValueCard from './card/PointValueCard.vue';
import pointValueEditForm from './edit/PointValueEditForm.vue';

import {isNull} from '@/utils/validationUtil';
import {successMessage} from '@/utils/notificationUtil';

const props = defineProps({
  embedded: {
    type: String,
    default: () => {
      return '';
    },
  },
  deviceId: {
    type: String,
    default: () => {
      return '';
    },
  },
});

const reactiveData = reactive({
  loading: true,
  status: 'idle' as 'idle' | 'loading' | 'success' | 'error',
  error: null as unknown | null,
  lastUpdated: null as number | null,
  deviceTable: {} as Record<string, any>,
  pointTable: {} as Record<string, any>,
  unitTable: {} as Record<string, any>,
  deviceLookupLoading: false,
  deviceLookupError: null as unknown | null,
  pointLookupLoading: false,
  pointLookupError: null as unknown | null,
  unitLookupLoading: false,
  unitLookupError: null as unknown | null,
  listData: [] as any[],
  query: {},
  page: {
    total: 0,
    hasNext: false,
    size: 12,
    current: 1,
  },
  cursorStack: [undefined] as Array<string | undefined>,
});

type PointValueListResponse = {
  items?: Array<Record<string, any>>;
  total?: number;
  hasNext?: boolean;
  nextCursor?: string | null;
};

let listRequestSequence = 0;
let lookupSequence = 0;
let deviceLookupRequestId = 0;
let pointLookupRequestId = 0;
let unitLookupRequestId = 0;
let disposed = false;

const editRef = ref<InstanceType<typeof pointValueEditForm>>();

const hasData = computed(() => {
  return reactiveData.status === 'success' && !reactiveData.loading && reactiveData.listData.length === 0;
});

const clearLookupTables = () => {
  reactiveData.deviceTable = {};
  reactiveData.pointTable = {};
  reactiveData.unitTable = {};
  reactiveData.deviceLookupLoading = false;
  reactiveData.deviceLookupError = null;
  reactiveData.pointLookupLoading = false;
  reactiveData.pointLookupError = null;
  reactiveData.unitLookupLoading = false;
  reactiveData.unitLookupError = null;
};

const listQuery = () => {
  const query = {...(reactiveData.query as Record<string, unknown>)};
  if (!isNull(props.deviceId)) query.deviceId = props.deviceId;
  else delete query.deviceId;
  return query;
};

const list = async () => {
  const requestId = ++listRequestSequence;
  const generation = ++lookupSequence;
  clearLookupTables();
  reactiveData.loading = true;
  reactiveData.status = 'loading';
  reactiveData.error = null;
  const query = listQuery();
  try {
    const response: PointValueListResponse = props.embedded === 'device'
      ? await getPointValueLatest({
        offset: (reactiveData.page.current - 1) * reactiveData.page.size,
        limit: reactiveData.page.size,
        ...query,
      })
      : await listPointValue({
        limit: reactiveData.page.size,
        cursor: reactiveData.cursorStack[reactiveData.page.current - 1],
        ...query,
      });
    if (requestId !== listRequestSequence) return;
    loadPointValueList(response, requestId, generation);
    reactiveData.page.hasNext = props.embedded === 'device' ? Boolean(response.hasNext) : Boolean(response.hasNext);
    if (props.embedded !== 'device') {
      reactiveData.cursorStack[reactiveData.page.current] = response.nextCursor ?? undefined;
    }
    reactiveData.status = 'success';
    reactiveData.lastUpdated = Date.now();
  } catch (error) {
    if (requestId !== listRequestSequence) return;
    reactiveData.error = error;
    reactiveData.status = 'error';
  } finally {
    if (requestId === listRequestSequence) reactiveData.loading = false;
  }
};

const loadPointValueList = (res: PointValueListResponse, requestId: number, generation: number) => {
  const rows = (Array.isArray(res.items) ? res.items : []).map((record) => {
    const nextRecord: Record<string, any> = {...record, hasLatestValue: record.hasLatestValue !== false};
    if (!nextRecord.hasLatestValue || !nextRecord.createTime || !nextRecord.operateTime) {
      nextRecord.interval = null;
      return nextRecord;
    }
    const createTime = new Date(nextRecord.createTime);
    const operateTime = new Date(nextRecord.operateTime);
    nextRecord.interval = operateTime.getTime() - createTime.getTime();
    return nextRecord;
  });
  reactiveData.listData = rows;
  reactiveData.page.total = res.total ?? rows.length;
  void loadDeviceLookup(rows, requestId, generation);
  void loadPointLookup(rows, requestId, generation);
  void loadUnitLookup(rows, requestId, generation);
};

const hasCurrentLookup = (requestId: number, generation: number, sequence: number) =>
  requestId === listRequestSequence && generation === lookupSequence && sequence > 0;

const loadDeviceLookup = (
  rows = reactiveData.listData,
  requestId = listRequestSequence,
  generation = lookupSequence,
) => {
  const sequence = ++deviceLookupRequestId;
  const deviceIds = Array.from(new Set(rows.map((pointValue) => pointValue.deviceId)
    .filter((id): id is string => typeof id === 'string' && id.length > 0)));
  reactiveData.deviceLookupError = null;
  if (deviceIds.length === 0) {
    reactiveData.deviceLookupLoading = false;
    reactiveData.deviceTable = {};
    return Promise.resolve();
  }
  reactiveData.deviceLookupLoading = true;
  return listDeviceByIds(deviceIds)
    .then((res) => {
      if (!hasCurrentLookup(requestId, generation, sequence)) return;
      reactiveData.deviceTable = (res || {}) as Record<string, any>;
    })
    .catch((error) => {
      if (!hasCurrentLookup(requestId, generation, sequence)) return;
      reactiveData.deviceLookupError = error;
    })
    .finally(() => {
      if (hasCurrentLookup(requestId, generation, sequence)) reactiveData.deviceLookupLoading = false;
    });
};

const loadPointLookup = (
  rows = reactiveData.listData,
  requestId = listRequestSequence,
  generation = lookupSequence,
) => {
  const sequence = ++pointLookupRequestId;
  const pointIds = Array.from(new Set(rows.map((pointValue) => pointValue.pointId)
    .filter((id): id is string => typeof id === 'string' && id.length > 0)));
  reactiveData.pointLookupError = null;
  if (pointIds.length === 0) {
    reactiveData.pointLookupLoading = false;
    reactiveData.pointTable = {};
    return Promise.resolve();
  }
  reactiveData.pointLookupLoading = true;
  return listPointByIds(pointIds)
    .then((res) => {
      if (!hasCurrentLookup(requestId, generation, sequence)) return;
      reactiveData.pointTable = (res || {}) as Record<string, any>;
    })
    .catch((error) => {
      if (!hasCurrentLookup(requestId, generation, sequence)) return;
      reactiveData.pointLookupError = error;
    })
    .finally(() => {
      if (hasCurrentLookup(requestId, generation, sequence)) reactiveData.pointLookupLoading = false;
    });
};

const loadUnitLookup = (
  rows = reactiveData.listData,
  requestId = listRequestSequence,
  generation = lookupSequence,
) => {
  const sequence = ++unitLookupRequestId;
  const pointIds = Array.from(new Set(rows.map((pointValue) => pointValue.pointId)
    .filter((id): id is string => typeof id === 'string' && id.length > 0)));
  reactiveData.unitLookupError = null;
  if (pointIds.length === 0) {
    reactiveData.unitLookupLoading = false;
    reactiveData.unitTable = {};
    return Promise.resolve();
  }
  reactiveData.unitLookupLoading = true;
  return listPointUnit(pointIds)
    .then((res) => {
      if (!hasCurrentLookup(requestId, generation, sequence)) return;
      reactiveData.unitTable = (res || {}) as Record<string, any>;
    })
    .catch((error) => {
      if (!hasCurrentLookup(requestId, generation, sequence)) return;
      reactiveData.unitLookupError = error;
    })
    .finally(() => {
      if (hasCurrentLookup(requestId, generation, sequence)) reactiveData.unitLookupLoading = false;
    });
};

const retryDeviceLookup = () => {
  void loadDeviceLookup();
};

const retryPointLookup = () => {
  void loadPointLookup();
};

const retryUnitLookup = () => {
  void loadUnitLookup();
};

const search = (params: any) => {
  const cleaned: Record<string, any> = {};
  for (const [k, v] of Object.entries(params)) {
    if (v !== '' && v != null) cleaned[k] = v;
  }
  reactiveData.query = cleaned;
  reactiveData.page.current = 1;
  reactiveData.cursorStack = [undefined];
  void list();
};

const reset = () => {
  reactiveData.query = !isNull(props.deviceId) ? {deviceId: props.deviceId} : {};
  reactiveData.page.current = 1;
  reactiveData.cursorStack = [undefined];
  void list();
};

const refresh = () => {
  void list();
};

const openWrite = (row: Record<string, unknown>) => {
  editRef.value?.show({
    ...row,
    value: String(row.calValue ?? ''),
  });
};

const writeValue = (formData: Record<string, unknown>, done: (successful?: boolean) => void) => {
  writePointValue({
    deviceId: formData.deviceId,
    pointId: formData.pointId,
    value: String(formData.value ?? ''),
  })
    .then(() => {
      if (disposed) return;
      successMessage();
      refresh();
      done(true);
    })
    .catch(() => {
      if (!disposed) done(false);
    });
};

const openDetail = (row: Record<string, unknown>) => {
  // Route detail page, on the same contract as every other entity detail
  // (device/driver/profile/point): navigable, shareable, breadcrumb-backed.
  void router.push({
    name: 'pointValueDetail',
    query: {
      deviceId: String(row.deviceId || ''),
      pointId: String(row.pointId || ''),
    },
  });
};

const sizeChange = (size: number) => {
  reactiveData.page.size = size;
  reactiveData.page.current = 1;
  reactiveData.cursorStack = [undefined];
  void list();
};

const currentChange = (current: number) => {
  if (reactiveData.loading || current < 1) return;
  if (props.embedded !== 'device' && current > reactiveData.page.current + 1) return;
  reactiveData.page.current = current;
  void list();
};

const cursorPrevious = () => {
  if (reactiveData.page.current <= 1) return;
  if (reactiveData.loading) return;
  reactiveData.page.current -= 1;
  void list();
};

const cursorNext = () => {
  if (!reactiveData.page.hasNext) return;
  if (reactiveData.loading) return;
  reactiveData.page.current += 1;
  void list();
};

watch(
  () => [props.embedded, props.deviceId],
  () => {
    reactiveData.query = !isNull(props.deviceId) ? {deviceId: props.deviceId} : {};
    reactiveData.page.current = 1;
    reactiveData.cursorStack = [undefined];
    void list();
  }
);

onMounted(() => {
  void list();
});

onBeforeUnmount(() => {
  disposed = true;
  listRequestSequence += 1;
  lookupSequence += 1;
});

defineExpose({
  refresh,
  list,
});
</script>
