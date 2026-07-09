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
      @open-add="openAdd"
      @size-change="sizeChange"
      @current-change="currentChange"
    />

    <blank-card>
      <el-row>
        <template v-if="reactiveData.loading">
          <el-col v-for="data in 12" :key="data" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
            <skeleton-card :footer="true" :loading="true"/>
          </el-col>
        </template>
        <template v-else>
          <el-col v-if="reactiveData.listData.length < 1">
            <el-empty :description="$t('point.empty')"/>
          </el-col>
          <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
            <point-card
              :data="data"
              :embedded="embedded === 'profile' || embedded === 'device'"
              :profile="reactiveData.profileTable[data.profileId ?? '']"
              @delete="onDelete"
              @detail="openDetail"
              @disable="onDisable"
              @edit="openEdit"
              @enable="onEnable"
            />
          </el-col>
        </template>
      </el-row>
    </blank-card>

    <point-edit-form ref="editRef" @add="onAdd" @update="onUpdate"/>

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
      <el-empty v-else :description="$t('common.description')"/>
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
import {computed, ref, watch} from 'vue';

import {addPoint, deletePoint, listPoint, updatePoint} from '@/api/point';
import {listProfileByIds} from '@/api/profile';
import {usePagedList} from '@/composables/usePagedList';
import {failMessage, successMessage} from '@/utils/notificationUtil';
import {isNull} from '@/utils/validationUtil';
import {pointTypeKey, rwFlagKey} from '@/utils/pointFormatUtil';

import type {PointRecord} from '@/config/types/manager';

import BlankCard from '@/components/card/blank/BlankCard.vue';
import SkeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
import PointEditForm from './add/PointEditForm.vue';
import PointCard from './card/PointCard.vue';
import PointTool from './tool/PointTool.vue';

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

const {
  state,
  load,
  search: _search,
  sort,
  sizeChange,
  currentChange,
} = usePagedList<PointRecord>({
  pageSize: 12,
  sortColumn: 'create_time',
  request: (query) => listPoint(query),
});

const reactiveData = state as typeof state & {
  detailVisible: boolean;
  detailRecord: PointRecord | null;
  profileTable: Record<string, Record<string, any>>;
};
reactiveData.detailVisible = false;
reactiveData.detailRecord = null;
reactiveData.profileTable = {};

const basePointQuery = computed(() => {
  const q: Record<string, unknown> = {};
  if (!isNull(props.profileId)) q.profileId = props.profileId;
  if (!isNull(props.deviceId)) q.deviceId = props.deviceId;
  return q;
});

const search = (params: Record<string, unknown>) => {
  _search({...basePointQuery.value, ...params});
};

const reset = () => {
  _search(basePointQuery.value);
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
      successMessage();
      load();
    })
    .catch(() => {
      failMessage();
    })
    .finally(() => {
      done();
    });
};

const onUpdate = (form: unknown, done: () => void) => {
  updatePoint(form as Record<string, unknown>)
    .then(() => {
      successMessage();
      load();
    })
    .catch(() => {
      failMessage();
    })
    .finally(() => {
      done();
    });
};

const onDisable = (id: string, profileId: string, done: () => void) => {
  updatePoint({id, profileId, enableFlag: 'DISABLE'})
    .then(() => {
      successMessage();
      load();
    })
    .catch(() => {
      failMessage();
    })
    .finally(() => {
      done();
    });
};

const onEnable = (id: string, profileId: string, done: () => void) => {
  updatePoint({id, profileId, enableFlag: 'ENABLE'})
    .then(() => {
      successMessage();
      load();
    })
    .catch(() => {
      failMessage();
    })
    .finally(() => {
      done();
    });
};

const onDelete = (id: string, done: () => void) => {
  deletePoint(id)
    .then(() => {
      successMessage();
      load();
    })
    .catch(() => {
      failMessage();
    })
    .finally(() => {
      done();
    });
};

const refresh = () => load();

const preHandle = () => emit('pre-handle');
const nextHandle = () => emit('next-handle');

watch(
  () => reactiveData.listData,
  (points) => {
    const profileIds = Array.from(new Set(points.map((p) => p.profileId).filter((id): id is string => !!id)));
    if (profileIds.length === 0) {
      reactiveData.profileTable = {};
      return;
    }
    listProfileByIds(profileIds)
      .then((res) => {
        reactiveData.profileTable = (res.data || {}) as Record<string, Record<string, any>>;
      })
      .catch(() => {
        // handled globally
      });
  }
);

defineExpose({reactiveData, refresh});

load();
</script>
