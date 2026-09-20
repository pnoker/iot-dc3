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
      v-if="reactiveData.profileLookupError"
      :closable="false"
      :title="$t('common.optionLoadFailed')"
      class="entity-page-error"
      show-icon
      type="error"
    >
      <el-button :loading="reactiveData.profileLookupLoading" link type="danger" @click="retryProfileLookup">
        {{ $t('common.retry') }}
      </el-button>
    </el-alert>

    <!-- Fluid grid wall — see .entity-card-wall in global.scss. -->
    <div class="entity-card-wall">
      <template v-if="reactiveData.loading">
        <skeleton-card v-for="data in 12" :key="data" :footer="true" :loading="true"/>
      </template>
      <template v-else>
        <el-empty v-if="reactiveData.status === 'success' && reactiveData.listData.length < 1" :description="$t('point.empty')"/>
        <el-empty v-else-if="reactiveData.status === 'error' && reactiveData.listData.length < 1" :description="$t('common.loadFailed')"/>
        <point-card
          v-for="data in reactiveData.listData"
          :key="data.id"
          :data="data"
          :embedded="embedded === 'profile' || embedded === 'device'"
          :profile="reactiveData.profileTable[data.profileId ?? '']"
          :busy="isActionBusy(data)"
          @delete="onDelete"
          @detail="openDetail"
          @disable="onDisable"
          @edit="openEdit"
          @enable="onEnable"
        />
      </template>
    </div>

    <point-edit-form ref="editRef" @add="onAdd" @update="onUpdate" />

    <el-drawer
      v-model="reactiveData.detailVisible"
      :close-on-click-modal="false"
      :close-on-press-escape="true"
      :title="$t('point.detail.pointInfo')"
      :size="isMobile ? '100%' : '520px'"
      destroy-on-close
    >
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
          {{ reactiveData.detailRecord.unit || "-" }}
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
              ? reactiveData.profileTable[reactiveData.detailRecord.profileId]
                  ?.profileName || "-"
              : "-"
          }}
        </el-descriptions-item>
        <el-descriptions-item
          :label="$t('point.detail.relatedDevices')"
          :span="2"
        >
          {{ reactiveData.detailRecord.deviceCount || 0 }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('point.add.description')" :span="2">
          {{ reactiveData.detailRecord.remark || "-" }}
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else :description="$t('common.description')" />
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from "vue";

import { addPoint, deletePoint, listPoint, updatePoint } from "@/api/point";
import { listProfileByIds } from "@/api/profile";
import { useBreakpoint } from "@/composables/useBreakpoint";
import { usePagedList } from "@/composables/usePagedList";
import { successMessage } from "@/utils/notificationUtil";
import { isNull } from "@/utils/validationUtil";
import { pointTypeKey, rwFlagKey } from "@/utils/pointFormatUtil";

import type { PointRecord } from "@/config/types/manager";

import SkeletonCard from "@/components/card/skeleton/SkeletonCard.vue";
import PointEditForm from "./add/PointEditForm.vue";
import PointCard from "./card/PointCard.vue";
import PointTool from "./tool/PointTool.vue";

type EditFormInstance = {
  show: (profileId: string) => void;
  showEdit: (row: PointRecord) => void;
};

const props = withDefaults(
  defineProps<{
    embedded?: string;
    pre?: boolean;
    next?: boolean;
    profileId?: string;
    deviceId?: string;
  }>(),
  {
    embedded: "",
    pre: false,
    next: false,
    profileId: "",
    deviceId: "",
  },
);

const { isMobile } = useBreakpoint();

const emit = defineEmits<{
  (e: "pre-handle"): void;
  (e: "next-handle"): void;
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
  sortColumn: "createTime",
  request: (query) => listPoint(query),
});

