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
    >
    </driver-tool>

    <blank-card>
      <el-row>
        <template v-if="reactiveData.loading">
          <el-col v-for="data in 12" :key="data" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
            <skeleton-card :footer="true" :loading="true"/>
          </el-col>
        </template>
        <template v-else>
          <el-col v-if="reactiveData.listData.length < 1">
            <el-empty :description="$t('driver.empty')"/>
          </el-col>
          <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
            <driver-card :data="data" :status-table="statusTable"/>
          </el-col>
        </template>
      </el-row>
    </blank-card>
  </div>
</template>

<script lang="ts" setup>
import {reactive, watch} from 'vue';

import {listDriver, listDriverStatus} from '@/api/driver';
import {usePagedList} from '@/composables/usePagedList';

import type {DriverRecord} from '@/config/types/manager';

import BlankCard from '@/components/card/blank/BlankCard.vue';
import SkeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
import DriverCard from './card/DriverCard.vue';
import DriverTool from './tool/DriverTool.vue';

const {
  state: reactiveData,
  load,
  search: _search,
  sort,
  sizeChange,
  currentChange,
} = usePagedList<DriverRecord>({
  pageSize: 12,
  sortColumn: 'create_time',
  request: (query) => listDriver(query),
});

const statusTable = reactive<Record<string, string>>({});

const search = (params: Record<string, unknown>) => {
  _search({type: 'driver', ...params});
};

const reset = () => {
  _search({type: 'driver'});
};

const refresh = () => load();

const loadStatus = () => {
  listDriverStatus({page: reactiveData.page, ...(reactiveData.query as Record<string, unknown>)})
    .then((res) => {
      Object.assign(statusTable, res.data as Record<string, string>);
    })
    .catch(() => {
      // handled globally
    });
};

watch(
  () => reactiveData.listData,
  () => {
    loadStatus();
  }
);

load();
</script>
