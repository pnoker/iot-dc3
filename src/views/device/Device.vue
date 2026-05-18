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
    <device-tool
      :embedded="embedded"
      :page="reactiveData.page"
      @refresh="refresh"
      @reset="reset"
      @search="search"
      @sort="sort"
      @show-add="showAdd"
      @show-import="showImport"
      @size-change="sizeChange"
      @current-change="currentChange"
    />

    <blank-card>
      <el-row>
        <skeleton-card :footer="true" :loading="reactiveData.loading">
          <el-col v-if="hasData">
            <el-empty :description="$t('device.empty')" />
          </el-col>
          <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="8" :md="12" :sm="12" :xl="6" :xs="24">
            <device-card
              :data="data"
              :driver="reactiveData.driverTable[data.driverId]"
              :embedded="embedded != ''"
              :status="reactiveData.statusTable[data.id]"
              @disable-thing="disableThing"
              @enable-thing="enableThing"
              @delete-thing="deleteThing"
            />
          </el-col>
        </skeleton-card>
      </el-row>
      <!--            <el-row>
                            <el-col v-for="data in 12" :key="data" :lg="6" :md="8" :sm="12" :xl="4" :xs="24">
                                <skeleton-card :footer="true" :loading="reactiveData.loading" />
                            </el-col>
                            <el-col v-if="hasData">
                                <el-empty :description="$t('device.empty')" />
                            </el-col>
                            <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="6" :md="8" :sm="12" :xl="4" :xs="24">
                                <device-card
                                    :data="data"
                                    :driver="reactiveData.driverTable[data.driverId]"
                                    :embedded="embedded != ''"
                                    :status="reactiveData.statusTable[data.id]"
                                    @disable-thing="disableThing"
                                    @enable-thing="enableThing"
                                    @delete-thing="deleteThing"
                                />
                            </el-col>
                        </el-row>-->
    </blank-card>

    <device-add-form ref="deviceAddFormRef" @add-thing="addThing" />
    <device-import-form ref="deviceImportFormRef" @import-template="importTemplate" @import-thing="importThing" />
  </div>
</template>

<script lang="ts" setup>
  import { computed, reactive, ref } from 'vue';

  import {
    addDevice,
    deleteDevice,
    listDevice,
    getDeviceStatus,
    importDevice,
    importDeviceTemplate,
    updateDevice,
  } from '@/api/device';
  import { listDriverByIds } from '@/api/driver';

  import type { Order } from '@/config/types';

  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import SkeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
  import { failMessage } from '@/utils/notificationUtil';
  import { isNull } from '@/utils/validationUtil';
  import DeviceAddForm from './add/DeviceAddForm.vue';
  import DeviceCard from './card/DeviceCard.vue';
  import DeviceImportForm from './import/DeviceImportForm.vue';
  import DeviceTool from './tool/DeviceTool.vue';

  interface DeviceListItem {
    id: string;
    driverId: string;
    [key: string]: unknown;
  }

  interface DeviceListPage {
    total: number;
    records: DeviceListItem[];
  }

  interface DeviceQuery extends Record<string, unknown> {
    driverId?: string;
    profileId?: string;
  }

  interface DeviceImportTemplateResult {
    data: BlobPart;
  }

  type DeviceListResponse = R<DeviceListPage>;
  type LookupTableResponse = R<Record<string, unknown>>;
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

  const reactiveData = reactive({
    loading: true,
    driverTable: {} as Record<string, Record<string, any>>,
    statusTable: {} as Record<string, string>,
    listData: [] as DeviceListItem[],
    query: {} as DeviceQuery,
    order: false,
    page: {
      total: 0,
      size: 12,
      current: 1,
      orders: [] as Order[],
    },
  });

  const hasData = computed(() => !reactiveData.loading && reactiveData.listData.length < 1);

  const withFixedQuery = (params: DeviceQuery = {}) => {
    const nextQuery: DeviceQuery = { ...params };

    if (!isNull(props.driverId)) {
      nextQuery.driverId = props.driverId;
    }

    if (!isNull(props.profileId)) {
      nextQuery.profileId = props.profileId;
    }

    return nextQuery;
  };

  const list = () => {
    const query = withFixedQuery(reactiveData.query);
    reactiveData.query = query;

    listDevice<DeviceListResponse>({
      page: reactiveData.page,
      ...query,
    })
      .then((res) => {
        const data = res.data;
        reactiveData.page.total = data.total;
        reactiveData.listData = data.records;

        const driverIds = Array.from(new Set(reactiveData.listData.map((device) => device.driverId)));
        if (driverIds.length === 0) {
          reactiveData.driverTable = {};
          return;
        }

        listDriverByIds(driverIds)
          .then((driverRes: LookupTableResponse) => {
            reactiveData.driverTable = driverRes.data as Record<string, Record<string, any>>;
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

    getDeviceStatus({
      page: reactiveData.page,
      ...query,
    })
      .then((res: LookupTableResponse) => {
        reactiveData.statusTable = res.data as Record<string, string>;
      })
      .catch(() => {
        // nothing to do
      });
  };

  const search = (params: DeviceQuery) => {
    reactiveData.query = withFixedQuery(params);
    list();
  };

  const reset = () => {
    reactiveData.query = withFixedQuery();
    list();
  };

  const showAdd = () => {
    deviceAddFormRef.value?.show();
  };

  const addThing = (form: unknown, done: () => void) => {
    addDevice(form as Record<string, unknown>)
      .then(() => {
        list();
        done();
      })
      .catch(() => {
        // nothing to do
      });
  };

  const showImport = () => {
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

        done();
      })
      .catch(() => {
        // nothing to do
      });
  };

  const importThing = (form: unknown, done: () => void) => {
    importDevice(form as Record<string, unknown>)
      .then(() => {
        list();
        done();
      })
      .catch(() => {
        // nothing to do
      });
  };

  const disableThing = (id: string, driverId: string, done: () => void) => {
    updateDevice({ id, driverId, enableFlag: 'DISABLE' })
      .then(() => {
        list();
        done();
      })
      .catch(() => {
        // nothing to do
      });
  };

  const enableThing = (id: string, driverId: string, done: () => void) => {
    updateDevice({ id, driverId, enableFlag: 'ENABLE' })
      .then(() => {
        list();
        done();
      })
      .catch(() => {
        // nothing to do
      });
  };

  const deleteThing = (id: string, done: () => void) => {
    deleteDevice(id)
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

  defineExpose({
    reactiveData,
    refresh,
  });

  list();
</script>

<style lang="scss" scoped></style>
