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
      @show-add="openAdd"
      @size-change="sizeChange"
      @current-change="currentChange"
    />

    <blank-card>
      <el-row>
        <el-col v-for="data in 12" :key="data" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
          <skeleton-card :footer="true" :loading="reactiveData.loading" />
        </el-col>
        <el-col v-if="hasData">
          <el-empty :description="$t('point.empty')" />
        </el-col>
        <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
          <point-card
            :data="data"
            :embedded="embedded === 'profile' || embedded === 'device'"
            :profile="reactiveData.profileTable[data.profileId]"
            @delete-thing="deleteThing"
            @detail-thing="openDetail"
            @disable-thing="disableThing"
            @edit-thing="openEdit"
            @enable-thing="enableThing"
          />
        </el-col>
      </el-row>
    </blank-card>

    <point-edit-form ref="editRef" @add-thing="onAdd" @update-thing="onUpdate" />

    <el-drawer v-model="reactiveData.detailVisible" :title="$t('point.detail.pointInfo')" size="520px">
      <el-descriptions v-if="reactiveData.detailRecord" :column="1" border>
        <el-descriptions-item :label="$t('point.detail.pointName')">
          {{ reactiveData.detailRecord.pointName }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('point.card.dataType')">
          {{ $t(pointTypeKey(reactiveData.detailRecord.pointTypeFlag)) }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('point.card.rw')">
          {{ $t(rwFlagKey(reactiveData.detailRecord.rwFlag)) }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('point.card.unit')">
          {{ reactiveData.detailRecord.unit || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('point.card.ratio')">
          {{ reactiveData.detailRecord.multiple }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('point.card.baseValue')">
          {{ reactiveData.detailRecord.baseValue }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('point.card.accuracy')">
          {{ reactiveData.detailRecord.valueDecimal }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('point.card.profile')">
          {{
            reactiveData.detailRecord.profileId
              ? reactiveData.profileTable[reactiveData.detailRecord.profileId]?.profileName || '-'
              : '-'
          }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('point.detail.relatedDevices')" :span="2">
          {{ reactiveData.detailRecord.deviceCount || 0 }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('point.add.description')" :span="2">
          {{ reactiveData.detailRecord.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else :description="$t('common.description')" />
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
  import { computed, reactive, ref } from 'vue';

  import { addPoint, deletePoint, listPoint, updatePoint } from '@/api/point';
  import { listProfileByIds } from '@/api/profile';

  import type { Order, PointRecord } from '@/config/types';

  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import SkeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
  import { pointTypeKey, rwFlagKey } from '@/utils/pointFormatUtil';
  import { failMessage } from '@/utils/notificationUtil';
  import { isNull } from '@/utils/validationUtil';
  import PointEditForm from './add/PointEditForm.vue';
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
  type EditFormInstance = { show: (profileId: string) => void; showEdit: (row: PointRecord) => void };

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

  const editRef = ref<EditFormInstance | null>(null);

  const state = reactive({
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

  const reactiveData = state as typeof state & {
    detailVisible: boolean;
    detailRecord: PointRecord | null;
  };
  reactiveData.detailVisible = false;
  reactiveData.detailRecord = null;

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

  const load = () => {
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
    load();
  };

  const reset = () => {
    reactiveData.query = withFixedQuery();
    load();
  };

  const openAdd = () => {
    editRef.value?.show(props.profileId);
  };

  const openEdit = (row: PointRecord) => {
    editRef.value?.showEdit(row);
  };

  const openDetail = (row: PointRecord) => {
    reactiveData.detailRecord = row;
    reactiveData.detailVisible = true;
  };

  const onAdd = (form: unknown, done: () => void) => {
    addPoint(form as Record<string, unknown>)
      .then(() => {
        load();
        done();
      })
      .catch(() => {
        // nothing to do
      });
  };

  const onUpdate = (form: unknown, done: () => void) => {
    updatePoint(form as Record<string, unknown>)
      .then(() => {
        load();
        done();
      })
      .catch(() => {
        // nothing to do
      });
  };

  const disableThing = (id: string, profileId: string, done: () => void) => {
    updatePoint({ id, profileId, enableFlag: 'DISABLE' })
      .then(() => {
        load();
        done();
      })
      .catch(() => {
        // nothing to do
      });
  };

  const enableThing = (id: string, profileId: string, done: () => void) => {
    updatePoint({ id, profileId, enableFlag: 'ENABLE' })
      .then(() => {
        load();
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
          load();
          done();
        } else {
          failMessage(res.data.message);
        }
      })
      .catch(() => {
        // nothing to do
      });
  };

  const refresh = () => load();

  const sort = () => {
    reactiveData.order = !reactiveData.order;
    if (reactiveData.order) {
      reactiveData.page.orders = [{ column: 'create_time', asc: true }];
    } else {
      reactiveData.page.orders = [{ column: 'create_time', asc: false }];
    }
    load();
  };

  const sizeChange = (size: number) => {
    reactiveData.page.size = size;
    load();
  };

  const currentChange = (current: number) => {
    reactiveData.page.current = current;
    load();
  };

  const preHandle = () => emit('pre-handle');
  const nextHandle = () => emit('next-handle');

  defineExpose({ reactiveData, refresh });

  load();
</script>
