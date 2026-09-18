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
    <device-tool
      :embedded="embedded"
      :page="reactiveData.page"
      @refresh="refresh"
      @reset="reset"
      @search="search"
      @sort="sort"
      @open-add="openAdd"
      @open-import="openImport"
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
      v-if="reactiveData.statusError"
      :closable="false"
      :title="$t('common.optionLoadFailed')"
      class="entity-page-error"
      show-icon
      type="error"
    >
      <el-button :loading="reactiveData.statusLoading" link type="danger" @click="retryStatusLookup">
        {{ $t('common.retry') }}
      </el-button>
    </el-alert>

    <el-alert
      v-if="reactiveData.driverLookupError"
      :closable="false"
      :title="$t('common.optionLoadFailed')"
      class="entity-page-error"
      show-icon
      type="error"
    >
      <el-button :loading="reactiveData.driverLookupLoading" link type="danger" @click="retryDriverLookup">
        {{ $t('common.retry') }}
      </el-button>
    </el-alert>

    <blank-card>
      <el-row>
        <template v-if="reactiveData.loading">
          <el-col v-for="data in 12" :key="data" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
            <skeleton-card :footer="true" :loading="true"/>
          </el-col>
        </template>
        <template v-else>
          <el-col v-if="reactiveData.status === 'success' && reactiveData.listData.length < 1">
            <el-empty :description="$t('device.empty')"/>
          </el-col>
          <el-col v-else-if="reactiveData.status === 'error' && reactiveData.listData.length < 1">
            <el-empty :description="$t('common.loadFailed')"/>
          </el-col>
          <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
            <device-card
              :data="data"
              :driver="reactiveData.driverTable[data.driverId ?? '']"
              :embedded="embedded != ''"
              :busy="isActionBusy(data)"
              :status="reactiveData.statusTable[data.id]"
              @delete="onDelete"
              @disable="onDisable"
              @enable="onEnable"
            />
          </el-col>
        </template>
      </el-row>
    </blank-card>

    <device-add-form ref="deviceAddFormRef" @add="onAdd"/>
    <device-import-form ref="deviceImportFormRef" @import="onImport" @import-template="importTemplate"/>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, reactive, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';

import {
  addDevice,
  deleteDevice,
  getDeviceImportOperation,
  importDevice,
  importDeviceTemplate,
  listDevice,
  listDeviceStatus,
  updateDevice,
} from '@/api/device';
import {listDriverByIds} from '@/api/driver';
import {usePagedList} from '@/composables/usePagedList';
import {failMessage, successMessage} from '@/utils/notificationUtil';
import {isNull} from '@/utils/validationUtil';

import type {DeviceRecord} from '@/config/types/manager';
import type {OperationUiStatus, OperationView} from '@/config/types/operation';

import BlankCard from '@/components/card/blank/BlankCard.vue';
import SkeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
import DeviceAddForm from './add/DeviceAddForm.vue';
import DeviceCard from './card/DeviceCard.vue';
import DeviceImportForm from './import/DeviceImportForm.vue';
import DeviceTool from './tool/DeviceTool.vue';

type DialogInstance = { show: () => void };

const props = withDefaults(
  defineProps<{
    embedded?: string;
    driverId?: string;
    profileId?: string;
  }>(),
  {
    embedded: '',
    driverId: '',
    profileId: '',
  }
);
const {t} = useI18n();
const OPERATION_POLL_INTERVAL_MS = 1_000;
let importAbortController: AbortController | null = null;
let lookupSequence = 0;
let statusRequestId = 0;
let driverRequestId = 0;
let disposed = false;

const deviceAddFormRef = ref<DialogInstance | null>(null);
const deviceImportFormRef = ref<DialogInstance | null>(null);

const {
  state,
  load,
  search: _search,
  sort,
  sizeChange,
  currentChange,
} = usePagedList<DeviceRecord>({
  pageSize: 12,
  sortColumn: 'create_time',
  request: (query) => listDevice(query),
});

const reactiveData = state as typeof state & {
  driverTable: Record<string, Record<string, any>>;
  statusTable: Record<string, string>;
  statusLoading: boolean;
  statusError: unknown | null;
  driverLookupLoading: boolean;
  driverLookupError: unknown | null;
};
reactiveData.driverTable = {};
reactiveData.statusTable = {};
reactiveData.statusLoading = false;
reactiveData.statusError = null;
reactiveData.driverLookupLoading = false;
reactiveData.driverLookupError = null;
const actionBusy = reactive(new Set<string>());

const baseDeviceQuery = computed(() => {
  const q: Record<string, unknown> = {};
  if (!isNull(props.driverId)) q.driverId = props.driverId;
  if (!isNull(props.profileId)) q.profileId = props.profileId;
  return q;
});

const search = (params: Record<string, unknown>) => {
  _search({...baseDeviceQuery.value, ...params});
};

const reset = () => {
  _search(baseDeviceQuery.value);
};

const openAdd = () => {
  deviceAddFormRef.value?.show();
};

