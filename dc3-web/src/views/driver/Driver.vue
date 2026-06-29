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
            <skeleton-card :footer="true" :loading="true" />
          </el-col>
        </template>
        <template v-else>
          <el-col v-if="reactiveData.listData.length < 1">
            <el-empty :description="$t('driver.empty')" />
          </el-col>
          <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
            <driver-card :data="data" :status-table="statusTable" />
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
