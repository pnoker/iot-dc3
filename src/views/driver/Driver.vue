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
        <el-col v-for="data in 12" :key="data" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
          <skeleton-card :footer="true" :loading="reactiveData.loading"></skeleton-card>
        </el-col>
        <el-col v-if="hasData">
          <el-empty :description="$t('driver.empty')"></el-empty>
        </el-col>
        <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
          <driver-card :data="data" :status-table="reactiveData.statusTable"></driver-card>
        </el-col>
      </el-row>
    </blank-card>
  </div>
</template>

<script lang="ts" setup>
  import { computed, reactive } from 'vue';

  import { getDriverStatus, listDriver } from '@/api/driver';

  import type { Order } from '@/config/types';

  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import SkeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
  import DriverCard from './card/DriverCard.vue';
  import DriverTool from './tool/DriverTool.vue';

  interface DriverListItem {
    id: string;
    [key: string]: unknown;
  }

  interface DriverListPage {
    total: number;
    records: DriverListItem[];
  }

  interface DriverQuery extends Record<string, unknown> {
    type: 'driver';
  }

  type DriverListResponse = R<DriverListPage>;
  type DriverStatusResponse = R<Record<string, unknown>>;

  const reactiveData = reactive({
    loading: true,
    statusTable: {} as Record<string, string>,
    listData: [] as DriverListItem[],
    query: {
      type: 'driver',
    } as DriverQuery,
    order: false,
    page: {
      total: 0,
      size: 12,
      current: 1,
      orders: [] as Order[],
    },
  });

  const hasData = computed(() => !reactiveData.loading && reactiveData.listData.length < 1);

  const list = () => {
    const listPromise = listDriver<DriverListResponse>({
      page: reactiveData.page,
      ...reactiveData.query,
    })
      .then((res) => {
        const data = res.data;
        reactiveData.page.total = data.total;
        reactiveData.listData = data.records;
      })
      .catch(() => {
        // nothing to do
      });

    const statusPromise = getDriverStatus({
      page: reactiveData.page,
      ...reactiveData.query,
    })
      .then((res: DriverStatusResponse) => {
        reactiveData.statusTable = res.data as Record<string, string>;
      })
      .catch(() => {
        // nothing to do
      });

    Promise.all([listPromise, statusPromise]).finally(() => {
      reactiveData.loading = false;
    });
  };

  const search = (params: Record<string, unknown>) => {
    reactiveData.query = { ...params, type: 'driver' };
    list();
  };

  const reset = () => {
    reactiveData.query = { type: 'driver' };
    list();
  };

  const refresh = () => {
    list();
  };

  const sort = () => {
    reactiveData.order = !reactiveData.order;
    if (reactiveData.order) {
      reactiveData.page.orders = [{ column: 'create_time', asc: true }];
    } else {
      reactiveData.page.orders = [{ column: 'create_time', asc: false }];
    }
    list();
  };

  const sizeChange = (size: number) => {
    reactiveData.page.size = size;
    list();
  };

  const currentChange = (current: number) => {
    reactiveData.page.current = current;
    list();
  };

  list();
</script>
