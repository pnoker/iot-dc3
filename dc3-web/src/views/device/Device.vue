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

    <blank-card>
      <el-row>
        <template v-if="reactiveData.loading">
          <el-col v-for="data in 12" :key="data" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
            <skeleton-card :footer="true" :loading="true"/>
          </el-col>
        </template>
        <template v-else>
          <el-col v-if="reactiveData.listData.length < 1">
            <el-empty :description="$t('device.empty')"/>
          </el-col>
          <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
            <device-card
              :data="data"
              :driver="reactiveData.driverTable[data.driverId ?? '']"
              :embedded="embedded != ''"
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
import {computed, ref, watch} from 'vue';

import {
  addDevice,
  deleteDevice,
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

import BlankCard from '@/components/card/blank/BlankCard.vue';
import SkeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
import DeviceAddForm from './add/DeviceAddForm.vue';
import DeviceCard from './card/DeviceCard.vue';
import DeviceImportForm from './import/DeviceImportForm.vue';
import DeviceTool from './tool/DeviceTool.vue';

interface DeviceImportTemplateResult {
  data: BlobPart;
}

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
};
reactiveData.driverTable = {};
reactiveData.statusTable = {};

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

const onAdd = (form: unknown, done: () => void) => {
  addDevice(form as Record<string, unknown>)
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

const openImport = () => {
  deviceImportFormRef.value?.show();
};

const importTemplate = (form: unknown, done: () => void) => {
  importDeviceTemplate(form as Record<string, unknown>)
    .then((res) => {
      const templateResponse = res as unknown as {
        data: DeviceImportTemplateResult;
        headers: Record<string, string>;
      };
      const url = window.URL.createObjectURL(new Blob([templateResponse.data.data]));
      const disposition = templateResponse.headers['content-disposition'] ?? '';
      const name = disposition.split(';')[1]?.split('filename=')[1] ?? 'device-import-template.xlsx';
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', name);
      document.body.appendChild(link);
      link.click();
    })
    .catch(() => {
      failMessage();
    })
    .finally(() => {
      done();
    });
};

const onImport = (form: unknown, done: () => void) => {
  importDevice(form as Record<string, unknown>)
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

const onDisable = (id: string, driverId: string, done: () => void) => {
  updateDevice({id, driverId, enableFlag: 'DISABLE'})
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

const onEnable = (id: string, driverId: string, done: () => void) => {
  updateDevice({id, driverId, enableFlag: 'ENABLE'})
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
  deleteDevice(id)
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

watch(
  () => reactiveData.listData,
  (devices) => {
    // Load status table
    listDeviceStatus({page: reactiveData.page, ...(reactiveData.query as Record<string, unknown>)})
      .then((res) => {
        reactiveData.statusTable = (res.data || {}) as Record<string, string>;
      })
      .catch(() => {
        // handled globally
      });

    // Load driver lookup table
    const driverIds = Array.from(new Set(devices.map((d) => d.driverId).filter((id): id is string => !!id)));
    if (driverIds.length === 0) {
      reactiveData.driverTable = {};
      return;
    }
    listDriverByIds(driverIds)
      .then((res) => {
        reactiveData.driverTable = (res.data || {}) as Record<string, Record<string, any>>;
      })
      .catch(() => {
        // handled globally
      });
  }
);

defineExpose({
  reactiveData,
  refresh,
});

load();
</script>
