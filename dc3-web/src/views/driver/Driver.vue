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
    <driver-tool
      :page="reactiveData.page"
      @refresh="refresh"
      @reset="reset"
      @search="search"
      @sort="sort"
      @size-change="sizeChange"
      @current-change="currentChange"
      @page-size-mode="onSizeMode"
    >
    </driver-tool>

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
      v-if="statusError"
      :closable="false"
      :title="$t('common.optionLoadFailed')"
      class="entity-page-error"
      show-icon
      type="error"
    >
      <el-button :loading="statusLoading" link type="danger" @click="retryStatusLookup">
        {{ $t('common.retry') }}
      </el-button>
    </el-alert>

    <!-- Fluid grid wall — see .entity-card-wall in global.scss. -->
    <div ref="wallRef" :class="['entity-card-wall', {'is-refreshing': isRefreshing}]">
      <template v-if="reactiveData.loading && reactiveData.listData.length < 1">
        <skeleton-card v-for="data in skeletonCount" :key="data" :footer="true" :loading="true"/>
      </template>
      <template v-else>
        <el-empty v-if="reactiveData.status === 'success' && reactiveData.listData.length < 1" :description="$t('driver.empty')"/>
        <el-empty v-else-if="reactiveData.status === 'error' && reactiveData.listData.length < 1" :description="$t('common.loadFailed')"/>
        <driver-card
          v-for="data in reactiveData.listData"
          :key="data.id"
          :busy="isActionBusy(data)"
          :data="data"
          :status-table="statusTable"
          @delete="onDelete"
        />
        <card-wall-ghosts v-if="reactiveData.listData.length > 0" :count="ghostCount"/>
      </template>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, reactive, ref, watch} from 'vue';

import {deleteDriver, listDriver, listDriverStatus} from '@/api/driver';
import {usePagedList} from '@/composables/usePagedList';

import type {DriverRecord} from '@/config/types/manager';
import {successMessage} from '@/utils/notificationUtil';

import CardWallGhosts from '@/components/card/wall/CardWallGhosts.vue';
import SkeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
import DriverCard from './card/DriverCard.vue';
import DriverTool from './tool/DriverTool.vue';

const wallRef = ref<HTMLElement | null>(null);

const {
  state: reactiveData,
  load,
  search: _search,
  sort,
  sizeChange,
  currentChange,
  bindWall,
  enableAuto,
  setManual,
  ghostCount,
} = usePagedList<DriverRecord>({
  autoPageSize: true,
  sortColumn: 'createTime',
  request: (query) => listDriver(query),
});

// Loading previews exactly the incoming fill; refreshes keep the current
// cards on screen (stale-while-revalidate) under the wall's activity line.
const skeletonCount = computed(() => Math.max(1, reactiveData.page.size));
const isRefreshing = computed(() => reactiveData.loading && reactiveData.listData.length > 0);

const onSizeMode = (value: 'auto' | number) => {
  if (value === 'auto') {
    enableAuto();
    return;
  }
  setManual(value);
};

bindWall(wallRef);

const statusTable = reactive<Record<string, string>>({});
let statusSequence = 0;
let statusRequestId = 0;
let disposed = false;
const statusLoading = ref(false);
const statusError = ref<unknown | null>(null);
const actionBusy = reactive(new Set<string>());

const search = (params: Record<string, unknown>) => {
  _search({type: 'driver', ...params});
};

const reset = () => {
  _search({type: 'driver'});
};

const refresh = () => load();

const isActionBusy = (driver: DriverRecord) => actionBusy.has(String(driver.id));

const onDelete = async (driver: DriverRecord) => {
  const id = String(driver.id);
  if (actionBusy.has(id)) return;
  actionBusy.add(id);
  try {
    await deleteDriver(driver.id, driver.version);
    if (disposed) return;
    successMessage();
    await load();
  } catch {
    // handled globally
  } finally {
    actionBusy.delete(id);
  }
};

const loadStatus = (sequence = statusSequence) => {
  const requestId = ++statusRequestId;
  statusLoading.value = true;
  statusError.value = null;
  const query = {...(reactiveData.query as Record<string, unknown>)};
  return listDriverStatus({
    offset: (reactiveData.page.current - 1) * reactiveData.page.size,
    limit: reactiveData.page.size,
    ...query,
  })
    .then((res) => {
      if (sequence !== statusSequence || requestId !== statusRequestId) return;
      // Swap atomically on arrival — badges stay visible under a refresh.
      for (const key of Object.keys(statusTable)) delete statusTable[key];
      Object.assign(statusTable, (res || {}) as Record<string, string>);
    })
    .catch((error) => {
      if (sequence !== statusSequence || requestId !== statusRequestId) return;
      statusError.value = error;
    })
    .finally(() => {
      if (sequence === statusSequence && requestId === statusRequestId) statusLoading.value = false;
    });
};

const retryStatusLookup = () => {
  void loadStatus();
};

watch(
  () => reactiveData.listData,
  () => {
    const sequence = ++statusSequence;
    statusError.value = null;
    void loadStatus(sequence);
  }
);

onBeforeUnmount(() => {
  disposed = true;
  statusSequence += 1;
  statusRequestId += 1;
  actionBusy.clear();
});

load();
</script>
