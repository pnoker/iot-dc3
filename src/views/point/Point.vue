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
    <point-tool
      :embedded="embedded"
      :next="next"
      :page="reactiveData.page"
      :pre="pre"
      @refresh="refresh"
      @reset="reset"
      @search="search"
      @sort="sort"
      @pre-handle="preHandle"
      @next-handle="nextHandle"
      @show-add="showAdd"
      @size-change="sizeChange"
      @current-change="currentChange"
    ></point-tool>

    <blank-card>
      <el-row>
        <el-col v-for="data in 12" :key="data" :lg="8" :md="12" :sm="12" :xl="6" :xs="24">
          <skeleton-card :footer="true" :loading="reactiveData.loading"></skeleton-card>
        </el-col>
        <el-col v-if="hasData">
          <el-empty :description="$t('point.empty')"></el-empty>
        </el-col>
        <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="8" :md="12" :sm="12" :xl="6" :xs="24">
          <point-card
            :data="data"
            :embedded="embedded != '' && embedded != 'edit'"
            :profile="reactiveData.profileTable[data.profileId]"
            @disable-thing="disableThing"
            @enable-thing="enableThing"
            @delete-thing="deleteThing"
          ></point-card>
        </el-col>
      </el-row>
    </blank-card>

    <point-add-form ref="pointAddFormRef" :profile-id="profileId" @add-thing="addThing"></point-add-form>
  </div>
</template>

<script lang="ts" setup>
  import { computed, reactive, ref } from 'vue';

  import { addPoint, deletePoint, listPoint, updatePoint } from '@/api/point';
  import { listProfileByIds } from '@/api/profile';

  import type { Order } from '@/config/types';

  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import SkeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
  import { failMessage } from '@/utils/notificationUtil';
  import { isNull } from '@/utils/validationUtil';
  import PointAddForm from './add/PointAddForm.vue';
  import PointCard from './card/PointCard.vue';
  import PointTool from './tool/PointTool.vue';

  interface PointListItem {
    id: string;
    profileId: string;
    [key: string]: unknown;
  }

  interface PointListPage {
    total: number;
    records: PointListItem[];
  }

  interface PointQuery extends Record<string, unknown> {
    profileId?: string;
    deviceId?: string;
  }

  type PointListResponse = R<PointListPage>;
  type LookupTableResponse = R<Record<string, unknown>>;
  type DialogInstance = { show: () => void };

  const props = withDefaults(
    defineProps<{
      embedded?: string;
      pre?: boolean;
      next?: boolean;
      profileId?: string;
      deviceId?: string;
    }>(),
    {
      embedded: '',
      pre: false,
      next: false,
      profileId: '',
      deviceId: '',
    }
  );

  const emit = defineEmits<{
    (e: 'pre-handle'): void;
    (e: 'next-handle'): void;
  }>();

  const pointAddFormRef = ref<DialogInstance | null>(null);

  const reactiveData = reactive({
    loading: true,
    profileTable: {} as Record<string, Record<string, any>>,
    listData: [] as PointListItem[],
    query: {} as PointQuery,
    order: false,
    page: {
      total: 0,
      size: 12,
      current: 1,
      orders: [] as Order[],
    },
  });

  const hasData = computed(() => !reactiveData.loading && reactiveData.listData.length < 1);

  const withFixedQuery = (params: PointQuery = {}) => {
    const nextQuery: PointQuery = { ...params };

    if (!isNull(props.profileId)) {
      nextQuery.profileId = props.profileId;
    }

    if (!isNull(props.deviceId)) {
      nextQuery.deviceId = props.deviceId;
    }

    return nextQuery;
  };

  const list = () => {
    const query = withFixedQuery(reactiveData.query);
    reactiveData.query = query;

    listPoint<PointListResponse>({
      page: reactiveData.page,
      ...query,
    })
      .then((res) => {
        const data = res.data;
        reactiveData.page.total = data.total;
        reactiveData.listData = data.records;

        const profileIds = Array.from(new Set(reactiveData.listData.map((point) => point.profileId)));
        if (profileIds.length === 0) {
          reactiveData.profileTable = {};
          return;
        }

        listProfileByIds(profileIds)
          .then((profileRes: LookupTableResponse) => {
            reactiveData.profileTable = profileRes.data as Record<string, Record<string, any>>;
          })
          .catch(() => {
            // nothing to do
          });
      })
      .catch(() => {
        // nothing to do
      })
      .finally(() => {
        reactiveData.loading = false;
      });
  };

  const search = (params: PointQuery) => {
    reactiveData.query = withFixedQuery(params);
    list();
  };

  const reset = () => {
    reactiveData.query = withFixedQuery();
    list();
  };

  const showAdd = () => {
    pointAddFormRef.value?.show();
  };

  const addThing = (form: unknown, done: () => void) => {
    addPoint(form as Record<string, unknown>)
      .then(() => {
        list();
        done();
      })
      .catch(() => {
        // nothing to do
      });
  };

  const disableThing = (id: string, profileId: string, done: () => void) => {
    updatePoint({ id, profileId, enableFlag: 'DISABLE' })
      .then(() => {
        list();
        done();
      })
      .catch(() => {
        // nothing to do
      });
  };

  const enableThing = (id: string, profileId: string, done: () => void) => {
    updatePoint({ id, profileId, enableFlag: 'ENABLE' })
      .then(() => {
        list();
        done();
      })
      .catch(() => {
        // nothing to do
      });
  };

  const deleteThing = (id: string, done: () => void) => {
    deletePoint(id)
      .then((res) => {
        if (res.data.ok) {
          list();
          done();
        } else {
          failMessage(res.data.message);
        }
      })
      .catch(() => {
        // nothing to do
      });
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

  const preHandle = () => {
    emit('pre-handle');
  };

  const nextHandle = () => {
    emit('next-handle');
  };

  defineExpose({
    reactiveData,
    refresh,
  });

  list();
</script>