const reactiveData = state as typeof state & {
  detailVisible: boolean;
  detailRecord: PointRecord | null;
  profileTable: Record<string, Record<string, any>>;
  profileLookupLoading: boolean;
  profileLookupError: unknown | null;
};
reactiveData.detailVisible = false;
reactiveData.detailRecord = null;
reactiveData.profileTable = {};
reactiveData.profileLookupLoading = false;
reactiveData.profileLookupError = null;
let profileLookupSequence = 0;
let profileLookupRequestId = 0;
let disposed = false;
const actionBusy = reactive(new Set<string>());

const basePointQuery = computed(() => {
  const q: Record<string, unknown> = {};
  if (!isNull(props.profileId)) q.profileId = props.profileId;
  if (!isNull(props.deviceId)) q.deviceId = props.deviceId;
  return q;
});

const search = (params: Record<string, unknown>) => {
  _search({ ...basePointQuery.value, ...params });
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

const onAdd = (form: unknown, done: (successful?: boolean) => void) => {
  addPoint(form as Record<string, unknown>)
    .then(() => {
      if (disposed) return;
      successMessage();
      void load();
      done(true);
    })
    .catch(() => {
      if (!disposed) done(false);
    });
};

const onUpdate = (form: unknown, done: (successful?: boolean) => void) => {
  updatePoint(form as Record<string, unknown>)
    .then(() => {
      if (disposed) return;
      successMessage();
      void load();
      done(true);
    })
    .catch(() => {
      if (!disposed) done(false);
    });
};

const isActionBusy = (point: PointRecord) => actionBusy.has(String(point.id));

const runAction = async (point: PointRecord, action: () => Promise<unknown>) => {
  const id = String(point.id);
  if (actionBusy.has(id)) return;
  actionBusy.add(id);
  try {
    await action();
    if (disposed) return;
    successMessage();
    await load();
  } catch {
    // handled globally
  } finally {
    actionBusy.delete(id);
  }
};

const onDisable = (point: PointRecord) =>
  runAction(point, () => updatePoint({...point, enableFlag: 'DISABLE'}));

const onEnable = (point: PointRecord) =>
  runAction(point, () => updatePoint({...point, enableFlag: 'ENABLE'}));

const onDelete = (point: PointRecord) =>
  runAction(point, () => deletePoint(point.id, point.version));

const refresh = () => load();

const loadProfileLookup = (points = reactiveData.listData, generation = profileLookupSequence) => {
  const requestId = ++profileLookupRequestId;
  const profileIds = Array.from(new Set(points.map((point) => point.profileId)
    .filter((id): id is string => typeof id === 'string' && id.length > 0)));
  reactiveData.profileLookupError = null;
  if (profileIds.length === 0) {
    reactiveData.profileLookupLoading = false;
    reactiveData.profileTable = {};
    return Promise.resolve();
  }
  reactiveData.profileLookupLoading = true;
  return listProfileByIds(profileIds)
    .then((res) => {
      if (generation !== profileLookupSequence || requestId !== profileLookupRequestId) return;
      reactiveData.profileTable = (res || {}) as Record<string, Record<string, any>>;
    })
    .catch((error) => {
      if (generation !== profileLookupSequence || requestId !== profileLookupRequestId) return;
      reactiveData.profileLookupError = error;
    })
    .finally(() => {
      if (generation === profileLookupSequence && requestId === profileLookupRequestId) {
        reactiveData.profileLookupLoading = false;
      }
    });
};

const retryProfileLookup = () => {
  void loadProfileLookup();
};

const preHandle = () => emit("pre-handle");
const nextHandle = () => emit("next-handle");

watch(
  () => reactiveData.listData,
  (points) => {
    const sequence = ++profileLookupSequence;
    reactiveData.profileTable = {};
    reactiveData.profileLookupError = null;
    void loadProfileLookup(points, sequence);
  },
);

watch(
  () => [props.profileId, props.deviceId],
  () => {
    profileLookupSequence += 1;
    _search(basePointQuery.value);
  },
);

onBeforeUnmount(() => {
  disposed = true;
  profileLookupSequence += 1;
  profileLookupRequestId += 1;
  actionBusy.clear();
});

defineExpose({ reactiveData, refresh });

load();
</script>