const onAdd = (form: unknown, done: (successful?: boolean) => void) => {
  addDevice(form as Record<string, unknown>)
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

const openImport = () => {
  deviceImportFormRef.value?.show();
};

const importTemplate = (form: unknown, done: (successful: boolean) => void) => {
  importDeviceTemplate(form as Record<string, unknown>)
    .then((res) => {
      if (disposed) return;
      const templateResponse = res as {data: Blob; headers: Record<string, string>};
      const url = window.URL.createObjectURL(templateResponse.data);
      const disposition = templateResponse.headers['content-disposition'] ?? '';
      const name = disposition.split(';')[1]?.split('filename=')[1] ?? 'device-import-template.xlsx';
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', name);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      done(true);
    })
    .catch(() => {
      if (!disposed) done(false);
    });
};

const operationErrorDetail = (operation: OperationView) => {
  if (operation.error && typeof operation.error === 'object') {
    const error = operation.error as {detail?: unknown; title?: unknown};
    if (typeof error.detail === 'string' && error.detail) return error.detail;
    if (typeof error.title === 'string' && error.title) return error.title;
  }
  return t('device.import.status.failed');
};

const delay = (milliseconds: number, signal: AbortSignal) =>
  new Promise<void>((resolve, reject) => {
    if (signal.aborted) {
      reject(new DOMException('Operation polling aborted', 'AbortError'));
      return;
    }
    const abort = () => {
      clearTimeout(timer);
      reject(new DOMException('Operation polling aborted', 'AbortError'));
    };
    const timer = window.setTimeout(() => {
      signal.removeEventListener('abort', abort);
      resolve();
    }, milliseconds);
    signal.addEventListener('abort', abort, {once: true});
  });

const pollOperation = async (
  statusUri: string,
  signal: AbortSignal,
  report: (status: OperationUiStatus) => void
) => {
  while (true) {
    const operation = await getDeviceImportOperation(statusUri, signal);
    report(operation.status);
    if (!['PENDING', 'RUNNING'].includes(operation.status)) return operation;
    await delay(OPERATION_POLL_INTERVAL_MS, signal);
  }
};

const onImport = async (
  form: unknown,
  file: File,
  idempotencyKey: string,
  report: (status: OperationUiStatus) => void
) => {
  importAbortController?.abort();
  const controller = new AbortController();
  importAbortController = controller;
  try {
    const accepted = await importDevice(form as Record<string, unknown>, file, idempotencyKey);
    if (disposed || controller.signal.aborted) return;
    const reportIfActive = (status: OperationUiStatus) => {
      if (!disposed && !controller.signal.aborted) report(status);
    };
    reportIfActive('PENDING');
    const operation = await pollOperation(accepted.statusUri, controller.signal, reportIfActive);
    if (disposed || controller.signal.aborted) return;
    if (operation.status === 'SUCCEEDED') {
      await load();
      return;
    }
    failMessage(operationErrorDetail(operation));
  } catch {
    if (!controller.signal.aborted && !disposed) report('REQUEST_ERROR');
  }
};

onBeforeUnmount(() => {
  disposed = true;
  importAbortController?.abort();
  lookupSequence += 1;
  actionBusy.clear();
});

const isActionBusy = (device: DeviceRecord) => actionBusy.has(String(device.id));

const runAction = async (device: DeviceRecord, action: () => Promise<unknown>) => {
  const id = String(device.id);
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

const onDisable = (device: DeviceRecord) =>
  runAction(device, () => updateDevice({...device, enableFlag: 'DISABLE'}));

const onEnable = (device: DeviceRecord) =>
  runAction(device, () => updateDevice({...device, enableFlag: 'ENABLE'}));

const onDelete = (device: DeviceRecord) =>
  runAction(device, () => deleteDevice(device.id, device.version));

const refresh = () => load();

const loadStatus = (generation = lookupSequence) => {
  const requestId = ++statusRequestId;
  reactiveData.statusLoading = true;
  reactiveData.statusError = null;
  const page = {...reactiveData.page, orders: [...reactiveData.page.orders]};
  const query = {...(reactiveData.query as Record<string, unknown>)};
  return listDeviceStatus({page, ...query})
    .then((res) => {
      if (generation !== lookupSequence || requestId !== statusRequestId) return;
      reactiveData.statusTable = (res || {}) as Record<string, string>;
    })
    .catch((error) => {
      if (generation !== lookupSequence || requestId !== statusRequestId) return;
      reactiveData.statusError = error;
    })
    .finally(() => {
      if (generation === lookupSequence && requestId === statusRequestId) reactiveData.statusLoading = false;
    });
};

const loadDriverLookup = (devices = reactiveData.listData, generation = lookupSequence) => {
  const requestId = ++driverRequestId;
  const driverIds = Array.from(new Set(devices.map((device) => device.driverId)
    .filter((id): id is string => typeof id === 'string' && id.length > 0)));
  reactiveData.driverLookupError = null;
  if (driverIds.length === 0) {
    reactiveData.driverLookupLoading = false;
    reactiveData.driverTable = {};
    return Promise.resolve();
  }
  reactiveData.driverLookupLoading = true;
  return listDriverByIds(driverIds)
    .then((res) => {
      if (generation !== lookupSequence || requestId !== driverRequestId) return;
      reactiveData.driverTable = (res || {}) as Record<string, Record<string, any>>;
    })
    .catch((error) => {
      if (generation !== lookupSequence || requestId !== driverRequestId) return;
      reactiveData.driverLookupError = error;
    })
    .finally(() => {
      if (generation === lookupSequence && requestId === driverRequestId) reactiveData.driverLookupLoading = false;
    });
};

const retryStatusLookup = () => {
  void loadStatus();
};

const retryDriverLookup = () => {
  void loadDriverLookup();
};

watch(
  () => reactiveData.listData,
  (devices) => {
    const sequence = ++lookupSequence;
    reactiveData.driverTable = {};
    reactiveData.statusTable = {};
    reactiveData.statusError = null;
    reactiveData.driverLookupError = null;
    void loadStatus(sequence);
    void loadDriverLookup(devices, sequence);
  }
);

watch(
  () => [props.driverId, props.profileId],
  () => {
    lookupSequence += 1;
    _search(baseDeviceQuery.value);
  }
);

defineExpose({
  reactiveData,
  refresh,
});

load();
</script>
